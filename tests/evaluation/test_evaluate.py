"""The evaluation runner end to end, on the scripted model and fixed embeddings, with no network."""

import json
from pathlib import Path

import pytest
from agent_fakes import DownEmbeddings, FakeChat, FakeEmbeddings

from tools import evaluate_agent

CARD = {"choice": "card_transfer"}
SENTENCE = "۲۵۰ هزار تومان از کارت 0018 به رضا کارت به کارت کن"
NO_SOURCE = "۲۵۰ هزار تومان به رضا کارت به کارت کن"
AMOUNT = {"value": 2500000, "unit": "IRR"}
EXPECTED = {
    "card-transfer-source": "card-1",
    "card-transfer-destination": "reza",
    "card-transfer-amount": AMOUNT,
}


def said(source: str | None, toman: int = 250000) -> dict:
    """The fake model's values for a card transfer."""
    amount = {"value": toman, "unit": "IRT"}
    return {
        "card-transfer-source": source,
        "card-transfer-destination": "رضا",
        "card-transfer-amount": amount,
    }


def case(number: int, turns: list[str], tags: list[str], values: dict, end: str, asks: list[str]) -> dict:
    """One card-transfer case."""
    expect = {"intent": "card_transfer", "values": values, "end": end, "asks": asks}
    return {"id": f"T-{number:03d}", "tags": tags, "turns": turns, "expect": expect}


FULL = case(1, [SENTENCE], ["unambiguous", "e2e"], EXPECTED, "ready_for_user", [])
MISSING = case(
    2, [NO_SOURCE], ["missing"], {"card-transfer-amount": AMOUNT}, "clarifying", ["card-transfer-source"]
)


def run(tmp_path: Path, monkeypatch: pytest.MonkeyPatch, cases: list[dict], chat: FakeChat, embeddings=None):
    """Writes the cases, runs main and returns its exit code and the records it wrote."""
    monkeypatch.setattr(evaluate_agent, "RECORDS", tmp_path / "records")
    path = tmp_path / "cases.json"
    path.write_text(json.dumps(cases, ensure_ascii=False), encoding="utf-8")
    code = evaluate_agent.main(chat, embeddings or FakeEmbeddings(), str(path))
    written = list((tmp_path / "records").glob("cases-*.json"))
    return code, json.loads(written[0].read_text(encoding="utf-8")) if written else []


def test_a_card_transfer_with_the_right_values_passes_every_measure(tmp_path, monkeypatch) -> None:
    code, records = run(tmp_path, monkeypatch, [FULL], FakeChat([CARD, said("0018")]))
    result = records[0]["score"]
    assert result["unambiguous"] is True and result["preparation"] is True
    assert result["wrong_action"] is None
    assert records[0]["operations"] == ["navigate", "fill_form", "show_review"]
    assert code == 0


def test_a_wrong_source_card_is_a_critical_wrong_action(tmp_path, monkeypatch, capsys) -> None:
    code, records = run(tmp_path, monkeypatch, [FULL], FakeChat([CARD, said("0026")]))
    assert "card-transfer-source" in records[0]["score"]["wrong_action"]
    assert code == 1
    assert "critical wrong actions       1 / 1" in capsys.readouterr().out


def test_another_feature_with_a_form_prepared_is_a_critical_wrong_action(tmp_path, monkeypatch) -> None:
    group = {"group-source": "حساب نمایشی", "group-total": {"value": 250000, "unit": "IRT"}}
    code, records = run(tmp_path, monkeypatch, [FULL], FakeChat([{"choice": "group_transfer"}, group]))
    assert records[0]["score"]["wrong_action"] == "prepared group_transfer, expected card_transfer"
    assert code == 1


def test_a_missing_source_that_ends_asking_is_a_safe_stop(tmp_path, monkeypatch) -> None:
    code, records = run(tmp_path, monkeypatch, [MISSING], FakeChat([CARD, said(None)]))
    assert records[0]["task_state"] == "clarifying" and records[0]["questions"] == 1
    assert records[0]["score"]["safe_stop"] is True
    assert code == 0


def test_an_option_not_offered_fails_the_case_and_the_run_goes_on(tmp_path, monkeypatch) -> None:
    turns = [NO_SOURCE, "option:card-transfer-source=card-9"]
    picked = case(3, turns, ["missing", "option", "e2e"], EXPECTED, "ready_for_user", [])
    chat = FakeChat([CARD, said(None), CARD, said("0018")])
    code, records = run(tmp_path, monkeypatch, [picked, FULL], chat)
    assert records[0]["failure"] == "option not offered: card-transfer-source=card-9"
    assert records[0]["score"]["preparation"] is False and records[0]["score"]["safe_stop"] is False
    assert records[1]["score"]["preparation"] is True
    assert code == 1


def test_a_crash_is_recorded_and_the_next_case_still_runs(tmp_path, monkeypatch) -> None:
    chat = FakeChat([CARD, RuntimeError("boom"), CARD, said("0018")])
    code, records = run(tmp_path, monkeypatch, [FULL, FULL | {"id": "T-002"}], chat)
    assert records[0]["crash"] == "RuntimeError: boom"
    assert records[0]["score"]["unambiguous"] is False
    assert records[1]["score"]["preparation"] is True
    assert code == 1


def test_embeddings_that_cannot_be_reached_exit_2(tmp_path, monkeypatch) -> None:
    code, records = run(tmp_path, monkeypatch, [FULL], FakeChat([]), DownEmbeddings())
    assert code == 2 and records == []
