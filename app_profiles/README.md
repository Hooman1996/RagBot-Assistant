# App profiles

Sanitized maps of the Android app this assistant navigates: the feature and entry-point
inventory (P1a-03), and later the screen catalogue and navigation graph (P1a-04). These are
built from static reverse engineering of the catalogued APK and drive the executor's allowed
screens, edges and features.

## Evidence-ID scheme

This repository is public (AGENTS.md rule 2), so these files carry **no identifying strings** —
no package or class names, no resource labels, no hosts, URLs, brand/product names or personal
data. Every finding is referenced by an evidence ID (`F-001`, `F-002`, …) and given a semantic
(generic) name.

The map from each evidence ID to its real class / manifest location (and any real brand label),
the raw decompiled/decoded output, and the derived data all live under the git-ignored
`local/evidence/` folder:

- `local/evidence/apk-decoded/` — apktool decode (manifest, resources, smali)
- `local/evidence/rea/` — REA output and derived JSON
- `local/evidence/findings/` — the private ID maps (e.g. `inventory-private.md`)

## Evidence labels

Each item carries one label with a one-line basis:

- **Observed** — read while running the app. *Not used in static steps.*
- **Recovered** — reconstructed from a precise code or manifest location.
- **Inferred** — a reasoned conclusion from several observations.
- **Unknown** — server-driven, feature-flagged, obfuscated or absent; the reason is given.

## Files

- `feature-inventory.md` — P1a-03 whole-app feature and entry-point inventory.
- `profile.schema.json`, `synthetic.profile.json` — the executor profile schema and a synthetic
  example (not derived from any real app).
