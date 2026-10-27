# Repository Guidance

This file is the canonical rule set for every coding agent (Codex, Claude Code) and human contributor.
`CLAUDE.md` is an exact mirror. Edit `AGENTS.md`, copy it to `CLAUDE.md`, and run
`python tools/project_checks.py foundation`; CI fails if the two files differ.

## What this repository is

RagBot Assistant: a banking-app assistant that keeps RagBot's knowledge answering and adds
navigation, saved-data lookup and form preparation inside an Android replica of our app.
The master plan is `docs/plan/RagBot_Assistant_Final_Project_Plan_v2.md`. When a task and the
plan disagree, stop and ask the managers; do not improvise.

## Non-negotiable rules

1. **RagBot is read-only.** Never modify the RagBot repository, its deployment, databases,
   Qdrant collections or MinIO buckets. Extraction baseline: RagBot commit
   `fc2f6a1dc85be71f782868f18833a895e502226a` (see `docs/source-manifest.md`).
2. **This repository is public.** Never commit original or built APK/AAB files, decompiled
   source, raw REA output, original-app screenshots or traces, code findings, original
   endpoint hosts, credentials or personal data. These live in the git-ignored
   `local/evidence/` folder, referenced only by evidence IDs. Issues, PR bodies and CI logs
   follow the same rule.
3. **No production banking calls.** The replica, mock services and adapters use explicit
   development addresses only. Do not copy endpoint defaults from the APK.
4. **The assistant never authorizes or submits a transaction.** No tool, command, executor
   action, alias, deep link or side effect may reach the user-only confirmation handler.
5. **The model gets no SQL tools or database credentials.** Business data is reached only
   through the application adapter's HTTP APIs.
6. **Money is exact integers** with an explicit unit. Never use floating point for amounts.
7. **No fake green.** Do not add empty tests, permanently skipped tests or placeholder CI jobs.
   A component without code is "not yet applicable", never "tested".
8. **Only the code the issue needs.** The owner reads every line. Write the smallest, plainest
   code that meets the issue's goal and acceptance command. No extra features, options,
   abstractions, helpers or files "for later".

## Approval before implementation

Nothing gets implemented without the owner's approval (hooman, 27 Oct 2026).

1. **Propose.** When the owner asks for the next step, a manager explains in plain language what
   it will do, and files it as an issue with the `proposal` label.
2. **Approve.** The owner approves it, and the issue's label changes from `proposal` to `approved`.
3. **Hand over.** The manager then writes the exact prompt for Claude Code or Codex, and the
   owner gives it to the agent.
4. **Implement.** Agents only start issues labelled `approved`, and only do what the prompt and
   the issue say. If an issue is labelled `proposal` or `blocked`, do not start it, even if it
   looks ready.

The features the assistant will support are chosen by the owner after the APK's features are
catalogued. The card transfer in the plan is an example, not a commitment.

Phase status is tracked in `docs/status.md`. Phase specs are in `docs/phases/`.

## Local files

Every file this project uses lives inside the checkout. Anything that must not be committed
goes under the git-ignored `local/` folder:

| Path | Holds |
|---|---|
| `local/evidence/` (mode 700) | APK and splits (`apk/`), raw REA output (`rea/`), original-app captures (`captures/`), the private code-findings register (`findings/`) |
| `local/ragbot-baseline/` | Read-only RagBot clone at `fc2f6a1`, push disabled |
| `local/toolchain/` | Downloaded tools (Node.js, JDK, JADX, Android SDK) and offline installers |
| `local/scratch/` | Temporary files, logs and large archives. Safe to delete. |

- Never put project files in `/tmp` or another folder outside the checkout.
- Never run `git clean -x` or `git clean -X`. They delete `local/`, including the evidence.
- `.agents/` (shared skills) and the `.claude/skills` symlink are also git-ignored.

## Git workflow

Agents run the git flow themselves: they create branches, commit, push, open PRs and merge.
Keep the structure small. Only three kinds of branch exist:

| Branch | Created from | Merges into | Used for |
|---|---|---|---|
| `main` | n/a | n/a | The only long-lived branch. Always working. |
| `feature/<issue>-<short-name>` | `main`, or a sub-main | Where it came from | One approved issue, for example `feature/3-db-isolation` |
| `sub/<name>` (sub-main) | `main` | `main`, once the whole feature works | A feature that needs several issues, for example `sub/transfer-flow` |

