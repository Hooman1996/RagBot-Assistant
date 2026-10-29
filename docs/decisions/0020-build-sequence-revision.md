# D-020 — Build-sequence revision: replica-first, whole-app fidelity

**Source:** Owner, 29 Oct 2026. Revises D-019.

## Decision

The build order and scope set by D-019 are revised as follows. The full sequence is in
`docs/plan/revised-build-sequence.md`.

1. **The replica and the in-app bridge are built before feature selection.** The owner chooses
   which features the assistant will support by navigating a working replica alongside the
   interactive navigation graph, rather than from documents. This moves the replica and the
   executor (bridge) ahead of P1b and makes the old Python routing prototype (P1c) unnecessary.
2. **The whole app's screens are recreated at full fidelity from the start**, not only the picked
   features. The owner accepts that this is a larger, longer build before any feature is chosen.
3. **The navigation graph is validated against the real app by the owner directly** (checking the
   real app themselves). No runtime observation pass is run from the project's environment, so the
   project makes no contact with the real app or its production services.

## Unchanged

All safety rules stand: the assistant never authorizes or submits a transaction; no production
banking calls (replica, mocks and adapters use development addresses only); the model gets no SQL
tools or database credentials; money is exact integers; RagBot is read-only; the public repo holds
only sanitized specs with raw evidence in `local/evidence/`.

The phase goals and gates in the master plan still apply; only the order and the frontend scope
change. The parallel RagBot extraction and parity work (P2/P3) is independent and may run alongside.
