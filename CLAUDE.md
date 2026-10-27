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
   endpoint hosts, credentials or personal data. These live in the private evidence root,
   referenced only by evidence IDs. Issues, PR bodies and CI logs follow the same rule.
3. **No production banking calls.** The replica, mock services and adapters use explicit
   development addresses only. Do not copy endpoint defaults from the APK.
4. **The assistant never authorizes or submits a transaction.** No tool, command, executor
   action, alias, deep link or side effect may reach the user-only confirmation handler.
5. **The model gets no SQL tools or database credentials.** Business data is reached only
   through the application adapter's HTTP APIs.
6. **Money is exact integers** with an explicit unit. Never use floating point for amounts.
7. **No fake green.** Do not add empty tests, permanently skipped tests or placeholder CI jobs.
   A component without code is "not yet applicable", never "tested".

## Approval before implementation

Nothing gets implemented without the owner's approval (hooman, 27 Oct 2026).

1. **Propose.** A manager files the task as an issue with the `proposal` label and explains
   in plain language what it will do.
2. **Approve.** The owner approves it, and the issue's label changes from `proposal` to `approved`.
3. **Implement.** Agents only start issues labelled `approved`. If an issue is labelled
   `proposal` or `blocked`, do not start it, even if it looks ready.

Phase status is tracked in `docs/status.md`. Phase specs are in `docs/phases/`.

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
  `conda activate assistant`, a clone of RagBot's `faq` env. Never install into `faq`.
  See `docs/runbook/server-setup.md`.
- Foundation checks: `python tools/project_checks.py foundation`
- Planning baseline checks: `python tools/project_checks.py baseline`
- All checks: `python tools/project_checks.py all`
- Tests: `python -m pytest`
- Lint: `ruff check .`

The check runner uses only the Python standard library so it runs on Windows and Linux.
