# Server Setup for Implementation

The development server is the machine where RagBot's models (vLLM, TEI) and databases are
already served. Codex and Claude Code implement the project there. Exact tool versions are
pinned by the tasks in the last column of section 3, not guessed here.

## 1. Layout on the dev server

Everything the project uses lives inside the checkout (owner decision, 27 Oct 2026). Files
that must not be committed sit under the git-ignored `local/` folder.

```text
/root/projects/assistant/                  <- repository checkout (repo root = this folder)
    .agents/skills/                        <- shared agent skills (git-ignored)
    .claude/skills -> ../.agents/skills    <- symlink so Claude Code sees the same skills
    local/                                 <- git-ignored, never committed
        evidence/        (mode 700)
            apk/         original APK and splits
            rea/         raw REA output
            captures/    original-app screenshots and traces
            findings/    private code-findings register
        ragbot-baseline/ read-only RagBot clone, detached at fc2f6a1, push disabled
        toolchain/       Node.js, JDK, JADX, Android SDK, offline installers
        scratch/         temporary files, logs, large archives (safe to delete)
```

- **RagBot source:** P2 copies the retained modules from `local/ragbot-baseline`, never from
  the checkout the running RagBot service uses. Its push URL is disabled, so the read-only
  rule is enforced, not just promised.
- **Running services:** PostgreSQL, Qdrant, vLLM and TEI already run as Docker containers
  that RagBot uses. This project only connects to them as a client, with its own database,
  role and collection (P0-06). It never stops, restarts or reconfigures a container. The
  names, the init SQL and how to run it are in `deployment/README.md`; settings are in
  `.env.example`.
- **Skills:** add a skill once under `.agents/skills/`. Codex reads it there, and Claude Code
  reads it through the `.claude/skills` symlink.
- **Safety of `local/`:** `.gitignore` keeps it out of commits, and the check runner only
  scans files git would commit. Never run `git clean -x` or `git clean -X`, which would delete
  it. P0-03 adds the setting that points code at `local/evidence/`; code never hard-codes it.

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
- **Package sources** (probe of 27 Oct 2026; nothing was installed):
  - PyPI: use `https://mirror-pypi.runflare.com/simple` (Liara's
    `https://package-mirror.liara.ir/repository/pypi/simple` is the fallback). pypi.org's index
    answers, but its file host times out. Pass the mirror per command
    (`pip install --index-url <mirror> ...`), and only into the `assistant` env, never `faq`.
  - npm: `registry.npmjs.org` works.
  - Node.js (`nodejs.org/dist`), the Temurin JDK and JADX (GitHub releases) download fine.
  - The system Node is 22.23.1, which is below REA's minimum, so P0-04 puts a project copy in
    `local/toolchain/`.
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
| `local/evidence/` (mode 700) | APK and all raw evidence | P0-03 | Moving into the checkout |
| Original APK and any split files in `local/evidence/apk/` | Investigation input | P0-03 | P0-03 |
| **A package mirror, proxy or offline transfer** (the server cannot reach PyPI, Anaconda or, likely, npm). PyPI mirror candidates are in section 2. | Every new tool below | P0-04 | P0-04 |
| Node.js in a range REA supports (its README lists 22.x ≥ 22.23.4, 24.x ≥ 24.22.1, or ≥ 26.12.1) and npm | REA MCP server and CLI | P0-04 | P0-04 |
| A JDK and JADX | REA's APK analysis | P0-04 | P0-04 |
| Android SDK command-line tools, platform-tools (`adb`) and the emulator, with hardware acceleration (`/dev/kvm` must exist) | Emulator profile, replica builds, original-app observation | P1a | P1a-02 |
| A **new** project database and Qdrant collection inside the existing Docker-hosted PostgreSQL and Qdrant. Never RagBot's `hihelp_db`, `hihelp_embeddings` or `hihelp-documents`. | Project data and vector store | P0-06 / P2 | P0-06 |
| Access to the existing Docker-hosted vLLM and TEI endpoints (development use, after a capacity check) | Answering and task interpretation | P2 / P6 | P6-01 |
| Appium | Independent UI checks | P5 | P5-07 |

## 4. Git identity on the server

Commits made on the server must use the owner's GitHub identity, never the machine's user and
host name (the repository is public). Set it once inside the checkout:

```bash
git config user.name "Hooman Aghalou"
git config user.email "67710406+Hooman1996@users.noreply.github.com"
```

## 5. Working rhythm

The full git and running-service rules are in `AGENTS.md`. In short:

1. A manager files a task as an issue labelled `proposal`.
2. The owner approves it, and the label changes to `approved`.
3. An agent starts a `feature/<issue>-<name>` branch from `main` (or from the feature's
   `sub/<name>` sub-main), changes and tests it there, then commits and pushes.
4. The agent opens a PR. Once CI is green and the acceptance command passed (and a manager has
   reviewed any `contracts/` change), the agent merges it and deletes the branch.
5. The agent pulls `main` in `/root/projects/assistant`, restarts the project's running
   services from it, and updates `docs/status.md`.

Recommended GitHub setting: protect `main` so that it only changes through a PR with green CI.
