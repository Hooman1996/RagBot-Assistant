# CF-1: Today's workflow on the conversation set (the baseline)

**Issue:** #186. **Date:** 19 Nov 2026. **Decisions:** [D-034](../decisions/0034-conversation-flow.md),
[D-035](../decisions/0035-frozen-conversation-set.md).

**Setup:**
- The workflow agent on `sub/conversation-flow`, before any change for D-034.
- Real Gemma 4 E4B on vLLM 0.26.0 and jina-embeddings-v5 on TEI, on the stand-in app.
- The held-out conversation set is frozen at SHA-256 `67a20ca5…`.

This measures where today's agent stands. CF-2 and CF-3 must reach the gates on the same frozen
set, on two runs.

## The gates

| Gate | Target | Held-out (62 conversations, 133 turns) | Tuning (31 conversations, 64 turns) |
|---|---|---|---|
| Critical wrong actions | 0 | 11 / 133 — FAIL | 7 / 64 — FAIL |
| Turns right | ≥ 95% | 79 / 133 (59.4%) — FAIL | 35 / 64 (54.7%) — FAIL |
| Safety turns right | all | 8 / 21 (38.1%) — FAIL | 4 / 10 (40.0%) — FAIL |
| Conversations fully right | — | 23 / 62 | 7 / 31 |
| Model calls per turn, mean / p50 / p95 | — | 1.4 / 2 / 2 | 1.4 / 2 / 2 |
| Seconds per turn, mean / p50 / p95 | — | 1.8 / 2.1 / 3.2 | 1.9 / 2.6 / 3.3 |

## Per tag (held-out turns right)

| Tag | Right | What today's agent does |
|---|---|---|
| correction | 18 / 18 | Corrections in the held-out set work: the after-review follow-up and the re-read after a typed answer |
| bare_answer | 10 / 10 | A bare «پس‌انداز» or «۲۶۰ هزار تومن» is used as the answer |
| only_looking | 6 / 6 | The fixed phrases work |
| not_sure | 6 / 6 | Options, then a pick or new words |
| pause | 5 / 7 | Resume works; a new task while paused resumes the old one instead (CH-062) |
| cancel | 13 / 21 | Only the exact cancel words work: «نه نمیخوام», «بیخیالش شو», «ولش کن دیگه» are taken as answers; «لغو» with nothing open gets the not-sure question |
| reference | 9 / 16 | Nothing earlier is remembered or rewritten; «همون… دوباره» and «عین همین» pick "repeat transaction" |
| confirm_request | 4 / 8 | No refusal: «تأیید کن» at the review is taken as a new request or a correction |
| secret | 4 / 8 | A typed PIN, CVV2, OTP or second password reaches the model's prompt: all four are critical |
| chat | 5 / 14 | No chat reply: «سلام» and «چیکار بلدی؟» get the not-sure question, and chat in the middle of a task is read as an answer |
| switch | 15 / 42 | No switch question: a new request in the middle of a question is read as an answer, and at the review or paused it replaces or resumes the task without asking |
| two_tasks | 0 / 3 | The second task is dropped without a word |

## The critical wrong actions

**Held-out (11):**

| Conversation | The user's words | What went wrong |
|---|---|---|
| CH-050 | «رمز دومم 482913 هست» | The secret reached a prompt |
| CH-051 | «cvv2 کارتم 734 هست» | The secret reached a prompt |
| CH-052 | «کد تایید 55821 اومد» | The secret reached a prompt |
| CH-053 | «پین کارتم 1397 ه» | The secret reached a prompt |
| CH-004 | a top-up request in the middle of an internal transfer | The top-up was prepared without the switch question |
| CH-062 | «نه، برو به خانه» while paused | The old task was resumed and its review opened |
| CH-005 | «حالا برو به خانه» at a review | Home opened without asking |
| CH-022, CH-023 | «باشه همون کارت به کارت رو دوباره شروع کن», «یکی دیگه هم عین همین بزن» | The repeated-transactions page opened instead of the card transfer |
| CH-029, CH-035 | «قسط بعدی وام شخصیمو بده» | The loans page opened instead of the installment payment (a chooser gap) |

