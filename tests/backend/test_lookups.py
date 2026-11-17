"""The read-only lookups over the mock fixtures."""

import json
from pathlib import Path

import pytest

from assistant.lookups import SOURCES, Ambiguous, FixtureLookups, NotFound, is_known
from assistant.words import offered


def test_a_contact_name_becomes_their_card() -> None:
    assert FixtureLookups().resolve("contacts.card", "رضا") == "6037990836388880"


def test_a_saved_destination_is_found_by_its_name() -> None:
    assert FixtureLookups().resolve("contacts.account", "نگار صادقی") == "0235942675932"
    assert FixtureLookups().resolve("contacts.card", "پریسا نیک‌نام") == "6104334806064544"


def test_a_typed_number_names_a_person_but_never_a_saved_destination() -> None:
    lookups = FixtureLookups()
    assert [item.key for item in lookups.matching("contacts.iban", "IR290170455491846632336115")] == ["reza"]
    assert lookups.matching("contacts.iban", "IR910180319412535974461725") == []


def test_a_typed_card_number_needs_no_contact() -> None:
    assert FixtureLookups().resolve("contacts.card", "6104334806064544") == "6104334806064544"


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
    assert FixtureLookups().resolve("cards.number", "card-1") == "6104336383233566"
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
    assert FixtureLookups().operator(name="RIGHTEL")["defaults"] == [200000, 500000, 1000000]
    assert FixtureLookups().operator(phone="۰۹۲۱۱۲۳۴۵۶۷")["operator"] == "RIGHTEL"  # by its prefix
    assert FixtureLookups().balance("wallets", "0176453202938") == 3250000


def test_the_inactive_wallet_is_not_offered_and_records_behind_a_tab_are() -> None:
    assert offered("wallets", "wallet-op-source") == {"status": "ACTIVE"}
    assert offered("loans", "loan") == {}  # corporate loans: the executor opens their tab
    assert offered("accounts", "loan-source") == {}  # joint accounts too


def test_a_record_is_named_by_its_label_or_the_words_no_other_record_has() -> None:
    accounts = FixtureLookups().candidates("accounts")
    named = FixtureLookups().named
    assert [item.key for item in named("با کارمزد از پس‌انداز استعلام کن", accounts)] == ["account-2"]
    assert [item.key for item in named("بریز به سپرده کوتاه‌مدت", accounts)] == ["account-1"]
    assert named("پس از واریز خبرم کن", accounts) == []  # «پس» alone is not «پس‌انداز»
    assert named("از کیف پول شخصی", accounts) == []  # «نمایشی» is in every label


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
        ("cards", "کارت 3566", ["card-1"]),
        ("cards", "كارت ۰۸۹۰", ["card-4"]),
        ("accounts", "پس انداز", ["account-2"]),
        ("accounts", "حساب پس‌انداز بگیر", ["account-2"]),
        ("wallets", "کیف پول فروشگاهمو", ["0339168371132"]),
        ("contacts.account", "حساب رضا", ["reza"]),
        ("charities", "خیریه آموزش", ["1"]),
        ("accounts", "حساب", ["account-3"]),
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
    assert lookups.resolve("contacts.iban", "IR910180319412535974461725") == "IR910180319412535974461725"
    assert [item.key for item in lookups.matching("contacts.iban", "IR290170455491846632336115")] == ["reza"]


def test_a_typed_number_must_fit_its_field() -> None:
    lookups = FixtureLookups()
    with pytest.raises(NotFound):  # an account number is no IBAN, even one inside a saved IBAN
        lookups.resolve("contacts.iban", "0970836557800")
    assert lookups.resolve("contacts.account", "0281707721676") == "0281707721676"
