# App profiles

Sanitized maps of the Android app this assistant navigates: the feature and entry-point
inventory (P1a-03), and the screen catalogue and navigation graph (P1a-04). These are
built from static reverse engineering of the catalogued APK and drive the executor's allowed
screens, edges and features.

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

The map from each evidence ID to its real class / route / manifest location (and any real brand
label), the raw decompiled/decoded output, and the derived data all live under the git-ignored
`local/evidence/` folder:

- `local/evidence/apk-decoded/` — apktool decode (manifest, resources, smali)
- `local/evidence/rea/` — REA output and derived JSON
- `local/evidence/findings/` — the private ID maps: `inventory-private.md` (P1a-03) and
  `navgraph-private.md` (P1a-04, each node ID → its real route/class and host activity)

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
- `profile.schema.json`, `synthetic.profile.json` — the executor profile schema and a synthetic
  example (not derived from any real app).
