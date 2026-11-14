# P6-02 baseline: the agent on the frozen evaluation set

**Issue:** #138. **Date:** 14 Nov 2026. **Agent:** `main` at `b5396df` (AG-01, #136), unchanged.
**Set:** [the evaluation set](../bridge/evaluation.md), held-out frozen by
[D-025](../decisions/0025-frozen-evaluation-set.md). This report measures; it recommends nothing.
The owner decides what to change.

## Step 0: the ground

| Check | Found |
|---|---|
| Revision | `main` at `b5396df`; `python -m pytest` 252 passed, 0 skipped |
| Model | vLLM `GET /v1/models`: `/app/model` (= `ASSISTANT_LLM_MODEL`), mounted from the Gemma 4 E4B-it weights (read-only `docker inspect`). `max_model_len` is now **6,144** (it was 4,000 on 9 Nov; the container was restarted on 11 Nov with a larger context, not by this project) |
| vLLM version | `GET /version`: 0.26.0 |
| TEI | `GET /info`: jina-embeddings-v5-text-small-retrieval, version 1.9.3, `max_client_batch_size` 50 |
| `main_debug.py` | The built-in example reaches review `S-176` with 2,500,000 rials, `ready_for_user` |
| Requirements | Every feature planned from Home (`plan_for(navigator, "S-026", feature)`); the expected values follow those requirements. 77 features, 21 without a form, five forms with nothing to ask |
| Choice keys | Read from the replica's Kotlin (`FormFiller` taps the tag `<ref>-<key>`), for example `schedule-frequency` `DAILY`/`WEEKLY`/`MONTHLY`, `wallet-op-operator` `HAMRAH_AVAL`/`IRANCELL`/`RIGHTEL`, `inquiry-kind` `credit-report`/`bounced-cheques`/`credit-rate` |

The claims handed over with the issue were checked again and hold: two shared titles
(«پرداخت قبض», «خرید شارژ»); 75 of 77 features reach their review with the demo values on the
stand-in, and `sayad_answer` and `sayad_transfer` reach it with the fixture cheques
`0000000000000311` and `0000000000000313`; one saved person, «رضا»; `matching("accounts", "حساب")`
returns two accounts and «حسابم» none; `matching("cards", "۰۰۱۸")` finds nothing.

## The held-out set

143 cases. Every one of the 77 features is an expected intent at least once; every feature with
a money requirement (17) has at least 3 cases, one of them `e2e`.

| Tag | Cases | | Feature group | Cases |
|---|---|---|---|---|
| `unambiguous` | 101 | | Transfers | 23 |
| `e2e` | 107 | | Payments and top-ups | 21 |
| `colloquial` | 21 | | Wallet | 24 |
| `missing` | 16 | | Cards and accounts | 11 |
| `wallet_vs_bank` | 9 | | Cheques | 14 |
| `knowledge` | 9 | | Vehicles | 11 |
| `not_sure` | 7 | | Loans, credit and rewards | 17 |
| `typed_number` | 6 | | Profile and app | 6 |
| `option` | 6 | | Knowledge questions | 9 |
| `ambiguous` | 6 | | Not sure | 7 |
| `correction` | 6 | | | |
| `not_found` | 4 | | | |

The held-out file was written before the tuning file (the issue asked for tuning first). Neither
had been run when both were written, and they share no sentence.

## Results

Both runs used the real Gemma 4 E4B-it and TEI services, on the stand-in app.

| Gate | Target | Held-out (baseline) | Tuning |
|---|---|---|---|
| Critical wrong actions | 0 | **9 / 143** — FAIL | 1 / 43 — FAIL |
| Unambiguous | ≥ 95% | **68 / 101 (67.3%)** — FAIL | 18 / 32 (56.2%) — FAIL |
| Task preparation | ≥ 95% | **51 / 107 (47.7%)** — FAIL | 12 / 34 (35.3%) — FAIL |
| Safe stops | all | **25 / 26 (96.2%)** — FAIL | 3 / 5 (60.0%) — FAIL |

| Also measured | Held-out | Tuning |
|---|---|---|
| Right feature among the narrowed 8 | 120 / 127 (94.5%) | 35 / 37 (94.6%) |
| Model chose the expected intent | 132 / 143 (92.3%) | — |
| Knowledge questions answered as such | 8 / 9 (one became `account_opening` and asked; nothing was prepared) | 3 / 3 |
| "Not sure" requests answered as such | 7 / 7 | 1 / 3 (two became knowledge questions) |
| Model errors | 0 | 0 |
| Crashes | 6 | 2 |
| Time | 281 s | 92 s |

The held-out set was run **once**. Every failure was reviewed, and none was a labelling mistake,
so there was no second run. The tuning set was run twice, with no label change. Between the two
runs the runner was changed to record the pending question as the reply and to keep the chosen
intent when the first turn crashes. The numbers above are the second run. The first run gave
1 / 43, 19 / 32, 13 / 34, 3 / 5 and 33 / 37 on the same cases: the served model is not fully
repeatable even at temperature 0.

The full records are in `local/scratch/p6-02/` (not committed):
`heldout-20261114T054916Z.json`, `tuning-20261114T054422Z.json` and `tuning-20261114T054127Z.json`.

### The critical wrong actions (held-out)

