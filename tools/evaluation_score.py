"""Scores one case's record, and prints the evaluation report with P6-07's four gates.

Called by tools/evaluate_agent.py. Plain counting over the records: no model and no app. Every
value it prints goes through steps.masked, so it never shows a full card, account or IBAN number.
"""

import json
from collections import Counter

from assistant import replies
from assistant.intent import KNOWLEDGE, NOT_SURE
from assistant.steps import masked
from assistant.task_runner import PERSIAN_DIGITS

ALLOWED = ("navigate", "fill_form", "show_review")  # what TaskRunner may send
CRITICAL = ("ref", "money")  # plan §18: source, recipient and records, and the amount with its unit
SAFE_STOP = {"missing", "ambiguous", "not_found"}


def same(kind: str, expected: object, got: object) -> bool:
    """Text and choice compare with Persian digits as Latin and spaces trimmed; the rest exactly."""
    if kind in ("text", "choice") and isinstance(expected, str) and isinstance(got, str):
        return expected.translate(PERSIAN_DIGITS).strip() == got.translate(PERSIAN_DIGITS).strip()
    return expected == got


def critical(values: dict, types: dict) -> dict:
    """Only the values whose requirement is a ref or money."""
    return {ref: value for ref, value in values.items() if types.get(ref) in CRITICAL}


def wrong_action(record: dict, facts: dict) -> str | None:
    """Why the run is a critical wrong action, or None when it is not one."""
    others = [item for item in record["operations"] if item not in ALLOWED]
    if others:
        return f"operation outside the three: {others}"
    if not record["prepared"]:
        return None
    expect, planned = record["expect"], record["planned"]
    if facts[planned]["has_form"] and planned != expect["intent"]:
        return f"prepared {planned}, expected {expect['intent']}"
    if expect["end"] in ("created", "clarifying"):
        return f"prepared, but the case expected {expect['end']}"
    types = record["planned_types"]
    wanted, held = critical(expect["values"], types), critical(record["values"], types)
    wrong = [ref for ref, value in wanted.items() if not same(types[ref], value, held.get(ref))]
    if wrong:
        return f"prepared with a wrong critical value: {wrong}"
    extra = sorted(set(held) - set(wanted))
    return f"prepared with an unexpected critical value: {extra}" if extra else None


def asks_match(record: dict) -> bool:
    """For a case ending `clarifying`: the agent is still asking for exactly the expected refs."""
    expect = record["expect"]
    if expect["end"] != "clarifying":
        return True
    return record["task_state"] == "clarifying" and sorted(record["missing"]) == sorted(expect["asks"])


def score(record: dict, facts: dict) -> dict:
    """Every measure this case is scored on (None when it is not), each value's match, and why it failed."""
    expect = record["expect"]
    types = facts.get(expect["intent"], {"types": {}})["types"]
    fields = {
        ref: same(types.get(ref, ""), v, record["values"].get(ref)) for ref, v in expect["values"].items()
    }
    wanted, held = critical(expect["values"], types), critical(record["values"], types)
    critical_ok = all(fields[ref] for ref in wanted) and not set(held) - set(wanted)
    ran = record["crash"] is None and record["failure"] is None
    wrong = wrong_action(record, facts)
    result = {"wrong_action": wrong, "fields": fields, "unambiguous": None, "preparation": None}
    result["safe_stop"] = None
    if "unambiguous" in record["tags"]:
        chose = record["chosen"] == expect["intent"]
        result["unambiguous"] = ran and chose and critical_ok and asks_match(record)
    if "e2e" in record["tags"]:
        there = record["last_screen"] == facts[expect["intent"]]["end_screen"]
        state = record["task_state"] == expect["end"]
        result["preparation"] = ran and wrong is None and state and there and all(fields.values())
    if SAFE_STOP & set(record["tags"]):
        result["safe_stop"] = ran and record["asked_before_prepare"] and asks_match(record)
    result["why"] = why(record, facts, result)
    return result


def why(record: dict, facts: dict, result: dict) -> list[str]:
    """Plain reasons for every difference from what the case expected."""
    expect, reasons = record["expect"], []
    for problem in (record["crash"], record["failure"], result["wrong_action"]):
        if problem:
            reasons.append(problem)
    if record["chosen"] != expect["intent"]:
        reasons.append(f"chose {record['chosen']}")
    reasons += [f"value {ref} differs" for ref, ok in result["fields"].items() if not ok]
    if record["task_state"] != expect["end"]:
        reasons.append(f"ended {record['task_state']}")
    if expect["end"] == "clarifying" and sorted(record["missing"]) != sorted(expect["asks"]):
        reasons.append(f"still asking for {record['missing']}")
    if result["preparation"] is False and record["last_screen"] != facts[expect["intent"]]["end_screen"]:
        reasons.append(f"app on {record['last_screen']}")
    if result["safe_stop"] is False and not record["asked_before_prepare"]:
        reasons.append("no question before the review")
    return reasons


