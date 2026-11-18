"""The ReAct agent's loop (D-032, D-033): the switch, code's understanding of a new request (fixed
replies, the workflow's chooser, not sure, knowledge), then think ⇄ act and their ends."""

import importlib

import pytest
from agent_fakes import PROFILE, FakeChat, FakeEmbeddings, RecordingApp
from langgraph.types import Command

from assistant import replies
from assistant.agent_nodes import Tools
from assistant.agent_state import new_request
from assistant.intent import FeatureIndex
from assistant.lookups import FixtureLookups
from assistant.models import Chat, ModelError
from assistant.navigator import Navigator
from assistant.react_graph import GREETING, HELP, NOT_SURE_OPTIONS, THANKS, build_agent, build_react_graph
from tools import evaluate_agent

CONFIG = {"configurable": {"thread_id": "react"}}
CARD = {"choice": "card_transfer"}
STOP = ModelError("chat: stop here")


class StepChat(FakeChat):
    """The scripted model, read through the real Chat.step."""

    step = Chat.step

    def json(self, prompt: str, schema: dict, tokens: int = 0) -> dict:
        return super().json(prompt, schema)


def call(tool: str, thought: str = "…", **given: object) -> dict:
    return {"thought": thought, "tool": tool, "arguments": given}


def react(chat: FakeChat, app: RecordingApp | None = None):
    app = app or RecordingApp()
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    tools = Tools(chat, index, Navigator(PROFILE), FixtureLookups(), app, app.current_screen)
    return build_react_graph(tools)


def chooser_calls(chat: FakeChat) -> int:
    """How many times the workflow's chooser was asked: its schema has a "choice"."""
    return sum("choice" in schema.get("properties", {}) for schema in chat.schemas)


@pytest.mark.parametrize(
    ("text", "reply"),
    [
        ("سلام", GREETING),
        ("سلام خوبی؟", GREETING),
        ("مرسی، خیلی کمک کردی", THANKS),
        ("ممنون!", THANKS),
        ("چیکار میتونی بکنی؟", HELP),
    ],
)
def test_a_greeting_thanks_or_what_can_you_do_gets_a_fixed_reply_and_no_model(text: str, reply: str) -> None:
    chat = StepChat([])
    app = RecordingApp()
    result = react(chat, app).invoke(new_request("c", text), CONFIG)
    assert result["reply"] == reply and result["task_state"] == "created"
    assert chat.prompts == [] and app.sent == []


def test_a_greeting_inside_a_request_is_the_request() -> None:
    chat = StepChat([CARD, STOP])
    react(chat).invoke(new_request("c", "سلام، کارت به کارت کن"), CONFIG)
    assert chooser_calls(chat) == 1


def test_a_knowledge_question_gets_the_workflows_reply() -> None:
    chat = StepChat([{"choice": "knowledge_question"}])
    result = react(chat).invoke(new_request("c", "کارمزد کارت به کارت چقدره؟"), CONFIG)
    assert result["reply"] == replies.KNOWLEDGE
    assert result["task_state"] == "created" and result["chosen"] == "knowledge_question"


