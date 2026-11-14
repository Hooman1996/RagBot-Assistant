"""The assistant's own words about the app: Persian words per feature, and the keys of choice fields.

Called by intent, the agent's nodes and replies. It reads app_profiles/assistant_words.json, a file
the assistant owns: the bank's app is never changed, so what the assistant must know about it lives
here. A choice's options are fixed pairs of app key and Persian label, or the records of a lookup.
It never shows the model a key, only labels, and never a full card or account number.
"""

import json
from pathlib import Path

from assistant.lookups import FixtureLookups

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


def fixed(intent: str) -> dict:
    """Values the feature itself decides, such as which paid inquiry the menu opens."""
    return WORDS["fixed"].get(intent, {})
