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
