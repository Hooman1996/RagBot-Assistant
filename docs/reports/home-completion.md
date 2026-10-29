# Populated Home completion

Approved issue [#42](https://github.com/Hooman1996/RagBot-Assistant/issues/42),
29 October 2026. Fetch found no main work after `3d927b3`; the existing foundation
was reused. D-020 is unchanged. This report covers Home only. The canonical
[frontend replication steps](../plan/frontend-replication-steps.md) orders the
remaining work; none of those proposed batches is implemented or approved here.

## Implemented Home

Home (`S-026`) and its embedded menu (`F-066`) remain the only registered graph
content. Compose renders the five top items, five quick actions and sixteen
services in the owner reference's RTL reading order. The deterministic local
configuration is `HomeFixture`; its keys identify our presentation fixture,
not recovered banking menu IDs. All financial identifiers and transaction
values are synthetic. Amounts are `Long` integers with explicit `IRR` units.

The fixed header has support/message and notification controls, the visible
notification marker, green hidden-balance/increase-balance styling, account
information on the physical left and increase balance on the right. Private
patterns have explicit recovered dimensions and begin below the status inset.
The content panel scrolls independently beneath the header; the bottom bar
occupies separate layout space. Services, layout editing and both synthetic
recent-history rows are reachable with bottom clearance.

Balance visibility and account expansion are local state. The recovered menu
editor has four columns, drag reordering, a changed-order Apply button and
Cancel. Only the service group changes. A cancelled draft leaves Home untouched;
applied order and Home state survive activity recreation. Local persistence
is **Designed for mock**, not a recovered banking-save result. Accessibility
move-before/move-after actions provide our alternative to dragging. Editor
tile taps also explain unavailability without discarding the draft.

All unimplemented destinations—including menu features, toolbar/tab/QR actions,
funding and transaction detail—show a Persian notice. Dismissal retains Home,
scroll offset and local state. No external launch, scanner, successful banking
navigation, authentication, eligibility decision or transaction occurs. Back
closes a notice/editor before collapsing account details or returning to demo.
The replica still has no INTERNET permission, backend, database, bridge or assistant.

## Evidence and assets

Two owner-provided original images show different scroll positions of one Home.
They supplement recovered resources, state and caller evidence; the earlier
saved replica capture supplies the third comparison image. HC-REF records exact
private input hashes. Original-app execution and production contact were not used.

HC-ASSETS records 51 reviewed private inputs: the earlier 18 foundation inputs,
31 additional packaged drawables, one recovered heading string and one isolated
banner illustration from the owner reference. The string retains the full
reference heading only in private resources. The public fallback omits the
identifying suffix and discloses visual substitutes on the demo entry.

The banner is a real Compose gradient/text/indicator component. Its decorative
chart is a tightly isolated private visual-reference extraction, with source
image hash, crop rectangle and output hash. It contains no owner account/history
values and is not the whole screenshot used as the interface. The actual
server-supplied artwork payload was not recovered. No other slides are invented;
nine visual markers reproduce the reference, without asserting an unproven slide
ordinal, swipe result or automatic advance.

`prepare_assets.py` under the private batch root checks the original artifact,
reviewed source resources, reused assets and reference image hashes, then verifies
output hashes. Assets, mappings, REA output, references and comparison captures
stay ignored under `local/`. There are no packaged original assets in public git.
Public builds remain functional with disclosed system-font/text-symbol substitutes.

HC-EDIT establishes drag/draft/Apply/Cancel from handlers, rather than from the
edit label. HC-DATA supports selected account `M-004` and balance `M-023`; exact
Home endpoint joins remain Unknown. The per-control
[Home requirements matrix](home-requirements.md) separates menu display effort
from complete journeys and proposes future records/services without inventing
contracts or bank database tables.

## Acceptance and comparison

The exact Android acceptance command passed on the pinned server emulator:

```text
./android/gradlew -p android --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
BUILD SUCCESSFUL in 1m 5s
85 actionable tasks: 17 executed, 68 up-to-date
```

There are **4 JVM tests** and **8 instrumentation tests**, with zero failures,
errors or skips. Coverage includes fixture membership/grouping/RTL order, exact
integer formatting, implemented semantic IDs, history reachability and bottom
clearance, unavailable controls, physical position and saved scroll restoration,
balance/expansion/Back/recreation, and real editor drag/Apply/Cancel. A recreation
diagnostic confirmed that Compose root coordinates can change while physical
screen position and scroll offset remain identical; acceptance checks the latter.

Lint reports **0 errors / 70 warnings**, with no warning suppression. These are
primarily dynamic private-resource lookups/unused-resource analysis, densityless
private bitmaps, two long original vector paths, resource reads, and existing
launcher-icon/backup/newer-plugin guidance. No new dependency or CI job was needed:
the existing Android CI already builds and runs unit/lint checks for this code.

Rendered QA inspects top, lower services, history, notice, editor and expanded
account captures. Labels fit in the pinned portrait configuration, the editor
footer is reachable, both history rows clear the bottom bar, and the replica's
PID-scoped crash log is empty. An editor status-icon contrast defect discovered
in QA was fixed and the full device suite rerun. Original-app fidelity remains
unscored; these captures do not prove an exact match.

Repository acceptance: **131 project checks**, **112 pytest tests**, Ruff lint
and format all pass. Final post-change output is retained in the private batch
logs. Earlier failures and the interrupted/no-device attempt remain recorded;
none is represented as a passing run.

Technical behavior and original-app fidelity are separate. The owner references
support this particular Home configuration and visible states. They do not prove
other customers' configuration, unseen banner slides, expanded/revealed-balance
geometry, every transaction type, eligibility, destination joins or remote layout
persistence. Public substitutes, synthetic values, the private chart extraction,
Android system-bar differences and a faint seam around the extracted chart are explicit limits. The visible banner gradient colors are approximated from the supplied reference; the original server configuration is Unknown.
No exact-similarity claim, numerical score or replica-derived original-app golden
is made.

Private evidence root: `local/evidence/home-completion/`. HC-QA includes `acceptance-status-contrast.log`, `qa-results.json`, `qa-top.png`,
`qa-lower.png`, `qa-history.png`, `qa-unavailable.png`, `qa-editor.png`,
`qa-expanded.png`, matching UI hierarchies and PID logs. HC-PLAN records the
coverage audit for all 51 feature families, 71 inventory rows and 485 graph nodes.
The remaining 44 batches are proposed only. FR-02 accounts/statement/history is
recommended next, with actual Home account-tab identity recovered before binding
it to legacy activities. Further work waits for explicit owner approval.
