# Mobile top-up and charity requirements (FR-12)

Approved issue [#68](https://github.com/Hooman1996/RagBot-Assistant/issues/68).
F-044 mobile top-up, F-047 charity donations and their F-031 receipts are frontend
previews only. Labels distinguish **Recovered** static evidence, **Designed for
mock** behavior and **Unknown** service/runtime behavior. MC-RECOVERY keeps the
private metadata, decompile, caller, layout and resource ledger under
`local/evidence/mobile-charity/`. The original application was never run and no
banking service was contacted.

## Top-up source modes

As with FR-11's bills, the repeated "charge by card" nodes are source modes of one
journey (**Recovered**):

- **S-211 from account** — opened by the Home and account charge registry keys.
- **S-213 by card** — opened by the card charge key (login) and by the anonymous
  charge key when its menu flag is off.
- **S-214 typed card** — opened by the anonymous charge key when its flag is on.

All three open S-212. From an account, S-212's accept opens a signature/OTP sheet;
from a card it opens S-215 (login card info) or S-216 (anonymous card info), both
sharing FR-11's card-info fields. Purchases end at the S-094 receipt. FR-10's saved
charge repeat also targets S-212 and now opens it prefilled. Catalogue labels of
S-211–S-214 move from Inferred to Recovered; S-212 and S-214 are renamed (IDs and
allocations unchanged).

## Charity journey

**Recovered:** the Home and account charity keys open S-283, a picker of
service-provided charities with the hint "خیریه مورد نظر خود را برای حمایت مالی
انتخاب کنید.". A charity opens S-284 with the chosen charity and source account:
destination, source account, amount, and optional withdrawal ID and source
description. Continue opens S-282 (titled "انتقال وجه داخلی") with note,
attachment and an OTP sheet, ending at the S-095 receipt.

## Surfaces

| Node | Recovered context | Preview behavior | Future requirements; confidence / effort |
|---|---|---|---|
| S-211/S-213/S-214 | Source, mobile number (11 digits), operator selector, amount field and an amount sheet of default amounts in rial and toman | Recovered checks and messages; prefix detection; per-operator synthetic limits and default amounts; per-mode drafts | Charge configuration and pre-validation services. High rules; 1 day |
| S-212 | Source, operator and number, amount; account accept opens signature/OTP | Account purchase unavailable; card modes open S-215/S-216 | Purchase contract. High; 0.25 day |
| S-215/S-216 | CVV2, expiry, second password, OTP; login adds password type and save choice | FR-11's shared card-info fields, never saved; OTP and purchase unavailable | Card purchase authorization. High; 0.25 day |
| S-283 | Charity list (name, description, icon, account) from the service | Two synthetic charities | Charity catalog service. High fields; 0.25 day |
| S-284 | Charity, source account, amount, optional IDs; amount checked against the internal transfer range | Recovered required and out-of-range checks with a synthetic range; change charity returns to the picker | Range service. High; 0.5 day |
| S-282 | Source, charity, amount, other info, note and attachment, OTP | Summary; attachment and donation unavailable | Donation contract. High; 0.25 day |
| S-094/S-095 | Charge: source, mobile, operator, amount, time, trace. Charity: source, destination, charity, bank, amount, time, trace | Independent labelled synthetic receipts | Receipt data. High; 0.25 day |

## Rules

**Recovered:**
- Mobile number: required; exactly 11 digits starting with `09`, else
  "مقدار وارد شده را بررسی فرمایید.".
- Operator: from four typed digits, a configured prefix selects the operator; three
  or fewer digits clear it. Missing operator → "اپراتور تلفن همراه خود را انتخاب کنید.".
  Operators are همراه اول, ایرانسل and رایتل.
- Amount: required; then, in this order, below the operator minimum, above its
  maximum, and not a multiple of its multiplier, each with the recovered message.
- Charity amount: required; outside the internal transfer range →
  "مبلغ ورودی در محدوده نمی باشد.".

**Designed for mock:** every operator's prefixes, limits, multiplier and default
amounts (the service supplies them; synthetic prefixes `0900`–`0902` are used), the
charity list, the charity range (10,000–100,000,000 IRR) and the optional-field
lengths (FR-08's).

**Unknown:** the anonymous menu flag's meaning, the cartable-account path,
charity eligibility and whether the range differs per charity.

**Units:** all amounts are exact Long rial integers; toman labels are display only
(rial ÷ 10).

## Models and services

Recovered client models: the charge configuration (operator, multiplier, minimum,
maximum, default amounts, prefixes), the charge transfer info and validation
result, the purchase requests for account and card, the charge pre-validation
result, the charity record and the charity transfer request. No M/E join beyond
these caller-backed models is claimed. FR-12 implements no HTTP service, catalog,
purchase, donation, OTP, database or external launcher.

## Unimplemented

Live operator configuration and charity catalogs, pre-validation, signature/OTP,
purchase and donation, attachments, operator and charity icons, the cartable path
and saving card data are not reproduced. Original-app fidelity is unscored.
