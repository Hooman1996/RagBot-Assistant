# P6-05b: the assistant offers and accepts only what the app accepts

**Issue:** #148. **Date:** 15 Nov 2026. **Decision:** [D-027](../decisions/0027-heldout-follows-the-app.md).
**Measured with:** [the evaluation set](../bridge/evaluation.md), held-out file at the D-027 hash.
Same models as P6-03: Gemma 4 E4B-it on vLLM and jina-embeddings-v5 on TEI.

The evaluation runs on the stand-in app, which accepts any value. To find what the real app
accepts, every record and choice key of every field the assistant can fill was tried on the
replica through the real executor: each feature run to its review with that one value and demo
values for the rest (204 tries, plus the four features that share a review page, run by name).

## What the app refused, and the fix

| Found on the replica | Fix (assistant side unless marked) | Where |
|---|---|---|
| Card transfer: the source picker lists only active debit cards (credit, bon, wallet and inactive cards fail `control_not_shown`) | Offer and accept only active debit cards; a named one the app doesn't list («کارت 0042») is refused, and the listed cards are offered | `assistant_words.json` `offer`; `replies.offered`; `agent_nodes.resolve` |
| Card-number conversion: debit cards only | Same rule (debit) | `offer` |
| Wallet operations: the inactive wallet isn't listed | Active wallets only | `offer` |
| Insurance installments: paid ones and ones being verified are disabled; a policy with nothing due shows no installments | Only the installments the app lets the user pick; a policy with none isn't offered | `assistant_words.json` `installments`; `FixtureLookups.installments`; `agent_nodes.payable` |
| Loan installments: paid ones are disabled, and they are ticked in order from the first overdue, else the first unpaid; a paid-off loan shows none | Only the next installment, set by code; a paid-off loan isn't offered | as above |
| Corporate loans and joint accounts sit on a second tab the executor doesn't open | Not offered; the user picks those in the app | `offer` |
| Wallet top-up: the amount is one of the operator's charges (Rightel has no 100,000) | Another amount is asked again, naming the charges | `assistant_words.json` `charges`; `FixtureLookups.charges`; `agent_nodes.charge` |
| Recurring transfer: weekly or monthly opens a dialog asking the day | The day is asked, with that frequency's days; the executor picks it in the dialog the frequency opened (**android:** `FormFiller`, owner-approved) | `assistant_words.json` `when`; `navigator.plan_feature`; `agent_nodes.check` |
| Card request: post delivery goes by the address page | The agent plans through the address page once post is chosen, and asks the address | `when` (`via`); `navigator.plan_feature` |

Not a problem: the cheque menu's and paid-inquiry menu's keys failed only when one feature's key
was tried on another feature's route; the assistant sets those by code from the feature.

After the fixes, every value the assistant can offer was checked against the sweep: 160 were
already accepted, and the 16 new combinations (installments with their policy or loan, wallet
top-up charges per operator) were tried on the replica and all reached the review.

## Tried live on the replica with the real models

| Said | What happened |
|---|---|
| «يك ميليون ريال از كارت 0042 به رضا كارت به كارت كن», then `****0026` | «0042» refused, `****0018`/`****0026` offered; review S-176 |
| «یه انتقال خودکار هفتگی … بساز», then شنبه | The day asked; the form filled with «هفتگی» and «شنبه» |
| «درخواست کارت جدید … با پست بفرستید», then خانه | The address asked; review S-128 |
| «قسط اول بیمه‌نامه DEMO-POLICY-0001 …», then قسط سوم | «قسط اول» refused, 3 and 4 offered; review S-404 |
| «قسط تسهیلات دارای بدهی …» | The first overdue installment set by code; review S-331 |
| «شارژ رایتل ۱۰ هزار تومنی …», then «۲۰ هزار تومن» | The amount asked again with Rightel's charges; review S-103 |

## Results

A fresh held-out run on the final code (one run; the model is not repeatable at temperature 0):

| Gate | Target | P6-03 run 3 (old cases) | This run (D-027 cases) |
|---|---|---|---|
| Critical wrong actions | 0 | 0 / 143 | **0 / 143 — PASS** |
| Unambiguous | ≥ 95% | 98 / 101 (97.0%) | **97 / 99 (98.0%) — PASS** |
| Task preparation | ≥ 95% | 102 / 107 (95.3%) | **102 / 106 (96.2%) — PASS** |
| Safe stops | all | 26 / 26 | **27 / 27 — PASS** |
| Right feature among the narrowed 8 | — | 127 / 127 | 127 / 127 |
| Model errors, crashes | — | 0, 0 | 0, 0 |

H-068 now asks for the source card, and H-027 asks for the day and fills it: both pass. The four
failing cases change no critical value: a title copied with an extra word (H-036), the fee account
not read (H-050), an account number taken for an interbank transfer, which stopped to ask (H-070),
and a job word with a spoken ending (H-085).

## Known limits

- Amount limits other than the wallet charge list (minimums, maximums, multiples) were not swept.
- The graph page's Run doesn't follow `when` (it is a development tool); the agent and its run do.
- After a blocked fill the executor leaves the app's picker open, and the next command stops on
  `close_dialog`. The bank builds the executor; recorded in #148.
- The eligible records come from the mock fixtures. In production the adapter's lookups return
  what the bank's server lists for each picker.
