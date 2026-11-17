"""The ReAct agent's read tools (D-032): features, requirements, records and the current screen."""

import json

from agent_fakes import PROFILE, FakeChat, FakeEmbeddings, RecordingApp
from langgraph.types import Command

from assistant.agent_nodes import Tools
from assistant.agent_state import new_request
from assistant.intent import FeatureIndex
from assistant.lookups import FixtureLookups
from assistant.models import Chat, ModelError
from assistant.navigator import Navigator
from assistant.react_graph import STEP_BUDGET, TOO_MANY_STEPS, build_react_graph

CONFIG = {"configurable": {"thread_id": "react"}}
REZA_CARD = "6037990836388880"


class StepChat(FakeChat):
    """The scripted model, read through the real Chat.step."""

    step = Chat.step

    def json(self, prompt: str, schema: dict, tokens: int = 0) -> dict:
        return super().json(prompt, schema)


def call(tool: str, **given: object) -> dict:
    return {"thought": "…", "tool": tool, "arguments": given}


def react(chat: FakeChat, app: RecordingApp | None = None):
    app = app or RecordingApp()
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    tools = Tools(chat, index, Navigator(PROFILE), FixtureLookups(), app, app.current_screen)
    return build_react_graph(tools)


def results(state: dict) -> list[dict]:
    return [step.get("result") for step in state["steps"]]


def test_a_card_transfer_to_reza_reads_the_feature_its_fields_and_reza() -> None:
    chat = StepChat(
        [
            call("find_features", text="کارت به کارت"),
            call("feature_requirements", intent="card_transfer"),
            call("find_records", ref="card-transfer-destination", words="رضا"),
            call("current_screen"),
            ModelError("chat: stop here"),
        ]
    )
    app = RecordingApp()
    result = react(chat, app).invoke(new_request("c", "کارت به کارت به رضا"), CONFIG)
    found, planned, records, screen = results(result)
    assert found["features"][0] == {
        "intent": "card_transfer",
        "title": "کارت به کارت",
        "words": found["features"][0]["words"],
    }
    assert result["offered"][0] == "card_transfer" and result["chosen"] == "card_transfer"
    assert planned["feature"] == "کارت به کارت"
    assert [item["ref"] for item in planned["fields"]] == [
        "card-transfer-source",
        "card-transfer-destination",
        "card-transfer-amount",
    ]
    assert planned["fields"][0]["question"] == "از کدام کارت انتقال انجام شود؟"
    assert records == {"records": ["رضا محمدی ****8880"], "count": 1}
    assert screen == {"page": "خانه", "filled": {}}
    # Reading changes nothing in the app.
    assert app.sent == []
    # The model was told what it read, and never a full number, an app key or a screen ID.
    seen = "\n".join(chat.prompts)
    assert "رضا محمدی ****8880" in seen and REZA_CARD not in seen
    assert "card-1" not in seen and "S-174" not in seen and "S-026" not in seen


def test_find_records_is_offered_only_for_a_planned_record_field() -> None:
    chat = StepChat([call("feature_requirements", intent="card_transfer"), ModelError("chat: stop here")])
    react(chat).invoke(new_request("c", "کارت به کارت"), CONFIG)
    before = [branch["properties"]["tool"]["const"] for branch in chat.schemas[0]["anyOf"]]
    after = {branch["properties"]["tool"]["const"]: branch for branch in chat.schemas[1]["anyOf"]}
    assert "find_records" not in before
    refs = after["find_records"]["properties"]["arguments"]["properties"]["ref"]["enum"]
    assert refs == ["card-transfer-source", "card-transfer-destination"]


def test_find_records_lists_only_what_the_apps_picker_lists() -> None:
    chat = StepChat(
        [
            call("feature_requirements", intent="card_transfer"),
            call("find_records", ref="card-transfer-source", words=""),
            ModelError("chat: stop here"),
        ]
    )
    result = react(chat).invoke(new_request("c", "کارت به کارت"), CONFIG)
    # Only the active debit cards, as for the workflow's question.
    assert result["steps"][1]["result"] == {"records": ["****3566", "****4544"], "count": 2}


def test_a_choice_shows_its_labels_never_its_keys() -> None:
    chat = StepChat([call("feature_requirements", intent="internal_transfer"), ModelError("chat: stop here")])
    result = react(chat).invoke(new_request("c", "انتقال داخلی"), CONFIG)
    fields = {item["ref"]: item for item in result["steps"][0]["result"]["fields"]}
    reason = fields["account-transfer-reason"]
    assert "پرداخت اجاره" in reason["options"]
    shown = json.dumps(result["steps"][0]["result"], ensure_ascii=False)
    keys = [option["key"] for option in result["plan"]["requirements"][-1]["options"]]
    assert result["plan"]["requirements"][-1]["ref"] == "account-transfer-reason"
    assert not any(f'"{key}"' in shown for key in keys if key not in reason["options"])


def test_current_screen_names_the_page_never_its_id() -> None:
    chat = StepChat([call("current_screen"), call("finish", message="خانه هستید")])
    result = react(chat).invoke(new_request("c", "کجام؟"), CONFIG)
    assert result["steps"][0]["result"] == {"page": "خانه"}


def test_a_turn_ends_after_the_step_budget() -> None:
    chat = StepChat([call("current_screen")] * STEP_BUDGET)
    app = RecordingApp()
    result = react(chat, app).invoke(new_request("c", "کجام؟"), CONFIG)
    assert len(result["steps"]) == STEP_BUDGET
    assert result["task_state"] == "failed" and result["reply"] == TOO_MANY_STEPS
    assert app.sent == []


def test_the_budget_counts_again_after_the_users_answer() -> None:
    chat = StepChat(
        [call("current_screen")] * (STEP_BUDGET - 1)
        + [call("ask_user", question="کدام؟")]
        + [call("current_screen")] * 3
        + [call("finish", message="باشه")]
    )
    graph = react(chat)
    graph.invoke(new_request("c", "کجام؟"), CONFIG)
    result = graph.invoke(Command(resume="خانه"), CONFIG)
    assert result["reply"] == "باشه"
