# RagBot Assistant in One Page

The full plan is `docs/plan/RagBot_Assistant_Final_Project_Plan_v2.md`. If this page and the plan disagree, the plan wins.

## What we are building

An assistant for our banking app. It answers the questions RagBot answers, and it can also
help the user act inside the app: find recipients through an API, navigate to the transfer
flow, fill the form, handle clarifications and corrections, and stop at a verified review.
**The user always performs the final authorization.**

We build it on an editable Android replica of our app, reconstructed from the APK with REA,
backed by stateful mock services with synthetic data. Later, the app team can replace the
mocks with their services and integrate the in-app executor.

## Architecture in brief

- **FastAPI backend** with a LangGraph task workflow and typed Python operations. No runtime MCP.
- **Local LLM** (vLLM) for RAG answers, and for constrained intent and slot extraction on tasks.
  Deterministic code does lookups, validation and verification.
- **In-app executor** inside the replica. It observes, navigates and fills forms through the
  app's normal handlers.
- **HTTP command loop.** Each app request returns one command at most. The app posts the
  result along with a fresh observation. There is no push channel.
- **Mock banking service** owns the transfer draft. The UI and the assistant use the same API.
- **PostgreSQL** with a new project database and separate `assistant` and `mock_bank`
  schemas. A dedicated Qdrant collection. MinIO is off by default.

## Phases

| Phase | Goal |
|---|---|
| P0 | Foundation: conventions, CI, plan, source manifest, APK baseline, REA setup, contracts, isolation |
| P1a / P1b | APK investigation: first workflow in detail, then the whole-app catalogue |
| P2 | Extract the RagBot backend into a clean service |
| P3 | Chatbot parity |
| P4 | Mock domain and a manual replica journey from home to review |
| P5 | Executor and a deterministic (non-LLM) demo |
| P6 | Conversational assistant with Persian evaluation gates |
| P7 | Hardening, acceptance report and presentation |
| P8 | Integration with the app team after the presentation |

## Rules to remember

See `AGENTS.md`. The short version: RagBot is read-only. This repo is public, so private
evidence stays out of it. No production calls. The assistant cannot submit transactions.