def test_a_request_not_clear_asks_with_the_four_closest_features() -> None:
    chat = StepChat([{"choice": "not_sure"}, STOP])
    app = RecordingApp()
    graph = react(chat, app)
    result = graph.invoke(new_request("c", "یه چیزی درباره حسابم"), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"] == replies.NOT_SURE and pending["task_state"] == "created"
    assert len(pending["options"]) == NOT_SURE_OPTIONS + 1 and pending["options"][-1]["ref"] == "cancel"
    assert app.sent == []  # nothing is done while the request is not clear
    picked = pending["options"][0]["ref"]
    graph.invoke(Command(resume=picked), CONFIG)
    state = graph.get_state(CONFIG).values
    assert state["chosen"] == picked and state["plan"]["intent"] == picked
    assert state["steps"][1]["tool"] == "ask_user" and "picked" in state["steps"][1]["result"]


def test_new_words_at_the_not_sure_question_are_understood_again() -> None:
    chat = StepChat([{"choice": "not_sure"}, CARD, STOP])
    graph = react(chat)
    graph.invoke(new_request("c", "یه کاری داشتم"), CONFIG)
    graph.invoke(Command(resume="کارت به کارت"), CONFIG)
    state = graph.get_state(CONFIG).values
    assert chooser_calls(chat) == 2 and state["plan"]["intent"] == "card_transfer"


def test_cancel_at_the_not_sure_question_cancels() -> None:
    graph = react(StepChat([{"choice": "not_sure"}]))
    graph.invoke(new_request("c", "یه کاری داشتم"), CONFIG)
    result = graph.invoke(Command(resume="cancel"), CONFIG)
    assert result["task_state"] == "cancelled" and result["reply"] == replies.CANCELLED


def test_a_bare_answer_to_a_tasks_question_never_goes_to_the_chooser() -> None:
    values = {"card-transfer-source": None, "card-transfer-destination": "رضا", "card-transfer-amount": None}
    chat = StepChat(
        [
            CARD,
            call("set_values", values=values),
            {"card-transfer-source": None, "card-transfer-amount": None},  # the workflow's reader
            call("ask_user", question="؟"),
            STOP,
        ]
    )
    graph = react(chat)
    graph.invoke(new_request("c", "کارت به کارت به رضا"), CONFIG)
    graph.invoke(Command(resume="پس‌انداز"), CONFIG)  # the chooser would call it account_opening
    assert chooser_calls(chat) == 1
    assert graph.get_state(CONFIG).values["plan"]["intent"] == "card_transfer"


def test_the_model_never_sees_a_full_card_number() -> None:
    chat = StepChat([CARD, STOP])
    react(chat).invoke(new_request("c", "از کارت 6104336383233566 کارت به کارت کن"), CONFIG)
    assert all("6104336383233566" not in prompt for prompt in chat.prompts)
    assert "****3566" in chat.prompts[-1]


def test_a_model_failure_ends_the_task_as_failed() -> None:
    result = react(StepChat([ModelError("chat: ConnectError")])).invoke(
        new_request("c", "کارت به کارت"), CONFIG
    )
    assert result["task_state"] == "failed"
    assert result["reply"] == replies.MODEL_FAILED


def test_a_tool_the_task_does_not_allow_is_a_model_error() -> None:
    chat = StepChat([CARD, call("confirm")])
    result = react(chat).invoke(new_request("c", "کارت به کارت"), CONFIG)
    assert result["task_state"] == "failed"


def test_the_switch_picks_the_agent_and_refuses_any_other() -> None:
    app = RecordingApp()
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    tools = Tools(FakeChat([]), index, Navigator(PROFILE), FixtureLookups(), app, app.current_screen)
    assert "think" in build_agent(tools, "react").nodes
    assert "think" not in build_agent(tools, "workflow").nodes
    with pytest.raises(ValueError):
        build_agent(tools, "other")


def test_main_debug_runs_the_react_agent(
    monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture
) -> None:
    monkeypatch.setenv("ASSISTANT_TRACE", "")
    monkeypatch.setenv("ASSISTANT_DEBUG", "")
    main_debug = importlib.import_module("main_debug")
    result = main_debug.main(StepChat([]), FakeEmbeddings(), ["سلام"], agent="react")
    assert result["reply"] == GREETING
    assert f"assistant: {GREETING}" in capsys.readouterr().out


def test_the_evaluator_runs_a_case_through_the_react_agent() -> None:
    case = {"id": "T-1", "tags": ["knowledge"], "turns": ["سلام"], "expect": {"end": "created"}}
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    record = evaluate_agent.run_case(case, PROFILE, Navigator(PROFILE), StepChat([]), index, "react")
    assert record["crash"] is None
    assert record["reply"] == GREETING and record["task_state"] == "created"
    assert record["operations"] == []
    assert record["model_calls"] == 0 and record["seconds"] >= 0


def test_a_tool_without_arguments_can_end_right_after_its_name() -> None:
    chat = StepChat([{"thought": "…", "tool": "current_screen"}])
    step = chat.step(
        "p", {"current_screen": {"type": "object", "properties": {}}, "finish": {"properties": {"m": {}}}}
    )
    assert step["arguments"] == {}
    no_arguments, finish = chat.schemas[0]["anyOf"]
    assert "arguments" not in no_arguments["properties"] and no_arguments["required"] == ["thought", "tool"]
    assert finish["required"] == ["thought", "tool", "arguments"]
