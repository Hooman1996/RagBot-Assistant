# RagBot Assistant

An assistant for our banking app. It keeps RagBot's knowledge answering, and it can also
navigate the app, find saved recipients and prepare a transfer up to a verified review. The
user always authorizes the transaction themselves.

- One-page overview: [`docs/overview.md`](docs/overview.md)
- Master plan: [`docs/plan/RagBot_Assistant_Final_Project_Plan_v2.md`](docs/plan/RagBot_Assistant_Final_Project_Plan_v2.md)
- Rules for contributors and coding agents: [`AGENTS.md`](AGENTS.md)

## Checks

```bash
python tools/project_checks.py all
python -m pytest
ruff check .
```

Status: P0 foundation. No backend, replica or mock services exist yet.
