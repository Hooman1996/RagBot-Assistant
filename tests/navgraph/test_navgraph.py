"""Tests for the navgraph suite, run against a throwaway navigation graph."""

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
    "nodes": [
        {
            "id": "F-001",
            "name": "Launch",
            "area": "F-001",
            "label": "Recovered",
            "dynamic": False,
            "note": "n",
        },
        {"id": "S-001", "name": "Home", "area": "F-014", "label": "Recovered", "dynamic": False, "note": "n"},
        {"id": "F-066", "name": "Menu", "area": "F-066", "label": "Inferred", "dynamic": True, "note": "n"},
    ],
    "edges": [
        {
            "from": "F-001",
            "to": "S-001",
            "action": "open",
            "label": "Recovered",
            "dynamic": False,
            "note": "n",
        },
        {"from": "F-066", "to": None, "action": "select", "label": "Inferred", "dynamic": True, "note": "n"},
    ],
}


def write(root: Path, graph) -> Path:
    folder = root / "app_profiles"
    folder.mkdir(parents=True, exist_ok=True)
    text = graph if isinstance(graph, str) else json.dumps(graph)
    (folder / "navigation-graph.json").write_text(text, encoding="utf-8")
    return root


def check(root: Path) -> project_checks.Report:
    report = project_checks.Report()
    project_checks.check_navgraph(root, report)
    return report


def test_valid_graph_passes(tmp_path: Path):
    report = check(write(tmp_path, GOOD))
    assert report.failures == []


def test_missing_graph_fails(tmp_path: Path):
    assert any("navigation graph exists" in f for f in check(tmp_path).failures)


def test_unparseable_json_fails(tmp_path: Path):
    assert any("valid JSON" in f for f in check(write(tmp_path, "{not json")).failures)


def test_node_missing_label_fails(tmp_path: Path):
    graph = json.loads(json.dumps(GOOD))
    del graph["nodes"][1]["label"]
    assert any("every node has id, name and label" in f for f in check(write(tmp_path, graph)).failures)


def test_duplicate_id_fails(tmp_path: Path):
    graph = json.loads(json.dumps(GOOD))
    graph["nodes"][1]["id"] = "F-001"
    assert any("node IDs are unique" in f for f in check(write(tmp_path, graph)).failures)


def test_unknown_label_fails(tmp_path: Path):
    graph = json.loads(json.dumps(GOOD))
    graph["nodes"][1]["label"] = "Guessed"
    assert any("one of the four" in f for f in check(write(tmp_path, graph)).failures)


def test_dangling_edge_to_fails(tmp_path: Path):
    graph = json.loads(json.dumps(GOOD))
    graph["edges"][0]["to"] = "S-999"
    assert any("no dangling edges" in f for f in check(write(tmp_path, graph)).failures)


def test_dangling_edge_from_fails(tmp_path: Path):
    graph = json.loads(json.dumps(GOOD))
    graph["edges"][0]["from"] = "S-999"
    assert any("no dangling edges" in f for f in check(write(tmp_path, graph)).failures)


def test_null_destination_without_dynamic_fails(tmp_path: Path):
    graph = json.loads(json.dumps(GOOD))
    graph["edges"][1]["dynamic"] = False
    assert any("marked dynamic" in f for f in check(write(tmp_path, graph)).failures)
