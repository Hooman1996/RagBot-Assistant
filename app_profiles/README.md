# App profiles

Sanitized maps of the Android app this assistant navigates: the feature and entry-point
inventory (P1a-03), the screen catalogue and navigation graph (P1a-04), and the API-structure
map (P1a-05). These are built from static reverse engineering of the catalogued APK and drive
the executor's allowed screens, edges, features and API calls.

## Evidence-ID scheme

This repository is public (AGENTS.md rule 2), so these files carry **no identifying strings** —
no package or class names, no resource labels, no hosts, URLs, brand/product names or personal
data. Every finding is referenced by an evidence ID and given a semantic (generic) name.

Node-ID scheme:

- `F-###` — the P1a-03 inventory IDs for features, entry points and navigation surfaces. The
  navigation graph (P1a-04) reuses three of them as nodes: `F-001` (launcher), `F-002` (internal
  router) and `F-066` (the server-driven dashboard-menu hub).
- `S-###` — a screen node in the P1a-04 navigation graph: an activity or a Compose navigation
  destination. IDs are stable, assigned in feature-area then name order. `F-000` is the area ID
  for a screen whose feature area could not be resolved statically.
- `E-###` — an endpoint in the P1a-05 API-structure map: one Retrofit interface method, assigned
  in service then declaration order. Its `area` reuses a P1a-03 feature-area ID (or `F-000` for a
  cross-cutting/infra service, or the one legacy aggregate interface whose metadata was stripped).
- `M-###` — a request/response model in the P1a-05 map, assigned in real-class-name order. An
  endpoint's `request_model`/`response_model`, and any model-typed parameter's `type`, name an
  `M-###`; every such reference resolves to a model in the same file.

The map from each evidence ID to its real class / route / manifest location (and any real brand
label), the raw decompiled/decoded output, and the derived data all live under the git-ignored
`local/evidence/` folder:

- `local/evidence/apk-decoded/` — apktool decode (manifest, resources, smali)
- `local/evidence/rea/` — REA output and derived JSON
- `local/evidence/findings/` — the private ID maps: `inventory-private.md` (P1a-03),
  `navgraph-private.md` (P1a-04, each node ID → its real route/class and host activity), and
  `api-private.md` (P1a-05, each `E-###`/`M-###` → its real service, method, header and model
  class, plus the real paths/hosts and the offline string-decryption attempt)

## Evidence labels

Each item carries one label with a one-line basis:

- **Observed** — read while running the app. *Not used in static steps.*
- **Recovered** — reconstructed from a precise code or manifest location.
- **Inferred** — a reasoned conclusion from several observations.
- **Unknown** — server-driven, feature-flagged, obfuscated or absent; the reason is given.

## Files

- `feature-inventory.md` — P1a-03 whole-app feature and entry-point inventory.
- `screen-catalogue.md` — P1a-04 whole-app screen catalogue, grouped by feature area.
- `navigation-graph.json` — P1a-04 nodes (screens) and edges (navigation steps), the structured
  map the later Python prototype loads.
- `navigation-graph.schema.json` — the JSON Schema for `navigation-graph.json`.
- `api-structure.json` — P1a-05 endpoints (Retrofit calls) and models (request/response shapes),
  the structured map the later Python prototype loads. Structure only: HTTP method, semantic
  endpoint name, parameter and model shapes, grouped by feature area, each with an evidence label.
  It carries **no** paths, hosts, header names, auth-token formats or brand/personal strings;
  those live only in `local/evidence/findings/api-private.md`.
- `api-structure.schema.json` — the draft 2020-12 JSON Schema for `api-structure.json`.
- `api-structure.md` — the human-readable API-structure map, grouped by feature area.
- `profile.schema.json`, `synthetic.profile.json` — the executor profile schema and a synthetic
  example (not derived from any real app).
