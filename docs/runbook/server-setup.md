# Server Setup for Implementation

The development server where Codex and Claude Code will implement the project. Exact versions
are pinned by the tasks shown in the last column, not guessed here.

## 1. Clone and check

```bash
git clone https://github.com/Hooman1996/RagBot-Assistant.git
cd RagBot-Assistant
python3.12 -m venv .venv && . .venv/bin/activate
python -m pip install pytest==8.3.5 ruff==0.11.8
python tools/project_checks.py all
python -m pytest
```

All checks should pass on a fresh clone. These are the same commands CI runs.

## 2. What the server needs, and when

| Need | Why | Needed by | Pinned in |
|---|---|---|---|
| Linux x86_64, git, Python 3.12 | Backend, check runner | Now | P0-01 (done) |
| Codex CLI and Claude Code, both signed in | The implementing agents | Now | n/a |
| Private evidence location (for example a private repo cloned **outside** this checkout) | APK and all raw evidence | P0-03 | P0-03 |
| Original APK and any split files, copied into the private evidence root | Investigation input | P0-03 | P0-03 |
| Node.js in a range REA supports (its README lists 22.x ≥ 22.23.4, 24.x ≥ 24.22.1, or ≥ 26.12.1) and npm | REA MCP server and CLI | P0-04 | P0-04 |
| A JDK and JADX | REA's APK analysis | P0-04 | P0-04 |
| Android SDK command-line tools, platform-tools (`adb`) and the emulator, with hardware acceleration (KVM) | Emulator profile, replica builds, original-app observation | P1a | P1a-01 |
| Docker (or local PostgreSQL and Qdrant) | Project database and vector store | P0-06 / P2 | P0-06 |
| Network access to our vLLM and TEI endpoints (development use only) | Answering and task interpretation | P2 / P6 | P6-01 |
| Appium | Independent UI checks | P5 | P5-07 |

## 3. Layout on the server

```text
~/work/RagBot-Assistant/            <- this repository (public)
~/work/RagBot-Assistant-evidence/   <- private evidence root (never inside the repo)
    apk/          original APK and splits
    rea/          raw REA output
    captures/     original-app screenshots and traces
    findings/     private code-findings register
```

The repository points to the evidence root through an environment variable set by P0-03,
never through a hard-coded path.

## 4. Working rhythm

1. A manager files a task as an issue labelled `proposal`.
2. The owner approves it, and the label changes to `approved`.
3. An agent implements only that issue on a branch and runs the acceptance command.
4. The agent opens a PR. CI must be green, and the managers review it.
5. Merge, then update `docs/status.md`.
