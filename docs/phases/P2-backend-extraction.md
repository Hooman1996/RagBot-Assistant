# P2: Backend Extraction

**Goal:** a clean FastAPI service in this repository that contains only the RagBot answering
capabilities we keep. RagBot itself is never modified.
**Gate (plan §17):** the backend starts without dashboard or evaluation imports, and without
MinIO when it is disabled. The retained regression tests pass, and the source manifest is complete.

## Starting facts (RagBot `fc2f6a1`, read-only inspection)

- `main.py` is one combined app. It imports the web dashboard (`web_auth`, `web_permissions`,
  templates, static assets), mass-answer jobs, `kb_manager`, `internal_evaluation_api`,
  `coverage_check`, analytics and OCR alongside the answering path.
- The mobile surface is `mobile_api.py` (`/api/mobile`). Its talk request carries a
  gateway-supplied `national_code`. The new service uses synthetic context instead.
- The answering core is `answering_service.py` / `agent_service.py` / `agent_graph.py`
  (LangGraph), with history in `conversation_history.py` and `new_architecture/app/services/history/`.
- MinIO is created and its bucket is checked at startup in
  `new_architecture/app/services/db_connection/connection.py`.

## Planned tasks

| Task | What it will do |
|---|---|
| P2-01 | Trace imports and startup from the mobile answering path. Write the keep/drop list into the source manifest. Investigation only. |
| P2-02 | Copy the retained modules with attribution headers, and record each one in the manifest |
| P2-03 | New FastAPI entry point that imports only retained modules, with its own configuration and project database |
| P2-04 | A minimal dependency lock built from real imports (not a copy of RagBot's freeze) |
| P2-05 | Make the MinIO client lazy and disabled by default. Readiness follows configuration. Test with MinIO unavailable. |
| P2-06 | Bring over the relevant regression tests and add a test that dashboard and evaluation packages are not importable at runtime |
| P2-07 | CI: make the Python lint, type and test jobs required for the new service |
