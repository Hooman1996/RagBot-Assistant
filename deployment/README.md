# Deployment

## Project database (P0-06)

The project shares the dev server's Docker-hosted PostgreSQL (`localhost:5432`) and Qdrant
(`localhost:6333`) with RagBot, but uses only its own resources:

| Resource | Name |
|---|---|
| Database | `ragbot_assistant_dev` |
| Schemas | `assistant` (owned by `assistant_app`), `mock_bank` (owned by `mock_bank_app`) |
| Login roles | `assistant_app`, `mock_bank_app`: no superuser, create-database or create-role rights, and no access to each other's schema |
| Qdrant collection | `ragbot_assistant_knowledge` |
| MinIO bucket (off by default) | `ragbot-assistant-dev` |

RagBot's `hihelp_db`, `hihelp_embeddings` and `hihelp-documents` are never targets.
`guard_target()` in `tools/project_checks.py` rejects them, and any name not listed above.

### Creating it

[init_project_db.sql](init_project_db.sql) creates the database, schemas and roles. Running it
is a separate step that needs the owner's approval; it has not been run. Run it once, with
`psql` (not installed on the dev server host), as a PostgreSQL admin:

```bash
psql -h localhost -p 5432 -U <admin> -d postgres -f deployment/init_project_db.sql
```

It stops at the first error, so it fails rather than reusing a role or database that already
exists. Then set each role's password from an interactive `psql` session, so no password is
written to a file or shell history:

```text
\password assistant_app
\password mock_bank_app
```

Put the passwords in `.env` (copied from [.env.example](../.env.example)), never in git.

The Qdrant collection is created later (P2), also as its own approved step.

### Known limit

PostgreSQL lets every role connect to every database by default (`CONNECT` for `PUBLIC`). The
new roles get no privileges inside `hihelp_db`, but they can still connect to it and see
anything RagBot granted to `PUBLIC`. Closing that needs `REVOKE CONNECT ON DATABASE hihelp_db
FROM PUBLIC` or a `pg_hba.conf` rule, and both change RagBot's resources, so this project does
not do either. That is the owner's decision.
