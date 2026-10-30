# Android replica development

Issues #40 (foundation), #42 (populated Home), #44 (FR-02 accounts), #46 (FR-03 cards), #48 (FR-04a card requests), #50 (FR-04b card blocking/PIN), #52 (FR-04c wallet cards), #54 (FR-05 profile/settings), #56 (FR-06 notifications/support) and #58 (FR-07 saved destinations/shortcuts), D-020. One Kotlin/Compose app: `org.ragbot.replica` (launcher
`.MainActivity`, label **RagBot Replica**). Tap **ورود به خانه** to open Home.
No credentials, banking service, INTERNET permission, assistant or bridge is present.

## Pinned build inputs

| Input | Version / reason |
|---|---|
| Gradle | 9.4.1, official wrapper and distribution SHA-256 pinned |
| Android Gradle Plugin | 9.2.0, stable support for API 37.0 |
| Kotlin / Compose compiler plugin | 2.2.10, matching AGP's built-in Kotlin; no separate Android Kotlin plugin |
| Compose | BOM 2025.08.00 (UI/runtime/foundation 1.9.0; Material3 1.3.2) |
| Activity Compose | 1.10.1 |
| JDK | Existing project Temurin 21.0.13+9; Java bytecode target 17 |
| SDK platform | Android 37.0, revision 2; compile/target 37, min 24 |
| Build tools | 36.0.0 |
| SDK command-line tools | 22.0 (archive 15859902), also pinned in CI; no retired `tools` package |
| Device | Existing `ragbot_investigation`, API 35; see [profile](emulator-profile.md) |
| Tests | JUnit 4.13.2; AndroidX runner 1.6.2, ext JUnit 1.2.1; Compose UI test via BOM |

