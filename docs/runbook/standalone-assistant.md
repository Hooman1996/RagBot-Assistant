# Standalone Assistant API

The standalone application serves the workflow and its HTTP bridge on loopback. It loads its graph
and banking lookup bindings at startup. The model and embeddings clients use the development
settings in `.env`, with addresses documented in `.env.example`.

From the checkout, in the `assistant` conda environment, start the synthetic lookup provider.

```bash
python -m uvicorn mock_services.app:create_app --factory --host 127.0.0.1 --port 8110
```

In another terminal, start the Assistant with both configuration files.

```bash
set -a
source .env
set +a
python -m assistant --graph app_profiles/replica.profile.json \
  --services mock_services/service-bindings.example.json
```

The standalone API listens on port 8101 by default and calls the model and mock services during
a turn. `GET /openapi.json` serves the [mobile contract](../../contracts/openapi/assistant_mobile.json)
with its schemas bundled. The development backend still starts with `python -m assistant` on port
8100 and serves the graph viewer and test harness with the development fixtures.

## Graph and service bindings

The graph uses the existing [profile schema](../../app_profiles/profile.schema.json). Its feature
list controls what the assistant offers, and its edges and forms control routes and required
inputs. The generated replica profile is the starting graph. To try a variant, copy it under
`local/scratch/`, edit the copy, and restart with its path.

The [example service file](../../mock_services/service-bindings.example.json) maps the two
internal-transfer lookups to typed, read-only operations.

| Lookup | Adapter | Provider |
|---|---|---|
| `accounts` | `accounts-http` | `GET /accounts` loads the source accounts and exact IRR balances. |
| `contacts.account` | `saved-accounts-http` | `GET /saved-accounts` loads named account recipients from the contact and saved-destination fixtures. |

To switch a provider, copy the service file under `local/scratch/`, change the operation's
endpoint to the other mock's loopback URL, and restart the Assistant with that file. Endpoints are
explicit loopback HTTP URLs with a port and path. Startup reports invalid graphs, invalid bindings,
and missing bindings used by the configured internal transfer. A failed configured lookup produces a
task error in history. Lookups outside the service file continue to use the development fixtures.

## Session and bridge calls

Generate one opaque, unpredictable session ID for the demo client and send it as
`X-Assistant-Session` on each request. Use the same ID in the URL for resume and cancel and in
the body for messages. Use a conversation ID for the life of the panel. The first accepted
message binds that session to its `device_id`, which is echoed on bridge commands. The development
backend defaults the session header to `replica-dev` for the existing replica. The standalone
header is a demo session binding for loopback use. Hosted access requires a trusted client identity
at the integration boundary.

| Operation | Success | Rejection or empty state |
|---|---|---|
| `POST /assistant/v1/messages` | `200` task status. The turn runs in the background. | `403` mismatched session. `409` busy turn, wrong device, stale question or unknown option. |
| `GET /assistant/v1/history?conversation_id=…` | `200` history and the current clarification or task state. | `200` empty history for a conversation outside this session or one that has yet to start. |
| `POST /assistant/v1/sessions/{session_id}/cancel` | `200` status for the session's current conversation. Read history for the final state. | `403` mismatched session. `409` when there is no cancellable task or the turn cannot start. |
| `POST /assistant/v1/sessions/{session_id}/resume` | `200` with a command or idle status after recording the bridge observation. | `403` mismatched session. |
| `POST /assistant/v1/results` | `200` idle status after an accepted result or retry. | `409` unknown, undelivered, expired or stale command result. |

All five operations return `422` for a missing session header or an invalid request. Resume and
results own the live observation. The observation attached to a message is validated with the
message body. The process holds session state in memory and runs one agent turn at a time. A
command already delivered when a task is cancelled can still report its result. The bridge stops
before its next command.

Feedback, comments, session close, and satisfaction remain in the OpenAPI document as planned
operations. The standalone process returns `404` for them and for the graph viewer and test
harness.

## Repeat the HTTP check

Run from the checkout in the `assistant` environment.

```bash
python -m pytest -s tests/backend/test_standalone_api.py
```

The test starts the standalone CLI on a free loopback port with a separate mock model listener
and the repository's stand-in bridge. It checks the actual HTTP routes against the published
OpenAPI, drives a Persian card-transfer request from clarification to S-176, attempts cross-session
operations and late or stale results, and verifies cancellation. Both listeners stop after the
test. The check runs independently of the emulator, the graph viewer, and the development harness.

Run the Demo 3/8 checks for alternate graph and provider bindings.

