# FR-12 — Mobile top-up and charity payment previews

Approved issue [#68](https://github.com/Hooman1996/RagBot-Assistant/issues/68).
Owner approval covers FR-12 only. The next proposed batch is FR-13; it does not
start automatically. The [requirements matrix](mobile-charity-requirements.md)
records the source modes, rules, fixtures, models and confidence.

## Baseline and recovery

Main was `e85194f`. A first baseline overlapped with in-progress edits and was
discarded. Rerun on a clean tree identical to main, it passed 131 repository
checks, 112 Python tests, both Ruff checks, Android build, 45 JVM and 78 device
tests (0 failures, 0 skipped) and lint (0 errors / 117 warnings). Output stays
under `local/evidence/mobile-charity/`.

MC-RECOVERY reused the private metadata decode, decompiled 37 charge and charity
classes offline, and traced registry keys, callers, layouts and resource strings.
It recovered the three top-up source modes, the phone and operator rules, the
ordered amount checks, the charity journey and its range check, and the charge
target of FR-10's saved-transaction repeat.

## Navigation and preview boundary

New demo entries open S-211, S-213, S-214, the S-283 charity picker and the
independent S-094 and S-095 receipts. FR-11's card-info fields are now a shared
component used by S-205/S-206 and S-215/S-216. Home's saved repetitive
transactions (S-200) now repeat a charge into S-212 prefilled; it was previously
unavailable. No confirmation sends anything: account purchase, card purchase,
OTP and donation report unavailable.

| Entry | What opens |
|---|---|
| **خرید شارژ با حساب** | S-211 → S-212 (accept unavailable) |
| **خرید شارژ با کارت** | S-213 → S-212 → S-215 card info |
| **خرید شارژ بدون ورود** | S-214 → S-212 → S-216 card info |
| **کمک به خیریه** | S-283 → S-284 → S-282 |
| Home bookmark → saved charge → **تکرار تراکنش** | S-212 prefilled |

Synthetic prefixes: `0900` همراه اول, `0901` ایرانسل, `0902` رایتل, for example
`09010000000`. Amounts follow each operator's synthetic limits and multiplier.

## Verification and delivery

Final server acceptance is recorded in the PR and the status row. New checks cover
prefix detection and clearing, the phone rule, the ordered amount checks per
operator, per-mode sources and the missing operator, charity amount range,
correction through Back and recreation, card secrets that are never kept, the
empty-data state, the charge repeat target, unavailable purchase, OTP, attachment
and donation, and the exact registry. FR-11 tests follow the shared card-info
tags, and the FR-10 repeat test now expects S-212.

Rendered review on the pinned API 35 fa-IR emulator with the real keyboard kept
controls reachable and the crash buffer empty. It found two fixes made before
acceptance: the bare sample number under the phone field read like a value (now
labelled "نمونه:"), and the charity transfer lacked the recovered Cancel button.
PNG/XML captures stay private under `local/evidence/mobile-charity/qa/`. After
green CI and merge, clean main is rebuilt and reinstalled, and the displayed
revision and APK hash are checked. Stop before FR-13.
