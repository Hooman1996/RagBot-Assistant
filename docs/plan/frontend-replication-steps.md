# Frontend replication steps

Canonical ordered implementation list for D-020 Stage 1. Issue #42 authorizes Home completion and this investigation only. **FR-01 is done; FR-02 is done in #44 (including owner-approved recovery of S-014/S-016); FR-03 is done in #46 (including owner-approved recovery of S-012 and the move of S-123 to FR-04); FR-04 was split with owner approval in #48; FR-04a (#48) and FR-04b (#50) are done; FR-04c and FR-05–FR-45 remain proposed.** The next approved issue must name its exact batch and acceptance command. No later batch starts automatically.

This list consolidates the earlier proposed “Home refinement, then account tab” paragraph in the foundation report. Phase specs and the revised build sequence remain the authority for later stages; this is the single frontend batch tracker, not a replacement master plan.

## Evidence and interpretation

- **Recovered:** the [feature inventory](../../app_profiles/feature-inventory.md), [screen catalogue](../../app_profiles/screen-catalogue.md), [navigation graph](../../app_profiles/navigation-graph.json) and [API/model inventory](../../app_profiles/api-structure.md) cover the pinned extracted artifact. The graph has **485 nodes / 590 edges**: 266 activities, 199 Compose destinations, 19 subgraphs and one menu hub. A host, lifecycle wrapper, nested graph or duplicate legacy/current implementation is not automatically a separate visible page.
- **Owner-supplied visual evidence:** HC-REF contains upper/lower scroll positions of the same original Home and the earlier replica capture. It establishes the visible Home configuration only. It does not observe other screens or prove their navigation.
- **Recovered with limits:** 341 graph nodes are Recovered, 117 Inferred and 27 Unknown. There are **43 dynamic edges with null targets** and **139 additional call sites with unattributed source screens**. A destination declaration, feature package or named service method is insufficient proof of reachability, content fidelity or an exact contract.
- **Inferred / Designed for mock:** batching, dependencies, day estimates and future records below are our implementation proposals. They are not new recovered graph edges, bank tables, menu identifiers or business rules. Local synthetic fixtures provide UI states; no mock HTTP service or database is authorized by this list.
- **Unknown:** encrypted M-002/M-003/M-029 wrappers expose transport shapes, not their inner business payloads. Service-area grouping does not establish a screen-to-endpoint relationship. Every future batch must trace actual content/view-model/repository callers before claiming E-###/M-### joins. The current map has 520 endpoints / 60 models, with 75 Unknown endpoints; no production path or host is needed.
- Private recovery sources are the existing inventory/navigation/API findings and decode, reused under HC-PLAN. Exact classes, resources, caller mappings and raw REA output stay private. Focused recovery starts from those records; do not repeat the whole investigation or ask the owner to discover every page. Owner comparison supplements code/resource evidence.

## Common contract for every proposed batch

Every step below includes the stated scope **plus** recovered loading/empty/error/validation/modal/keyboard/Back/recreation states, Persian/RTL/accessibility behavior, asset provenance and honest unsupported controls. A state is implemented only after its content and incoming/outgoing handlers are evidenced. Recovery gaps block the affected part; useful independent UI work may continue. Do not register unknown destinations or inflate coverage by counting containers as pages.

Each batch produces a sanitized requirements matrix covering feature/node IDs, purpose, user inputs, fixture/display data, prerequisites/destination, caller-supported E/M references, proposed future records/services, unknowns, effort and confidence. It distinguishes displaying an item from its full journey. Exact integer money includes its explicit unit. Legal/eligibility/rate rules remain Unknown until recovered; synthetic outcomes are labelled **Designed for mock**. No service, database, assistant, bridge, external banking launch or transaction submission is introduced.

Acceptance for **each** batch includes the repository checks and Android build/unit/lint/device command in [replica development](../runbook/replica-development.md), meaningful per-batch tests, installation/inspection on the existing pinned emulator, private top/detail/error captures, no crashes/clipping or bottom-bar obstruction, retained state, and graph-ID validation. Technical acceptance and original-app fidelity are reported separately. Unavailable references stay unscored; our own screenshots are not original-app goldens. PR acceptance and green CI precede merge; the merged revision is then installed and verified.

Effort is an initial estimate in focused engineering days including recovery, UI, documentation and device review; it is not a delivery promise. “High” confidence is precise layout/handler evidence; “medium” has named content but missing joins/states; “low” has inferred identity or route/contract gaps. A proposed dependency means component reuse or a recovery prerequisite, not a claimed graph transition. If a 4–7 day batch proves too large, split its related subjourneys in its approval proposal before implementation.

## Ordered batches

### FR-01 — Populated Home

