"""The assistant's own words about the app cover every feature and every choice the agent can ask."""

import re

from agent_fakes import PROFILE

from assistant.agent_nodes import plan_for
from assistant.lookups import FixtureLookups
from assistant.navigator import HOME, Navigator
from assistant.words import WORDS, feature_words, fixed, options

NAVIGATOR = Navigator(PROFILE)
LOOKUPS = FixtureLookups()


def test_every_feature_has_persian_words() -> None:
    intents = {item["intent"] for item in PROFILE["features"]}
    assert set(WORDS["features"]) == intents
    assert all(re.search(r"[؀-ۿ]", feature_words(intent)) for intent in intents)


def test_every_choice_the_agent_can_ask_has_allowed_keys_and_plain_labels() -> None:
    for feature in PROFILE["features"]:
        for item in plan_for(NAVIGATOR, HOME, feature).requirements:
            if item.type != "choice":
                continue
            found = options(item.screen, item.ref, LOOKUPS)
            labels = [choice["label"] for choice in found]
            assert found, (feature["intent"], item.ref)
            assert all(re.fullmatch(r"[\w-]+", choice["key"]) for choice in found), item.ref
            assert len(set(labels)) == len(labels), item.ref
            assert not [label for label in labels if re.search(r"\d{12,}", label)], item.ref


def test_every_fixed_value_is_an_allowed_key_of_its_feature() -> None:
    features = {item["intent"]: item for item in PROFILE["features"]}
    for intent in WORDS["fixed"]:
        wanted = {item.ref: item for item in plan_for(NAVIGATOR, HOME, features[intent]).requirements}
        for ref, key in fixed(intent).items():
            assert key in [choice["key"] for choice in options(wanted[ref].screen, ref, LOOKUPS)], (
                intent,
                ref,
            )
