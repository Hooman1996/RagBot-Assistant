# Project Status

Last updated: 28 October 2026. This page is updated whenever a task changes state.

**Where we are:** P0 is done. Reverse engineering (P1a) is under way; P1a-01 (UI toolkit), P1a-02 (emulator profile) and P1a-03 (whole-app feature and entry-point inventory) are done. P1a-04 (screen catalogue and navigation graph) is next.

## Status key

| Status | Meaning |
|---|---|
| Done | Merged to `main` and its acceptance check passed |
| Approved | The owner approved the issue; an agent may start it |
| Proposed | Spec written (and an issue filed if marked #), waiting on owner approval |
| Blocked | Cannot start until a named input arrives |
| Planned | Spec written. Its issues will be filed once the earlier phase's findings exist |

## Phases

| Phase | Status | Spec | Notes |
|---|---|---|---|
| Planning (plan v1, review v3, plan v2) | Done | `docs/plan/` | Plan v2 is the master; D-018 and D-019 change the P1 order |
| P0: Foundation and readiness | Done | [P0](phases/P0-foundation.md) | 8 of 8 tasks done |
| P1a: Reverse engineering and whole-app catalogue | In progress | [P1](phases/P1-apk-investigation.md) | P1a-01 (PR #26) and P1a-03 (#31) done; inventory in `app_profiles/feature-inventory.md` |
| P1b: Owner picks the features | Planned | [P1](phases/P1-apk-investigation.md) | After P1a |
| P1c: Navigation prototype | Planned | [P1](phases/P1-apk-investigation.md) | Python simulator, no Android |
| P1d: Picked features in detail | Planned | [P1](phases/P1-apk-investigation.md) | After P1c |
| P2: Backend extraction | Planned | [P2](phases/P2-backend-extraction.md) | Can start after P0 |
| P3: Chatbot parity | Planned | [P3](phases/P3-chatbot-parity.md) | After P2 |
| P4: Mock domain and manual replica | Planned | [P4](phases/P4-mock-domain-and-replica.md) | Needs P1d and the contracts |
| P5: Executor and deterministic demo | Planned | [P5](phases/P5-executor.md) | **First internal demo** |
| P6: Conversational assistant | Planned | [P6](phases/P6-conversational-assistant.md) | Needs P3, P5 and the model gate |
| P7: Demo hardening and review | Planned | [P7](phases/P7-hardening-and-demo.md) | Presentation package |
| P8: After-presentation integration | Planned | [P8](phases/P8-integration.md) | Only if the app team accepts |

## P0 tasks

| Task | What it does | Status |
|---|---|---|
| P0-01 | Agent rules, issue template, CI, check runner | Done (PR #1) |
| P0-02 | Plan, overview, decision index, RagBot source manifest | Done (PR #1) |
| P0-03 | Record the APK's identity (hash, version, splits). No decompilation. | Done (PR #17). Package and SDK fields are filled in P1a-01 with apktool |
| P0-04 | Install and pin the REA MCP server for both agents. No APK analysis. | Done (PR #18) |
| P0-05 | Contract and lifecycle schemas, with no assistant submit path | Done (PR #13) |
| P0-06 | Project-only database and collection names, and a production/RagBot block | Done (PR #15) |
| P0-07 | Agent skills in `.agents/skills/` | Done (PR #21). Runbook: `docs/runbook/agent-skills.md` |
| P0-08 | Readiness check on the server | Done (PR #24). Report: `docs/reports/P0-08-readiness.md` |

## P1a tasks

| Task | What it does | Status |
|---|---|---|
| P1a-01 | Identify the UI toolkit and what REA can recover | Done (PR #26) |
| P1a-02 | Fix and record the emulator profile | Done (PR #29). Runbook: `docs/runbook/emulator-profile.md` |
| P1a-03 | Inventory every feature and entry point | Done (#31) |
| P1a-04 | Whole-app screen catalogue and navigation graph in `app_profiles/` | Next |
| P1a-05 | Start the private code-findings register | Planned |

## Inputs needed from the owner

| Input | Needed by | Status |
|---|---|---|
| Original APK and any split files | P0-03 | Done: in `local/evidence/apk/` on the server |
| Private evidence location reachable by both agents | P0-03 | Done: `local/evidence/` (mode 700), moved on 27 Oct 2026 |
| Dev server checkout `/root/projects/assistant` and conda env `assistant` | Now | Done: checks, tests and lint pass on the server |
| A package source for npm, Node.js, a JDK and JADX | P0-04 | Done: all reachable (probe of 27 Oct 2026, runbook §2) |
| The list of agent skills to add | P0-07 | Done: issue #20 |
| Approval of each proposed issue | Every task | Ongoing |
