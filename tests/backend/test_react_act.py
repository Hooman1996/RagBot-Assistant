"""The ReAct agent's act tools (D-032): values through the workflow's checks, the app moved before a
question, the workflow's question and options, the review, a correction, and no way past the review."""

from agent_fakes import PROFILE, FakeChat, FakeEmbeddings, RecordingApp, given, reviewed
from langgraph.types import Command

from assistant import replies
from assistant.agent_nodes import Tools
from assistant.agent_state import new_request
from assistant.intent import FeatureIndex
from assistant.lifecycle import next_input
from assistant.lookups import FixtureLookups
from assistant.models import Chat, ModelError
from assistant.navigator import Navigator
from assistant.react_graph import build_react_graph
from assistant.react_tools import NOT_NAMED, TEXTS, named_by_user, shown_again

CONFIG = {"configurable": {"thread_id": "react"}}
CARD_SENTENCE = "۲۵۰ هزار تومان به رضا کارت به کارت کن"
AMOUNT = {"value": 250000, "unit": "IRT"}
ALLOWED = {"navigate", "fill_form", "show_review"}


class StepChat(FakeChat):
    """The scripted model, read through the real Chat.step."""

    step = Chat.step

    def json(self, prompt: str, schema: dict, tokens: int = 0) -> dict:
        return super().json(prompt, schema)


def call(tool: str, **arguments: object) -> dict:
    return {"thought": "…", "tool": tool, "arguments": arguments}


def card_values(source: str | None = None, destination: str | None = "رضا", amount: object = AMOUNT) -> dict:
    values = {
        "card-transfer-source": source,
        "card-transfer-destination": destination,
        "card-transfer-amount": amount,
    }
    return call("set_values", values=values)


def react(chat: FakeChat, app: RecordingApp):
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    tools = Tools(chat, index, Navigator(PROFILE), FixtureLookups(), app, app.current_screen)
    return build_react_graph(tools)


def tools_offered(chat: FakeChat, step_number: int) -> list[str]:
    """The tools offered at the model's nth step; the workflow's own reading has no anyOf."""
    steps = [schema for schema in chat.schemas if "anyOf" in schema]
    return [branch["properties"]["tool"]["const"] for branch in steps[step_number]["anyOf"]]


