# FR-07 — Saved destinations and quick-action configuration

Approved issue [#58](https://github.com/Hooman1996/RagBot-Assistant/issues/58)
follows completed FR-06. The owner authorized the next batch; this implements
FR-07 only. FR-08 remains proposed and must not start automatically.

## Baseline and evidence

Fetched main was `bb2b62e01e4fe969f750ae84124bcf44e485f204`; no newer commits
or unrelated checkout changes were present. Baseline: 131 repository checks,
112 Python tests and both Ruff checks passed; Android build, 21 JVM tests and
lint passed (0 errors / 111 warnings). The first full baseline device run
passed 47/48 tests: the existing FR-06 OTP test asserted visibility before the
identity sheet's native window finished dismissal. The exact test passed on
rerun using the unchanged retained baseline APKs. Its assertion now waits for
Activity window focus before checking S-081; no assertion was removed.
Failed output, rerun output and original XML/lint results remain private.

SD-FAVORITES and SD-SHORTCUTS reference the bounded recovery ledgers, decoded
resources/callbacks and focused REA evidence under
`local/evidence/saved-destinations/`. Static recovery never ran the original
banking app or contacted its services. The
[requirements matrix](saved-destinations-requirements.md) separates recovered
behavior, synthetic preview state, future services and unresolved contracts.

## Implemented navigation and content

- Home **search** opens S-084 service search/pins through the recovered S-083
  host relationship. Home **bookmark** independently targets recurring transfers
  S-199, outside FR-07, and remains unavailable. No server menu key is inferred
  from the fixture label.
- S-084 shows pinned services for an empty query, local search results otherwise,
  per-row pin/unpin, and separate no-pins/no-results presentation. Synthetic
  service rows show unavailable because the dynamic menu registry is absent.
  No shortcut drag or reorder UI is invented.
- S-066 is separate legacy destination management: account/IBAN/card/wallet
  segments, conditional search, swipe-revealed edit/delete, local editor,
  delete confirmation and long-press vertical reorder with Apply/Cancel.
  Equivalent accessibility actions reveal controls and move rows.
- S-197 and S-198 are explicitly **loan-only**, rather than every kind in the
  M-010 enum. They provide search, swipe/delete and confirmation. S-197's edit
  callback is a recovered no-op; the replica explains that editing is unavailable.
  No add-loan form is invented. Selecting S-198 returns the selected identity.
- S-083 and S-196 are hosts, not additional visible pages. Only S-066, S-084,
  S-197 and S-198 are added to the existing implemented-content registry.
  Graph IDs, edges and six FR-07 allocations are unchanged.

No incoming current app handler was proven for S-066/S-197/S-198 within the
bounded recovery. They use clearly labelled independent demo entries outside
replicated content. The S-198 entry has a labelled local review form, note and
selected-result display to demonstrate return/cancel semantics without building
a transfer form or inventing a graph target. Original caller job context and
full M-010 result are documented for future integration.

All records and shortcuts are **Designed for mock**. Shared local record/pin
changes survive Back/recreation. Filters, list positions, non-secret editor
text and unapplied order changes are saveable; cancelling confirmations or order
editing does not mutate records. Preview controls remain on the demo page.
No network permission, arbitrary URL/file launch, service or bank mutation is
introduced. Existing Home, Accounts, Cards, Profile and FR-06 behavior remains
covered by the complete regression suite.

## Validation and fidelity limits

The legacy account field checks 13 ASCII digits; wallet checks 16 characters.
The original account flow also requires owner inquiry. The local editor
explicitly discloses unavailable bank lookup/service validation and permits
only synthetic local saves. Card/IBAN precise checks, selectors and inquiry
prerequisites remain unresolved; those previews require nonempty name/value.
Duplicate creation is recovered, but exact cross-type scope is Unknown; our
local duplicate check is deliberately conservative. The original description
fallback and server ordering are not claimed as replicated.

The generic legacy editor approximates the recovered sheets; original bank
logos, drag affordance geometry, inquiry/owner presentation and type-specific
selector fidelity remain incomplete. Search additionally matches synthetic
identifiers, while current loan recovery proves display-name matching. Generic
loading/error/retry wording and persistence are preview designs. Service errors,
remote concurrency/eligibility, capacity and exact encrypted inner contracts
remain Unknown. No pagination was recovered, so none is invented.

SD-ASSETS verifies the pinned original APK, previous 80 source/output hashes,
four new caller-backed shortcut vectors and their gradient dependencies. The
final private overlay has 84 prepared resources, with provenance and adaptation
hashes under `local/evidence/saved-destinations/`. Public builds retain the
existing substitute disclosure. Original-app visual fidelity remains
**unscored**; replica captures are inspection evidence, not original goldens.
FR-05 and FR-06 fidelity gaps are unchanged.

## Opening the screens

| Surface | Open in the replica |
|---|---|
| S-084 service search / pins | **ورود به خانه**, then Home's search icon **جستجو**; type a service label to inspect pin/unpin |
| S-066 legacy destinations | Scroll the demo page to **مقصدهای ذخیره‌شده قدیمی**; swipe a row to reveal edit/delete; **ویرایش ترتیب** enables long-press drag |
| S-197 loan destinations | Demo **مدیریت مقصد تسهیلات**; swipe a row for controls |
| S-198 loan picker | Demo **انتخاب مقصد تسهیلات — بازبینی مستقل**; choose a row to return a local result, or Back to cancel |

The labelled review note/result are outside replicated content. The existing
loopback viewer and SSH forwarding remain in the
[replica runbook](../runbook/replica-development.md#desktop-viewer-and-stopping-the-simulation).

## Verification and delivery

SD-QA includes replica PNGs and accessibility hierarchies under
`local/evidence/saved-destinations/qa/`: shortcut pins/search/no-results/empty/
loading, legacy top/swipe/editor/delete/order/long row, loan list/delete/error/
retry, picker and returned identity. The pinned device remains API 35,
fa-IR, 1080×2400, density 420 and font scale 1.0. Mixed-direction identifiers,
Persian text, scroll clipping at viewport edges, system bars, dialogs and
keyboard clearance were inspected. No replica crash records were found.

Rendered review caught editor controls behind the real keyboard despite the
first 56-test green run. The editor now opens fully expanded and applies IME
insets inside its scrollable content. A new device test waits for the actual
IME and checks Save/Cancel bounds against its top edge. The corrected capture
`legacy-editor-keyboard-fixed.png` confirms clearance; failed captures and
failed-run output remain private. The illustration's own caption is not drawn
twice; public substitutes retain visible text and private images have an
accessibility description.

The existing JUnit4/Compose testing stack and verified dependency cache are
reused. Final server acceptance: **131 repository checks, 112 Python tests,
Ruff check/format, assemble, 26 JVM tests and 57 device tests passed**, with zero
failures/errors/skips. Lint: **0 errors / 117 warnings**, six above the 111
baseline. Four are UnusedResources diagnostics for runtime-resolved shortcut
vectors; two are VectorPath warnings for the original long placeholder paths.
Their use is verified by rendered captures and no warnings are suppressed.

The final suite includes route/registry boundaries, filtering, local add/edit/
delete/cancel, stable IDs after edits/deletions, real drag and accessible reorder,
Apply/Cancel/recreation, picker identity and originating review draft, pin state,
empty/loading/error/retry, unsupported routing/editing, actual IME clearance and
all previous Home/Accounts/Cards/Profile/FR-06 regressions. Failed baseline,
intermediate and pre-fix visual evidence is retained separately from the final
acceptance artifacts.

Public CI builds with substitutes; its result is recorded in the PR before
merge. After merge the clean main build is reinstalled and its displayed revision
and installed APK SHA-256 are checked against the built artifact. The receipt
is `local/evidence/saved-destinations/merged-install-verification.json`.
Only FR-07 is delivered; **FR-08 — Individual card/account transfers** remains
proposed and requires explicit approval.
