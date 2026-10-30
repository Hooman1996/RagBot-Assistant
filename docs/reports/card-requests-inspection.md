# FR-04a — Card operation requests

Issue [#48](https://github.com/Hooman1996/RagBot-Assistant/issues/48), following FR-03 #46. This is a local frontend batch. No backend, database, assistant, bridge, original-app execution or banking network access was added.

## Scope decisions

The owner approved the next batch, then chose during scoping (recorded on #48):

- FR-04 is split into FR-04a card operation requests (this batch), FR-04b blocking and PIN/second-password activation, and FR-04c wallet-linked cards. FR-04b and FR-04c remain proposed.
- After a valid confirm, the replica shows a local result clearly labelled **Designed for mock**: no card is issued or changed and no fee is taken.
- Credit-card repayment confirm/receipt `S-132`/`S-133` is a money payment and moved to FR-11. IDs are unchanged.

The owner's decision mentioned a fixed local OTP code. The recovered request flow has **no OTP step**, so none was added.

## Implemented flow

On the card tab's debit group, **درخواست کارت** opens issue `S-124`. **تمدید کارت**, **المثنی کارت** and **تغییر رنگ کارت** open modify `S-127` with the matching operation (CR-ROUTES). In the original these are server menu entries. A separate shared operations menu list (`S-002`, FR-45) can also list them; it is not built. Row titles and their presence are **Designed for mock**.

- `S-124` / `S-127`: choose the source account (issue) or debit card (modify), read the notice, and choose delivery. In-person delivery shows the branch and is the default, as recovered when the server enables it. Postal is the other option.
- In-person delivery goes straight to colour `S-123`; postal goes to address `S-129` first (CR-FLOW, recovered branch).
- `S-129`: choose a saved address. **افزودن آدرس** leads to the add-address screen of FR-05, so it explains that it is unavailable.
- `S-123`: choose a colour.
- `S-128`: review request type, account or card number, name, branch or postal code/address/phone, fee and colour; **ثبت درخواست** shows the labelled local result with a synthetic tracking number. **بازگشت به کارت‌ها** and **انصراف** leave the whole flow and return to the card tab, matching the recovered return to Home.

System Back steps back one page and keeps the draft. The draft, page choices and the shown result survive recreation.

## Data and behavior

Branch, notice, addresses, colours, phone, tracking number and fees are synthetic. Fees are `Long` with explicit `IRR`; the original fee amount is also an integer. The [requirements matrix](card-requests-requirements.md) records the recovered inputs, the draft record, caller-backed models and the unresolved contracts.

## Private evidence and assets

CR-ROUTES, CR-FLOW, CR-PAGES and CR-CONFIRM refer to focused static findings under `local/evidence/card-requests/`. No new private assets were added: the batch reuses the FR-03 set (76 resources) and draws colour swatches in Compose.

## Original-app fidelity

Original UI fidelity is **unscored**; there are no owner references for these screens. Known gaps:

- Server notice text, fee list, branch, colour list and the colour sample card designs are replaced by fixtures and plain swatches.
- The source account/card pickers are simple lists rather than the original picker sheets.
- The status sheet shows only the success case; failure and pending statuses are not shown.
- The insufficient-balance branch, which offers an account top-up, is not implemented (top-up belongs to FR-15).
- The original's return to the generic menu list or shortcuts, when started from them, is not modelled.

## Acceptance

Baseline on main `7ee7be9`: repository checks 131/131, pytest 112, Ruff check/format pass, Android assemble, 10 JVM tests, lint 0 errors/107 warnings, 21/21 device tests.

Final results are in the PR. Android: 12 JVM tests and 24/24 server device tests (0 skipped). Earlier device runs failed and are kept privately. One failure was a test assumption about a merged result text. In the others, FR-03 tests tapped card rows and group chips that the longer row list had pushed out of view; the chips are now clicked through their accessibility action. A manual reproduction confirmed the UI itself selects the group correctly.

Rendered review on emulator-5554 covers both first pages, the address, colour and confirm pages, the labelled result and the return to the card tab. There are no replica crash records. Captures and logs are private under `local/evidence/card-requests/qa/`.

## Opening the batch and stopping point

Open **RagBot Replica → ورود به خانه → کارت** and use **درخواست کارت**, **تمدید کارت**, **المثنی کارت** or **تغییر رنگ کارت**.

This report records FR-04a as delivered. FR-04b, [card blocking, PIN and second-password activation](card-security-inspection.md), followed in #50.
