"""The task's life around the review: cancel, a correction after it, a pause, and a reconcile."""

from agent_fakes import (
    CARD_SENTENCE,
    CHANGES,
    FakeChat,
    RecordingApp,
    build_agent,
    every_change_is_allowed,  # noqa: F401 - an autouse fixture: every state change is checked
)
from langgraph.types import Command

from assistant import replies
from assistant.agent_state import new_request
from assistant.lifecycle import next_input
from assistant.task_runner import Uncertain, Unreachable

CONFIG = {"configurable": {"thread_id": "life"}}
CARD = {"choice": "card_transfer"}
FULL = "۲۵۰ هزار تومان از کارت 0018 به رضا کارت به کارت کن"


def card(source: str | None = "کارت 0018", toman: int = 250000) -> dict:
    amount = {"value": toman, "unit": "IRT"}
    return {
        "card-transfer-source": source,
        "card-transfer-destination": "رضا",
        "card-transfer-amount": amount,
    }


def say(graph, text: str) -> dict:
    """Sends a message as the chat box does: a follow-up of a task at its review or paused, else new."""
    return graph.invoke(next_input(graph.get_state(CONFIG).values, "c", text), CONFIG)


def operations(app: RecordingApp) -> list[str]:
    return [operation for operation, _ in app.sent]


class FlakyApp(RecordingApp):
    """The stand-in app, failing chosen commands: by raising, or by reporting an uncertain outcome."""

    def __init__(self, failures: dict[int, object]) -> None:
        super().__init__()
        self.failures = failures  # the command's number, from 1, and how it fails

    def send(self, operation: str, arguments: dict) -> dict:
        failure = self.failures.get(len(self.sent) + 1)
        if isinstance(failure, Exception):
            self.sent.append(("not sent", arguments))
            raise failure
        result = super().send(operation, arguments)
        return result | {"outcome": "uncertain"} if failure == "uncertain" else result


def test_the_cancel_option_ends_a_question_and_sends_nothing() -> None:
    app = RecordingApp()
    graph = build_agent(FakeChat([CARD, card(None)]), app)
    pending = graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)["__interrupt__"][0].value
    assert pending["options"][-1] == {"ref": "cancel", "label": "لغو"}
    result = graph.invoke(Command(resume="cancel"), CONFIG)
    assert result["task_state"] == "cancelled" and result["reply"] == replies.CANCELLED
    assert ("clarifying", "cancelled") in CHANGES
    assert app.sent == []


def test_a_cancel_phrase_ends_a_not_sure_question() -> None:
    graph = build_agent(FakeChat([{"choice": "not_sure"}]), RecordingApp())
    graph.invoke(new_request("c", "یه کاری دارم"), CONFIG)
    result = graph.invoke(Command(resume="بیخیال"), CONFIG)
    assert result["task_state"] == "cancelled"
    assert ("created", "cancelled") in CHANGES


def test_a_correction_after_the_review_prepares_a_new_review() -> None:
    chat = FakeChat([CARD, card(), {"answer": "correction"}, card()])
    app = RecordingApp()
    graph = build_agent(chat, app)
    assert graph.invoke(new_request("c", FULL), CONFIG)["task_state"] == "ready_for_user"
    result = say(graph, "نه، مبلغ رو ۳۰۰ هزار تومن کن")
    assert result["task_state"] == "ready_for_user"
    assert ("ready_for_user", "resolving") in CHANGES
    assert operations(app) == ["navigate", "fill_form", "show_review"] * 2
    assert app.sent[4][1]["values"]["card-transfer-amount"] == {"value": 3000000, "unit": "IRR"}
    assert app.screen == "S-176" and "3,000,000" in result["reply"]


def test_a_new_request_after_the_review_starts_a_new_task() -> None:
    chat = FakeChat([CARD, card(), {"answer": "new_request"}, {"choice": "notifications"}])
    graph = build_agent(chat, RecordingApp())
    graph.invoke(new_request("c", FULL), CONFIG)
    result = say(graph, "اعلان‌ها را نشان بده")
    assert result["task_state"] == "completed"
    assert result["messages"] == ["اعلان‌ها را نشان بده"]


def test_a_cancel_after_the_review_needs_no_model() -> None:
    chat = FakeChat([CARD, card()])
    graph = build_agent(chat, RecordingApp())
    graph.invoke(new_request("c", FULL), CONFIG)
    result = say(graph, "لغو")
    assert result["task_state"] == "cancelled"
    assert ("ready_for_user", "cancelled") in CHANGES
    assert len(chat.prompts) == 2


def test_an_app_out_of_reach_pauses_the_task_and_a_message_resumes_it() -> None:
    app = FlakyApp({1: Unreachable("برنامه هنوز به سرور وصل نشده است")})
    chat = FakeChat([CARD, card()])
    graph = build_agent(chat, app)
    paused = graph.invoke(new_request("c", FULL), CONFIG)
    assert paused["task_state"] == "paused" and paused["reply"] == replies.STOPPED["unreachable"]
    assert paused["values"]["card-transfer-source"] == "card-1"
    result = say(graph, "ادامه")
    assert result["task_state"] == "ready_for_user"
    assert ("preparing", "paused") in CHANGES and ("paused", "reconciling") in CHANGES
    assert len(chat.prompts) == 2  # nothing was asked of the model again


def test_an_uncertain_command_is_reconciled_once() -> None:
    app = FlakyApp({2: "uncertain"})
    result = build_agent(FakeChat([CARD, card()]), app).invoke(new_request("c", FULL), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert ("preparing", "reconciling") in CHANGES and ("reconciling", "resolving") in CHANGES
    assert operations(app) == ["navigate", "fill_form", "navigate", "fill_form", "show_review"]


def test_a_second_uncertain_command_fails_visibly() -> None:
    app = FlakyApp({1: Uncertain("پاسخی از برنامه نرسید"), 2: Uncertain("پاسخی از برنامه نرسید")})
    result = build_agent(FakeChat([CARD, card()]), app).invoke(new_request("c", FULL), CONFIG)
    assert result["task_state"] == "failed"
    assert replies.UNCERTAIN in result["reply"]


def test_a_cancel_during_a_run_stops_before_the_next_command() -> None:
    app = RecordingApp()
    graph = build_agent(FakeChat([CARD, card()]), app, stopped=lambda: len(app.sent) >= 1)
    result = graph.invoke(new_request("c", FULL), CONFIG)
    assert result["task_state"] == "cancelled" and result["reply"] == replies.STOPPED["cancelled"]
    assert ("preparing", "cancelling") in CHANGES and ("cancelling", "cancelled") in CHANGES
    assert operations(app) == ["navigate"]  # the navigate already sent is not undone
