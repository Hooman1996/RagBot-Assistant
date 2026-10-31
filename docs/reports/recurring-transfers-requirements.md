# Recurring and continuous transfer requirements (FR-10)

Approved issue [#64](https://github.com/Hooman1996/RagBot-Assistant/issues/64).
F-039 continuous (automatic, standing-order) transfers and F-041 repetitive
transactions are frontend previews only. Labels distinguish **Recovered** static
evidence, **Designed for mock** behavior and **Unknown** service/runtime behavior.
RT-RECOVERY identifies the private metadata, decompile, caller and resource ledger
under `local/evidence/recurring-transfers/`. The original application was never run
and no banking service was contacted.

## Two different families

The catalogue groups both under recurring transfers, but the code separates them
(**Recovered**):

- **Continuous transfer** ("انتقال وجه خودکار") is a standing order. The user
  defines a title, date range and frequency, then completes an internal or interbank
  transfer form in continuous mode. Its confirmation registers the order with a
  one-time password, and the service executes it on schedule.
- **Repetitive transactions** ("تراکنش‌های پرتکرار") are saved past transactions
  (saved from receipts) that can be repeated. Types: card-to-card, mobile charge,
  internal, Paya, Satna and Pol. Repeating re-runs a service pre-validation, then
  opens the matching confirmation page.

## Surfaces and caller contracts

| Existing node / purpose | Recovered incoming context and interactions | Current fixture / deliberate preview behavior | Future requirements; confidence / preliminary effort |
|---|---|---|---|
| S-180 continuous menu | Standing-order registry key of the transfer and account menus; renders menu children and dispatches each through the exact-key registry | Two synthetic children: new request (S-181) and history (S-178). Reached as a fourth child of FR-08's S-193 | Eligible children and order. High; 0.5 day |
| S-181 request details | Insert-request registry key, history's new-request button. Title, start/end Jalali dates, service-provided frequency list, week-day or month-day sheet, transfer-type sheet from the menu children, then an execution-date call | Title, FR-02 Persian date fields with calendar picker, the three recovered frequencies, Saturday-first week days, days 1–31, transfer-type dialog (internal/interbank) | Frequency catalogue, date picker bounds, execution-date service. High rules; 1 day |
| S-177 execution confirmation | Opened with menu, job, frequency title, service-computed execution dates, request body and month warning flag; Continue opens S-192 or S-191 in continuous mode | Request summary and frequency title; execution dates explicitly not computed; Continue opens the FR-08 form with a visible continuous banner | Execution-date list and its algorithm. High route, unknown dates; 0.5 day |
| S-178 history | Get-request registry key and a Home handler. Paged list, name/destination search, new request, item → details | Three synthetic standing orders, search and empty result | Paged history service. High fields; 0.5–1 day |
| S-179 history details | Opened from history with trace and job. Request fields, executions with status, receipt per execution, attachment download, one of delete/cancel/back | Recovered action rule and confirmation texts; local cancel/delete; receipts and attachment unavailable | Track/details service, cancel/delete calls, execution receipts. High rules; 1 day |
| S-200 repetitive transactions | Home bookmark button (FR-07 recovery), hosted by S-199 with the menu title. Title search, typed rows, repeat, delete with confirmation | Four synthetic rows (card, internal, Paya, charge); repeat opens FR-08 S-176 or S-173 with the row prefilled; charge repeat unavailable; local delete | List/delete service, per-type pre-validation and inquiry. High identity; 1 day |

S-199 is the host activity of the single S-200 route, so it is not registered as a
separate page (same treatment as FR-07's hosts). S-200's catalogue label is
corrected from Inferred to Recovered with its ID and allocation unchanged. All seven
allocations are covered: six registered pages plus the S-199 host.

Effort is preliminary future mock/frontend work, excluding real bank integration
and authorization design.

## Entry and result relationships

Standing-order, insert-request and get-request registry keys start S-180, S-181 and
S-178. S-180's children dispatch through the same registry. The replica adds S-180
as a synthetic fourth child of FR-08's S-193 menu (membership is a mock). S-178
opens S-181 and S-179. S-181 → transfer-type choice → S-177 → S-192 internal or
S-191 interbank in continuous mode, carrying the request. FR-08's forms and review
show a continuous banner in that mode and keep separate drafts from normal
transfers. Their final confirmation still cannot submit, so no standing order is
registered.

Home's bookmark (recovered in FR-07) now opens S-200. Repeat targets are recovered:
card → S-176, internal/Paya/Satna/Pol → S-173 variants, mobile charge → S-212,
which is not built yet (FR-12) and therefore reports unavailable. **Designed for
mock:** the service pre-validation and inquiry before repeat are skipped and the
page says so.

## Schedule rules

**Recovered:**
- The request needs a title and both dates. Dates are Jalali `yyyy/MM/dd` at 00:00.
- A start date later than the end date is rejected with the recovered message.
- A span longer than 365 or 366 days (chosen by a start-year predicate whose
  direction looks inverted) is rejected with the recovered message.
- Frequencies are DAILY, WEEKLY (one week day) and MONTHLY (one day 1–31).
- Choosing day 30 or 31 shows the recovered month-end warning.
- Changing the frequency resets the chosen day.

**Designed for mock:** FR-02's Persian date field (typed text or its calendar picker), a fixed 365-day limit,
and a fixed frequency list (the original fetches titles from a service).
**Unknown:** past-date and equal-date handling, picker bounds, and the
execution-date algorithm.

**Units:** request and record amounts are exact Long rial integers. FR-08's
`TransferRules.amountRial` still validates continuous-mode form amounts. The
service returns amounts as strings, so a future contract must fix their unit and
format.

## Status, cancel and delete

**Recovered execution statuses:** success, failed, process failed, not started,
cancelled and unknown. The details action follows the recovered rule:
- a cancelled order shows only Back;
- if no execution has started, it offers delete ("only before transactions start");
- if some executions are still waiting, it offers cancel ("only waiting
  transactions are cancelled");
- otherwise it shows Back.

The replica applies cancel and delete only to its local list. A cancelled order's
waiting executions become cancelled; completed ones keep their status. The order
status labels (active/cancelled) are **Designed for mock**; the service's
status values other than cancelled are **Unknown**.

## State, cancellation and accessibility

The schedule draft, local records, the selected record, the repetitive list and
searches survive Back and recreation. Normal and continuous FR-08 form drafts are
kept apart, and the continuous banner is cleared whenever the transfer menu opens a
normal form. Dialogs cancel without mutation. No password, OTP or attachment is
collected. Persian/RTL text, LTR date/number fields, numeric keyboards and keyboard
clearance reuse FR-08 components.

## Candidate models and future services

| Candidate | Caller-supported relationship | Remaining contract gaps |
|---|---|---|
| M-046 | Result of the add-continuous call (E-298) made from S-173's continuous confirmation callback, which then shows the add result sheet | One-time password, encrypted request and result-sheet states **Unknown** |
| M-045 | Result of the track call (E-303); the same model backs S-179's details state | Whether S-179 loads it through E-303 or the encrypted transaction call (E-299) is not proven |
| M-029 | Envelope for frequency, execution-date, history, transaction, attachment, cancel and delete calls (E-296–E-304) and for the repetitive list/add/delete repository | Inner payloads recovered only as client-side model fields |

Proposed future mock records: the frequency catalogue, the execution-date
calculation, standing orders with executions and receipts, cancel/delete outcomes,
repetitive rows per type with pre-validation and inquiry, and saving a receipt as a
repetitive row. These need approved typed contracts and user-only authorization
boundaries. FR-10 implements no HTTP service, request, scheduler, timer, background
job, database or external launcher.

## Unimplemented and uncertain states

Not reproduced, absent or unavailable:
- execution-date lists, the add result sheet and one-time password;
- execution receipts and attachment download;
- history pagination;
- saving receipts as repetitive rows;
- repetitive pre-validation tooltips and charge repeat;
- the original date picker.

The FR-07 Home bookmark notice is replaced by the S-200 route. All records and
generic empty/loading/error/retry states are **Designed for mock**. Private
overlay/font provenance remains SD-ASSETS (84 resources); public substitutes remain
disclosed. Original-app fidelity is unscored.
