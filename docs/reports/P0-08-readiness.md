# P0-08 Readiness check

Date: 27 Oct 2026. `main` checked: `1f6b8f90eeb2022a28030f98c0e8a70be1b1f351`.

| Item | Result | Note |
|---|---|---|
| Repository | Pass | `main` clean and equal to `origin/main`; git identity matches `server-setup.md` §4; no APK, AAB, dex, jar, zip or so file in history. |
| Checks | Pass | `project_checks.py all`: 117 passed, 0 failed, 0 not applicable; `pytest`: 94 passed; `ruff check` and `ruff format --check` clean. |
| `local/` layout | Pass | All folders exist, including the four evidence subfolders; `local/evidence` is mode 700; `local/ragbot-baseline` at `fc2f6a1`, push URL disabled; `local`, `.agents/skills` and `.claude/skills` are ignored. |
| APK | Pass | `apk-baseline`: 5 passed, 0 failed. |
| REA | Pass | `rea-environment`: 8 passed; `rea doctor` passes `node`, `jadx`, `ghidra`, `ghidra-java`; `rea` connected in Claude Code and enabled in Codex; `inspect_apktool_client` reports 2.12.1. |
| Skills | Pass | Claude Code and Codex both list exactly the 19 skills of `agent-skills.md` §1 from `.agents/skills`; `jetpack-compose-performance` is absent. |
| Nothing outside the project changed | Pass | Docker: 9 containers, all started before 27 Oct 2026, 0 restarts. `faq`: latest revision is rev 0 (Apr 2026). No project service running (only REA MCP servers started by Claude Code sessions). RagBot's running checkout: no tracked changes; untracked files only, all dated before 27 Oct 2026. |
| Room for P1a | Pass | 824 GB free of 968 GB; `local/` uses about 2.8 GB. |
