# Stage 1 — replica foundation and first Home batch

Approved issue [#40](https://github.com/Hooman1996/RagBot-Assistant/issues/40),
29 October 2026. Based on main `055e628`; fetch found no later main work.
D-020 governs the order. This is one Home batch, not whole-app reconstruction
or completed feature selection. Setup and commands: [replica development](../runbook/replica-development.md).

## Implemented scope and evidence

One Kotlin/Compose application with a distinct identity, a replica-only demo
entry and Home. No credentials or service connection is needed. The launcher
does not consume intent extras/data, expose deep links or redirect intents.
There is no network permission, database, banking client, assistant, transaction
path or bridge. The only exported application component is its launcher.

Recovered structure informs the RTL header, expandable account region, downward
indicator, rounded content panel and custom four-tab bottom bar with central
barcode control. Menu/transaction lists and account fields are empty local
preview inputs. No dynamic tile configuration or transition has been guessed.
Only `S-026` and its embedded `F-066` hub are registered. The hub is visible
content within Home, not an invented second screen. Tabs do not acquire IDs
from legacy account/card activities merely because their purposes are similar.

The private evidence ledger RF-HOME records source locations, confidence and
limits. It reuses P1a-01/03/04/05 findings and the existing decode; focused REA
queries inspect only the Home host/frame. No original app was installed,
executed or contacted. Asset input evidence RF-ASSETS records the 18 reviewed
private resources, provenance and hashes. Public source contains our own
implementation and neutral keys; original assets/mappings remain private.

### Deliberate preview behavior and fidelity gaps

- **Designed for mock:** demo entry chooses Home without authentication;
  Home Back returns to the demo entry. Back first closes a preview notice or
  collapses expanded account details. Recreation preserves these local states.
- **Designed for mock:** empty account details can be expanded with the downward
  indicator. This makes the empty local state inspectable; the exact original
  expansion trigger/animation remains for owner comparison.
- **Designed for mock:** a control needing unimplemented content opens a Persian
  unavailable notice while Home stays selected. Logout has no authentication
  side effect. QR does not open a scanner or payment flow.
- **Unknown:** authenticated menu records/order, banners, feature visibility,
  saved default account/tab and customer details. Notifications/support use the
  recovered initial hidden state. No banner/menu/customer record is fabricated.
- **Unknown:** exact endpoint joins for Home. Existing endpoint/model maps are
  preserved. Model relevance does not establish a screen-to-endpoint contract.
- **Visual limits:** public builds use explicitly disclosed system fonts and
  text-symbol icons. The private build consumes a reviewed asset subset. The
  transaction heading is sanitized; original branding is omitted. Insets,
  avatar composition, typography scaling, panel/header geometry, optional
  sections and interaction timing still need owner comparison. There is no
  pixel-similarity score or replica-derived fidelity golden.

Functionality and original-app fidelity are separate results. This batch can
establish a runnable evidence-informed frame and tested preview behavior;
original visual equivalence remains **unscored/pending owner comparison**.

## First-batch requirements assessment

All proposed records/services below are **Designed for mock**, not recovered
bank tables. Effort is preliminary for the indicated continuation only.
RF-HOME and RF-ASSETS are private evidence IDs under this task's evidence root.
`M-004` and `M-023` describe relevant data shapes; no precise `E-###` join is
claimed without supporting caller evidence. Menu and recent-transaction shapes
have no assigned model ID in the current model map.

| Existing node / state | Purpose; user inputs | Required data and current source | E/M evidence | Preconditions; expected result | Proposed future mock data/service | Unknowns / evidence | Effort; confidence |
|---|---|---|---|---|---|---|---|
| `S-026`, demo entry | Open Home; one tap, no credentials | Local entry state | None needed | Replica installed → Home | No service required | Entry is Designed for mock; RF-QA | Small; high |
| `S-026`, frame/header | Show toolbar/profile and current selection; no text input | Local Home selection; empty profile image, private asset or public substitute | Endpoint join Unknown | Demo entered → Home remains selected | Synthetic display profile and visibility flags, if later needed | Authenticated toolbar visibility/photo Unknown; RF-HOME | Small/medium; high structure, medium fidelity |
| `S-026`, account/balance | Show selected account and hidden balance; optional expand tap | Empty owner/type/number/IBAN local inputs; no balance value | Relevant `M-004`, `M-023` Recovered; exact E join Unknown | No account selected → blank fields; balance/increase/manage/share actions unavailable | Synthetic selected-account record, integer `balance_rial` and explicit `IRR` unit; future read API | Ownership/default-account rules, labels and formatting require validation; RF-HOME | Medium; medium |
| `F-066`, embedded menu hub | Show server-configured services; no preview tile input | Four menu/configuration lists left empty locally | No assigned M ID; E join Unknown | Home → visible empty hub; no dynamic outgoing edge executed | Synthetic Home configuration with verified title/icon/order/visibility and explicit destination allowlist; future config API | Owner menu configuration needed before populating; RF-HOME | Medium/higher; high empty-state honesty, low populated fidelity |
| `S-026`, recent transactions | Show history section; no input | Empty local list | No assigned M ID; E join Unknown | Home → heading and no invented transaction rows | Synthetic recent-history records with exact integer amounts/unit; future read API | Empty-state appearance and client shape still need review; RF-HOME | Medium; medium |
| `S-026`, four-tab bar | Home selection; account/card/profile taps | Local selected Home and unavailable notice | No service used; tab-state IDs not separately catalogued | Home reselect stays Home; other tabs leave Home selected and explain unavailable | Future per-tab state and verified data, assessed in separate batch | Do not substitute `S-085`/`S-125` for unproven tab identity; RF-HOME/RF-QA | Medium per tab; high current behavior |
| `S-026`, barcode control | Request QR feature; tap | Local unavailable notice | Endpoint/model join Unknown | Home → unavailable notice; no camera/payment | Later verified eligibility/config record and separate flow, if approved | Auth/menu gates and exact destination Unknown; RF-HOME | Medium/higher; high notice, low destination confidence |
| `S-026`, toolbar actions | Profile, saved items, search, logout taps | Local unavailable notice | None called; precise E/M join Unknown | Home → notice; dismissal/Back restores Home | Future synthetic profile/saved-menu records and local menu search if recovered; logout requires later session design | Header destinations and visibility not inferred from shared feature areas; RF-HOME | Small/medium each; high notice, medium structure |

No API/mock schema is implemented in this batch. This table covers only the
implemented Home frame and its controls; other features' requirements remain
unassessed.

## Verification record

Baseline: 131 project checks passed, 112 pytest tests passed, Ruff lint and
format passed. Android has no previous implementation/tests.

Android strategy: two focused JVM tests for semantic registry/unsupported
controls and four device tests for entry, RTL/accessibility semantics,
unavailable navigation, Back/recreation and empty-data handling. No DI framework
or screenshot-test infrastructure was added because this batch needs neither.

The exact Android acceptance command passed from the verified file cache:

```text
./android/gradlew -p android --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
Starting 4 tests on ragbot_investigation(AVD) - 15
Finished 4 tests on ragbot_investigation(AVD) - 15
BUILD SUCCESSFUL in 54s
85 actionable tasks: 20 executed, 65 up-to-date
```

Two JVM tests and four device tests passed, with zero failed/skipped tests.
Lint passed with **zero errors and 26 warnings** in the private build: intentional
dynamic resource lookup/unused-resource false positives, densityless private
pattern assets, missing launcher icon, backup-rule guidance and a newer Gradle
version notice. No warning was suppressed. Those are limits of this small debug
foundation, not proof of visual equivalence. The standard Compose path library
is packaged without symbol stripping because no NDK was needed/installed.

RF-BUILD is `acceptance-status-contrast.log` under the private evidence root. The cache audit
matched all 992 unique artifacts against their provenance receipt size and
SHA-256 before the file-cache acceptance run; the bootstrap helper was then
stopped. Earlier setup/download failures are retained privately and are not
reported as passing tests.

Rendered QA RF-QA passed on the pinned 1080×2400 / 420 dpi / `fa-IR` / font-scale
1.0 emulator: cold launch, demo entry, rightmost selected Home, expansion,
unavailable account tab, notice dismissal, collapse and return to demo. Taps
were derived from the current UI hierarchy. Home, expanded account and notice
screenshots were inspected; Persian text was readable, no visible clipping was
found and all captured hierarchy nodes were within the viewport. A system-bar
contrast defect found during this inspection was fixed and covered by a device
assertion. The final screenshot confirms white icons on the dark header.
Replica PID logs contain no crash. No original app comparison was performed.

Private evidence root: `local/evidence/replica-foundation/`:

- RF-HOME: `home-evidence.md`; RF-ASSETS: `prepared-asset-manifest.json`.
- RF-BUILD: `acceptance-status-contrast.log`; cache receipt audit: `cache-audit.json`.
- RF-QA: `qa-results.json`, `cold-launch.txt`, `demo.png`, `home.png`,
  `expanded.png`, `unavailable.png`, matching XML dumps, `logcat.txt`, `crash.txt`.

Repository checks after implementation: 131 passed; pytest: 112 passed; Ruff
lint and format checks passed. Only this batch is complete; visual fidelity and
the rest of Stage 1 are pending.

## Owner comparison and next proposed batch

Compare the private Home screenshot against the real app on equivalent state:
toolbar visibility, account header/empty state, fonts, bottom-bar spacing,
system insets, menu sections and recent-history empty state. Supply reviewed
references for authenticated menu configuration and expandable account details
before those portions can pass fidelity review. Original-app material stays
private; this environment must not obtain credentials or run the original.

Next proposed batch: refine Home against those owner references, then reconstruct
the account-tab frame and its verified empty/selection states. First establish
its graph identity and data requirements; do not assume a legacy account-list
activity is the tab. That batch needs its own approved issue. It has not started.
