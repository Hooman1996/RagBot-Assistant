# Cards inspection requirements (FR-03)

Approved issue [#46](https://github.com/Hooman1996/RagBot-Assistant/issues/46).
This report separates recovered frontend requirements, implemented local preview
behavior and future service proposals. It does not define a banking API or bank
database, and it does not authorize another frontend batch.

## Identity and evidence boundaries

The nine FR-03 destinations are card information `S-012`, fetch cards `S-068`,
bon card balance `S-119`, card balance without login `S-120`, legacy card list
`S-125`, card management `S-126`, card display status `S-130`, credit card
detail `S-131` and credit file-loan summary `S-134`. The Home card tab is
hosted content of `S-026`, **not** `S-125`; no new graph ID is added for it.

**Recovered:** the tab groups cards into debit, credit, bon, gift and wallet
lists chosen by a server header menu. Action rows are that menu's sub-menus,
routed by key with the current group's card list. Owner-approved corrections:
`S-012` (previously unassigned) is card information and moved from FR-45 to
FR-03; `S-123` card color is reached only inside card issue/modify steps and
moved to FR-04. IDs and graph edges are unchanged. The private route trace also
identifies three other unassigned FR-45 nodes as card-related: a pre-login
balance variant, a familiar-card picker and a received-card confirmation. They
are recorded under CI-ROUTES, remain in FR-45 and are not implemented.

Private evidence IDs CI-TAB, CI-STATE, CI-ROUTES, CI-DATA, CI-S012, CI-S119,
CI-S120, CI-S125, CI-S126, CI-S130, CI-S131 and CI-S134 identify the caller,
view-model, layout and model chains in
`local/evidence/cards-inspection/evidence-index.md`. The original app was not
executed and no original service was contacted. This report contains semantic
IDs and sanitized requirements only.

**Designed for mock:** fixture values, card colours, the configured group set
and rows, preview error/loading scenarios, local retry and the review entries
for `S-126`/`S-068`, `S-130` and `S-120`. Data is bundled in the app, with no
backend service or database. Future services below are proposals only.

## Model and amount requirements

| Existing model | Caller-supported role in this batch | Recovered data/type limits | Required replica representation |
|---|---|---|---|
| `M-026` cards by group | Tab list, `S-131`/`S-134` (first credit card) and `S-068` candidates | Gift/debit/credit/bon/wallet groups, each a list of card records. Card records carry number, owner, default/logo-variant/disabled/inactive flags, colours, status, IBAN, client number, linked accounts and an optional integer balance the tab does not display. The tab's call uses the encrypted envelope (`E-049`/`M-029`); `M-026` is the inner payload shape. | Synthetic grouped records. Keep card selection separate from account selection. Do not show the record's own balance as the card balance. |
| `M-024` card balance | Tab balance per debit card (`E-065`, encrypted; inner `M-024`) | The original amount is floating point and is converted to a whole number for display; an empty response maps to the retry state. | Exact integer `IRR`. Hidden, shown, zero and missing are distinct states. |
| `M-008` card and account | `S-125` rows, `S-126` management rows, `S-012` information | Card number plus account number, IBAN, owner, status, currency and an account balance (floating point in the original). | Exact integer `IRR` account balance, labelled as the list record's own field; `نامشخص` when absent. |
| `M-060` cards with balance | No FR-03 screen caller confirmed | Same group shape as `M-026`. | Not used; join remains **Unknown**. |
| No assigned M ID: credit detail | `S-131` | Card number, owner, state/description, credit limit, remaining credit, used amount, last-statement debt plus instalments, total payable; all integer in the original. | Separate exact integer fields; none derived from another. |
| No assigned M ID: file-loan summary | `S-134` | File number, due date, instalment count, remaining total. | Exact integer `IRR` remaining amount; date shown as text. |

All money uses exact integers with an explicit unit. Converting the original's
floating-point balances needs an explicit adapter contract; this batch does not
guess rounding, scale or sign rules.

## Per-destination requirements

Preliminary effort is for frontend inspection only. Confidence separates
identity/data recovery from visuals and unresolved contracts.

| Existing node / family | Purpose; user inputs | Required data and local fixture source | Prerequisites and navigation result | Supported API/model references | Proposed future mock records/services | Unknowns; effort/confidence |
|---|---|---|---|---|---|---|
| Card tab in `S-026`; `F-033` | Choose a group, swipe cards, show/hide/refresh a debit balance, copy, open rows | `CardFixture` groups, per-group card position, shown-balance set, row configuration | Home card-tab handler. One fetch per Home session; empty group shows the recovered empty text; rows route with the current group's cards. Unsupported rows explain their limit. | `M-026` list via `E-049`; `M-024` balance via `E-065` | Grouped card read with empty/loading/error; per-card balance read with known/zero/missing results | Server group menus, sub-menu order and card colours **Unknown**; skeleton/compact-card visuals not replicated. 1–2 days / high identity, medium visuals |
| `S-012`; `F-033` | Pick a card of the incoming group; copy account/IBAN; share | Card records with primary/other accounts, IBAN, status | Card-information row with the group's type; Back returns to the tab with its state | `M-008` information read by card number (E-ID not confirmed: `E-076` candidate) | Card information read per card | External share and exact default pick **Unknown**. 1 day / high identity, medium join |
| `S-125`; `F-033` | Search by card number | Debit card records with account balance | Debit list row; rows are display-only | `M-008` list (E-ID not confirmed: `E-046` candidate) | Card-and-account list read | Which groups the legacy list includes is **Unknown**. 0.5–1 day / high identity |
| `S-131`; `F-033` | Inspect credit summary; Pay/Cancel | First credit card and its credit summary | Credit detail row; Pay leads to `S-132` (moved to FR-11 in #48), unavailable here; disabled when nothing is payable | Credit detail read by card number (encrypted; no M ID) | Credit summary read per credit card | Repayment amount rules and source-account choice belong to FR-11. 1 day / high fields, medium visuals |
| `S-134`; `F-033` | Inspect file-loan summaries; open a file | First credit card's file rows | File-loan row (menu title); file tap leads to a loan screen outside FR-03, unavailable here | File-loan summary read by card number (encrypted; no M ID) | Credit file summaries per credit card | Loan detail journey belongs to the loan batches. 0.5 day / high |
| `S-119`; `F-033` | CVV2, expiry, static second PIN, save-card option | First bon card number; no secrets stored | Bon-balance row. Required CVV2, length check, PIN ≥5 digits, required expiry; valid submit explains the secure inquiry is unavailable | Bon balance request (encrypted) | None until a secure-inquiry design is approved | CVV2 length rule comes from an obfuscated check; saved-secure-data path and messages **Unknown**. 1 day / medium |
| `S-120`; `F-033` | Card number, CVV2, expiry, dynamic password | None | Pre-login balance menu (not built): labelled review entry; validate only | Pre-login card balance call (encrypted) | Same as `S-119` | Card-number rules and the flag-gated alternate page **Unknown**. 0.5–1 day / medium |
| `S-126`; `F-021`/`F-033` | Choose group segment, search; default/lock/delete | Card records per group with lock state | Profile card management (not built): labelled review entry. Fetch button opens `S-068`; mutations unavailable | `M-026` read; default/lock/delete/add are separate remote writes | Management read; mutation proposals need FR-04/approval | Lock authentication and segment hiding rules **Unknown**. 1 day / high identity |
| `S-068`; `F-021`/`F-033` | Select all / individual candidates; confirm/cancel | Candidate card records; local draft | From `S-126`; confirm (remote add) unavailable, draft kept; cancel/Back returns | Candidate read (`M-026`, `E-054` candidate) and add-cards write | Candidate read only | Add-card validation and results **Unknown**. 0.5 day / high |
| `S-130`; `F-021`/`F-033` | Toggle card display | Current preference (fixture: shown) | Settings card display status (not built): labelled review entry; toggle unavailable and unchanged | Get/set show-card preference (encrypted) | Preference read; write needs approval | Effect on card selection in other journeys **Unknown**. 0.5 day / high |

The joins above follow caller → helper → typed service method evidence. Where
the encrypted envelope hides the payload, the inner model is the caller's
parse type, not a proven HTTP schema. Paths, hosts, menu keys, original class
names and raw request material remain private.

## Input validation and local state

- Card numbers are shown grouped by four in left-to-right order inside the RTL
  layout. Masked server characters pass through. No client masking or unmasking
  is applied.
- Shown balances persist for the session, matching the original view model's
  lifetime. Hide returns a card to hidden. Refresh re-reads the unchanged
  fixture. Retry on a missing balance explains that it is unavailable.
- Group selection and each group's card position survive Back, tab switches and
  recreation. `S-131`/`S-134` deliberately ignore the tab's current card, as the
  original does.
- Secure forms validate locally and never submit. Field values are kept only in
  saved UI state for recreation; nothing is persisted or sent.

## Pending evidence and scope decisions

The owner approved, during implementation, including `S-012`, moving `S-123`
to FR-04, review entries for the off-tab nodes and validate-only secure forms
(recorded on #46). Wallet cards, card issue/reissue, blocking, PIN,
activation, default/lock/delete, adding cards, the display preference, credit
payment and file details are unavailable. They are not claimed complete.

Exact remote contracts, encrypted envelopes, server menus, eligibility, remote
persistence and original visual fidelity remain outside what local fixtures
prove. A proposed record or service is our test design, not a recovered bank
table. No later step is started by this assessment.