def test_a_card_transfer_in_one_sentence_reaches_the_review_and_stops() -> None:
    chat = StepChat(
        [
            {"choice": "card_transfer"},
            card_values("****3566"),
            card_values("****3566")["arguments"]["values"],  # the workflow's reader agrees
            call("open_review"),
        ]
    )
    app = RecordingApp()
    sentence = "۲۵۰ هزار تومان از کارت 6104336383233566 به رضا کارت به کارت کن"
    result = react(chat, app).invoke(new_request("c", sentence), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert [operation for operation, _ in app.sent] == ["navigate", "fill_form", "show_review"]
    values = app.sent[1][1]["values"]
    assert values["card-transfer-amount"] == {"value": 2500000, "unit": "IRR"}
    assert values["card-transfer-destination"] == "6037990836388880"
    assert values["card-transfer-source"] == "card-1"
    assert app.screen == "S-176"
    # The reply is the workflow's list of what was prepared, not the model's words.
    assert result["reply"].startswith(replies.REVIEW_OPEN)
    assert "****8880" in result["reply"] and "2,500,000" in result["reply"]
    assert result["steps"][-1]["result"] == {"review": "open"}


def test_a_missing_source_moves_the_app_then_asks_the_workflows_question() -> None:
    chat = StepChat(
        [
            {"choice": "card_transfer"},
            card_values(),
            card_values()["arguments"]["values"],  # the workflow's reader
            call("move_app"),
            call("ask_user", question="کارت؟"),
            call("open_review"),
        ]
    )
    app = RecordingApp()
    graph = react(chat, app)
    result = graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"] == "از کدام کارت انتقال انجام شود؟"
    assert [o["label"] for o in pending["options"]] == ["****3566", "****4544", "لغو"]
    assert pending["task_state"] == "clarifying"
    # The app already waits on the form, with what is known filled.
    assert app.screen == "S-174" and not reviewed(app)
    assert set(app.sent[1][1]["values"]) == {"card-transfer-destination", "card-transfer-amount"}
    # Only ask_user (and current_screen) once the app waits; no review before every value is in.
    assert tools_offered(chat, 2) == ["current_screen", "ask_user"]
    assert "open_review" not in tools_offered(chat, 1) and "ask_user" in tools_offered(chat, 1)
    result = graph.invoke(Command(resume="card-transfer-source=card-1"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert given(app)["card-transfer-source"] == "card-1"
    picked = result["steps"][3]["result"]
    assert picked == {"picked": {"card-transfer-source": "****3566"}, "missing": [], "next": "open_review"}


def test_asking_at_once_reads_the_words_moves_the_app_and_never_moves_it_twice() -> None:
    # The model skips set_values and move_app: the workflow's reader reads the user's words, the app
    # moves as a step of its own, and the answer resumes only the question.
    chat = StepChat(
        [
            {"choice": "card_transfer"},
            call("ask_user", question="کارت؟"),
            {
                "card-transfer-source": None,
                "card-transfer-destination": "رضا",
                "card-transfer-amount": AMOUNT,
            },
            call("open_review"),
        ]
    )
    app = RecordingApp()
    graph = react(chat, app)
    result = graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    assert result["__interrupt__"][0].value["question"] == "از کدام کارت انتقال انجام شود؟"
    moved = len(app.sent)
    assert moved == 2 and app.screen == "S-174"
    result = graph.invoke(Command(resume="card-transfer-source=card-2"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert [step["tool"] for step in result["steps"]] == [
        "understand",
        "move_app",
        "ask_user",
        "open_review",
    ]
    assert [operation for operation, _ in app.sent[moved:]] == ["navigate", "fill_form", "show_review"]
    assert given(app)["card-transfer-amount"] == {"value": 2500000, "unit": "IRR"}


def test_an_amount_the_user_never_wrote_is_refused() -> None:
    chat = StepChat(
        [
            {"choice": "card_transfer"},
            card_values("****3566", amount={"value": 900000, "unit": "IRT"}),
            card_values("****3566", amount=None)["arguments"]["values"],  # the reader finds no amount
            ModelError("chat: stop here"),
        ]
    )
    app = RecordingApp()
    sentence = "از کارت 6104336383233566 به رضا کارت به کارت کن"
    result = react(chat, app).invoke(new_request("c", sentence), CONFIG)
    found = result["steps"][1]["result"]
    assert found["refused"] == ["card-transfer-amount"]
    assert "card-transfer-amount" in found["missing"]
    assert app.sent == []


def test_a_record_the_user_never_named_is_refused() -> None:
    # The model read «سپرده پس‌انداز» in find_records; the user named no source.
    values = {
        "account-transfer-source": "سپرده پس‌انداز",
        "account-transfer-destination": "رضا",
        "account-transfer-amount": {"value": 500000, "unit": "IRR"},
        "account-transfer-reason": None,
    }
    chat = StepChat(
        [
            {"choice": "internal_transfer"},
            call("set_values", values=values),
            {"account-transfer-source": None, "account-transfer-reason": None},
            ModelError("chat: stop here"),
        ]
    )
    result = react(chat, RecordingApp()).invoke(
        new_request("c", "۵۰۰ هزار ریال به رضا انتقال داخلی بده"), CONFIG
    )
    found = result["steps"][1]["result"]
    assert "account-transfer-source" in found["refused"] and found["note"] == NOT_NAMED
    assert "account-transfer-source" in found["missing"]


def test_a_name_not_found_is_asked_with_the_workflows_note_and_records() -> None:
    chat = StepChat(
        [
            {"choice": "card_transfer"},
            card_values("****3566", "علی"),
            card_values("****3566", "علی")["arguments"]["values"],
            call("move_app"),
            call("ask_user", question="به چه کسی؟"),
        ]
    )
    sentence = "۲۵۰ هزار تومان از کارت 6104336383233566 به علی کارت به کارت کن"
    result = react(chat, RecordingApp()).invoke(new_request("c", sentence), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"].startswith(replies.NOT_FOUND.format(text="علی"))
    assert pending["options"][-1] == {"ref": "cancel", "label": "لغو"}
    assert "رضا محمدی ****8880" in [option["label"] for option in pending["options"]]


def test_cancel_or_only_looking_at_a_question_leaves_the_page_and_sends_nothing_more() -> None:
    for answer in ("cancel", "میخواستم ببینم فقط"):
        chat = StepChat(
            [
                {"choice": "card_transfer"},
                card_values(),
                card_values()["arguments"]["values"],  # the workflow's reader
                call("move_app"),
                call("ask_user", question="کارت؟"),
            ]
        )
        app = RecordingApp()
        graph = react(chat, app)
        graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
        sent = len(app.sent)
        result = graph.invoke(Command(resume=answer), CONFIG)
        assert result["task_state"] == "cancelled"
        assert result["reply"] == replies.CANCELLED_OPEN
        assert len(app.sent) == sent and not reviewed(app)


def test_a_correction_after_the_review_prepares_it_again() -> None:
    chat = StepChat(
        [
            {"choice": "card_transfer"},
            card_values("****3566"),
            card_values("****3566")["arguments"]["values"],
            call("open_review"),
            {"answer": "correction"},  # the workflow's follow-up reads the new message
            card_values("****3566", "رضا", {"value": 300000, "unit": "IRT"})["arguments"]["values"],
            call("open_review"),
        ]
    )
    app = RecordingApp()
    graph = react(chat, app)
    sentence = "۲۵۰ هزار تومان از کارت 6104336383233566 به رضا کارت به کارت کن"
    graph.invoke(new_request("c", sentence), CONFIG)
    previous = graph.get_state(CONFIG).values
    result = graph.invoke(next_input(previous, "c", "نه، ۳۰۰ هزار تومان"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert app.sent[-2][1]["values"]["card-transfer-amount"] == {"value": 3000000, "unit": "IRR"}
    assert "3,000,000" in result["reply"]
    # The workflow's reader read the correction, so the review can open again at once.
    assert "open_review" in tools_offered(chat, 2)


def test_a_page_to_see_opens_it() -> None:
    chat = StepChat([{"choice": "home"}])
    app = RecordingApp(screen="S-174")
    result = react(chat, app).invoke(new_request("c", "برو به خانه"), CONFIG)
    assert result["task_state"] == "completed"
    assert app.screen == "S-026"
    assert result["reply"] == replies.PAGE_OPEN.format(title="خانه")
    # A page to see has nothing for the model to set: code opens it, with no model step.
    assert len(chat.prompts) == 1 and result["steps"][-1]["tool"] == "open_review"


def test_a_loan_installment_offers_no_feature_tool_and_reaches_the_review() -> None:
    values = {"loan": "وام شخصی", "loan-installment": None, "loan-source": "سپرده پس‌انداز"}
    chat = StepChat(
        [
            {"choice": "loan_repayment"},
            call("set_values", values=values),
            values,  # the workflow's reader
            call("open_review"),
        ]
    )
    app = RecordingApp()
    result = react(chat, app).invoke(
        new_request("c", "تسهیلات در جریان و پرداخت: قسط بعدی وام شخصیمو از سپرده پس‌انداز بده"), CONFIG
    )
    assert not {"finish", "find_features", "feature_requirements"} & set(tools_offered(chat, 0))
    assert tools_offered(chat, 0)[-3:] == ["set_values", "move_app", "ask_user"]
    # The only installment the app lets the user pay is set by the workflow's code.
    assert result["steps"][1]["result"]["missing"] == []
    assert result["task_state"] == "ready_for_user"
    assert given(app)["loan-source"] == "account-2"


def test_no_tool_can_confirm_submit_or_pay() -> None:
    assert set(TEXTS) == {
        "find_records",
        "current_screen",
        "set_values",
        "move_app",
        "open_review",
        "ask_user",
    }
    # Every tool any state offers is one of these, and every command reaching the app is one of three.
    chat = StepChat(
        [
            {"choice": "card_transfer"},
            card_values(),
            card_values()["arguments"]["values"],  # the workflow's reader
            call("move_app"),
            call("ask_user", question="کارت؟"),
            call("open_review"),
        ]
    )
    app = RecordingApp()
    graph = react(chat, app)
    graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    graph.invoke(Command(resume="card-transfer-source=card-2"), CONFIG)
    steps = len([schema for schema in chat.schemas if "anyOf" in schema])
    offered = {name for number in range(steps) for name in tools_offered(chat, number)}
    assert offered <= set(TEXTS)
    assert {operation for operation, _ in app.sent} <= ALLOWED
    assert not any(word in name for name in TEXTS for word in ("confirm", "submit", "pay", "authoriz"))


def test_a_typed_iban_the_model_copies_masked_is_the_users() -> None:
    assert named_by_user("IR****6115", ["به شبای IR290170455491846632336115 بفرست"])
    assert not named_by_user("سپرده پس‌انداز", ["به شبای IR290170455491846632336115 بفرست"])
    # «حساب» alone names no record, though one label holds the word: it is asked, never matched.
    assert not named_by_user("حساب", ["از حساب برای 09129013458 شارژ بگیر"])


def test_a_bare_last_four_of_a_long_number_is_read_as_that_number() -> None:
    bill = ["قبض آب 2058601103691 با شناسه پرداخت 1250001 رو پرداخت کن"]
    assert shown_again("3691", bill) == "****3691"
    assert shown_again("4544", ["از کارت 4544 به شماره کارت 6104331610890890"]) == "4544"
    assert shown_again("1250001", bill) == "1250001"


def test_the_workflows_reader_wins_over_the_model_for_a_missing_field() -> None:
    said = {
        "account-transfer-source": "سپرده کوتاه‌مدت",
        "account-transfer-destination": "رضا",
        "account-transfer-amount": {"value": 1000000, "unit": "IRT"},
        "account-transfer-reason": "خرید کالا و خدمات",  # the model's misreading of «بابت اجاره»
    }
    chat = StepChat(
        [
            {"choice": "internal_transfer"},
            call("set_values", values=said),
            said | {"account-transfer-reason": "پرداخت اجاره"},  # the workflow's reader
            ModelError("chat: stop here"),
        ]
    )
    sentence = "یک میلیون تومن از سپرده کوتاه‌مدت به رضا انتقال داخلی بزن، بابت اجاره"
    result = react(chat, RecordingApp()).invoke(new_request("c", sentence), CONFIG)
    assert result["values"]["account-transfer-reason"] == "0"


def test_a_missing_field_the_reader_finds_nothing_for_is_asked_whatever_the_model_set() -> None:
    # Seen live: the model put «رضا» in the source card too; the user would read «رضا» پیدا نشد.
    chat = StepChat(
        [
            {"choice": "card_transfer"},
            card_values("رضا"),
            card_values()["arguments"]["values"],  # the workflow's reader: no source
            ModelError("chat: stop here"),
        ]
    )
    result = react(chat, RecordingApp()).invoke(new_request("c", CARD_SENTENCE), CONFIG)
    found = result["steps"][1]["result"]
    assert found["missing"] == ["card-transfer-source"] and found["note"] != replies.NOT_FOUND.format(
        text="رضا"
    )
    assert "card-transfer-source" in found["refused"]
