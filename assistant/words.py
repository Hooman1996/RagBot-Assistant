"""The assistant's own words about the app: Persian words per feature, and the keys of choice fields.

Called by intent, the agent's nodes and replies. It reads app_profiles/assistant_words.json, a file
the assistant owns: the bank's app is never changed, so what the assistant must know about it lives
here. A choice's options are fixed pairs of app key and Persian label, or the records of a lookup.
It never shows the model a key, only labels, and never a full card or account number.
"""

import json
from pathlib import Path

from assistant.lookups import FixtureLookups, source_of

WORDS = json.loads(
    (Path(__file__).resolve().parent.parent / "app_profiles" / "assistant_words.json").read_text(
        encoding="utf-8"
    )
)


def feature_words(intent: str) -> str:
    """The feature's Persian words, or an empty string when there are none."""
    return WORDS["features"].get(intent, "")


def options(screen: str, ref: str, lookups: FixtureLookups) -> list[dict]:
    """A choice's allowed keys with their labels: the screen's own list first, then the field's."""
    found = WORDS["choices"].get(f"{screen} {ref}") or WORDS["choices"].get(ref)
    if found is None:
        return []
    if isinstance(found, dict):
        return [{"key": item.key, "label": item.label} for item in lookups.candidates(found["lookup"])]
    return found


def field_words(ref: str) -> str:
    """Words that tell the model more about a field than its short description, if any."""
    return WORDS["fields"].get(ref, "")


def offered(lookup: str, ref: str) -> dict:
    """The fields a lookup's record must have to be offered for this field, as the app's own picker
    lists them: a card-transfer source is an active debit card. Empty when every record may be."""
    return WORDS["offer"].get(source_of(lookup), {}).get(ref, {})


def installment_of(ref: str) -> str | None:
    """The record field (a policy or a loan) whose installments this choice picks, if it is one."""
    return WORDS["installments"].get(ref)


def has_installments(ref: str) -> bool:
    """True for a policy or loan field, whose record must have an installment the app lets the user pick."""
    return ref in WORDS["installments"].values()


def charges_of(ref: str) -> str | None:
    """The operator field whose charge list this amount must come from, if it is such an amount."""
    return WORDS["charges"].get(ref)


def when(values: dict) -> tuple[str | None, dict[str, list[dict]]]:
    """What chosen values change in the app: a screen the route must pass (a card sent by post goes
    by the address page), and the fields the app then asks, each with its options (a weekly day)."""
    through, fields = None, {}
    for ref, cases in WORDS["when"].items():
        case = cases.get(values.get(ref)) if isinstance(values.get(ref), str) else None
        if case:
            through = case.get("via", through)
            fields |= {case["field"]: case["options"]} if "field" in case else {}
    return through, fields


def fixed(intent: str) -> dict:
    """Values the feature itself decides, such as which paid inquiry the menu opens."""
    return WORDS["fixed"].get(intent, {})
