# Source Manifest

Records exactly what this repository takes from RagBot, and on what terms (plan §5).

## Baseline

| Field | Value |
|---|---|
| Source repository | `Hooman1996/RagBot` |
| Pinned commit | `fc2f6a1dc85be71f782868f18833a895e502226a` |
| Commit date | 24 October 2026 |
| Access | **Read-only.** This project never modifies RagBot's source, deployment, records, database objects, Qdrant collections or MinIO buckets. |
| Relationship | One-way extraction. No merge-back and no automatic sync. Later RagBot fixes are ported deliberately, with attribution and tests. |

The pinned commit is the extraction baseline. It is not a claim about what is deployed today.

## Copied modules

None yet. P2 (backend extraction) fills this table. Each copied or adapted module gets one row.

| RagBot path | New path | Copied or adapted | Adaptation notes | Dependency decision |
|---|---|---|---|---|

## Retained

Capabilities to keep or adapt, after P2's import and startup trace confirms what each needs:

- Knowledge answers and contextual follow-ups
- Query rewriting, retrieval, embeddings and reranking
- Required knowledge lookup and source attribution
- Mobile conversation, history and feedback functions
- Necessary model, service and database clients
- Health, lifecycle, error handling and focused logs
- Relevant tests and Persian normalization functions

### Mobile parity baseline

The `/api/mobile` operations at the pinned commit (`mobile_api.py`). The assistant implements equivalent behavior in its own namespace.

| Original operation | Behavior to preserve and test |
|---|---|
| `POST /api/mobile/v1/talk` | Answer, follow-up context, response fields, errors and session behavior |
| `GET /api/mobile/v1/history` | Correct user/session history, ordering and returned fields |
| `POST /api/mobile/v1/queries/{query_id}/feedback` | Feedback for an owned answer |
| `POST /api/mobile/v1/queries/{query_id}/comment` | Comment behavior and validation |
| `POST /api/mobile/v1/sessions/{session_id}/close` | Session closure and resulting lifecycle behavior |
| `POST /api/mobile/v1/sessions/{session_id}/satisfaction` | Satisfaction behavior and validation |

## Excluded

Excluded from the runtime. P2 must confirm each exclusion with a dependency check, not just by deleting folders.

| Excluded surface | Notes |
|---|---|
| Web dashboard, templates, static files, frontend build tooling | |
| Evaluation application, knowledge-gap audit application and their routes | Includes `evaluation_system/` and `internal_evaluation_api.py` |
| Analytics dashboard and mass-answer/batch application | |
| Web login, cookies, CSRF and dashboard roles | |
| OCR and document-upload workflows | Unless a parity finding proves a requirement |
| Knowledge-base management UI/API | Replaced by a small offline development ingestion utility |
| Production records, credentials and unrelated generated artifacts | Never copied |

### Known coupling to remove

- MinIO: `utils/health_checks.py` treats MinIO as optional for readiness, but
  `new_architecture/app/services/db_connection/connection.py` creates a MinIO client and
  checks or creates the bucket at startup. P2 must make it lazy and disabled by default.

## Evidence labels

Every APK-derived finding in this project carries exactly one label (plan §7):

| Label | Meaning |
|---|---|
| Observed | Verified in the running original application |
| Recovered | Supported by a precise APK code or resource location |
| Inferred | A plausible interpretation requiring verification |
| Designed for mock | Behavior deliberately introduced in our development system |
| Unknown | Insufficient evidence |

## License and attribution

RagBot and this repository have the same owner. Copied files keep a header comment naming
their RagBot path and the pinned commit. P2 records any third-party license that comes with
a retained dependency.
