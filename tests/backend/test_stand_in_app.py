"""The stand-in app: every command and result it handles matches the contract, and it plays fair."""

from fnmatch import fnmatchcase

from agent_fakes import PROFILE, RecordingApp

from assistant.contract_check import problems
from assistant.lookups import FixtureLookups
from assistant.navigator import HOME, Navigator
from assistant.task_runner import TaskReport, TaskRunner

CARD_VALUES = {
    "card-transfer-source": "card-1",
    "card-transfer-destination": "رضا",
    "card-transfer-amount": "2500000",
}


def test_a_card_transfer_on_the_stand_in_matches_the_contract_at_every_step() -> None:
    app = RecordingApp()
    commands, results = [], []
    real_run = app.run

    def run(command: dict) -> dict:
        commands.append(command)
        results.append(real_run(command))
        return results[-1]

    app.run = run
    navigator = Navigator(PROFILE)
    report = TaskRunner(app, navigator, FixtureLookups()).run(
        navigator.plan(HOME, "S-174"), CARD_VALUES, True, TaskReport()
    )
    assert report.state == "review"
    assert [problems("command.schema.json", item) for item in commands] == [[], [], []]
    assert [problems("result.schema.json", item) for item in results] == [[], [], []]
    assert [item["observation"]["observation_version"] for item in results] == [2, 3, 4]
    assert results[1]["observation"]["fields"] == [
        {"ref": "card-transfer-source", "value": "card-1"},
        {"ref": "card-transfer-destination", "value": "****0091"},
        {"ref": "card-transfer-amount", "value": "2500000"},
    ]
    assert results[1]["observation"]["draft"]["draft_version"] == 1
    assert results[2]["observation"]["draft"]["review_version"] == 1


def test_no_walk_to_any_feature_takes_a_user_only_step() -> None:
    for feature in PROFILE["features"]:
        app = RecordingApp()
        result = app.send("navigate", {"destination": feature["destination"]})
        for step in result.get("trace", []):
            for tag in step["action"].split(" > "):
                assert not any(fnmatchcase(tag, item) for item in PROFILE["user_only_actions"]), feature


def test_a_row_edge_puts_the_key_in_place_of_the_star() -> None:
    row = next(edge for edge in PROFILE["edges"] if edge.get("requires", [{}])[0].get("ref") == "charity")
    result = RecordingApp().send("navigate", {"destination": "S-284", "arguments": {"charity": "1"}})
    assert result["outcome"] == "succeeded"
    assert row["action"].replace("*", "1") in [step["action"] for step in result["trace"]]


def test_a_row_edge_without_its_key_is_blocked() -> None:
    result = RecordingApp().send("navigate", {"destination": "S-284"})
    assert result["outcome"] == "blocked"
    assert result["needs"] == ["missing_argument"]


def test_a_secret_or_unknown_field_is_refused() -> None:
    app = RecordingApp("S-174")
    for ref in ("card-transfer-cvv2", "no-such-field"):
        result = app.send("fill_form", {"form": "S-174", "values": {ref: "1234"}})
        assert result["outcome"] == "rejected_policy"
        assert result["error_code"] == "field_not_fillable"


def test_show_review_away_from_a_form_has_no_review() -> None:
    result = RecordingApp().send("show_review", {})
    assert result["outcome"] == "blocked"
    assert result["needs"] == ["no_review_here"]
    assert result["observation"]["screen"] == HOME
