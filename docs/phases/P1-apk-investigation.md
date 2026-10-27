# P1a / P1b: APK Investigation

**Goal:** understand the app from its APK alone, well enough to rebuild the transfer journey
faithfully (P1a) and to catalogue the rest of the app (P1b). The evidence pack is also a
showcase deliverable (plan §§7, 19).

**Rules:** all raw output (decompiled code, REA sessions, screenshots of the original app,
traces) goes to the private evidence root. Git only gets sanitized specs that reference
evidence IDs. Every finding carries one evidence label. Runtime observation uses only the
observer's own account, makes no transactions and never bypasses controls.

## P1a: first workflow in detail

**Gate:** a first-workflow specification, an evidence-linked graph, a contract map, an unknowns
list and the toolkit decision, with observed, recovered and designed-for-mock clearly separated.

| Task | What it will do |
|---|---|
| P1a-01 | Fix the emulator profile (API level, resolution, density, Persian locale, font scale, keyboard) and record it |
| P1a-02 | Trace startup and the transfer journey: home, chatbot menu (Hibot), source card, saved recipients, amount, review, the user-confirmation boundary |
| P1a-03 | Capture the reachable original screens: UI hierarchy, loading, errors, dialogs, validation, Back behaviour |
| P1a-04 | Recover client contracts for this journey: requests, responses, units, identifiers, errors. Record how Hibot reaches RagBot. |
| P1a-05 | Write the sanitized screen catalogue and navigation graph into `app_profiles/` (semantic names plus evidence IDs) |
| P1a-06 | Toolkit decision record (native Views, Compose, Flutter or other) and its effect on P4/P5. Feasibility report and unknowns ledger. |

## P1b: whole-app catalogue

**Gate:** every discoverable feature in the pinned APK is classified, with implementation
coverage and unknowns reported separately.

| Task | What it will do |
|---|---|
| P1b-01 | Inventory every menu, feature and entry point, with evidence status |
| P1b-02 | Mark server-driven or unreachable features as unknown, with the reason |
| P1b-03 | Start the private code-findings register (severity, confidence, evidence, suggested fix) |

P1b runs alongside P2 to P5 and never blocks the first internal demo.
