# Code walkthrough

This page explains our code in plain words: which file does which part of the job, what each
block takes in, what it gives back, and where its longer documentation lives. Every Python file is
covered. The backend files in `assistant/` are explained block by block, with line numbers, so
every line is accounted for. The Kotlin bridge is explained function by function. What the app team
must build is in [the app-team handover](app-team-handover.md).

Line numbers match `main` at commit `95533cd` (8 Nov 2026), except `command_queue.py`,
`task_runner.py`, `bridge_link.py` and `steps.py`, which match #134 (9 Nov 2026), and `app.py`,
`graph_tasks.py`, `graph_page.py`, the agent's files (5.17 to 5.24), `tools/project_checks.py`,
`tools/stand_in_app.py` and `main_debug.py`, which match #136 (9 Nov 2026), and the evaluation
runner (9.7 and 9.8), which matches #138 (14 Nov 2026). If a file changes, the numbers move, but the
order of the blocks usually stays the same.

## Contents

1. [The whole system on one page](#1-the-whole-system-on-one-page)
2. [One journey, file by file: card to رضا](#2-one-journey-file-by-file-card-to-رضا)
3. [Python words you will meet](#3-python-words-you-will-meet)
4. [The data files the code reads](#4-the-data-files-the-code-reads)
5. [The Python backend, file by file](#5-the-python-backend-file-by-file)
6. [The graph page in the browser](#6-the-graph-page-in-the-browser)
7. [The Kotlin bridge inside the replica](#7-the-kotlin-bridge-inside-the-replica)
8. [The replica app around the bridge](#8-the-replica-app-around-the-bridge)
9. [Tools: the check runner and the helper scripts](#9-tools-the-check-runner-and-the-helper-scripts)
10. [The tests, and what each one proves](#10-the-tests-and-what-each-one-proves)
11. [Things worth knowing](#11-things-worth-knowing)

## 1. The whole system on one page

There are two programs. They talk over HTTP, and the phone always starts the conversation.

```text
 Python backend (assistant/, port 8100)              Replica app on the emulator (android/)
 ───────────────────────────────────────             ──────────────────────────────────────
 graph page (browser) ─┐                              CommandPoller  every 0.5 s:
                       ▼                                 1. POST .../resume  {observation}
 graph_tasks ──► navigator   (plans the route)  ◄──────     reply: {command} or {status}
      │          lookups     (رضا → card number)          2. Executor runs the command
      ▼                                                      Policy + Actions → ComposeControls
 task_runner  (navigate, fill, check, review)                → the real Compose screens
      │                                                   3. POST .../results {result}
      ▼                                         ──────►
 bridge_link ──► command_queue ◄── mobile_api
```

- **The backend thinks.** It decides the route, which values are needed, turns "رضا" into a card
  number, and checks every screen after every step.
- **The app obeys and reports.** It knows four operations only (`observe`, `navigate`,
  `fill_form`, `show_review`). It never confirms, submits or pays.
- **The contract sits in the middle.** Every message is checked against
  `contracts/schemas/*.schema.json`.

The design decision is [D-023](decisions/0023-python-navigator-thin-bridge.md). The short version
for the bridge is [docs/bridge/README.md](bridge/README.md). How to run it yourself is
[docs/bridge/try-it.md](bridge/try-it.md).

**The agent** (#136) sits on top of `task_runner`: it turns one Persian sentence into the feature,
its values and one question for what is missing, with the Gemma model and the TEI embeddings, and
drives the same runner ([the agent](bridge/agent.md), sections 5.17 to 5.24). The graph page has a
chat box for it, and `main_debug.py` runs it in one process. The graph page's Run still plays the
user's part without the model. What is **not built yet**: RAG answers, the in-app panel and the real
assistant API.

## 2. One journey, file by file: card to رضا

This is the acceptance journey (`python tools/bridge_e2e.py`). Following it once is the fastest way
to see which file does what. The long version, with the real JSON of every message, is
[docs/bridge/command-lifecycle.md](bridge/command-lifecycle.md).

| # | What happens | File and function |
|---|---|---|
| 1 | The app starts and, if the flag file exists, starts its loop | `MainActivity.kt` → `BridgeConfig.enabled` → `CommandPoller.start` |
| 2 | Every 0.5 s the app sends what it sees (screen `S-026`, version 7) | `CommandPoller.pollOnce` → `Observer.observe` → `BackendClient.resume` |
| 3 | The backend checks that JSON, stores it, and answers "nothing to do" | `mobile_api.resume` → `contract_check.problems` → `command_queue.next_command` |
| 4 | You click `S-174` on the graph page | `graph_task.js clicked` → `GET /graph/plan/S-174` |
| 5 | The backend plans: `S-026 → S-174` by tapping `menu-card-transfer`; three questions | `graph_page.plan` → `graph_tasks.preview` → `navigator.plan_feature` |
| 6 | It suggests demo answers (`card-1`, `رضا`, `250000`) | `demo_values.demo_value` |
| 7 | You press Run | `graph_task.js run` → `POST /graph/run` → `graph_tasks.start` (background thread) |
| 8 | Before anything moves, every value is prepared: `رضا` → `0000000000000091`; `250000` → `{"value": 250000, "unit": "IRR"}` | `task_runner.prepared` → `lookups.resolve`, `task_runner.money_value` |
| 9 | The runner queues `navigate {"destination": "S-174"}` and waits | `task_runner.move` → `bridge_link.send` → `command_queue.queue_command` |
| 10 | On its next poll the app receives that command | `command_queue.next_command` → `mobile_api.reply_with` |
| 11 | The app checks it (right device? not expired? still the same screen?) and walks the edge by tapping `menu-card-transfer` | `Executor.execute` → `Navigator.navigate` → `Actions.tap` (asks `Policy.mayTap`) → `ComposeControls.click` |
| 12 | The app posts the result with a fresh observation (`S-174`) | `Executor.report` → `BackendClient.postResult` |
| 13 | The backend records it; the runner sees it and checks the screen is `S-174` | `mobile_api.post_result` → `command_queue.record_result` → `bridge_link.wait_for` → `task_runner.move` |
| 14 | The runner sends `fill_form` with the three values | `task_runner.fill` |
| 15 | The app types each value through the field's own handler and reads it back | `FormFiller.fill` → `Actions.type` (asks `Policy.mayType`) → `ComposeControls.setText` |
| 16 | The runner compares what the screen shows (`****0091`, `250,000`) with what it sent | `task_runner.shows` |
| 17 | The runner sends `show_review`; the app taps `card-transfer-review` and lands on `S-176` | `task_runner.show_review` → `FormFiller.showReview` |
| 18 | The run ends in state `review`. The confirm button is the user's alone | `TaskReport.state = "review"`; `ProfileUserOnly.kt` lists `card-transfer-submit` |

## 3. Python words you will meet

You only need these to read every Python line in this project.

| Word | Meaning here |
|---|---|
| `"""text"""` under a `def` or `class` | A docstring: a sentence that says what the block does. It does not run. |
| `import x` / `from x import y` | Use code from another file or library. |
| `CONSTANT = 30` (capital letters) | A fixed setting, read but never changed. |
| `def name(a: str) -> dict:` | A function. `a: str` means "a is text"; `-> dict` means "gives back a dictionary". The types are notes; Python does not enforce them. |
| `dict` / `list` / `tuple` / `set` | Key → value map / ordered list / list that cannot change / collection with no repeats. |
| `X \| None` | "Either an X or nothing". |
| `class Name(Exception)` | A named error. `raise Name(...)` throws it; `try: ... except Name:` catches it. |
| `@dataclass` | A class that is just a bundle of named values. `frozen=True` means it cannot change after it is made. |
| `field(default_factory=list)` | "Start each new bundle with its own empty list." |
| `@property` | A method you read like a value: `plan.requirements`, not `plan.requirements()`. |
| `Protocol` | "Anything that has these methods will do." Used so tests can pass a fake. |
| `[x for x in items if ...]` | Build a new list from `items`, keeping those that pass the test. |
| `{k: v for ...}` | The same, but builds a dictionary. |
| `next((x for x in items if ...), None)` | The first item that passes, or `None`. |
| `any(...)` / `all(...)` | True if at least one / every item passes. |
| `a or b` | `a` if it is not empty, otherwise `b`. |
| `**d` inside `{...}` | Copy every key of dictionary `d` into the new one. |
| `*items` inside `[...]` | Copy every item of list `items` into the new one. |
| `Path(__file__).resolve().parent.parent` | The folder two levels above this file: the repository root. |
| `FastAPI`, `APIRouter` | The web framework. A router is a group of URLs. |
| `@router.post("/x")` above a function | "When someone POSTs to `/x`, run this function and send back what it returns as JSON." |
| `BaseModel` with `extra="forbid"` | Pydantic: checks a request body and refuses any field not listed. |
| `HTTPException(status_code=409, detail=...)` | Send an error reply instead of a normal one. |
| `Annotated[dict, Body()]` | "Take the whole JSON body as a dictionary." |
| `Thread(target=f, daemon=True).start()` | Run `f` in the background; it dies with the server. |
| `deque` | A queue that is fast to take from the front. Used for breadth-first search. |
| `fnmatchcase(tag, pattern)` | Does `tag` fit `pattern`, where `*` means "anything"? |

## 4. The data files the code reads

The code is small because the knowledge lives in data files.

| File | What it holds | Read by | Documented in |
|---|---|---|---|
| `app_profiles/replica.profile.json` | The replica's map: 259 screens, 265 edges, 103 forms, 77 features, the user-only list | `navigator.py`, `graph_data.py`, `graph_tasks.py`, the profile check | [app_profiles/README.md](../app_profiles/README.md) |
| `app_profiles/navigation-graph.json` | The whole original app's screens (built and not built), for drawing grey nodes | `graph_data.py` | [screen-catalogue.md](../app_profiles/screen-catalogue.md) |
| `app_profiles/profile.schema.json` | The shape every profile must have | the profile and contracts checks | same README |
| `mock_services/fixtures/*.json` | All synthetic people, cards, accounts, bills... | `lookups.py`; also packed into the app for its in-app mock bank | [docs/runbook/mock-data.md](runbook/mock-data.md) |
| `contracts/schemas/*.schema.json` | The exact shape of a command, observation and result | `contract_check.py`, the contracts check | [contracts/](../contracts/) |
| `contracts/transitions/command.json` | Which command state may follow which | the contracts tests | [command-lifecycle.md](bridge/command-lifecycle.md) |
| `contracts/openapi/*.json` | Every HTTP API we will ever offer, and the one user-only API we never call | the contracts check | [plan](plan/RagBot_Assistant_Final_Project_Plan_v2.md) |

`replica.profile.json` is **generated** from Kotlin (`ReplicaGraph.kt`, `ProfileForms.kt`,
`ProfileWords.kt`, `ProfileSelections.kt`, `ProfileFeatures.kt`, `ProfileUserOnly.kt`) by the JVM
test `ReplicaProfileTest`. Never edit it by hand. Its pieces look like this:

```json
{ "from": "S-026", "to": "S-174", "action": "menu-card-transfer" }
```
An **edge**: on screen `S-026`, tap the control tagged `menu-card-transfer`, and you reach
`S-174`. Several taps are joined with ` > `. A `*` is a row key chosen at run time, and such an
edge carries `"requires": [...]` to say what must be chosen.

```json
{ "id": "S-174", "fields": [
  { "ref": "card-transfer-source", "type": "ref", "requires": true,
    "ask": "از کدام کارت انتقال انجام شود؟", "description": "کارت مبدا", "lookup": "cards" },
  { "ref": "card-transfer-destination", "type": "ref", "requires": true,
    "ask": "به چه کسی یا کدام کارت منتقل شود؟", "description": "کارت مقصد", "lookup": "contacts.card" },
  { "ref": "card-transfer-amount", "type": "money", "requires": true,
    "ask": "چند ریال منتقل شود؟", "description": "مبلغ انتقال" } ] }
```
A **form**: the fields of a screen. `ref` is the control's test tag. `requires` means the screen
will not continue without it. `lookup` says where a name is turned into a value. A field with
`"secret": true` (PIN, OTP, password, CVV2) is listed only so it can be refused.

```json
{ "intent": "card_transfer", "destination": "S-174", "form": "S-174",
  "required_slots": ["card-transfer-source", "card-transfer-destination", "card-transfer-amount"],
  "lookups": ["cards", "contacts"] }
```
A **feature**: one journey the assistant can prepare. It ends before the user's own step.

`"user_only_actions": ["card-transfer-submit", "card-transfer-otp", ...]` is the list of tags only
the user may touch. Both the Python navigator and the Kotlin `Policy` refuse them.

## 5. The Python backend, file by file

All in `assistant/`. Read them in this order: start-up, then the command loop, then the brain,
then the graph page. The same order with one-line summaries is in
[docs/bridge/README.md](bridge/README.md#read-the-code-in-this-order).

### 5.1 `__init__.py` (1 line)

| Lines | What it does |
|---|---|
| 1 | Marks `assistant/` as a Python package, so `from assistant.x import y` works. The docstring says what is in it: the command loop, the graph page and the agent. |

### 5.2 `__main__.py` — start the server

**Run by:** `python -m assistant`. **Input:** nothing. **Output:** a web server on
`127.0.0.1:8100` until you stop it.

| Lines | What it does |
|---|---|
| 1-4 | Docstring: port 8100 because 8000 belongs to another service on the server; local address only. |
| 6 | `uvicorn` is the program that serves a FastAPI app over HTTP. |
| 8 | Brings in `create_app` from `app.py`. |
| 10 | Runs the next line only when the file is started directly, not when imported. |
| 11 | Builds the app and serves it on the local address only, so nothing outside the machine can reach it. |

### 5.3 `app.py` — put every part together

**Called by:** `__main__.py`, `main_debug.py --replica` and the tests. **Input:** optionally a
`CommandQueue` (tests pass one with a fake clock; `main_debug.py` shares its own). **Output:** a
ready FastAPI app with all routes.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: what the app is; the chat box's agent reads the models only when a turn runs; no RAG and no bank. |
| 9 | `subprocess` lets Python run a command, here `git`. |
| 11-24 | Imports: the web framework, the agent's pieces (chat box, graph, tools, feature index, model clients), the bridge link, the queue, the graph data readers, the graph tasks and the route groups. |
| 26 | `DEVICE_ID = "replica-dev"`: the one development device. The app uses the same name (`BridgeConfig.DEVICE_ID`). |
| 29-32 | `source_revision()`: runs `git rev-parse --short HEAD` and returns, for example, `95533cd`. The graph page shows it, so you can check the running server is the code you expect (the AGENTS.md "Running services" rule). |
| 35-36 | `create_app(queue=None)`: the builder. |
| 37 | Use the queue passed in, or make a new one. |
| 38 | Make an empty FastAPI app with a title. |
| 39 | Add the app's two routes (`/assistant/v1/...`), sharing the queue. |
| 40 | Add the development route (`/test-harness/v1/commands`), with the list of built screens so an unbuilt one is refused. |
| 41 | Read the replica profile once. |
| 42 | Make the `GraphTasks` helper: the queue, the device ID and the profile. |
| 43 | The feature index over the profile's features, with a TEI client. Nothing is embedded until the first chat turn. |
| 44 | A `BridgeLink` to the replica: the agent's bridge, the same kind a Run uses. |
| 45 | The agent's `Tools`: the chat model, the index, the graph tasks' navigator and lookups, the bridge, and `current_screen`. |
| 46 | The chat box, with the compiled agent graph and the graph tasks. |
| 47 | `tasks.chat_busy = chat.busy`: a Run is refused while a chat turn runs, and the chat refuses a turn while a Run goes. |
| 48-50 | Add the graph page routes (`/graph/...`), with the chat, the drawn graph data and the git revision. |
| 51 | Give the finished app back. |

### 5.4 `contract_check.py` — is this JSON allowed?

**Called by:** `mobile_api.py` for every body the app posts, and by tests. **Input:** a schema
file name and a value. **Output:** a list of problems; an empty list means valid.

| Lines | What it does |
|---|---|
| 1-5 | Docstring: it only reads the schemas and must never loosen them. |
| 7-11 | Imports: JSON reading, file paths, and the `jsonschema` library (draft 2020-12) with its `referencing` helper. |
| 13 | `SCHEMAS`: the path of `contracts/schemas/`. |
| 16-22 | `load_registry()`: reads every `*.schema.json` and registers it under its file name. Needed because `result.schema.json` says `"$ref": "observation.schema.json"`, and the validator must find that file by name. |
| 25 | Does this once, when the module is first imported. |
| 28-32 | `problems(schema_file, value)`: reads the named schema, makes a validator that can follow references, and returns the message of every error, for example `'fields' is a required property`. |

### 5.5 `command_queue.py` — one command at a time, per device

This is the heart of the loop. It follows the state table in `contracts/transitions/command.json`.
Lifecycle in pictures: [command-lifecycle.md](bridge/command-lifecycle.md).

**Called by:** `mobile_api.py` (the app reports in or posts a result), `test_harness_api.py` and
`bridge_link.py` (someone queues a command). **Holds:** everything in memory. A restart forgets it.

| Lines | What it does |
|---|---|
| 1-6 | Docstring: one outstanding command per device; memory only; it must never build a submit or confirm. |
| 8-12 | Imports: environment variables, random IDs, a type for "a function", dataclasses, and time helpers. |
| 14 | `show` from `steps.py`: prints a step when `ASSISTANT_TRACE=1`, otherwise does nothing. |
| 16 | `PROTOCOL_VERSION = 1`: the wire version. The app refuses any other (`Executor.policyProblem`). |
| 17 | `PROFILE_VERSION = 1`: the profile version. The app refuses any other. |
| 18 | `DEBUGGING`: true when the backend was started with `ASSISTANT_DEBUG=1`. Read once, at import. |
| 19 | `COMMAND_LIFETIME`: a command expires 30 seconds after it is queued, or one hour when debugging, so a paused breakpoint does not expire it ([debugging](runbook/debugging.md)). |
| 20 | `FINISHED_KEPT = 50`: remember the last 50 finished command IDs, to spot repeats. |
| 21 | `OPERATIONS`: the only four operations it will ever build. |
| 22 | `NEEDS_DRAFT_VERSION`: these two must also say which draft version they expect. |
| 25-26 | `DeviceNotSeen`: error when nobody has heard from the device yet, so there is no screen version to aim at. |
| 29-30 | `DeviceBusy`: error when the device already has a different command waiting. |
| 33-34 | `UnknownCommand`: error when a result arrives for a command we never sent. |
| 37-45 | `Device`: what we know about one device. `observation` = its last report; `outstanding` = the command waiting or running; `delivered` = has the app picked it up?; `last_result` = the latest result; `finished` = recent finished IDs. |
| 48-50 | `utc_now()`: the current time in UTC. Tests replace it with a clock they move by hand. |
| 53-55 | `format_time()`: writes a time as `2026-11-08T10:00:30Z`, the exact shape the schema requires. |
| 58-63 | `CommandQueue.__init__`: keeps the clock and an empty dictionary of devices. |
| 65-67 | `device(id)`: returns that device's record, creating an empty one the first time. |
| 69-70 | `queue_command(device_id, operation, arguments)`: **Input** device, operation, arguments. **Output** the full command dictionary. |
| 71-72 | Refuses any operation outside the four. This is one of the walls of rule 4. |
| 73-75 | Refuses a device that has not reported in (`DeviceNotSeen`). |
| 76 | Throws away a waiting command that has expired (see 142). |
| 77-81 | If a command is already waiting: the same request again gets the same command back (safe to repeat); a different one is `DeviceBusy`. |
| 82-94 | Builds the command: versions, a new `task_id` and `command_id`, the device, `session_id` (the same as the device in development), the operation and arguments, `expected_observation_version` (the screen version this command was planned against) and `expires_at` (now + `COMMAND_LIFETIME`). |
| 95-97 | For `fill_form` and `show_review`, adds `expected_draft_version`: the draft version the app last reported, or 0 when there is no draft. |
| 98-105 | Prints the step "command queued": the first 8 characters of the command ID, the operation, arguments, expected observation version and expiry. |
| 106-108 | Stores it as outstanding, not yet delivered, and returns it. |
| 110-111 | `next_command(device_id, observation)`: called on every poll. **Input** the app's observation. **Output** a command to run, or `None`. |
| 112-113 | Stores the observation as the latest. |
| 114 | Drops an expired command first. |
| 115-117 | Nothing waiting, or already handed over: nothing to send. A command is never delivered twice. |
| 118-120 | If the screen changed since the command was planned (version differs), the command is closed as `rejected_stale` and not sent. `bridge_link` will try again with the new version. |
| 121 | Otherwise marks it delivered. |
| 122-126 | Prints the step "command handed to the app": the command ID and the observation version it runs on. |
| 127 | Returns the command. |
| 129-130 | `record_result(result)`: called when the app posts a result. |
| 131-134 | If this command already finished, a repeat changes nothing (the network may resend). |
| 135-139 | If it is the outstanding command, store the result's fresh observation and finish it. |
| 140 | Otherwise `UnknownCommand`. |
| 142-143 | `drop_if_expired(device)`: closes a command that is past its time, so it can never run late. |
| 144-146 | Nothing waiting, or not expired yet: do nothing. (The times are compared as text, which works because of the fixed format.) |
| 147-151 | If the app had already picked it up, it may have run, so the result is `uncertain` (`no_result_before_expiry`). If not, it is `rejected_expired`. |
| 153-155 | `close_unsent()`: closes the outstanding command on the backend's side. |
| 156 | Prints the step "command closed here" with the outcome and error code (stale, expired or uncertain). |
| 157-165 | Writes the result (not the app's), with the last known observation and draft version 0, and finishes. |
| 167-172 | `finish()`: keeps the result as `last_result`, clears the outstanding command, and adds its ID to the last 50 finished. |

### 5.6 `mobile_api.py` — the two calls the app makes

Follows `contracts/openapi/assistant_mobile.json` (only the two calls the loop needs exist now).

**Called by:** the app's `BackendClient`. **Input:** JSON from the app. **Output:** JSON back.

| Lines | What it does |
|---|---|
| 1-6 | Docstring: every body is checked; there is no route that submits, confirms or pays. |
| 8-13 | Imports: types, the web framework, the queue and the checker. |
| 15 | `IDLE_REPLY`: the "nothing to do" answer, `{"kind": "status", "task_state": "paused"}`. |
| 18-22 | `reply_with(command)`: wraps a command as `{"kind": "command", "command": {...}}`, or returns the idle answer. This is the shape of `response.schema.json`. |
| 25-29 | `require_valid(schema, value)`: if the checker finds problems, refuse with HTTP 422 and the list of problems. |
| 32-34 | `build_mobile_router(queue)`: makes the route group under `/assistant/v1`. |
| 36-40 | `POST /assistant/v1/sessions/{session_id}/resume`. **Input** an observation. Checks it, gives it to `queue.next_command`, and replies with the next command or idle. In development `session_id` is the device ID. |
| 42-50 | `POST /assistant/v1/results`. **Input** a result. Checks it, records it; an unknown command gets HTTP 409 `unknown_command`. Always replies idle: the app asks for its next command on its next poll. |
| 52 | Returns the route group. |

### 5.7 `test_harness_api.py` — move the replica by hand

Follows `contracts/openapi/test_harness.json`. **Never an assistant tool.**

**Called by:** the graph page (a node with no questions), `tools/bridge_e2e.py`, tests.
**Input:** `{"device_id": "...", "destination": "S-xxx"}`. **Output:** the queued command, or an error.

| Lines | What it does |
|---|---|
| 1-6 | Docstring: it takes a destination and nothing else, so it cannot ask for a fill, a confirm or a payment. |
| 8-11 | Imports. |
| 14-20 | `QueueRequest`: the body may have exactly `device_id` and `destination`. Any other field is refused (HTTP 422). |
| 23-25 | `build_harness_router(queue, built_screens)`: the route group under `/test-harness/v1`. |
| 27-28 | `POST /test-harness/v1/commands`. |
| 30-31 | A screen the replica does not have: HTTP 404 `screen_not_built`. |
| 32-33 | Queues a `navigate` to that screen and returns the command. |
| 34-37 | Device not seen, or busy: HTTP 409 with the reason. |
| 39 | Returns the route group. |

### 5.8 `navigator.py` — plan the route and list what it needs

The brain, part 1. Plain breadth-first search over the profile's edges. No LLM, no app, standard
library only. Explained with an example in
[command-lifecycle.md, step 1](bridge/command-lifecycle.md#step-1-the-navigators-plan).

**Called by:** `graph_tasks.py`, `task_runner.py`, and the profile check in
`tools/project_checks.py`. **Input:** the profile; the current screen; the target screen.
**Output:** a `Plan` (route + legs + requirements), or `NoRoute` / `CrossesUserOnly`.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: never plans over a user-only action; never lists a secret field. |
| 9-11 | Imports: a queue, dataclass helpers, and pattern matching for `*`. |
| 13 | `HOME = "S-026"`: Home. |
| 14 | `BACK = "back"`: the action name for pressing Back. |
| 17-18 | `NoRoute`: no allowed route to the target. |
| 21-22 | `CrossesUserOnly`: a route exists, but only through a step that is the user's. |
| 25-35 | `Requirement`: one value the route needs. `screen` where it is asked; `ref` the tag; `kind` is `field` (typed on a form) or `selection` (a row chosen before a move); `type` is `text`, `money`, `ref` or `choice`; `ask` the Persian question; `description`; `lookup` where names are resolved. |
| 38-44 | `Leg`: one stop of the plan. Navigate to `screen` (with `selections` as arguments), then fill `fields`. |
| 47-53 | `Plan`: the `target`, the `route` (list of edges), and the `legs`. |
| 55-59 | `requirements`: every selection and field of every leg, in order, each once (`dict.fromkeys` removes repeats and keeps order). |
| 61-67 | `as_json()`: the plan as plain JSON for the graph page. |
| 70-72 | `tags(action)`: `"a > b"` becomes `["a", "b"]`, the taps in order. |
| 75-90 | `search(start, target, edges)`: classic breadth-first search. Look at the start's edges first, then screens one step away, then two, and so on. The first time it reaches the target, that route is one of the shortest. `came_by` remembers which edge first reached each screen. Edges back to the start, or to a screen already reached, are skipped. Returns the edges in order, `[]` when start is the target, or `None`. |
| 93-100 | `walk_back(...)`: from the target, follow `came_by` back to the start, putting each edge at the front, to get the route in forward order. |
| 103-104 | `Navigator`: answers route questions over one profile. |
| 106-111 | Setup: keep the user-only patterns and all edges; **`self.edges` is all edges minus any that touch a user-only tag**; forms as a dictionary by screen; the features. |
| 113-115 | `is_user_only(tag)`: does the tag fit any user-only pattern? |
| 117-119 | `crosses_user_only(edge)`: does any tap of the edge fit? |
| 121-123 | `key_is_allowed(edge, key)`: if this key replaced the `*`, would it spell a user-only tag? Stops a crafted key such as `submit`. |
| 125-127 | `feature_for(screen)`: the first feature that ends on this screen, or `None`. |
| 129-130 | `route(start, target)`: the route used for real. |
| 131-133 | First try only edges that need no selection (simpler for the user). |
| 134-135 | If none, try all allowed edges. |
| 136-137 | Found: return it. |
| 138-139 | Not found with allowed edges, but found when user-only edges are included: `CrossesUserOnly`. |
| 140-141 | Already at Home and still nothing: `NoRoute`. |
| 142 | Otherwise press Back to Home and plan from there. |
| 144-148 | `fields_of(screen)`: the screen's fields that are required and not secret. A secret field is never a requirement. |
| 150-154 | `requirement(screen, kind, item)`: turns one profile entry into a `Requirement`. |
| 156-160 | `plan(current, target, via=None)`: route to `via` first if given, then on to the target; then cut it into legs. |
| 162-165 | `plan_feature(current, feature)`: plans through the feature's own form, so a page shared by several services is reached the right way. |
| 167-168 | `legs(current, target, route)`: cuts the route at every screen that has fields. Screens without fields are passed inside one `navigate`. |
| 170-172 | If the route does not start with Back, and the current screen itself has fields, the first leg is "fill here". |
| 173-181 | Walk the route. Collect each edge's selections. When an edge lands on a screen with fields, close a leg there (its selections + its fields) and start collecting again. |
| 182-184 | Make sure the last leg ends on the target, carrying any selections still unused. |

### 5.9 `lookups.py` — turn "رضا" into a card number

The one module between the assistant and business data. **In production a real adapter with the
same three methods replaces it** (see [the handover](app-team-handover.md#deliverable-4-read-only-lookup-apis)).

**Called by:** `task_runner.py`, `demo_values.py`, the profile check. **Input:** a lookup name such
as `contacts.card` and the user's words. **Output:** the value to send to the app. Read-only.

| Lines | What it does |
|---|---|
| 1-6 | Docstring: reads `mock_services/fixtures/` and never writes; standard library only. |
| 8-10 | Imports. |
| 12 | `FIXTURES`: the path of `mock_services/fixtures/`. |
| 15-22 | `Source`: where one lookup's records are: the `file`, the list inside it (`rows`), the field that is the record's `key`, and the field shown to the user (`label`). |
| 25-46 | `SOURCES`: twenty lookups. For example `contacts` reads `contacts.json`, list `people`, key `id`, label `name`. `loan_groups` and `loans` share a file. |
| 49-50 | `NotFound`: nothing matches what the user said. |
| 53-54 | `Ambiguous`: several match, so the user must choose. |
| 57-63 | `Candidate`: one record: its `key`, the `value` to send to the app, and the `label` to show. |
| 66-68 | `source_of("contacts.card")` returns `contacts`. |
| 71-73 | `is_known(lookup)`: can this adapter serve it? Used by the profile check. |
| 76-80 | `FixtureLookups`: the adapter. The folder can be changed for tests. |
| 82-86 | `records(lookup)`: reads the file and returns its list of records. |
| 88-93 | `candidates(lookup)`: every record as a `Candidate`. The part after the dot picks what is sent: `contacts.card` sends the card number; plain `contacts` sends the ID. Records without that attribute are skipped. |
| 95-96 | `resolve(lookup, text)`: **the main method**. |
| 97-99 | For contacts, a typed number is sent as is: the user may send to someone not saved. |
| 100-104 | Find matches. None → `NotFound`. More than one → `Ambiguous`, listing their labels. |
| 105 | Exactly one: return its value. |
| 107-111 | `matching(lookup, text)`: first, records whose key, value or label equals the text exactly. If none, records whose label contains the text. |

### 5.10 `task_runner.py` — carry the plan out, checking every step

The brain, part 2. Scripted, no LLM. It sends only `navigate`, `fill_form` and `show_review`, and
fills only what the plan lists. Every way it stops is listed in
[command-lifecycle.md](bridge/command-lifecycle.md#every-way-it-can-stop).

**Called by:** `graph_tasks.py`; tests use a fake bridge. **Input:** a `Plan`, the user's values as
text (ref → text), and whether to finish on the review. **Output:** a `TaskReport` that ends as
`review`, `arrived` or `stopped` with a Persian reason.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: resolves every value before the app moves; checks each page; never confirms. |
| 9-14 | Imports, including `show` from `steps.py`, which prints a step when `ASSISTANT_TRACE=1`. |
| 16 | `PERSIAN_DIGITS`: a table that turns `۲۵۰۰۰۰` into `250000`. |
| 17 | `NO_REVIEW`: the `needs` code the app sends when a screen has no review page. |
| 20-25 | `Bridge`: anything with `send(operation, arguments) -> result`. The real one is `BridgeLink`; tests pass a fake. |
| 28-29 | `TaskStopped`: the run cannot go on; the message says why. |
| 32-42 | `TaskReport`: `state` (`running`, `review`, `arrived`, `stopped`; `idle` before any run), `reason`, and `steps` (each command and what came back). `as_json` for the page. |
| 45-47 | `plain_digits(text)`: only the digits, in Latin form. `"۲۵۰,۰۰۰"` → `"250000"`. |
| 50-55 | `money_value(text)`: a whole number of rials as `{"value": 250000, "unit": "IRR"}`. Anything else (a decimal point, letters) stops the task. This is rule 6 (exact integers) in code. |
| 58-68 | `shows(requirement, sent, shown)`: does the screen agree with what was sent? No value shown is fine only for a picker or choice. Money: compare digits. A masked number (`****0091`): compare the last four digits. Text: must be equal. Anything else: must not be empty. |
| 71-77 | `TaskRunner`: keeps the bridge, the navigator and the lookups. |
| 79-81 | `missing(plan, values)`: the requirements with no answer yet, so all can be asked at once. |
| 83-84 | `prepared(plan, values)`: turns every answer into what the app takes. **Runs before the app moves**, so a bad value moves nothing. |
| 85-88 | A value the plan did not ask for is refused (stops anyone smuggling in, say, a PIN field). |
| 89-91 | A missing value stops the run. |
| 92 | Prepares each one. |
| 94-105 | `prepared_value(item, text)`: money → `money_value`; no lookup → the trimmed text; a lookup → `lookups.resolve`; a miss or a tie stops the run with the field's description. After a lookup resolves, line 104 prints the step "resolve value": the field, the lookup, what the user said and what will be sent (a card number masked). |
| 107-108 | `run(plan, values, review, report)`: the main loop. The caller can read `report` while this runs. |
| 109-110 | Prints the step "plan": the target, the route as `from → to (action)` lines, and the refs it needs. |
| 112 | Prepare everything first. |
| 113-116 | For each leg: move there; if it has fields, fill them. |
| 117 | At the end: open the review if this is a feature with a form; otherwise the state is `arrived`. |
| 118-120 | Any `TaskStopped`: the state becomes `stopped` with the reason. |
| 121-122 | Prints the step "run ended" with the state and the reason, if any, and returns the report. |
| 124-144 | `command(operation, arguments, report)`: prints "send …" with the arguments, sends one command through the bridge, prints "answer to …" (outcome, screen, fields, and `needs` and `error_code` when present), and appends a step to the report (operation, arguments, outcome, screen, fields, needs, error, trace). |
| 146-155 | `move(leg, sent, report)`: builds `navigate {"destination": ..., "arguments": {selection: key}}`, checks the keys first, sends it, and stops unless the outcome is `succeeded` **and** the screen is the leg's screen. |
| 157-162 | `check_keys(leg, sent)`: refuses a chosen key that would spell a user-only tag, before the app ever sees it. |
| 164-175 | `fill(leg, sent, report)`: sends `fill_form {"form": screen, "values": {...}}`; stops unless it succeeded on the same screen; then reads back each field from the observation and stops if any does not show what was sent. When all agree, prints "fields checked" with the fields as shown on screen. |
| 177-184 | `show_review(report)`: sends `show_review`. Succeeded → `review`. The app says there is no review here → `arrived`. Anything else stops. **It never sends a confirm: there is no such operation.** |
| 187-189 | `why(result)`: the app's own reason, from `needs`, `error_code` or the outcome. |

### 5.11 `bridge_link.py` — send one command and wait for its answer

**Called by:** `task_runner.py` (through `graph_tasks.py`). **Input:** an operation and its
arguments. **Output:** the app's result. It waits because the app pulls commands; the backend never
pushes.

| Lines | What it does |
|---|---|
| 1-6 | Docstring: it only waits; its one decision is to resend when the screen moved before delivery. |
| 8-11 | Imports. `DEBUGGING` comes from the queue; `TaskStopped` comes from the runner, so a failure here stops the run cleanly. |
| 13 | `RESULT_TIMEOUT_SECONDS`: give up after 40 s with no result, or after one hour when the backend was started with `ASSISTANT_DEBUG=1`, so a paused breakpoint does not end the wait. |
| 14 | `POLL_SECONDS = 0.1`: look for the result every tenth of a second. |
| 15 | `STALE_TRIES = 3`: try at most three times when the screen keeps moving. |
| 18-24 | `BridgeLink`: one device's end of the loop: the queue, the device, the timeout. |
| 26-32 | `send(operation, arguments)`: queue, wait; if the result is `stale_observation` (the screen moved before the app picked it up, so it never ran), queue again with the new version. Up to three times, then return the last result. |
| 34-41 | `queued(...)`: queues; turns `DeviceNotSeen` into "the app has not connected yet" and `DeviceBusy` into "another command is running", both as `TaskStopped` in Persian. |
| 43-52 | `wait_for(command_id)`: checks the device's `last_result` every 0.1 s until it is for this command; after the timeout stops with "no answer came from the app". |

### 5.12 `graph_tasks.py` — what the graph page asks of the backend

**Called by:** `graph_page.py`, and by `app.py` for the agent's navigator, lookups and current
screen. **Holds:** at most one run, on a background thread.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: everything goes through `navigator` and `task_runner`, so this file adds no way to reach a confirm; a run is refused while the chat takes a turn. |
| 9-18 | Imports. |
| 21-22 | `RunInProgress`: a run (or a chat turn) is already going. |
| 25-34 | `GraphTasks`: keeps the queue, the device, a `Navigator` over the profile, the lookups, an `idle` report, and `chat_busy`, which `app.py` sets to the chat's `busy`. |
| 36-39 | `current_screen()`: the screen in the app's last observation; Home before the app reports in. The agent plans from it too. |
| 41-47 | `plan_for(screen)`: if a feature with a form ends on this screen, plan through that form; otherwise plan straight there. Returns the plan and the feature. |
| 49-58 | `preview(screen)`: what a click shows. `{"status": "user_only"}` or `{"status": "no_route"}` when it cannot plan; otherwise `status: ok` + target + route + requirements + the feature's intent + a demo value for each requirement. |
| 60-68 | `start(screen, values)`: refuses a second run, or a run while a chat turn goes; plans; decides whether to end on a review (only for a feature with a form); makes a `TaskRunner` with a `BridgeLink`; resets the report; starts `runner.run` on a background thread so the web request returns at once. |
| 70-72 | `progress()`: the report so far, as a dictionary. |

### 5.13 `demo_values.py` — the "Fill with demo values" button

**Called by:** `graph_tasks.preview`. **Input:** a requirement, the target screen, the lookups.
**Output:** an example answer, or `""`. Holds no secret: a secret field is never a requirement.

| Lines | What it does |
|---|---|
| 1-6 | Docstring. |
| 8-9 | Imports. |
| 11 | `CONTACT = "رضا"`: the demo contact. |
| 12 | `BILL`: fields whose demo value is read from the first bill (`billId` or `payId`). |
| 14-72 | `TYPED`: a fixed example for each field ref: amounts as whole rials, Persian dates, synthetic codes, choice keys (`DAILY`, `PDF`...). The replica's own validation accepts each one. |
| 74-87 | `BY_SCREEN`: where the same ref needs a different value on a particular screen, for example `svc-amount` on `S-308`. |
| 90-91 | `demo_value(item, target, lookups)`. |
| 92-94 | First a screen-specific value (for the target or the field's own screen), then a general one. |
| 95-96 | A bill field: from the first bill in `bills.json`. |
| 97-98 | No lookup and no example: empty. |
| 99-100 | A contact: `رضا`. |
| 101 | Any other lookup: the key of its first record (such as `card-1`). |

### 5.14 `graph_data.py` — what the map draws

**Called by:** `app.py` once at start. **Input:** `app_profiles/`. **Output:** nodes with `x`, `y`
and a `built` flag, and edges with a `walked` flag.

| Lines | What it does |
|---|---|
| 1-5 | Docstring: built screens are green; everything else in the navigation graph is grey. |
| 7-9 | Imports. |
| 11 | `PROFILES`: the path of `app_profiles/`. |
| 12 | `START = "D-000"`: the demo front page, where the map starts. |
| 13-15 | Layout sizes: column width, row height, and how many grey nodes per column. |
| 18-20 | `read_json(name)`: reads one file of `app_profiles/`. |
| 23-25 | `built_screens()`: the set of screens in the replica profile. |
| 28-38 | `depths(edges)`: breadth-first from `D-000`, how many steps each screen is from the start. |
| 41-55 | `place_nodes(...)`: one column per step from the start, stacked in area then ID order; unreachable screens go in grey columns to the right, 90 per column. |
| 58-81 | `graph_payload()`: reads both graphs; adds designed replica pages that the original graph does not have; marks each node built or not; places them; edges are the replica's walked edges plus the recovered ones with a known target; a walked edge is drawn differently. |

### 5.15 `graph_page.py` — the page's web routes

**Called by:** the browser. All under `/graph`.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: a run takes only a target and values, and a chat turn only the user's words or an offered option, so neither can reach a confirm. |
| 9-18 | Imports. |
| 20-21 | Where the page files are. |
| 24-30 | `RunRequest`: a run body may have only `screen` and `values` (ref → text). |
| 33-46 | `ChatRequest`: a chat turn has exactly one of `text` or `option`, each non-empty; any other field is refused (422). |
| 49-53 | `build_graph_router(...)`: the route group, around the queue, the tasks, the chat and the graph data. |
| 55-59 | `GET /graph`: the HTML page; each load starts a new chat conversation. |
| 61-74 | `GET /graph/graph_map.js`, `/graph/graph_task.js` and `/graph/graph_chat.js`: the three scripts. |
| 76-79 | `GET /graph/data`: every node and edge, plus the device ID. |
| 81-90 | `GET /graph/state`: the last observation, the last result, whether a command is waiting, and the git revision. The page asks every 0.5 s. |
| 92-95 | `GET /graph/plan/{screen}`: the preview of a click. |
| 97-100 | `GET /graph/run`: the current run's report. |
| 102-111 | `POST /graph/run`: starts a run. 409 when a run or a chat turn is going; 404 when there is no route. Replies 202 with the first report. |
| 113-116 | `GET /graph/chat`: the conversation, the pending question and options, the task state and the runner's report. |
| 118-125 | `POST /graph/chat`: starts or resumes the agent. 409 with the reason (`turn_in_progress`, `run_in_progress`, `unknown_option`). Replies 202. |
| 127 | Returns the route group. |

### 5.16 `steps.py` — numbered prints of every step

For a person following a run while debugging ([debugging](runbook/debugging.md)). Off by default.

**Called by:** `task_runner.py` and `command_queue.py`. **Input:** a title and named values.
**Output:** printed lines when `ASSISTANT_TRACE=1`; nothing otherwise. It never changes what the
callers do.

| Lines | What it does |
|---|---|
| 1-6 | Docstring: off by default; it must never print a full card, account or IBAN number. |
| 8-11 | Imports: a counter, JSON, environment variables and regular expressions. All standard library. |
| 13 | `LONG_NUMBER`: any run of 12 or more digits. |
| 14 | `counter`: one step number for the whole process, never reset. |
| 17-19 | `masked(text)`: replaces each long number with `****` and its last 4 digits, so `0000000000000091` prints as `****0091`. |
| 22-23 | `show(title, **values)`: prints one step. |
| 24-25 | Reads `ASSISTANT_TRACE` on every call, so tests can switch it; anything but `1` prints nothing. |
| 26 | The header: `── 3 · send navigate ─────`, numbered from the counter. |
| 27-30 | One line per value, names aligned. Dicts and lists are written as JSON with Persian kept readable; a value of several lines is indented under the first. |
| 31 | Masks the whole block and prints it at once (`flush=True`, so it shows straight away). |

### 5.17 `models.py` — the two model clients

**Called by:** `intent.py` and `slots.py`, through the agent's nodes. **Input:** a prompt and a
JSON schema, or a list of texts. **Output:** a dictionary, or one vector per text. **Raises:**
`ModelError` on any failure, so the task fails instead of guessing.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: read-only HTTP clients over `httpx`; addresses from `ASSISTANT_LLM_URL`, `ASSISTANT_LLM_MODEL` and `ASSISTANT_EMBED_URL`; any failure raises `ModelError`. |
| 9-12 | Imports: JSON, environment variables and `httpx`. |
| 14-15 | A 30 s timeout, and at most 400 answer tokens (the served model's whole context is 4,000). |
| 18-19 | `ModelError`. |
| 22-27 | `Chat`: reads the model's address and name when made. Nothing is called yet, so a test or `create_app` can make one with no network. |
| 29-40 | `json(prompt, schema)`: the body: temperature 0, the prompt as one user turn, and `response_format` with the schema, strict. |
| 41-46 | Posts to `/chat/completions` and reads the answer's text as JSON. An HTTP error, a missing key or text that is not JSON becomes `ModelError`, with only the error's type name. |
| 47-49 | Refuses an answer that is not a JSON object; returns it. |
| 52-57 | `Embeddings`: reads the TEI address; the batch size is learned on first use. |
| 59-66 | `embed(texts, query=False)`: the first time, reads `/info` for `max_client_batch_size` (50 today). |
| 67-74 | Posts the texts to `/embed` in batches, with `normalize: true`. A query (the user's words) adds `prompt_name: "query"`, as RagBot does; a feature's text has none. |
| 75-79 | Any failure, or the wrong number of vectors, becomes `ModelError`; returns the vectors. |

### 5.18 `agent_state.py` — what the agent remembers, and how its task state may change

**Used by:** the nodes, `agent_chat.py`, `main_debug.py` and the tests.

| Lines | What it does |
|---|---|
| 1-6 | Docstring: every change of `task_state` goes through `moved()`. |
| 8-10 | Imports: JSON, paths and `TypedDict`. |
| 12-16 | `TRANSITIONS`: `contracts/transitions/task.json`, read once. |
| 19-33 | `AgentState`: the twelve keys of one request, each with a comment: conversation, messages, offered and chosen intents, plan, values, missing, pending note and options, task state, report, reply. |
| 36-37 | `BadTransition`. |
| 40-55 | `new_request(conversation_id, text)`: the state a new message starts from: the one message, `task_state` `created`, everything else empty, so nothing of an earlier request carries over. |
| 58-65 | `moved(state, *path)`: walks `path` from the current state. Staying put is not a change; any other step must be in the table, or it raises `BadTransition`. Returns `{"task_state": last step}`. A feature without a form passes two steps, `ready_for_user` then `completed`. |

### 5.19 `intent.py` — which feature the sentence means

**Called by:** `Nodes.understand`. **Input:** the user's words. **Output:** the 8 closest features,
then one intent, `knowledge_question` or `not_sure`.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: what the model sees and never sees. |
| 9 | Imports the two clients' types. |
| 11-13 | `CLOSEST = 8`, and the two fixed answers. |
| 16-18 | `feature_text(feature)`: what is embedded: `title — description`. |
| 21-27 | `FeatureIndex`: the features, the embeddings client, and no vectors yet. |
| 29-37 | `closest(text)`: embeds every feature the first time only, embeds the text as a query, scores each feature by dot product (the vectors have unit length, so it is the cosine), sorts highest first (ties keep profile order) and keeps 8. |
| 40-54 | `choice_prompt(text, offered)`: the instructions, one line per offered feature (`intent: «title» — description Values: slot refs`), the two fixed answers, and the user's words. Nothing else. |
| 57-67 | `choose(chat, text, offered)`: the schema's `choice` is an enum of exactly the 8 intents and the 2 fixed answers; asks the model; anything else becomes `not_sure`. |

### 5.20 `slots.py` — the values, and their check

**Called by:** `Nodes.understand`, `Nodes.extract` and `Nodes.check`. **Input:** the user's
messages and the offered requirements. **Output:** the model's values, then only the good ones.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: the model sees only the offered requirements and the words, with long numbers hidden; code keeps nothing the plan does not ask for. |
| 9-12 | Imports, including `money_value` from `task_runner.py`, so money is made the same way everywhere. |
| 14-15 | `LONG_NUMBER` (12 or more digits, Persian or Latin) and the `[number N]` placeholder. |
| 16 | `UNITS`: `IRR` is 1, `IRT` (toman) is 10. |
| 17-22 | `MONEY`: the schema of an amount: a whole number and a unit. |
| 25-27 | `numbers(messages)`: every long number in the messages, in order, in Latin digits. |
| 30-38 | `hidden(messages)`: the messages with each long number replaced by `[number 1]`, `[number 2]`..., counting across all of them. |
| 41-57 | `extract_prompt(messages, wanted)`: one line per requirement (ref, type, Persian ask and description), how money is written (`هزار` is 1000, toman is `IRT`, no unit means `IRR`), and the messages with their numbers hidden. |
| 60-70 | `extract(chat, messages, requirements)`: the schema allows exactly the offered refs, each a string (or an amount) or `null`, all required; asks the model; drops the nulls. |
| 73-80 | `money(value)`: only a dict with a real integer (not `True`) and a known unit; multiplies toman by 10 and builds the contract's `{"value": int, "unit": "IRR"}` with `money_value`, which refuses a negative. Anything else is `None`. |
| 83-92 | `text(value, messages)`: a non-empty text, trimmed; `[number N]` is turned back into the user's number. |
| 95-105 | `checked(values, requirements, messages)`: keeps only refs the plan asks for, each through `money` or `text`; drops the rest. |

### 5.21 `replies.py` — what the agent says in Persian

**Called by:** the nodes. Plain text building; every value and label is masked.

| Lines | What it does |
|---|---|
| 1-6 | Docstring. |
| 8-9 | Imports: the lookups' types and `masked`. |
| 11-21 | The fixed sentences: the knowledge message, "which one?", the failure messages, the review and page messages, and the notes for a name not found or found twice. |
| 22 | `FEW, MANY = 2, 8`: a lookup with this many records offers them as options (owner, 9 Nov 2026). |
| 25-28 | `option(ref, item)`: one record as a choice. Its ref is `field=key`, so the answer says which field and which record; its label is the record's label, plus its value when that differs (`رضا ****0091`), masked. |
| 31-39 | `lookup_options(lookups, wanted)`: each missing ref field's records, when its lookup has 2 to 8. |
| 42-44 | `question(note, asks)`: one message: the note, if any, then every question. |
| 47-55 | `shown(value, item, lookups)`: an amount as `2,500,000 ریال`; a record by its option label; anything else masked. |
| 58-68 | `ready(report, requirements, lookups)`: every value the runner sent (form values and navigate arguments), one line each by its Persian description, after "the review is open" or, when the app had no review, "the form is filled". |

### 5.22 `agent_nodes.py` — the agent's nodes

**Used by:** `agent_graph.build_graph`. Each node is one method that takes the state and returns
only the keys it changes.

| Lines | What it does |
|---|---|
| 1-6 | Docstring: the model only returns JSON; code acts; the only commands are the task runner's. |
| 8-21 | Imports. |
| 23 | `MODEL_SEES`: the four keys of a requirement the model may see. |
| 26-35 | `Tools`: everything the nodes use: the chat model, the feature index, the navigator, the lookups, the bridge, and a function that says where the app is. Only the bridge and that function differ between the server and `main_debug.py`. |
| 38-42 | `plan_for(navigator, start, feature)`: through the feature's form when it has one, as `graph_tasks.plan_for` does. |
| 45-50 | `Nodes`: the tools, and the features by intent. |
| 52-55 | `failed(state, reply)`: ends the task as `failed` with a masked Persian reason. |
| 57-68 | `understand`: hides long numbers, narrows to 8, lets the model choose. A model failure fails the task. A feature moves the task to `resolving`; a knowledge question or "not sure" leaves it `created` (no task, plan §13). |
| 70-83 | `plan`: plans from the app's current screen; no allowed route fails the task. Keeps the plan as JSON, with its start screen and intent, and lists every requirement as missing. |
| 85-100 | `extract`: offers the model only the missing requirements, with only `MODEL_SEES`. Nothing missing: no model call. A value already held wins over the model's. |
| 102-108 | `check`: code only: `slots.checked`, then what is still missing. |
| 110-121 | `clarify`: one message with every missing question; the options are `resolve`'s, or the lookups' records. `interrupt()` pauses here; on resume this node runs again from its first line and gets the answer. An option's ref sets that field's key; text is added to the messages for `extract`. |
| 123-145 | `resolve`: code only: each value with a lookup must match one record (a typed number to a contact needs none), and is replaced by that record's key. A tie or a miss removes the value and goes back to `clarify` with a note and, for a tie, the matches as options. |
| 147-155 | `prepare`: plans again from the same start, turns each amount into the text the runner reads, and runs `TaskRunner.run` over the bridge, ending at the review when the feature has a form. |
| 157-168 | `verify`: code only: a stopped run fails with the runner's reason; the last observed screen must be the feature's `review` (or destination), else it fails. |
| 170-177 | `ready_for_user`: a feature without a form: "page open", and `completed`. Otherwise: the review message with every value. |
| 179-188 | `reply`: the knowledge message; or "which one?" with the 8 offered features by title, paused with `interrupt()`. An offered intent goes on to `plan`; text goes back to `understand`. |

### 5.23 `agent_graph.py` — the one graph

**Called by:** `app.py` and `main_debug.py`. **Output:** the compiled graph, with its checkpointer.

| Lines | What it does |
|---|---|
| 1-6 | Docstring: no edge leads past the review. |
| 8-14 | Imports. |
| 17-21 | `after_understand`: end on a failure; `reply` for a knowledge question or "not sure"; else `plan`. |
| 24-28 | `after_reply`: new words go back to `understand`; a knowledge reply ends; a picked feature goes to `plan`. |
| 31-43 | `after_plan`, `after_extract`, `after_verify`: on to the next node, unless the task failed. |
| 46-48 | `until_complete`: back to `clarify` while something is missing. Used after `check` and after `resolve`. |
| 51-64 | `build_graph(tools)`: one `StateGraph` over `AgentState`, one node per method. |
| 66-76 | The edges, as in [the agent](bridge/agent.md#the-graph). |
| 77 | Compiles with LangGraph's `InMemorySaver`: each conversation is kept by its `thread_id` while a question waits. |

### 5.24 `agent_chat.py` — the graph page's chat box

**Called by:** `graph_page.py`. **Holds:** one conversation per page load, and at most one turn,
on a background thread.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: it and a graph-page Run refuse each other. |
| 9-17 | Imports. |
| 19 | The Persian line shown if a turn crashes. |
| 22-23 | `TurnRefused`: its message is the reason code the route returns with 409. |
| 26-33 | `AgentChat`: the compiled graph, the page's `GraphTasks`, no turn yet, a new conversation. |
| 35-43 | `new_conversation()`: a fresh conversation ID and an empty transcript, unless a turn is running. `GET /graph` calls it. |
| 45-47 | `busy()`: true while a turn's thread lives. `graph_tasks` asks it before a Run. |
| 49-62 | `start(text, option)`: refuses during a turn or a Run; an option must be one the agent offered, and is shown by its label; starts the turn thread. |
| 64-82 | `turn(answer)`: resumes the paused question with `Command(resume=...)`, or starts a new request. Reads the pending question from `__interrupt__`, the task state and the report, and adds the agent's line. A crash is printed and shown as one Persian line. |
| 84-93 | `progress()`: what `GET /graph/chat` returns. |

## 6. The graph page in the browser

Not Python, but it is how you drive everything today. Files in `assistant/static/`. How to use it:
[try-it.md](bridge/try-it.md).

| File | Function | What it does |
|---|---|---|
| `graph.html` | (layout) | The SVG map on the left, the panel on the right: the chat box, Now, Clicked, Route, questions, two buttons. No internet. |
| `graph_map.js` | `start` | Reads `/graph/data`, draws every edge and node, turns on pan and zoom and search, and calls `refresh` every 0.5 s. |
| | `drawNode`, `drawEdge`, `make`, `applyView` | Draw the boxes and lines, and move or zoom the view. |
| | `refresh` | Reads `/graph/state`, paints the current screen amber, shows the last result and the backend revision. |
| | `find`, `markRoute`, `describe` | Search highlight, route highlight, last-result text. |
| `graph_task.js` | `clicked` | A grey node: "not built". A green node: reads `/graph/plan/{id}`, shows the route; with no questions it moves straight there (`moveTo`), otherwise it shows the questions. |
| | `moveTo` | `POST /test-harness/v1/commands` with the destination. |
| | `fillDemo` | Puts the plan's demo values into the inputs. |
| | `run`, `follow`, `showProgress` | `POST /graph/run` with the answers, then reads `/graph/run` every 0.4 s and shows each step until it ends. |
| `graph_chat.js` | `sendText`, `sendTurn` | `POST /graph/chat` with the typed words (Send or Enter), or with an option's ref when its button is pressed. |
| | `followChat`, `showChat` | Reads `/graph/chat` every 0.5 s while a turn runs; shows the messages, one button per option, the task state, and the runner's steps in the side panel with `graph_task.js`'s `stepBlock`. |
| | `chatLine`, `optionButton` | One message (right to left), and one option button. Text only, never HTML. |

## 7. The Kotlin bridge inside the replica

Folder: `android/app/src/main/java/org/ragbot/replica/bridge/`. Same reading order as
[docs/bridge/README.md](bridge/README.md#read-the-code-in-this-order). Safety walls:
[safety.md](bridge/safety.md). **This is the part the app team rebuilds in their own app**, so the
files they replace are marked.

| File | Function | Input → Output, and what it does |
|---|---|---|
| `Contract.kt` | data classes | The command, observation and result of `contracts/schemas/` as Kotlin data. `SUPPORTED_OPERATIONS` is the four. `FieldValue` is text or money (whole rials). `Ending` is how an operation ended before the observation is added; `succeeded`, `blocked`, `failed` build one. No logic. |
| `ContractJson.kt` | `commandIn(reply)` | Backend reply text → a `Command`, or null for the idle status. Uses the app's JSON reader, which accepts only whole numbers, so a fractional amount cannot get in. |
| | `fieldValue` | `{"value": 250000, "unit": "IRR"}` → `Money`; anything else → `Text`. Refuses any unit but IRR. |
| | `write(observation)`, `write(result)` | Kotlin data → JSON text, with optional fields only when set. `quote` escapes strings. |
| `BridgeConfig.kt` | `enabled(context)` | True only in a debug build **and** when the file `bridge.enabled` exists in the app's files folder (`adb shell touch ...`). `DEVICE_ID = "replica-dev"`. |
| `BridgeState.kt` | object | Three values the app writes after each screen draw: current `screen`, Home `homeTab`, and the `activity`. Read by `ComposeControls`. |
| `BackendClient.kt` | `resume(observation)` | POSTs to `/assistant/v1/sessions/replica-dev/resume` → the next command or null. |
| | `postResult(result)` | Logs `result <id>: <outcome> on <screen>` with the trace, `needs` and `error_code`, then POSTs to `/assistant/v1/results` → the next command or null. |
| | `logged(command)` | Logs `command <id>: <operation> <destination or form>` for a command the backend handed over (tag `RagBotBridge`, read with `adb logcat -s RagBotBridge`). Never a field's value. Only here, because `Executor` and `CommandPoller` also run in JVM unit tests, where `android.util.Log` throws. |
| | `post(path, body)` | One HTTP POST to `BuildConfig.BRIDGE_BACKEND` (a local development address set at build time). It waits 5 s to connect and up to an hour for the answer, so a backend paused at a breakpoint still answers it on Continue ([debugging](runbook/debugging.md)); any non-200 is an error. No credentials. |
| `CommandPoller.kt` | `start()` | Once per process (Android 10+): makes `Executor(ComposeControls())` and `BackendClient`, starts a background thread. |
| | `loop()` | Forever: `pollOnce`, ignore "backend not running", sleep 0.5 s. |
| | `pollOnce()` | Observe → resume; while a command comes back: execute it, post the result. One thread, so commands never overlap. |
| `Executor.kt` | `execute(command)` | Command → result. If this command ID ran before, returns the first result (journal of 50). Otherwise checks `rejection`, runs, adds the fresh observation. |
| | `rejection` | Before touching the screen: `policyProblem`, expiry (`rejected_expired`), screen version (`rejected_stale` / `stale_observation`), draft version (`stale_draft`). |
| | `policyProblem` | Wrong device, protocol or profile version; `navigate` to an unknown screen or with an unknown or unsafe argument; `fill_form` with a field that is not fillable; any other operation → `rejected_policy`. |
| | `run` | Dispatches `navigate` → `Navigator`, `fill_form` → `FormFiller.fill`, `show_review` → `FormFiller.showReview`, `observe` → nothing. A crash in the middle becomes `uncertain`. |
| | `report` | Adds the observation, draft version and, for `navigate`, the trace. |
| | `expired` | Reads `expires_at` as UTC; unreadable counts as expired. |
| `Policy.kt` | `mayTap(tag, screen)` | Rule 4 in code. Allowed only if an exported edge or a non-secret field of this screen names the tag, and never if it is user-only. |
| | `mayType(ref, screen)` | Allowed only for a fillable field. |
| | `fillable(form, ref)` | The field, or null if unknown, secret or user-only. |
| | `isUserOnly`, `isSafeKey`, `matches` | Pattern match against the user-only list; a row key must be one plain word; `*` matching. |
| `Actions.kt` | `tap(tag)` | The one place the bridge taps. Asks `Policy`, waits up to 3 s for the control, refuses a disabled or non-clickable one, then clicks. Null means done; otherwise an `Ending` such as `blocked("control_disabled:x")`. |
| | `type(ref, text)` | The one place it types. Asks `Policy`, refuses a password field, sets the text, waits until the field shows it (digits only for numbers). |
| | `back`, `state`, `find`, `waitUntil`, `errorsAmong` | Back button, current state, find a control, wait for a condition, which fields show an error. |
| `Routes.kt` | `find(from, to, arguments)` | Breadth-first search over the exported edges (the same idea as Python's). **Replica only: an app team with direct screen opening does not need this.** |
| | `applied(edge, arguments)` | Puts the chosen row key in place of `*`. |
| `Navigator.kt` | `navigate(destination, arguments)` | Up to 40 steps: stop at a dialog (`close_dialog`); arrived → `succeeded`; otherwise take the next edge, or press Back. Reports a `trace`. Stops with `no_executor_route`, `missing_argument`, `unexpected_screen`, `valid_form` or the fields in error. |
| | `walk(edge, ...)` | Taps each tag of the edge and waits for the new screen. |
| `FormFiller.kt` | `fill(form, values)` | Must already be on that form, else `blocked("navigate")`. Each value: a text field is typed; anything else is a choice, opened by its picker and tapped by `ref-key`. |
| | `showReview()` | Already on a review page → done. Otherwise take the one exported edge to a review page; none → `blocked("no_review_here")`. **Never taps the confirm.** |
| | `Reviews.pages` | The pages where the user's confirm or consent waits. |
| `Observer.kt` | `observe()` | Builds the observation: screen, dialog, loading, the form's non-secret fields **masked** (card and IBAN to `****1234`), draft. The version rises only when something visible changed. |
| | `filled()`, `reviewed()`, `trackUserChanges` | Draft bookkeeping: the first fill starts a draft; each fill or a user's own edit raises its version, so an old command goes stale. Going Home clears it. |
| `Controls.kt` | interface | **What an app team re-implements.** `state`, `find(tag)`, `withPrefix`, `click`, `setText`, `back`. `Control` is one control's tag, text and flags. |
| `ComposeControls.kt` | the real `Controls` | **What an app team replaces with bindings to their screens.** Reads the Compose semantics tree of the top window, on the main thread. `click` runs the control's own `onClick`; `setText` runs its own `onValueChange`, so the screen validates as for a user. Never reads a password field. |

## 8. The replica app around the bridge

`android/app/src/main/java/org/ragbot/replica/` has about 80 Kotlin files. You only need to know
these to follow the bridge. Development runbook: [replica-development.md](runbook/replica-development.md).
What was built: [replica-complete.md](reports/replica-complete.md).

| File | What it does |
|---|---|
| `MainActivity.kt` | The app. Lines ~56-57 hand the activity to `BridgeState` and start `CommandPoller` when enabled. Around line 382 it publishes the current page to `BridgeState` after each draw. |
| `HomeNavigation.kt` | `HomeDestinations`: `HOME = "S-026"`, `FRONT = "D-000"`, and `implemented`, the set of every built screen. `Executor` refuses a `navigate` to anything else. |
| `ReplicaGraph.kt` | Every screen move. Each `Edge` has `from`, `to`, `action` (tags joined by ` > `) and a kind: `REAL` and `DEMO` are exported; `USER_ONLY` needs the user; `STEP` is covered by others. |
| `ReplicaProfile.kt` | The shape of the profile (`FormField`, `ProfileForm`, `ProfileFeature`) and the object the app reads it through. |
| `ProfileForms.kt`, `ProfileWords.kt`, `ProfileSelections.kt`, `ProfileFeatures.kt`, `ProfileUserOnly.kt` | The forms and their fields, their Persian questions, row choices, the 77 features, and the user-only list. Exported to `replica.profile.json`. |
| `MockBank.kt` | The in-app mock bank: every service screen calls it; it checks against the fixtures and returns a receipt or a Persian failure. In memory; resets on restart. Only the user's own confirm reaches it. |
| `MockData.kt`, `MockJson.kt` | Read the fixtures packed into the app from `mock_services/fixtures/` at build time. |
| `*Screens.kt`, `*Fixture.kt` | The screens of each feature area, and their sample data. |

## 9. Tools: the check runner and the helper scripts

### 9.1 `tools/project_checks.py` — the check runner (CI runs `all`)

Standard library only (except the `contracts`, `profile` and `mock-data` suites, which need
`jsonschema`). Each check adds a PASS or FAIL line; anything that cannot run here is `N/A`, never
a fake pass (rule 7). Usage in its docstring, lines 1-17.

| Lines | Block | What it does |
|---|---|---|
| 19-32 | imports | Standard library. |
| 34 | `RAGBOT_BASELINE_SHA` | The pinned RagBot commit. |
| 36-53 | constants | The issue template fields, CI file, plan, manifest, overview, decision index; lines `.gitignore` must have. |
| 55-88 | forbidden things | File types never to commit (APK, dex, keys), folder names (`decompiled`, `evidence`...), dump suffixes, and secret patterns (private keys, AWS, GitHub, Google, Slack tokens). |
| 90-154 | contract and isolation constants | Schema and OpenAPI folders, the five OpenAPI files, the words that mean a final action, the agreed four operations, profile paths, evidence labels, mock files, our own database, schema, role, collection and bucket names, RagBot's names, development hosts. |
| 156-197 | APK and toolchain pins | The APK record, and the expected versions and SHA-256 hashes of Node.js, JDK, Ghidra, REA, jadx and apktool. |
| 199-208 | `Report` | Collects passes, failures and N/A; `check(ok, name, detail)` adds one. |
| 211-236 | `repo_files` | The files git would commit (tracked + untracked, not ignored); without git, a folder walk. |
| 239-253 | `looks_like_android_package` | True for dex bytes, or a zip holding `AndroidManifest.xml` or `classes.dex`. |
| 256-282 | `scan_forbidden` | Fails on any forbidden folder, file type, APK content or secret pattern. |
| 285-287 | `read` | A file's text, or `None`. |
| 290-330 | `check_foundation` | `AGENTS.md` exists and `CLAUDE.md` is an exact copy; it names the RagBot commit; templates exist; CI runs `all`; `.gitignore` blocks private material; then `scan_forbidden`. |
| 333-359 | `check_baseline` | The plan, overview, decision index and source manifest exist; the manifest pins RagBot and defines the labels; no database dumps under `docs/`. |
| 362-376 | `load_json`, `file_refs` | Read JSON; list every `$ref` to another file. |
| 379-471 | `check_contracts` | Integers must be real integers (1500.0 is refused). Every schema is valid. Every valid example passes and every invalid example fails for exactly its stated reason; each schema has both. The command operations are exactly the four (the message still says "five", a leftover wording from before D-023). Each OpenAPI file is 3.1.0, its references exist, and **only `user_only_app.json` has a submit or confirm path**. Every profile matches its schema and has no user-only edge. |
| 474-482 | `guard_target` | Raises unless a name is one of our own resources and not RagBot's. Meant for reset and migration code. |
| 485-488 | `is_development_host` | True for `localhost` or `127.0.0.1`. |
| 491-530 | `check_isolation` | `.env.example`: MinIO off; every database, schema, role, collection and bucket is ours; every host is local; every secret is `change-me`. `deployment/init_project_db.sql` creates exactly our names and connects only to our database. |
| 533-591 | `check_navgraph` | `navigation-graph.json` is valid JSON with nodes and edges, every node has id, name and label, IDs are unique, labels are known, no edge points to an unknown node, and an edge with no target is marked dynamic. |
| 594-654 | `check_api_structure` | The same kind of checks for `api-structure.json`: endpoints and models, unique IDs, known labels, every model reference exists. |
| 657-667 | `check_requirements` | Every required field has a question; no secret field is required or asked; every row edge (with `*`), and only a row edge, lists what it needs chosen. |
| 670-698 | `check_routes` | Imports our own `lookups` and `navigator`. Every `ref` names a known lookup; every feature has a route from Home; no feature's route touches a user-only action. |
| 701-716 | `check_titles_and_reviews` | Every feature has a Persian title; a feature with a form has a `review` the assistant can reach from its form without a user-only step, and a feature without one has none. |
| 719-779 | `check_profile` | The replica profile matches the schema; its screens are in the graph; edges join profile screens; no edge uses a user-only action; forms are profile screens; every feature has a valid target and fillable, non-secret slots; then the three checks above; and the committed profile equals a fresh export if one was built (otherwise N/A). |
| 782-788 | `valid_card`, `valid_iban` | The Luhn check for card numbers; the mod-97 check for IBANs. |
| 791-819 | `check_mock_data` | Every mock file exists and matches its schema; every card and IBAN passes its checksum; every cheque book names a mock account. |
| 822-839 | `check_apk_baseline` | The APK record lists sizes and hashes; if `local/evidence/` exists, the local APK files match; otherwise N/A. |
| 842-856 | `check_rea_environment` | If `local/toolchain/` exists, each tool has its pinned version and hash; otherwise N/A. |
| 859-884 | `SUITES` | Names each suite and what it runs; `all` runs every one. |
| 887-891 | `run` | Runs one suite into a report. |
| 894-917 | `main` | Reads the suite name and `--root`, prints PASS / FAIL / N/A and a total, and exits 1 if anything failed. |

### 9.2 `tools/bridge_e2e.py` — the journey on the real emulator

Run by hand, with the backend and the app's loop running. See
[try-it.md](bridge/try-it.md#check-it-from-the-command-line).

| Lines | What it does |
|---|---|
| 1-7 | Docstring: no arguments → the card-to-رضا journey; `--features` → every feature with demo values. It cannot confirm. |
| 9-16 | Standard library imports. |
| 18-23 | The backend address, device ID, 180 s run timeout, the profile path, the `adb` prefix for the emulator, and the Persian-digit table. |
| 26-29 | `get(path)`: reads one JSON route. |
| 32-37 | `post(path, body)`: posts JSON and reads the reply. |
| 40-49 | `run(screen, values)`: like the Run button; polls `/graph/run` every 0.3 s until it is not `running`. |
| 52-59 | `restart_app()`: force-stops and starts the app, so each feature starts from the front page. |
| 62-71 | `go_home()`: queues a move to `S-026` and waits until the app is there. |
| 74-80 | `screen_text()`: dumps the screen with `uiautomator` and returns all its text, Latin digits, no separators. |
| 83-101 | `card_to_reza()`: goes Home; takes the plan's demo values with `رضا` and `250000`; runs; fails unless the state is `review` on `S-176`; waits 3 s and fails if the app left the review; fails unless the screen shows the source card, رضا's card and the amount; prints PASS and each step. |
| 104-112 | `refused_when_not_built()`: a move to `S-003` must be refused with 404. |
| 115-129 | `every_feature()`: for each feature, restart the app, plan, run with demo values, and print how it ended. |
| 132-140 | `main()`: fails if the app never reported in; runs `--features` or the two checks. |
| 143-144 | Runs `main` when started directly. |

### 9.3 `android/tools/check_secret_state.py` — secrets do not survive a kill

Proves that `S-119` and `S-120` lose CVV2 and the second password when Android kills the app,
while the card expiry is restored. Run from the host with the debug app installed.

| Lines | What it does |
|---|---|
| 1-5 | Docstring. |
| 7-11 | Imports. |
| 13-15 | The emulator, the app package, and a sample expiry. |
| 18-19 | `shell(...)`: runs `adb shell ...` and returns its output. |
| 22-29 | `nodes()`: dumps the screen as XML (up to 5 tries) and returns every node. |
| 32-33 | `label(node)`: its text or description. |
| 36-39 | `tap_node(node)`: taps the middle of its bounds. |
| 42-51 | `tap(text)`: finds a node with this text, scrolling down then up, and taps it. |
| 54-55 | `fields()`: every text input. |
| 58-63 | `type_into(node, text)`: tap, type, close the keyboard. |
| 66-68 | `launch()`: starts the app. |
| 71-93 | `check(screen)`: types into the two password fields and the expiry, goes Home, kills the process, confirms it is dead, relaunches; fails if a password field came back filled, or if the expiry did not. |
| 96-108 | `main()`: opens `S-120` from the front page and checks it; then opens `S-119` through Home → card → gift card and checks it. |
| 111-112 | Runs `main`. |

### 9.4 `android/tools/verified_maven.py` — build-only download cache

The development server cannot reach Google's Maven directly, so this small local server feeds
Gradle with Android libraries, checking each download. It is never part of the app. See
[replica-development.md](runbook/replica-development.md).

| Lines | What it does |
|---|---|
| 1-5 | Docstring. |
| 7-16 | Imports. |
| 18-24 | The cache folder under `local/scratch/`, locks, and three mirrors: Maven Central, a Google mirror, and an independent mirror used only to cross-check. |
| 27-31 | `fetch(url)`: downloads with the system certificate store. |
| 34-36 | `Handler`: the HTTP handler; `log_message` silences logging. |
| 38-44 | `do_HEAD`, `do_GET`: one request per file at a time. |
| 46-51 | `respond`: refuses `..` or `?` in the path. |
| 52-60 | Not cached yet: try Maven Central and compare against its `.sha1`. |
| 61-73 | Not on Central (404): take it from the Google mirror, and require that the independent mirror's `.sha1`, size and SHA-256 all agree. |
| 74-89 | Write it atomically (`.pending` then rename) and append a line to `maven-provenance.jsonl`: path, origin, size, SHA-256. |
| 90-102 | Serve the file; report a broken connection, an HTTP error, or anything else as 502. |
| 105 | Serves on `127.0.0.1:8766` until stopped. |

### 9.5 `tools/stand_in_app.py` — a stand-in for the replica

**Used by:** `main_debug.py` and the tests, as the task runner's bridge. It plays the app from the
profile alone, and every command goes through the real queue and contract checks on one thread.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: it decides nothing for the assistant, never takes a user-only edge, never fills a secret field. |
| 9-12 | Imports: the queue's operations, the contract check, the navigator's breadth-first `search`, and `plain_digits`. |
| 15-16 | `StandInError`: the queue or a contract refused something. |
| 19-23 | `must_match(schema, value)`: stops the run when a command or result breaks its schema. |
| 26-30 | `shown(value)`: what the app shows: an amount as its digits (`2500000`, as the replica shows it), a 12+ digit value as `****` and its last 4 digits, anything else as given. |
| 33-48 | `StandInApp`: a navigator over the profile (its edges are already free of user-only ones), the screens and features, the queue and device, Home, no fields, no draft. It reports in once, as the replica's first poll does, so a command can be queued. |
| 50-52 | `current_screen()`: where it is; the agent's `Tools.screen`. |
| 54-64 | `send(operation, arguments)`: the seven steps: `queue_command`, `next_command` with its observation, the command's schema, `run`, the result's schema, `record_result`, return. |
| 66-84 | `run(command)`: performs the operation, then wraps it with a fresh observation and the draft version, as a contract result. |
| 86-99 | `observe()`: a contract-valid observation; its version rises after every operation. |
| 101-116 | `route(destination, arguments)`: the shortest walk, preferring edges whose selections the arguments name, first from here, then by way of Home (one `back` step), as `Routes.kt` does. |
| 118-141 | `navigate(...)`: an unknown screen is `rejected_policy`; no walk is `blocked`. Walks each edge, putting the row key in place of `*` and recording the trace; a row edge with no key stops `blocked` with `missing_argument`. |
| 143-161 | `fill(form, values)`: a field the form lacks, a secret or a user-only one is `rejected_policy` `field_not_fillable`; off the form it is `blocked` `navigate`. Shows each value, starts or raises the draft. |
| 163-175 | `show_review()`: on a feature's form whose `review` is another screen, moves there and marks the draft reviewed; anywhere else `blocked` with `no_review_here`. |

### 9.6 `main_debug.py` — the agent in one process

At the top of the repository. How to use it, and its VS Code entry, are in
[debugging](runbook/debugging.md#6-the-agent-in-one-process-main_debugpy).

| Lines | What it does |
|---|---|
| 1-6 | Docstring: the two ways to run it; it stops at the review. |
| 8-9 | `os` and `sys`. |
| 11 | `ASSISTANT_TRACE=1`: print every step. |
| 12-13 | `ASSISTANT_DEBUG=1`, before the imports below, because `command_queue` reads it when imported: a paused breakpoint cannot expire a command. |
| 15-31 | Imports: a thread and uvicorn for `--replica`, LangGraph's `Command`, and the same pieces `app.py` uses, plus the stand-in. |
| 33 | `EXAMPLE`: the card-transfer sentence, then `1` (the first card in the options). |
| 34 | One conversation ID. |
| 37-40 | `answer_for(turn, pending)`: an option's number becomes its ref; anything else is the user's words. |
| 43-50 | `said(result)`: prints the reply or the question and its numbered options, and the task state; returns the pending question, if any. |
| 53-57 | `main(chat, embeddings, turns, replica=False)`: the seam the test uses. Reads the profile, makes the navigator and the real queue. |
| 58-67 | `--replica`: the backend's own app (`create_app` with the same queue) under uvicorn on `127.0.0.1:8100`, on a daemon thread; `BridgeLink`, as the server uses; and where the replica is. |
| 68-70 | Otherwise the stand-in, with its own `current_screen`. |
| 71-74 | The feature index, the tools, the compiled graph and the conversation's config: the same objects as `app.py`. |
| 76-83 | One `graph.invoke` per turn: a new request, or `Command(resume=...)` while a question waits. Prints each reply. |
| 84-87 | If the agent still asks when the turns run out, reads the answer from the keyboard; returns the last result. |
| 90-93 | Run directly: `--replica` or not, the turns from the command line or the example, the real clients. |

### 9.7 `tools/evaluate_agent.py` — scores the agent on a case file

**Run by:** a person, `python tools/evaluate_agent.py tests/evaluation/heldout.json`; the tests call
`main` with fake models. **Input:** a case file ([the evaluation set](bridge/evaluation.md)).
**Output:** the report on screen, one record file under `local/scratch/p6-02/`, and exit 0, 1 or 2.

| Lines | What it does |
|---|---|
| 1-7 | Docstring: it drives the same agent as `main_debug.py` on the stand-in, never the replica, and changes nothing. |
| 9-17 | Imports; run as a script, Python only sees `tools/`, so the repository's root is put on the path first (the ruff line allows the later imports). |
| 19-32 | The same pieces as `main_debug.py`, plus the scorer and the stand-in. |
| 34-35 | `RECORDS`, where each run's record goes; the `option:` prefix. |
| 38-48 | `RecordingApp`: the stand-in, keeping the name of every operation it is sent. |
| 51-61 | `feature_facts(profile, navigator)`: each feature's requirement types, whether it has a form, and the screen its run must end on, planned from Home. |
| 64-66 | `pending_of(result)`: the agent's pending question after a turn, or `None`. |
| 69-90 | `talk(...)`: the first turn is a new request, and the offered 8 and the chosen intent are read from the state; each later turn resumes the question; an `option:` the question does not offer fails the case; it stops when the agent has answered. It counts the questions, notes whether the first one came before any `prepare`, and returns the question still waiting. |
| 93-122 | `run_case(...)`: a fresh queue, stand-in, tools and graph, with the case ID as `thread_id`. Records the final task state (the pending question's while one waits), `missing`, the values, the prepared feature and its requirement types, whether `prepare` ran, the reply (or the pending question), the operations and the last screen. Any exception becomes a crash, with the chosen intent from the last finished step. |
| 125-150 | `main(chat, embeddings, cases_path)`: reads the profile and the cases; one word is embedded first, and an unreachable service exits 2; one `FeatureIndex` for every case; runs and scores each case; writes the record file; prints the report; exits 0 when every gate passes, else 1. |
| 153-154 | Run directly, with the real clients. |

### 9.8 `tools/evaluation_score.py` — the measures and the report

**Called by:** `evaluate_agent.py`. Plain counting over the records; every value it prints goes
through `steps.masked`.

| Lines | What it does |
|---|---|
| 1-5 | Docstring. |
| 7-17 | Imports; `ALLOWED`, the three operations the task runner may send; `CRITICAL`, the types `ref` and `money`; the tags scored as safe stops. |
| 20-24 | `same(kind, expected, got)`: text and choice compare with Persian digits as Latin and spaces trimmed; refs and money exactly. |
| 27-29 | `critical(values, types)`: only the ref and money values. |
| 32-50 | `wrong_action(record, facts)`: an operation outside the three; or, once `prepare` ran, another feature with a form, an expected end of `created` or `clarifying`, a wrong critical value or an unexpected one. Returns the reason. |
| 53-58 | `asks_match(record)`: for a case ending `clarifying`, the agent still asks for exactly `asks`. |
| 61-84 | `score(record, facts)`: each value's match, the critical wrong action, and the unambiguous, preparation and safe-stop passes for the measures the tags name (`None` for the others), and why it failed. |
| 87-104 | `why(...)`: plain reasons: crash, option not offered, wrong action, another intent, each differing value, the end state, what is still asked, the screen, no question before the review. |
| 107-126 | `ratio`, `gate`, `scored` and `group`: the `passed / total (percent)` text, one gate line, a measure's count, and knowledge, not sure or a feature. |
| 129-168 | `report(records, seconds)`: the four gates with their targets, the right feature among the narrowed 8, the knowledge and not-sure confusion, per-field accuracy, the per-feature table, model errors, crashes and time; returns whether every gate passed. |
| 171-189 | `failing(records)`: every case that failed a measure, with the expected and the got intent, values (masked) and end state, and why. |

## 10. The tests, and what each one proves

Run all Python tests with `python -m pytest`. Each file's first line says what it covers. The
backend tests in `tests/backend/` are the ones that prove our runtime code.

| File | Tests | What they prove |
|---|---|---|
| `tests/backend/samples.py` | (helpers) | A valid observation and result, shared by the tests below. |
| `tests/backend/test_command_queue.py` | 13 | A device must report first; a queued command matches the contract; it is delivered once; one outstanding command per device; the same request returns the same command; a result frees the device; a repeated result changes nothing; unknown results are refused; an expired command is never delivered; a delivered one with no result ends `uncertain`; a stale one is closed, not delivered; each of the four builds a valid command and no other can be built. |
| `tests/backend/test_bridge_link.py` | 6 | A result comes back; a stale observation is resent; it gives up on a screen that keeps moving; a stale draft is not resent; a device that cannot take it stops the task; no result stops the task. |
| `tests/backend/test_navigator.py` | 10 | Direct and multi-step routes; selections and fields in order; screens without fields pass inside one navigate; no route; a user-only crossing is refused; secret and optional fields are never asked; going back by way of Home; a row key that spells a user-only tag is refused; a feature routes through its form. |
| `tests/backend/test_lookups.py` | 8 | رضا becomes her card; a typed number needs no contact; an unknown name is not guessed; two matches are ambiguous; every source finds records; the attribute picks what is sent; unknown lookups are unknown; it never writes. |
| `tests/backend/test_task_runner.py` | 13 | Card to رضا navigates, fills and stops at the review; a missing value stops before the app moves; all missing values are found at once; an unknown contact stops first; money must be a whole number, Persian digits too; an unexpected screen or a blocked navigate stops the run; a page with the wrong amount stops before the review; a selection travels as a navigate argument; a value the plan does not ask for is refused; a crafted row key never reaches the app; **no feature ever sends an operation outside the four**. |
| `tests/backend/test_api.py` | 17 | The HTTP routes: idle status; a click reaches the app and the result reaches the page; bodies outside the contract, unknown commands, unbuilt screens, extra fields, unseen devices are refused; a second destination waits; **no route can confirm, submit or pay**; the page loads nothing from the internet; plans, demo values and runs behave. |
| `tests/backend/test_steps.py` | 4 | The step prints are silent unless `ASSISTANT_TRACE=1`; a card number is masked and Persian stays readable; a traced card-to-رضا run never prints the full card; `ASSISTANT_DEBUG=1` gives a one-hour command and result wait. |
| `tests/backend/test_graph_run.py` | 2 | A whole run over the real routes, with a fake app that speaks the contract: card to رضا reaches the review and stops; a second run is refused. |
| `tests/backend/agent_fakes.py` | (helpers) | `FakeChat` (scripted answers; keeps every prompt and schema), `FakeEmbeddings` (fixed vectors), an unreachable embedding service, and `RecordingApp`, the stand-in app keeping every operation. No network. |
| `tests/backend/test_agent.py` | 20 | The agent end to end. Card to رضا in one sentence reaches `ready_for_user` with exactly `navigate`, `fill_form`, `show_review` and `{"value": 2500000, "unit": "IRR"}` for «۲۵۰ هزار تومان»; a missing source card is one question with 7 card options, and an option reaches the review; an ambiguous name gets the matches as options; a name not found asks again and sends nothing; a choice or a slot that was not offered is refused; money that is not a whole number in a known unit is refused; a knowledge question gets the fixed message and sends nothing; a feature without a form ends `completed`; `verify` fails on another screen; a model that cannot be reached fails and sends nothing; **no prompt holds a screen ID, an edge action, a user-only tag or a full card number**; every task-state change in every test is allowed by `task.json`. |
| `tests/backend/test_stand_in_app.py` | 6 | The stand-in's commands and results match the schemas, with rising versions, masked fields and a draft; no walk takes a user-only step; a row key replaces `*`, and a missing one is `blocked`; a secret or unknown field is refused; `show_review` away from a form has no review. |
| `tests/backend/test_agent_chat.py` | 9 | A chat turn and a graph-page Run refuse each other; an option must be one the agent offered; a turn takes exactly one of `text` or `option`; a fresh page shows an empty conversation; the chat script loads nothing from the internet. |
| `tests/backend/test_main_debug.py` | 1 | `main_debug.main` runs the built-in example to the review with the fakes, and never prints the full card. |
| `tests/evaluation/conftest.py` | (helpers) | Lets the evaluation tests import `tests/backend/agent_fakes.py` (a site package named `tests` hides this repository's `tests` folder). |
| `tests/evaluation/test_cases.py` | 6 | Both case files: the format, unique ids, the closed tag list, one turn per unambiguous case; every intent, ref, lookup key, money value and option turn is valid for the feature's plan from Home; an `e2e` case gives every value and ends right; the held-out counts and the tuning minimums; no held-out first turn in the tuning file; **the held-out file's SHA-256 equals the one in D-025**. |
| `tests/evaluation/test_evaluate.py` | 7 | The runner on the fakes: a right card transfer passes every measure and exits 0; a wrong amount and another feature with a form are critical wrong actions (exit 1); a missing source that ends asking is a safe stop; an option not offered fails the case and the run goes on; a crash is recorded and the next case runs; unreachable embeddings exit 2. |
| `tests/contracts/test_contracts.py` | 8 | The contracts suite and the transition tables. |
| `tests/foundation/test_project_checks.py` | 14 | The foundation and baseline checks, on throwaway copies of the repository. |
| `tests/isolation/test_isolation.py` | 9 | The reset guard, the development-host list and the isolation suite. |
| `tests/navgraph/test_navgraph.py`, `tests/api_structure/test_api_structure.py`, `tests/profile/test_profile.py`, `tests/mock_data/test_mock_data.py`, `tests/apk_baseline/test_apk_baseline.py`, `tests/rea_environment/test_rea_environment.py` | 9, 9, 9, 7, 6, 8 | Each check suite catches what it should, on throwaway inputs. |

The Kotlin tests (`PolicyTest`, `ExecutorTest`, the device tests `BridgeJourneysTest`,
`BridgeServicesTest`, `BridgeReachTest` and the per-feature screen tests) are listed in
[docs/bridge/README.md](bridge/README.md#read-the-code-in-this-order).

## 11. Things worth knowing

- **Everything in the backend is in memory.** Restarting the server forgets the device, the
  outstanding command, the run and the agent's conversations. The app simply reports in again.
- **One device, one run.** `DEVICE_ID = "replica-dev"` on both sides; `session_id` equals the
  device ID. A real deployment needs a real session and device mapping.
- **Timings.** App polls every 0.5 s; a command lives 30 s; the backend waits 40 s for a result and
  checks every 0.1 s; the app waits up to an hour for each reply; the app waits 3 s for a control; a navigate takes at most 40 steps. With
  `ASSISTANT_DEBUG=1` the command and the wait both last an hour ([debugging](runbook/debugging.md)).
- **Stale is normal.** If the screen changes between planning and delivery, the command is closed
  as stale and resent with the new version, up to three times.
- **Who refuses a confirm, and where.** Five walls: the operations (no confirm operation exists),
  the routes (no user-only edge in either graph), the fields (a secret field is never asked for),
  the last gate (`Policy.mayTap` / `mayType`) and the screen (password fields are never read or
  written). The backend adds its own limits on top (`command_queue.OPERATIONS`, the runner's
  checks, the harness's one field). Details: [safety.md](bridge/safety.md#the-five-walls).
- **Where amounts become integers.** `task_runner.money_value` (Python) and `ContractJson.fieldValue`
  (Kotlin); the contracts check refuses `1500.0`.
- **What changes for production is small on our side:** a real `lookups` adapter, a real session and
  device identity, and the rest of the assistant: RAG answers, the panel and its API (the agent is
  built: [agent.md](bridge/agent.md)).
  The rest is the app team's: see [the handover](app-team-handover.md).
