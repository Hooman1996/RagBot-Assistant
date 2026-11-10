"""main_debug.py end to end, through its main() seam, with the scripted model and the stand-in app."""

import importlib

import pytest
from agent_fakes import CARD_SENTENCE, FakeChat, FakeEmbeddings


def test_main_debug_runs_the_example_to_the_review(
    monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture
) -> None:
    # main_debug sets both switches when imported; monkeypatch puts them back after the test.
    monkeypatch.setenv("ASSISTANT_TRACE", "")
    monkeypatch.setenv("ASSISTANT_DEBUG", "")
    main_debug = importlib.import_module("main_debug")
    answers = [
        {"choice": "card_transfer"},
        {
            "card-transfer-source": None,
            "card-transfer-destination": "رضا",
            "card-transfer-amount": {"value": 250000, "unit": "IRT"},
        },
    ]
    result = main_debug.main(FakeChat(answers), FakeEmbeddings(), main_debug.EXAMPLE)
    assert main_debug.EXAMPLE[0] == CARD_SENTENCE
    assert result["task_state"] == "ready_for_user"
    out = capsys.readouterr().out
    assert "1. ****0018" in out
    assert "2,500,000 ریال" in out
    assert "0000000000000091" not in out