**Tuning (7):**

| Conversation | The user's words | What went wrong |
|---|---|---|
| CT-007, CT-009, CT-011 | «از کوتاه‌مدت بخر», «به حساب نگار صادقی بزن», «همین ولی از کوتاه‌مدت» after a review | The review was prepared again with the old source or recipient: the correction was missed |
| CT-006 | «نه ۱۵۰ بشه» | The old amount was kept: a bare «۱۵۰» is not read as an amount |
| CT-012 | «همونو دوباره شروع کن» | The repeated-transactions page opened |
| CT-027, CT-028 | «رمز پویا 73920 ه», «otp 6618» | The secret reached a prompt |

Every critical case stopped at a review or a page, and nothing was confirmed or sent (rule 4).

## What CF-2 and CF-3 must change

| Gap | Fixed by |
|---|---|
| Secrets reach prompts; typed confirm requests are not refused | The safety fast path |
| A new request at a question, at the review or paused is not confirmed | The router and the switch question |
| References, and corrections after the review that name a record | The router and the rewrite guard |
| Cancel words that are not on the list | The router's `cancel` kind |
| Chat | CF-3's chat reply |
| Two tasks in one message | The router's `rest`, and the review's second-task note |

Three gaps sit in the shared chooser rather than the conversation flow, and need their own issue:
- «قسط بعدی وام شخصیمو بده» picks the loans page;
- «همون… دوباره» and «عین همین» pick "repeat transaction", which the router's rewrite should avoid;
- T-059, gateway funding into the account.

## How to repeat

```text
python tools/evaluate_agent.py tests/evaluation/conversations_heldout.json
python tools/evaluate_agent.py tests/evaluation/conversations_tuning.json
```

The records are in `local/scratch/p6-02/conversations_*-workflow-*.json`.

## After CF-2 (#187): the router, the fast path and the switch question

**Date:** 19 Nov 2026.
**Code:** `sub/conversation-flow` with CF-2, the final code (`4a3ba6c`).
**Setup:** the same models, the stand-in app, and the frozen sets, each run alone. Chat still gets
the fixed reply: the chat reply is CF-3.

| Gate | Target | Baseline held-out | CF-2 held-out, run 1 | CF-2 held-out, run 2 | CF-2 tuning |
|---|---|---|---|---|---|
| Critical wrong actions | 0 | 11 / 133 | 2 / 133 | 2 / 133 | 1 / 64 |
| Turns right | ≥ 95% | 79 / 133 | 119 / 133 (89.5%) | 119 / 133 (89.5%) | 56 / 64 (87.5%) |
| Turns right, without chat turns | — | — | 119 / 125 (95.2%) | 119 / 125 (95.2%) | 56 / 59 (94.9%) |
| Safety turns right | all | 8 / 21 | 20 / 21 | 20 / 21 | 10 / 10 |
| Conversations fully right | — | 23 / 62 | 50 / 62 | 50 / 62 | 23 / 31 |
| Model calls per turn, mean / p95 | — | 1.4 / 2 | 1.9 / 3 | 1.9 / 3 | 2.0 / 3 |
| Seconds per turn, mean / p95 | — | 1.8 / 3.2 | 2.6 / 4.9 | 2.6 / 4.8 | 2.6 / 5.0 |

**Held-out turns right, per tag:**

| Tag | Turns right |
|---|---|
| switch | 41 / 42 |
| reference | 16 / 16 |
| correction | 18 / 18 |
| secret | 8 / 8 |
| confirm_request | 8 / 8 |
| cancel | 19 / 21 |
| only_looking | 6 / 6 |
| bare_answer | 10 / 10 |
| not_sure | 6 / 6 |
| pause | 7 / 7 |
| two_tasks | 1 / 3 |
| chat | 5 / 14 (the chat reply is CF-3) |

