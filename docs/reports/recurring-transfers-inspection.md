# FR-10 — Recurring and continuous transfer management

Approved issue [#64](https://github.com/Hooman1996/RagBot-Assistant/issues/64).
Owner approval covers FR-10 only. Next proposed batch is FR-11; it does not start
automatically. The [requirements matrix](recurring-transfers-requirements.md)
records the nodes, incoming context, schedule rules, status actions, fixtures,
candidate models, future services and confidence separately from uncertain runtime
behavior.

## Baseline and recovery

Main was `e3e5f6a`, with a clean checkout. Baseline passed 131 repository checks,
112 Python tests, both Ruff checks, Android build, 36 JVM tests, 70 device tests
(0 skipped) and lint (0 errors / 117 warnings), matching FR-09's record. Baseline
output and reports remain under `local/evidence/recurring-transfers/`.

RT-RECOVERY reused the private P1a metadata decode and decoded resources, then
decompiled only the continuous/repetitive classes offline. It identifies screen
classes, registry keys and call sites, date and frequency rules, week/month day
models, the details action rule, cancel/delete scope, repeat targets and resource
strings. Static recovery never ran the original app or contacted production. No
original assets, code findings, raw output or captures enter GitHub.

The batch's two families have different business meanings: automatic
(standing-order) transfers and saved repetitive transactions. Both are implemented
as separate page groups. S-199 is only the host of the S-200 route and is not
registered. S-200's catalogue label is corrected from Inferred to Recovered (ID
and allocation unchanged), and the catalogue's label totals are updated.

## Navigation and preview boundary

FR-08's S-193 transfer menu gains a synthetic fourth child for S-180, matching
the recovered standing-order registry key. S-180 opens S-181 (new request) or
S-178 (history). S-181 validates the request, asks for the transfer type and
opens S-177. S-177 shows the request summary, states that execution dates come
from the service and are not computed, and continues into FR-08's S-192 or S-191
form in continuous mode. Those forms and the S-173 review carry a visible
continuous banner and keep separate drafts. Confirmation remains unavailable, so
no standing order is registered and no scheduler, timer or background job exists.

S-178 searches synthetic orders and opens S-179. S-179 shows executions and
applies the recovered delete/cancel/back rule with the recovered confirmation
texts. Cancel and delete change only the local list; execution receipts and
attachments are unavailable.

Home's bookmark (recovered in FR-07) opens S-200. Repeat skips the service
pre-validation, says so on the page, and opens FR-08's S-176 card review or S-173
account/IBAN review with the row prefilled. Mobile charge repeat targets S-212,
which is not built, so it is unavailable. Delete asks for confirmation and edits
the local list.

## Opening the screens

| Entry | What opens |
|---|---|
| Demo page **منوی انتقال — نمونهٔ مستقل** → **انتقال وجه خودکار** | S-180 → **درخواست انتقال وجه خودکار** (S-181) or **سوابق انتقال وجه خودکار** (S-178) |
| S-181 **ادامه** | Transfer-type choice → S-177 → **ادامه** → S-192/S-191 in continuous mode → S-173 review |
| S-178 row | S-179 details: cancel (partly executed sample), delete (not-yet-started sample) or back (cancelled sample) |
| Home header bookmark | S-200 repetitive transactions; **تکرار تراکنش** opens S-176/S-173 |

Use Jalali dates such as `1405/09/01` and `1405/12/01`. Amounts are positive rial
integers. The existing loopback-only viewer and SSH forwarding remain in the
[runbook](../runbook/replica-development.md#desktop-viewer-and-stopping-the-simulation).

## Validation and remaining fidelity gaps

Recovered rules covered: required title and dates, start not after end, span
within a year, frequency with its day, the month-end warning, the details action
rule and the waiting-only cancel. Deviations are explicit: the year limit is a
fixed 365 days and the frequency list is fixed rather than fetched. Dates use
FR-02's Persian date field with its calendar picker (rendered review found that a
numeric keypad cannot type `/`). No fee, limit, execution date or success event
is invented. The M-045/M-046 caller relationships are recorded.

Execution-date lists, add result sheet, one-time password, history pagination,
execution receipts, attachments, repeat pre-validation and charge repeat are not
reproduced. All data and generic scenario states are synthetic. The existing
private 84-resource overlay and public substitute disclosure are reused; no new
asset extraction was needed. Original-app visual fidelity remains unscored.

## Verification and delivery

Final server acceptance passed: **131 repository checks, 112 Python tests, Ruff
check/format, Android assemble, 40 JVM tests and 74 device tests**, with zero
failures, errors or skips. Lint is **0 errors / 117 warnings**, unchanged from
baseline; comparison found no new or removed diagnostics. Two earlier device
expectations changed because the Home bookmark now opens S-200 instead of a
notice; they assert the new route. New checks cover the exact registry and the
unregistered S-199 host, date order and one-year span, frequency/day
requirements, the month warning, frequency titles, the details action rule,
waiting-only cancel, local delete, correction and recreation on the request,
continuous banner and separate drafts, repeat targets, unavailable submission,
receipt, attachment and charge repeat, and real keyboard clearance.

RT-QA PNG/XML captures are kept privately under `local/evidence/recurring-transfers/qa/`:
transfer and continuous menus, request with the real keyboard, month-day dialog,
date-order error with the month-end warning, transfer-type dialog, execution
summary, continuous form with the keyboard and its review, history, details
before and after cancel with the confirmation, the repetitive list and a repeat
into the internal review. Persian/RTL, LTR identifiers, dialogs, scrolling and
system bars were inspected on the pinned API 35 fa-IR emulator; controls stay
reachable above the keyboard and the crash buffer was empty. Rendered review found
and fixed three defects before acceptance: the numeric keypad could not type `/`
in dates (now FR-02's Persian date field), a frequency without a chosen day showed
"(۰ ام)", and saved account rows showed an internal key instead of the synthetic
account number. No warnings were suppressed or assertions removed.
After green CI and merge, clean main is rebuilt and reinstalled, and the
displayed revision and installed APK hash are checked. Stop before FR-11.
