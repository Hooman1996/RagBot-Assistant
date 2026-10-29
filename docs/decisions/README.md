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
| D-013 | Every project file lives inside the checkout. Private and local-only files go in git-ignored `local/` (evidence, RagBot baseline, toolchain, scratch). This replaces plan §16's "evidence root outside the tree". Safeguards: `.gitignore`, a check runner that scans only committable files, and a ban on `git clean -x`. | Owner, 27 Oct 2026 |
| D-014 | Git workflow: agents run it (branch, commit, push, PR, merge on green CI). Only `main`, one `feature/<issue>-<name>` branch per issue, and a `sub/<name>` sub-main for multi-step features. Nothing is pushed directly to `main`. | Owner, 27 Oct 2026 |
| D-015 | The dev server has no direct PyPI or Anaconda access. Candidate PyPI mirrors are recorded in the runbook, to be tested before use and set only for the `assistant` env. | Owner, 27 Oct 2026 |
| D-016 | Running project services (backend, mock services, replica) always run the latest version: restarted from `main` after each merge, or from the branch under test. RagBot's services and the Docker containers are never touched. | Owner, 27 Oct 2026 |
| D-017 | Agents write only the code an issue needs, as plainly as possible; the owner reads every line. Each step is explained, then approved, then handed to an agent as an exact prompt written by a manager. | Owner, 27 Oct 2026 |
| D-018 | The assistant's features are chosen by the owner after the APK's features are catalogued; they are not limited to transfers. The plan's card transfer is an example. The phase order this implies (catalogue before the detailed first feature) is proposed separately. | Owner, 27 Oct 2026 |
| D-019 | Order of work: make the repo, server and plan ready; install the REA MCP server; add the agent skills; run a final readiness check; then reverse engineer the whole app into a feature catalogue and navigation graph. The owner then picks features, a Python navigation prototype is tested, and only then are the picked features studied in detail and built. This replaces plan §17's transfer-first P1a. | Owner, 27 Oct 2026 |
| D-020 | Build-sequence revision (revises D-019): the replica and the in-app bridge are built before feature selection, so the owner picks features by navigating a working replica (the Python routing prototype P1c is dropped); the whole app's screens are recreated at full fidelity from the start; the navigation graph is validated against the real app by the owner directly, with no observation pass from the project environment. Full sequence in `docs/plan/revised-build-sequence.md`. | Owner, 29 Oct 2026 |
