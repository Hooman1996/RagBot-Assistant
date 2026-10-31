# Revised build sequence (approved)

> **Revised by D-021 (owner, 31 Oct 2026).** Stage 1 builds core journeys, not the whole app:
> FR-01 to FR-13 plus eight named batches, with the rest built only if picked. Stage 2 covers only
> the built journeys, and every built journey becomes an assistant feature (Stage 4 is replaced).
> See `docs/decisions/0021-core-journeys-first.md`.

**Approved by the owner, 29 Oct 2026.** This revises the order and scope set by decision
**D-019** and is recorded as decision **D-020**. The master plan
(`RagBot_Assistant_Final_Project_Plan_v2.md`) stays the reference for goals and gates; where
the order differs, this document and D-020 win.

Two decisions drive the revision:

- **Replica and bridge are built *before* feature selection**, so the owner chooses features by
  navigating a working replica rather than from documents.
- **The whole app's screens are recreated at full fidelity from the start**, not only the picked
  features.

Everything below is built on **our replica**, with **synthetic data**, using **development
addresses only**. The real app and its production services are never called during this work.
The real app is involved only at the final integration stage, and only by the app team, on
their own staging build.

## Standing rules (unchanged)

1. The assistant **never authorizes or submits a transaction.** Final confirmation is the
   user's alone and is unreachable from any assistant operation, alias or deep link.
2. **No production banking calls.** Replica, mocks and adapters use development addresses only.
3. The model gets **no SQL tools or database credentials**; business data is reached only
   through the application adapter's HTTP APIs.
4. **Money is exact integers** with an explicit unit.
5. **RagBot is read-only.** The public repo holds only sanitized specs; raw evidence stays in
   `local/evidence/`, referenced by evidence IDs. Every reconstructed finding carries an
   evidence label.

## Parallel track — independent of the replica, can start after P0

- **T1. Extract the RagBot answering backend (P2).** A clean FastAPI service that keeps only the
  answering capability, dashboard and evaluation code not importable at runtime, MinIO off by
  default.
- **T2. Chatbot parity (P3).** The retained answering works in our stack, retained regression
  tests pass.

These feed the assistant in stage 6 but block nothing before it.

## Main track

1. **Reconstruct the frontend — whole app, full fidelity.** Recreate the app's screens and
   behaviour faithfully across the whole app (layout, Persian/RTL, validation, loading and error
   states, dialogs, Back), marking anything we cannot verify with its evidence label. Confirm the
   replica toolkit first (Jetpack Compose + Kotlin, per P1a-01). Server-driven or dynamic screens
   (e.g. the home menu) cannot be made faithful from static analysis alone and are flagged for the
   stage-2 validation.
2. **Build and validate the navigation graph.** The extracted graph (485 nodes / 590 edges) is
   enriched with each node and edge's prerequisites and stopping points, and validated against the
   real app. **Validation source: the owner checks the real app directly** (owner decision,
   29 Oct 2026). No observation pass is run from our environment.
3. **Implement the replica's bridge and the interactive graph.** The in-app executor bound to the
   replica maps semantic node/action IDs to the replica's navigation and form handlers. Alongside
   it, the interactive graph view: click a node and the replica moves to that screen, side by side.
   The real app gets its own bindings only at stage 8.
4. **Explore and choose features (P1b, on a live replica).** The owner navigates the replica and
   the interactive graph, with each feature annotated by its data dependencies and estimated effort
   (from the API-structure map: mock services, DB tables, draft/version flows, rated easy / medium /
   hard). Chosen features are recorded as a decision; each becomes one app-profile entry (intent,
   description, required slots, lookups, destination, form).
5. **Study the picked features in detail, then build their mocks.**
   - **5a (P1d).** For each picked feature: trace the journey to the user-confirmation boundary,
     capture its screens and states, recover its exact client contracts (requests, responses, units,
     identifiers, errors). The mocks are built from this, so it comes first.
   - **5b (P4).** Mock services for the picked features: APIs, synthetic records, validation, and
     stateful draft create/update/read with versions. The user-only mock submit/receipt is kept
     separate from anything the assistant can reach.
