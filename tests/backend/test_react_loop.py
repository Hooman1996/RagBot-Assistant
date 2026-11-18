"""The ReAct agent's loop (D-032): the switch, think ⇄ act, ask_user, finish and their ends."""

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
from assistant.react_graph import build_agent, build_react_graph
from tools import evaluate_agent

CONFIG = {"configurable": {"thread_id": "react"}}


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


def test_a_greeting_is_answered_through_the_loop_with_nothing_sent() -> None:
    chat = StepChat([call("finish", "سلام کرد.", message="سلام! چه کاری برایتان انجام دهم؟")])
    app = RecordingApp()
    result = react(chat, app).invoke(new_request("c", "سلام"), CONFIG)
    assert result["reply"] == "سلام! چه کاری برایتان انجام دهم؟"
    assert result["task_state"] == "created"
    assert result["steps"] == [call("finish", "سلام کرد.", message=result["reply"]) | {"result": {}}]
    assert app.sent == []
    # The model is held to the tools it may call, each by its own branch of the schema.
    branches = chat.schemas[0]["anyOf"]
    names = [branch["properties"]["tool"]["const"] for branch in branches]
    assert names == ["find_features", "feature_requirements", "current_screen", "ask_user", "finish"]
    assert "«سلام»" in chat.prompts[0]


def test_ask_user_waits_and_the_answer_goes_back_to_the_model() -> None:
    chat = StepChat(
        [
            call("ask_user", question="چه کاری انجام دهم؟"),
            call("finish", message="باشد."),
        ]
    )
    graph = react(chat)
    result = graph.invoke(new_request("c", "یه کاری داشتم"), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending == {
        "question": "چه کاری انجام دهم؟",
        "options": [{"ref": "cancel", "label": "لغو"}],
        "fields": [],
        "task_state": "created",
    }
    result = graph.invoke(Command(resume="هیچی"), CONFIG)
    assert result["reply"] == "باشد."
    assert result["messages"] == ["یه کاری داشتم", "هیچی"]
    assert result["steps"][0]["result"] == {"answer": "هیچی"}
    assert '{"answer": "هیچی"}' in chat.prompts[1]


def test_cancel_at_ask_user_cancels() -> None:
    graph = react(StepChat([call("ask_user", question="کدام؟")]))
    graph.invoke(new_request("c", "یه کاری داشتم"), CONFIG)
    result = graph.invoke(Command(resume="cancel"), CONFIG)
    assert result["task_state"] == "cancelled"
    assert result["reply"] == replies.CANCELLED


def test_the_model_never_sees_a_full_card_number() -> None:
    chat = StepChat([call("finish", message="شماره 6104336383233566 دیده شد")])
    result = react(chat).invoke(new_request("c", "کارت 6104336383233566 من"), CONFIG)
    assert "6104336383233566" not in chat.prompts[0]
    assert "****3566" in chat.prompts[0]
    assert "6104336383233566" not in result["reply"]


def test_an_empty_message_is_refused_and_the_model_goes_on() -> None:
    chat = StepChat([call("finish", message=" "), call("finish", message="سلام")])
    result = react(chat).invoke(new_request("c", "سلام"), CONFIG)
    assert result["reply"] == "سلام"
    assert result["steps"][0]["result"] == {"error": "the message is empty"}


def test_a_model_failure_ends_the_task_as_failed() -> None:
    result = react(StepChat([ModelError("chat: ConnectError")])).invoke(new_request("c", "سلام"), CONFIG)
    assert result["task_state"] == "failed"
    assert result["reply"] == replies.MODEL_FAILED


def test_a_tool_the_state_does_not_allow_is_a_model_error() -> None:
    chat = StepChat([call("confirm")])
    result = react(chat).invoke(new_request("c", "تأیید کن"), CONFIG)
    assert result["task_state"] == "failed"


def test_the_switch_picks_the_agent_and_refuses_any_other() -> None:
    app = RecordingApp()
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    tools = Tools(FakeChat([]), index, Navigator(PROFILE), FixtureLookups(), app, app.current_screen)
    assert "think" in build_agent(tools, "react").nodes
    assert "understand" in build_agent(tools, "workflow").nodes
    with pytest.raises(ValueError):
        build_agent(tools, "other")


def test_main_debug_runs_the_react_agent(
    monkeypatch: pytest.MonkeyPatch, capsys: pytest.CaptureFixture
) -> None:
    monkeypatch.setenv("ASSISTANT_TRACE", "")
    monkeypatch.setenv("ASSISTANT_DEBUG", "")
    main_debug = importlib.import_module("main_debug")
    chat = StepChat([call("finish", message="سلام!")])
    result = main_debug.main(chat, FakeEmbeddings(), ["سلام"], agent="react")
    assert result["reply"] == "سلام!"
    assert "assistant: سلام!" in capsys.readouterr().out


def test_the_evaluator_runs_a_case_through_the_react_agent() -> None:
    case = {"id": "T-1", "tags": ["knowledge"], "turns": ["سلام"], "expect": {"end": "created"}}
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    chat = StepChat([call("finish", message="سلام!")])
    record = evaluate_agent.run_case(case, PROFILE, Navigator(PROFILE), chat, index, "react")
    assert record["crash"] is None
    assert record["reply"] == "سلام!" and record["task_state"] == "created"
    assert record["operations"] == []
    assert record["model_calls"] == 1 and record["seconds"] >= 0


def test_a_tool_without_arguments_can_end_right_after_its_name() -> None:
    chat = StepChat([{"thought": "…", "tool": "current_screen"}])
    step = chat.step(
        "p", {"current_screen": {"type": "object", "properties": {}}, "finish": {"properties": {"m": {}}}}
    )
    assert step["arguments"] == {}
    no_arguments, finish = chat.schemas[0]["anyOf"]
    assert "arguments" not in no_arguments["properties"] and no_arguments["required"] == ["thought", "tool"]
    assert finish["required"] == ["thought", "tool", "arguments"]
