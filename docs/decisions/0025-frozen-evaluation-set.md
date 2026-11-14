# D-025 — The frozen Persian evaluation set

**Source:** Owner, 14 Nov 2026 (issue #138). Records plan §18's "frozen evaluation set and
thresholds before P6 tuning".

## Decision

1. **The held-out set is frozen.** `tests/evaluation/heldout.json` (143 cases) is frozen at
   SHA-256 `fdf15cd250eebcc390737faea806023a442541434e850bcb5d3a3b64280a3d6f` on 14 Nov 2026.
   `tests/evaluation/test_cases.py` reads the hash from this file and fails if the file's bytes
   change.
2. **The tuning set may change freely.** `tests/evaluation/tuning.json` (43 cases) is for prompt
   and profile work. It never shares a sentence with the held-out set; the case test checks this.
3. **The gates, scored by `tools/evaluate_agent.py`** (every definition is in
   [the evaluation set](../bridge/evaluation.md#the-four-measures)):

   | Gate | Target |
   |---|---|
   | Critical wrong actions, over every case | 0 |
   | Unambiguous (intent plus critical values), over the `unambiguous` cases | at least 95% |
   | Task preparation, over the `e2e` cases of all 77 features | at least 95% |
   | Safe stops, over the `missing`, `ambiguous` and `not_found` cases | all |

   Critical values are the requirements of type `ref` (sources, recipients, records) and
   `money`. The preparation gate counts every feature, including those with choice fields
   (owner, 14 Nov 2026).
4. **Any change to a held-out case, a gate or a definition needs a new decision and a fresh
   reported run** of the held-out set, with the new hash written here or in the new decision.

## Why

Plan §18 asks for at least 100 held-out cases, frozen before tuning, with separate tuning
examples, so a reported number cannot be raised by editing the cases it is measured on.

## Unchanged

The agent, the profile and the contracts. This decision measures; it changes nothing the agent
does. The baseline run is in [the P6-02 report](../reports/p6-02-baseline.md).
