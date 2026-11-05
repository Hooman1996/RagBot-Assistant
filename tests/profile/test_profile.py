"""Tests for the profile suite, run against a throwaway checkout."""

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

FILES = ("replica.profile.json", "profile.schema.json", "navigation-graph.json")


@pytest.fixture
def checkout(tmp_path: Path) -> Path:
    (tmp_path / "app_profiles").mkdir()
    for name in FILES:
        shutil.copy(REPO / "app_profiles" / name, tmp_path / "app_profiles" / name)
    return tmp_path


def edit(root: Path, change) -> None:
    path = root / project_checks.REPLICA_PROFILE
    profile = json.loads(path.read_text(encoding="utf-8"))
    change(profile)
    path.write_text(json.dumps(profile), encoding="utf-8")


def failures(root: Path) -> list[str]:
    report = project_checks.Report()
    project_checks.check_profile(root, report)
    return report.failures


def test_committed_profile_passes(checkout: Path):
    assert failures(checkout) == []


def test_fresh_export_is_not_applicable_when_absent(checkout: Path):
    report = project_checks.Report()
    project_checks.check_profile(checkout, report)
    assert any("fresh profile export" in item for item in report.not_applicable)


def test_stale_export_fails(checkout: Path):
    export = checkout / project_checks.PROFILE_EXPORT
    export.parent.mkdir(parents=True)
    export.write_text("{}", encoding="utf-8")
    assert any("matches a fresh export" in f for f in failures(checkout))


def test_screen_missing_from_graph_fails(checkout: Path):
    edit(checkout, lambda p: p["screens"].append("S-999"))
    assert any("in the graph" in f for f in failures(checkout))


def test_edge_to_unknown_screen_fails(checkout: Path):
    edit(checkout, lambda p: p["edges"].append({"from": "S-026", "to": "S-999", "action": "x"}))
    assert any("edge endpoint" in f for f in failures(checkout))


def test_edge_with_user_only_step_fails(checkout: Path):
    edit(
        checkout,
        lambda p: p["edges"].append({"from": "S-026", "to": "S-173", "action": "x > payment-submit"}),
    )
    assert any("user-only" in f for f in failures(checkout))


def test_edge_matching_a_user_only_pattern_fails(checkout: Path):
    edit(
        checkout,
        lambda p: p["edges"].append({"from": "S-026", "to": "S-173", "action": "toll-fee-account-1"}),
    )
    assert any("user-only" in f for f in failures(checkout))


def test_secret_required_slot_fails(checkout: Path):
    def change(profile: dict) -> None:
        feature = next(f for f in profile["features"] if f["intent"] == "card_balance")
        feature["required_slots"] = ["secure-cvv2"]

    edit(checkout, change)
    assert any("fillable slots" in f for f in failures(checkout))


def test_field_type_outside_the_schema_fails(checkout: Path):
    def change(profile: dict) -> None:
        profile["forms"][0]["fields"][0]["type"] = "password"

    edit(checkout, change)
    assert any("matches the schema" in f for f in failures(checkout))
