"""The ReAct agent's loop (D-032, D-036): the fast path at door A, then the model reads every message
in the conversation: chat replies, a feature found and started, which one is meant, an answer, chat or
a secret at a question, a switch of task, a cancel, a request that refers to an earlier one, and
the loop's ends."""

import importlib

import pytest
from agent_fakes import PROFILE, FakeChat, FakeEmbeddings, RecordingApp
from langgraph.types import Command

from assistant import replies
from assistant.agent_nodes import Tools
from assistant.agent_state import new_request
from assistant.intent import KNOWLEDGE, FeatureIndex
from assistant.lifecycle import next_input
from assistant.lookups import FixtureLookups
from assistant.models import Chat, ModelError
from assistant.navigator import Navigator
from assistant.react_graph import AT_REVIEW, build_agent, build_react_graph
from assistant.react_tools import NEXT_ANSWER, NEXT_PICKED
from tools import evaluate_agent

CONFIG = {"configurable": {"thread_id": "react"}}
STOP = ModelError("chat: stop here")
CARD_SENTENCE = "۲۵۰ هزار تومان از کارت 6104336383233566 به رضا کارت به کارت کن"
HELLO = "سلام! بفرمایید، چه کاری انجام دهم؟"
SOURCE_QUESTION = "از کدام کارت انتقال انجام شود؟"
NO_SOURCE = {"card-transfer-source": None, "card-transfer-destination": "رضا", "card-transfer-amount": None}


class StepChat(FakeChat):
    """The scripted model, read through the real Chat.step."""

    step = Chat.step

    def json(self, prompt: str, schema: dict, tokens: int = 0) -> dict:
        return super().json(prompt, schema)


def call(tool: str, thought: str = "…", **given: object) -> dict:
    return {"thought": thought, "tool": tool, "arguments": given}


def started(feature: str, request: str | None = None, read: dict | None = None) -> list[dict]:
    """The model's two steps that start a feature, the features found then the one started, and the
    workflow's reading of the request that start_task does (none for a page to see)."""
    title = next(item["title"] for item in PROFILE["features"] if item["intent"] == feature)
    return [
        call("find_features", request=title),
        call("start_task", feature=feature, request=request or title, second=None),
    ] + ([] if feature == "home" else [read or {}])


def asking_for_the_source() -> list[dict]:
    """A card transfer to رضا whose source (and amount) is asked."""
    return [
        *started("card_transfer"),
        call("set_values", values=NO_SOURCE),
        NO_SOURCE,
        call("ask_user", question="؟"),
    ]


def at_the_review() -> list[dict]:
    """A card transfer prepared to its review in one sentence."""
    values = NO_SOURCE | {
        "card-transfer-source": "****3566",
        "card-transfer-amount": {"value": 250000, "unit": "IRT"},
    }
    return [*started("card_transfer"), call("set_values", values=values), values, call("open_review")]


def react(chat: FakeChat, app: RecordingApp | None = None):
    app = app or RecordingApp()
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    tools = Tools(chat, index, Navigator(PROFILE), FixtureLookups(), app, app.current_screen)
    return build_react_graph(tools)


def tools_offered(chat: FakeChat, step_number: int) -> list[str]:
    steps = [schema for schema in chat.schemas if "anyOf" in schema]
    return [branch["properties"]["tool"]["const"] for branch in steps[step_number]["anyOf"]]


@pytest.mark.parametrize(
    "text", ["سلام", "مرسی، خیلی کمک کردی", "کارمزد کارت به کارت چقدره؟", "بلیت هواپیما بخر"]
)
def test_chat_gets_the_models_own_reply_and_starts_nothing(text: str) -> None:
    chat = StepChat([call("reply", text=HELLO)])
    app = RecordingApp()
    result = react(chat, app).invoke(new_request("c", text), CONFIG)
    assert result["reply"] == HELLO and result["task_state"] == "created"
    assert result["chosen"] == KNOWLEDGE  # answered, nothing started
    assert app.sent == []
    assert tools_offered(chat, 0) == ["reply", "find_features", "current_screen"]


@pytest.mark.parametrize("said", ["کارت به کارت انجام شد.", "کارمزدش ۵۰۰ تومان است.", "کارمزد 500 است"])
def test_a_reply_with_a_number_or_a_claim_is_replaced_by_the_safe_line(said: str) -> None:
    result = react(StepChat([call("reply", text=said)])).invoke(new_request("c", "سلام"), CONFIG)
    assert result["reply"] == replies.CHAT_SAFE


