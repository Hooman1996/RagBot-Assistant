"""Tests for the apk-baseline suite, run against a throwaway record and evidence folder."""

from __future__ import annotations

import hashlib
import importlib.util
import sys
from pathlib import Path

import pytest

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("project_checks", REPO / "tools" / "project_checks.py")
project_checks = importlib.util.module_from_spec(spec)
sys.modules["project_checks"] = project_checks  # dataclasses resolve their module by name
spec.loader.exec_module(project_checks)

APK_BYTES = b"stand-in for the APK"


@pytest.fixture
def checkout(tmp_path: Path) -> Path:
    root = tmp_path / "repo"
    (root / "docs").mkdir(parents=True)
    digest = hashlib.sha256(APK_BYTES).hexdigest()
    (root / "docs/apk-baseline.md").write_text(
        f"| Role | Bytes | SHA-256 |\n|---|---|---|\n| `base` | {len(APK_BYTES)} | `{digest}` |\n",
        encoding="utf-8",
    )
    return root


def add_apk(root: Path, name: str, data: bytes) -> None:
    folder = root / "local/evidence/apk"
    folder.mkdir(parents=True, exist_ok=True)
    (folder / name).write_bytes(data)


def check(root: Path) -> project_checks.Report:
    report = project_checks.Report()
    project_checks.check_apk_baseline(root, report)
    return report


def test_matching_apk_passes(checkout: Path):
    add_apk(checkout, "app.apk", APK_BYTES)
    report = check(checkout)
    assert report.failures == []
    assert report.not_applicable == []


def test_no_evidence_folder_is_not_applicable(checkout: Path):
    report = check(checkout)
    assert report.failures == []
    assert report.not_applicable == ["APK hashes: no local APK (local/evidence/ is absent)"]


def test_missing_apk_fails(checkout: Path):
    (checkout / "local/evidence/apk").mkdir(parents=True)
    assert any("APK found" in f for f in check(checkout).failures)


def test_changed_apk_fails(checkout: Path):
    add_apk(checkout, "app.apk", APK_BYTES + b"!")
    assert any("match the recorded" in f for f in check(checkout).failures)


def test_unrecorded_split_fails(checkout: Path):
    add_apk(checkout, "app.apk", APK_BYTES)
    add_apk(checkout, "split_config.apk", b"another file")
    assert any("match the recorded" in f for f in check(checkout).failures)


def test_missing_record_fails(checkout: Path):
    (checkout / "docs/apk-baseline.md").unlink()
    add_apk(checkout, "app.apk", APK_BYTES)
    assert any("apk-baseline.md exists" in f for f in check(checkout).failures)
