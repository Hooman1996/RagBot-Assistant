# Standalone Assistant API

The standalone application serves the workflow and its HTTP bridge on loopback. The default
workflow uses the existing replica profile and synthetic lookups. The model and embeddings clients
use the development settings in `.env`, with addresses documented in `.env.example`.

From the checkout, in the `assistant` conda environment, start one worker.

```bash
set -a
source .env
set +a
python -m uvicorn assistant.app:create_standalone_app --factory --host 127.0.0.1 --port 8101
```

The process starts with the model services offline. They are called on demand when a turn needs
them. `GET /openapi.json` serves the [mobile contract](../../contracts/openapi/assistant_mobile.json)
with its schemas bundled. The development backend still starts with `python -m assistant` on port
8100 and serves the graph viewer and test harness used by the existing replica smoke.

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

The test starts the Uvicorn factory on a free loopback port with a separate mock model listener
and the repository's stand-in bridge. It checks the actual HTTP routes against the published
OpenAPI, drives a Persian card-transfer request from clarification to S-176, attempts cross-session
operations and late or stale results, and verifies cancellation. Both listeners stop after the
test. The check runs independently of the emulator, the graph viewer, and the development harness.

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
