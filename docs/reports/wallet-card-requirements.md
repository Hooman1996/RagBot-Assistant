# Wallet-linked card requirements (FR-04c)

Approved issue [#52](https://github.com/Hooman1996/RagBot-Assistant/issues/52).
This report separates recovered frontend requirements, implemented local preview
behavior and future service proposals. It does not define a banking API or bank
database, and it does not authorize another frontend batch.

## Identity and evidence boundaries

The five FR-04c nodes are wallet-card address `S-137`, issue host `S-138`,
issue confirm `S-139`, send type `S-140` and wallet set/change PIN `S-141`.
`S-138` is a container for the other three and gets no page of its own.

**Recovered (WC-ROUTES, WC-TAB, WC-FLOW, WC-PIN):** the wallet router opens
`S-138` for the first-card key and `S-141` for the PIN key. A generic issue key
opens shared menu list `S-002`, and verify-delivered opens unassigned `S-004`;
both are FR-45. The card tab's wallet group has its own card face, a separate
wallet-balance call and copy/share. The issue graph runs send type → address →
confirm. Its draft carries the wallet account number, owner name, integer fee,
the first available card design and the address. Confirm submits and shows a
status sheet with a tracking number. `S-141` reuses the card PIN pattern with
a wallet picker, confirm and OTP.

**Designed for mock:** the wallet card record, notice, fee value, disabled
in-person option, fixed OTP and labelled result.

## Record and amount requirements

| Recovered shape (no assigned M ID) | Role | Fields | Replica representation |
|---|---|---|---|
| Wallet item | Send type | Wallet account number | Wallet card's account field |
| Operation info | Send type | Notice, branch address, in-person-enabled flag | Fixture notice; in-person disabled |
| Owner | Draft | Wallet number, full name | From the wallet card fixture |
| Fee | Draft | Integer amount | `120_000` IRR |
| Card design | Draft | Colour code and id (first available) | Not shown |
| Address | Draft | Id, address, postal code | FR-04a address fixture |
| Wallet balance | Tab | Balance response per wallet card | Card balance field, exact integer IRR |

## Per-destination requirements

| Existing node / family | Purpose; user inputs | Required data and fixture source | Prerequisites and navigation result | Supported API/model references | Proposed future mock records/services | Unknowns; effort/confidence |
|---|---|---|---|---|---|---|
| Wallet group in `S-026` tab; `F-034` | View wallet card and balance; open rows | Wallet card fixture | Wallet chip → rows | Wallet balance read (encrypted) | Wallet card list and balance read | Wallet group order and empty/disabled states **Unknown**. 0.5 day / high |
| `S-140`; `F-034` | Choose wallet and delivery | Wallets, operation info, fee | First-card row → address | Wallet operation info and fee reads | Wallet issue info | Eligibility and in-person rule **Unknown**. 0.5 day / high |
| `S-137`; `F-034`/`F-062` | Choose address | Saved addresses | Required → confirm; add address is FR-05 | Address list read; card colour types read | Shared saved-address read | Address limits **Unknown**. 0.5 day / high |
| `S-139`; `F-034` | Review and register | Draft | Register → labelled result → card tab | Wallet card issue write and status | Wallet issue request with status | Status texts and fee debit **Unknown**. 0.5 day / medium |
| `S-141`; `F-034` | Assign/change wallet card PINs | Wallet cards | PIN row → section → confirm → OTP → result | Wallet PIN send-OTP and verify writes | PIN challenge, no secret storage | OTP expiry and limits **Unknown**. 0.5 day / high |
| `S-138`; `F-034` | Host container | — | Starts at `S-140` | — | — | No visible page. |

Requests and responses travel in the encrypted envelope. The shapes above are
caller parse types, not proven HTTP schemas. Proposed services are our designs,
not recovered bank tables.
