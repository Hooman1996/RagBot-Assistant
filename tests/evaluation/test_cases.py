"""Checks both evaluation case files: their format, their labels, their counts and the freeze.

Runs in CI with no network. The held-out file is frozen: its SHA-256 must equal the one recorded
in decision D-025, so changing a held-out case needs a visible decision and a fresh run.
"""

import hashlib
import json
import re
from collections import Counter
from pathlib import Path

import pytest

from assistant.agent_nodes import plan_for
from assistant.intent import KNOWLEDGE, NOT_SURE
from assistant.lookups import FixtureLookups, source_of
from assistant.navigator import HOME, Navigator

FOLDER = Path("tests/evaluation")
DECISION = Path("docs/decisions/0025-frozen-evaluation-set.md")
PROFILE = json.loads(Path("app_profiles/replica.profile.json").read_text(encoding="utf-8"))
FEATURES = {item["intent"]: item for item in PROFILE["features"]}
NAVIGATOR = Navigator(PROFILE)
LOOKUPS = FixtureLookups()
TAGS = {
    "unambiguous",
    "e2e",
    "missing",
    "ambiguous",
    "not_found",
    "typed_number",
    "option",
    "correction",
    "knowledge",
    "not_sure",
    "wallet_vs_bank",
    "colloquial",
}
ENDS = {"ready_for_user", "completed", "created", "clarifying", "failed"}
OPTION = "option:"
IBAN = re.compile(r"IR\d{24}")
# The held-out correction cases, by what the later turn changes.
CORRECTED = {"amount": ["H-138", "H-141", "H-143"], "source": ["H-139", "H-142"], "recipient": ["H-140"]}
HELDOUT_AT_LEAST = {
    "unambiguous": 80,
    "e2e": 60,
    "missing": 10,
    "ambiguous": 6,
    "not_found": 4,
    "typed_number": 4,
    "option": 6,
    "correction": 6,
    "knowledge": 8,
    "not_sure": 6,
    "wallet_vs_bank": 6,
    "colloquial": 20,
}


def read(name: str) -> list[dict]:
    """One case file."""
    return json.loads((FOLDER / name).read_text(encoding="utf-8"))


def requirements(intent: str) -> dict:
    """The feature's requirements from Home, by ref: what the agent will ask for."""
    plan = plan_for(NAVIGATOR, HOME, FEATURES[intent])
    return {item.ref: item for item in plan.requirements}


def keys(lookup: str) -> list[str]:
    """Every record key the lookup offers."""
    return [item.key for item in LOOKUPS.candidates(lookup)]


def typed(lookup: str, value: object) -> bool:
    """A contacts value the user typed: a card, account or wallet number, or an IBAN."""
    return (
        source_of(lookup) == "contacts"
        and isinstance(value, str)
        and (value.isdigit() or IBAN.fullmatch(value) is not None)
    )


def money_ok(value: object) -> bool:
    """A whole number of rials, as the agent keeps it: an int and the unit, nothing else."""
    return (
        isinstance(value, dict)
        and set(value) == {"value", "unit"}
        and type(value["value"]) is int
        and (value["unit"] == "IRR")
    )


def normal(text: str) -> str:
    """For the overlap check: no spaces or ZWNJ, Persian ی and ک, Latin digits."""
    text = text.replace(" ", "").replace("‌", "").replace("ي", "ی").replace("ك", "ک")
    return text.translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789"))


