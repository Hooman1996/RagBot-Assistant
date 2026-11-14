# P6: Conversational Assistant

**Goal:** a user can ask in Persian to prepare a transfer, clarify and correct it, and get a
verified review. The local LLM interprets the request and deterministic code executes it.
**Gate (plan §§17-18):** the model and full-task gates pass, retained RAG parity holds, and there
is no fabricated success and no implicit submission.

| Task | What it will do |
|---|---|
| P6-01 | Identify the real model, vLLM version, structured-output support and GPU capacity. Run the smoke test. |
| P6-02 | Persian evaluation set: at least 100 held-out cases plus separate tuning examples, frozen before tuning |
| P6-03 | Intent selection (D-023): semantic search with the TEI embeddings narrows the features to the closest few, and the model chooses among them in a fixed JSON shape (an intent, "knowledge question" or "not sure", plus slot values). Code checks every answer, and the model never sees the graph, the forms or the user-only list. Exact integer amounts and ریال/تومان handling. See [the design note](../bridge/intent-selection.md). |
| P6-04 | LangGraph task workflow following the §13 transition table: clarify (one question for everything the navigator found missing), resolve, prepare (the task runner), verify, ready_for_user |
| P6-05 | Assistant panel: open, minimize, reopen, cancel, clarification choices, progress |
| P6-06 | Mixed requests: answer knowledge questions without losing the task |
| P6-07 | Run the gates: zero critical wrong actions, at least 95% on the unambiguous set, at least 95% task preparation |

## Progress (D-024, #136)

The agent loop is built first, before RAG and before P1b, P1d and P4
([D-024](../decisions/0024-agent-loop-first.md), [the agent](../bridge/agent.md)).

| Task | Done in #136 | Left |
|---|---|---|
| P6-01 | Found read-only: vLLM 0.26.0 serving Gemma 4 E4B-it (`/app/model`, 4,000-token context; 6,144 since 11 Nov 2026); `response_format` with `json_schema` holds answers to the schema; TEI 1.9.3 with jina-embeddings-v5-text-small-retrieval; smoke call passed | GPU capacity and latency under load; whether a larger model is available |
| P6-02 | Done in #138: the frozen set of 143 held-out cases and 43 tuning cases, and `tools/evaluate_agent.py` ([the evaluation set](../bridge/evaluation.md), [D-025](../decisions/0025-frozen-evaluation-set.md)); baseline in [the P6-02 report](../reports/p6-02-baseline.md) | — |
| P6-03 | Narrowing to 8 with TEI, the constrained choice, the values with exact toman/rial handling, all checked in code; long numbers hidden from the model. #140 ([report](../reports/p6-03-fixes.md), [D-026](../decisions/0026-assistant-side-knowledge.md)): the assistant's own words about the app (Persian words per feature, allowed keys for every choice field), amounts checked against the user's words, normalized record matching; on the frozen held-out set every gate passes | Record-based choice lists from the bank's lookup APIs in production; a fresh held-out set |
| P6-04 | One LangGraph graph: understand, plan, extract, check, clarify (one question, options for 2 to 8 records), resolve, prepare, verify, ready_for_user; every change checked against `task.json`; in-memory checkpointer ; a correction typed during a question replaces the held value (#140) ; #142: cancel at any question, at the review, while paused or during a run (`cancelling` → `cancelled`), a correction after the review prepared again, `paused` when the app is out of reach, `reconciling` after an uncertain command ([the agent](../bridge/agent.md#the-tasks-life-around-the-review)) | Persistence (P7), the knowledge path with RAG (P6-06) |
| P6-05 | #144: the replica's assistant panel ([the panel](../bridge/panel.md)) behind a floating, draggable **دستیار** bubble (and beside the chatbot): a small chat box with options and progress that steps aside while the agent moves the app, minimize, and a separate cancel, in the app's own window; the backend's `POST /assistant/v1/messages`, `GET /history` and `POST /sessions/{id}/cancel` | Storage of conversations (with the database schema, later); the bank's own panel (handover deliverable 5) |
| P6-06 | A knowledge question gets a fixed message | Answers with RAG, without losing the task |
| P6-07 | — | The gates, with `tools/evaluate_agent.py`, repeated on the replica |

