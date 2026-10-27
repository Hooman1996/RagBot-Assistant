"""Tests for the contracts suite and the §13 transition tables."""

from __future__ import annotations

import importlib.util
import json
import re
import shutil
import sys
from pathlib import Path

import pytest

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("project_checks", REPO / "tools" / "project_checks.py")
project_checks = importlib.util.module_from_spec(spec)
sys.modules["project_checks"] = project_checks  # dataclasses resolve their module by name
spec.loader.exec_module(project_checks)


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def save(path: Path, data) -> None:
    path.write_text(json.dumps(data), encoding="utf-8")


@pytest.fixture
def checkout(tmp_path: Path) -> Path:
    for name in ("contracts", "app_profiles"):
        shutil.copytree(REPO / name, tmp_path / name)
    return tmp_path


def failures(root: Path) -> list[str]:
    return project_checks.run("contracts", root).failures


def plan_table(heading: str) -> dict[str, list[str]]:
    """Read a transition table from plan §13: current state -> allowed next states."""
    plan = (REPO / project_checks.PLAN_FILE).read_text(encoding="utf-8")
    section = plan.split(heading, 1)[1].split("\n#", 1)[0]
    table: dict[str, list[str]] = {}
    for line in section.splitlines():
        cells = line.split("|")[1:-1]
        if len(cells) < 2:
            continue
        allowed = re.findall(r"`(\w+)`", cells[1])
        for state in re.findall(r"`(\w+)`", cells[0]):
            table[state] = allowed
        for state in allowed:
            table.setdefault(state, [])
    return table


def test_repository_passes_contract_checks():
    assert failures(REPO) == []


@pytest.mark.parametrize(
    "name, heading", [("task", "### Task transitions"), ("command", "### Command transitions")]
)
def test_transitions_match_plan(name: str, heading: str):
    plan = plan_table(heading)
    data = load(REPO / "contracts" / "transitions" / f"{name}.json")
    assert sorted(data) == sorted(plan)
    # Every pair of states: allowed in the data exactly when the plan lists it.
    wrong = [(a, b) for a in plan for b in plan if (b in data[a]) != (b in plan[a])]
    assert wrong == []


def test_record_states_match_transition_tables():
    schemas = REPO / "contracts" / "schemas"
    status = next(
        part["then"]
        for part in load(schemas / "response.schema.json")["allOf"]
        if part["if"]["properties"]["kind"]["const"] == "status"
    )
    assert set(status["properties"]["task_state"]["enum"]) == set(plan_table("### Task transitions"))
    outcomes = set(load(schemas / "result.schema.json")["properties"]["outcome"]["enum"])
    assert outcomes == set(plan_table("### Command transitions")) - {"issued", "executing"}


def test_submit_in_assistant_api_fails(checkout: Path):
    path = checkout / "contracts/openapi/assistant_mobile.json"
    api = load(path)
    api["paths"]["/assistant/v1/drafts/{draft_id}/submit"] = {
        "post": {"operationId": "submitDraft", "responses": {"200": {"description": "done"}}}
    }
    save(path, api)
    assert any("assistant_mobile.json has no submit" in f for f in failures(checkout))


def test_extra_command_operation_fails(checkout: Path):
    path = checkout / "contracts/schemas/command.schema.json"
    schema = load(path)
    schema["$defs"]["operation"]["enum"].append("authorize")
    save(path, schema)
    assert any("exactly the five" in f for f in failures(checkout))


def test_edge_to_user_only_action_fails(checkout: Path):
    path = checkout / "app_profiles/synthetic.profile.json"
    profile = load(path)
    profile["edges"].append({"from": "example_form", "to": "home", "action": "confirm_example"})
    save(path, profile)
    assert any("no edge uses a user-only action" in f for f in failures(checkout))


def test_broken_valid_example_fails(checkout: Path):
    path = checkout / "contracts/examples/valid/command.navigate.json"
    command = load(path)
    del command["expires_at"]
    save(path, command)
    assert any("command.navigate.json passes" in f for f in failures(checkout))


def test_invalid_example_rejected_for_another_reason_fails(checkout: Path):
    path = checkout / "contracts/examples/invalid/command.money_wrong_unit.json"
    example = load(path)
    example["expected_error"] = "'submit' is not one of"
    save(path, example)
    assert any("command.money_wrong_unit.json is rejected" in f for f in failures(checkout))