def check_case(case: dict, prefix: str) -> None:
    """One case: its shape, tags, intent, values, end and option turns."""
    assert set(case) == {"id", "tags", "turns", "expect"}, case
    assert re.fullmatch(prefix + r"-\d{3}", case["id"]), case["id"]
    assert case["tags"] and set(case["tags"]) <= TAGS, case["id"]
    assert case["turns"] and all(isinstance(turn, str) and turn.strip() for turn in case["turns"]), case["id"]
    expect = case["expect"]
    assert set(expect) == {"intent", "values", "end", "asks"} and expect["end"] in ENDS, case["id"]
    assert expect["asks"] == [] or expect["end"] == "clarifying", case["id"]
    if "unambiguous" in case["tags"]:
        assert len(case["turns"]) == 1 and expect["intent"] in FEATURES, case["id"]
    if expect["intent"] in (KNOWLEDGE, NOT_SURE):
        assert expect["values"] == {} and expect["asks"] == [] and expect["end"] == "created", case["id"]
        return
    assert expect["intent"] in FEATURES, case["id"]
    wanted = requirements(expect["intent"])
    assert set(expect["values"]) <= set(wanted) and set(expect["asks"]) <= set(wanted), case["id"]
    for ref, value in expect["values"].items():
        item = wanted[ref]
        if item.type == "money":
            assert money_ok(value), (case["id"], ref)
        elif item.lookup is not None:
            assert value in keys(item.lookup) or typed(item.lookup, value), (case["id"], ref)
        else:
            assert isinstance(value, str) and value.strip(), (case["id"], ref)
    if "e2e" in case["tags"]:
        if FEATURES[expect["intent"]]["form"] is None:
            assert expect["end"] == "completed", case["id"]
        else:
            assert set(expect["values"]) == set(wanted) and expect["end"] == "ready_for_user", case["id"]
    for turn in case["turns"][1:]:
        if turn.startswith(OPTION):
            ref = turn[len(OPTION) :]
            field, _, key = ref.partition("=")
            fits = field in wanted and wanted[field].lookup is not None and key in keys(wanted[field].lookup)
            assert ref in FEATURES or fits, (case["id"], turn)


@pytest.mark.parametrize(("name", "prefix"), [("heldout.json", "H"), ("tuning.json", "T")])
def test_every_case_has_the_format_and_valid_labels(name: str, prefix: str) -> None:
    cases = read(name)
    assert len({case["id"] for case in cases}) == len(cases)
    for case in cases:
        check_case(case, prefix)


def money_features() -> list[str]:
    """Every feature with a money requirement."""
    return [intent for intent in FEATURES if any(r.type == "money" for r in requirements(intent).values())]


def test_the_heldout_set_has_every_required_count() -> None:
    cases = read("heldout.json")
    tags = Counter(tag for case in cases for tag in case["tags"])
    intents = Counter(case["expect"]["intent"] for case in cases)
    assert len(cases) >= 100
    assert all(tags[tag] >= least for tag, least in HELDOUT_AT_LEAST.items()), tags
    assert all(intents[intent] >= 1 for intent in FEATURES), set(FEATURES) - set(intents)
    for intent in money_features():
        mine = [case for case in cases if case["expect"]["intent"] == intent]
        assert len(mine) >= 3 and any("e2e" in case["tags"] for case in mine), intent
    kinds = set()
    for case in cases:
        if "typed_number" in case["tags"]:
            wanted = requirements(case["expect"]["intent"])
            kinds |= {
                wanted[ref].lookup.split(".")[-1]
                for ref, value in case["expect"]["values"].items()
                if wanted[ref].lookup and typed(wanted[ref].lookup, value)
            }
    assert {"card", "account", "iban"} <= kinds, kinds
    by_id = {case["id"]: case for case in cases}
    assert all("correction" in by_id[item]["tags"] for ids in CORRECTED.values() for item in ids)
    sides = {case["expect"]["intent"] for case in cases if "wallet_vs_bank" in case["tags"]}
    assert {"bill_payment", "wallet_bill", "mobile_top_up", "wallet_mobile_top_up"} <= sides, sides


def test_the_tuning_set_has_its_minimums() -> None:
    cases = read("tuning.json")
    tags = {tag for case in cases for tag in case["tags"]}
    intents = {case["expect"]["intent"] for case in cases}
    assert len(cases) >= 40
    assert tags == TAGS, TAGS - tags
    assert set(money_features()) <= intents, set(money_features()) - intents


def test_no_heldout_first_turn_appears_in_the_tuning_set() -> None:
    tuning = {normal(turn) for case in read("tuning.json") for turn in case["turns"]}
    shared = [case["id"] for case in read("heldout.json") if normal(case["turns"][0]) in tuning]
    assert shared == []


def test_the_heldout_set_is_frozen_at_the_hash_in_d025() -> None:
    recorded = re.search(r"SHA-256 `([0-9a-f]{64})`", DECISION.read_text(encoding="utf-8")).group(1)
    assert hashlib.sha256((FOLDER / "heldout.json").read_bytes()).hexdigest() == recorded
