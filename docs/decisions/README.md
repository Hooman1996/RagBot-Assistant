# Decision Index

The settled decisions behind the plan, with their source. The full reasoning for each
review recommendation is in Appendix A of the master plan. New decisions get a file
`NNNN-short-title.md` in this folder and a row here.

| ID | Decision | Source |
|---|---|---|
| D-001 | New independent repository. RagBot is read-only, with no merge-back. | Plan §§3, 5 |
| D-002 | Runtime orchestration is LangGraph with typed Python operations. Runtime MCP is deferred. REA keeps its development MCP. | Plan §6; review C1 |
| D-003 | Mobile communication is an HTTP request/response loop with one outstanding command per device. | Plan §10; review C3 |
| D-004 | The in-app executor uses the original app's toolkit (Kotlin for native, Dart for Flutter), decided after P1a. | Plan §8; review A3 |
| D-005 | The LLM does constrained intent and slot extraction and RAG answering. Deterministic code executes and verifies. | Plan §11; review C2 |
| D-006 | The mock banking service owns the transfer draft. UI and assistant share one update API. | Plan §12; review C4 |
| D-007 | A new project database with `assistant` and `mock_bank` schemas and roles, and a dedicated Qdrant collection. MinIO is off by default. | Plan §12; review C5, C7 |
| D-008 | The repository is public. APKs, decompiled code, raw REA output, original captures and code findings stay in a private evidence root. | Plan §§7, 16; review A9 |
| D-009 | Cloud coding agents may read decompiled APK material within the owner-authorized scope (owner decision, 27 Oct 2026). | Plan §3; review A2 |
| D-010 | One shared GitHub issue backlog, one owner per issue, and the bounded task template. | Plan §16; review B1, B4 |
| D-011 | The assistant cannot authorize or submit a transaction through any path. | Plan §13 |
| D-012 | Python 3.12 is the backend target, matching RagBot. | P0-01 |
