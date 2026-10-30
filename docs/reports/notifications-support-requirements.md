# Notifications and support requirements (FR-06)

Approved issue [#56](https://github.com/Hooman1996/RagBot-Assistant/issues/56).
This is a frontend reconstruction assessment. Future records and services below
are proposals, not bank contracts or authorization to implement a backend.

## Evidence boundaries

**Recovered:** focused content, view-model, callback, resource and caller traces
for F-025/F-026 and S-075–S-082. NS-NOTIFICATIONS and NS-SUPPORT identify the
private recovery ledgers under `local/evidence/notifications-support/`.
Static recovery establishes client behavior, not observed server operation.
The original app was never executed.

**Designed for mock:** all displayed records, FAQ prose, statuses, selectors,
local pagination and retained state. FAQ answers describe the replica only;
they are not bank policy. Local read/delete operations do not establish server
unread-count or retention rules. Draft validation never delivers a message.

**Unknown:** encrypted inner payloads, server menu configuration, authorization,
eligibility, server ordering and retention rules, delivery and attachment
contracts. An API inventory candidate is not a proven screen association.

## Surface requirements

All surfaces have Persian/RTL content, labelled Back, scrolling and keyboard
clearance. Page-local scroll/filter/expansion state survives recreation and
return from a child page. Reopening a popped page resets that page's view state;
non-secret drafts and local notification read/delete state remain in saved UI
state. This retention is **Designed for mock** where original behavior is not
recoverable. No password or OTP is saved.

| Feature / existing node | Purpose, arguments and input | Display, states and relationships | Fixture / future service proposal | Confidence, unknowns and preliminary effort |
|---|---|---|---|---|
| F-025 / S-075 | Notification inbox; incoming unread count/session context; type choice, item selection, read/delete and confirmation | Type-filtered list, read/unread items, item sheet, individual/all operations, pagination; populated/empty/loading/error/retry | `NotificationFixture`; proposed notification read/page, local read/delete records and count reconciliation | High content / medium contracts; server types, ordering and count reconciliation Unknown; 1–2 days |
| F-026 / S-076 | Chatbot question shell; incoming title context; question input | Question input validation; answering explicitly unavailable, no generated responses | Root chatbot draft and static greeting shell; no answering service in FR-06 | Medium; dynamic entry and answer behavior Unknown; 0.5 day |
| F-026 / S-077 | Authenticated FAQ; incoming support menu/context | Expandable question/answer items; dynamic menu children require caller/configuration evidence | `SupportFixture`; proposed FAQ page records and authenticated support-menu configuration | High expansion / medium menu; exact server menu and content Unknown; 0.5–1 day |
| F-026 / S-078 | FAQ without login; mobile and local OTP gate for message access | Distinct unauthenticated heading, FAQ expansion, local identity preview before S-081 | `SupportFixture`; proposed public FAQ reads and a future synthetic challenge/session record | High distinction / medium auth details; real challenge limits and identity contract Unknown; 1 day |
| F-026 / S-079 | Suggestion; title/shortcut-origin arguments; description and reaction | Four reaction choices plus unselected default; nonempty description enables continuation; local result sheet and caller return | Root `supportDrafts`; proposed suggestion draft/reaction and receipt records | High inputs / medium return configuration; no real receipt or delivery; 0.5 day |
| F-026 / S-080 | Ticket list; selection passes ticket identity to S-082 | Synthetic ticket captions, status and last-modified date; list states and pagination | `SupportFixture`; proposed ticket summary page and status catalogue | High ticket-to-detail identity / medium contract; server categories/status lifecycle Unknown; 0.5–1 day |
| F-026 / S-081 | View/send message; verification result and mobile context; new message/reply drafts | Separate compose and history tabs; history selection updates this same screen; explicit preview-only send; no attachment callback recovered | Root per-context drafts and `SupportFixture`; proposed message/history records and reply eligibility | High tabs / medium data; ordering, attachment and reply prerequisites need a service contract; 1–2 days |
| F-026 / S-082 | Inspect selected ticket passed from S-080; nullable-ticket argument also opens new-message composer | Ticket identity and conversation; edit/continue only where eligible; no implied delivery; independent no-ticket review entry | Selected ticket key, `SupportFixture`, per-ticket/new-ticket drafts; proposed ticket detail and ordered message reads | High selected identity / medium lifecycle; real reopen/close/reply policy Unknown; 0.5–1 day |

These overlapping estimates include focused recovery and UI review; they are not
an estimate for implementing real support services.

Displayed notification records need a stable ID, type, optional title, message,
date/time and SEEN/UNSEEN status; selection is local UI state. FAQ records need
question, answer and expanded state. Ticket summaries need ID, caption, question,
trace, status and last-modified time. Thread records need stable ticket/record
identity, question/answer text and timestamps; our role-labelled synthetic
messages flatten those pairs for display. Exact server timestamp format,
thread chronology and nullable-field policies remain Unknown. No attachment
metadata or category is invented to fill missing evidence.

## Model candidates and caller verification

NS-NOTIFICATIONS/NS-SUPPORT trace the actual view-model → application API →
service callers. The inventory's historical F-022 service grouping does not
change these screens' F-026 allocation. E/M identifiers below refer to the
[existing inventory](../../app_profiles/api-structure.md); they do not define
transport contracts. Encrypted envelopes remain **Unknown** business schemas.

| Candidate | Caller-supported relationship | Boundary / proposed future data |
|---|---|---|
| M-012 / M-030 | S-078 public FAQ caller constructs offset/count request and consumes FAQ page (E-395); S-077 also decodes FAQ data through an encrypted wrapper (E-393) | Not the notification page request. FAQ page has start/end/total rows and question/answer data. No frontend FAQ paging callback recovered beyond initial fetch. |
| M-014 / M-058 | S-081 nonactivated ask caller constructs question, trace, mobile and session context; E-379 returns a ticket | Active branch encrypts the logical request; inner HTTP schema remains Unknown. Future synthetic draft and ticket creation records require separate approval. |
| M-016 | S-081 nonactivated close caller supplies record ID, trace and verified context to E-392 | Encrypted active branch exists; real closure eligibility/result is Unknown. Preview never closes a ticket. |
| M-017 / M-058 | S-081 nonactivated edit caller supplies question plus ID/trace/context to E-399 and consumes ticket result | Active branch encrypts. Future draft edit/version and error records; preview keeps text only. |
| M-018 / M-032 | S-081 nonactivated history caller supplies trace/context to E-388 and consumes question-list response | S-082 authenticated thread caller also uses a trace, but no direct typed M-018 HTTP mapping is asserted for it. Future stable ticket-to-thread read. |
| M-019 / M-040 | S-081 nonactivated paginated list caller supplies offset/count/context to E-384 and consumes data plus row metadata | S-080 authenticated list is encrypted (E-380); do not assign its inner transport to M-019/M-040 solely by resemblance. |
| M-020 / M-041 | S-078 verification caller constructs OTP, identification code and reference number; nonactivated E-397 consumes verification result, then passes result/mobile to S-081 | Narrow M-020 catalogue correction replaces the erroneous single payload field with these three semantic string fields. Active verification uses an encrypted wrapper. No genuine session/token is created locally. |
| M-048 | Notification inquiry/activation callers belong to Home/preferences context | Activation type and FCM-validity response; not notification items or support content. FR-06 does not implement activation or push. |
| M-049 | Notification view-model response callback updates unseen count; Home passes current count to S-075 | Confirmed count relationship. Exact E-190 versus E-194 join and global/category reconciliation remain Unknown without the final service invocation join. |
| M-058 | Explicit selected-ticket argument S-080 → S-082; S-081 history selects the same logical ticket locally | ID, caption, question, trace, status and last-modified date. Future ticket summary/detail and stable thread foreign key; never associate a different fixture on lookup failure. |

Notification callers use dedicated page/type/count, ID-list and category-wide
request models, with notification-type and item lists. These do not have new
M-identifiers assigned by FR-06. E-189/E-195/E-198–E-200 are inventory candidates
for list/delete/read operations; the exact encrypted service joins remain
**Unknown**, rather than being approved by their names.

Suggestion uses a description/reaction request through its recovered application
API caller (E-400 candidate; encrypted payload Unknown). S-076's original
answering, similar-question, reaction and escalation service relationships are
unimplemented and are not inferred from the ticket model.

## Recovered rules and explicit preview differences

- Notification item tap opens its sheet and marks an unread item read. Types,
  selection/select-all and single/group/category/all delete sheets are recovered.
  The original request starts at page zero with count 20; our finite six-record
  fixture uses three records per window and loads more at the list boundary.
  No refresh callback was recovered. Local counts derive from all undeleted
  unread records, including records outside the visible page.
- FAQ items toggle individually. No categories or search are added. Authenticated
  child menus depend on incoming server configuration; no fabricated support
  hub or unconditional child link replaces that gap.
- Suggestion completion dismisses the local result and returns to its caller.
  The recovered shortcut-origin variant can instead return to FAQ; no configured
  caller supplies that variant here. Its title/origin argument and dynamic menu
  remain documented requirements, rather than an invented FAQ link.
- Chatbot questions enable at 4–250 raw UTF-16 characters; ticket messages at
  5–250. Suggestion description must be nonempty; reaction defaults to NONE,
  with EXCELLENT/GOOD/FAIR/POOR options and no recovered mandatory-rating rule.
  Whitespace normalization and server validation remain Unknown.
- Recovered ticket states are ASKED, SEEN_BY_BACK_OFFICE, ANSWERED,
  SEEN_BY_CUSTOMER and CLOSED. The fixture exercises ASKED editing, ANSWERED
  continuation/end controls and CLOSED inspection without a composer. End,
  edit and send explain unavailable service behavior; none changes delivery or
  ticket status. There is no recovered ticket category selector.
- Thread response order is consumed without a recovered sort rule. Our oldest
  first synthetic thread order, two-ticket page window and explicit show-more
  control are preview choices. Selecting a history item on S-081 stays on S-081;
  selecting S-080 opens S-082. The compose tab resets selected thread/editor,
  as recovered; keeping non-secret draft text is a deliberate preview difference.
- S-078 uses a labelled local phone/OTP gate with code `12345`, no SMS, resend
  provider or actual authentication. OTP is discarded on recreation. Original
  expiry timer/session-expired return, server errors and active/nonactivated
  authentication branches remain service-dependent and unimplemented.
  The preview reuses the existing 11-digit `09` mobile validator; exact support
  service mobile normalization remains Unknown. S-082's nullable-ticket branch
  starts an empty authenticated composer; its dynamic menu entry is not
  configured, so it has a labelled independent review entry.

## Unavailable behavior

Sending, submission, attachment picking/downloading, arbitrary URLs, external
launches and chatbot answers have no service implementation. Controls explain
that limitation on the current page. No INTERNET or notification permission,
FCM, background delivery, authentication provider, RagBot, LLM, RAG or LangGraph
integration is introduced. Future service work requires separate approval and
validated contracts.

No attachment control was recovered in the inspected support/input composables,
so none is invented. Notification sharing has a recovered control and shows an
unavailable notice; copying synthetic notification text stays local. Original
Back/process-death retention, response/error wording, server menu configuration,
message timestamps and encrypted envelopes remain uncertain.

FR-05's documented fidelity gaps remain open. S-071 retains its independent
review entry because no genuine incoming route has been established. FR-07
remains proposed.
