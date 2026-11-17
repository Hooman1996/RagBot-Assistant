# RA-5: The ReAct agent and the workflow agent, side by side

**Issue:** #171. **Date:** 17 Nov 2026. **Decision:** [D-032](../decisions/0032-react-agent-beside-the-workflow.md).

**Setup:**
- Measured with [the evaluation set](../bridge/evaluation.md). The held-out file is unchanged: SHA-256 `a052b637…`, the [D-031](../decisions/0031-real-looking-mock-data.md) hash.
- Models: Gemma 4 E4B-it on vLLM 0.26.0 (6,144 tokens) and jina-embeddings-v5 on TEI 1.9.3, on the stand-in app.
- Both agents ran from this branch's code, committed as `e28daf8` before the runs. Each run was alone, with nothing else using the model.

**Result:** the ReAct agent fails the critical-wrong-action gate on held-out, with 3 / 145 on both runs. All three are a wrong feature, chosen by the model, that no code guard catches. This is RA-5's stop condition, so it is recorded here and tuning stops. **The workflow stays the demo default.**

## The gates

| Gate | Target | Workflow held-out 1 | Workflow held-out 2 | ReAct held-out 1 | ReAct held-out 2 |
|---|---|---|---|---|---|
| Critical wrong actions | 0 | **0 / 145** | **0 / 145** | 3 / 145 — FAIL | 3 / 145 — FAIL |
| Unambiguous | ≥ 95% | 101 / 101 | 101 / 101 | 99 / 101 (98.0%) | 99 / 101 (98.0%) |
| Task preparation | ≥ 95% | 108 / 108 | 108 / 108 | 106 / 108 (98.1%) | 105 / 108 (97.2%) |
| Safe stops | all | 27 / 27 | 27 / 27 | 27 / 27 | 27 / 27 |
| Right feature in the narrowed 8 | — | 129 / 129 | 129 / 129 | 129 / 129 | 129 / 129 |
| Model errors, crashes | — | 0, 0 | 0, 0 | 0, 0 | 0, 0 |

Tuning set, one run per agent. Both pass every gate:

| Gate | Workflow | ReAct (before tuning) | ReAct (after tuning) |
|---|---|---|---|
| Critical wrong actions | 0 / 55 | 0 / 55 | 0 / 55 |
| Unambiguous | 41 / 41 | 38 / 41 | 41 / 41 |
| Task preparation | 45 / 45 | 39 / 45 | 45 / 45 |
| Safe stops | 7 / 7 | 7 / 7 | 7 / 7 |

## What each case costs

Each figure is per case: every turn of the case, model calls and the app's commands included, on the stand-in app.

| | Workflow held-out | ReAct held-out | Workflow tuning | ReAct tuning |
|---|---|---|---|---|
| Model calls, mean / p50 / p95 | 1.7 / 2 / 2 | 4.3 / 4 / 7 | 1.7 / 2 / 2 | 4.3 / 4 / 8 |
| Seconds, mean / p50 / p95 | 2.0 / 2.1 / 4.1 | 9.9 / 10.1 / 16.0 | 2.1 / 2.4 / 4.4 | 10.4 / 10.8 / 19.5 |
| Whole held-out run | 299 s | 1,435 s | 122 s | 575 s |

The ReAct agent costs about 2.5 times the model calls and about 5 times the time. On the replica, through the panel, the first question or the review came after 11 to 16 s, and an answer took 3 to 4 s (RA-3's live runs).

## The failing held-out cases

The same cases failed on both runs, except H-059 on run 2.

| Case | The user's words | Expected | ReAct did | Critical | Why no guard catches it |
|---|---|---|---|---|---|
| H-114 | «میخوام یه دو میلیون ریال از درگاه به حسابم اضافه کنم» | account funding, review | wallet funding, review | yes | The feature is the model's choice; both features take the same amount. The workflow chose right. |
| H-135 | «سهام یه شرکت برام بخر» | not sure: ask which | the credit product's form, filled | yes | Nothing tells code that a request fits no feature. The workflow's chooser has a `not_sure` answer; the ReAct prompt has none. |
| H-137 | «یه چیزی درباره حسابم» | not sure: ask which | opened the accounts page | yes | The same. It opened a page and prepared nothing, but the gate counts any preparation of a case that expected a question. |
| H-089 | «یه مساعده … بریز به پس‌انداز» | wage advance | wallet withdrawal, asking | no | Wrong feature, stopped at a question. |
| H-059 (run 2) | «۱۰۰ امتیاز باشگاهمو به شماره … بده» | review | failed | no | The model gave the whole sentence as the points value. Code kept it as text, and the app refused it on the form (`fill_form` reported the field wrong), so the task failed without a review. |

None of the three critical cases confirmed, submitted or paid anything. Each stopped at a review or a page, and only the user confirms (rule 4). Under D-025, though, they are critical wrong actions, and the gate is 0.

## What was tuned, on the tuning set only

The first ReAct tuning run failed six cases. Each was traced, and the prompt, the tool text or the ReAct tools' own code was changed. The workflow and the held-out cases were not touched.

| Tuning case | Cause | Change |
|---|---|---|
| T-008 | The record guard did not see the user's masked IBAN («IR****6115») as the user's own, so the model looped on `find_records`. | `named_by_user` reads `****1234` inside a word. An identical repeated call now returns an error instead of running again. |
| T-049 | The model searched features with only «وام شخصی», and the right feature was missing. | `find_features` searches the user's own newest words first, as the workflow does, then up to 2 features from the model's words. |
| T-007, T-051 | Wrong feature: «انتقال داخلی» was taken as card to card, and «… نشون بده» as a payment. | The workflow's rules were added to the prompt: match the user's own words, and a thing to see gets the feature that shows it. |
| T-025 | The model copied the bill ID as its last four digits («3691»). | Four bare digits that end a long number the user wrote (and that the user did not write alone) are read as that number. The tool text says to copy `****1234` exactly. |
| T-047 | The model kept the label word («نشانی خیابان …»). | The tool text says to give a text without its field's label words. |

After these changes, the ReAct agent passed every tuning gate. The held-out runs above were made only after that, once each, and nothing was changed after seeing them.

## Recommendation

- **Keep `workflow` as the demo default.** It passes every gate on held-out and tuning, and answers in about 2 s.
- **Show the ReAct agent only with the switch** (`ASSISTANT_AGENT=react`), for the panel's «مراحل دستیار». It is a good way to show the model reasoning step by step, and every act still goes through the workflow's checks. It should not be the default until it passes the critical gate.
- **The likely fix is to give the ReAct agent the workflow's own feature choice.** For example, `find_features` could return the chooser's pick, `not_sure` included, and `feature_requirements` would refuse any other feature. That is a design change, not tuning, so it needs a new issue and the owner's approval. A fresh held-out run is then needed: the cases above are now known, so a fix aimed at them would make held-out numbers optimistic.

The owner picks the demo default from this report.

## How to repeat

```text
python tools/evaluate_agent.py tests/evaluation/heldout.json --agent react      # twice
python tools/evaluate_agent.py tests/evaluation/heldout.json --agent workflow   # twice
python tools/evaluate_agent.py tests/evaluation/tuning.json --agent react
python tools/evaluate_agent.py tests/evaluation/tuning.json --agent workflow
```

Each run writes `local/scratch/p6-02/<cases>-<agent>-<time>.json`. The records of these runs are kept in `local/scratch/p6-02/` and `local/scratch/react/`.
