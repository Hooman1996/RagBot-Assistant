# Accounts and history inspection requirements (FR-02)

Approved issue [#44](https://github.com/Hooman1996/RagBot-Assistant/issues/44).
This report separates recovered frontend requirements, implemented local preview
behavior and future service proposals. It does not define a banking API or bank
database, and it does not authorize another frontend batch.

## Identity and evidence boundaries

The original six allocated destinations are account management `S-063`, account fetching
`S-067`, legacy account selection `S-085`, statement export requests `S-086`,
transaction information `S-090` and transaction history `S-091`. The hosted
account tab is content of `S-026`; it is **not** the legacy `S-085` activity. A
simple account inspection state may remain embedded in that tab without a new
graph ID.

**Recovered:** account records and source-account rows are different models;
account statement export requests and generic application transaction history
are different flows. History list/detail are not interchangeable with bank
statement lines. Focused recovery identified the bank-statement list/detail as existing `S-016`/`S-014`. The owner explicitly approved including them in FR-02 and correcting their catalogue/step allocation. They remain separate from `S-091`/`S-090`; no IDs or contracts were replaced. The batch now has eight implemented activity destinations plus the account tab embedded in Home.

Private evidence IDs AI-DATA, AI-S063, AI-S067, AI-S085, AI-TAB, AI-S086,
AI-S090 and AI-S091 identify the relevant model/caller chains in
`local/evidence/accounts-inspection/requirements-evidence.md` and the focused
REA records beside it. Layout and caller recovery by the companion
investigations supplements that ledger. The original app was not executed and
no original service was contacted. This report contains semantic IDs and
sanitized requirements only.

**Designed for mock:** fixture values, preview error/loading scenarios and any
local selection persistence are our designs. They are not evidence of a bank's
eligibility rules, server records or successful account activation. Data is
bundled in the app, with no backend service/database. Future services below are
proposals only. Technical acceptance and comparison with the original UI must
be recorded separately in the batch's implementation report.

## Model and amount requirements

| Existing model | Caller-supported role in this batch | Recovered data/type limits | Required replica representation |
|---|---|---|---|
| `M-004` account | Management/candidate-fetch records, `S-063`/`S-067`; Home selected-account context | Account number, IBAN, currency code/description, account/deposit/ownership/status descriptions, branch details and active/default/permission flags. This model has no balance field. Some flags are nullable in the recovered type. | Synthetic identifiers and descriptions; preserve distinct active/default/allowed flags. Do not invent an account balance from these flags. |
| `M-009` source account | Hosted account tab and legacy selection rows | Optional balance, account number/description, currency, IBAN, ownership, permissions, active/default-related flags. Original balance uses a nullable floating-point type. | Exact integer amount plus explicit unit when present; an absent/hidden balance is distinct from zero. No floating-point money. |
| `M-023` balance | Separate balance-response shape; relevant to account-tab display | Available, actual, ledger and pledged balances; currency and date. Original monetary fields use floating point. The precise new tab's response/endpoint join still needs corroboration. | Exact integer amounts and explicit currency/unit; local hidden/loading/error/ready states remain separate from the account record. |
| `M-038` source-account-list wrapper | `S-085` list request | Focused recovery finds a list of `M-009` rows plus a total balance. The existing API map's field summary repeats row fields rather than describing that wrapper. Identity is preserved; that summary is not treated as a complete schema. | Local list plus optional exact integer total/unit. Future map repair must be explicit rather than a silent meaning change. |
| `M-039` statement line | Caller-supported bank-statement list/detail `S-016`/`S-014`; **not** bound to `S-086`, `S-090` or `S-091` | Amount, actual/previous balances, debit/credit description, dates/times, narrative, references, parties and currency. Original amount/actual balance use floating point; previous balance is an integer. | Use exact integer amounts/unit in the local fixtures; preserve debit/credit separately, not a guessed sign rule. |
| No assigned M ID: history records/page | `S-091` list and `S-090` selected item | History item has a nullable integer amount, type/outcome, identifiers, trace/reference, date, source/destination and type-specific optional fields. Page has start/end row, total rows and list. No currency field was established on this history-item model. | Local record/page types; explicit `IRR` fixture unit is Designed for mock. Keep missing amount distinct from zero; do not reuse `M-039`. |
| No assigned M ID: statement request/result | `S-086` export request | Account number, format, start/end dates; request-result rows add row count and download status. These shapes occur in a service whose internal naming is misleading; actual payload and UI establish their statement purpose. | Synthetic request/result records; account/date/format validation. No real request, download or external file launch. |

All money in the replica must use exact integers with an explicit unit, such as
integer rial with `IRR`. The original app's floating-point field choices do not
override repository rules. Converting an eventual remote decimal response into
that representation needs an explicit adapter contract; this frontend batch
must not guess rounding, currency scale or sign conventions.

## Per-destination requirements

Preliminary effort is for frontend inspection only, excluding backend mocks,
real authentication, transaction operations and the assistant. Confidence
separates identity/data recovery from exact visuals and unresolved contracts.

| Existing node / family | Purpose; user inputs | Required data and local fixture source | Prerequisites and navigation result | Supported API/model references | Proposed future mock records/services | Unknowns; effort/confidence |
|---|---|---|---|---|---|---|
| Account tab embedded in `S-026`; `F-028` | Inspect selected account, reveal/hide balance, choose an account or a recovered menu action | Local synthetic `M-009`-shaped rows, selected-row key, separate balance/visibility state and menu configuration | Home account-tab handler opens this hosted content. List-account control is present when multiple accounts exist and invokes `S-085`; supported selection returns to tab. Unsupported actions explain their limit. | `M-009` caller-supported; precise modern tab E-ID Unknown. `M-023` is a separate balance candidate rather than assumed on every account row. | Synthetic account overview, balance reads and verified per-account menu configuration | Original tab re-entry may choose a default account again; replica selection persistence must be labelled Designed for mock. No new tab node ID. 1–2 days / high identity, medium state/visuals |
| `S-063`; `F-021`/`F-028` | Inspect account management; search and ownership grouping where recovered; request default/lock/delete actions | Synthetic `M-004` management records and local selection/search state | Requires account collection; supported entry opens management. Fetch/add entry passes candidate accounts to `S-067`. Mutating controls remain preview-only notices or separately labelled local state, never banking success. | Caller → account-management read → `E-030`, typed `M-004` list. Other action endpoint joins not assumed. | Management-account read, ownership descriptions and explicit active/default/allowed scenario records; later mutation proposals require approval | Authentication/password/biometric and account-changing effects are outside read-only inspection; no inferred eligibility. 1–2 days / high record/read identity, medium interaction coverage |
| `S-067`; `F-021`/`F-028` | Inspect fetched candidate accounts; search/filter/select candidates; apply or cancel | Candidate `M-004` collection supplied by caller; local selected account keys and recovered filter groups | Enter from management with candidate rows. Cancel returns without changes. Apply must disclose preview-only behavior; no real account is activated. | Save caller is corroborated with `E-019`, encrypted wrapper `M-002`; selected account numbers enter that wrapper. Candidate retrieval exact E-ID Unknown. | Candidate-account read and fixture-only selection result; later activation service design separate from preview | Encrypted wrapper internals and activation validation/error contracts remain Unknown. Do not equate selecting a row with an activated account. 1–2 days / high data/caller purpose, medium filters/visuals |
| `S-085`; `F-028` | Select a source account; no new account-detail page is inferred from a row tap | Synthetic `M-038` wrapper containing `M-009` rows and optional total; picker shows account number, balance, owner/customer/category/type/IBAN with current selection | Called by the account tab's list control when applicable. Active row returns selected account to caller; inactive row reports unavailable/locked and does not navigate to details. | Corroborated caller chain to `E-041` and `M-038`/`M-009` | Source-account list/total read with empty/loading/error and active/inactive/default scenarios | Full caller modes and account permissions are not inferred from account descriptions. 1 day / high selection identity/data, medium visual confidence |
| `S-016`/`S-014`; `F-029` | Inspect bank statement lines and selected details; start/end Persian dates, apply/refresh/page, tap a line | Synthetic signed integer IRR amounts, running balances consistent with selected fixture account, date/time/narrative; detail branch/reference/party/document fields are labelled demonstration values | Verified account-menu statement handler → `S-016`; selected row → `S-014`. Back preserves applied dates/list position; invalid draft does not replace the valid results. | Caller passes the account plus date/page request and typed `M-039` lines; precise E-ID remains Unknown (`E-471` is only a candidate) | Account/date/page statement read and synthetic immutable line-detail records; no payment write or database schema inferred | Original calendar visuals and live paging unknown. Source rejects approximate age >365 despite a 366-day label and spans ≥90 days; this discrepancy is retained and tested. 1–2 days / high identity/fields, medium visuals |
| `S-086`; `F-029` | Request statement export; select account, format and start/end dates; request file-list inspection | Local account choices, formats and dates; request-result status/row count are future requirements, not invented completed requests | Account/format/nonempty dates required; reversed/future dates have recovered validation states. Request/cancel remain local; no real export request/download. Separate from history list/detail. | Typed account/date/format request/result caller shapes recovered; no assigned M IDs or precise E-ID join proved. `M-039` does not establish this form's contract. | Synthetic export-request list/status and preview-only validation result; later read/request/download services need separate contracts | Exact encrypted export interface, processing times, file availability and service limits remain Unknown. 1–2 days / high form purpose, medium full status/visual coverage |
| `S-091`; `F-030` | Inspect transaction history; search amount or destination; select a row and page | Synthetic history items/page metadata, current transaction category and search/page state | Category-specific history entry; populated row opens `S-090` with selected item/type and row context; Back restores list/query. Recovered filter control is disabled, so no invented filtering panel. | Caller parses history item/page shapes, not `M-039`; existing E-ID remains Unknown across encrypted category dispatch | History read with deterministic pages and category/outcome/empty/error scenarios; search over local synthetic items | Search normalization and exact server paging/filter contract require verification; not assumed to be an account-filtered ledger. 1–2 days / high list/detail identity, medium category/paging coverage |
| `S-090`; `F-030` | Inspect selected history record and trace/status; attachment/share controls are requests for unavailable behavior | Same synthetic history record selected in `S-091`, plus explicit type-specific detail/status fields | Requires a selected item/type. Back restores originating history state. Unimplemented trace/download/share/external actions remain honest notices; no payment is retried or submitted. | Actual caller dispatches by transaction category to several services; no single universal E/M contract established. Do not borrow a similarly named trace endpoint. | Per-type synthetic detail/trace reads and sanitized attachment metadata, if later approved | Optional fields, status/trace transitions and category-specific receipts differ; not all journeys belong to FR-02. 1–2 days / high selected-item identity, medium per-type details |

The API joins above are based on caller → helper → typed service method evidence,
with matching HTTP parameter/response shapes that uniquely identify the named
inventory entry. They are not joins inferred only from similar feature names.
None is called by this implementation. Paths, hosts, original method/class names
and raw request material remain private. Where signatures collide, the E-ID is
explicitly Unknown even if a catalogue label seems plausible.

## Input validation and local state

- Account selections reference a fixture key, never a financial identifier as a
  navigation route. Active/default/ownership/permission fields remain distinct.
- Search is local to the visible fixture collection. Its normalization and
  matching must be documented as preview design unless recovered exactly.
- `S-086` has recovered missing-account, missing-format, missing-start/end,
  reversed-range and future-date errors. Its format order is spreadsheet,
  CSV and PDF. Calendar bounds and today must use one explicit local scenario;
  successful validation is not a successful bank request or generated file.
- `S-091` pagination retains selected category/query/list state through detail
  navigation and Back. It does not gain a date-range/amount filter panel merely
  because another statement flow has those fields.
- Hidden, absent, loading, failed and zero balances are separate states. Local
  error/retry controls select a deterministic fixture state, not a network call.
- Back/recreation must preserve preview selection, filters, detail origin and
  Home position as required by acceptance. If original tab re-entry resets to a
  default account, deliberate preview persistence must remain labelled as our
  design rather than a recovered original rule.

## Pending evidence and scope decisions

The owner approved `S-016`/`S-014` inclusion during implementation. The catalogue now identifies those nodes as bank statement list/details, preserving the existing list → detail edge and IDs. No new graph edges are inferred from the preview menu configuration.

The current Home history menu reaches a different, still unimplemented hub; it does not open legacy `S-091`. The legacy history and separate `S-086` export form have explicit **Designed for mock** review entries outside replicated content. `S-091`'s filter stays disabled. Account menus use a deterministic two-row fixture (statement and management), not a claim about server ordering.

Display selection is retained across tab re-entry in this preview, deliberately differing from the recovered default-account reselection. Candidate Apply updates only visible local account keys, while Cancel discards its draft. Account deletion/default/locking, external sharing, more-menu destinations, receipt/trace, request submission and download results are unavailable. The preview does not claim those complete journeys. Group labels are recovered; search normalization, finite page size/“show more”, calendar grid/keyboard alternative and local retention are preview designs.

Exact remote contracts, encrypted envelopes, user eligibility, production
behavior, remote persistence and original-app visual fidelity remain outside
what local fixtures prove. A proposed record/service is our test design, not a
recovered banking database table. No later replication step is started by this
requirements assessment.
