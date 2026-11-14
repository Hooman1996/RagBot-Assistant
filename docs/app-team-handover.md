# What we ask of the app team

This page says which parts are ours and which are the app team's, exactly what each of their
deliverables must look like, and a message you can send them. It builds on
[what changes for production](bridge/production.md) and the [P8 phase](phases/P8-integration.md).
Nothing here has been agreed with the app team yet. The code it refers to is explained in the
[code walkthrough](code-walkthrough.md).

**When to send it.** The plan puts this work in P8, after the presentation and only if the app team
accepts. The message below is written so you can send it either as an early heads-up now (to get
their answers to the open questions) or as the formal request after the demo.

## 1. Who does what

| Part | Today (replica) | In production | Owner |
|---|---|---|---|
| The assistant: understanding the request, asking questions, RAG answers | Understanding, the one question and preparing are built (#136, [the agent](bridge/agent.md)); RAG answers are not yet | Our backend | **Us** |
| Route planning and the list of what a route needs | `assistant/navigator.py` | Unchanged | **Us** |
| Running the plan and checking every screen | `assistant/task_runner.py` | Unchanged | **Us** |
| The command loop and its API | `assistant/command_queue.py`, `mobile_api.py` | Unchanged, plus real authentication | **Us** |
| The contracts (command, observation, result, pull API) | `contracts/` | Unchanged; changes only by agreement | **Us**, reviewed with them |
| Turning "رضا" into a card number | `assistant/lookups.py` over mock JSON | A real adapter with the same methods | **Us** (the adapter) + **them** (the API behind it) |
| The four bridge operations inside the app | `android/.../bridge/` in the replica | Inside their real app | **Them** |
| Stable tags on screen controls | The replica's tags | On their real screens | **Them** |
| The assistant panel (open, minimize, reopen, cancel, options, progress) | `android/.../assistant/` in the replica ([the panel](bridge/panel.md)) | Inside their real app | **Them**, calling our three panel calls |
| The map of screens, edges, forms and features | `app_profiles/replica.profile.json` | Their real profile | **Them**, checked by our tool |
| Read-only lookup APIs (contacts, cards, accounts...) | `mock_services/fixtures/*.json` | Their APIs | **Them** |
| Confirm, OTP, PIN, signing and every money-moving service | The user's own tap in the replica | Unchanged: the user's own tap | **Them**, never given to us |
| Login, device and user identity | Fixed `replica-dev` | Real sessions | **Agreed together** |

## 2. The deliverables, and exactly what each looks like

### Deliverable 1: the four bridge operations in their app

A small module inside their app that pulls commands from our backend, runs them on their real
screens, and reports back. It does not plan routes and does not decide what is missing; our
backend does. Our replica's version is a working example they can copy from:
`android/app/src/main/java/org/ragbot/replica/bridge/`. The two files that bind to the screens,
`Controls.kt` and `ComposeControls.kt`, are what they replace; `Routes.kt` is unnecessary if their
app can open a screen directly.

**The loop (two HTTP calls, app to backend).** Defined in
[`contracts/openapi/assistant_mobile.json`](../contracts/openapi/assistant_mobile.json).

```text
loop every ~0.5 s while the assistant is active:
  POST /assistant/v1/sessions/{session_id}/resume   body: Observation
     reply: {"kind": "status", ...}                 → nothing to do
     reply: {"kind": "command", "command": {...}}   → run it, then:
  POST /assistant/v1/results                        body: Result
```

The backend never calls into the phone. If they prefer push, that is an open question below.

**A command they receive** ([`command.schema.json`](../contracts/schemas/command.schema.json)):

```json
{
  "protocol_version": 1, "profile_version": 1,
  "task_id": "task-1", "task_revision": 1, "command_id": "cmd-1",
  "device_id": "device-1", "session_id": "session-1",
  "operation": "fill_form",
  "arguments": {
    "form": "S-174",
    "values": {
      "card-transfer-destination": "0000000000000091",
      "card-transfer-amount": { "value": 250000, "unit": "IRR" }
    }
  },
  "expected_observation_version": 3,
  "expected_draft_version": 0,
  "expires_at": "2026-11-08T12:00:30Z"
}
```

| Operation | `arguments` | What their app must do | Ends with |
|---|---|---|---|
| `observe` | `{}` | Nothing; just report | `succeeded` |
| `navigate` | `{"destination": "S-174", "arguments": {"charity": "1"}}` | Reach that screen in the right state, using the chosen row keys. How is their choice: taps, a direct open, a deep link | `succeeded` on arrival, or `blocked` with why (dialog open, a field in error, unexpected screen) |
| `fill_form` | `{"form": "S-174", "values": {ref: text or money}}` | Set each field by its tag **through the form's own handlers**, so the form validates exactly as for a user. A choice field's value is the option's key | `succeeded`, or `blocked` with the refs that show an error |
| `show_review` | `{}` | Open this form's review page, and **stop** | `succeeded`, or `blocked` `["no_review_here"]` |

**Rules their module must keep before running anything** (the replica's `Executor.kt` does all of
these):

1. Refuse a wrong `device_id`, `protocol_version` or `profile_version` → `rejected_policy`.
2. Refuse an unknown operation, an unknown screen, an unknown argument, or a field that is not
   fillable → `rejected_policy`.
3. Refuse a command at or after `expires_at` (UTC) → `rejected_expired`.
4. Refuse a command whose `expected_observation_version` is not the current one → `rejected_stale`
   with `error_code: "stale_observation"`; same for the draft version → `stale_draft`.
5. Run each `command_id` at most once. If the same command arrives again, send back the first
   result (keep a small journal).
6. Run one command at a time.
7. Never tap a confirm, submit, pay, OTP, PIN, password, CVV2, signature or consent control, and
   never read or write a secret field, **even if a command asks**. This must be enforced in their
   app, not only in our data.

**An observation they send** ([`observation.schema.json`](../contracts/schemas/observation.schema.json)):

```json
{
  "observation_version": 7, "app_version": 1, "profile_version": 1,
  "screen": "S-174", "modal": null, "loading": false,
  "fields": [
    { "ref": "card-transfer-destination", "value": "****0091" },
    { "ref": "card-transfer-amount", "value": "250,000" }
  ],
  "draft": { "draft_id": "draft-1", "draft_version": 2, "review_version": 0 },
  "supported_operations": ["observe", "navigate", "fill_form", "show_review"]
}
```

- `screen` is a profile screen ID (or `"unknown"`); `modal` names an open dialog; `loading` is
  true while a spinner shows.
- `fields` are the current form's **non-secret** values as shown, with card, account and IBAN
  numbers masked to the last four digits.
- `observation_version` rises whenever anything visible changes. `draft_version` rises on every
  fill, including the user's own edits; `review_version` is the draft version the review shows.

**A result they send** ([`result.schema.json`](../contracts/schemas/result.schema.json)):

```json
{
  "command_id": "cmd-1",
  "outcome": "blocked",
  "postcondition": null,
  "observation": { "...": "a fresh observation, as above" },
  "draft_version": 2,
  "needs": ["card-transfer-amount"],
  "trace": [ { "from": "S-026", "to": "S-174", "action": "menu-card-transfer" } ]
}
```

`outcome` is one of `succeeded`, `blocked` (then `needs` is required), `failed`, `uncertain`,
`not_executed`, `rejected_stale`, `rejected_expired`, `rejected_policy`, `cancelled`. `error_code`
is a short code such as `field_not_set`, never free text. `trace` is optional, for `navigate`.

**How we will check it:** our backend checks every body against the schemas and refuses anything
else; their port of our policy tests (`PolicyTest.kt`) with every real user-only action added; and
the card-to-رضا journey on their test build reaching the review and staying there.

### Deliverable 2: stable tags on their controls

Every control the profile names needs a stable ID that does not change between builds or
languages: Compose `testTag`, or a view `resource-id`. Naming rules from the replica:

- A field's tag is its `ref`, for example `card-transfer-amount`.
- A picker that opens a list is `<ref>-picker`; each option is `<ref>-<key>`, for example
  `account-transfer-reason-0`.
- A row in a list is `<prefix>-<key>-<action>`, for example `repetitive-*-repeat`, where `*` is
  the chosen row's key.
- The review button of a form is a tag of its own, for example `card-transfer-review`.
- Confirm, OTP and similar controls also get tags, so they can be put on the user-only list.

### Deliverable 3: the real profile

One JSON file, the same shape as
[`app_profiles/replica.profile.json`](../app_profiles/replica.profile.json), that passes
[`app_profiles/profile.schema.json`](../app_profiles/profile.schema.json) and
`python tools/project_checks.py profile`. The shape, with one of each item:

```json
{
  "profile_version": 1,
  "screens": ["S-026", "S-174", "S-176", "S-200"],
  "edges": [
    { "from": "S-026", "to": "S-174", "action": "menu-card-transfer" },
    { "from": "S-174", "to": "S-176", "action": "card-transfer-review" },
    { "from": "S-200", "to": "S-176", "action": "repetitive-*-repeat",
      "requires": [ { "ref": "repetitive", "type": "ref", "lookup": "repetitive",
                      "description": "تراکنش تکراری", "ask": "کدام تراکنش تکرار شود؟" } ] }
  ],
  "forms": [
    { "id": "S-174", "fields": [
      { "ref": "card-transfer-source", "type": "ref", "requires": true, "lookup": "cards",
        "description": "کارت مبدا", "ask": "از کدام کارت انتقال انجام شود؟" },
      { "ref": "card-transfer-amount", "type": "money", "requires": true,
        "description": "مبلغ انتقال", "ask": "چند ریال منتقل شود؟" },
      { "ref": "card-transfer-otp", "type": "text", "secret": true }
    ] }
  ],
  "features": [
    { "intent": "card_transfer", "description": "Prepare a card-to-card transfer up to its review.",
      "destination": "S-174", "form": "S-174", "lookups": ["cards", "contacts"],
      "required_slots": ["card-transfer-source", "card-transfer-destination", "card-transfer-amount"] }
  ],
  "user_only_actions": ["card-transfer-submit", "card-transfer-otp"]
}
```

| Part | What they fill in |
|---|---|
| `screens` | Every screen the assistant may reach, by a stable ID |
| `edges` | Each move: from, to, and the tags tapped (joined by ` > `). A `*` stands for a chosen row, and such an edge lists what must be chosen in `requires` |
| `forms` | Each form's fields: `ref` (the tag), `type` (`text`, `money`, `ref`, `choice`), `requires` if the form will not continue without it, a short Persian `description`, a Persian `ask` question, and a `lookup` for a `ref`. Secret fields are listed with `"secret": true` and no question |
| `features` | Each journey the assistant may prepare, ending **before** the user's own step |
| `user_only_actions` | Every confirm, submit, pay, OTP, PIN, password, CVV2, signature and consent control. We review this list with them before anything else |

The navigator reads this file as data, so a new app build means a new profile, not new code.

### Deliverable 4: read-only lookup APIs

Our adapter (`assistant/lookups.py`) needs, for each lookup source, **the current user's records**.
It does three things with them: list the records (`records`), present each as key, value and label
(`candidates`), and pick exactly one from the user's words (`resolve`). The real adapter keeps
these methods and calls their APIs instead of reading JSON.

**Proposed shape, to agree with them** (one read-only call per source, scoped to the signed-in
user):

```text
GET /<their-prefix>/lookups/contacts
200 {"records": [
      {"key": "reza", "label": "رضا", "card": "0000000000000091", "iban": "IR070000000000000000000077", "account": "0000000000077", "wallet": "0000000000000201"}
    ]}
```

| Source | Each record needs | Replaces | Used for |
|---|---|---|---|
| `contacts` | key, name, card, IBAN, account, wallet | `contacts.json` | "to رضا" → a destination |
| `cards` | key, masked number, kind | `cards.json` | source card |
| `accounts` | key, type, IBAN | `accounts.json` | source account |
| `bills` | bill ID, payment ID, company | `bills.json` | saved bills |
| `loans`, `loan_groups` | key, title | `loans.json` | repayments |
| `cheque_books`, `digital_cheques`, `sayad_cheques`, `cardboard_tasks` | key, label | `cheques.json` | cheque choices |
| `vehicles` | key, label | `vehicles.json` | plates and licences |
| `wallets`, `wallet_statement` | number, title | `wallets.json` | wallet choices |
| `transactions`, `statements`, `standing_orders`, `group_history`, `repetitive` | key, title | `transactions.json` | repeat and history choices |
| `charities`, `insurance` | key, name | `charities.json`, `insurance.json` | charity and policy choices |

Rules: **read only**; no money-moving call; amounts as integers with a unit; nothing secret; the
user's own records only. Only the sources of the features the owner picks are needed at first.

### Deliverable 5: the assistant panel

An **Assistant** entry beside their chatbot's, opening a panel where the user talks to our backend.
The replica's panel (`android/app/src/main/java/org/ragbot/replica/assistant/`, #144) is the working
reference; [the panel](bridge/panel.md) has every rule. In short:

- It calls three routes of `contracts/openapi/assistant_mobile.json`: `POST /assistant/v1/messages`
  (the user's words, or an option with its question's ID, plus their current observation),
  `GET /assistant/v1/history` (to show progress and to reopen) and
  `POST /assistant/v1/sessions/{session_id}/cancel`.
- **Minimize** keeps the conversation and shows a small entry to reopen it; **cancel task** is a
  separate, visibly different control.
- It sits in the app's own window (not a dialog), catches no Back press, and shows no spinner, so
  the bridge can still see and tap the screen behind it.
- It never confirms, submits or pays: the user confirms on their own review screen, as today.

Acceptance: on their test build, a request typed in the panel reaches the review with the panel
minimized, and cancel stops it.

### Also needed from them

- A development or staging backend address and a test build with the loop switched on, off by
  default in real builds, with a controlled way to switch it on.
- How a device and a signed-in user are identified to our backend (today `session_id` equals the
  device ID).

## 3. What we never ask for

- The confirm or submit calls, or any route to them.
- A PIN, OTP, password, CVV2 or signature, in either direction.
- Any money-moving API, database access or credentials.

## 4. Open questions to put to them

The full list is in [production.md](bridge/production.md#open-questions-for-the-app-team). The
most important:

1. Are their screens Compose, Android views, or a mix, and do controls have stable IDs?
2. Which screens can open directly with their arguments, and which need steps first?
3. Polling, as now, or a push channel?
4. Which login and session does a command run under, and how does our backend prove it is ours?
5. Are any menus server-driven? Then the profile may need to come from their server.
6. Pickers: fill by key, as in the replica, or by another reference? (This decides whether the
   lookups must return full numbers or only keys.)

## 5. Who to contact and how

- **Who:** the Android lead (deliverables 1 and 2), the owner of their customer-data APIs
  (deliverable 4), and whoever owns their navigation map or product flows (deliverable 3). Ask
  their lead to name one contact for all of it.
- **How:** a short written message with this page linked, followed by a 30-minute call to walk
  through the card-to-رضا demo and the open questions.
- **What to attach:** this page, [production.md](bridge/production.md),
  [safety.md](bridge/safety.md), and a screen recording of `tools/bridge_e2e.py` on the replica.

## 6. Draft message

> **Subject: In-app assistant: what we would need from the app team**
>
> Hello,
>
> We are building an assistant for our mobile banking app. It keeps RagBot's question answering
> and adds one thing: on request, it can take the user to a screen and fill a form up to its review
> page. **The user always confirms, enters OTP or PIN, and submits personally**: the assistant has
> no way to do any of these, and never sees a secret.
>
> We have a working prototype on a replica of the app with synthetic data. The backend plans the
> route and checks each screen; the app only follows four simple commands: observe the screen,
> go to a screen, fill a form, and open its review page.
>
> To run this on the real app, we would need four things from your team:
>
> 1. **A small bridge module in the app** that pulls these four commands from our backend, runs
>    them through your screens' own handlers (so your validation runs as usual), and reports what
>    the screen shows. The message formats are fixed and documented; we have a reference version
>    in Kotlin you can copy from.
> 2. **Stable IDs (tags) on the controls** of the screens involved.
> 3. **A map of those screens**: which screen leads to which, each form's fields, and which
>    controls only the user may touch (confirm, OTP, PIN, and so on). We will review that last list
>    with you first.
> 4. **Read-only APIs** for the signed-in user's saved data: contacts and saved destinations,
>    cards, accounts, and later bills, loans and similar. No money-moving APIs.
>
> Everything is described here, with examples of each message:
> docs/app-team-handover.md in our repository.
>
> Before we plan anything, we would like your view on a few questions: are your screens in Compose
> or Android views; can screens be opened directly; how should a device and user be identified to
> our backend; and do you prefer polling or push?
>
> Could you name one contact on your side, and would a 30-minute call next week suit you? We can
> show the demo on the replica then.
>
> Thank you,
> hooman