**Status:** done (Home acceptance passed; issue #42). **Families:** F-014, F-065, F-066. **Existing nodes:** F-066, S-026 (2 primary allocations). **Dependencies:** existing foundation.

**What opens afterward:** Open the demo entry and scroll the complete configured Home, inspect synthetic account/history rows and dismiss unavailable-control notices without losing position.

**Frontend scope:** Header/account geometry, all supplied menu groups, single evidenced banner with indicators, layout editing only as recovered, recent history, fixed bottom controls.

**Evidence / specific gaps:** HC-REF and HC-HOME; Owner upper/lower screenshots establish one configuration, not every user configuration or slide. External destinations remain unavailable; hidden eligibility and precise service joins remain unresolved.

**Requirements output:** Home control/data/service matrix, asset provenance, fixture-versus-evidence labels and remaining visual questions.

**Acceptance beyond the common contract:** Home-specific content, RTL, scroll clearance, retained state, Back, recreation and comparison captures pass on emulator-5554. **Effort:** current approved batch. **Evidence confidence:** high visual structure; medium behavior/contracts.

### FR-02 — Accounts, account details and statement/history inspection

**Status:** done (#44; 6 JVM / 15 server device tests, build/lint and rendered review). **Families:** F-021, F-028–F-030. **Existing nodes:** S-014, S-016, S-063, S-067, S-085–S-086, S-090–S-091 (8 primary allocations). **Dependencies:** FR-01.

**What opens afterward:** Open account selection/management, inspect a selected account and navigate statement/history list to detail and back.

**Frontend scope:** Recover account-tab content separately from legacy list activity; combine list, simple detail, account fetch/management and read-only statement/filter components with local empty/loading/error fixtures.

**Evidence / specific gaps:** AI-TAB and AI-STATEMENT resolve the embedded Home account tab separately from the S-085 picker. Owner approved moving recovered bank-statement list/detail S-016/S-014 from FR-45 to this batch; IDs are unchanged. S-086 is a separate export-request form, S-091/S-090 are legacy category history/detail, and the current Home history hub remains unresolved/unimplemented. Dynamic account-menu ordering and visual references are still unavailable.

**Requirements output:** [Requirements matrix](../reports/accounts-requirements.md) covers selection/ownership, exact integer IRR, filters/pagination, model roles and caller-backed joins; [acceptance and fidelity limits](../reports/accounts-inspection.md) are separate. API-map wrapper discrepancy remains explicit.

**Acceptance beyond the common contract:** Only verified handlers open; selection survives Back/recreation, filters validate, list/detail use consistent integer amounts and Home return retains position. **Effort:** 3–5 days. **Evidence confidence:** medium; next-step identity gate.

### FR-03 — Cards tab, list and read-only details

**Status:** done (#46; 10 JVM / 21 server device tests, build/lint and rendered review). **Families:** F-021, F-033. **Existing nodes:** S-012, S-068, S-119–S-120, S-125–S-126, S-130–S-131, S-134 (9 primary allocations). **Dependencies:** FR-02.

**What opens afterward:** Open Home → کارت, switch card group and card, show/hide a debit balance, and open card information, the legacy card list, credit summary, file-loan summary and the bon-balance form; review entries open card management/fetch, card display status and the pre-login balance form.

**Frontend scope:** The hosted card tab is content of S-026 (no new ID): card pager per group, group selector and configured action rows. Read-only destination pages with local fixtures; card mutations, payment, PIN processing and remote preferences stay unavailable.

**Evidence / specific gaps:** CI-TAB resolves the hosted tab independently of legacy S-125, which is a separate list reached by a tab action. Owner approved moving recovered card information S-012 from FR-45 to this batch and S-123 card color, reachable only inside card-operation steps, to FR-04; IDs are unchanged. S-126/S-068, S-130 and S-120 are reached from Profile, settings and pre-login menus that are not built yet, so they use labelled review entries. Server card-group menus, their order and card colours remain Unknown.

**Requirements output:** [Requirements matrix](../reports/cards-requirements.md) covers card groups, masking, selected/default state, distinct balance and credit amounts, caller-backed M-024/M-026/M-008 roles and the unconfirmed M-060; [acceptance and fidelity limits](../reports/cards-inspection.md) are separate.

**Acceptance beyond the common contract:** Debit/credit/empty/loading/error cases, card and group restoration, known/hidden/zero/missing balances, masking pass-through and RTL render; out-of-scope actions explain the unavailable state on the current page. **Effort:** 3–5 days. **Evidence confidence:** medium-high identity, medium visuals/contracts.

### FR-04a — Card operation requests

**Status:** done (#48; 12 JVM / 24 server device tests, build/lint and rendered review). **Families:** F-033. **Existing nodes:** S-123–S-124, S-127–S-129 (5 primary allocations). **Dependencies:** FR-03.

**What opens afterward:** From the card tab, request a new card or extend, reissue or recolour a debit card: choose source and delivery, an address for postal delivery, a colour, review and register, and see a labelled local result.

**Frontend scope:** Issue (S-124) and modify (S-127) first pages, address (S-129), colour (S-123) and confirm (S-128) with one shared draft. In-person delivery skips the address page, as recovered.

**Evidence / specific gaps:** CR-FLOW traces the card-tab routes, the delivery branch and the confirm/status step. The recovered confirm has no OTP. Owner approved a labelled synthetic result. Server notices, fees, colour lists, branch data, the insufficient-balance top-up branch and the shared operations menu list (S-002, FR-45) are not reproduced; adding an address (S-463) belongs to FR-05.

**Requirements output:** [Requirements matrix](../reports/card-requests-requirements.md); [acceptance and fidelity limits](../reports/card-requests-inspection.md).

**Acceptance beyond the common contract:** Delivery branch, required address/colour, Back draft restoration, recreation, Cancel leaving the flow and a labelled result that changes nothing. **Effort:** 2–3 days. **Evidence confidence:** medium-high flow, medium visuals/contracts.

### FR-04b — Card blocking, PIN and second-password activation

**Status:** done (#50; 14 JVM / 28 server device tests, build/lint and rendered review). **Families:** F-033. **Existing nodes:** S-118, S-121–S-122, S-135–S-136 (5 primary allocations). **Dependencies:** FR-04a.

**What opens afterward:** From the card tab, block a card of the current group, assign or change a first or second PIN, or activate the dynamic second PIN; review entries open the two pre-login block forms. Every valid confirm ends in a labelled local result.

**Frontend scope:** Signed-in block list with the recovered warning (S-121, no OTP); PIN sections with confirm and OTP (S-135); activation with OTP (S-136); pre-login block forms (S-118 legacy, S-122 flag-gated v2). One shared OTP and result pattern.

**Evidence / specific gaps:** CS-ROUTES traces the tab and pre-login routes; CS-PAGES the warning, PIN rules and OTP sheet. The OTP is a fixed local code and the dynamic-PIN SMS request is unavailable. Exact pre-login field rules, OTP timers and failure texts are not recovered.

**Requirements output:** [Requirements matrix](../reports/card-security-requirements.md); [acceptance and fidelity limits](../reports/card-security-inspection.md).

**Acceptance beyond the common contract:** Invalid fields, wrong and cancelled OTP, Back/recreation without restoring secrets, and labelled results that change nothing. **Effort:** 2–3 days. **Evidence confidence:** medium-high flows, medium pre-login rules.

### FR-04c — Wallet-linked card journeys

**Status:** proposed (split from FR-04 with owner approval in #48). **Families:** F-034. **Existing nodes:** S-137–S-141 (5 primary allocations). **Dependencies:** FR-04a.

**What opens afterward:** Inspect wallet-linked card issue, send type, address and PIN content through locally simulated states.

**Frontend scope:** Wallet card issue host and its destinations; containers are not extra pages.

**Evidence / specific gaps:** Wallet-card tab presentation and eligibility gates require caller traces.

**Requirements output:** Per-step inputs, address prerequisites, M-005/M-011 candidates and synthetic result provenance.

**Acceptance beyond the common contract:** Invalid fields, cancellation and address selection work; no wallet or card write occurs. **Effort:** 2–3 days. **Evidence confidence:** medium-low.

### FR-05 — Profile, settings and saved addresses

**Status:** proposed. **Families:** F-021–F-024, F-062. **Existing nodes:** S-025, S-064–S-065, S-069–S-074, S-463–S-464 (11 primary allocations). **Dependencies:** FR-02.

**What opens afterward:** Open profile, inspect/edit a synthetic profile, inspect preferences/change history and select or edit a local saved address.

**Frontend scope:** Combine small profile/preferences/address screens and shared validation/dialogs; storefront settings are recovered before enabling controls.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Profile tab has no separate catalogue node; S-025 remains an unassigned settings menu until its caller relationship is proved. Storefront semantics and mutation persistence are uncertain.

**Requirements output:** Profile/address fields, preference scope, notification/biometric prerequisites and changelog data; synthetic changes stay application-local.

**Acceptance beyond the common contract:** Editing, cancellation, local persistence, address validation, Back and recreation behave consistently; unsupported platform settings are explicit. **Effort:** 3–5 days. **Evidence confidence:** medium; storefront low.

### FR-06 — Notifications and support inbox

**Status:** proposed. **Families:** F-025, F-026. **Existing nodes:** S-075–S-082 (8 primary allocations). **Dependencies:** FR-05.

**What opens afterward:** Inspect notifications and FAQ, open a synthetic ticket/thread, and compose a local suggestion or reply.

**Frontend scope:** Combine inbox/list/detail/delete sheets and support thread forms using deterministic unread/read/empty/error and local draft states; chatbot screen is a visual shell with unavailable answering.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Notification types, unauthenticated ticket OTP and server-provided FAQ content require recovery. No RagBot answering integration belongs in this step.

**Requirements output:** Unread counts, pagination, FAQ/ticket/reply fields and M-012/M-014/M-016–M-020/M-030/M-032/M-040/M-041/M-048/M-049/M-058 candidates with caller evidence.

**Acceptance beyond the common contract:** Read/delete/dismiss and draft restoration are tested locally; FAQ/login variants are distinguishable; no message is transmitted and no assistant is implemented. **Effort:** 3–5 days. **Evidence confidence:** medium.

### FR-07 — Saved destinations and quick-action configuration

**Status:** proposed. **Families:** F-021, F-027, F-040. **Existing nodes:** S-066, S-083–S-084, S-196–S-198 (6 primary allocations). **Dependencies:** FR-02, FR-03.

**What opens afterward:** Open saved destination list/picker, inspect/edit a synthetic favorite and choose recovered shortcut configuration.

**Frontend scope:** Combine related favorite/picker/configuration screens and their local order/selection state; preserve caller result semantics.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Shortcut eligibility and edit/reorder gestures need code evidence; no drag interaction or Home server identifier is inferred from a label.

**Requirements output:** M-010 candidate fields, supported destination types, duplicate/order rules and caller-result contracts; exact encrypted endpoint payloads remain Unknown.

**Acceptance beyond the common contract:** Filtering, selection/cancel, local edits and Back/recreation preserve the originating form and do not navigate to an invented target. **Effort:** 2–4 days. **Evidence confidence:** medium.

### FR-08 — Individual card/account transfers

**Status:** proposed. **Families:** F-031, F-039. **Existing nodes:** S-093, S-097–S-098, S-173–S-176, S-191–S-193 (10 primary allocations). **Dependencies:** FR-02, FR-03, FR-07.

**What opens afterward:** Choose a transfer type and synthetic source/recipient, fill the individual transfer form and inspect review/receipt previews.

**Frontend scope:** Card, internal and interbank forms share source/recipient/amount components; anonymous entry remains visibly distinct; receipt previews carry synthetic labels.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Transfer-type eligibility, limits/fees, OTP and ambiguous route arguments need caller recovery; S-194/S-195 are reserved unresolved, not fabricated alternatives.

**Requirements output:** M-025/M-031/M-033/M-034/M-036 candidates, exact integer units, source ownership, validation and user-only authorization boundary.

**Acceptance beyond the common contract:** Invalid recipient/amount, selected source, review corrections, cancellation and Back are tested; no handler submits or authorizes a transfer. **Effort:** 4–6 days. **Evidence confidence:** medium.

### FR-09 — Group transfer preparation and history

**Status:** proposed. **Families:** F-030, F-031, F-039. **Existing nodes:** S-087–S-089, S-096, S-182–S-190 (13 primary allocations). **Dependencies:** FR-08.

**What opens afterward:** Inspect group-transfer history/destination details, enter or import a synthetic recipient fixture and inspect validation/verification/waiting/result previews.

**Frontend scope:** One complete group-transfer family combines file/recipient list, error rows, validation and history/detail with deterministic local files only.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Accepted file schema, duplicate-row rules, large-file limits and waiting transitions need code recovery; importing arbitrary desktop files is not assumed.

**Requirements output:** File type M-001, row fields/error mapping, source context, totals/units and receipt requirements; payment contracts remain separate from display fixtures.

**Acceptance beyond the common contract:** Valid/invalid fixture rows, totals, error recovery, cancellation and navigation restore state; waiting/result previews cannot imply a live transfer. **Effort:** 4–6 days. **Evidence confidence:** medium-low.

### FR-10 — Recurring and continuous transfer management

**Status:** proposed. **Families:** F-039, F-041. **Existing nodes:** S-177–S-181, S-199–S-200 (7 primary allocations). **Dependencies:** FR-08.

**What opens afterward:** Inspect recurring/continuous schedules, request/history details and edit a local schedule through review.

**Frontend scope:** Combine schedule setup/list/detail/history and continuous-execution presentation with shared transfer selectors; no scheduler runs.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; The two recurring families may have different business semantics; start/end/count/calendar rules and cancel/execution states need independent caller evidence.

**Requirements output:** M-045/M-046 candidates, Persian date rules, integer amount/unit, source/recipient and future schedule service proposal.

**Acceptance beyond the common contract:** Schedule validation, correction, read-only execution states and Back restore inputs; no background transfer or success event occurs. **Effort:** 3–5 days. **Evidence confidence:** medium.

### FR-11 — Bill and insurance installment payments

**Status:** proposed. **Families:** F-031, F-043, F-054. **Existing nodes:** S-092, S-132–S-133, S-205–S-210, S-401–S-405 (14 primary allocations; credit-card repayment confirm/receipt S-132–S-133 moved from FR-04 with owner approval in #48). **Dependencies:** FR-02, FR-03.

**What opens afterward:** Inspect bill entry/inquiry, insurance policy/installment lists and their amount/source/review/receipt previews.

**Frontend scope:** Combine small payment-list/form/review journeys using shared selectors, payable-item rows and receipt presentation; keep each family’s validation separate.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Repeated bill-transfer nodes may represent distinct source modes, not duplicate screens; insurance eligibility, partial-payment rules and anonymous card forms require recovery.

**Requirements output:** Bill identifiers, installment status/due dates, integer currency units, OTP and receipt data; M-031 candidate and caller-backed E joins only.

**Acceptance beyond the common contract:** Input errors, paid/unpaid/empty fixtures, source modes, cancellation and review correction pass; no payment or policy request is sent. **Effort:** 4–6 days. **Evidence confidence:** medium.

### FR-12 — Mobile top-up and charity payment previews

**Status:** proposed. **Families:** F-031, F-044, F-047. **Existing nodes:** S-094–S-095, S-211–S-216, S-282–S-284 (11 primary allocations). **Dependencies:** FR-02, FR-03, FR-11.

**What opens afterward:** Select a synthetic operator/product or charity, enter a recipient/amount, and inspect source-specific review/receipt previews.

**Frontend scope:** Combine two short purchase/donation journeys sharing source, amount, phone input and receipt components; charity and operator rules stay separately documented.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Operator denominations, anonymous card variants and charity-specific amount constraints are server-configured or unresolved; no live catalog is inferred.

**Requirements output:** Product/charity display records, M-021 candidate, phone validation, integer amount/unit, source/OTP and future catalog-service proposal.

**Acceptance beyond the common contract:** Operator/charity selection, invalid inputs, empty catalogs and review correction pass; fixtures are synthetic and no purchase/donation succeeds remotely. **Effort:** 3–5 days. **Evidence confidence:** medium.

### FR-13 — Wallet overview, QR display and history

**Status:** proposed. **Families:** F-032. **Existing nodes:** S-110–S-115 (6 primary allocations). **Dependencies:** FR-02.

**What opens afterward:** Open a synthetic wallet, inspect its balance/history/detail and recoverable QR display state.

**Frontend scope:** Combine read-only wallet menu/main/statement/history/detail and QR presentation; all displayed identities are synthetic.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Wallet creation/eligibility and QR payload semantics are not proved by the node names. QR must remain unavailable until its format is recovered.

**Requirements output:** M-055/M-056 candidates, wallet identifier/masking, balance units, history pagination and QR display prerequisites.

**Acceptance beyond the common contract:** Wallet/empty/error states, detail navigation and Back preserve selection; QR content is clearly local and cannot initiate payment. **Effort:** 2–4 days. **Evidence confidence:** medium.

### FR-14 — Wallet purchases, transfers and QR scanner shell

**Status:** proposed. **Families:** F-032. **Existing nodes:** S-099–S-109, S-116–S-117 (13 primary allocations). **Dependencies:** FR-12, FR-13.

**What opens afterward:** Inspect wallet charge/withdrawal, wallet-to-wallet, bill/top-up/purchase and fast-payment scan forms through local review states.

**Frontend scope:** Reuse recovered wallet amount/source/recipient and review components; camera permission/scanner UI only where recoverable, with local test input.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Scanner payload grammar, wallet-specific limits, availability and bill/top-up variants need focused traces. Never treat scanned content as a trusted navigation command.

**Requirements output:** Wallet operation fields, M-055/M-056 candidates, camera/QR validation and user-only boundary; future services proposed, not built.

**Acceptance beyond the common contract:** Malformed QR, denied permission, insufficient synthetic balance, correction and cancellation pass; no external launch or wallet operation occurs. **Effort:** 4–6 days. **Evidence confidence:** medium-low.

### FR-15 — Account/wallet funding and gateway states

**Status:** proposed. **Families:** F-045. **Existing nodes:** S-217–S-235 (19 primary allocations). **Dependencies:** FR-02, FR-13.

**What opens afterward:** Inspect account/wallet increase-balance entry, in-flow/standalone waiting, result and receipt previews.

**Frontend scope:** Consolidate four closely related funding graphs around source/amount/waiting/result components; preserve differences recovered from callers.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Gateway callback/result semantics and S-236/S-237 identity are unresolved; no WebView loads an original URL and no payment application launches.

**Requirements output:** Funding context, amount/unit, local waiting/result fixture transitions, callback expectations and provenance of synthetic receipts.

**Acceptance beyond the common contract:** All recovered entry variants, cancellation/timeouts/recreation and Back work; waiting state has a deterministic local exit and never implies settlement. **Effort:** 3–5 days. **Evidence confidence:** medium-low.

### FR-16 — Vehicle-service hub and saved vehicle/contact records

**Status:** proposed. **Families:** F-046. **Existing nodes:** S-246, S-248, S-252–S-254, S-256, S-259–S-260, S-262–S-273 (20 primary allocations). **Dependencies:** FR-05.

**What opens afterward:** Open the recovered vehicle-service hub, manage synthetic plates/contacts/license selections and inspect inquiry history.

**Frontend scope:** Combine shared selectors/menu/history/navigation-state wrappers. S-263–S-273 are graph containers, not eleven new pages; Back-handler nodes need content-level recovery.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Most vehicle route labels are Inferred; menu variants, ownership, plate formats and handler/container-to-content relationships need recovery before registration.

**Requirements output:** Vehicle/license/contact fields, selection result contracts, history fixtures and per-query prerequisites; no new database tables inferred.

**Acceptance beyond the common contract:** Persian plate RTL/LTR fragments, selection/cancel, empty lists, nested Back and recreation pass with only implemented destinations registered. **Effort:** 3–5 days. **Evidence confidence:** low-medium.

### FR-17 — Road toll inquiry and payment previews

**Status:** proposed. **Families:** F-046. **Existing nodes:** S-240–S-242, S-249–S-251, S-257–S-258, S-261, S-274–S-276 (12 primary allocations). **Dependencies:** FR-11, FR-16.

**What opens afterward:** Inspect annual/freeway/traffic/pollution inquiry and charge rows, then recovered confirmation and receipt previews.

**Frontend scope:** Combine the related toll inquiry/result/fee/review components with a local selected vehicle; no live balance lookup or payment.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Pollution route may have a different payment boundary; query parameters, fee freshness, unpaid-item selection and receipt joins need verification.

**Requirements output:** Per-toll fields, integer amount/unit, result-status taxonomy, fees and expected navigation; encrypted service relationships remain Unknown.

**Acceptance beyond the common contract:** No-dues/multiple-dues/error fixtures, item selection, review and Back preserve vehicle/context; no toll payment is submitted. **Effort:** 3–5 days. **Evidence confidence:** low-medium.

### FR-18 — Vehicle status, violations, tax and document inquiries

**Status:** proposed. **Families:** F-046. **Existing nodes:** S-238–S-239, S-243–S-245, S-247, S-255, S-277–S-281 (12 primary allocations). **Dependencies:** FR-16, FR-17.

**What opens afterward:** Inspect active-plate/license/negative-point/document queries and violation/transfer-tax charge previews.

**Frontend scope:** Reuse vehicle query/result components while documenting each inquiry’s fields and the two charge-confirmation variants separately.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Personal/vehicle identity checks, result categories and which inquiries require fees are unresolved; a matching package name is not a service join.

**Requirements output:** Query fields/consent, response rows, errors/expiry and proposed future status services; identify Unknown wrappers explicitly.

**Acceptance beyond the common contract:** Each query has distinct fixtures/validation, correct return context and fee/confirmation boundaries; no original service or external launch is used. **Effort:** 3–5 days. **Evidence confidence:** low-medium.

### FR-19 — Cheque overview, tracking and chequebook requests

**Status:** proposed. **Families:** F-035, F-036. **Existing nodes:** S-142–S-147, S-149–S-150, S-153–S-156 (12 primary allocations). **Dependencies:** FR-02.

**What opens afterward:** Open cheque menu/list/tracking, inspect chequebook/request details and walk issue inquiry/fee/review states.

**Frontend scope:** Combine cheque inventory, status tracking and the short chequebook issue family; S-155/S-156 are retained as variants until content equivalence is established.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; S-148 is unresolved, lists are largely Inferred, and cheque/book status enums and request prerequisites need caller/content recovery.

**Requirements output:** Cheque/book/request fixtures, issue page-count/fee fields, account eligibility and caller-backed service references; no issuance contract guessed.

**Acceptance beyond the common contract:** Filters/statuses, issue validation, request-detail Back and empty/error fixtures pass; issuing remains a local preview. **Effort:** 3–5 days. **Evidence confidence:** medium-low.

### FR-20 — Digital cheque activation, cartable and transfer

**Status:** proposed. **Families:** F-035, F-037. **Existing nodes:** S-151–S-152, S-157–S-159 (5 primary allocations). **Dependencies:** FR-19.

**What opens afterward:** Inspect digital-cheque activation requirements, cartable list/detail and transfer review with synthetic records.

**Frontend scope:** Combine closely related digital-cheque list/detail/transfer and activation-state UI, reusing cheque identifiers and source context.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; External integration handoff and activation prerequisites need recovery; no real integration app is opened and no invented cartable category is added.

**Requirements output:** Digital-cheque ownership/status, recipient and activation-result records, required inputs and external-dependency boundary.

**Acceptance beyond the common contract:** Activation unavailable/error states, list/detail, transfer validation/cancel and Back pass without external execution or transfer. **Effort:** 2–4 days. **Evidence confidence:** medium.

### FR-21 — Cheque acceptance, payment and agent flows

**Status:** proposed. **Families:** F-038. **Existing nodes:** S-160–S-172 (13 primary allocations). **Dependencies:** FR-19, FR-20.

**What opens afterward:** Inspect cheque-ID/type/receiver/agent entry, inquiry/validation, preview and history/detail states.

**Frontend scope:** Implement the distinct acceptance/issue/transfer forms and shared local preview/history components only after tracing their callers.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Nine nodes are Inferred; issue versus transfer/accept variants, legal-validation result and agent authority are unresolved, so no success path may be invented.

**Requirements output:** Cheque/receiver/agent inputs, authority checks, legal/error states, fees and user-only operation boundary; exact encrypted contracts stay Unknown.

**Acceptance beyond the common contract:** Valid/invalid IDs, agent selection, negative inquiry/legal states, review correction and Back pass; no cheque operation is authorized. **Effort:** 4–6 days. **Evidence confidence:** low-medium.

### FR-22 — Guarantee lists, details and authenticity inquiries

**Status:** proposed. **Families:** F-050, F-059. **Existing nodes:** S-363–S-365, S-446–S-451 (9 primary allocations). **Dependencies:** FR-02.

**What opens afterward:** Inspect guarantee records/detail and own-bank/all-bank authenticity inquiry forms/results.

**Frontend scope:** Combine related guarantee lookup and inquiry surfaces while retaining separate account/company and inquiry input contexts.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Guarantee ownership/company prerequisites and inferred route variants need caller evidence; do not identify Home’s guarantee tile with one specific route yet.

**Requirements output:** Guarantee list/detail/inquiry inputs, result/error fixture fields and E-305/E-306/E-174/E-175 candidates with joins unproven.

**Acceptance beyond the common contract:** Empty/list/detail, inquiry validation, failed/successful local result and Back restore context without live inquiry. **Effort:** 2–4 days. **Evidence confidence:** medium.

### FR-23 — Bank identifier conversion

**Status:** proposed. **Families:** F-042. **Existing nodes:** S-201–S-204 (4 primary allocations). **Dependencies:** FR-02, FR-03.

**What opens afterward:** Inspect account/card/IBAN conversion variants, synthetic ownership result and any recovered paid-query review.

**Frontend scope:** Combine four small conversion/source/authentication variants with shared identifier validation and result presentation.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Exact available conversion directions, no-login fee gates and call-to-model mapping need traces; unknown payment dependencies remain unavailable.

**Requirements output:** M-034/M-042/M-043/M-050–M-054 candidates, identifier normalization, ownership fields, fee/unit and result prerequisites.

**Acceptance beyond the common contract:** Invalid checksums/formats, mode changes, paid-query unavailable notice, copy/display and Back pass using synthetic identifiers. **Effort:** 2–3 days. **Evidence confidence:** medium.

### FR-24 — Credit and bounced-cheque inquiries

**Status:** proposed. **Families:** F-059. **Existing nodes:** S-440–S-445, S-452–S-453 (8 primary allocations). **Dependencies:** FR-02, FR-13, FR-22.

**What opens afterward:** Inspect inquiry menu/fee, bounced-cheque and credit-report/rating results with their detail and unsuccessful states.

**Frontend scope:** Combine closely related inquiry/result/detail patterns with recovered account/wallet fee choices; guarantee inquiries remain in FR-22.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Report ownership/consent, rating bands and server-provided result content are not established by endpoint names; no score/eligibility is fabricated as recovered.

**Requirements output:** Inquiry inputs, fee source/amount/unit, report/detail fields and E-165–E-173 candidates with actual caller joins pending.

**Acceptance beyond the common contract:** Empty/negative/error/rating fixtures, fee-source change, validation and Back pass with clear synthetic labels. **Effort:** 2–4 days. **Evidence confidence:** medium-low.

### FR-25 — Rewards, referral and agent history

**Status:** proposed. **Families:** F-057, F-060, F-061. **Existing nodes:** S-426–S-433, S-454–S-462 (17 primary allocations). **Dependencies:** FR-02, FR-07.

**What opens afterward:** Inspect points/reward detail/history, local coin-transfer request review and referral/proxy-plan/agent history.

**Frontend scope:** Combine adjacent loyalty/referral benefits and their shared lists, agreement/detail/history selectors; keep points and money units distinct.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Coin-transfer eligibility, referral-plan/proxy rights and their relationship to Home’s club entry need recovery. Product availability is not inferred from package presence.

**Requirements output:** Point ledger, request/plan/referral/agent records, agreement/recipient inputs, limits and E-142–E-144 candidates without unproved screen joins.

**Acceptance beyond the common contract:** Points versus currency display, negative/ineligible fixtures, selection/cancel and Back pass; no coin transfer, proxy grant or referral registration occurs. **Effort:** 4–6 days. **Evidence confidence:** medium-low.

### FR-26 — Personal finance dashboard, budgets and stories

**Status:** proposed. **Families:** F-058. **Existing nodes:** S-434–S-439 (6 primary allocations). **Dependencies:** FR-02.

**What opens afterward:** Open financial-performance detail, inspect synthetic budgets/category detail and recovered story/list presentation.

**Frontend scope:** Combine six finance-management host/content records, charts, budget form and story navigation; retain one Home banner unless further slides are evidenced.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Category taxonomy, aggregation/time windows, story order and budget validation need view-model/data tracing; no numerical financial advice is inferred.

**Requirements output:** Integer amounts/currency, periods/categories/budget records, chart accessibility and future aggregation-service proposal.

**Acceptance beyond the common contract:** Chart/text totals agree, empty/error periods and budget edits validate, story Back works and Home scroll is retained. **Effort:** 2–4 days. **Evidence confidence:** medium.

### FR-27 — Brokerage and fund management

**Status:** proposed. **Families:** F-052. **Existing nodes:** S-383–S-394 (12 primary allocations). **Dependencies:** FR-02, FR-05.

**What opens afterward:** Inspect broker list/inquiry/agreement, credential-form presentation, fund detail/orders and subscription/redemption review states.

**Frontend scope:** One investment family combines lists/detail/order history and amount/review components; broker credentials are synthetic empty inputs.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Account-link/signature prerequisites, price freshness, order settlement and broker handoff require caller recovery; no exchange or broker is contacted.

**Requirements output:** Fund units versus currency, prices/dates/order status, agreement fields and proposed future investment records/services with encrypted joins Unknown.

**Acceptance beyond the common contract:** List/detail, invalid quantities/amounts, stale/error fixture states, order cancellation preview and Back pass without investment orders. **Effort:** 4–6 days. **Evidence confidence:** medium-low.

### FR-28 — Crowdfunding plans and participation preview

**Status:** proposed. **Families:** F-053. **Existing nodes:** S-395–S-400 (6 primary allocations). **Dependencies:** FR-02, FR-27.

**What opens afterward:** Inspect plan list/detail/agreement and participation amount/review previews.

**Frontend scope:** Combine the small six-record investment journey, reusing reviewed investment list/detail/amount elements.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Plan availability, risk disclosures, investment bounds and confirmation prerequisites are server-dependent; do not fabricate returns or accepted eligibility.

**Requirements output:** Synthetic plan/detail/agreement records, integer amount/unit and proposal for future plan/participation services.

**Acceptance beyond the common contract:** Closed/empty/open fixture plans, invalid bounds, agreement correction and Back pass; no participation is registered. **Effort:** 2–3 days. **Evidence confidence:** medium.

### FR-29 — Digital-signature enrollment and document states

**Status:** proposed. **Families:** F-051. **Existing nodes:** S-366–S-382 (17 primary allocations). **Dependencies:** FR-05.

**What opens afterward:** Inspect certificate/menu/document lists, enrollment conditions/information/agreement, video/sign capture presentation and pending/result/detail states.

**Frontend scope:** Combine one certificate lifecycle with shared pending/list/detail components; capture permissions and local test inputs are isolated from real credentials.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; 16 of 17 nodes are Inferred; multiple pending/list variants and priority-Back wrapper require content/caller recovery. No valid certificate or cryptographic signature is produced.

**Requirements output:** Certificate/document/identity/permission state, enrollment fields and E-117–E-129 candidates; raw key material is never a fixture.

**Acceptance beyond the common contract:** Denied permission, rejected/pending/expired fixture states, capture cancellation and nested Back pass; review/result cannot imply legal signing. **Effort:** 4–6 days. **Evidence confidence:** low-medium.

### FR-30 — Promissory-note inventory and details

**Status:** proposed. **Families:** F-049. **Existing nodes:** S-340–S-344, S-359–S-362 (9 primary allocations). **Dependencies:** FR-29.

**What opens afterward:** Inspect promissory menu, owned-note entry/list/result/detail and the recovered sub-flow boundaries.

**Frontend scope:** Combine read-only note discovery/detail and host/container lifecycle, not separate pages for S-360–S-362.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Ownership lookup keys, note status taxonomy and result-versus-list semantics need caller/model recovery; recovered route names do not establish full contracts.

**Requirements output:** Synthetic note/ownership/status records, input/lookup fields and signature prerequisites; current API map has no dedicated F-049 service group.

**Acceptance beyond the common contract:** Empty/error/owned-note fixtures, identifier validation and container Back/recreation pass without creating or endorsing a note. **Effort:** 2–4 days. **Evidence confidence:** medium.

### FR-31 — Promissory-note issue and guarantee preview

**Status:** proposed. **Families:** F-049. **Existing nodes:** S-345–S-358 (14 primary allocations). **Dependencies:** FR-05, FR-15, FR-29, FR-30.

**What opens afterward:** Inspect note issue/guarantee amount, party/address, agreements, prevalidation, draft/payment and result states.

**Frontend scope:** Combine the two related note operation subflows with shared party/address/agreement/review UI while preserving distinct issue/guarantee rules.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Legal terms, fee rules, signature timing, guarantee-party eligibility and draft/result state transitions need caller evidence; no note is legally issued or guaranteed.

**Requirements output:** Exact integer face value/unit, parties/addresses, note/draft/status records, validation and future service proposals; encrypted contracts remain Unknown.

**Acceptance beyond the common contract:** Missing party/address, invalid amount, failed prevalidation, correction, interrupted draft and Back pass; payment/signature remains local preview. **Effort:** 4–6 days. **Evidence confidence:** medium-low.

### FR-32 — Wage-advance onboarding and dashboard

**Status:** proposed. **Families:** F-056. **Existing nodes:** S-410–S-416, S-420, S-423–S-425 (11 primary allocations). **Dependencies:** FR-02.

**What opens afterward:** Inspect service entry, activation conditions/account/agreement/rating, dashboard and history.

**Frontend scope:** Combine one product’s activation/read-only state; nested subgraphs are state boundaries, not extra screens.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Product purpose is Inferred at inventory level. Employer eligibility, rating, limit/availability and active-versus-inactive routing need caller/model traces.

**Requirements output:** Synthetic enrollment/employer/limit/history fixtures, account and consent inputs, unit and eligibility-state provenance.

**Acceptance beyond the common contract:** Ineligible/inactive/active/error fixture paths, account cancel and Back/recreation pass; activation is not sent to a service. **Effort:** 3–5 days. **Evidence confidence:** medium-low.

### FR-33 — Wage-advance request and settlement previews

**Status:** proposed. **Families:** F-056. **Existing nodes:** S-417–S-419, S-421–S-422 (5 primary allocations). **Dependencies:** FR-32.

**What opens afterward:** Inspect advance amount/agreement/review and settlement amount/receipt preview.

**Frontend scope:** Combine the two short monetary subflows using the dashboard’s local limit/context and shared amount/review components.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Available advance/settlement bounds, fees, deductions and settlement receipt semantics need caller evidence; no payroll rule is invented.

**Requirements output:** Integer amount/unit, limit/outstanding records, agreements, validation and proposed future request/settlement services.

**Acceptance beyond the common contract:** Bounds/insufficient availability, correction, cancellation and Back pass; no request, deduction or settlement is submitted. **Effort:** 2–3 days. **Evidence confidence:** medium.

### FR-34 — Credit-product guide and onboarding

**Status:** proposed. **Families:** F-055. **Existing nodes:** S-406–S-409 (4 primary allocations). **Dependencies:** FR-02, FR-13.

**What opens afterward:** Inspect product guide/onboarding and recovered main state with explicit eligibility/error fixtures.

**Frontend scope:** Combine the small four-record product family; recover content and entry rules before assigning any purchase/credit behavior.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Inventory purpose and three routes are Inferred. Product terms, wallet linkage and whether main is a container require recovery; unavailable content remains explicit.

**Requirements output:** Product/terms/eligibility fixture proposal and known input/state fields; current API map has no dedicated F-055 group.

**Acceptance beyond the common contract:** Guide paging/Back and eligible/ineligible fixture state render; no credit application or invented purchase flow is added. **Effort:** 2–4 days. **Evidence confidence:** low.

### FR-35 — Loan overview, details and repayment preview

**Status:** proposed. **Families:** F-048. **Existing nodes:** S-288, S-306–S-307, S-309, S-324, S-330–S-331, S-338 (8 primary allocations). **Dependencies:** FR-02, FR-11.

**What opens afterward:** Open loan menu/list/group/summary/detail and inspect repayment confirmation variants.

**Frontend scope:** Combine read-only obligations and the short repayment presentation; preserve distinct confirmation variants until source/content equivalence is proved.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Loan categories, repayment-source choice, outstanding/late-fee calculation and group visibility need traces; F-048’s large package is not one implementable journey.

**Requirements output:** Loan/installment/status/summary records, integer principal/due/fee unit, repayment inputs and caller-backed candidate endpoints.

**Acceptance beyond the common contract:** No-loan/multiple/overdue/error fixtures, list/detail return and review correction pass; no repayment occurs. **Effort:** 3–5 days. **Evidence confidence:** medium.

### FR-36 — Child-loan application and guarantor preview

**Status:** proposed. **Families:** F-048. **Existing nodes:** S-289–S-301 (13 primary allocations). **Dependencies:** FR-29, FR-31, FR-35.

**What opens afterward:** Inspect conditions/calculator, applicant/guarantor/document inputs, inquiry/prevalidation, promissory selection and result preview.

**Frontend scope:** One complete child-loan family uses local applicant/guarantor/note/document fixtures and recovered stepper/validation.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Eligibility law/rates/ceilings, document rules and guarantor inquiry are service/legal dependencies. Never turn synthetic conditions into recovered policy.

**Requirements output:** Per-step fields, document descriptors, integer financial units, consent/guarantor/note requirements, error taxonomy and future service proposal.

**Acceptance beyond the common contract:** Missing documents/guarantor, invalid inputs, failed inquiry, calculation fixtures and Back correction pass; no loan request or legal operation is sent. **Effort:** 4–6 days. **Evidence confidence:** medium-low.

### FR-37 — Marriage-loan application and spouse/guarantor preview

**Status:** proposed. **Families:** F-048. **Existing nodes:** S-310–S-323 (14 primary allocations). **Dependencies:** FR-29, FR-31, FR-35, FR-36.

**What opens afterward:** Inspect marriage-specific conditions/calculator/spouse/customer/guarantor, inquiry/prevalidation, note selection and review/result previews.

**Frontend scope:** Reuse proven loan components while implementing distinct spouse and application fields; do not copy child-loan rules blindly.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Spouse/identity linkage, eligibility, legal conditions and duplicate/combined applicant behavior need caller recovery and synthetic scenarios.

**Requirements output:** Marriage/spouse/guarantor/document records, validation, integer amounts and exact unknown service payloads; no real identifiers.

**Acceptance beyond the common contract:** Spouse/guarantor errors, condition/amount changes, inquiry failures and stepper Back pass with retained values; no application submitted. **Effort:** 4–6 days. **Evidence confidence:** medium-low.

### FR-38 — Company and hub loan requests

**Status:** proposed. **Families:** F-048. **Existing nodes:** S-287, S-302–S-305, S-336–S-337, S-339 (8 primary allocations). **Dependencies:** FR-35.

**What opens afterward:** Inspect company loan list/request/review/receipt and recovered hub/welcome/request confirmation/result states.

**Frontend scope:** Combine related institutional/hub request family after tracing whether shared request states truly belong to it; otherwise keep them unresolved.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; S-287/S-336/S-337/S-339 ownership is a planning allocation, not a proved common route. Organization authority and hub eligibility/contracts need focused caller tracing.

**Requirements output:** Company/request/authority fields, terms, result records and actual caller-backed allocation; reassign only with graph evidence.

**Acceptance beyond the common contract:** Company selection, missing authority, request correction and Back work; no invented join or successful request is added if recovery remains incomplete. **Effort:** 3–5 days. **Evidence confidence:** low-medium.

### FR-39 — Other loans, calculation and promissory/guarantor states

**Status:** proposed. **Families:** F-048. **Existing nodes:** S-285–S-286, S-308, S-325–S-329, S-332–S-335 (12 primary allocations). **Dependencies:** FR-31, FR-35, FR-38.

**What opens afterward:** Inspect remaining loan type/detail/entry/confirmation/result, shared agreement/customer/calculator and loan-note guarantor states.

**Frontend scope:** Consolidate remaining related loan forms and loan-specific promissory handoffs using recovered prerequisites rather than generic pages.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Shared agreement/customer/calculator nodes may serve several subflows; ownership, loan-type fields and note-draft lifecycle remain recovery gates.

**Requirements output:** Per-type input/condition/guarantor/note-draft matrix, units and caller-backed shared-state ownership; future mock records are proposals.

**Acceptance beyond the common contract:** Each recovered loan type has distinct validation/fixtures; guarantor/note correction and Back preserve state; no unproved type or submission is implemented. **Effort:** 4–6 days. **Evidence confidence:** low-medium.

### FR-40 — Merchant terminal requests and management

**Status:** proposed. **Families:** F-064. **Existing nodes:** S-469–S-482 (14 primary allocations). **Dependencies:** FR-02, FR-05.

**What opens afterward:** Inspect terminal menu/list, installation/business/document requirements, request review/tracking, account change and deactivate preview.

**Frontend scope:** Combine one terminal-management journey and its shared business/address/account forms; two subgraphs remain containers.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Feature purpose is Inferred at inventory level; terminal eligibility, document obligations, deactivate side effects and tracking states need caller/model recovery.

**Requirements output:** Synthetic merchant/terminal/request/document records, account/address inputs, authority/eligibility and future service proposals.

**Acceptance beyond the common contract:** New/existing/ineligible terminal fixtures, missing docs, correction, tracking and Back pass; no terminal request/account change/deactivation occurs. **Effort:** 4–6 days. **Evidence confidence:** medium-low.

### FR-41 — Intro, launcher/router and login presentation

**Status:** proposed. **Families:** F-001, F-002, F-015, F-017, F-018. **Existing nodes:** F-001, F-002, S-027–S-029, S-048–S-050 (8 primary allocations). **Dependencies:** FR-02, FR-05.

**What opens afterward:** Inspect recovered welcome/login/activation/first-run account selection and return to the replica demo identity.

**Frontend scope:** Combine the small access shell, local input/OTP state and first-run selectors. Preserve distinct replica-only demo entry and never request production credentials.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Router conditions, login fallback/lockout, activation/first-run flags and intro paging need recovery; this is visual/interaction replication, not new authentication.

**Requirements output:** Local session/first-run states, credential/OTP inputs, M-003/M-011/M-013/M-029 candidates and security boundary.

**Acceptance beyond the common contract:** Empty/invalid/locked/OTP-expired local states, Back and recreation pass; no real login, token storage, production call or exported deep link exists. **Effort:** 3–5 days. **Evidence confidence:** medium.

### FR-42 — Registration, recovery and identity-check presentation

**Status:** proposed. **Families:** F-016, F-019. **Existing nodes:** S-030–S-047, S-051 (19 primary allocations). **Dependencies:** FR-05, FR-41.

**What opens afterward:** Inspect registration/recovery stepper, agreements/auth methods/card/national-ID/user-password forms and identity/camera states.

**Frontend scope:** One access-enrollment family combines duplicate legacy/current content after proving equivalence; lifecycle-attach destinations are not pages.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; S-034 is Unknown; authentication-method duplicates, eKYC camera/provider flow and identity validation require deep content/caller traces. Do not invent a verification success.

**Requirements output:** Per-variant fields, consent, document/camera permissions, OTP and synthetic identity states; exact encrypted enrollment contracts stay Unknown.

**Acceptance beyond the common contract:** Format errors, method changes, denied camera, interrupted/failed identity state and Back retain input; no registration, upload or real identity verification occurs. **Effort:** 5–7 days; split if current/legacy flows differ materially. **Evidence confidence:** low-medium.

### FR-43 — Account-opening applications

**Status:** proposed. **Families:** F-020. **Existing nodes:** S-052–S-062 (11 primary allocations). **Dependencies:** FR-02, FR-05, FR-41, FR-42.

**What opens afterward:** Inspect account type/information/agreement, branch/contact/additional/document selection and application result preview.

**Frontend scope:** Combine current and in-app account-opening variants only where content evidence supports shared forms; retain their separate stepper/result conditions.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Account-type eligibility, branch policies, identity/document completeness and inferred route result arguments need recovery.

**Requirements output:** Opening inputs, branch M-057 candidate, identity/contact/address/document prerequisites, integer opening amount/unit if evidenced, future application records.

**Acceptance beyond the common contract:** Invalid/missing contact/docs, branch selection, agreement correction and Back pass; no bank account is created and no invented branch page is added. **Effort:** 4–6 days. **Evidence confidence:** medium.

### FR-44 — Postal correspondence and delivery selection

**Status:** proposed. **Families:** F-063. **Existing nodes:** S-465–S-468 (4 primary allocations). **Dependencies:** FR-05.

**What opens afterward:** Inspect letter list, selected delivery address and sending-type preview.

**Frontend scope:** Combine four small correspondence records using the saved-address picker and local delivery-option fixtures.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; Letter categories, availability, delivery pricing and current/legacy route semantics need caller/content recovery; no postal dispatch is inferred.

**Requirements output:** Letter/address/delivery/fee fields, unit if applicable and future correspondence-service proposal; current API map has no dedicated F-063 group.

**Acceptance beyond the common contract:** Empty/list, missing address, sending-type selection/cancel and Back pass; no letter, delivery or external launch is sent. **Effort:** 1–3 days. **Evidence confidence:** medium-low.

### FR-45 — Unresolved destinations and shared-surface coverage closure

**Status:** proposed. **Families:** F-000, F-035, F-039, F-045, F-069. **Existing nodes:** S-001–S-011, S-013, S-015, S-017–S-024, S-148, S-194–S-195, S-236–S-237 (26 primary allocations). **Dependencies:** FR-01–FR-44; recovery may run within each earlier approved issue.

**What opens afterward:** Open only additionally identified shared/image/menu/scanner or feature content after its identity and caller relationships are recovered; see an explicit unresolved ledger for everything else.

**Frontend scope:** Focused REA caller/resource/content recovery for remaining unassigned/Unknown nodes; move proven content into its owning family batch rather than create generic placeholder pages.

**Evidence / specific gaps:** Catalogue/graph and existing private route/content map; 18 unassigned activities plus five feature activities have no recovered screen identity. Shared chooser/menu/scanner nodes also lack ownership; dynamic targets and 139 source-unattributed calls remain unresolved.

**Requirements output:** Per-node recovery result, callers/prerequisites, deduplication/ownership decision and explicit blocked reason; no API, page or route fabricated from a class name.

**Acceptance beyond the common contract:** Every graph node has implementation evidence, a proved duplicate/container role, or explicit unresolved/deferred record; graph validation passes and no unknown destination is counted reconstructed. **Effort:** 3–7 days investigation initially; implementation cannot be estimated until identified. **Evidence confidence:** low; unresolved content is gated.

## Coverage mapping

This is an exhaustive allocation of the current inventory, not an implementation-completeness claim. **All 51 feature families F-014–F-064 and all 485 graph nodes have a primary batch allocation.** Nodes in split families are assigned by the exact lists above; their union is checked against the graph with no missing, extra or duplicate node IDs. Cross-family component reuse does not add a second primary allocation. The current 19 subgraphs remain in their owning batch; they do not earn standalone screen coverage.

| Feature family | Catalogue nodes | Allocated batch(es) |
|---|---:|---|
| F-014 Home / dashboard | 1 | FR-01 |
| F-015 Login | 3 | FR-41 |
| F-016 Registration / sign-up | 18 | FR-42 |
| F-017 Intro / welcome | 2 | FR-41 |
| F-018 Device/app activation | 1 | FR-41 |
| F-019 Identity verification (eKYC) | 1 | FR-42 |
| F-020 In-app account opening | 11 | FR-43 |
| F-021 User profile | 7 | FR-02, FR-03, FR-05, FR-07 |
| F-022 Settings | 3 | FR-05 |
| F-023 Dashboard storefront settings | 1 | FR-05 |
| F-024 Changelog / what's new | 1 | FR-05 |
| F-025 Notifications / inbox | 1 | FR-06 |
| F-026 Support / tickets | 7 | FR-06 |
| F-027 Quick-action shortcuts hub | 2 | FR-07 |
| F-028 Accounts | 1 | FR-02 |
| F-029 Account statement | 3 | FR-02 |
| F-030 Transactions history | 5 | FR-02, FR-09 |
| F-031 Receipts | 7 | FR-08, FR-09, FR-11, FR-12 |
| F-032 Wallet | 19 | FR-13, FR-14 |
| F-033 Bank cards | 20 | FR-03, FR-04a, FR-04b, FR-11 |
| F-034 Wallet-linked card | 5 | FR-04c |
| F-035 Cheques | 12 | FR-19, FR-20, FR-45 |
| F-036 Chequebook management | 3 | FR-19 |
| F-037 National digital-cheque integration | 3 | FR-20 |
| F-038 Cheque acceptance / payment | 13 | FR-21 |
| F-039 Money transfer | 23 | FR-08, FR-09, FR-10, FR-45 |
| F-040 Saved/trusted destinations | 3 | FR-07 |
| F-041 Recurring transfers | 2 | FR-10 |
| F-042 IBAN / account-number conversion | 4 | FR-23 |
| F-043 Bill payment | 6 | FR-11 |
| F-044 Mobile top-up | 6 | FR-12 |
| F-045 Payment gateway / balance top-up | 21 | FR-15, FR-45 |
| F-046 Road-toll payment | 44 | FR-16, FR-17, FR-18 |
| F-047 Charitable donations | 3 | FR-12 |
| F-048 Loans / credit facilities | 55 | FR-35, FR-36, FR-37, FR-38, FR-39 |
| F-049 Promissory notes | 23 | FR-30, FR-31 |
| F-050 Guarantees / sureties | 3 | FR-22 |
| F-051 Digital signature | 17 | FR-29 |
| F-052 Brokerage / securities | 12 | FR-27 |
| F-053 Crowdfunding | 6 | FR-28 |
| F-054 Insurance | 5 | FR-11 |
| F-055 Branded credit / BNPL product | 4 | FR-34 |
| F-056 Wage-advance / earned-income service | 16 | FR-32, FR-33 |
| F-057 Loyalty / rewards club | 8 | FR-25 |
| F-058 Personal finance management | 6 | FR-26 |
| F-059 Inquiries / status checks | 14 | FR-22, FR-24 |
| F-060 Referral / advocacy program | 8 | FR-25 |
| F-061 Agent / proxy management | 1 | FR-25 |
| F-062 Saved addresses | 2 | FR-05 |
| F-063 Postal correspondence | 4 | FR-44 |
| F-064 Merchant / market service | 14 | FR-40 |

| Other inventory surface | Allocation / explicit disposition |
|---|---|
| F-001 launcher; F-002 router | FR-41; distinct replica demo entry remains available. |
| F-003–F-005 push/sign-in components; F-008–F-010 profiling/crash/analytics infrastructure | Deferred from frontend replication: SDK infrastructure, not user-facing pages. No production messaging, telemetry, uploads or real sign-in is introduced. |
| F-006 preview tooling | Excluded from user-facing replication: development component, not an app journey. |
| F-007 file-sharing provider | Recover the sharing UI/result expectations in each owning receipt/document batch; actual external sharing needs an explicitly approved later scope and sanitized local files. No independent page. |
| F-011 no deep links; F-012 no app shortcuts | Preserve absence; launcher entry only. F-027 concerns in-app shortcuts and belongs to FR-07. |
| F-013 runtime receivers | Deferred infrastructure until an evidenced frontend state requires them; no separate page or speculative receiver. |
| F-065 bottom navigation | Home shell FR-01; account/card/profile content FR-02/FR-03/FR-05 after tab identity recovery. The central QR control is investigated in FR-14, not assumed a tab. |
| F-066 dashboard menu | FR-01 renders the supplied deterministic configuration. Feature journeys remain in their family batches. Unseen server configurations/targets remain unresolved. |
| F-067 Compose hosts; F-068 app-named routes; F-070 nested graphs/dialogs | Cross-cutting evidence used by each owning batch. No separate implementation step per host/dialog/container. |
| F-069 computed/obfuscated routes | Recovery gate within owning batch; remaining unresolved identity/targets are audited in FR-45. No guessed page or edge. |
| F-071 absence of stock drawer/tab widgets | Preserve recovered custom shell; do not invent drawer or standard-widget navigation. |
| F-000 remaining unassigned | S-025 is allocated to FR-05 with a caller-ownership gate; S-001–S-011, S-013, S-015 and S-017–S-024 remain in FR-45. S-014/S-016 were recovered as statement content and moved to FR-02, and S-012 as card information moved to FR-03, each with owner approval. Allocation is not proof of reachability. |
| Dynamic edges / unattributed call sites | Explicitly unresolved: 43 null-target edges and 139 additional source-unattributed call sites. Recover within the owning approved batch; retain unresolved records where evidence is insufficient. No synthetic pages or invented transitions. |

Coverage audit HC-PLAN: 51/51 feature families assigned; 71/71 inventory rows assigned or explicitly deferred/excluded above; 485/485 graph nodes allocated exactly once; 0 missing/extra/duplicate IDs. This audit measures planning coverage only. It does not certify 485 reconstructed pages or recovered API contracts.

## Recommended next approval and stopping point

Approve **FR-04c — Wallet-linked card journeys** next. It completes the card family with the wallet card issue, send type, address and PIN content, reusing the FR-04a/FR-04b request, OTP and labelled-result patterns. FR-04c remains proposed and does not start automatically.

Small tasks already consolidated include account list/details/statement/history, card issue/PIN/address variants, profile/settings/addresses, notifications/support, saved destinations/shortcuts, bill/insurance installments, mobile top-up/charity, wallet history/details, rewards/referral/agent history and postal delivery/address selection. Larger wallet, vehicle, cheque, promissory, wage-advance and loan families are split along distinct data/state journeys. This avoids both one-step-per-icon planning and a single oversized “all banking screens” issue.

FR-01 is done. FR-02, FR-03, FR-04a and FR-04b acceptance and rendered review are recorded in their reports. FR-04c and FR-05–FR-45 remain proposed. FR-45 preserves unresolved destinations; it does not authorize inventing their UI. Stage 1 closes only after supported whole-app coverage and explicit unknowns are reviewed. D-020 Stage 2 owner graph validation, Stage 3 bridge/interactive graph, later feature selection/mocks/assistant and app-team integration retain their existing stages. This document does not authorize or start them.
