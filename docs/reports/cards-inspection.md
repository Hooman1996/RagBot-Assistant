# FR-03 — Cards tab, list and read-only details

Issue [#46](https://github.com/Hooman1996/RagBot-Assistant/issues/46), following FR-02 #44. This is a local frontend batch. No backend, database, assistant, bridge, original-app execution or banking network access was added.

## Implemented content and identity

The Home **کارت** tab is hosted content of `S-026` (CI-TAB), traced from the Home tab handler independently of the legacy card-list activity `S-125`. It has no graph ID of its own. It shows, top to bottom: a card pager for the selected card group (with a page indicator when there are several cards), a horizontal card-group selector, then that group's action rows. The group set, titles and rows are a deterministic **Designed for mock** configuration: in the original they are server menus whose content and order are unknown. Gift cards are configured with no cards, to show the recovered empty state. Wallet cards are not configured; their card journeys belong to FR-04.

The card face follows the recovered layout: debit-only default indicator, share, copy, bank logo, the number grouped by four and the owner. Only debit cards show the balance control, as in the original. Balance states are kept separate: hidden (the initial state), shown amount, shown zero, and missing (the recovered retry state). Copy copies the synthetic number and shows the recovered confirmation. Share is unavailable. No client-side card-number masking was found: numbers are grouped as received, and one fixture uses a server-style masked number to show it passes through unchanged.

Tab actions open only recovered destinations:

- `S-012` card information: own card picker for the incoming group, primary/other accounts, IBAN and status with copy. Share is unavailable. With owner approval this previously unassigned node moved from FR-45 to FR-03 (ID unchanged; catalogue name corrected).
- `S-125` legacy card list: number search; rows show owner, customer number, account, the list record's own balance and IBAN, with a lock marker. Recovered row taps have no action.
- `S-131` credit card detail and `S-134` file-loan summary: both read the **first** credit card, as the recovered code does, not the tab's current card. Credit limit, remaining credit, used amount, overdue-instalment debt and total settlement are separate fields. Pay and file details are unavailable.
- `S-119` bon card balance: recovered fields and validation. With owner approval, submitting explains that the secure inquiry is unavailable. No PIN is processed and no balance is shown.
- The credit-history row opens the existing FR-02 legacy history `S-091`, matching its recovered route.

Card issue, blocking and PIN rows explain that they are unavailable and keep the current tab, group and card.

Three FR-03 node groups are reached from menus the replica does not have yet: Profile (`S-126` card management → `S-068` fetch cards), app settings (`S-130` card display status) and the pre-login menu (`S-120` balance without login). With owner approval they have labelled **Designed for mock** review buttons on the demo entry, not claimed routes. In management, default, lock and delete are unavailable. In fetch-cards, selections are a local draft and confirming is unavailable, because adding cards is a remote change. The display-status switch is a remote preference and stays unchanged. `S-120` validates only, like `S-119`.

`S-123` card color is reached only inside the card issue/modify steps. With owner approval it moved to FR-04.

## Data and behavior

All cards, owners, numbers, accounts and amounts are synthetic. Money is `Long` with explicit `IRR`. Card balance, the legacy list's account balance, credit limit, remaining credit, used amount, overdue debt and total settlement are separate fixture fields; none is derived from another. The [requirements matrix](cards-requirements.md) records the caller-backed `M-026`, `M-024` and `M-008` roles, unresolved joins and future service proposals.

Selected group, each group's card position and shown balances survive Back, tab switches and recreation. This matches the recovered view model, which keeps its card data for the life of the Home screen and does not reset balances on tab re-entry. Page-local selections (card information picker, fetch-cards draft, searches, form fields) survive recreation. Scenario chips on the demo entry select populated, empty, loading and error states. Error has a local retry, which is **Designed for mock**; the original shows a general failure message.

## Assets and private evidence

CI-TAB, CI-STATE, CI-ROUTES, CI-DATA, CI-S012/S119/S120/S125/S126/S130/S131/S134 and CI-COVERAGE refer to focused static findings under `local/evidence/cards-inspection/`. CI-ASSETS adds 12 narrowly reviewed card inputs to the previous 64: 76 total, each with source/output hashes. Nothing was bulk-copied into public git. Public builds use system fonts and text symbols, plus a plain colour behind the card face, and remain buildable without private assets.

## Original-app fidelity

Original UI fidelity is **unscored**: there are no owner references for these screens. Static geometry, resources and handlers support the reconstruction. Our own captures are not originals, and functional tests do not establish a visual match. Known gaps:

- Server card colours are replaced by a fixed colour under the recovered card pattern.
- The card does not shrink on scroll; the original has normal and compact heights.
- The loading skeleton is replaced by a spinner.
- The balance progress state is not shown, because local balances are immediate.
- Refresh re-reads the unchanged fixture.
- Expiry is typed as month/year instead of using the original picker.
- Validation message texts and the file-loan empty illustration are not recovered.
- The S-131 repayment-amount field is omitted, because payment is outside FR-03.

## Acceptance

Baseline on main `49f8904`: repository checks 131/131, pytest 112, Ruff check/format pass, Android assemble, 6 JVM tests, lint 0 errors/89 warnings, 15/15 device tests.

Final acceptance on the branch: see the PR for the exact command output. Android: 10 JVM tests, 21/21 server device tests (0 skipped), lint 0 errors/107 warnings. The 18 new warnings are not suppressed. Twelve UnusedResources, one DiscouragedApi and one LocalContextResourcesRead come from the established name-based lookup of private resources. Two VectorPath come from original vectors. IconDensities and IconMissingDensityFolder occur because one density of each private bitmap is copied. Lint also found density-less private bitmaps; that real defect was fixed. The first device run failed (ambiguous text with the pager keeping an adjacent page composed, and unmerged row texts) and is retained privately.

Rendered review on emulator-5554 covers: the debit tab (hidden, shown, zero, missing), credit and gift groups, unavailable notices, every destination page, the numeric keyboard on the secure form, review entries, the error state and the return to Home. It found and fixed a washed-out card face, a reversed card number in an RTL info row and density-less bitmaps. There are no replica crash records. Captures, hierarchy dumps and PID-scoped logs are private under `local/evidence/cards-inspection/qa/`. CI runs build, unit tests and lint; device tests run on the server.

## Opening the batch and stopping point

Open **RagBot Replica → ورود به خانه → کارت**. Use the group chips and swipe cards; tap **مشاهده موجودی** on a debit card; open the action rows. The demo entry's **مدیریت کارت**, **نمایش کارت** and **موجودی بدون ورود** buttons open the review entries.

Only FR-03 is implemented. [FR-04, card lifecycle and wallet-linked card journeys](../plan/frontend-replication-steps.md#fr-04--card-lifecycle-and-wallet-linked-card-journeys) is the recommended next approval. It remains proposed.