The recovered min 24 / target 37 settings are preserved. A target API does not
require a matching device API. The older pinned Compose/Activity releases provide
everything this frame needs; no need to import the original dependency list.
Compatibility was checked against the official [AGP 9.2 release notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes)
and [Compose compiler setup](https://developer.android.com/develop/ui/compose/compiler).

## Server environment and build

From `/root/projects/assistant`, inside `conda activate assistant`:

```bash
source android/env.sh
./android/gradlew -p android --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am force-stop org.ragbot.replica
adb -s emulator-5554 shell am start -n org.ragbot.replica/.MainActivity
```

The source revision appears on the demo entry. It is generated from the checkout
HEAD at build time; rebuild after switching commits. APKs remain ignored build
outputs. Unsaved source edits are not represented by that revision: the acceptance
record also identifies the tested tree, and final installation uses merged source.

`env.sh` selects the existing local SDK/JDK, Gradle user cache under
`local/toolchain/gradle-home`, Android user files under
`local/toolchain/replica-android-user`, and temporary files under
`local/scratch/replica-foundation`. The debug signing key is generated at
`local/toolchain/replica-debug.keystore` by `env.sh` on first use (standard public
Android debug alias/password; never a release signing key). CI creates its own
ephemeral key at that same project-local path. It never changes `HOME` or `CODEX_HOME`.
Build outputs stay in ignored `android/**/build/`. No global install is needed.
CI also installs its SDK and keeps its Gradle/Android user caches and temporary
files under the checkout's `local/`, using its pre-provided JDK through setup-java.

If the existing emulator is stopped, use section 5 of the emulator profile with
both `restrict=on` options. Keep its existing AVD path; do not recreate it or
change locale, density, font scale, guest network or snapshots for this task.
Only install/launch `org.ragbot.replica` and its instrumentation package.

## Download provenance and blocked official routes

Gradle was downloaded from `services.gradle.org` and matched its published
SHA-256: `2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb`.
The wrapper was generated with that verified distribution, rather than a
third-party wrapper binary.

Official Android SDK and Google Maven downloads returned HTTP 404 from this
server. The existing owner-approved mirror policy applies. SDK zips came from
`https://mirrors.cloud.tencent.com/AndroidSDK/` and matched both its publisher
index and the independent nixpkgs index already pinned by P1a-02:

| File | Bytes | Publisher SHA-1 |
|---|---:|---|
| `platform-37.0_r02.zip` | 67281901 | `ed8ebf7f8822a4de5686d427f237d2fa30ff7410` |
| `build-tools_r36_linux.zip` | 63737259 | `b0b6376977657e8ad9b969bacf4093601da2c6fb` |

Downloaded indexes/zips and installation log remain under task-local scratch
and evidence. Installation used the existing sdkmanager with
`SDK_TEST_BASE_URL=file://$PWD/local/scratch/replica-foundation/`, whose
`repository2-4.xml` and verified zips form a local SDK repository, then:

```bash
local/toolchain/android-sdk/cmdline-tools/latest/bin/sdkmanager \
  --sdk_root="$ANDROID_HOME" 'platforms;android-37.0' 'build-tools;36.0.0'
```

Maven Central artifacts come from the official `repo.maven.apache.org` and
match its published SHA-1 before use. Google Maven artifacts come from Aliyun's
Google repository; each file matches an independent Huawei copy's publisher
SHA-1, byte size and SHA-256 before being served to Gradle. For the two large
lint compiler jars, curl-prefetched bytes additionally matched Huawei's
independent published checksum/size headers (both SHA-1 and SHA-256); those
headers and receipts are retained locally. The build-only
helper uses the system CA bundle and retains TLS verification. It listens only
on `127.0.0.1:8766`; it has no connection to the emulator or app.
The local Gradle user properties set a 300000 ms read timeout for this
verify-before-serve bootstrap; app behavior and CI do not depend on it.

To rebuild a lost server Maven cache, start this helper in another activated
shell, then bootstrap Gradle in the first shell:

```bash
python android/tools/verified_maven.py > local/evidence/replica-foundation/maven-proxy.log 2>&1
# In the first shell, with android/env.sh sourced:
REPLICA_MAVEN_CACHE=http://127.0.0.1:8766 \
  ./android/gradlew -p android --no-daemon -I "$PWD/android/mirror.init.gradle" \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
```

Per-artifact origin, size and SHA-256 receipts are in
`local/scratch/replica-foundation/maven-provenance.jsonl`. The verified files
form a regular Maven repository at `local/scratch/replica-foundation/maven`.
After bootstrap, stop only this helper (Ctrl-C) and use the file cache:

```bash
export REPLICA_MAVEN_CACHE="file://$PWD/local/scratch/replica-foundation/maven"
mkdir -p "$GRADLE_USER_HOME/init.d"
cp android/mirror.init.gradle "$GRADLE_USER_HOME/init.d/replica-mirror.gradle"
```

On later shells, `env.sh` detects that local init script and selects the file
cache. The normal acceptance command then works without a build proxy process.
Public builds and CI use official repositories; this server init script is
opt-in and lives only in the local cache. Do not disable TLS or run with an
unverified binary fallback. A checksum/size discrepancy stops dependency use.

## Private visual inputs

Public builds show an explicit font/icon fidelity notice before Home. Their
system font and text-symbol icons are functional development substitutes.
They do not establish original-app visual fidelity.

The Home build used 51 reviewed private inputs (HC-ASSETS) and FR-02 64
(AI-ASSETS), then FR-03 used 76 (CI-ASSETS). FR-06 reuses that verified chain
and adds four recovered rating vectors: 80 neutral resource keys (NS-ASSETS).
FR-07 adds four caller-backed shortcut vectors, for 84 keys (SD-ASSETS).
The current full manifest is
`local/evidence/saved-destinations/prepared-asset-manifest.json`. It records
the APK baseline, exact sources, transformations and SHA-256 for every input/output.
Nothing is bulk-copied or committed.

```bash
python local/evidence/saved-destinations/prepare_assets.py
python - <<'PYSETUP'
import os
from pathlib import Path
settings = Path(os.environ["GRADLE_USER_HOME"]) / "gradle.properties"
previous = settings.read_text().splitlines() if settings.exists() else []
kept = [line for line in previous if not line.startswith("replicaPrivateResources=")]
kept.append(f"replicaPrivateResources={Path.cwd()}/local/evidence/saved-destinations/resources")
settings.write_text("\n".join(kept) + "\n")
PYSETUP
```

This private preparation script/mapping is a required local comparison-build
input, kept with the evidence. It verifies the baseline APK and source/output
hashes before copying the reviewed subset. A clean public checkout can build
without it. If requested private resources are missing, the build/runtime fails
explicitly; it does not silently claim an equivalent replacement.

## Tests and device evidence

Unit tests check the registry against the committed semantic graph and reject
unimplemented destinations. Instrumentation checks demo entry, Persian text,
RTL placement, selected Home, unavailable controls, Back, recreation, empty
account data and lack of network permission. JUnit/Compose tests require no DI,
mocking library, database, Robolectric or screenshot-golden framework.

Private QA artifacts are under `local/evidence/replica-foundation/`. Capture
screenshots and UI hierarchy only for the replica. Select tap coordinates from
the hierarchy, not from screenshots. Keep logs scoped to the replica PID and
check its crash records. Device tests run on this server; CI runs build, unit
tests and lint. Original-app fidelity remains unscored until the owner compares
matching states; our own screenshots are not goldens proving equivalence.

## Populated Home and the next batches

Issue #42 completes the configured Home from the two owner references. On demo
entry, all account/history values are explicitly synthetic. Tap **ورود به خانه**,
scroll the white content panel, and use **ویرایش چیدمان** to open the recovered
service-order editor. Drag services, then **اعمال تغییرات**; **انصراف** or Back
leaves the current order unchanged. Accessibility move-before/move-after actions
are an explicitly designed preview alternative. Applied order is local saved UI
state, not a banking save. Fixed and quick groups are unaffected. Unimplemented
controls show a notice while retaining Home state and scroll position.

The banner has one evidenced slide only. The private illustration is extracted
from the owner's visual reference; its original server asset is not recovered.
The rest of the banner, its text and indicators are Compose. Public fallback
builds disclose substituted fonts/icons/banner/heading on the demo entry.
The 51-input private preparation manifest pins every source/output hash and the
reference-art crop. Keep that manifest/script/resources local; never commit them.

Home acceptance adds menu membership/group/RTL ordering, lower-service/history
reachability above the bottom bar, real drag Apply/Cancel, editor tile notices,
scroll/header restoration and recreation. The Home requirements are in
[the batch matrix](../reports/home-requirements.md); results and fidelity limits
are in [Home completion](../reports/home-completion.md).
The only remaining frontend tracker is
[frontend replication steps](../plan/frontend-replication-steps.md). These are
proposals; neither opening Home nor this runbook approves their implementation.

After device tests (which may uninstall the tested package), explicitly install
the comparison APK and launch it using the commands above. Capture top, lower
services, recent history, unavailable notices and editor states to the private
batch evidence folder. Use current UI-tree coordinates for taps. Keep PID-scoped
logs and crash evidence. Forwarding the optional local viewer on port 6080 does
not affect app state, emulator profile or guest networking.


## FR-02 accounts, statement and legacy review entries

Open **ورود به خانه → حساب** for the embedded account tab. **لیست و تغییر حساب**
opens the separate picker; **مدیریت حساب** opens management, then
**فراخوانی حساب های جدید** opens the local candidate fixture. Apply affects
only local visibility; Cancel/Back discards the draft. No banking account is
created, changed or activated. **صورتحساب** opens recovered bank-statement
list `S-016`; tapping a line opens `S-014`. Use the Persian date inputs/calendar,
apply, scroll and show-more/refresh to inspect the finite local fixture.

The demo entry separately offers **سوابق قدیمی** (`S-091` → `S-090`) and
**درخواست صورتحساب** (`S-086`). These explicit review entries are Designed for
mock; they do not claim current Home routes. Home history remains unavailable
because its actual hub is outside this batch. Export requests, downloads,
receipts, authenticated mutations and external sharing remain honest notices.
Scenario chips on the demo entry select populated/empty/loading/error fixture
states; they stay outside replicated content.

FR-02 assets are prepared with the command above. AI-ASSETS verifies the prior
51 resources and 13 newly reviewed inputs, including the export-file control;
`local/evidence/accounts-inspection/prepared-asset-manifest.json` records all
64 source/output hashes. Public builds need none of these files and disclose
substitutes. Run the full Android command from the environment section; server
device tests cover accounts, date validation, statement/history list/detail,
selection, draft cancellation, unavailable controls and Home state retention.
The [FR-02 report](../reports/accounts-inspection.md) and
[requirements](../reports/accounts-requirements.md) separate behavior from
unscored visual fidelity.

## FR-03 cards tab and review entries

Open **ورود به خانه → کارت** for the hosted card tab. Swipe cards, switch the
group chips and tap **مشاهده موجودی** on a debit card (hidden, shown, zero and
missing balances are separate). Rows open card information `S-012`, the
legacy list `S-125`, credit detail `S-131`, file-loan summary `S-134`, the
bon-balance form `S-119` and, for credit cards, legacy history `S-091`. Issue,
block, PIN, share, pay and file-detail controls explain that they are
unavailable. The demo entry's **مدیریت کارت** (`S-126` → `S-068`),
**نمایش کارت** (`S-130`) and **موجودی بدون ورود** (`S-120`) buttons are
Designed-for-mock review entries for menus not built yet. Secure balance forms
validate only; no PIN is processed and no balance is returned.

CI-ASSETS verifies the previous 64 resources plus 12 card inputs (76) and keeps
private bitmaps in their source density folder. Public builds show a plain
colour behind the card face. The [FR-03 report](../reports/cards-inspection.md)
and [requirements](../reports/cards-requirements.md) separate behavior from
unscored visual fidelity.

## FR-04a card operation requests

On the card tab's debit group, **درخواست کارت** opens issue `S-124`;
**تمدید کارت**, **المثنی کارت** and **تغییر رنگ کارت** open modify `S-127`.
In-person delivery goes to colour `S-123`; postal goes to address `S-129`
first. Confirm `S-128` ends in a labelled Designed-for-mock result: nothing is
issued, changed or charged. Cancel returns to the card tab; Back keeps the
draft. Adding an address is unavailable (FR-05). No new private assets.
See the [FR-04a report](../reports/card-requests-inspection.md).

## FR-04b card blocking and PIN

On the card tab, **مسدودسازی کارت** (every group) opens block `S-121` for the
current group's cards; **رمز کارت** opens PIN `S-135`; **فعال‌سازی رمز پویا**
opens activation `S-136`. The demo entry's **مسدودی بدون ورود** and
**مسدودی بدون ورود ۲** open the pre-login block forms `S-118`/`S-122`. The OTP
is the fixed local code `12345`; every valid confirm ends in a labelled
Designed-for-mock result and nothing is changed. PIN, OTP, CVV2 and second
password values are never kept in saved state. See the
[FR-04b report](../reports/card-security-inspection.md).

## FR-04c wallet-linked cards

The card tab's **کیف کارت** group shows the wallet card with its own balance.
**اولین کیف کارت** opens send type `S-140` → address `S-137` → confirm `S-139`
(hosted by container `S-138`) and ends in a labelled result; **دریافت و تغییر
رمز** opens wallet PIN `S-141` (OTP `12345`). See the
[FR-04c report](../reports/wallet-card-inspection.md).

## FR-05 profile, settings and addresses

**ورود به خانه → پروفایل** opens the hosted Profile tab. Its rows open edit
profile `S-065` (credential changes use OTP `12345` and a labelled result),
additional information `S-064`, account management `S-063`, card management
`S-126`, saved addresses `S-464` → add `S-463`, the settings menu `S-025`
(biometric `S-070`, services `S-072`, storefront `S-073`, card display
`S-130`), invitation `S-069` and changelog `S-074`. Notification settings
`S-071` has a labelled review entry on the demo page. Profile, address and
toggle changes stay in the app's saved UI state only. The card-request and
wallet address steps now open add address `S-463`. See the
[FR-05 report](../reports/profile-inspection.md).

## FR-06 notifications and support

From Home, the notification header button opens **اعلان‌ها** (`S-075`):
type filters, item detail/copy, selection and read/delete controls affect only
synthetic preview state. Deleting asks first; dismissing cancels. Both the
support/message header button and **پشتیبانی** menu tile open authenticated
FAQ (`S-077`) through independently recovered handlers. Expand each FAQ item
to inspect its synthetic answer.

The demo page's explicitly independent entries open chatbot question `S-076`,
FAQ without login `S-078`, suggestion `S-079`, ticket list `S-080` and the
no-ticket new-message variant of `S-082`. The ticket list opens the selected
ticket in `S-082`; **پیام جدید بدون تیکت** instead opens an empty composer.
In `S-078`, **ارسال/مشاهده پیام** uses a labelled local phone/OTP gate
(code `12345`, never saved) before `S-081` with compose/history tabs.
Suggestions show a local result; sending/editing/closing and chatbot answers
show unavailable notices. No message, SMS or answer request leaves the app.

Scenario controls remain on the demo page. The unanswered/answered/closed
fixture tickets exercise editing, a long thread with continuation controls,
and read-only closed state. No server support-menu snapshot, ticket delivery,
attachment control, session-expiry service or real authentication is claimed.
S-071 remains independent. See the [inspection](../reports/notifications-support-inspection.md)
and [requirements](../reports/notifications-support-requirements.md). FR-07 was
subsequently approved in #58; later batches still require owner approval.

## Desktop viewer and stopping the simulation

The optional viewer is loopback-only. In VS Code Remote SSH, forward **6080**
privately, then open `http://127.0.0.1:6080/vnc.html?autoconnect=1&resize=scale&path=websockify`
(using the local port VS Code assigns). The private operational README is
`local/toolchain/replica-viewer/README.md`; the password stays in
`local/scratch/replica-viewer/password.txt`.

From the SSH terminal, start/stop the already-installed viewer with:

```bash
local/toolchain/replica-viewer/start.sh
local/toolchain/replica-viewer/stop.sh
```

Stopping the viewer leaves the emulator running. To stop only the replica, or
then shut down this project's emulator when finished reviewing:

```bash
source android/env.sh
adb -s emulator-5554 shell am force-stop org.ragbot.replica
adb -s emulator-5554 emu kill
```

To restart the emulator, use the pinned profile's boot command with both
`restrict=on` flags in [emulator profile §5](emulator-profile.md#5-commands),
then install/launch with the commands above and start the viewer. There is no
permanent service or public ADB listener. Do not use these commands against any
other device, original banking app or shared infrastructure.

## Saved destinations and shortcuts (FR-07)

Home's **جستجو** header opens service search and local pins (S-084); its bookmark
is the separate recurring-transfer entry and remains unavailable. On the demo
page, scroll below Home entry to independent **مقصدهای ذخیره‌شده قدیمی** (S-066),
**مدیریت مقصد تسهیلات** (S-197) and **انتخاب مقصد تسهیلات — بازبینی مستقل**
(S-198). The last entry returns to a labelled local review form; it is not a
transfer route. Swipe a destination row to reveal controls. Legacy **ویرایش ترتیب**
enables long-press vertical drag with local Apply/Cancel.

All edits/pins/results are local preview state. Service inquiry and synthetic
shortcut launches are unavailable. See the [inspection report](../reports/saved-destinations-inspection.md)
and [requirements](../reports/saved-destinations-requirements.md). Private captures,
raw evidence and acceptance logs are under `local/evidence/saved-destinations/`.
