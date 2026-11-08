"""The scripted runner, on a fake app that behaves as the contract says."""

import json
from pathlib import Path

import pytest

from assistant.lookups import FixtureLookups
from assistant.navigator import HOME, Navigator, Plan
from assistant.task_runner import TaskReport, TaskRunner, TaskStopped, money_value

PROFILE = json.loads(Path("app_profiles/replica.profile.json").read_text(encoding="utf-8"))
CARD_VALUES = {
    "card-transfer-source": "card-1",
    "card-transfer-destination": "رضا",
    "card-transfer-amount": "250000",
}
FORBIDDEN = ("submit", "confirm", "authorize", "pay", "select_candidate")


class FakeApp:
    """Moves where it is told and shows what it is given, unless a test says otherwise."""

    def __init__(self, screen: str = HOME) -> None:
        self.screen = screen
        self.sent: list[tuple[str, dict]] = []
        self.fields: list[dict] = []
        self.land_on: str | None = None
        self.needs: list[str] = []
        self.wrong_amount = False

    def send(self, operation: str, arguments: dict) -> dict:
        self.sent.append((operation, arguments))
        if operation == "navigate":
            self.screen = self.land_on or arguments["destination"]
        if operation == "fill_form":
            self.fields = [self.field(ref, value) for ref, value in arguments["values"].items()]
        if operation == "show_review":
            self.screen = "S-176"
        return self.result()

    def field(self, ref: str, value: str | dict) -> dict:
        if isinstance(value, dict):
            return {"ref": ref, "value": "1" if self.wrong_amount else str(value["value"])}
        return {"ref": ref, "value": f"****{value[-4:]}" if value.isdigit() and len(value) >= 12 else value}

    def result(self) -> dict:
        reply = {
            "outcome": "blocked" if self.needs else "succeeded",
            "observation": {"screen": self.screen, "fields": self.fields},
        }
        if self.needs:
            reply["needs"] = self.needs
        return reply


def plan_card() -> Plan:
    return Navigator(PROFILE).plan(HOME, "S-174")


def run(app: FakeApp, values: dict[str, str], plan: Plan | None = None, review: bool = True) -> TaskReport:
    runner = TaskRunner(app, Navigator(PROFILE), FixtureLookups())
    return runner.run(plan or plan_card(), values, review, TaskReport())


def test_card_to_reza_navigates_fills_and_stops_at_the_review() -> None:
    app = FakeApp()
    report = run(app, CARD_VALUES)
    assert report.state == "review"
    assert [operation for operation, _ in app.sent] == ["navigate", "fill_form", "show_review"]
    values = app.sent[1][1]["values"]
    assert values["card-transfer-destination"] == "0000000000000091"
    assert values["card-transfer-amount"] == {"value": 250000, "unit": "IRR"}


def test_a_missing_value_stops_before_the_app_moves() -> None:
    app = FakeApp()
    report = run(app, {"card-transfer-source": "card-1"})
    assert report.state == "stopped"
    assert app.sent == []


def test_every_missing_value_is_found_at_once() -> None:
    runner = TaskRunner(FakeApp(), Navigator(PROFILE), FixtureLookups())
    assert len(runner.missing(plan_card(), {})) == 3


def test_an_unknown_contact_stops_before_the_app_moves() -> None:
    app = FakeApp()
    report = run(app, {**CARD_VALUES, "card-transfer-destination": "علی"})
    assert report.state == "stopped"
    assert app.sent == []


@pytest.mark.parametrize("amount", ["250000.5", "1e5", "abc", "-1"])
def test_money_must_be_a_whole_number(amount: str) -> None:
    with pytest.raises(TaskStopped):
        money_value(amount)


def test_persian_digits_are_money_too() -> None:
    assert money_value("۲۵۰۰۰۰") == {"value": 250000, "unit": "IRR"}


def test_an_unexpected_screen_stops_the_run() -> None:
    app = FakeApp()
    app.land_on = "S-192"
    report = run(app, CARD_VALUES)
    assert report.state == "stopped"
    assert "S-192" in report.reason
    assert [operation for operation, _ in app.sent] == ["navigate"]


def test_a_blocked_navigate_stops_the_run_with_the_apps_reason() -> None:
    app = FakeApp()
    app.needs = ["close_dialog"]
    report = run(app, CARD_VALUES)
    assert report.state == "stopped"
    assert "close_dialog" in report.reason


def test_a_page_showing_the_wrong_amount_stops_before_the_review() -> None:
    app = FakeApp()
    app.wrong_amount = True
    report = run(app, CARD_VALUES)
    assert report.state == "stopped"
    assert "card-transfer-amount" in report.reason
    assert "show_review" not in [operation for operation, _ in app.sent]


def test_a_selection_travels_as_a_navigate_argument() -> None:
    app = FakeApp()
    navigator = Navigator(PROFILE)
    plan = navigator.plan(HOME, "S-284")
    values = {"charity": "1", "charity-amount": "100000", "charity-source": "account-1"}
    run(app, values, plan, review=False)
    assert app.sent[0] == ("navigate", {"destination": "S-284", "arguments": {"charity": "1"}})


@pytest.mark.parametrize(
    "ref", ["card-transfer-cvv2", "card-transfer-password", "otp-code", "payment-card-cvv2"]
)
def test_a_value_the_plan_does_not_ask_for_is_refused(ref: str) -> None:
    app = FakeApp()
    report = run(app, {**CARD_VALUES, ref: "1234"})
    assert report.state == "stopped"
    assert app.sent == []


def test_a_row_key_that_spells_a_user_only_tag_never_reaches_the_app() -> None:
    app = FakeApp()
    plan = Navigator(PROFILE).plan(HOME, "S-158")
    report = run(app, {"digital-cheque": "confirm"}, plan, review=False)
    assert report.state == "stopped"
    assert app.sent == []


def test_no_feature_ever_sends_an_operation_outside_the_four() -> None:
    navigator = Navigator(PROFILE)
    for feature in PROFILE["features"]:
        app = FakeApp()
        plan = (
            navigator.plan_feature(HOME, feature)
            if feature["form"]
            else navigator.plan(HOME, feature["destination"])
        )
        values = {item.ref: "100" for item in plan.requirements if item.lookup is None}
        for item in plan.requirements:
            if item.lookup:
                values[item.ref] = FixtureLookups().candidates(item.lookup)[0].key
        run(app, values, plan, review=feature["form"] is not None)
        assert not {operation for operation, _ in app.sent} & set(FORBIDDEN), feature["intent"]
        assert {operation for operation, _ in app.sent} <= {"navigate", "fill_form", "show_review"}
