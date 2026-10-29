# Android replica development — first Home batch

Issue #40, D-020. One Kotlin/Compose app: `org.ragbot.replica` (launcher
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

The local comparison build uses 18 reviewed private assets via neutral resource
keys. Nothing is bulk-copied or committed. Evidence RF-ASSETS records the APK
baseline, exact sources, transformations and SHA-256 for every input/output in
`local/evidence/replica-foundation/prepared-asset-manifest.json`.

```bash
python local/evidence/replica-foundation/prepare_assets.py
printf 'replicaPrivateResources=%s/local/evidence/replica-foundation/resources\n' "$PWD" \
  > "$GRADLE_USER_HOME/gradle.properties"
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
