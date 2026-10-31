# FR-13 — Wallet overview, QR display and history

Approved issue [#70](https://github.com/Hooman1996/RagBot-Assistant/issues/70).
Owner approval covers FR-13 only. The next proposed batch is FR-14; it does not
start automatically. The [requirements matrix](wallet-requirements.md) records the
routes, rules, fixtures, models and confidence.

## Baseline and recovery

Main was `833e51e`. On a clean tree it passed 131 repository checks, 112 Python
tests, both Ruff checks, Android build, 50 JVM and 81 device tests (0 failures,
0 skipped) and lint (0 errors / 117 warnings). Output stays under
`local/evidence/wallet/`.

WL-RECOVERY reused the private metadata decode, decompiled the wallet screens,
view models, models, router, menu manager and QR generator offline, and traced
router keys, composables, smali branches and resource strings. It recovered the
router targets, the wallet card fields and balance states, the five/four wallet
cap, the active-wallet pickers, the QR payload, statement paging and date checks,
the row and detail rules, and the receipt fields. No catalogue label changes:
S-110–S-115 were already Recovered.

## Navigation and preview boundary

| Entry | What opens |
|---|---|
| Home → **کیف پول**, or demo **کیف پول** | S-111 |
| S-111 → **صورتحساب کیف** | S-115 → row → S-114 |
| S-111 → **سوابق تراکنش‌ها** | S-110 → any item → S-091 |
| S-111 → **دریافت با بارکد** | S-112 |
| Demo **رسید مستقل کیف پول — نمونه** | S-113 |

Top actions and buy, bill and mobile-charge rows report unavailable (FR-14). The
QR page shows the recovered payload as local text with Latin digits, states that
no image is built and no payment starts, and offers no scanner. Copy writes the
wallet ID to the clipboard; share and export are unavailable.

## Verification and delivery

Final server acceptance is recorded in the PR and the status row. New checks cover
the wallet cap, active pickers, the exact QR payload, the detail-row rules, menu
targets, the registry, balance show/hide through recreation, unavailable
operations, picker selection and pages kept through detail, Back and recreation,
the date error, the QR amount through recreation and wallet change, copy, share,
the history target, the receipt, and the empty and error states.

Rendered review on the pinned API 35 fa-IR emulator kept controls reachable and the
crash buffer empty. It found one fix before acceptance: the fa locale showed the
QR payload's wallet number in Persian digits, unlike the real payload; it now uses
Latin digits. PNG/XML captures stay private under `local/evidence/wallet/qa/`.
After green CI and merge, clean main is rebuilt and reinstalled, and the displayed
revision and APK hash are checked. Stop before FR-14.
