# Wallet overview, QR display and history requirements (FR-13)

Approved issue [#70](https://github.com/Hooman1996/RagBot-Assistant/issues/70).
F-032 wallet main, history menu, statement list and detail, QR display and receipt
are read-only frontend previews. Labels distinguish **Recovered** static evidence,
**Inferred** joins, **Designed for mock** behavior and **Unknown** service/runtime
behavior. WL-RECOVERY keeps the private metadata, decompile, router, smali and
resource ledger under `local/evidence/wallet/`. The original application was never
run and no banking service was contacted.

## Navigation

**Recovered:** the wallet router opens S-111 from the Home fixed-wallet key, S-110
from the transaction-history key, S-115 from the statement key and S-112 from the
receive-barcode key. The six per-type history keys (charge, withdrawal,
wallet-to-wallet, buy, mobile charge, bill) all open the transaction list S-091
built in FR-02. A statement row opens S-114 with the row and wallet number. S-113
is opened by the transaction detail S-090 and FR-14's confirmations, not by an
FR-13 screen. Charge, withdrawal, wallet-to-wallet, buy, bill and mobile-charge
operations belong to FR-14/FR-15.

**Inferred:** Home's **کیف پول** menu item is the fixed-wallet key, so it opens
S-111.

## Surfaces

| Node | Recovered context | Preview behavior | Future requirements; confidence / effort |
|---|---|---|---|
| S-111 | Server menu split into a top action section (charge, withdrawal, wallet-to-wallet) and a row section; wallet cards with title, number (unmasked), owner, "holder type/ status", individual or commercial background and a per-wallet balance shown on request (hidden, progress, value, retry) | Three synthetic wallets; show/hide balance; recovered row targets open; operations unavailable | Wallet list, balance and menu services. High; 0.5 day |
| S-110 | Renders the passed menu's children | Six recovered history keys with synthetic titles, each opening S-091 | Menu configuration. High; 0.25 day |
| S-115 | Active-wallet picker, date range, rows of 10 sorted by newest, next page at the end, file export | Picker, FR-02's date checks, pages of 10 with **نمایش بیشتر**, export unavailable | Statement and export services. High; 0.5 day |
| S-114 | Wallet number, the row card and type-dependent detail rows | Recovered rows over synthetic data | None beyond the statement row. High; 0.25 day |
| S-112 | Active-wallet picker, owner, wallet name, wallet ID with copy, optional amount, QR image, share text or image | Payload shown as local text; amount add/remove; copy notice; share unavailable | QR rendering and sharing. High payload; 0.5 day |
| S-113 | Type-dependent receipt: type and status, amount, source/destination, names, bill fields, time, tracking number, share | One labelled synthetic wallet-to-wallet receipt; share unavailable | Receipt data per type (FR-14). Medium; 0.25 day |

## Rules

**Recovered:**
- More than five wallets show only the first four; otherwise all are shown.
- The QR and statement pickers list `ACTIVE` wallets only.
- QR payload: `{"amount": "<digits>" | null, "destination": "<wallet number>"}`.
  The fast-payment scanner (FR-14) rejects a non-digit destination and drops a
  non-digit amount.
- Statement request: wallet number, from/to dates, page from 1, 10 rows,
  `-transactionDate`. Date checks are the same utility as FR-02's S-016: no future
  date, start before end, start within 366 days, range under 90 days.
- Row: deposit when `crDr` is `CREDIT`, otherwise withdrawal; type, time, tracking
  code and remaining balance.
- Detail: source (deposit) or destination (withdrawal) except for bill and gateway
  payments; name except for gateway payments; trace code; note except for mobile
  credit.

**Designed for mock:** wallets, numbers, balances, menu titles, statement rows and
the receipt; the QR amount accepts digits only.

**Unknown:** wallet creation and eligibility, live menu contents and enabled flags,
wallet number format, balance failures, export formats, QR image and share, and
receipt status codes.

**Units:** balances and amounts are exact Long rial integers.

## Models and services

Recovered client models: the wallet item and its UI form, the balance response, the
statement history request and row, the transaction export request, the wallet
transfer receipt, the QR transaction payload and the transaction type enum. No M/E
join beyond these caller-backed models is claimed. FR-13 implements no HTTP
service, database, scanner, export, share or wallet operation.

## Unimplemented

Balance loading and retry, the charge payment-type sheet (its children are FR-14
flows), QR image rendering, sharing, export, notes and every wallet operation are
not reproduced. Original-app fidelity is unscored.
