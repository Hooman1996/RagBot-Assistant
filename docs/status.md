# Project Status

Last updated: 27 October 2026. This page is updated whenever a task changes state.

**Where we are:** planning is complete and the repository foundation is merged. We are at the
point right before implementation. Every remaining task is waiting on the owner's
approval or on an input.

## Status key

| Status | Meaning |
|---|---|
| Done | Merged to `main` and its acceptance check passed |
| Proposed | Spec written (and an issue filed if marked #), waiting on owner approval |
| Blocked | Cannot start until a named input arrives |
| Planned | Spec written. Its issues will be filed once the earlier phase's findings exist |

## Phases

| Phase | Status | Spec | Notes |
|---|---|---|---|
| Planning (plan v1, review v3, plan v2) | Done | `docs/plan/` | Plan v2 is the master |
| P0: Foundation | In progress | [P0](phases/P0-foundation.md) | 2 of 6 tasks done |
| P1a: First-workflow investigation | Blocked | [P1](phases/P1-apk-investigation.md) | Needs the APK and P0-03/P0-04 |
| P1b: Whole-app catalogue | Planned | [P1](phases/P1-apk-investigation.md) | Starts after P1a's first findings |
| P2: Backend extraction | Planned | [P2](phases/P2-backend-extraction.md) | Can start after P0, in parallel with P1a |
| P3: Chatbot parity | Planned | [P3](phases/P3-chatbot-parity.md) | After P2 |
| P4: Mock domain and manual replica | Planned | [P4](phases/P4-mock-domain-and-replica.md) | Needs the P1a toolkit decision and contracts |
| P5: Executor and deterministic demo | Planned | [P5](phases/P5-executor.md) | **First internal demo** |
| P6: Conversational assistant | Planned | [P6](phases/P6-conversational-assistant.md) | Needs P3, P5 and the model gate |
| P7: Demo hardening and review | Planned | [P7](phases/P7-hardening-and-demo.md) | Presentation package |
| P8: After-presentation integration | Planned | [P8](phases/P8-integration.md) | Only if the app team accepts |

## P0 tasks

| Task | What it does | Status |
|---|---|---|
| P0-01 | Agent rules, issue template, CI, check runner | Done (PR #1) |
| P0-02 | Plan, overview, decision index, RagBot source manifest | Done (PR #1) |
| P0-03 | Record the APK's identity (hash, version, splits) in the private evidence root | Blocked (#4): needs the APK and the evidence location |
| P0-04 | Install and pin REA, then identify the app's UI toolkit | Blocked (#5): needs P0-03 |
| P0-05 | Contract and lifecycle schemas, with no assistant submit path | Proposed (#2) |
| P0-06 | Project-only database and collection names, and a production/RagBot block | Proposed (#3) |

## Inputs needed from the owner

| Input | Needed by | Status |
|---|---|---|
| Original APK and any split files | P0-03 | Not yet provided |
| Private evidence location reachable by both agents | P0-03 | Decided: `local/evidence/` inside the checkout, git-ignored (move pending on the server) |
| Dev server checkout `/root/projects/assistant` and conda env `assistant` | Now | Done: checks, tests and lint pass on the server |
| A package mirror, proxy or offline transfer (the server cannot reach PyPI or Anaconda) | P0-04 (REA, Node, JDK, JADX) and P1a (Android SDK) | Two PyPI mirrors to test (runbook §2). npm, JDK and Android SDK sources still open. |
| Approval of each proposed issue | Every task | Ongoing |
