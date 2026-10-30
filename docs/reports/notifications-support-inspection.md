# FR-06 — Notifications and support inbox

Approved issue [#56](https://github.com/Hooman1996/RagBot-Assistant/issues/56)
covers F-025/F-026 and existing nodes S-075–S-082 only. The owner authorized the
issue, implementation, tests, PR and merge after acceptance and green CI.
FR-07 and subsequent batches remain proposed.

## Baseline

Fetched main matched reviewed revision `04dae1762064c23456602219f44c9a3cd103b48c`.
Baseline acceptance passed: 131 repository checks, 112 Python tests, Ruff check
and format, 17 JVM tests and 36 device tests (zero failures/skips), lint
0 errors / 107 warnings. The first sandboxed Android attempt could not open
Gradle's lock socket; the rerun with local socket/emulator access passed.
Both logs and baseline XML/lint reports are retained privately.

## Recovery and data boundaries

NS-NOTIFICATIONS and NS-SUPPORT reference focused private recovery ledgers,
decoded content and REA evidence under `local/evidence/notifications-support/`.
They distinguish **Recovered** content/callbacks from **Unknown** dynamic menus,
encrypted payloads and runtime behavior. No original-app execution, production
contact, backend, database, bridge or assistant work is part of this batch.

All records and FAQ prose are **Designed for mock**. Local notification state
and non-secret drafts survive Back/recreation. Messages are never transmitted;
chatbot answers are unavailable. The [requirements matrix](notifications-support-requirements.md)
separates reconstructed behavior, synthetic state, future service proposals and
uncertainties.

## Implemented navigation and content

- Home's notification header button opens **S-075** with the local unread count.
  Type filters, item sheets, copy, unread/read presentation, automatic finite
  paging, selection, read-all and single/group/category/all deletion are
  inspectable. Delete confirmations cancel without mutation. Sharing explains
  that external launching is unavailable. Empty/loading/error/retry scenarios
  are selected outside replicated content.
- Home's support/message header button and **پشتیبانی** menu tile each have
  independently recovered handlers leading to authenticated FAQ **S-077**.
  The generic menu-key relationship is recovered; the owner's actual remote
  menu snapshot remains Unknown. Originating Home tab/scroll is retained.
- **S-077/S-078** expand synthetic FAQ questions individually. They explicitly
  distinguish authenticated/unauthenticated variants. Dynamic authenticated
  child menus are unresolved; no invented links replace them. No FAQ category
  or search control was recovered.
- **S-078 → S-081** uses a labelled local mobile/OTP gate (`12345`, never
  saved), with compose/history tabs. History selection stays on S-081. The
  compose tab resets the selected thread; non-secret drafts remain local.
  Real session expiry, resend and authentication services are unimplemented.
- **S-080 → S-082** passes the selected ticket identity. ASKED supports a local
  edit draft; ANSWERED shows a synthetic long conversation and continue/end
  controls; CLOSED has no composer. Sending/editing/ending never changes a
  delivered message or ticket status. S-082 also has a recovered nullable-ticket
  new-message variant, inspectable through a labelled independent entry.
- **S-079** has description, four reaction options and a local result sheet.
  **S-076** preserves the greeting/question-input shell and validation while
  answering stays unavailable. No assistant reply is fabricated.

The demo page contains clearly labelled independent entries for S-076, S-078,
S-079, S-080 and S-082 without a ticket. These expose content whose incoming
menus/login surface are not configured in this replica; they are not graph
edges. S-071 retains its existing independent entry. Only existing implemented
IDs are registered; no graph node/edge or batch allocation is added.

The [requirements matrix](notifications-support-requirements.md) assesses every
requested model candidate. M-049 is an unseen-count response; M-048 belongs to
activation/inquiry, and M-012 is not the notification page request. A narrow
M-020 catalogue correction records its three semantic fields, backed by
NS-SUPPORT. Encrypted inner service contracts remain Unknown.

## Fidelity and scope limits

Original-app visual fidelity remains **unscored**, independently of technical
acceptance. Server-driven content and menu configuration cannot be established
from our captures. Existing private fonts/icons plus four caller-proven rating
vectors use the established hash-pinned resource mechanism (NS-ASSETS, 80
verified inputs); public builds retain their substitute disclosure. Replica
captures are review evidence, not original-app goldens.

Unknowns include the live support menu, server notification types/count rules,
error wording, original persistence/ordering, chatbot nested visual details,
exact sheet/icon geometry and service-backed states. No attachment callback was
recovered from the bounded support/input composable inspection; no picker or
download control is invented. Our captures establish rendered replica behavior
only. Original-app comparison is still required.

S-071 retains its independent review entry because no genuine caller was recovered.
FR-05's documented fidelity gaps are unchanged. No support hub or new graph ID
is invented to hide an unresolved caller.

## Rendered review and opening the screens

NS-QA contains 21 replica PNGs with accessibility hierarchies under
`local/evidence/notifications-support/qa/`: notification top/detail/delete/Back
cancellation/error/retry; both FAQ variants; suggestion with all four recovered
rating vectors; ticket list; long-thread top/middle and reply keyboard;
phone/OTP; compose/history tabs; no-ticket composer; chatbot shell/keyboard and
unavailable answering. Persian text, mixed-direction DEMO identifiers, scrolling,
system bars and keyboard clearance were inspected on emulator-5554. Scrolling
clips content at its viewport normally; no actionable control was obscured.
Replica-scoped logs contain no replica crash records.

| Surface | How to open |
|---|---|
| S-075 notifications | Demo **ورود به خانه**, then the **اعلان‌ها** header button |
| S-077 authenticated FAQ | Home support/message header button or **پشتیبانی** tile |
| S-076 chatbot shell | Demo **پرسش از چت‌بات** |
| S-078 FAQ without login | Demo **پرسش‌های متداول بدون ورود** |
| S-079 suggestion | Demo **پیشنهاد** |
| S-080 ticket list / S-082 selected ticket | Demo **لیست تیکت‌ها**, then a ticket; second ticket contains the long thread |
| S-081 compose/history | S-078 **ارسال/مشاهده پیام**, synthetic mobile, labelled OTP `12345` |
| S-082 new message | Demo **پیام جدید بدون تیکت** |

The independent entries are outside replicated content. The existing loopback
viewer and private SSH forwarding remain documented in the
[replica runbook](../runbook/replica-development.md#desktop-viewer-and-stopping-the-simulation).

## Acceptance and stopping point

Final server acceptance: 131 repository checks, 112 Python tests, Ruff check and
format passed. Android assemble, 21 JVM tests and 48 device tests passed with
zero failures/errors/skips. Lint: 0 errors / 111 warnings, four more than the
107 baseline. The additions are UnusedResources diagnostics for the four
private rating vectors resolved at runtime; rendered QA confirms their use.
No warnings are suppressed. Public CI builds without private assets; its result
is recorded in the PR before merge.

The narrow API inventory correction also validates against its JSON schema:
520 endpoint IDs and 60 model IDs preserved. Graph IDs and batch allocations
are unchanged. Existing Home, Accounts, Cards and Profile regressions passed.

Earlier test nullability/Back-event failures are retained privately, including
XML/logs and the native dialog-focus investigation. The final test waits for
native window readiness and retains cancellation, item, unread-count and
navigation assertions. History pagination/scroll and per-context draft
restoration also have device coverage; no test is skipped or weakened.

Acceptance output, original failed runs, provenance and comparison captures
remain under `local/evidence/notifications-support/`. After the accepted PR is
merged, rebuild/install merged main and verify the displayed revision and the
installed APK hash as documented in the runbook.

FR-06 is the stopping point. The next proposed batch is **FR-07 — Saved
destinations and quick-action configuration**; it has not started and requires
explicit owner approval after review.
