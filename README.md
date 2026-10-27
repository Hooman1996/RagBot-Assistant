# RagBot Assistant

An assistant for our banking app. It keeps RagBot's knowledge answering, and it can also
navigate the app, find saved recipients and prepare a transfer up to a verified review. The
user always authorizes the transaction themselves.

- One-page overview: [`docs/overview.md`](docs/overview.md)
- Master plan: [`docs/plan/RagBot_Assistant_Final_Project_Plan_v2.md`](docs/plan/RagBot_Assistant_Final_Project_Plan_v2.md)
- Rules for contributors and coding agents: [`AGENTS.md`](AGENTS.md)
- Current status: [`docs/status.md`](docs/status.md) · Phase specs: [`docs/phases/`](docs/phases/README.md)
- Server setup: [`docs/runbook/server-setup.md`](docs/runbook/server-setup.md)

## Checks

```bash
python tools/project_checks.py all
python -m pytest
ruff check .
```

Status: planning is complete, and P0-01/P0-02 are done. All further work waits on owner approval
(see `AGENTS.md`). No backend, replica or mock services exist yet.