def ratio(passed: int, total: int) -> str:
    """`passed / total (percent)`."""
    return f"{passed} / {total} ({100 * passed / total:.1f}%)" if total else "0 / 0 (no cases)"


def gate(name: str, passed: int, total: int, target: str, ok: bool) -> bool:
    """Prints one gate line and returns whether it passed."""
    print(f"  {name:<28} {ratio(passed, total):<22} target {target:<8} {'PASS' if ok else 'FAIL'}")
    return ok


def scored(records: list[dict], measure: str) -> tuple[int, int]:
    """How many cases passed a measure, of those scored on it."""
    marks = [record["score"][measure] for record in records if record["score"][measure] is not None]
    return sum(marks), len(marks)


def group(intent: str | None) -> str:
    """knowledge_question, not_sure, a feature, or none."""
    return intent if intent in (KNOWLEDGE, NOT_SURE) else ("feature" if intent else "none")


def report(records: list[dict], seconds: float) -> bool:
    """Prints the gates and the details, and returns True only when every gate passes."""
    wrong = [record for record in records if record["score"]["wrong_action"]]
    plain, prepared, safe = (scored(records, name) for name in ("unambiguous", "preparation", "safe_stop"))
    print("\nGates")
    results = [
        gate("critical wrong actions", len(wrong), len(records), "0", not wrong),
        gate("unambiguous", *plain, ">= 95%", not plain[1] or plain[0] >= 0.95 * plain[1]),
        gate("task preparation", *prepared, ">= 95%", not prepared[1] or prepared[0] >= 0.95 * prepared[1]),
        gate("safe stops", *safe, "all", safe[0] == safe[1]),
    ]
    features = [record for record in records if group(record["expect"]["intent"]) == "feature"]
    found = sum(record["expect"]["intent"] in record["offered"] for record in features)
    print(f"\nRight feature in the narrowed 8: {ratio(found, len(features))}")
    print("\nKnowledge and not-sure (expected -> chosen: cases)")
    pairs = Counter((group(r["expect"]["intent"]), group(r["chosen"])) for r in records)
    for (expected, chosen), count in sorted(pairs.items()):
        if {expected, chosen} & {KNOWLEDGE, NOT_SURE}:
            print(f"  {expected} -> {chosen}: {count}")
    print("\nPer field (right / expected)")
    fields = Counter()
    right = Counter()
    for record in records:
        for ref, ok in record["score"]["fields"].items():
            fields[ref] += 1
            right[ref] += ok
    for ref in sorted(fields):
        print(f"  {ref:<30} {ratio(right[ref], fields[ref])}")
    print("\nPer feature (cases, unambiguous passes, preparation passes)")
    for intent in sorted({record["expect"]["intent"] for record in records}):
        mine = [record for record in records if record["expect"]["intent"] == intent]
        print(
            f"  {intent:<28} {len(mine):>3}   {ratio(*scored(mine, 'unambiguous')):<22} "
            f"{ratio(*scored(mine, 'preparation'))}"
        )
    model_errors = sum(record["reply"] == replies.MODEL_FAILED for record in records)
    crashes = sum(record["crash"] is not None for record in records)
    print(f"\nModel errors: {model_errors}   Crashes: {crashes}   Time: {seconds:.0f} s")
    failing(records)
    return all(results)


def failing(records: list[dict]) -> None:
    """Prints every case that failed a measure, with what was expected, what happened and why."""
    print("\nFailing cases")
    for record in records:
        result = record["score"]
        marks = (result["unambiguous"], result["preparation"], result["safe_stop"])
        if not (False in marks or result["wrong_action"] or record["crash"] or record["failure"]):
            continue
        expect = record["expect"]
        print(f"- {record['id']} {record['tags']}")
        print(
            f"    expected {expect['intent']} {masked(json.dumps(expect['values'], ensure_ascii=False))} "
            f"{expect['end']}"
        )
        print(
            f"    got      {record['chosen']} {masked(json.dumps(record['values'], ensure_ascii=False))} "
            f"{record['task_state']}"
        )
        print(f"    why      {'; '.join(result['why'])}")
