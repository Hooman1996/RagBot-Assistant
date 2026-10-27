# P0: Foundation

**Goal:** the repository, rules, private evidence setup, pinned inputs, contract skeleton and
isolated environment exist before any feature work starts.
**Gate (plan §17):** the foundation checks pass, RagBot is untouched, the source and input
records exist, and no forbidden material has entered git.

| Task | What it will do | Inputs | Acceptance command | Status |
|---|---|---|---|---|
| P0-01 | Agent rules, issue and PR templates, CI and the check runner | Plan v2 | `python tools/project_checks.py foundation` | Done |
| P0-02 | Store the plan, overview, decision index and RagBot source manifest | RagBot `fc2f6a1` | `python tools/project_checks.py baseline` | Done |
| P0-03 | Set up the private evidence root. Record the APK's SHA-256, package, version, signing digest, splits, ABIs and minimum Android version. No APK bytes enter git. | APK, evidence location | `python tools/project_checks.py apk-baseline` | Blocked (#4) |
| P0-04 | Install and pin REA (`rea-agents`) with its Node, JDK and JADX versions for both agent environments. Run a first small APK query and record the UI toolkit and its recovery limits. **This is the first decompilation step.** | P0-03, server | `python tools/project_checks.py rea-environment` | Blocked (#5) |
| P0-05 | Schemas for commands, observations, results and the four API groups. Task and command transition tables as data. No assistant submit or authorize operation. | Plan §§9-13 | `python tools/project_checks.py contracts` | Proposed (#2) |
| P0-06 | Project-only database, schemas, roles and Qdrant collection names. A reset guard that rejects RagBot targets, and a development-host allowlist. | Plan §§12, 15 | `python tools/project_checks.py isolation` | Proposed (#3) |

**Order:** P0-05 and P0-06 can run now, in parallel. P0-03 then P0-04 run once the APK and the
evidence location exist.

**After P0:** file the P1a and P2 issues using the real findings (plan Appendix B).
