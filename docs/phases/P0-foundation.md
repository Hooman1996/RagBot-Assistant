# P0: Foundation and readiness

**Goal:** the repository, rules, server, contracts, isolated environment and tools are ready
before any reverse engineering starts.
**Gate:** the readiness check (P0-08) passes on the server, RagBot is untouched, and no
forbidden material has entered git. Only then does the owner ask for the reverse-engineering
prompt (P1a).

| Task | What it will do | Inputs | Acceptance command | Status |
|---|---|---|---|---|
| P0-01 | Agent rules, issue and PR templates, CI and the check runner | Plan v2 | `python tools/project_checks.py foundation` | Done |
| P0-02 | Store the plan, overview, decision index and RagBot source manifest | RagBot `fc2f6a1` | `python tools/project_checks.py baseline` | Done |
| P0-03 | Record the APK's SHA-256, package, version, signing digest, splits, ABIs and minimum Android version. The APK stays in `local/evidence/apk/`; no APK bytes enter git. No decompilation. | APK on the server | `python tools/project_checks.py apk-baseline` | Done (PR #17) |
| P0-04 | Install and pin the REA MCP server (`rea-agents`) with its Node, JDK, JADX, apktool and Ghidra versions under `local/toolchain/`, and register it for Claude Code and Codex. Check that it starts and lists its tools. **No APK analysis yet.** | A package source for npm, Node, JDK and JADX | `python tools/project_checks.py rea-environment` | Done (PR #18) |
| P0-05 | Schemas for the four records and the response, the five API groups, the state tables as data, and the app-profile schema. No assistant submit or authorize operation. | Plan §§9-13, the reviewed schema doc | `python tools/project_checks.py contracts` | Done (PR #13) |
| P0-06 | Project-only database, schemas, roles and Qdrant collection names. A reset guard that rejects RagBot targets, and a development-host allowlist. | Plan §§12, 15 | `python tools/project_checks.py isolation` | Done (PR #15) |
| P0-07 | Add the agent skills the owner chooses to `.agents/skills/` (git-ignored, shared by Codex and Claude Code through the `.claude/skills` symlink) | Owner's list of skills | Both agents list the skills | Planned |
| P0-08 | Readiness check on the server: `main` clean and up to date, all checks green, `local/` layout and permissions, APK hash matches P0-03, the REA MCP server answers in both agents, the skills are listed, and the Docker containers were not touched | P0-03 to P0-07 | A written report | Planned |

**Order:** P0-05, then P0-06 (both edit the check runner). P0-03 can run alongside them. P0-04
needs a package source. P0-07 and P0-08 come last.