```bash
python -m pytest -s tests/backend/test_configured_standalone.py
```

These checks start real mock providers on separate free loopback ports. They write the second graph
and both service files under `local/scratch/`. Each variant uses the same stand-in client and
existing mobile API.

## Demo 2/8 acceptance

The backend code tested on 22 Nov 2026 was `2b490451faaa7e1e4905fbc829a0197a694d1b39`. The
documented startup on port 8101 served OpenAPI with the model and embedding endpoints unset.
The HTTP check reported the following results.

```text
PASS five standalone routes matched OpenAPI and development and planned routes returned 404
PASS messages, history, resume, results and cancel on the standalone process
PASS invalid and stale inputs, session isolation, and review S-176 at 2,500,000 IRR
2 passed in 4.60s
```

The development backend at the same code revision passed `python tools/bridge_e2e.py` with the
existing replica build `06c1386add06`. The smoke stopped at S-176 with source `****3566`, recipient
`****8880`, and 250,000 IRR. S-003 was refused. The 174 project checks, 496 Python tests, and both
Ruff checks passed. See the [demo roadmap](../plan/revised-build-sequence.md#approved-demo-roadmap)
for the remaining steps.

## Demo 3/8 acceptance

The documented example started the Assistant on 8101 and the read-only mock provider on 8110.
The restart test used `app_profiles/replica.profile.json` for A. For B, it copied the graph and
combined the two existing menu taps into one edge, made the payment ID required, and removed card
transfer from the offered features. It used the same stand-in bridge for both runs.

| Run | Backend plan to the internal-transfer form | Endpoint evidence | Review |
|---|---|---|---|
| A | S-026 → S-193 → S-192 | Provider A served `GET /accounts` and `GET /saved-accounts`. The latter supplied recipient `****7800`. | S-173, `account-2`, 3,000,000 IRR |
| B | S-026 → S-192, with payment ID `12345` filled | `GET /accounts` stayed on A. `GET /saved-accounts` moved to B and supplied recipient `****5821`. | S-173, `account-2`, 3,000,000 IRR |

Startup rejected an unknown graph screen, an unsupported operation field, an edge to a user-only
action, an invalid endpoint, a mismatched adapter, and a missing internal-transfer binding.
HTTP 404 and an incompatible response both produced a banking lookup error in history before a
bridge command was sent. The 174 project checks, 505 Python tests, and both Ruff checks passed.

## Demo 4/8 acceptance

On 23 Nov 2026, the standalone backend ran `c4e43aef2ff977ec3663e45619f607284c6870f0`.
The debug replica `0.1` (version code 1) displayed build `c4e43aef2ff9` on the pinned
`ragbot_investigation` emulator. Its executor accepted profile version 1. The generated profile
matched `app_profiles/replica.profile.json`. The setup used the
[standalone replica runbook](../bridge/try-it.md#verify-the-replica-against-standalone).

The QA probe completed `observe` at D-000. The adapted standalone smoke reached S-173 and
verified the rendered review. The graph state route returned 404 on the standalone API.

| Panel turn | Bridge result | Rendered review |
|---|---|---|
| «۳۰۰ هزار تومن از سپرده پس‌انداز به حساب رضا انتقال داخلی بزن بابت اجاره» | `navigate` followed S-026 → S-193 → S-192, `fill_form` succeeded at S-192, and `show_review` reached S-173. | Source `****0579`, recipient `****5821`, 3,000,000 IRR, reason «پرداخت اجاره». |
| «نه، مبلغ رو ۳۲۰ هزار تومن کن» | `navigate` returned from S-173 to S-192, `fill_form` succeeded there, and `show_review` opened a fresh S-173. | Same accounts and reason, 3,200,000 IRR. |

The configured mock returned HTTP 200 on `GET /accounts` and `GET /saved-accounts`. The backend
trace recorded `contacts.account` resolving to `****5821` in the smoke and in both panel turns.
That ending came from the alternate provider. The replica fixture's recipient ends in `7800`.
Cancel Task cleared the standalone task and left the corrected review visible. All bridge runs
stayed within the user review boundary. The local evidence ID is `DEMO-4`.

The relevant device group passed 36 tests across `AssistantPanelTest`, `BridgeJourneysTest`,
and `BridgeServicesTest`. Earlier runs reproduced a blocked picker being reported before its
dismissal finished. The bridge now waits for the picker to close before reporting the result.
The 178 Android JVM tests, 505 Python tests, 174 project checks, and both Ruff checks also passed.
Android lint reported 0 errors and 116 warnings.
