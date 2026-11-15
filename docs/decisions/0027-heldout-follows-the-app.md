# D-027 — A held-out case follows what the app accepts

**Source:** Owner, 15 Nov 2026 (issue #148): "its ok to change the case, just make sure everything
is working according to a real app".

## Decision

1. **The assistant only offers or accepts a record the app lists for that field.** A record the user
   names that the app doesn't list, such as a credit card as a card-transfer source, isn't used. The
   assistant says so and asks, offering the records the app lists. The rule is the `offer` list in
   `app_profiles/assistant_words.json` ([D-026](0026-assistant-side-knowledge.md)).
2. **Two held-out cases change to match.**
   - H-068: «يك ميليون ريال از كارت 0042 به رضا كارت به كارت كن» names the credit card `card-4`,
     which the app's source picker doesn't list. The case moves from `unambiguous`, `e2e` (prepared
     with `card-4`) to `not_found`: the source is asked, and the recipient and amount are kept.
   - H-027: a monthly transfer. The app asks for its day in a dialog, and the sentence names no
     day, so the assistant asks for it. The case gains a second turn, `option:schedule-day=1`, and
     still ends on the filled form with the day. It is now `e2e`, `option`: a case with two turns
     can't be `unambiguous`.
   - H-104 (#150): a withdrawal of 5,000,000 rials from a wallet that holds 3,250,000, which the
     app refuses. The amount becomes «۳۰۰ هزار تومن» (3,000,000 rials), so the case still prepares a
     withdrawal; the refusal over the balance is covered by the backend tests.
   No other case changes. The case checks now plan each case with its expected values, so a
   field the app asks only for some values (the day) counts, and an option turn may pick a choice.
3. **The held-out set is frozen** on 15 Nov 2026 (after #150) at
   SHA-256 `a3fb82c092b0265ef8150f24eb8e14261459bb50a0080ab66ea86ee2e3849d17`. This replaces
   the hash in [D-025](0025-frozen-evaluation-set.md). `tests/evaluation/test_cases.py` now reads the
   hash from this file. Everything else in D-025 (the tuning set, the gates, the definitions) holds.

## Why

The stand-in app accepts any record and fills any form, so H-068 expected a transfer from a card
the real app never offers as a source, H-027 a monthly transfer without the day the app needs, and H-104 a withdrawal over the wallet's
balance.
A case must expect what the app does, not what the stand-in allows.

## Counts after the change

143 cases: 99 `unambiguous` (was 101), 106 `e2e` (was 107), 5 `not_found` (was 4), 7 `option`
(was 6). The fresh run
is in [the P6-05b report](../reports/p6-05b-app-accepted-values.md).
