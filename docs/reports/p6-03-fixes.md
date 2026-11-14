# P6-03: the P6-02 failures, fixed on the assistant's side

**Issue:** #140. **Date:** 14 Nov 2026. **Decision:** [D-026](../decisions/0026-assistant-side-knowledge.md).
**Measured with:** [the evaluation set](../bridge/evaluation.md); the held-out file is unchanged
(SHA-256 `fdf15cd2…`, [D-025](../decisions/0025-frozen-evaluation-set.md)). Same models as the
baseline: Gemma 4 E4B-it on vLLM 0.26.0 and jina-embeddings-v5 on TEI 1.9.3.

Nothing in `android/` changed: the bank's app cannot be changed in production, so every fix is
in the assistant's Python code or in the assistant's own file `app_profiles/assistant_words.json`.

## The fixes, by baseline cause

| Baseline cause (held-out cases) | Fix | Where |
|---|---|---|
| Choice key (20) | Every choice field and row selection the agent can ask has its allowed keys with Persian labels. The model sees the labels and must pick one (schema enum); code maps it to the key and refuses anything else, asking again with the options. A choice with one key, or a value the feature decides (the cheque menu's and the paid-inquiry menu's row), is set by code and never asked. | `assistant_words.json` `choices`, `fixed`; `words.py`; `agent_nodes.plan`; `slots.choice`, `slots.shape`; `replies.lookup_options` |
| Choice key sent as a row key, crash (6) | The same: only an allowed key can reach `navigate`. | as above |
| Record not found as written (13) | Matching makes digits Latin and ي/ك Persian, drops ZWNJ and «ٔ», ignores generic words (کارت، حساب، کیف، شبا…), words no record holds and spoken endings. «حساب» alone is still ambiguous and «علی» still not found. | `lookups.plain`, `lookups.matching`, `lookups.holds` |
| Wrong pick (7) and shared title (3) | Persian words per feature for the embeddings and the model's choice; features embedded with TEI's `document` prompt; firmer knowledge and not-sure lines in the choice prompt. | `assistant_words.json` `features`; `intent.py`; `models.Embeddings` |
| Persian amount, ×1000 (3 critical) | Code reads the amounts in the user's words (`amounts.py`). The newest answer's amount wins; else the model's amount only if the user wrote it; else the one written amount; else the agent asks. | `amounts.py`, `slots.money` |
| Correction ignored (3 critical) | A typed answer re-reads every value, and a new value replaces the old one. | `agent_nodes.clarify`, `agent_nodes.extract`, `slots.money` |
| Number placeholder not restored (4) | A long number is shown as `****` and its last 4 digits (the model copied `[number 1]` as `1`), and put back wherever it appears; with only generic words around it, just the number is kept. A typed IBAN that matches no saved record is kept. | `slots.hidden`, `slots.text`, `lookups.typed`, `lookups.resolve` |
| Fee account not read («کارمزدش از حساب …») | A field hint: «حسابی که مبلغ یا کارمزد از آن برداشت می‌شود». | `assistant_words.json` `fields`; `agent_nodes.extract` |
| A typed account number taken as an IBAN | A typed number must fit its field: an IBAN field takes only an IBAN, and a bare number is never matched inside a saved IBAN, so the agent asks. | `lookups.typed`, `lookups.matching` |

## Results

| Gate | Target | Held-out baseline (P6-02) | Held-out run 1 | Held-out run 3 (final code) |
|---|---|---|---|---|
| Critical wrong actions | 0 | 9 / 143 | 1 / 143 | **0 / 143 — PASS** |
| Unambiguous | ≥ 95% | 68 / 101 (67.3%) | 96 / 101 (95.0%) | **98 / 101 (97.0%) — PASS** |
| Task preparation | ≥ 95% | 51 / 107 (47.7%) | 100 / 107 (93.5%) | **102 / 107 (95.3%) — PASS** |
| Safe stops | all | 25 / 26 | 26 / 26 | **26 / 26 — PASS** |
| Right feature among the narrowed 8 | — | 120 / 127 | 127 / 127 | 127 / 127 |
| Knowledge / not sure answered as such | — | 8 / 9, 7 / 7 | 9 / 9, 7 / 7 | 9 / 9, 7 / 7 |
| Crashes | — | 6 | 0 | 0 |

Tuning set, final code: 0 / 43 critical wrong actions, 31 / 32 unambiguous, 33 / 34 prepared,
5 / 5 safe stops, all PASS (it was 1, 18 / 32, 12 / 34 and 3 / 5 before).

**How to read the held-out numbers.** The fixes for the baseline causes were designed from the
P6-02 report, which lists the held-out failures, so the held-out set is no longer unseen. Run 1
measured those fixes. Two more were made after run 1: the fee-account hint (the tuning set fails
the same way, T-032) and the IBAN rule (from the one critical wrong action, H-070). Run 2 had the
hint (1 critical, 98 / 101, 102 / 107, 26 / 26); run 3 has both. Treat runs 2 and 3 as optimistic.
A fresh held-out set (a new decision under D-025) would measure the agent without that bias.

The full records are in `local/scratch/p6-02/` (`heldout-20261114T070344Z.json`,
`…T071234Z.json`, `…T072000Z.json`); the runner's output is in `local/scratch/p6-03/`.

## What still fails (held-out run 3)

| Case | What happens |
|---|---|
| H-030 | «قسط بعدی تسهیلات شخصی» — which installment is "the next" depends on the loan's paid installments; the model cannot know it from the labels, so the agent asks. Nothing is prepared. |
| H-036 | The model keeps «عنوان محل کار» as the address title (a text field). |
| H-050 | The fee account «از پس‌انداز» is still not read in this one case; the agent asks. |
| H-070 | A transfer to «شماره حساب 0000000000077» is still taken as interbank; the agent now asks for an IBAN instead of preparing it. |
| H-085 | The model keeps «کارمنده» as the job (a text field). |

## Production notes

- The record-based choice lists (vehicle plates and owners, Sayad cheques, addresses, branches) are
  written from the mock fixtures. In production they must come from the bank's lookup APIs, like
  the other lookups.
- The allowed keys were read from the replica's own tags. For the real app they must be read the
  same way (from the app's fields), not invented.
