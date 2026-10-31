# Bill, insurance and credit repayment requirements (FR-11)

Approved issue [#66](https://github.com/Hooman1996/RagBot-Assistant/issues/66).
F-043 bill payment, F-054 insurance installments and the F-033 credit-card
repayment (S-132/S-133, moved here in #48) are frontend previews only. Labels
distinguish **Recovered** static evidence, **Designed for mock** behavior and
**Unknown** service/runtime behavior. BP-RECOVERY keeps the private metadata,
decompile, caller, layout and resource ledger under `local/evidence/bill-payments/`.
The original application was never run and no banking service was contacted.

## Bill source modes

The three nodes the catalogue called "bill transfer" are distinct source modes of
one journey, not duplicates (**Recovered**):

- **S-209 from account** — source-account picker, bill ID and payment ID. Opened by
  the Home and account bill registry keys.
- **S-210 by card** — source-card picker with the same IDs. Opened by the card bill
  registry key (login mode) and by the anonymous bill key when its menu flag is off.
- **S-208 typed card** — the source card number is typed. Opened by the anonymous
  bill key when its menu flag is on.

All three call a pre-validation inquiry and open S-207. From an account, S-207's
accept opens a signature/OTP confirmation sheet. From a card, it opens S-205 (login
card info: CVV2, expiry, dynamic or static password, a save-card-data choice and
an OTP request) or S-206 (anonymous card info without the password-type and
save choices). Every payment ends at the S-092 bill receipt. Catalogue names and
labels of S-208–S-210 and S-207 are corrected accordingly (IDs and allocations
unchanged).

## Surfaces and caller contracts

| Node | Recovered context | Preview behavior | Future requirements; confidence / effort |
|---|---|---|---|
| S-209/S-210/S-208 entry | Bill ID and payment ID numeric, at most 13 digits; required; at least 6 digits; barcode of 13+ digits splits into bill ID and payment ID | Recovered checks and messages; per-mode drafts; typed card needs 16 digits (shape only); scan unavailable | Inquiry service and card check. High rules; 1 day |
| S-207 confirmation | Source, company, amount, payment details, note and attachment; amount and company come from the inquiry | Fixture inquiry: unpaid bill opens S-207; paid and unknown bills show local errors; note local; attachment and account payment unavailable | Pre-validation and payment contracts. High fields; 0.5 day |
| S-205/S-206 card info | CVV2, expiry, second password, OTP request; login adds password type and save-card-data choice | Masked fields reset on Back/recreation and never saved; OTP and payment unavailable | Card-payment authorization. High fields; 0.5 day |
| S-092 bill receipt | Source, payee, bill ID, payment ID, amount, time, trace and tracking numbers | Independent labelled synthetic receipt; share unavailable | Receipt data. High; 0.25 day |
| S-403 insurance menu | Children dispatched by the menu registry; history key opens transaction history filtered to insurance payments | Two synthetic children; history unavailable | Menu service. Medium; 0.25 day |
| S-402 insurance list | Loads the user's policies with insured name and national code; no input | Two synthetic policies | Policy list service. High fields; 0.5 day |
| S-401 installments | Policy header and installments with number, year, due date, amount, remaining amount, payment ID, status; single selection; source account | Recovered selectability, Pay visibility and selection error; source dialog | Pre-validation contract. High rules; 1 day |
| S-404/S-405 | Confirmation with source, purpose and amount, then OTP; receipt | Confirmation shows the remaining amount (Designed for mock); payment unavailable; independent receipt | Payment/OTP contracts. High fields; 0.5 day |
| S-131 → S-132/S-133 | Pay asks for the repayment amount, then the source account, then S-132 and an OTP; receipt S-133 | Recovered amount checks; account dialog; S-132 summary; payment unavailable; independent receipt | Repayment and OTP contracts. High rules; 0.5 day |

Effort is preliminary future mock/frontend work, excluding bank integration and
authorization design.

## Rules

**Recovered:**
- Bill ID and payment ID: empty → "تکمیل این فیلد اجباری است."; fewer than 6
  digits → "مقدار وارد شده را بررسی فرمایید."; input stops at 13 digits. No
  client-side checksum was found.
- The bill amount is never derived from the payment ID; the inquiry returns it.
- Installments with status paid or verification-waiting cannot be selected; Pay
  appears only while a due or postponed installment exists; Pay without a choice →
  "یکی از اقساط را جهت پرداخت انتخاب کنید.". There is no installment amount entry.
- Repayment amount: empty → required; 0 → "مبلغ ورودی در محدوده نمی باشد.";
  above the total payable → "مبلغ نمی تواند بیشتر از مبلغ بدهی اعتباری باشد.";
  any smaller amount (partial repayment) is allowed.

**Designed for mock:** the bill inquiry fixture (one unpaid, one paid bill), its
paid/unknown messages, installment status titles (the service supplies them), the
confirmation amount, menu membership and the account source dialogs.

**Unknown:** server-side bill validation and paid-bill handling, the anonymous
menu flag's meaning, the cartable-account path, insurance eligibility beyond
installment status, and what the insurance history page shows.

**Units:** all amounts are exact Long rial integers. The bill inquiry returns its
amount as a string, so a future contract must fix the unit and format.

## State and accessibility

Per-mode bill drafts, the note, the selected policy, installment and source, and
the repayment amount survive Back and recreation. Editing a field clears only its
own stale error. Card credentials are never saved. Persian/RTL text, LTR numeric
fields, numeric keyboards, dialogs and keyboard clearance follow FR-08.

## Candidate models and services

Recovered client models: the bill transfer info (source, bill ID, payment ID,
trace and job IDs, pre-validation result, card mode), the bill pre-validation
result (amount, company names, dates, service ID, cartable flag, owner), the bill
payment request (trace, source, IDs, OTP, expiry, CVV2, second password,
signature), the insurance list/policy/installment models, the installment
pre-validation request/result, and the credit-card detail, OTP and verify models.
Bill and insurance calls use the encrypted envelope. No M/E join beyond these
caller-backed models is claimed. FR-11 implements no HTTP service, inquiry,
payment, OTP, database or external launcher.

## Unimplemented

Barcode scanning, real inquiry, signature/OTP sheets, payment, receipts as
transaction results, attachment upload, insurance history, the cartable path and
saving card data are not reproduced. Original-app visual fidelity is unscored.