1. **Branch.** Start one `feature/` branch per approved issue. For a multi-step feature, create
   its `sub/` branch once, then start each step's `feature/` branch from it.
2. **Change and test on the branch.** Run the issue's acceptance command and the full checks
   (Commands below) on the branch. If the change affects a running service, run that service
   from the branch and check it (Running services below).
3. **Commit and push** the branch. Commit messages: `[P0-06] short summary (#3)`.
4. **Open a PR** into the branch it came from, using the PR template, with the acceptance
   output pasted in.
5. **Merge** only when CI is green and the acceptance command passed. Changes under `contracts/`
   also need a manager's review first. Use squash-merge for `feature/` branches and a merge
   commit for a `sub/` branch going into `main`.
6. **Clean up.** Delete the merged branch, update the server checkout and the running services
   to the new version, and update `docs/status.md`.

- Never commit or push directly to `main` or to a `sub/` branch. Changes reach them only by a PR.
- Never create other branch kinds, and never more than one branch per issue. Open branches
  should be the few issues in progress, not a backlog.
- Never force-push, rebase or amend a branch that has been pushed.
- At the end of a task, report the PR link, the merge commit and the acceptance output.

## Running services

Whatever is running must be the latest version of the branch being used.

- After a merge into `main`, pull `main` in `/root/projects/assistant` and restart every
  running project process (for example the `uvicorn` backend, the mock services, the replica
  on the emulator) from that checkout.
- When testing a branch, run the services from that branch, and switch them back to `main`
  when the test ends.
- After a restart, confirm the running version is the checkout's `git rev-parse --short HEAD`
  (once a service reports its version, check that; until then, check its start time).
- This applies only to this project's own processes. Never stop, restart or reconfigure RagBot's
  services or the Docker containers (PostgreSQL, Qdrant, vLLM, TEI, MinIO).

## How work is organized

- One shared backlog: GitHub issues using `.github/ISSUE_TEMPLATE/task.yml`.
- Each issue has one implementation owner and lists its allowed files. Stay inside them.
- Work on a branch and open a PR. Contract changes (`contracts/`) need a reviewed PR and
  regenerated models/tests.
- Every task has an acceptance command. Run it before you open the PR, and paste its result
  in the PR body.
- Hit a stop condition: record the evidence in the PR/issue (sanitized) and stop that task.

## Evidence labels

Every APK-derived finding carries one label: `Observed`, `Recovered`, `Inferred`,
`Designed for mock` or `Unknown`. A declared activity or method is not proof of a usable
navigation path.

## Layout

| Path | Purpose |
|---|---|
| `assistant/` | FastAPI entry point, conversations, RAG integration, LangGraph tasks, command processing |
| `application_adapter/` | Typed feature interfaces, banking domain rules, mock/real HTTP adapters |
| `mock_services/` | Stateful mock banking APIs, fixtures, migrations, scenario controls |
| `android/` | Replica app and in-app executor |
| `contracts/` | Canonical OpenAPI/schemas, examples, generated transport models |
| `app_profiles/` | Sanitized screen maps, navigation graph, permitted actions |
| `tests/` | Unit, contract, integration, model-evaluation and device tests |
| `docs/` | Plan, overview, decisions, phase specs, source manifest, sanitized reports |
| `tools/` | Check runner and reproducible helper scripts |
| `deployment/` | Local development and demo setup |

Folders appear when their phase starts; do not create empty placeholders.

## Commands

- Python target: **3.12** (matches RagBot).
- Dev server: the checkout lives in `/root/projects/assistant`. Run everything inside
  `conda activate assistant`, a copy of RagBot's `faq` env. Never install into `faq`.
  The server has no direct PyPI access, so prefer packages already in `assistant`.
  See `docs/runbook/server-setup.md`.
- Foundation checks: `python tools/project_checks.py foundation`
- Planning baseline checks: `python tools/project_checks.py baseline`
- All checks: `python tools/project_checks.py all`
- Tests: `python -m pytest`
- Lint: `ruff check .`

The check runner uses only the Python standard library so it runs on Windows and Linux.
