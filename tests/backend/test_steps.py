"""The step prints: silent by default, cards masked, and the debug timing switch."""

import subprocess
import sys

import pytest
from test_task_runner import CARD_VALUES, FakeApp, run

from assistant.steps import show


def test_nothing_is_printed_unless_tracing_is_on(
    monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture
) -> None:
    monkeypatch.delenv("ASSISTANT_TRACE", raising=False)
    show("step", card="6037991234567890")
    assert capsys.readouterr().out == ""


def test_a_card_number_is_masked_and_persian_stays_readable(
    monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture
) -> None:
    monkeypatch.setenv("ASSISTANT_TRACE", "1")
    show("step", card="6037991234567890", values={"name": "رضا", "iban": "IR120000000000000000001234"})
    out = capsys.readouterr().out
    assert "****7890" in out
    assert "****1234" in out
    assert "6037991234567890" not in out
    assert "رضا" in out


def test_a_traced_card_to_reza_run_never_prints_the_full_card(
    monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture
) -> None:
    monkeypatch.setenv("ASSISTANT_TRACE", "1")
    assert run(FakeApp(), CARD_VALUES).state == "review"
    out = capsys.readouterr().out
    assert "0091" in out
    assert "0000000000000091" not in out


def test_debug_mode_keeps_a_command_for_an_hour() -> None:
    code = (
        "from assistant.command_queue import COMMAND_LIFETIME; "
        "from assistant.bridge_link import RESULT_TIMEOUT_SECONDS; "
        "print(COMMAND_LIFETIME.total_seconds(), RESULT_TIMEOUT_SECONDS)"
    )
    shown = subprocess.run(
        [sys.executable, "-c", code], env={"ASSISTANT_DEBUG": "1"}, capture_output=True, text=True, check=True
    )
    assert shown.stdout.split() == ["3600.0", "3600"]
