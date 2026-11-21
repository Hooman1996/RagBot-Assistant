"""The ReAct agent's read tools and its view of the task (D-032, D-036): the started feature's fields,
records and the current screen, the step budget and repeated calls; nothing shown in full."""

from agent_fakes import PROFILE, FakeChat, FakeEmbeddings, RecordingApp
from langgraph.types import Command

from assistant.agent_nodes import Tools
from assistant.agent_state import new_request
from assistant.intent import FeatureIndex
from assistant.lookups import FixtureLookups
from assistant.models import Chat, ModelError
from assistant.navigator import Navigator
from assistant.react_graph import REPEATED, STEP_BUDGET, TOO_MANY_STEPS, build_react_graph

CONFIG = {"configurable": {"thread_id": "react"}}
STOP = ModelError("chat: stop here")
REZA_CARD = "6037990836388880"


class StepChat(FakeChat):
    """The scripted model, read through the real Chat.step."""

    step = Chat.step

    def json(self, prompt: str, schema: dict, tokens: int = 0) -> dict:
        return super().json(prompt, schema)


def call(tool: str, **given: object) -> dict:
    return {"thought": "…", "tool": tool, "arguments": given}


def started(feature: str = "card_transfer", read: dict | None = None) -> list[dict]:
    """The model's two steps that start a feature, the features found then the one started, and the
    workflow's reading of the request that start_task does (none for a page to see)."""
    title = next(item["title"] for item in PROFILE["features"] if item["intent"] == feature)
    return [
        call("find_features", request=title),
        call("start_task", feature=feature, request=title, second=None),
    ] + ([] if feature == "home" else [read or {}])


def react(chat: FakeChat, app: RecordingApp | None = None):
    app = app or RecordingApp()
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    tools = Tools(chat, index, Navigator(PROFILE), FixtureLookups(), app, app.current_screen)
    return build_react_graph(tools)


def tools_offered(chat: FakeChat, step_number: int) -> list[str]:
    steps = [schema for schema in chat.schemas if "anyOf" in schema]
    return [branch["properties"]["tool"]["const"] for branch in steps[step_number]["anyOf"]]


def found_intents(found: dict) -> list[str]:
    return [item["feature"] for item in found["features"]]


def test_the_model_starts_a_found_feature_and_reads_records_and_the_screen() -> None:
    chat = StepChat(
        [
            *started(),
            call("find_records", ref="card-transfer-destination", words="رضا"),
            call("current_screen"),
            STOP,
        ]
    )
    app = RecordingApp()
    result = react(chat, app).invoke(new_request("c", "کارت به کارت به رضا"), CONFIG)
    found, began, records, screen = (step["result"] for step in result["steps"])
    assert {"feature": "card_transfer", "title": "کارت به کارت"}.items() <= found["features"][0].items()
    # A feature whose title is in the user's words is marked, and only that one.
    assert [item["feature"] for item in found["features"] if item.get("named")] == ["card_transfer"]
    assert began["started"] == "کارت به کارت"
    assert result["chosen"] == "card_transfer" and result["plan"]["intent"] == "card_transfer"
    assert records == {"records": ["رضا محمدی ****8880"], "count": 1}
    assert screen == {"page": "خانه", "filled": {}}
    assert app.sent == []  # reading changes nothing in the app
    first = chat.prompts[3]
    assert "«کارت به کارت»" in first and "از کدام کارت انتقال انجام شود؟" in first
    # A feature is started only from those find_features found, and no tool finishes past the review.
    assert "start_task" not in tools_offered(chat, 0) and "start_task" in tools_offered(chat, 1)
    start = next(b for b in chat.schemas[1]["anyOf"] if b["properties"]["tool"]["const"] == "start_task")
    assert start["properties"]["arguments"]["properties"]["feature"]["enum"] == found_intents(found)
    seen = "\n".join(chat.prompts)
    assert "رضا محمدی ****8880" in seen and REZA_CARD not in seen
    assert "card-1" not in seen and "S-174" not in seen and "S-026" not in seen


