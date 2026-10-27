# Server Setup for Implementation

The development server is the machine where RagBot's models (vLLM, TEI) and databases are
already served. Codex and Claude Code implement the project there. Exact tool versions are
pinned by the tasks in the last column of section 3, not guessed here.

## 1. Layout on the dev server

```text
/root/projects/assistant/        <- this repository's checkout (repo root = this folder)
    .agents/                     <- shared agent skills (local, not committed)
    .claude/skills -> ../.agents/skills   <- symlink so Claude Code sees the same skills
/root/projects/assistant-evidence/  <- private evidence root (never inside the repo)
    apk/          original APK and splits
    rea/          raw REA output
    captures/     original-app screenshots and traces
    findings/     private code-findings register
/root/projects/ragbot-baseline/  <- read-only RagBot clone, detached at fc2f6a1, push disabled
```

- **RagBot source:** P2 copies the retained modules from `/root/projects/ragbot-baseline`, never
  from the checkout the running RagBot service uses. Its push URL is disabled, so the
  read-only rule is enforced, not just promised.
- **Running services:** PostgreSQL, Qdrant, vLLM and TEI already run as Docker containers
  that RagBot uses. This project only connects to them as a client, with its own database,
  role and collection (P0-06). It never stops, restarts or reconfigures a container.

- **Skills:** add a skill once under `.agents/skills/`. Codex reads it there, and Claude Code
  reads it through the `.claude/skills` symlink. Both paths are listed in the checkout's
  `.git/info/exclude`, so they stay local until the managers decide to commit shared skills.
- **Evidence root:** both agents run on this server, so a local folder outside the checkout
  (mode `700`) meets the plan's rule. P0-03 adds the environment variable that points to it.
  Code never hard-codes the path.

## 2. Python environment

The `faq` conda environment holds everything RagBot needs. The project uses a **copy** of it
named `assistant`, so RagBot's environment is never modified.

**State on 27 Oct 2026:** done. The server cannot reach the Anaconda channel or PyPI (both
return HTTP 403), so `conda create --clone` failed. The env was copied with
`conda-pack --ignore-editable-packages` and `conda-unpack` instead. It has Python 3.12.12 and
the same packages as `faq`, including `pytest 9.1.1` and `ruff 0.16.4`. CI pins those same
versions.

- **No internet installs:** any new package (Python, npm, JDK, Android SDK) needs a mirror or
  proxy, or an offline transfer, first. Tasks should prefer packages already in `assistant`.
- **Editable packages copied from `faq`:** `snappbox_decision_support` points to
  `/root/projects/eda`, and `dots_ocr` / `dotsocr` point to a path that does not exist on this
  server. The project does not use any of them.

Install every later package into `assistant` only, and record it in the task that needs it.

Check a fresh checkout with the same commands CI runs:

```bash
cd /root/projects/assistant
conda activate assistant
python tools/project_checks.py all
python -m pytest
ruff check . && ruff format --check .
```

## 3. What the server needs, and when

| Need | Why | Needed by | Pinned in |
|---|---|---|---|
| git, conda env `assistant` (Python 3.12.12) | Backend, check runner | Now | Done |
| Codex CLI and Claude Code, both signed in | The implementing agents | Now | n/a |
| `/root/projects/assistant-evidence/` (mode 700) | APK and all raw evidence | P0-03 | Done (empty) |
| Original APK and any split files in `apk/` of the evidence root | Investigation input | P0-03 | P0-03 |
| **A package mirror, proxy or offline transfer** (the server cannot reach PyPI, Anaconda or, likely, npm) | Every new tool below | P0-04 | P0-04 |
| Node.js in a range REA supports (its README lists 22.x ≥ 22.23.4, 24.x ≥ 24.22.1, or ≥ 26.12.1) and npm | REA MCP server and CLI | P0-04 | P0-04 |
| A JDK and JADX | REA's APK analysis | P0-04 | P0-04 |
| Android SDK command-line tools, platform-tools (`adb`) and the emulator, with hardware acceleration (`/dev/kvm` must exist) | Emulator profile, replica builds, original-app observation | P1a | P1a-01 |
| A **new** project database and Qdrant collection inside the existing Docker-hosted PostgreSQL and Qdrant. Never RagBot's `hihelp_db`, `hihelp_embeddings` or `hihelp-documents`. | Project data and vector store | P0-06 / P2 | P0-06 |
| Access to the existing Docker-hosted vLLM and TEI endpoints (development use, after a capacity check) | Answering and task interpretation | P2 / P6 | P6-01 |
| Appium | Independent UI checks | P5 | P5-07 |

## 4. Working rhythm

1. A manager files a task as an issue labelled `proposal`.
2. The owner approves it, and the label changes to `approved`.
3. An agent implements only that issue on a branch and runs the acceptance command.
4. The agent opens a PR. CI must be green, and the managers review it.
5. Merge, then update `docs/status.md`.
