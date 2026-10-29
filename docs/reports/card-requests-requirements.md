# Card operation request requirements (FR-04a)

Approved issue [#48](https://github.com/Hooman1996/RagBot-Assistant/issues/48).
This report separates recovered frontend requirements, implemented local preview
behavior and future service proposals. It does not define a banking API or bank
database, and it does not authorize another frontend batch.

## Identity and evidence boundaries

The five FR-04a destinations are card colour `S-123`, card issue first page
`S-124`, card modify first page `S-127`, operation confirm `S-128` and
operation address selection `S-129`. One draft record travels through them.

**Recovered (CR-ROUTES, CR-FLOW):** the card router opens issue directly and
opens modify with a debit card type for extend, reissue and recolour. A
generic operations entry instead opens a shared menu list (`S-002`, FR-45).
The first page picks delivery; in-person skips the address page. Address leads
to colour, colour to confirm. Confirm fetches a trace identifier, submits the
issue or modify request, and shows a success/failure/pending status sheet with
a tracking number. A failed balance check offers an account top-up. There is
no OTP step.

**Designed for mock:** row configuration, notice, branch, addresses, colours,
fees, phone, tracking number and the labelled local result. Nothing is sent,
stored outside saved UI state, charged or issued.

## Record and amount requirements

| Recovered shape (no assigned M ID) | Role | Recovered fields | Replica representation |
|---|---|---|---|
| Operation info | First pages | Notice, branch, branch address, phone, in-person-enabled flag | Fixture notice, branch and phone; in-person enabled |
| Fee list | First pages → confirm | Event, event code, integer fee amount; looked up by operation | Fixed integer `IRR` fee per operation |
| Delivery option | First pages | Title, subtitle (branch address for in-person), in-person flag, enabled flag | Two options; in-person selected by default |
| Saved address | `S-129` | Identifier, title, address, postal code | Two synthetic addresses |
| Colour type | `S-123` | Identifier, colour code, design family | Three synthetic colours |
| Operation draft | All pages | Operation type, card number or account number, name, fee, in-person flag, address id/text/postal code, phone, colour | `CardRequest` draft (operation, source, delivery, address, colour) |
| Confirm state | `S-128` | Loading, status, sub-description, top-up prompt | Labelled success result only |

Requests and responses travel in the encrypted envelope; the shapes above are
caller parse types, not proven HTTP schemas. Fees use exact integers with an
explicit unit.

## Per-destination requirements

| Existing node / family | Purpose; user inputs | Required data and fixture source | Prerequisites and navigation result | Supported API/model references | Proposed future mock records/services | Unknowns; effort/confidence |
|---|---|---|---|---|---|---|
| `S-124`; `F-033` | Request a new card; choose source account and delivery | Accounts (FR-02 fixture), operation info, fee list | Issue row; in-person → `S-123`, postal → `S-129`; Cancel leaves the flow | Operation info and fee reads (encrypted; no M ID) | Operation info, fee list per operation | Account eligibility for issue **Unknown**. 1 day / high flow |
| `S-127`; `F-033` | Extend, reissue or recolour a debit card; choose card and delivery | Debit cards (FR-03 fixture) | Modify rows with operation type; same branch as `S-124` | Same reads; operation type from menu key | Same, plus card eligibility per operation | Which cards each operation allows **Unknown**. 0.5 day / high flow |
| `S-129`; `F-033`/`F-062` | Choose a delivery address; add an address | Saved addresses | Required selection → `S-123`; add address → `S-463` (FR-05), unavailable | Address list read (no M ID) | Saved-address read shared with FR-05 | Address validation and limits **Unknown**. 0.5 day / high |
| `S-123`; `F-033` | Choose a card colour | Colour list | Required selection → `S-128` | Colour types read by job id | Colour list per operation | Colour sample designs and availability rules **Unknown**. 0.5 day / medium visuals |
| `S-128`; `F-033` | Review and register the request | Draft plus account/card, branch or address | Register → labelled local result; Cancel/result close → card tab | Trace, issue-new and modify-existing writes; trace status read | Draft create/confirm with versioned status (P4, if the feature is picked) | Real status codes, fee debit, failure and pending texts **Unknown**. 1 day / medium |

## Validation and local state

- Address and colour are required before continuing; the first page always has
  a source and a delivery choice.
- Back keeps the draft; Cancel and the result button clear it.
- The labelled result states that nothing was issued, changed or charged.
- No PIN, OTP, card data or address is sent, stored or validated against a
  service.

Exact contracts, eligibility, fees, server option lists and original visual
fidelity remain outside what local fixtures prove. Proposed services are our
designs, not recovered bank tables.