def test_find_records_is_offered_for_the_tasks_record_fields() -> None:
    chat = StepChat([*started(), STOP])
    react(chat).invoke(new_request("c", "کارت به کارت"), CONFIG)
    offered = {branch["properties"]["tool"]["const"]: branch for branch in chat.schemas[3]["anyOf"]}
    refs = offered["find_records"]["properties"]["arguments"]["properties"]["ref"]["enum"]
    assert refs == ["card-transfer-source", "card-transfer-destination"]


def test_find_records_lists_only_what_the_apps_picker_lists() -> None:
    chat = StepChat([*started(), call("find_records", ref="card-transfer-source", words=""), STOP])
    result = react(chat).invoke(new_request("c", "کارت به کارت"), CONFIG)
    # Only the active debit cards, as for the workflow's question.
    assert result["steps"][2]["result"] == {"records": ["****3566", "****4544"], "count": 2}


def test_a_choice_shows_its_labels_never_its_keys() -> None:
    chat = StepChat([*started("internal_transfer"), STOP])
    result = react(chat).invoke(new_request("c", "انتقال داخلی"), CONFIG)
    reason = next(item for item in result["plan"]["requirements"] if item["ref"] == "account-transfer-reason")
    labels = [option["label"] for option in reason["options"]]
    keys = [option["key"] for option in reason["options"] if option["key"] not in labels]
    assert "پرداخت اجاره" in labels and "پرداخت اجاره" in chat.prompts[3]
    shape = chat.schemas[3]["anyOf"]
    values = next(b for b in shape if b["properties"]["tool"]["const"] == "set_values")
    reason_shape = values["properties"]["arguments"]["properties"]["values"]["properties"][
        "account-transfer-reason"
    ]
    assert reason_shape["anyOf"][0] == {"enum": labels}
    assert not any(f'"{key}"' in chat.prompts[3] for key in keys)


def test_a_turn_ends_after_the_step_budget() -> None:
    reading = [call("current_screen"), call("find_records", ref="card-transfer-source", words="")]
    chat = StepChat(started() + reading * 5)
    app = RecordingApp()
    result = react(chat, app).invoke(new_request("c", "کارت به کارت"), CONFIG)
    assert len(result["steps"]) == STEP_BUDGET
    assert result["task_state"] == "failed" and result["reply"] == TOO_MANY_STEPS
    assert app.sent == []


def test_the_budget_counts_again_after_the_users_answer() -> None:
    reading = [call("current_screen"), call("find_records", ref="card-transfer-source", words="")]
    nothing = {"card-transfer-source": None, "card-transfer-destination": None, "card-transfer-amount": None}
    chat = StepChat(
        started()
        + reading * 3
        + [call("ask_user", question="؟")]
        + [call("set_values", values=nothing), nothing]
        + reading * 2
        + [STOP]
    )
    graph = react(chat)
    graph.invoke(new_request("c", "کارت به کارت"), CONFIG)
    graph.invoke(Command(resume="نمیدونم"), CONFIG)
    state = graph.get_state(CONFIG).values
    assert len(state["steps"]) > STEP_BUDGET and state["reply"] != TOO_MANY_STEPS


def test_the_same_call_twice_is_not_run_again() -> None:
    chat = StepChat([*started(), call("current_screen"), call("current_screen"), STOP])
    result = react(chat).invoke(new_request("c", "کارت به کارت"), CONFIG)
    assert result["steps"][3]["result"] == {"error": REPEATED}


def test_a_wallet_feature_is_found_only_once_the_user_says_kif() -> None:
    for text, wallet in (
        ("خرید شارژ برای 09121234567", False),
        ("با کیف پول خرید شارژ برای 09121234567", True),
    ):
        chat = StepChat([call("find_features", request=text), STOP])
        result = react(chat).invoke(new_request("c", text), CONFIG)
        found = found_intents(result["steps"][0]["result"])
        assert "mobile_top_up" in found
        assert ("wallet_mobile_top_up" in found) is wallet
