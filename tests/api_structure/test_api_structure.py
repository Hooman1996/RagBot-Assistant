"""Tests for the api-structure suite, run against a throwaway API-structure map."""

from __future__ import annotations

import importlib.util
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("project_checks", REPO / "tools" / "project_checks.py")
project_checks = importlib.util.module_from_spec(spec)
sys.modules["project_checks"] = project_checks  # dataclasses resolve their module by name
spec.loader.exec_module(project_checks)

GOOD = {
    "endpoints": [
        {
            "id": "E-001",
            "area": "F-028",
            "name": "get source accounts",
            "method": "GET",
            "params": [{"name": "url", "in": "url", "type": "string", "label": "Recovered"}],
            "request_model": None,
            "response_model": "M-001",
            "auth": "bearer",
            "label": "Recovered",
            "note": "n",
        },
        {
            "id": "E-002",
            "area": "F-000",
            "name": "post thing",
            "method": "POST",
            "params": [{"name": "body", "in": "body", "type": "M-002", "label": "Recovered"}],
            "request_model": "M-002",
            "response_model": None,
            "auth": "none",
            "label": "Unknown",
            "note": "n",
        },
    ],
    "models": [
        {
            "id": "M-001",
            "name": "res x",
            "fields": [{"name": "amount", "type": "long", "label": "Recovered"}],
            "label": "Recovered",
            "note": "",
        },
        {"id": "M-002", "name": "req y", "fields": [], "label": "Unknown", "note": "n"},
    ],
}


def write(root: Path, api) -> Path:
    folder = root / "app_profiles"
    folder.mkdir(parents=True, exist_ok=True)
    text = api if isinstance(api, str) else json.dumps(api)
    (folder / "api-structure.json").write_text(text, encoding="utf-8")
    return root


def check(root: Path) -> project_checks.Report:
    report = project_checks.Report()
    project_checks.check_api_structure(root, report)
    return report


def test_valid_map_passes(tmp_path: Path):
    assert check(write(tmp_path, GOOD)).failures == []


def test_missing_map_fails(tmp_path: Path):
    assert any("API-structure map exists" in f for f in check(tmp_path).failures)


def test_unparseable_json_fails(tmp_path: Path):
    assert any("valid JSON" in f for f in check(write(tmp_path, "{not json")).failures)


def test_endpoint_missing_label_fails(tmp_path: Path):
    api = json.loads(json.dumps(GOOD))
    del api["endpoints"][0]["label"]
    assert any("has id, name, method and label" in f for f in check(write(tmp_path, api)).failures)


def test_duplicate_endpoint_id_fails(tmp_path: Path):
    api = json.loads(json.dumps(GOOD))
    api["endpoints"][1]["id"] = "E-001"
    assert any("endpoint IDs are unique" in f for f in check(write(tmp_path, api)).failures)


def test_unknown_label_fails(tmp_path: Path):
    api = json.loads(json.dumps(GOOD))
    api["endpoints"][0]["label"] = "Guessed"
    assert any("one of the four" in f for f in check(write(tmp_path, api)).failures)


def test_unknown_response_model_ref_fails(tmp_path: Path):
    api = json.loads(json.dumps(GOOD))
    api["endpoints"][0]["response_model"] = "M-999"
    assert any("names a known model" in f for f in check(write(tmp_path, api)).failures)


def test_unknown_param_model_ref_fails(tmp_path: Path):
    api = json.loads(json.dumps(GOOD))
    api["endpoints"][1]["params"][0]["type"] = "M-999"
    assert any("names a known model" in f for f in check(write(tmp_path, api)).failures)


def test_duplicate_model_id_fails(tmp_path: Path):
    api = json.loads(json.dumps(GOOD))
    api["models"][1]["id"] = "M-001"
    assert any("model IDs are unique" in f for f in check(write(tmp_path, api)).failures)