| Case | What happened |
|---|---|
| H-054 | «نمره منفی گواهینامه ... رو استعلام کن» prepared `licence`, not `negative_points` |
| H-070 | A transfer to a typed account number prepared `interbank_transfer`, not `internal_transfer` |
| H-075, H-077, H-104 | «۵۰۰ هزار تومان», «۲ میلیون تومنی» became 5,000,000,000 and 20,000,000,000 rials (a factor of 1,000) at the review |
| H-114 | «از درگاه به حسابم اضافه کنم» prepared `wallet_funding`, not `account_funding` |
| H-141, H-143 | «مبلغ رو ... کن» during the question was ignored: the review shows the first amount |
| H-142 | «نه از حساب نمایشی، از پس‌انداز» during the question was ignored: the review shows the first source |

The tuning run's one critical wrong action is the same factor of 1,000 («۵۰۰ هزار تومن» became
5,000,000,000 rials). The three card corrections (H-138 to H-140) never reached a review: the
source card was "not found" first (see below), so they stopped safely.

## Every held-out failure, by cause

60 cases failed at least one measure. Each is listed once, under the cause that stopped it first.

| Cause | Cases | Which | What it points to |
|---|---|---|---|
| Wrong pick | 7 | H-054, H-070, H-073, H-074, H-088, H-114, H-120 | In all 7 the expected feature was **not among the narrowed 8**. Narrowing: more Persian words per feature (the descriptions are English), or all 77 to the model (P6-03) |
| Shared title | 3 | H-028, H-078, H-079 | Bank bill and top-up taken as the wallet ones; both pairs share a title. More Persian words per feature (P6-03) |
| Choice key | 20 | H-027, H-029, H-030, H-034, H-035, H-039, H-040, H-047, H-048, H-049, H-050, H-051, H-052, H-053, H-058, H-085, H-086, H-087, H-110, H-112 | The model copies the user's words («ماهانه», «ایرانسل», «وام شخصی», «12 ب 345 ایران 67») where the app needs a key. Allowed keys for choice fields in the profile (P6-03) |
| Choice key sent as a row key (crash) | 6 | H-055, H-056, H-057, H-091, H-092, H-093 | A row selection («گزارش اعتباری», «قبض‌ها») is sent as a `navigate` argument and breaks the command contract; the stand-in stops with a crash, the replica would answer `invalid_value`. Allowed keys (P6-03), and a check of selection keys before sending |
| Correction ignored | 3 | H-141, H-142, H-143 | All three are critical wrong actions. The correction flow (P6-04) |
| Persian amount | 3 | H-075, H-077, H-104 | «N هزار تومان» read a factor of 1,000 too large; all three are critical wrong actions. The amount extraction and its check (P6-03) |
| Record not found as written | 13 | H-061, H-062, H-063, H-066, H-068, H-069, H-072, H-076, H-082, H-097, H-105, H-108, H-111 | The model copies «کارت 0018», «حساب رضا», «کیف پول رضا», «خیریه آموزش», and the lookup only finds a record whose label contains the text as written; Persian digits («۰۰۲۶»), a missing ZWNJ («پس انداز»), Arabic ي/ك and the label's «خیریهٔ» also miss. H-105 also read «صد هزار تومن» a factor of 1,000 too large. More Persian words per record, or a looser match (P6-03, P6-04) |
| Number placeholder not restored | 4 | H-046, H-094, H-095, H-099 | The model writes `1` or «کیف [number 1]» instead of `[number 1]`; only a placeholder that is the whole value is put back. The same happens to the typed IBAN «IR[number 1]» (H-074, under wrong pick). The extract prompt and the placeholder handling (P6-03) |
| Other | 1 | H-036 | The model kept «عنوان محل کار» instead of «محل کار» |

The only safe-stop failure is H-066: «از کارت 0018 ... به علی» asked for the source card (not
found as written) instead of saying «علی» was not found. Nothing was prepared.

## Agent bugs and gaps found (recorded, not fixed)

1. **Persian text sent as a selection key.** `TaskRunner` sends a row selection the model wrote in
   Persian; it breaks `command.schema.json`. Nothing checks the key before sending (6 held-out,
   2 tuning).
2. **A correction during a question is ignored.** In `agent_nodes.extract`, a value already held
   wins over a new one (`found | state["values"]`), so the review shows the old amount or source
   (3 critical wrong actions).
3. **Amounts a factor of 1,000 too large.** «۵۰۰ هزار تومان» comes back as 500,000,000 toman.
   Code accepts any whole number (3 held-out and 1 tuning critical wrong actions).
4. **A record is found only by its label as written.** `FixtureLookups.matching` compares the text
   as written: no Persian digits, ZWNJ or Arabic-letter handling, and a word such as «کارت» or
   «حساب» in front of the name makes it miss (13 failures).
5. **A partial placeholder is not put back.** `slots.text` restores `[number N]` only as the whole
   value; «کارت [number 1]», «IR[number 1]» and a bare `1` are kept as written. A typed IBAN cannot
   reach the review today, and an unsaved IBAN would be refused anyway, because `resolve` lets only
   digits through for a contact.
6. **Narrowing misses.** All 7 wrong picks had the expected feature outside the narrowed 8.

The runner also found that while a question waits, LangGraph's saved state still shows
`resolving` (the `clarify` node has not returned); the agent reports `clarifying` with the
question, and the runner reads that, as the chat box does.
