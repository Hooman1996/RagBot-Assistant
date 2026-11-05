"""Tests for the mock-data suite, run against a throwaway copy of the files."""

from __future__ import annotations

import importlib.util
import json
import shutil
import sys
from pathlib import Path

import pytest

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("project_checks", REPO / "tools" / "project_checks.py")
project_checks = importlib.util.module_from_spec(spec)
sys.modules["project_checks"] = project_checks  # dataclasses resolve their module by name
spec.loader.exec_module(project_checks)


@pytest.fixture
def checkout(tmp_path: Path) -> Path:
    shutil.copytree(REPO / "mock_services", tmp_path / "mock_services")
    return tmp_path


def failures(root: Path) -> list[str]:
    report = project_checks.Report()
    project_checks.check_mock_data(root, report)
    return report.failures


def edit(root: Path, name: str, change) -> None:
    path = root / "mock_services" / "fixtures" / f"{name}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    change(data)
    path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")


def test_the_committed_files_pass(checkout: Path):
    assert failures(checkout) == []


def test_a_missing_file_fails(checkout: Path):
    (checkout / "mock_services" / "fixtures" / "bills.json").unlink()
    assert any("every mock file exists" in f for f in failures(checkout))


def test_a_float_amount_fails(checkout: Path):
    edit(checkout, "bills", lambda d: d["bills"][0].update(amount=1.5))
    assert any("bills.json" in f for f in failures(checkout))


def test_another_unit_fails(checkout: Path):
    edit(checkout, "accounts", lambda d: d.update(unit="USD"))
    assert any("accounts.json" in f for f in failures(checkout))


def test_a_card_with_a_bad_checksum_fails(checkout: Path):
    edit(checkout, "contacts", lambda d: d["people"][0].update(card="0000000000000092"))
    assert any("card passes" in f for f in failures(checkout))


def test_an_iban_with_a_bad_checksum_fails(checkout: Path):
    edit(checkout, "contacts", lambda d: d["people"][0].update(iban="IR080000000000000000000077"))
    assert any("IBAN passes" in f for f in failures(checkout))


def test_a_cheque_book_on_an_unknown_account_fails(checkout: Path):
    edit(checkout, "cheques", lambda d: d["books"][0].update(accountKey="nobody"))
    assert any("cheque book" in f for f in failures(checkout))
