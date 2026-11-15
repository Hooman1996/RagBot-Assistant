"""The read-only lookups over the mock fixtures."""

import json
from pathlib import Path

import pytest

from assistant.lookups import SOURCES, Ambiguous, FixtureLookups, NotFound, is_known
from assistant.words import offered


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


def test_where_keeps_only_the_records_with_those_fields() -> None:
    active_debit = FixtureLookups().candidates("cards", {"kind": "DEBIT", "status": "فعال"})
    assert [card.key for card in active_debit] == ["card-1", "card-2"]


def test_installments_are_the_ones_the_app_lets_the_user_pick() -> None:
    lookups = FixtureLookups()
    assert lookups.installments("insurance", "policy-1") == ["3", "4"]  # 1 paid, 2 being verified
    assert lookups.installments("insurance", "policy-2") == []
    assert lookups.installments("loans", "loan-personal") == ["1"]  # the next unpaid one
    assert lookups.installments("loans", "loan-overdue") == ["0"]  # an overdue one comes first
    assert lookups.installments("loans", "loan-paid") == []


def test_a_wallet_top_up_amount_comes_from_the_operators_charge_list() -> None:
    assert FixtureLookups().charges("RIGHTEL") == [200000, 500000, 1000000]


def test_records_the_app_hides_or_keeps_behind_a_tab_are_not_offered() -> None:
    assert offered("wallets", "wallet-op-source") == {"status": "ACTIVE"}  # the inactive wallet
    assert offered("loans", "loan") == {"corporateTitle": None}  # corporate loans sit on another tab
    assert offered("accounts", "loan-source") == {"ownership": "انفرادی"}  # so do joint accounts


def test_the_offer_rule_filters_only_its_own_lookup() -> None:
    assert offered("cards.number", "topup-source") == {"kind": "DEBIT"}
    assert offered("accounts", "topup-source") == {}  # the account page still offers every account


def test_a_lookup_outside_the_adapter_is_unknown() -> None:
    assert is_known("contacts.card")
    assert not is_known("passwords")


def test_lookups_read_and_never_write() -> None:
    source = Path("assistant/lookups.py").read_text(encoding="utf-8")
    assert "write_" not in source
    assert '"w"' not in source


@pytest.mark.parametrize(
    ("lookup", "said", "keys"),
    [
        ("cards", "کارت 0018", ["card-1"]),
        ("cards", "كارت ۰۰۴۲", ["card-4"]),
        ("accounts", "پس انداز نمایشی", ["account-2"]),
        ("accounts", "حساب پس‌انداز بگیر", ["account-2"]),
        ("wallets", "کیف نمایشی تجاریمو", ["0000000000102"]),
        ("contacts.account", "حساب رضا", ["reza"]),
        ("charities", "خیریه آموزش", ["1"]),
        ("accounts", "حساب", ["account-1", "account-3"]),
        ("accounts", "حساب جاری", []),
        ("contacts.card", "به علی", []),
    ],
)
def test_matching_ignores_digits_letters_zwnj_and_generic_words(
    lookup: str, said: str, keys: list[str]
) -> None:
    assert [item.key for item in FixtureLookups().matching(lookup, said)] == keys


def test_a_typed_iban_is_kept_unless_it_is_a_saved_one() -> None:
    lookups = FixtureLookups()
    assert lookups.resolve("contacts.iban", "IR380000000000000000000004") == "IR380000000000000000000004"
    assert [item.key for item in lookups.matching("contacts.iban", "IR070000000000000000000077")] == ["reza"]


def test_a_typed_number_must_fit_its_field() -> None:
    lookups = FixtureLookups()
    with pytest.raises(NotFound):  # an account number is no IBAN, even one inside a saved IBAN
        lookups.resolve("contacts.iban", "0000000000077")
    assert lookups.resolve("contacts.account", "0000000000099") == "0000000000099"
