# Individual transfer requirements (FR-08)

Approved issue [#60](https://github.com/Hooman1996/RagBot-Assistant/issues/60).
F-039 forms and F-031 receipts are frontend previews only. Labels below distinguish
**Recovered** static evidence, **Designed for mock** behavior and **Unknown**
service/runtime behavior. IT-CARD and IT-ACCOUNT identify private focused REA,
resource and caller ledgers under `local/evidence/individual-transfers/`.
The original application was never run and no banking service was contacted.

## Surfaces and caller contracts

| Existing node / purpose | Recovered incoming context and interactions | Current fixture / deliberate preview behavior | Future requirements; confidence / preliminary effort |
|---|---|---|---|
| S-174 anonymous card transfer | Anonymous service registry selects this variant by configuration; inherited source picker, destination, scan/contact/favorites controls, amount in IRR, Continue/Cancel | `CardTransferFixture` has one disclosed source; shape-only destination checks and a synthetic owner; unavailable scan/contact/favorites | Eligible source cards, destination inquiry, ownership and validation before review. High identity/fields, medium appearance; 1 day |
| S-175 card transfer without source picker | Same anonymous registry selects this alternative; editable source/destination, scanners, amount in IRR | Independent `CardTransferDraft`; neither this identity nor its name proves the authenticated Home quick-tile route | Preserve caller/configuration and source entry mode; inquiry and source eligibility. High identity, unknown fixed Home caller; 0.5–1 day |
| S-176 card review | Transfer info: job identity, retrieval reference, source PAN, integer amount, destination PAN/owner; flags for familiar destinations, secure data, source mode and repetitive origin | Synthetic source/recipient/amount summary; dynamic/static password choices, CVV2, expiry, password, note and local favorite checkbox. OTP, attachments and final submission unavailable | Inquiry/job correlation, OTP challenge/errors, upload constraints and user-only authorization boundary. Never connect assistant handlers to authorization. High fields/caller; 1–2 days |
| S-093 card receipt | Original final financial result or unresolved result context; receipt source and repetitive-origin flag | Independent `CardTransferFixture` sample, explicitly synthetic; trace/share unavailable; no submit-to-receipt transition | Typed result/status/receipt and unresolved-outcome presentation. High identity, medium conditional fields; 0.5 day |
| S-193 transfer menu | Incoming menu with children; selected child dispatches by exact registered key | Two synthetic menu rows for internal/interbank; order/availability local | Eligible menu children and registry, preserve title/context. High dispatch identity; 0.5 day |
| S-192 internal transfer | Source-account picker; optional destination/amount, job and normal/recurring context; this batch covers normal mode only | `AccountFixture` sources and per-mode `AccountTransferDraft`; destination, amount, synthetic reason selector, optional identifiers/descriptions | Source ownership/eligibility, destination owner inquiry, reason records and service errors. High structure/validation; 1 day |
| S-191 interbank transfer | Source picker, menu/job/mode context, IBAN inquiry, limits and reason selection; normal mode only | Same fixture source picker, IBAN arithmetic checks, explicit unavailable owner/allowance/limits; no invented Paya/Satna choice | Inquiry allowance/owners, limits and eligibility, reason/type selection and exact unit contracts. High caller/model joins, unknown bank policy; 1–2 days |
| S-173 account review | Internal or IBAN transfer info plus transfer type/flags; original final result later launches receipt | Selected source, destination, amount, reason, identifiers/descriptions and note retained; favorite checkbox local; attachment/submission unavailable | Preserve exact inquiry/result identity, conditional fields and user-only outcome. High caller, medium conditional rendering; 1 day |
| S-098 internal receipt | Financial result, receipt source and internal transfer info | Independent labelled synthetic sample in `AccountTransferScreens`; no successful transaction claim | Status/description, source/destination, amount, date, trace and conditional metadata. High identity; 0.5 day |
| S-097 IBAN receipt | Financial result, transaction type, receipt source and optional request context | Independent labelled sample; share unavailable | Same fields plus bank/destination description and conditional central-bank reference. High identity; 0.5 day |

Effort is preliminary future mock/frontend work, excluding real bank integration
and authorization design. Existing ten allocations and all graph IDs remain
unchanged. S-194/S-195 remain unresolved and unregistered.

## Entry and result relationships

Recovered Home **all-services/menu** selection passes a menu into the exact-key
registry. Transfer-menu keys resolve S-193; its internal/interbank child keys
resolve S-192/S-191. This is separate from the existing fixed Home quick tiles:
their transfer callers remain **Unknown**. The current replica has no recovered
all-services bottom-sheet caller UI, so S-193 uses a labelled independent review
entry. No quick tile is reassigned based on its label. S-174/S-175 likewise have
independent review entries; their anonymous registry is not an authenticated
Home caller. Normal form/review callbacks preserve selected identities and Back.

Both account forms call a legacy familiar-destination launcher, not FR-07's
LOAN-only S-198 picker. Its exact result mapping is incomplete; the control
honestly reports unavailable. No loan result is repurposed as an account/card.
Original receipts follow service-backed outcomes. Replica receipts are separate
review entries, never reachable by pretending local submission succeeded.

## Validation, units and state

Recovered card fields use 16-digit source shape, destination validation and
source/destination inequality; exact destination checksum/bank rules remain
Unknown. Amount UI explicitly says **IRR/rial**, with 11 digits maximum. The
amount decompile is contradictory, so its bank rule is not claimed recovered.
**Designed for mock:** normalized Persian/Arabic digits, 16-digit card shape,
positive integer amount and synthetic owner summary. Fixture PANs deliberately
do not claim valid bank checksums. `TransferRules.amountRial` parses exact Long
integers, rejects overflow/decimals/separators/signs and performs no conversion.
Eleven digits is a recovered card field constraint, not a bank transfer limit.

Recovered internal destination validation requires 13 ASCII digits and inequality
with source. Existing synthetic source accounts have 10 digits; they are reused
without changing older fixtures or claiming bank validity. Interbank recovery
requires 24 IBAN digits, rejects all-equal values and checks mod-97. The preview
normalizes digits and accepts an optional IR prefix, deliberately rejecting the
original utility's permissive suffix/embedded-character extraction. Arithmetic
does not prove a bank or recipient exists. Positive account amounts and two
synthetic reasons are mock guards; no bank limits or fees are invented.

Recovered optional fields: payment identifier up to 30 numeric characters,
withdrawal identifier up to 17, source/destination descriptions up to 50 each.
The replica uses an expandable inline editor instead of the original sheet.
Non-secret drafts, source identity, mode, notes, checkbox, scroll and editor
state survive Back/recreation. Card variants and account modes retain separate
drafts. Cancelling source/reason dialogs leaves the draft unchanged. Card CVV2,
password and expiry use transient memory only and clear on recreation/Back;
there is no save-secure-data control. No OTP or account credentials are stored.
Original retention/reset rules after service errors/process death remain Unknown.

## Candidate models and future services

| Candidate | Caller-supported relationship | Remaining contract gaps |
|---|---|---|
| M-025 | Card step-one response consumed before S-176; destination account name/number, retrieval reference and system trace | Existing inventory summary lists different fields. Focused class/caller evidence supports these consuming fields; inventory is not silently treated as a wire schema |
| M-033 | Card dynamic-password request response; description, status, trace and timestamp fields | Inventory summary appears to contain M-025 fields instead. Exact encrypted request/response and OTP lifecycle remain Unknown |
| M-031 | Card/account final financial result; consumed by review outcome callbacks and S-093/S-097/S-098 | Status/description, receipt fields and conditional central-bank reference are supported; encrypted inner HTTP contract, authorization and unresolved-result policy remain Unknown |
| M-034 | S-191 inquiry repository result; allowance, account owners and description; caller supplies job, destination payment identifier and IBAN with IR | Exact endpoint join, encrypted serialization, errors and ownership policy remain Unknown |
| M-036 | S-191 limits repository result; group/id/name and integer item value | Units and dynamic eligibility/type/range semantics need a complete contract; no invented thresholds |

Candidate E-469/E-519 aggregate signatures do not establish these repository
joins. No endpoint is assigned by model resemblance. Encrypted envelopes remain
**Unknown**, and no transport contract or inventory is rewritten in this batch.

Proposed future mock records are source eligibility, recipient inquiry, reason
choices, limits with explicit units, correlated transfer drafts, OTP challenges,
attachments and financial outcomes/receipts. These require approved typed service
contracts and user-only authorization boundaries. FR-08 implements no HTTP
service, transaction request, database, permission prompt or external launcher.

## Unimplemented and uncertain states

All populated records, generic empty/loading/error/retry states and persistence
are **Designed for mock**, controlled outside replicated content. Original
service-error families, menu eligibility, recurring flags, bank fees/limits,
attachment rules, expiry picker geometry, source carousel and exact conditional
receipt/status rendering remain incomplete. Share/download/scans/contacts,
remote favorites, OTP and submission show unavailable. Private overlay/font
provenance remains SD-ASSETS (84 resources); public substitutes remain disclosed.
Original-app fidelity is unscored; earlier batch fidelity gaps are unchanged.
