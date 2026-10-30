# Saved destinations and shortcuts requirements (FR-07)

Approved issue [#58](https://github.com/Hooman1996/RagBot-Assistant/issues/58).
This matrix separates **Recovered** frontend behavior, **Designed for mock**
local previews, future service proposals and **Unknown** contracts. It does not
define bank APIs or authorize transfer implementation.

## Surfaces and caller contracts

| Feature / existing node | Purpose and incoming context | Inputs and recovered behavior | Local data / display | Future requirements; confidence / preliminary effort |
|---|---|---|---|---|
| F-021 / S-066 | Legacy familiar-destination management; current incoming Profile menu remains unproven | Account, IBAN, card and wallet segments; search appears above five records; add/edit sheets; swipe-revealed edit/delete; delete confirmation; long-press vertical reorder with Apply/Cancel | `SavedDestinationFixture.records`: synthetic identity, type, name, destination and description. List order is local; no ownership inquiry | Typed destination list/inquiry/add/update/delete/order operations, bank/owner metadata, validation and errors. High structure, medium form fidelity; 1–1.5 days |
| F-040 / S-196 | Host selecting management or picker from picker-mode argument | Hosts S-197 or S-198; picker also carries job context | No separate page or registered route | Preserve mode/job arguments in a future caller integration. High identity; included below |
| F-040 / S-197 | Current management, explicitly LOAN only | Search above five records or while query remains nonempty; swipe edit/delete; edit callback is a no-op; delete confirmation | Six synthetic loan destinations; same local record store as picker | Loan destination list/delete with stable identity and job/session context. Editing remains unavailable. High handlers; 0.5 day |
| F-040 / S-198 | LOAN picker, job context from its caller | Search, swipe/delete, select; return selected M-010 as result data and finish; Back returns cancellation | Labelled independent review form receives selected fixture identity; note and previous result retained on cancel | Integrate the real caller only in its approved batch; preserve original draft and consume exactly the returned destination. High result contract; 0.5 day |
| F-027 / S-083 | Shortcut activity host, optional title from menu caller | Hosts S-084; separate start/reorder launcher is not proof of drag UI | No separate registered page | Preserve caller title and refresh semantics. High identity; included below |
| F-027 / S-084 | Service search and pinned list; Home search handler is proven separately | Search field / IME Search, favorites and result headings, per-row pin/unpin and item routing through a dynamic menu registry; empty/no-results illustrations | `ShortcutFixture`: five synthetic candidates, preview keys and two initial pins. Pin changes remain local; unknown menu destinations show unavailable | Eligible candidates, search, pinned list, pin/unpin and trusted menu registry. High structure/handlers, medium service mapping; 0.5–1 day |

SD-FAVORITES and SD-SHORTCUTS identify private caller/content/resource ledgers
under `local/evidence/saved-destinations/`. These are static findings, not
observations of original-app runtime. S-196 and S-083 are host containers;
allocations and graph IDs are unchanged, and only four content IDs register.

Home **search** opens S-084 through the shortcut host with a title. Home's
**bookmark** independently opens recurring transfers S-199, outside FR-07;
it retains its unavailable notice. Neither labels nor synthetic descriptions
are used to infer a server menu key or shortcut eligibility. S-066, S-197 and
S-198 have labelled independent review entries outside replicated content.
The picker harness is **Designed for mock**, not an invented transfer route.

## Models, services and unknown payloads

**M-010** is a caller-supported familiar destination record: `id`, `type`,
`name`, `dest`, `desc`, `orderPos`, `isMine`, `bankId` and `bank`. The local
preview uses a subset and synthetic string IDs; these do not claim to be bank
IDs. Own/null-ID and unsupported destination kinds are excluded from the
legacy list. The enum's additional kinds do not establish support in either
management surface: legacy has four segments; current management/picker use LOAN.

For current loan list/picker, the repository fetch chain reaches **E-419**,
with the picker job context (management uses no job context), and decodes the
response into familiar destination records. Delete reaches **E-421** with the
selected identity. The inventory describes their raw encrypted **M-029**
envelopes; caller parsing into M-010 does not replace the wire envelope or
recover its inner HTTP schema. Exact request serialization, errors, ownership,
permissions and concurrency rules remain **Unknown**.

The inventory's **E-113/E-116/E-480/E-498/E-508** are candidates only; shared
M-010 response/type names do not prove these screens call them. Legacy service
joins must be established independently. Encrypted M-002/M-029 payloads remain
Unknown; no transport contract or API inventory mapping is changed here.

Shortcut callers use a distinct result record containing search description,
menu key and pinned flag, and a pin request containing description/key. M-010
is **not** assigned to shortcuts. No existing E/M ID is assigned without a
verified join. Dynamic menu resolution, server eligibility, capacity, search
minimum length, error text and exact encrypted payloads remain Unknown.

Proposed future mock records/services are destination records with type and
order, destination inquiry/validation results, mutation outcomes and versions;
shortcut candidates/pins with stable preview identities and an explicit menu
registry. These are proposals, not bank tables. No HTTP service, database,
external launcher or banking operation is implemented in FR-07.

## Validation, state and deliberate preview differences

Recovered local checks are 13 ASCII digits for a legacy account and 16
characters for a wallet identifier. The original account sheet additionally
requires an owner inquiry result. This service prerequisite is explicitly
unavailable and bypassed only for labelled synthetic local saves. Card/IBAN
precise checks and type-specific selectors remain Unknown; the preview requires
a nonempty name/value. No fixture lookup proves bank ownership, a valid bank
account or successful remote persistence.

**Designed for mock:** all names/numbers/descriptions, initial ordering and
eligibility; deterministic substring search; shared local records and pins;
local duplicate rejection; generic loading/empty/error/retry scenarios; editable
non-secret drafts and explicit local-save labels. Unknown server duplicate,
order and retention rules are not claimed as recovered. There is no pagination
control because no pagination contract was recovered within this batch.

Saveable UI state preserves selected segment/query, scroll, editor text,
pending confirmation/order edits and applied local record/pin changes through
recreation. Cancel/Back dismisses a confirmation or discards order edits without
mutating records. The independent picker review retains its note and previous
result on cancellation. No passwords or OTP values are introduced. The editor uses a fully expanded,
scrollable sheet so its controls clear the real IME; exact original sheet
geometry remains unscored.

Unsupported loan editing, inquiry-backed validation and synthetic shortcut
routing are explained in place. No transfer target is invented. Original-app
reset/retention behavior after process death, service failures and exact visual
geometry remain **Unknown**, independently of technical acceptance.
