# P1: APK Investigation, Feature Choice and Navigation Prototype

**Goal:** reverse engineer the whole app, catalogue every feature and build its navigation
graph. Then the owner picks the assistant's features, we test the navigation in a small
prototype, and only then study the picked features in detail (owner decision D-019). The
evidence pack is also a showcase deliverable (plan §§7, 19).

**Rules:** all raw output (decompiled code, REA sessions, screenshots of the original app,
traces) goes to `local/evidence/`. Git only gets sanitized specs that reference evidence IDs.
Every finding carries one evidence label. Runtime observation uses only the observer's own
account, makes no transactions and never bypasses controls.

## P1a: reverse engineering and whole-app catalogue

Starts only when the owner asks for the reverse-engineering prompt, after P0-08 passes.

**Gate:** every discoverable feature is listed with its evidence status, and the sanitized screen
catalogue and navigation graph are in `app_profiles/`.

| Task | What it will do |
|---|---|
| P1a-01 | Identify the UI toolkit (native Views, Compose, Flutter or other) and what REA can recover from this build |
| P1a-02 | Fix the emulator profile (API level, resolution, density, Persian locale, font scale, keyboard) and record it |
| P1a-03 | Inventory every menu, feature and entry point, with evidence status. Mark server-driven or unreachable features as unknown, with the reason. |
| P1a-04 | Write the sanitized screen catalogue and navigation graph for the whole app into `app_profiles/` (semantic names plus evidence IDs) |
| P1a-05 | Start the private code-findings register in `local/evidence/findings/` (severity, confidence, evidence, suggested fix) |

## P1b: owner picks the features

The owner reviews the catalogue and chooses which features the assistant will support. The
choice is recorded as a decision. Each chosen feature becomes one entry in the app profile
(intent, description, required facts, lookups, destination, form).

## P1c: navigation prototype

A small Python simulator with no Android app. It loads the graph and the profile, keeps a
pretend current screen, and runs the loop in a terminal: intent, clarification, route, form
filling and review. It shows every step, so routes and conversations can be tested before
anything goes into the app.

**Gate:** for every picked feature, a route is found from every reachable screen, and the
simulated conversations pass their tests. The folder and exact scope are set in its proposal.

## P1d: picked features in detail

**Gate:** for each picked feature, a specification, its evidence-linked part of the graph, a
contract map and an unknowns list, with observed, recovered and designed-for-mock separated.

| Task | What it will do |
|---|---|
| P1d-01 | Trace each picked journey up to the user-confirmation boundary |
| P1d-02 | Capture its screens: UI hierarchy, loading, errors, dialogs, validation, Back behaviour |
| P1d-03 | Recover its client contracts: requests, responses, units, identifiers, errors. Record how Hibot reaches RagBot. |
| P1d-04 | Feasibility report and unknowns ledger for the picked features, and their effect on P4/P5 |
