# D-024 — The agent loop comes first

**Source:** Owner, 9 Nov 2026 (issue #136). Revises the stage order of D-020 to D-023.

## Decision

1. **The conversational agent loop is built now, before P1b (choose features), P1d (study the
   picked features) and P4 (mock services).** Every built journey is already a feature in the
   replica profile (D-021), and the mocks already run inside the app (D-022), so the agent has
   everything it needs to drive the replica today.
2. **No RAG in this stage.** A knowledge question gets a fixed Persian "not available yet" message.
   The retained RagBot answering (P2, P3) joins later.
3. **The models are the ones already served:** the Gemma model on vLLM for the choice and the
   values, and the TEI embeddings to narrow the features. Both are read-only HTTP clients
   (`assistant/models.py`). The assistant never configures, restarts or writes to either service.
   The served model is Gemma 4 E4B-it (owner, 9 Nov 2026: go on with it; the 12B name in RagBot's
   example configuration is not what runs).
4. **LangGraph runs the conversation, as D-002 says:** one `StateGraph`, an in-memory
   checkpointer per conversation, and `interrupt()` for the one question. Persistence across
   restarts is P7.
5. **The model has no tools.** It returns JSON held to a fixed schema; code plans, resolves, runs
   and checks. The agent reaches the app only through the existing task runner, so the only
   commands remain `navigate`, `fill_form` and `show_review`.

## Why

The replica, the bridge, the navigator and the task runner already cover every built journey. The
missing piece between a sentence and the review was the agent, and building it now shows whether
the served model is good enough before more features are studied.

## Unchanged

All safety rules. The assistant never confirms, submits or pays. Money is an exact integer with
its unit. The model never sees screens, edges, forms beyond the offered requirements, the
user-only list or a full card number.
