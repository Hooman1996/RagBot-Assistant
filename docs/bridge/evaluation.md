# The evaluation set

A fixed set of Persian requests, each with what the agent ([the agent](agent.md)) should end up
holding and doing, and one runner that scores the real agent on it. It measures; it changes
nothing. Built in #138; the freeze is [D-025](../decisions/0025-frozen-evaluation-set.md). The
first measurement is [the P6-02 baseline](../reports/p6-02-baseline.md).

| File | What it is |
|---|---|
| `tests/evaluation/heldout.json` | 143 held-out cases, frozen by their SHA-256. The gates are reported on these. |
| `tests/evaluation/tuning.json` | 43 tuning cases, for prompt and profile work. They may change freely. |
| `tools/evaluate_agent.py` | Runs a case file through the agent and writes every case's record. |
| `tools/evaluation_score.py` | Scores each record and prints the report. |
| `tests/evaluation/test_cases.py` | Checks both files' format, labels and counts, and the freeze. Runs in CI. |
| `tests/evaluation/test_evaluate.py` | Runs the runner on fake models. Runs in CI. |

## How to run it

```bash
conda activate assistant
set -a; source .env; set +a
python tools/evaluate_agent.py tests/evaluation/tuning.json
python tools/evaluate_agent.py tests/evaluation/heldout.json
```

It needs the Gemma model on vLLM and the TEI embeddings (the addresses in `.env`). The held-out
set takes about 5 minutes. Exit 0: every gate passes. Exit 1: a gate fails, which is an honest
result. Exit 2: it could not run (the embeddings were not reachable). The full record of every
case is written to `local/scratch/p6-02/<file name>-<UTC time>.json`.

## How it runs a case

The runner builds the same agent as `main_debug.py`: the profile, the navigator, the lookups, the
real `Chat` and `Embeddings`, and one `FeatureIndex` shared by every case, so the 77 features are
embedded once. Each case then starts fresh: a new `CommandQueue`, a new stand-in app on Home, new
`Tools`, a new graph, and the case ID as the conversation's `thread_id`.

The first turn starts a request. Each later turn answers the agent's pending question. A turn
written `option:<ref>` picks that option, only if the pending question offers it; otherwise the
case fails with "option not offered". If the agent answers before the turns run out, the runner
stops sending. While a question waits, the task state is the one the agent reports with the
question (`clarifying`), as the chat box shows it. Any exception is recorded as a crash, and the
next case runs. A crash is never a pass.

**What the stand-in cannot catch.** The runner uses the stand-in app, not the replica. The
stand-in follows the profile, and the profile has no allowed keys for choice fields, so a choice
key the real app would refuse may still reach the stand-in's review. The comparison of every value
with the case's expected value catches that. The stand-in does check every command against the
contract, so a value that breaks it is a crash. P6-07 repeats the gate runs on the replica.

## The case format

Both files are a JSON list, UTF-8, one case per object:

```json
{
  "id": "H-061",
  "tags": ["unambiguous", "e2e"],
  "turns": ["از کارت 0018 مبلغ ۵۰۰ هزار تومان برای رضا کارت به کارت کن"],
  "expect": {
    "intent": "card_transfer",
    "values": {
      "card-transfer-source": "card-1",
      "card-transfer-destination": "reza",
      "card-transfer-amount": {"value": 5000000, "unit": "IRR"}
    },
    "end": "ready_for_user",
    "asks": []
  }
}
```

- `id`: `H-001`… held out, `T-001`… tuning.
- `turns`: the user's messages in order.
- `expect.intent`: a profile intent, `knowledge_question` or `not_sure`.
- `expect.values`: what the agent should hold at the end, by requirement ref, as the agent keeps it
  after `resolve`: a lookup value is the record's key (`card-1`, `reza`, `account-2`); a typed card,
  account or wallet number is its Latin digits, and an IBAN its text; money is
  `{"value": <int>, "unit": "IRR"}` in rials; a text value is the exact text the app takes; a
  choice value is the app's key (from `assistant/demo_values.py` or the replica's Kotlin). A case
  that ends while the agent still asks lists only values already in that form: a named record is
  resolved only once nothing else is missing.
- `expect.end`: the final task state: `ready_for_user` for a prepared review, `completed` for a
  feature without a form, `created` for a knowledge question or "not sure", `clarifying` when the
  turns run out while the agent must still be asking, `failed` only where a visible stop is right.