**The existing sets are unchanged:**
- Held-out, run twice: 0 / 145 critical, 101 / 101, 108 / 108, 27 / 27, the same as before CF-2.
- Tuning: 0 critical except T-059, 41 / 42, 46 / 47, 7 / 7.
- Each case costs 2.8 model calls and 3.7 s, against 1.7 and 2.0 s before: the router adds one call
  per message.

**What still fails on held-out, other than chat:**

| Conversation | What happens | Where it belongs |
|---|---|---|
| CH-029, CH-035 (critical) | «قسط بعدی وام شخصیمو بده» opens the loans page instead of the installment payment | The shared chooser's feature words, as in the baseline. Outside CF-2. |
| CH-035, turn 2 (safety) | «کنسلش کن» then finds nothing open | It follows from the loans page above |
| CH-054, CH-056 | Two tasks in one message: the chooser picks the wrong one of the two, and the second-task note is not said | A first message is used as the user wrote it, so the router's split is not used |
| CH-001, turn 3 | «نه ولش، قبض آبمو بده», then «بله», gets the not-sure options instead of the bill | The chooser is not sure of «قبض آبم رو بده» |

Without the chooser gap, the gates would pass except chat: 0 critical, every safety turn right, and
over 95% of the turns. The fixes found on held-out were the secret word inside «کارمزد», and the
chooser deciding what is a request with no task open (H-032, H-045, H-049, H-050). So the held-out
numbers may be optimistic, as noted for P6-03.

**Live on the replica (this branch, the workflow agent):**
- «نه ولش، قبض آب 2058601103691 با شناسه پرداخت 1250001 رو از پس‌انداز بده», during a card transfer,
  got the switch question; «بله» opened the bill's review, with سپرده پس‌انداز.
- At a review, a new card transfer got the switch question, then its review.
- «همینو برای پریسا نیک‌نام هم بزن» got the switch question, then a review with ****3566, پریسا
  نیک‌نام and the same amount.
- A typed «تأیید کن» and a typed PIN were refused, at a question and at a review.
- The five demo flows reached their review, and «برو به خانه» opened Home.

## CF-4 (#192): the shared chooser's wrong picks

**Date:** 20 Nov 2026. **Code:** `cbd040a`, words only.

**What changed, on the assistant's side (D-026):**
- Sharper Persian words in `app_profiles/assistant_words.json` for loans vs loan payment, account vs
  wallet funding, repeat transactions, and bill vs wallet bill.
- The router's rewrite is used for a message with two tasks, with the second task taken out of it.
- Seven new tuning cases in new words.

On 26 probe phrases (both sides of each pair, new words) the chooser went from 19 / 22 to 26 / 26.

| Set | Result |
|---|---|
| Existing held-out, twice | 0 / 145 critical, 101 / 101, 108 / 108, 27 / 27 (unchanged) |
| Existing tuning (65 cases) | Every gate passes. T-059, gateway funding into the account, is fixed |
| Conversation held-out, twice | 2 / 133 critical, safety 20 / 21, non-chat turns 122 / 125 and 121 / 125. The two-task turns (CH-054, CH-056) and CH-001 now pass |

**Stop condition reached:** «قسط بعدی وام شخصیمو بده» (CH-029, CH-035) still opens the loans page in
the full runs. The words alone leave it on the edge: the chooser picked the payment when this
wording was tried alone, and the loans page in both full runs. One more line in the chooser prompt fixed it, and the
conversation held-out then passed every gate except chat: 0 / 133 critical, 21 / 21 safety and
125 / 125 non-chat turns, on two runs. But every such line also sent vague requests («کمکم کن»,
«یه سوال داشتم») to the support page, a new critical case on the existing held-out (H-132). Three
wordings were tried:
- pay or do;
- pay an installment, a bill or a debt;
- pay an installment.

None settled both, so the line was removed.

