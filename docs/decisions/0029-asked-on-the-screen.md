# D-029 — A question is asked on the screen it is about

**Source:** Owner, 16 Nov 2026 (issue #157): "when finding the user's intent, we should start
navigating to where an input is needed from the user so when we ask something from him, the user
can see the information that we are requesting"; then, for fields on several screens, "per screen".

## Decision

1. **The app moves before a question.** Once the feature is known and something is missing, the
   app goes along the route as far as the values allow. It fills what is known on the way, and
   stops on the screen where a missing choice is made, or on the form with a missing field. Then the
   question is asked. No review opens until every value is in, and only the user confirms.
2. **Each screen's values are asked on that screen.** When missing fields sit on several screens,
   only the first screen's are asked; after the answer the app fills them, moves on, and asks the
   next screen's there.
3. **One held-out case changes.** H-096 (Sayad cheque registration, only the receiver named) asked
   for the cheque number, amount and due date (S-165) together with the receiver's code (S-171). It
   now expects the three S-165 fields; the code is asked later, on S-171. No other case changes.
4. **The held-out set is frozen** on 16 Nov 2026 at
   SHA-256 `4129af7560832d74d25aef76dadeb7ba1c447a21b45972929887d8c0e8cc81c5`. This replaces the hash in
   [D-028](0028-plate-choice-in-heldout.md). `tests/evaluation/test_cases.py` now reads the hash from
   this file. The evaluation counts a task as prepared only once a run went for the review: moving
   the app to a question's screen prepares nothing, so it still counts as asked before preparing.
   Everything else in D-025 to D-028 holds.

## Why

The user answers better when the form the question is about is in front of them.

## The fresh run

Held-out run twice: 0 / 143 critical wrong actions, 99 / 99 unambiguous, 106 / 106 prepared, 27 / 27 safe stops, the same on both runs; H-096 passes on both. Tuning: 0 / 49, 37 / 37, 40 / 40, 5 / 5.
