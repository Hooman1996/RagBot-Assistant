# D-026 — The assistant carries its own knowledge of the app, and its own checks

**Source:** Owner, 14 Nov 2026 (issue #140): "when we go for production we will not be able to
change how an already running app works, so we have to have our own ways of handling failures ...
the fixes will be on our side, and we cannot change their application in the production."

## Decision

1. **Nothing in the bank's app changes for the assistant.** The replica stands in for an app we
   cannot change, so no fix may rely on changing `android/`.
2. **What the assistant must know about the app lives on the assistant's side**, in
   `app_profiles/assistant_words.json`, edited by hand and read by `assistant/words.py`:
   Persian words per feature, the allowed keys and Persian labels of every choice field and row
   selection, values a feature decides itself, and field hints. The generated
   `replica.profile.json` stays as the app describes itself.
3. **Code checks what the model says before anything reaches the app:**
   - an amount is used only if the user wrote it (the newest answer's amount wins);
   - a choice or selection is sent only as one of its allowed keys;
   - a record is matched after normalizing the user's words, and a typed number must fit its field;
   - a typed answer during a question is read again in full, so a correction replaces the old value.
4. **The model sees app knowledge only as Persian labels**, never as keys, screens or full numbers;
   a long number is shown as `****` and its last 4 digits.

## Why

The P6-02 baseline ([report](../reports/p6-02-baseline.md)) failed every gate, mostly for reasons
the app cannot fix for us: choice keys the model could not know, Persian text sent as keys,
amounts a factor of 1,000 too large, corrections ignored, records missed by spelling. Each is now
handled before a command is sent ([the P6-03 report](../reports/p6-03-fixes.md)).

## Unchanged

The app, the contracts, the frozen held-out set (D-025) and every safety rule. The record-based
choice lists written from the mock fixtures must come from the bank's lookup APIs in production.
