# Home display and journey requirements

Approved Home completion batch [#42](https://github.com/Hooman1996/RagBot-Assistant/issues/42).
This assessment covers the configured Home shown in the two owner references,
not every customer's menu or every feature journey. Implementation and test
results are recorded in the Home completion report. The remaining frontend
batches are ordered in [frontend replication steps](../plan/frontend-replication-steps.md).

## Evidence and interpretation

**Observed:** the owner supplied upper and lower views of the same Home at two
scroll positions. They establish the visible labels, grouping, RTL order,
notification/support visibility and one financial-performance banner. They do
not establish what a tile does after it is tapped, unseen slides or eligibility.

**Recovered:** the Home implementation has separate fixed, quick-access and
service lists; a scrolling content panel under the account header; recent
transactions; and four bottom tabs with a central QR control. The configuration
and records are supplied through application state. The layout editor has an
actual reorderable grid, draft order, Apply and Cancel. These are source-backed
behaviors, rather than conclusions drawn from the edit label.

**Designed for mock:** one deterministic local configuration reproduces the
owner's visible menu arrangement. Account and history records are synthetic.
The editor applies locally; it does not save to a banking service. Unimplemented
controls show a Persian unavailable notice and preserve Home selection, account
state and scroll position. A rendered tile is not an implemented journey.

**Unknown:** exact tile-to-route joins, encrypted wrappers, endpoint contracts,
user-specific visibility rules and unseen banner content remain unresolved
unless stated below. The current graph's `F-066` dynamic edge still has an
unresolved target. No destination IDs are invented, and no new screens are
registered by this report.

Private evidence IDs are HC-VISUAL (owner references), HC-SHELL (frame/state),
HC-MENU (menu rendering), HC-EDIT (editor and callbacks), HC-DATA (caller/data
trace) and HC-ID (catalogue cross-check). Their locations, hashes and focused
REA results are recorded in the private HC-REQ ledger under
`local/evidence/home-completion/`. No original source, resources, personal values
or identifying mappings are included here. Existing public inventories remain
[features](../../app_profiles/feature-inventory.md),
[screens](../../app_profiles/screen-catalogue.md),
[navigation](../../app_profiles/navigation-graph.json) and
[API/models](../../app_profiles/api-structure.md).

## Fixture and effort conventions

All menu entries require only their title, group, ordinal, reviewed icon key and
local availability state to render. They have **no text or financial user input
on Home**; the input is a tap. `T1`–`T5`, `Q1`–`Q5` and `R1.1`–`R4.4` below
identify fixture positions read **right to left**, not new graph IDs. The fixture lives in
[`HomeFixture.kt`](../../android/app/src/main/java/org/ragbot/replica/HomeFixture.kt)
as bundled local state, never a mock backend. Runtime configuration for other
customers remains Unknown.

Every row below inherits the same current precondition/result: enter the
replica-only demo, tap the visible item, receive the unavailable notice, dismiss
it and remain on the same Home state/scroll offset. The “journey” column describes
later assessment needs and candidate catalogue entries; it does **not** assert a
verified Home transition, implement a destination or authorize later work.

No exact `E-###` join is established for these menu controls by a corroborated
caller chain. Their **API/model join is Unknown**, including when the API
inventory has a similarly named endpoint. Menu/configuration shapes have no
assigned `M-###` in the current map. Thus the absence of an E/M ID in a row is
explicitly Unknown, not evidence that the future feature needs no service.

Proposed mock records/services are **our designs**, not recovered bank tables.
Their purpose is to identify future data work without defining a contract now.
Estimates are preliminary person-days: display **S** ≤1 day using shared menu
components; complete journey **M** 2–4, **L** 5–10, **XL** >10; **U** cannot yet be
estimated. They exclude backend/assistant/bridge implementation. Display
confidence is high for visible labels/order, and lower for private icon matching
or unobserved states. Journey confidence below reflects route/requirements
recovery, independently of display confidence.

## Top group

Display source for every row: local configuration at the listed `T` position;
HC-VISUAL/HC-MENU. Each is display S / high confidence. Journey prerequisites,
inputs and records are not needed to paint the menu.

| Item / fixture | Existing family and candidate nodes | Purpose; later inputs/prerequisites to recover | Future journey and proposed mock data | Unknowns; journey effort/confidence |
|---|---|---|---|---|
| کیف پول / T1 | `F-032`; wallet main `S-111` exists | Wallet overview; active wallet/session and selection rules Unknown | Wallet list/summary read service; synthetic wallet and integer balance/unit records | Home entry binding, wallet choice and eligibility Unknown; L / medium |
| باشگاه مشتریان / T2 | `F-057`; host `S-433`, home `S-432` exist | Rewards overview; membership/activation and points prerequisites Unknown | Synthetic membership, integer points, rewards and history reads | Home target and membership branches Unknown; L / medium |
| پشتیبانی / T3 | `F-026`; `S-076`–`S-082` contain questions, FAQ and tickets | Help entry; which help mode opens and session requirements Unknown | Synthetic FAQ, ticket list/thread; future support reads and local draft inputs | Tile is not proven to select FAQ, chatbot or ticket list; L / medium |
| سرمایه‌گذاری / T4 | Related `F-052`/`F-053` exist; no exact tile family/node binding | Investment entry; account, product, agreements and eligibility Unknown | Proposed synthetic product/fund summaries and investment entry configuration | Do not equate tile to brokerage `S-383` or crowdfunding `S-395` by name; XL / low |
| عملکرد مالی / T5 | `F-058`; host `S-437`, main `S-436` exist | Financial analysis; account/time-range/filter inputs and permissions Unknown | Synthetic categorized integer-amount summaries, budget/story reads | Home target and categorization/period contract Unknown; L / medium |

## Quick actions

Display source for every row: local configuration at `Q` position;
HC-VISUAL/HC-MENU. Each is display S / high confidence. The inputs below are
future recovery topics, not enabled Home forms or recovered validation rules.

| Item / fixture | Existing family and candidate nodes | Purpose; later inputs/prerequisites to recover | Future journey and proposed mock data | Unknowns; journey effort/confidence |
|---|---|---|---|---|
| کارت‌به‌کارت / Q1 | `F-039`; `S-174`–`S-176` include card-transfer variants | Card transfer entry; source/destination card, integer amount/unit, credentials/OTP fields and source-selection rules need journey verification | Synthetic source cards, saved destinations and preview-only recipient/fee reads | No proof which authenticated variant this item selects; no submission implemented; L / medium |
| انتقال وجه / Q2 | `F-039`; transfer menu `S-193`, internal `S-192`, interbank `S-191` | Choose transfer kind; account, destination and amount input rules depend on selected flow | Synthetic transfer-type configuration, account/destination records and preview read results | Supported types, schedules, limits and authorization contract Unknown; XL / medium |
| شارژ / Q3 | `F-044`; Home has a launch edge to `S-211`; other variants `S-213`–`S-216` | Mobile top-up entry; phone/operator/product/source/amount requirements need recovery | Synthetic operator/product list, saved phone aliases and read-only quote records | Launch existence does not prove this tile's active mode or preconditions; L / medium |
| قبض / Q4 | `F-043`; Home has a launch edge to `S-209` | Bill entry; bill/payment identifiers, source and validation modes need recovery | Synthetic bill aliases, inquiry results and exact integer bill/unit records | Tile binding, scan/manual variants, fees and wrappers Unknown; L / medium |
| صورتحساب / Q5 | `F-029`; request statement `S-086` exists | Account statement entry; account/date range/filter/output choices need recovery | Synthetic statement lines, page/date filtering and read-only export metadata | Home target, export permission and output types Unknown; M–L / medium |

## Service rows

Display source for every row: local service configuration at `R` position;
HC-VISUAL/HC-MENU. Each is display S / high confidence. The editor changes this
service order only; top and quick groups remain distinct. Precise service
prerequisites and journey inputs are Unknown until each family is recovered.

| Item / fixture | Existing family and candidate nodes | Purpose; later inputs/prerequisites to recover | Proposed future mock data/service | Unknowns; journey effort/confidence |
|---|---|---|---|---|
| چک / R1.1 | `F-035`–`F-038`; Home launch to menu `S-147` | Cheque services; selected operation, cheque/account identifiers and permissions | Synthetic cheque menu, cheque summaries and status/inquiry reads | Enabled subservices, digital/legacy variants and validations Unknown; XL / medium |
| تسهیلات / R1.2 | `F-048`; menu `S-324`, summary `S-338` exist | Loan services; customer/product/loan selection and eligibility inputs | Synthetic loan products, existing-loan summaries, schedules and eligibility scenarios | Menu target and large application journeys require separate recovery; XL / medium |
| پایانه فروش / R1.3 | `F-064`; `S-478`/`S-479` hosts, `S-474` menu | Merchant/terminal services; business, terminal and account prerequisites | Synthetic terminals, request tracking and installation requirements | Product meaning and Home entry binding remain partly Inferred; XL / low–medium |
| مساعده / R1.4 | `F-056`; host `S-420`, entry `S-415`, dashboard `S-414` | Wage advance; employer/enrollment/account/limit/amount prerequisites | Synthetic enrollment, limits, read-only request/history scenarios | Activation vs dashboard entry, eligibility and complete contract Unknown; XL / low–medium |
| سفته الکترونیک / R2.1 | `F-049`; host `S-344`, main `S-359` | Promissory-note services; chosen role, identity, amount and guarantee inputs | Synthetic note list, role/configuration and inquiry records | Signing/payment/issue/guarantee prerequisites need separate recovery; XL / medium |
| خدمات خودرو / R2.2 | Related `F-046`; road-service menu `S-253`/`S-254` exists | Vehicle service selection; plate/license/vehicle identifiers and permissions | Synthetic vehicle aliases, service catalogue and inquiry/fee reads | Umbrella tile-to-menu join and enabled service set Unknown; XL / medium |
| تبدیل بانکی / R2.3 | `F-042`; `S-201`–`S-204` variants | Bank identifier conversion; input identifier/type/source rules | Synthetic conversion examples with valid-format fake identifiers and result metadata | Actual entry mode, supported conversions and fees Unknown; M–L / medium |
| استعلامات / R2.4 | `F-059`; menu `S-443`, inquiry states `S-440`–`S-453` | Select inquiry; subject identifiers, consent and fee prerequisites | Synthetic inquiry types and explicit success/empty/failure scenarios | Home binding, requested identifiers and response contracts Unknown; L / medium |
| خیریه / R3.1 | `F-047`; Home launch to picker `S-283` | Charity choice; recipient/source/amount and consent inputs | Synthetic charity list and integer amount/unit preview records | Tile binding, recipient eligibility and complete payment path Unknown; M–L / medium |
| بیمه / R3.2 | `F-054`; menu `S-403`, list `S-402` | Insurance services; policy/person/source selection rules | Synthetic insurance types, policies/installments and inquiry records | Home entry, partner/external boundaries and eligibility Unknown; L / medium |
| کارتابل / R3.3 | No proven family/node binding; displayed inside `F-066` | Work queue; task role/type/ownership inputs Unknown | Proposed synthetic work-item list with explicit fixture-only states | Do not assume digital-cheque cartable `F-037` or another workflow solely from label; U / low |
| سوابق / R3.4 | Related `F-030`; transaction list `S-091` exists | Historical activity entry; history type/account/date filters Unknown | Synthetic history categories and read-only records | Generic label does not prove transaction, transfer, login or activation history destination; U / low |
| حساب وکالتی / R4.1 | Related `F-028`/`F-060`; exact binding unproven | Proxy-account services; plan/account/consent prerequisites | Synthetic plan/account eligibility and request-state records | Do not assume referral activity or a shared endpoint name establishes this journey; L / low |
| ضمانت‌نامه / R4.2 | `F-050`; `S-363`–`S-365` | Guarantees; guarantee selection, subject identity and permissions | Synthetic guarantee list/details and status inquiry records | List vs menu entry and issuance/inquiry roles Unknown; L / medium |
| افتتاح حساب / R4.3 | `F-020`; Home launch to `S-057` | Account-opening entry; identity/contact/address/product/branch requirements | Synthetic product/branch lists and nonpersonal identity/address scenarios | Onboarding/eKYC dependencies and account creation rules need full recovery; XL / medium |
| شعب / R4.4 | No proven family/node binding; displayed inside `F-066` | Branch discovery; search/location/region inputs Unknown | Proposed synthetic branch directory and local filter records | No new branch screen ID or external map launch inferred from icon; U / low |

## Header, local content and navigation controls

Every row specifies its own data source/result. Unless explicitly given an M ID,
its precise E/M association is Unknown; no service is invoked by this batch.
Future record proposals are independent designs and are not implemented here.

| Control / existing IDs | Purpose and user input | Display data/source; supported E/M evidence | Prerequisite and current result; future destination | Proposed future records/services | Unknowns/evidence; effort/confidence |
|---|---|---|---|---|---|
| Demo entry / `S-026` | Open Home; one tap | Local entry state; no E/M required | Installed replica → Home, no credentials | None | Designed for mock; S / high |
| Profile avatar / `S-026`, related `F-021` | Profile request; tap | Reviewed empty-profile asset; no owner photo; E/M Unknown | Home → unavailable notice; tab state destination identity unresolved | Synthetic display-profile read | Actual image/permissions/tab binding Unknown; HC-SHELL/HC-VISUAL; S display, L journey / medium |
| Notifications / `F-025`, `S-075` | Inbox request; tap | Fixture visibility and unread marker; no live count; E/M join Unknown | Home → notice; catalogued inbox exists, toolbar binding supported by Home launch edge | Synthetic inbox items/read-state/count read | Activation and badge/count semantics not established by reference alone; HC-SHELL/HC-VISUAL; S display, M journey / medium |
| Message/support icon / `S-026`, related `F-026` | Request help; tap | Fixture visibility and reviewed icon; E/M Unknown | Home → notice; exact help target Unknown | Shared support entry configuration and synthetic FAQ/ticket data | Visible support control does not prove an unseen chat screen; HC-SHELL/HC-VISUAL; S display, L journey / medium |
| Saved/bookmark / `S-026`, related `F-027` | Saved actions request; tap | Local icon; E/M Unknown | Home → notice; Home launch to `S-083` exists but per-control binding still needs review | Synthetic saved-menu IDs/read service | Not assumed to be saved bank destinations `F-040`; HC-SHELL/HC-ID; S display, M journey / medium |
| Search / `S-026` | Menu search request; tap; no search field implemented | Local icon; menu state shape recovered, E/M Unknown | Home → notice; exact search state/node unresolved | Local search over later verified configuration, no backend required for proposed design | Matching/filtering/navigation behavior Unknown; HC-SHELL; S display, M journey / medium |
| Logout / `S-026`, router `F-002` | Exit request; tap | Local icon; E/M Unknown | Home → notice; no real session destroyed or original router launched | Later replica session state only if separately approved | Original logout/session side effects not replicated; HC-SHELL; S display, M journey / medium |
| Balance display/hide / `S-026`, `F-028` | Reveal/hide synthetic balance; tap | Synthetic exact integer and explicit `IRR`/ریال unit; caller-supported `M-023`; exact E join Unknown | Home local display state only | Synthetic account-balance read with explicit stale/error/loading states later | Request/refresh/error and privacy preference contract Unknown; HC-DATA/HC-VISUAL; S display, M full states / high shape, medium behavior |
| Increase balance / `S-026`, `F-045` | Top-up request; tap | Local label/add icon; E/M Unknown | Home → notice; several account/wallet variants catalogued, no guessed target | Synthetic source/amount/configuration and read-only gateway outcome scenarios | Amount input, gateway/external return, eligibility Unknown; HC-SHELL/HC-ID; S display, L journey / medium |
| Account number and expansion / `S-026`, `F-028` | Inspect local owner/type/IBAN; expand/collapse tap | Synthetic selected-account display; caller-supported `M-004`; exact E join Unknown | Home local expansion, retains scroll and survives recreation | Synthetic selected-account record/read | Default account and formatting rules need later review; HC-SHELL/HC-DATA/HC-VISUAL; S–M / high shape, medium behavior |
| Account management/share / `S-026`, related `F-028` | Management/share request; tap | Synthetic account fields/local icons; `M-004` relevant, E join Unknown | Home → notice; no external share or management navigation | Synthetic account choices and later verified export representation | Permissions/PII handling and target binding Unknown; HC-SHELL; S display, M journey / medium |
| Financial-performance banner / `S-026`, related `F-058` | Present one evidenced offer; tap | One local copy/layout and reviewed illustration; fixed reference indicator state; E/M Unknown | Home → notice; no invented slide navigation or external launch | Proposed banner configuration: title/body/artwork reference/verified action; other records absent | Reference shows indicators but not the content/actions of other slides; HC-VISUAL/HC-MENU; S–M display, L feature / medium |
| ویرایش چیدمان / `S-026`, embedded `F-066` | Open editor; drag a service, Apply or Cancel | Draft copied from local service fixture, changed-order state; no M assigned; E join Unknown | Apply updates service order locally; Cancel/Back dismisses without applying draft; no destination screen | Proposed per-preview service-order preference; a future persistence service would need recovered contract | Reorder, apply/cancel and four-column grid Recovered; local save/recreation policy Designed for mock; HC-EDIT; M / high UI mechanism |
| Recent-transactions heading / `S-026`, related `F-030` | Show recent history; scroll | Synthetic local rows: outcome, type, date/time, source/destination display, integer amount/unit; no assigned transaction M or proven E ID | Scroll reveals content above fixed bottom bar | Synthetic history read sorted deterministically; no inferred transaction submission | Exact list request/paging and identifier formatting Unknown; HC-VISUAL/HC-DATA; M display, L history journey / medium |
| Transaction row / `S-026`, related `F-030` | Inspect a record; tap if offered | Same synthetic local record, no owner details; E/M Unknown | Notice, no receipt/details screen registered; `S-090` exists but binding unproven | Synthetic record detail/receipt read if later recovered | No inference that every transaction type shares one destination; HC-VISUAL/HC-ID; S display, M–L details / medium |
| Home tab / `S-026`, `F-065` | Reselect Home; tap | Local selected tab and reviewed icon/label; no E/M needed | Remains Home; state/scroll preserved | None | HC-SHELL; S / high |
| Account tab / `S-026`, `F-065`, related `F-028` | Request account tab; tap | Local unselected icon/label; E/M Unknown | Notice; legacy `S-085` is not assumed to be this tab | Later synthetic account-tab data after identity recovery | Separate tab-state ID absent in current graph; HC-SHELL/HC-ID; S display, M–L tab / medium |
| Card tab / `S-026`, `F-065`, related `F-033` | Request card tab; tap | Local unselected icon/label; E/M Unknown | Notice; legacy card activities not substituted for hosted tab | Later synthetic card-tab data after identity recovery | Same tab-state identity gap; HC-SHELL/HC-ID; S display, L tab / medium |
| Profile tab / `S-026`, `F-065`, related `F-021` | Request profile tab; tap | Local unselected icon/label; E/M Unknown | Notice; remains on Home | Later synthetic profile/menu state | Exact tab data and actions require recovery; HC-SHELL; S display, L tab / medium |
| Central QR / `S-026`, `F-065`, related `F-032` | Request QR action; tap | Reviewed local control; E/M Unknown | Notice; no camera/external launch; Home launch edge to scanner `S-108` does not prove all runtime prerequisites | Future synthetic eligibility/configuration and nonpayment scan examples | Authorization/menu gates and scanner behavior Unknown; HC-SHELL/HC-ID; S display, L journey / medium |
| Back and unavailable notice / `S-026` | Dismiss notice/editor, collapse account, return to demo | Local transient state only; no E/M required | Notice closes first; editor draft discarded; then account collapse/return to demo as applicable | None | Preview behavior Designed for mock, not real authentication/navigation success; HC-SHELL/HC-EDIT; S–M / high preview confidence |

## Follow-up requirements boundaries

Home rendering needs no backend. A later mock integration could read one explicit
synthetic Home configuration, selected account, balance and recent-history list;
it must preserve the distinction between those display records and an enabled
business journey. The account/balance shapes have caller support (`M-004`,
`M-023`), but precise endpoint IDs are still withheld because the existing API
map does not independently establish their Home joins. Encrypted wrapper fields
and exact contracts remain Unknown.

Each later implementation batch must recover its entry callback, state model,
inputs, branching and API caller chain before assigning precise contracts. That
is particularly necessary for investment, cartable, history, proxy account and
branches, where visible labels are insufficient to select a unique destination.
No database schema, backend service, assistant tool, bridge or transaction
operation is introduced by this assessment.