@pytest.mark.parametrize(
    ("text", "reply"),
    [
        ("رمز دوم کارتم 482913 هست", replies.NO_SECRETS),
        ("تایید کن", replies.CONFIRM_BY_TOUCH),
        ("لغو", replies.NOTHING_OPEN),
    ],
)
def test_the_fast_path_answers_a_secret_a_confirm_or_a_cancel_with_no_model(text: str, reply: str) -> None:
    chat = StepChat([])
    result = react(chat).invoke(new_request("c", text), CONFIG)
    assert result["reply"] == reply and chat.prompts == []
    assert "482913" not in str(result["turns"]) and "482913" not in str(result["messages"])


def test_a_request_not_clear_asks_which_feature_and_a_pick_goes_back_to_the_model() -> None:
    which = ["internal_transfer", "interbank_transfer", "card_transfer"]
    chat = StepChat(
        [
            call("find_features", request="یه کاری با حسابم دارم"),
            call("ask_which", features=which),
            call("start_task", feature="card_transfer", request="…", second=None),
            {},  # the workflow's reading of the request
            STOP,
        ]
    )
    app = RecordingApp()
    graph = react(chat, app)
    result = graph.invoke(new_request("c", "یه کاری با حسابم دارم"), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"] == replies.NOT_SURE and pending["task_state"] == "created"
    assert [option["ref"] for option in pending["options"]] == which + ["cancel"]
    assert app.sent == []  # nothing is done while the request is not clear
    graph.invoke(Command(resume="card_transfer"), CONFIG)
    state = graph.get_state(CONFIG).values
    assert state["steps"][1]["result"] == {"picked": "کارت به کارت", "next": NEXT_PICKED}
    assert state["plan"]["intent"] == "card_transfer"


def test_cancel_or_new_words_at_the_which_question() -> None:
    asked = [call("find_features", request="یه کاری"), call("ask_which", features=["home", "card_transfer"])]
    graph = react(StepChat(list(asked)))
    graph.invoke(new_request("c", "یه کاری داشتم"), CONFIG)
    result = graph.invoke(Command(resume="cancel"), CONFIG)
    assert result["task_state"] == "cancelled" and result["reply"] == replies.CANCELLED
    graph = react(StepChat([*asked, STOP]))
    graph.invoke(new_request("c", "یه کاری داشتم"), CONFIG)
    graph.invoke(Command(resume="کارت به کارت"), CONFIG)
    state = graph.get_state(CONFIG).values
    assert state["steps"][1]["result"] == {"answer": "کارت به کارت", "next": NEXT_ANSWER}
    assert state["messages"] == ["یه کاری داشتم", "کارت به کارت"]


def test_a_typed_answer_to_a_tasks_question_goes_to_the_model_with_the_conversation() -> None:
    chat = StepChat([*asking_for_the_source(), STOP])
    graph = react(chat)
    graph.invoke(new_request("c", "کارت به کارت به رضا"), CONFIG)
    graph.invoke(Command(resume="پس‌انداز"), CONFIG)
    state = graph.get_state(CONFIG).values
    assert state["plan"]["intent"] == "card_transfer"
    assert state["steps"][-1]["result"] == {"answer": "پس‌انداز", "next": NEXT_ANSWER}
    assert "- user: «پس‌انداز»" in chat.prompts[-1] and SOURCE_QUESTION in chat.prompts[-1]
    # A task the model just started is not offered to be cancelled until the user speaks again.
    assert "cancel_task" not in tools_offered(chat, 2) and "cancel_task" in tools_offered(chat, 4)


def test_chat_in_the_middle_of_a_task_is_replied_to_and_its_question_asked_again() -> None:
    chat = StepChat(
        [
            *asking_for_the_source(),
            call("reply", text="سلام! در خدمتم."),  # code asks the task's question right after it
            {"card-transfer-source": None, "card-transfer-amount": None},  # the reader reads the new words
        ]
    )
    graph = react(chat)
    graph.invoke(new_request("c", "کارت به کارت به رضا"), CONFIG)
    result = graph.invoke(Command(resume="سلام خوبی؟"), CONFIG)
    question = result["__interrupt__"][0].value["question"]
    assert question.startswith("سلام! در خدمتم.\n") and SOURCE_QUESTION in question
    # A secret typed next is refused, and the question comes back without the earlier reply.
    result = graph.invoke(Command(resume="رمز دومم 482913"), CONFIG)
    again = result["__interrupt__"][0].value["question"]
    assert again.startswith(replies.NO_SECRETS + "\n") and "در خدمتم" not in again


def test_a_secret_typed_at_a_question_is_refused_and_the_question_asked_again_with_no_model() -> None:
    chat = StepChat([*asking_for_the_source(), STOP])
    graph = react(chat)
    graph.invoke(new_request("c", "کارت به کارت به رضا"), CONFIG)
    prompts = len(chat.prompts)
    result = graph.invoke(Command(resume="رمز دومم 482913 هست"), CONFIG)
    question = result["__interrupt__"][0].value["question"]
    assert question.startswith(replies.NO_SECRETS + "\n") and SOURCE_QUESTION in question
    assert len(chat.prompts) == prompts  # the model never read it
    result = graph.invoke(Command(resume="card-transfer-source=card-1"), CONFIG)
    state = graph.get_state(CONFIG).values
    assert state["values"]["card-transfer-source"] == "card-1"
    assert all("482913" not in prompt for prompt in chat.prompts)
    assert "482913" not in str(state["turns"]) and "482913" not in str(state["messages"])


def test_a_confirm_request_at_the_which_question_is_refused_and_it_is_asked_again() -> None:
    chat = StepChat(
        [call("find_features", request="یه کاری"), call("ask_which", features=["home", "card_transfer"])]
    )
    graph = react(chat)
    graph.invoke(new_request("c", "یه کاری داشتم"), CONFIG)
    result = graph.invoke(Command(resume="پرداختش کن"), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"] == replies.CONFIRM_BY_TOUCH + "\n" + replies.NOT_SURE
    assert [option["ref"] for option in pending["options"]] == ["home", "card_transfer", "cancel"]


def test_a_new_task_while_one_is_open_asks_the_switch_question() -> None:
    switching = [
        *asking_for_the_source(),
        *started("bill_payment", "قبض برقمو بده"),
    ]
    chat = StepChat([*switching, STOP])
    graph = react(chat)
    graph.invoke(new_request("c", "کارت به کارت به رضا"), CONFIG)
    result = graph.invoke(Command(resume="قبض برقمو بده"), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"] == replies.SWITCH.format(old="کارت به کارت", new="پرداخت قبض")
    assert [option["ref"] for option in pending["options"]] == ["switch=yes", "switch=no", "cancel"]
    graph.invoke(Command(resume="switch=yes"), CONFIG)
    state = graph.get_state(CONFIG).values
    assert state["plan"]["intent"] == "bill_payment" and state["messages"] == ["قبض برقمو بده"]
    # «نه» keeps the open task.
    graph = react(StepChat([*switching, STOP]))
    graph.invoke(new_request("c", "کارت به کارت به رضا"), CONFIG)
    graph.invoke(Command(resume="قبض برقمو بده"), CONFIG)
    graph.invoke(Command(resume="switch=no"), CONFIG)
    state = graph.get_state(CONFIG).values
    assert state["plan"]["intent"] == "card_transfer" and state["steps"][-1]["result"]["switch"] == "no"


def test_the_model_cancels_the_open_task() -> None:
    graph = react(StepChat([*asking_for_the_source(), call("cancel_task")]))
    graph.invoke(new_request("c", "کارت به کارت به رضا"), CONFIG)
    result = graph.invoke(Command(resume="اصلا پشیمون شدم، ادامه نده"), CONFIG)
    assert result["task_state"] == "cancelled" and result["reply"] == replies.CANCELLED_OPEN


@pytest.mark.parametrize(
    ("request_", "kept"),
    [
        (
            "۲۵۰ هزار تومان از کارت ****3566 به امید کارت به کارت کن",
            "250 هزار تومان از کارت 6104336383233566 به امید کارت به کارت کن",
        ),
        (
            "۹۰۰ هزار تومان به امید کارت به کارت کن",
            "همونو برای امید هم بزن",
        ),  # an amount the user never wrote
    ],
)
def test_a_request_that_refers_to_an_earlier_one_keeps_only_the_users_own_words(
    request_: str, kept: str
) -> None:
    chat = StepChat([*at_the_review(), *started("card_transfer", request_), STOP])
    graph = react(chat)
    graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    previous = graph.get_state(CONFIG).values
    result = graph.invoke(next_input(previous, "c", "همونو برای امید هم بزن"), CONFIG)
    assert result["__interrupt__"][0].value["options"][0]["ref"] == "switch=yes"  # the review is open
    graph.invoke(Command(resume="switch=yes"), CONFIG)
    assert graph.get_state(CONFIG).values["messages"] == [kept]


def test_the_model_sees_the_conversation_across_requests() -> None:
    chat = StepChat([call("reply", text=HELLO), STOP])
    graph = react(chat)
    graph.invoke(new_request("c", "سلام"), CONFIG)
    previous = graph.get_state(CONFIG).values
    graph.invoke(next_input(previous, "c", "کارت به کارت"), CONFIG)
    assert f"- user: «سلام»\n- assistant: «{HELLO}»\n- user: «کارت به کارت»" in chat.prompts[-1]


def test_a_model_failure_at_the_review_keeps_the_review_and_the_review_rule_is_said_only_there() -> None:
    chat = StepChat([*at_the_review(), STOP])
    graph = react(chat)
    graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    assert not any(AT_REVIEW in prompt for prompt in chat.prompts)
    previous = graph.get_state(CONFIG).values
    result = graph.invoke(next_input(previous, "c", "یه چیزی بگم"), CONFIG)
    assert result["task_state"] == "ready_for_user" and result["reply"] == replies.MODEL_FAILED
    assert AT_REVIEW in chat.prompts[-1]


def test_a_task_the_user_took_over_ends_before_the_model_reads_the_next_message() -> None:
    # P6-12: the user confirmed the review by touch (or left it); the app is no longer on it.
    chat = StepChat([*at_the_review(), call("reply", text=HELLO)])
    app = RecordingApp()
    graph = react(chat, app)
    graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    app.screen = "S-026"
    previous = graph.get_state(CONFIG).values
    result = graph.invoke(next_input(previous, "c", "مرسی"), CONFIG)
    assert result["task_state"] == "created" and result["plan"] is None and result["reply"] == HELLO
    assert tools_offered(chat, -1) == ["reply", "find_features", "current_screen"]  # no task open
    assert "The open task: none." in chat.prompts[-1] and AT_REVIEW not in chat.prompts[-1]
    assert "از کارت ****3566 به رضا کارت به کارت کن" in chat.prompts[-1]  # the conversation is kept, masked


def test_the_model_never_sees_a_full_card_number() -> None:
    chat = StepChat([*started("card_transfer"), STOP])
    react(chat).invoke(new_request("c", "از کارت 6104336383233566 کارت به کارت کن"), CONFIG)
    assert all("6104336383233566" not in prompt for prompt in chat.prompts)
    assert "****3566" in chat.prompts[-1]


def test_a_model_failure_ends_the_request_as_failed() -> None:
    result = react(StepChat([ModelError("chat: ConnectError")])).invoke(
        new_request("c", "کارت به کارت"), CONFIG
    )
    assert result["task_state"] == "failed" and result["reply"] == replies.MODEL_FAILED


def test_a_tool_the_state_does_not_allow_is_a_model_error() -> None:
    result = react(StepChat([call("confirm")])).invoke(new_request("c", "کارت به کارت"), CONFIG)
    assert result["task_state"] == "failed"
    result = react(StepChat([call("start_task", feature="card_transfer", request="…", second=None)])).invoke(
        new_request("c", "کارت به کارت"), CONFIG
    )
    assert result["task_state"] == "failed"  # a feature is started only once find_features found it


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
    result = main_debug.main(StepChat([call("reply", text=HELLO)]), FakeEmbeddings(), ["سلام"], agent="react")
    assert result["reply"] == HELLO
    assert f"assistant: {HELLO}" in capsys.readouterr().out


def test_the_evaluator_runs_a_case_through_the_react_agent() -> None:
    case = {"id": "T-1", "tags": ["knowledge"], "turns": ["سلام"], "expect": {"end": "created"}}
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    chat = StepChat([call("reply", text=HELLO)])
    record = evaluate_agent.run_case(case, PROFILE, Navigator(PROFILE), chat, index, "react")
    assert record["crash"] is None
    assert record["reply"] == HELLO and record["task_state"] == "created"
    assert record["operations"] == [] and record["chosen"] == KNOWLEDGE
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
