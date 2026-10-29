# FR-02 — Accounts, statements and history inspection

Issue [#44](https://github.com/Hooman1996/RagBot-Assistant/issues/44), following populated Home #42. This is a local frontend batch. No backend, database, assistant, bridge, original-app execution or banking network access was added.

## Implemented content and identity

The Home **حساب** tab is embedded `S-026`, with a recovered account header, default indicator, synthetic owner/customer/account/IBAN fields, local copy, balance show/hide and a conditional account-list control. It is distinct from the selectable-account activity `S-085`. The two configured account action rows are a deterministic **Designed for mock** fixture: statement and management. Original server menu ordering is unknown.

- `S-085`: source-account selection/search and ownership groups when applicable; selection returns to the tab and hides balance.
- `S-063` / `S-067`: management inspection and candidate selection, ownership/search, Apply/Cancel. Apply changes only local visibility; cancellation discards the draft. Default/delete/authenticated mutations remain unavailable.
- `S-016` / `S-014`: bank statement date range, local paging/refresh, signed amount/remaining balance/narrative rows and selected line details. These existing IDs were previously unassigned. The owner approved their recovery/allocation correction; IDs and graph edges are preserved.
- `S-086`: separate export-request form, account/format/date selection and validation. Request/file-list controls explain that no export service is implemented; no request success, pending count or generated file is fabricated.
- `S-091` / `S-090`: legacy transaction-history search, finite local pages and selected detail. Its recovered filter control stays disabled. Receipt/trace actions explain their unavailable state.

The current Home history menu targets a different unresolved hub. It remains unavailable rather than opening the wrong legacy screen. The demo entry offers clearly labelled independent review access to legacy history and export; these are not recovered Home transitions. Existing Home recent rows remain notice-only, without fabricated detail navigation.

## Data and behavior

All identifiers, records, balances and transaction parties are synthetic. Money is `Long` with explicit `IRR`, including negative statement amounts and values above floating-point precision. Missing balance remains unknown rather than zero. Statement running balances agree with the selected fixture account when its balance is known. The [requirements matrix](accounts-requirements.md) records the caller-supported `E-030`, `E-041`, `E-019` joins, model roles and future service proposals; unresolved wrappers/joins remain Unknown.

Display selection, applied range/query, paging and Home scroll survive supported navigation/recreation. Temporary candidate edits are discarded on Cancel/Back. Retaining the displayed account across tab re-entry is deliberately **Designed for mock**; recovered code may reselect the default account. The default star is an indicator, not a toggle.

Persian date validation uses the platform ICU calendar (API 24+) with strict parsing and full-input checks: [Calendar](https://developer.android.com/reference/android/icu/util/Calendar), [SimpleDateFormat](https://developer.android.com/reference/android/icu/text/SimpleDateFormat). The replica calendar grid and keyboard alternative are our presentation, not a claimed recovery of the original picker visuals. List range validation preserves the recovered approximate 365-day calculation despite its 366-day label and rejects spans of 90 days or more. The separate export form has its own required/date-order/future checks and format order.

## Assets and private evidence

AI-TAB, AI-DATA, AI-S063/067/085/086/090/091 and AI-STATEMENT refer to focused static findings under `local/evidence/accounts-inspection/`. AI-ASSETS adds 13 narrowly reviewed private inputs to the previous 51: 64 total, each with source/output hashes and provenance. The required local preparation script and manifest stay private. Nothing was bulk-copied into public git. Public builds retain disclosed system-font/symbol substitutes and remain buildable without these assets.

Original UI fidelity is **unscored**: there are no owner references for these new states. Static geometry, resources and handlers support the reconstruction; functional tests and our own captures do not establish an original-app match. Known comparison gaps are dynamic menu configuration, custom calendar visuals, the dark-green preview balance-action tint used for readability, exact shared toolbar/row styling and category-specific history/receipt details. Pagination uses explicit preview “show more” controls for finite local data. Management permissions, inactive/locked combinations, authenticated changes and service outcomes require later evidence/approved journeys; they are not claimed complete.

## Acceptance

Final acceptance passes: repository checks 131/131; pytest 112; Ruff check/format; Android assemble, 6 JVM tests, lint (0 errors, 89 warnings) and 15/15 server device tests (0 skipped). The exact Android command completed successfully in 1m33s on emulator-5554 (`acceptance-complete.log`). Warnings are retained, chiefly dynamic private-resource/unused-resource and existing tooling warnings; none is suppressed. CI must also pass before merge.

Rendered review covers account tab/picker/management/candidates, statement list/detail, legacy history/detail, export/calendar/keyboard and unavailable notices. It found and repaired child-page status contrast, RTL Back and inactive Home selection styling, as well as missing picker fields and default Material selection tint. Final captures use `qa-final-*.png` with matching hierarchy dumps. No clipping, overlap or replica crashes are accepted; crash/PID evidence is private. Baseline: repository checks 131 passed, pytest 112 passed, Ruff passed; Android 4 JVM / 8 device tests passed after the stopped emulator finished booting. The initial offline-device attempt is retained as failed evidence. Subsequent implementation failures and repairs are retained privately, including a real cancellation-draft restoration bug.

Device captures, hierarchy dumps, PID-scoped logs and acceptance output remain private under `local/evidence/accounts-inspection/`. No original screenshots or resources are published. CI runs applicable Android build/unit/lint checks; server instrumentation is independently required.

## Opening the batch and stopping point

Open **RagBot Replica → ورود به خانه → حساب**. Choose **لیست و تغییر حساب**, **صورتحساب** or **مدیریت حساب**. The demo's independent review buttons open legacy history/export. Scenario controls are outside replicated content and select deterministic populated/empty/loading/error states.

Only FR-02 is implemented here. [FR-03, cards tab/list/read-only details](../plan/frontend-replication-steps.md#fr-03--cards-tab-list-and-read-only-details) is the recommended next approval because it reuses the verified shell/account selection while requiring its own tab identity recovery. It remains proposed.
