# D-028 — The road tolls go by the app's plate list

**Source:** Owner, 16 Nov 2026 (issue #156): approved the saved-plate change, then, after the live
test, "add it, new decision".

## Decision

1. **The four road-toll features (annual, traffic, freeway, air pollution) go by the app's plate
   list.** In the app, the vehicle menu opens the saved plates; tapping one opens the inquiry
   filled, and «پلاک جدید» opens it empty. The replica's map now has that step,
   `vehicle-menu-<key> > vehicle-*`, with the choice `toll-vehicle`: a saved car plate, or `new`.
   With no plate named, the assistant asks «کدام پلاک؟» first and offers the saved plates; only
   the user picks one, never the model. A plate the user types, whole or in part, is `new`.
2. **Four held-out cases gain that value.** H-042 to H-045 each type the plate in full, so each
   now also expects `"toll-vehicle": "new"`. What they prepare and where they end don't change.
   No other case changes.
3. **The held-out set is frozen** on 16 Nov 2026 at
   SHA-256 `984a08e08677d1208e75e8fde57867ecf7f9617989db2572e49982cb9fd75a1b`. This replaces the hash in
   [D-027](0027-heldout-follows-the-app.md). `tests/evaluation/test_cases.py` now reads the hash
   from this file. Everything else in D-025 and D-027 holds.

## Why

The format check requires an `e2e` case to list every value its plan needs, and the plate choice
is now one of them. A case must expect what the app does.

## The fresh run

Held-out on the final code, run twice: 0 / 143 critical wrong actions, 99 / 99 unambiguous, 106 / 106 prepared, 27 / 27 safe stops, the same on both runs (a third run, started before the last small change, matched too). Tuning: 0 / 49, 37 / 37, 40 / 40, 5 / 5. H-042 to H-045 pass on every run.
