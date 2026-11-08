"""Route finding and requirement collection, on a small profile and on the real one."""

import json
from pathlib import Path

import pytest

from assistant.navigator import HOME, CrossesUserOnly, Navigator, NoRoute

MONEY = {"ref": "amount", "type": "money", "requires": True, "ask": "چند ریال؟", "description": "مبلغ"}
OPTIONAL = {"ref": "note", "type": "text", "ask": "توضیح؟"}
SECRET = {"ref": "cvv2", "type": "text", "secret": True}
PICK = {"ref": "loan", "type": "ref", "ask": "کدام؟", "lookup": "loans"}

SMALL = {
    "user_only_actions": ["pay-*", "confirm"],
    "edges": [
        {"from": HOME, "to": "A", "action": "open-a"},
        {"from": "A", "to": "B", "action": "row-*", "requires": [PICK]},
        {"from": "B", "to": "C", "action": "next"},
        {"from": "C", "to": "R", "action": "confirm"},
        {"from": "A", "to": "D", "action": "direct"},
        {"from": "D", "to": "E", "action": "tap > pay-now"},
    ],
    "forms": [{"id": "B", "fields": [MONEY, OPTIONAL, SECRET]}],
    "features": [{"intent": "x", "destination": "C", "form": "B"}],
}


@pytest.fixture
def navigator() -> Navigator:
    return Navigator(SMALL)


def test_a_direct_route(navigator: Navigator) -> None:
    plan = navigator.plan(HOME, "A")
    assert [edge["action"] for edge in plan.route] == ["open-a"]
    assert plan.requirements == []


def test_a_multi_step_route_lists_its_selection_and_fields_in_order(navigator: Navigator) -> None:
    plan = navigator.plan(HOME, "C")
    assert [edge["to"] for edge in plan.route] == ["A", "B", "C"]
    assert [(item.kind, item.ref, item.ask) for item in plan.requirements] == [
        ("selection", "loan", "کدام؟"),
        ("field", "amount", "چند ریال؟"),
    ]
    assert plan.requirements[0].lookup == "loans"


def test_screens_without_fields_pass_inside_navigate(navigator: Navigator) -> None:
    legs = navigator.plan(HOME, "C").legs
    assert [leg.screen for leg in legs] == ["B", "C"]
    assert [item.ref for item in legs[0].selections] == ["loan"]


def test_an_unreachable_target_is_no_route(navigator: Navigator) -> None:
    with pytest.raises(NoRoute):
        navigator.plan(HOME, "nowhere")


@pytest.mark.parametrize("target", ["R", "E"])
def test_a_route_that_crosses_a_user_only_action_is_refused(navigator: Navigator, target: str) -> None:
    with pytest.raises(CrossesUserOnly):
        navigator.plan(HOME, target)


def test_a_secret_field_is_never_a_requirement(navigator: Navigator) -> None:
    assert "cvv2" not in [item.ref for item in navigator.plan(HOME, "B").requirements]


def test_an_optional_field_is_not_asked(navigator: Navigator) -> None:
    assert "note" not in [item.ref for item in navigator.plan(HOME, "B").requirements]


def test_no_forward_route_goes_back_by_way_of_home(navigator: Navigator) -> None:
    plan = navigator.plan("D", "B")
    assert plan.route[0] == {"from": "D", "to": HOME, "action": "back"}
    assert [edge["to"] for edge in plan.route] == [HOME, "A", "B"]


def test_a_row_key_that_spells_a_user_only_tag_is_not_allowed(navigator: Navigator) -> None:
    edge = {"from": "X", "to": "Y", "action": "pay-*"}
    assert not navigator.key_is_allowed(edge, "now")


def test_a_feature_routes_through_its_form() -> None:
    profile = json.loads(Path("app_profiles/replica.profile.json").read_text(encoding="utf-8"))
    navigator = Navigator(profile)
    feature = next(item for item in profile["features"] if item["intent"] == "sayad_transfer")
    assert "S-172" in [edge["to"] for edge in navigator.plan_feature(HOME, feature).route]