- `expect.asks`: the refs the agent must still be asking for when `end` is `clarifying`.

## The tags

| Tag | Meaning |
|---|---|
| `unambiguous` | One sentence, names exactly one feature, every value it gives is clear. Exactly one turn. |
| `e2e` | Scored for task preparation: must end `ready_for_user` (with a value for every requirement) or `completed`. |
| `missing` | A critical value is not given; the agent must ask before any review. |
| `ambiguous` | A named record matches more than one; the agent must offer options. |
| `not_found` | A named record does not exist (for example «به علی»); the agent must say so and ask, never prepare. |
| `typed_number` | The recipient is a typed card, account or IBAN number (shown to the model only as `****` and its last 4 digits). |
| `option` | A later turn picks an option. |
| `correction` | A later turn changes a value already given. |
| `knowledge` | A banking question, not a task. |
| `not_sure` | Fits no feature, or is too vague to pick one. |
| `wallet_vs_bank` | Bill or top-up wording that must separate the bank feature from the wallet one. |
| `colloquial` | Spoken forms, typos, Arabic ي/ك, missing or extra ZWNJ, Persian or Latin digits, numbers in words. |

## The labelling rules

1. **Critical values** are the requirements of type `ref` (sources, recipients, records) and
   `money`: plan §18's source, recipient and amount with its unit. `text` and `choice` values are
   scored too, but they are not critical.
2. **Bill and top-up.** A sentence that does not mention the wallet (کیف, کیف پول) means
   `bill_payment` or `mobile_top_up`. A sentence that does mention it means `wallet_bill` or
   `wallet_mobile_top_up`.
3. **Money.** «تومان», «تومن» and «ت» mean toman; «ریال» means rial; no unit means rial, as the
   extract prompt says. Expected amounts are always whole rials.
4. **Knowledge** is a banking question that asks for information, not an action («سقف کارت به کارت
   روزانه چقدره؟»). **Not sure** is a request no feature serves, or one too vague to pick a feature
   («یه کاری با کارتم دارم»).
5. **Gate scope (owner, 14 Nov 2026).** The preparation gate counts the `e2e` cases of all 77
   features, including features with choice fields, which fail until allowed keys exist (P6-03).
6. **Nothing real.** Every value, name and number is synthetic and comes from
   `mock_services/fixtures/` (rule 2).
7. **Written by hand, in natural Persian,** with varied wording; a feature's title is never the
   whole sentence. Tuning and held-out are written separately, and no held-out sentence appears,
   even normalized, in the tuning file.

## The four measures

Each case is scored only on the measures its tags name. Text and choice values are compared after
turning Persian digits into Latin and trimming spaces; refs and money are compared exactly; nothing
else is normalized.

- **Critical wrong action** (every case): any operation other than `navigate`, `fill_form` or
  `show_review`; or `prepare` ran and any of these holds: the prepared feature has a form and is
  not the expected intent; a critical value differs from the expected one, or a critical value is
  held that the case did not expect; the case expected `created` or `clarifying`. **Gate: 0.**
- **Unambiguous pass** (`unambiguous` cases): the first chosen intent is the expected one, every
  expected critical value matches, no unexpected critical value is held, and for an `end` of
  `clarifying` the agent is still asking for exactly `asks`. **Gate: at least 95%.**
- **Preparation pass** (`e2e` cases): no crash, the final task state is `end`, the stand-in is on
  the feature's `review` (or its `destination` when it has no form), every expected value of every
  type matches, and there is no critical wrong action. **Gate: at least 95%.**
- **Safe stop pass** (`missing`, `ambiguous` and `not_found` cases): at least one question came
  before any `prepare`, and a case whose turns run out ends `clarifying` with the agent asking for
  exactly `asks`. **Gate: all.**

The report also gives the right feature among the narrowed 8, the knowledge and not-sure confusion
(expected against chosen), the accuracy of every field that appears in the expected values, a
table per feature, model errors (the reply is the "model could not be reached" message), crashes,
the total time, and every failing case with what was expected, what happened and why.

## Changing the held-out set

The held-out file is frozen ([D-025](../decisions/0025-frozen-evaluation-set.md)): the case test
compares its SHA-256 with the one in the decision. To change a held-out case, a gate or a
definition: record a new decision, edit the file, write the new hash, and run the held-out set
again, reporting both the old and the new numbers. The tuning file needs none of this.