6. **Implement and test the assistant.**
   - **6a — scripted executor demo (P5).** Navigation, form filling and review work end-to-end from
     a fixed, non-LLM script, from several starting screens. Policy tests confirm no assistant
     operation reaches the user-only confirm handler.
   - **6b — conversational assistant (P6).** The LLM for RAG answers and for constrained intent and
     slot extraction, revalidated in deterministic code; a LangGraph workflow (clarify → resolve →
     prepare → verify → ready-for-user); exact-integer money; the assistant panel; mixed
     knowledge-plus-task requests.
   - **6c — the runtime loop** (below).
   - **6d — evaluation gates (P6).** A frozen Persian evaluation set (≥100 held-out cases); gates:
     zero critical wrong actions, ≥95% on the unambiguous set, ≥95% task preparation.
7. **Demonstrate on the replica, hardened (P7).** Fault scenarios, restart and resume, a bounded
   latency baseline, three clean repeatable runs on the pinned emulator, an honest acceptance report
   and presentation that identifies any remaining reconstruction differences.
8. **Integrate with the app team (P8) — after the demo, only if they accept.** The team validates
   the transitions our static map could not, implements the real bridge bindings in their codebase,
   and provides the real service contracts, real menu IDs and trusted user context. (Menu IDs alone
   are not enough — prerequisites, arguments, behaviour and results all need verification.) The
   accepted scenarios run on their staging build, with compatibility differences resolved before any
   production claim.

## The runtime loop (what happens on every user request)

Built in stage 6, shown in stage 7, reused unchanged by the app team in stage 8.

9. **Receive request + current state.** The assistant API interprets the intent, maintains the
   task, and determines the intended destination or operation.
10. **Determine required information** from three sources together: the **feature contract** (which
    fields and rules a feature needs, known in advance), the **live screen/form state** the app
    reports, and the **services** (current cards, recipients, balances). The assistant asks the user
    about anything missing or ambiguous.
11. **Request navigation with context.** The backend sends a semantic destination ID plus validated
    arguments; the deterministic navigator picks a valid route or traverses the intermediate steps.
12. **Execute and report.** The bridge verifies each transition and returns fresh state, errors or
    missing prerequisites. Transport is a **pull-based HTTP command/result loop** — the app asks, the
    backend returns at most one command, the app performs it and reports the result with a fresh
    observation; there is no push channel. A WebSocket is not in scope; it could be considered later
    if it earns its place.
13. **Populate and validate the form.** Fill from what the user supplied and what services resolved,
    through the app's normal handlers; the backend checks the resulting draft. Filling may happen
    during navigation as prerequisites arise.
14. **Present the verified review; the user authorizes.** The assistant never submits. It stays
    available to explain, correct or cancel; any correction produces a fresh review.

**Step-10 distinction:** the navigation graph says *where and how to move*; the feature contracts
say *what information is required*; the app and services say *what the current values are*. LangGraph
coordinates all three.

## Relationship to the phase specs

| Revised stage | Phase |
|---|---|
| T1, T2 | P2, P3 |
| 1 Reconstruct frontend (whole app) | P4-05/06, widened to the whole app |
| 2 Validate navigation graph | P1a-04 (done) + validation |
| 3 Replica bridge + interactive graph | P5 core, pulled earlier; interactive graph is new |
| 4 Explore and choose | P1b, on a live replica |
| 5a Study picked features | P1d |
| 5b Mocks for picked features | P4-02/03/04 |
| 6a Scripted executor demo | P5 |
| 6b Conversational assistant | P6 |
| 6d Evaluation gates | P6-02/07 |
| 7 Hardened demo | P7 |
| 8 App-team integration | P8 |

The small Python routing prototype (old P1c) is dropped — its job is done by the live replica plus
bridge in stages 3–4.
