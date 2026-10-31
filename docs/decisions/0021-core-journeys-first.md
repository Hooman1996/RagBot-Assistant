# D-021 — Core journeys first, the rest on demand

**Source:** Owner, 31 Oct 2026. Revises D-020's frontend scope.

## Decision

1. **Stage 1 stops at a core-journey replica, not the whole app.** FR-01 to FR-13 are done. Eight
   more batches are built, in this order, and then Stage 1 closes:
   FR-35 (loans and repayment), FR-23 (bank identifier conversion), FR-14 (wallet purchases and
   transfers, without the QR scanner), FR-15 (account and wallet funding), FR-19 and FR-20
   (cheques and digital cheques), FR-16 and FR-17 (vehicle hub and road tolls).
2. **The other batches are deferred.** FR-18, FR-21, FR-22, FR-24 to FR-34 and FR-36 to FR-45 are
   built only if the owner later picks one of their features. In the interactive graph their nodes
   are shown as not built.
3. **Every built journey is an assistant feature.** The assistant opens screens and fills fields
   in all built journeys. PINs, OTPs, passwords and the final confirm stay with the user (D-011):
   for those screens the assistant fills everything else and stops. Assistant support is built in
   two waves; the first covers the demo journeys (transfers, saved recipients, bills, top-up, loan
   repayment, identifier conversion), the second the rest.
4. **Lighter batch records.** A batch no longer writes separate inspection and requirements
   reports. It records one row in `docs/plan/frontend-replication-steps.md` and its acceptance
   output in the PR. The detailed requirements matrix is written once, in Stage 5a, for features
   the assistant supports.
5. **Stage 2 is narrowed** to the graph edges that lead to and run inside the built journeys.

## Unchanged

All safety rules, the stage order after Stage 1, the evidence labels and the per-batch acceptance
command stand.
