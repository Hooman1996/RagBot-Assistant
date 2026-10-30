# Card blocking and PIN requirements (FR-04b)

Approved issue [#50](https://github.com/Hooman1996/RagBot-Assistant/issues/50).
This report separates recovered frontend requirements, implemented local preview
behavior and future service proposals. It does not define a banking API or bank
database, and it does not authorize another frontend batch.

## Identity and evidence boundaries

The five FR-04b destinations are pre-login block (legacy) `S-118`, signed-in
block `S-121`, pre-login block v2 `S-122`, PIN assign/change `S-135` and dynamic
second-PIN activation `S-136`.

**Recovered (CS-ROUTES):** the card router opens `S-121` for the debit, bon and
credit block keys with that group's card list; PIN keys open `S-135` with the
card type and the PIN menu; the dynamic-PIN key opens `S-136`. The pre-login
router opens `S-122` when a menu flag is set, otherwise `S-118`.

**Recovered (CS-PAGES, CS-PIN, CS-OTP):** `S-121` lists cards, shows a warning
sheet and blocks by card number, with a success sheet and no OTP. `S-135` has
assign/change modes for first and second PINs; it confirms, sends an OTP to the
registered phone and verifies it. `S-136` goes ready → OTP → success. The
pre-login forms take card number, CVV2, expiry and a static or dynamic second
password, with a dynamic-password request.

**Designed for mock:** the fixed OTP code, synthetic phone, labelled results
and the unavailable dynamic-password request. Nothing is sent or changed.

## Per-destination requirements

| Existing node / family | Purpose; user inputs | Required data and fixture source | Prerequisites and navigation result | Supported API/model references | Proposed future mock records/services | Unknowns; effort/confidence |
|---|---|---|---|---|---|---|
| `S-121`; `F-033` | Block a signed-in card; choose card, confirm warning | Cards of the tab's group (FR-03 fixture) | Block row with the group's list → warning → result → card tab | Block-by-card-number write (encrypted; card-number response) | Block request with status; card status read reflecting it | Card states that may not be blocked **Unknown**. 0.5 day / high |
| `S-135`; `F-033` | Assign or change first/second PIN; current/new/retype | Cards of the group; operation menu | PIN row → section → confirm → OTP → result | Issue/change PIN send-OTP and verify writes (encrypted) | PIN operation draft with OTP challenge; no secret storage | OTP expiry, retry limits and failure texts **Unknown**. 1 day / high rules |
| `S-136`; `F-033` | Activate dynamic second PIN; choose card | Cards of the group, registered phone | Activation row → send code → OTP → result | Activation request-OTP and verify writes (encrypted) | Activation challenge and status | Phone source and eligibility **Unknown**. 0.5 day / high |
| `S-118`; `F-033` | Pre-login block; card number, CVV2, expiry, dynamic password | None | Pre-login menu (not built): labelled review entry → result | Pre-login block and dynamic-password request (encrypted) | Pre-login block request with secure-data checks | Exact field rules partly recovered. 0.5 day / medium |
| `S-122`; `F-033` | Pre-login block v2; same inputs plus expiry sheet | None | Flag-gated pre-login route (not built): labelled review entry → result | Same as `S-118` | Same as `S-118` | Menu flag meaning and v2 differences **Unknown**. 0.5 day / medium |

Requests and responses travel in the encrypted envelope. The shapes above are
caller parse types, not proven HTTP schemas.

## Validation and local state

- First PIN: exactly 4 digits. Second PIN: 5–12 digits. Retype must equal the
  new PIN. Every shown field is required. Persian digits are accepted.
- Pre-login forms reuse the FR-03 checks: 16-digit card number, CVV2, month/year
  expiry and a second password of at least 5 digits.
- OTP: a wrong code shows an error and keeps the sheet open; cancel returns to
  the form with no result.
- Secrets are never kept in saved state, never sent and never logged.

Exact contracts, eligibility, retry limits and original visual fidelity remain
outside what local fixtures prove. Proposed services are our designs, not
recovered bank tables.
