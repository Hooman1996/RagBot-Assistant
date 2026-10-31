# FR-11 — Bill and insurance installment payments

Approved issue [#66](https://github.com/Hooman1996/RagBot-Assistant/issues/66).
Owner approval covers FR-11 only. The next proposed batch is FR-12; it does not
start automatically. The [requirements matrix](bill-payments-requirements.md)
records the source modes, rules, fixtures, models and confidence.

## Baseline and recovery

Main was `f8fdeda`, with a clean checkout. Baseline passed 131 repository checks,
112 Python tests, both Ruff checks, Android build, 40 JVM tests and lint (0 errors /
117 warnings). Of 74 device tests, one existing FR-08 test
(`interbankRejectsBadChecksumAndRetainsItsOwnDraft`) failed once on unchanged main
and passed three of three isolated reruns; it is recorded as an intermittent
baseline flake, not changed. Output stays under `local/evidence/bill-payments/`.

BP-RECOVERY reused the private metadata decode, decompiled 59 bill, insurance and
credit-card classes offline, and traced registry keys, callers, layouts and
resource strings. It recovered the bill source modes, the ID rules, insurance
selectability and Pay visibility, and the repayment amount checks. Six catalogue
labels move from Inferred to Recovered (S-133, S-207–S-210, S-405), and S-208–S-210
are renamed after their source modes; IDs and allocations are unchanged.

## Navigation and preview boundary

New demo entries open S-209, S-210, S-208, the S-403 insurance menu and three
independent receipts (S-092, S-405, S-133). Credit repayment starts from the
existing S-131 credit detail (Home → card tab → credit card → **جزئیات و
بازپرداخت** → **پرداخت**). No confirmation sends anything: account bill payment,
card payment, OTP requests, insurance payment and repayment report unavailable.

## Opening the screens

| Entry | What opens |
|---|---|
| **پرداخت قبض با حساب** | S-209 → S-207 (accept unavailable) |
| **پرداخت قبض با کارت** | S-210 → S-207 → S-205 card info |
| **پرداخت قبض بدون ورود** | S-208 → S-207 → S-206 card info |
| **منوی بیمه — نمونهٔ مستقل** | S-403 → S-402 → S-401 → source → S-404 |
| S-131 **پرداخت** | Amount → source account → S-132 |

Use bill ID `9000000000001` with payment ID `1250001` (unpaid). Bill ID
`9000000000002` with payment ID `430002` is a paid fixture bill.

## Verification and delivery

Final server acceptance is recorded in the PR and the status row. New checks cover
the ID rules and per-mode sources, the fixture inquiry with paid and unknown
bills, installment selectability and Pay visibility, repayment amount limits,
correction through Back and recreation, card secrets that are never kept, the
empty-data state, unavailable payment, OTP, attachment, scan and history, and the
exact registry. One existing card test now expects S-131's repayment dialog
instead of an unavailable notice.

Rendered review on the pinned API 35 fa-IR emulator with the real keyboard kept
controls reachable and the crash buffer empty, and found three defects fixed
before acceptance: stale "required" errors stayed after valid input, the
repayment amount field was laid out right-to-left, and S-132 showed the card
number ungrouped. PNG/XML captures stay private under
`local/evidence/bill-payments/qa/`. After green CI and merge, clean main is rebuilt
and reinstalled, and the displayed revision and APK hash are checked. Stop before
FR-12.
