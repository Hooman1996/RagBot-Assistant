# FR-08 — Individual card/account transfers

Approved issue [#60](https://github.com/Hooman1996/RagBot-Assistant/issues/60).
Owner approval covers FR-08 only. Next proposed batch is FR-09; it does not start
automatically. The [requirements matrix](individual-transfers-requirements.md)
records all ten nodes, incoming context, validation, fixtures, candidate models,
future services and confidence separately from uncertain runtime behavior.

## Baseline and recovery

Fetched main was `e950f44b2a8dd2e9c90f01906882c3437edc8e85`, with no newer work
or unrelated checkout edits. Baseline passed 131 repository checks, 112 Python
tests, both Ruff checks, Android build, 26 JVM tests and lint (0 errors / 117
warnings). Device baseline passed 56/57: the existing saved-destination picker
test timed out after a coordinate click while IME/scroll moved the demo entry.
Its helper now invokes the button's semantic click and waits for the page;
existing assertions remain. The focused FR-08 run passed all nine device tests,
including that regression. Failed baseline output and original reports remain
under `local/evidence/individual-transfers/baseline-artifacts/`.

IT-CARD and IT-ACCOUNT identify focused REA and decoded caller/resource ledgers.
Static recovery never ran the original app or contacted production. No original
assets, code findings, raw output or captures enter GitHub. No catalogue or graph
allocation changes were needed. S-194/S-195 remain unresolved and unregistered.

## Navigation and preview boundary

S-174/S-175 show distinct anonymous card forms. Their recovered anonymous registry
configuration chooses source picker versus editable source. Both can open S-176
with a synthetic inquiry summary. S-193's two local menu children open S-192
internal and S-191 interbank forms; validated local drafts open S-173. Reviews
allow corrections, notes and a local favorite checkbox. Final submission, OTP,
attachments, scanning, contacts, remote destination selection and sharing show
honest unavailable notices. No final handler submits or authorizes a transfer.
S-093/S-097/S-098 are independent synthetic receipts, never fake success pages.

Recovered Home all-services/menu callbacks join the registered transfer-menu
keys. The current replica lacks that caller surface. The fixed Home transfer
quick tiles are separate and their exact destinations remain Unknown, so they
stay unavailable. Labelled independent entries outside replicated content make
the implemented forms/menu/receipts inspectable without inventing a Home route.
The original account destination launcher is legacy, not FR-07's loan picker.

Per-variant/per-mode non-secret drafts retain source, recipient, exact integer
IRR amount, reason, optional identifiers/descriptions, note and local checkbox
through Back/recreation. Root navigation dismisses focus/IME. Card passwords,
CVV2 and expiry stay transient and reset; no secret enters saved state. Source
and reason dialogs cancel without mutation. No network permission, arbitrary
Intent, push, authentication or bank service is introduced.

## Opening the screens

From the demo page, scroll past the existing saved-destination review controls
to **بازبینی مستقل انتقال‌ها — فقط پیش‌نمایش محلی**.

| Entry | What opens |
|---|---|
| **کارت به کارت بدون ورود با انتخاب مبدا** | S-174 source-picker variant; choose the disclosed sample card |
| **کارت به کارت بدون انتخابگر مبدا** | S-175 editable source variant |
| Card form **بررسی محلی اطلاعات** | S-176 review; synthetic 16-digit source `0000000000000001`, destination `0000000000000002`, amount `250000` rial |
| **منوی انتقال — نمونهٔ مستقل** | S-193 → internal S-192 or interbank S-191; choose source, synthetic reason and amount |
| Account form **ادامه — بررسی محلی** | S-173 review; internal destination `0000000000002`; synthetic checksum-valid IBAN `IR220000000000000000000001` (does not establish a bank account) |
| **رسید مستقل کارت به کارت — نمونه** | S-093 |
| **رسید مستقل انتقال داخلی — نمونه** | S-098 |
| **رسید مستقل انتقال شبا — نمونه** | S-097 |

Scenario controls remain on the demo page. The existing loopback-only viewer
and SSH forwarding remain in the [runbook](../runbook/replica-development.md#desktop-viewer-and-stopping-the-simulation).

## Validation and remaining fidelity gaps

Card validation is explicitly shape-only; checksum/eligibility and the corrupted
amount-rule decompile are not claimed recovered. Internal 13-digit validation,
IBAN mod-97, recovered field lengths and IRR units are documented separately from
mock positive-Long guards. No fee, limit, bank ownership or successful inquiry
is invented. M-025/M-033 inventory summary discrepancies are recorded in the
requirements, without treating their summaries as encrypted wire contracts.

The account extra-fields editor is inline instead of a sheet; source carousel,
expiry picker, conditional receipt rendering and service-error families remain
incomplete. All data and generic scenario states are synthetic. Existing private
84-resource overlay and public substitute disclosure are reused; no new asset
extraction is needed. Original-app visual fidelity remains unscored. Earlier
FR-05/06/07 fidelity and service gaps are unchanged.

## Verification and delivery

Final server acceptance passed: **131 repository checks, 112 Python tests,
Ruff check/format, Android assemble, 31 JVM tests and 65 device tests**, with zero
failures, errors or skips. Lint is **0 errors / 117 warnings**, unchanged from
baseline; comparison found no new or removed diagnostics. The complete suite
preserves previous Home/Accounts/Cards/Profile/support/saved-destination coverage.
New checks cover the exact registry and unresolved routes, malformed/equal card
recipients, amount units/overflow, IBAN checksum, selected source identity,
separate mode drafts, correction/Back/recreation, transient credentials,
independent receipts, unavailable actions, scenario retry and real IME clearance.

IT-QA includes replica PNG/XML captures under `qa/`: card form/review/keyboard,
long mixed-direction note with IME, account form/review, source/reason dialogs,
transfer menu, independent card/IBAN receipts, submission notice and error state.
Persian/RTL, scrolling, identifiers, dialogs and system bars were inspected on
the pinned API 35 fa-IR 1080×2400/density-420/font-1 emulator. No overlapping
controls or system-bar obstruction was found; content scrolls to controls with
the keyboard open. The crash buffer was empty. Exact original-app visual
comparison remains unscored. No warnings were suppressed or assertions removed.
Private output, captures/hierarchies and installation receipt stay under
`local/evidence/individual-transfers/`. Public CI uses substitutes. After green
CI and merge, clean main is rebuilt/reinstalled, displayed revision and installed
APK hash are checked, and the replica is left ready for review. Stop before FR-09.
