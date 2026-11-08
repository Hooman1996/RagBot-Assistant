"""The read-only lookups over the mock fixtures."""

import json
from pathlib import Path

import pytest

from assistant.lookups import SOURCES, Ambiguous, FixtureLookups, NotFound, is_known


def test_a_contact_name_becomes_their_card() -> None:
    assert FixtureLookups().resolve("contacts.card", "رضا") == "0000000000000091"


def test_a_typed_card_number_needs_no_contact() -> None:
    assert FixtureLookups().resolve("contacts.card", "0000000000000026") == "0000000000000026"


def test_an_unknown_name_is_not_guessed() -> None:
    with pytest.raises(NotFound):
        FixtureLookups().resolve("contacts.card", "علی")


def test_two_matches_are_ambiguous(tmp_path: Path) -> None:
    people = [{"id": "a", "name": "رضا احمدی", "card": "1"}, {"id": "b", "name": "رضا کریمی", "card": "2"}]
    (tmp_path / "contacts.json").write_text(json.dumps({"people": people}), encoding="utf-8")
    with pytest.raises(Ambiguous):
        FixtureLookups(tmp_path).resolve("contacts.card", "رضا")


def test_every_source_finds_its_records() -> None:
    lookups = FixtureLookups()
    for name in SOURCES:
        assert lookups.candidates(name), name


def test_an_attribute_picks_what_is_sent() -> None:
    assert FixtureLookups().resolve("cards.number", "card-1") == "0000000000000018"
    assert FixtureLookups().resolve("cards", "card-1") == "card-1"


def test_a_lookup_outside_the_adapter_is_unknown() -> None:
    assert is_known("contacts.card")
    assert not is_known("passwords")


def test_lookups_read_and_never_write() -> None:
    source = Path("assistant/lookups.py").read_text(encoding="utf-8")
    assert "write_" not in source
    assert '"w"' not in source
