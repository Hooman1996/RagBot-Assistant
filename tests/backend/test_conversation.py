"""The conversation flow of D-034 in the workflow agent: the fast path, the turn router at both doors,
the rewrite guard, the switch question, the history and the second task."""

import pytest
from agent_fakes import (
    CARD_SENTENCE,
    FakeChat,
    RecordingApp,
    build_agent,
    every_change_is_allowed,  # noqa: F401 - an autouse fixture: every state change is checked
    reviewed,
)
from langgraph.types import Command

from assistant import replies
from assistant.agent_state import new_request
from assistant.conversation import Conversation
from assistant.lifecycle import fast, next_input
from assistant.lookups import FixtureLookups
from assistant.models import ModelError

CONFIG = {"configurable": {"thread_id": "c"}}
CARD = {"choice": "card_transfer"}
FULL = "۲۵۰ هزار تومان از کارت 6104336383233566 به رضا کارت به کارت کن"
AMOUNT = {"value": 250000, "unit": "IRT"}


def card(source: str | None = "****3566", amount: object = AMOUNT) -> dict:
    return {
        "card-transfer-source": source,
        "card-transfer-destination": "رضا",
        "card-transfer-amount": amount,
    }


def say(graph, text: str) -> dict:
    """A new message, as the chat box sends it."""
    return graph.invoke(next_input(graph.get_state(CONFIG).values, "c", text), CONFIG)


def pending(result: dict) -> dict:
    return result["__interrupt__"][0].value


@pytest.mark.parametrize(
    ("text", "kind"),
    [
        ("رمز دومم 482913 هست", "secret"),
        ("cvv2 کارتم 734 هست", "secret"),
        ("OTP 6618", "secret"),
        ("تأیید کن", "confirm"),
        ("بزن بره!", "confirm"),
        ("لغو", "cancel"),
        ("فقط میخواستم ببینم", "only_looking"),
        ("قبضمو پرداخت کن", None),
        ("رمز کارتمو عوض کن", None),
        ("کارمزدش از کارت 4544 برداشته بشه", None),
        ("۳۰۰ هزار تومن", None),
    ],
)
def test_the_fast_path_reads_refusals_and_cancels_and_leaves_the_rest_to_the_router(
    text: str, kind: str
) -> None:
    assert fast(text) == kind


def test_the_rewrite_guard_keeps_the_users_own_values_only() -> None:
    conversation = Conversation(FakeChat([]), FixtureLookups())
    turns = [{"from": "user", "text": FULL}]
    rewrite = "۲۵۰ هزار تومان از کارت ****3566 به رضا کارت به کارت کن"
    assert "6104336383233566" in conversation.guarded(rewrite, turns, "همونو دوباره بزن")
    assert conversation.guarded("۹۰۰ هزار تومان به رضا کارت به کارت کن", turns, "همونو بزن") is None
    assert conversation.guarded("۲۵۰ هزار تومان به نگار صادقی کارت به کارت کن", turns, "همونو بزن") is None
    assert conversation.guarded(None, turns, "همونو بزن") is None


def test_a_typed_secret_at_the_review_is_refused_and_never_reaches_a_prompt() -> None:
    chat = FakeChat([CARD, card()])
    graph = build_agent(chat, RecordingApp())
    graph.invoke(new_request("c", FULL), CONFIG)
    result = say(graph, "رمز دومم 482913 هست")
    assert result["reply"] == replies.NO_SECRETS and result["task_state"] == "ready_for_user"
    seen = chat.prompts + chat.routed + [str(result["turns"]), str(result["messages"])]
    assert all("482913" not in text for text in seen)
    assert result["turns"][-1] == {"from": "user", "text": replies.REMOVED}


def test_a_typed_confirm_at_the_review_is_refused_and_the_review_stays() -> None:
    chat = FakeChat([CARD, card()])
    app = RecordingApp()
    graph = build_agent(chat, app)
    graph.invoke(new_request("c", FULL), CONFIG)
    sent = len(app.sent)
    result = say(graph, "تأیید کن")
    assert result["reply"] == replies.CONFIRM_BY_TOUCH and result["task_state"] == "ready_for_user"
    assert len(app.sent) == sent  # nothing more reaches the app
    assert len(chat.routed) == 1  # no model call for the refusal: only the first message was routed


def test_a_cancel_with_nothing_open_gets_the_fixed_line() -> None:
    result = build_agent(FakeChat([]), RecordingApp()).invoke(new_request("c", "لغو"), CONFIG)
    assert result["reply"] == replies.NOTHING_OPEN


def test_chat_with_no_task_gets_a_reply_and_nothing_is_done() -> None:
    app = RecordingApp()
    chat = FakeChat([{"kind": "chat"}, {"choice": "not_sure"}])  # the chooser finds no feature
    result = build_agent(chat, app).invoke(new_request("c", "سلام"), CONFIG)
    assert result["reply"] == replies.KNOWLEDGE and result["task_state"] == "created"
    assert app.sent == []


def test_chat_that_names_a_feature_with_no_task_open_is_a_request() -> None:
    # A request worded as a question («… چند بار رفته تو طرح آلودگی؟») may read as chat to the
    # router; with no task open, the chooser decides, as it did before the router.
    chat = FakeChat([{"kind": "chat"}, CARD, CARD, card()])
    result = build_agent(chat, RecordingApp()).invoke(new_request("c", FULL), CONFIG)
    assert result["task_state"] == "ready_for_user"


def test_chat_or_a_secret_in_the_middle_of_a_question_asks_it_again() -> None:
    chat = FakeChat([CARD, card(None), {"kind": "chat"}])
    graph = build_agent(chat, RecordingApp())
    first = pending(graph.invoke(new_request("c", CARD_SENTENCE), CONFIG))
    again = pending(graph.invoke(Command(resume="سلام، خوبی؟"), CONFIG))
    assert again["question"] == replies.KNOWLEDGE + "\n" + first["question"]
    assert again["options"] == first["options"]
    refused = pending(graph.invoke(Command(resume="پین کارتم 1397 ه"), CONFIG))
    assert refused["question"] == replies.NO_SECRETS + "\n" + first["question"]
    assert all("1397" not in text for text in chat.prompts + chat.routed)
    result = graph.invoke(Command(resume="card-transfer-source=card-1"), CONFIG)
    assert result["task_state"] == "ready_for_user"


def test_a_new_task_in_the_middle_of_a_question_asks_to_switch() -> None:
    chat = FakeChat([CARD, card(None), {"kind": "new_task"}, {"choice": "notifications"}])
    app = RecordingApp()
    graph = build_agent(chat, app)
    first = pending(graph.invoke(new_request("c", CARD_SENTENCE), CONFIG))
    switch = pending(graph.invoke(Command(resume="نه ولش، اعلان‌ها را نشان بده"), CONFIG))
    assert switch["question"] == replies.SWITCH.format(old="کارت به کارت", new="اعلان‌ها")
    assert switch["task_state"] == "clarifying"
    back = pending(graph.invoke(Command(resume="switch=no"), CONFIG))
    assert back["question"] == first["question"] and not reviewed(app)


def test_yes_at_the_switch_question_starts_the_new_task() -> None:
    chat = FakeChat([CARD, card(None), {"kind": "new_task"}, {"choice": "notifications"}])
    graph = build_agent(chat, RecordingApp())
    graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    graph.invoke(Command(resume="اعلان‌ها را نشان بده"), CONFIG)
    result = graph.invoke(Command(resume="آره"), CONFIG)
    assert result["task_state"] == "completed" and result["plan"]["intent"] == "notifications"
    assert ("clarifying", "cancelled") in __import__("agent_fakes").CHANGES


def test_a_reference_is_rewritten_and_the_history_carries_over() -> None:
    rewrite = {"kind": "new_task", "standalone": "۲۵۰ هزار تومان از کارت ****3566 به رضا کارت به کارت کن"}
    chat = FakeChat([CARD, card(), {"kind": "new_task"}, CARD, rewrite, CARD, card()])
    graph = build_agent(chat, RecordingApp())
    graph.invoke(new_request("c", FULL), CONFIG)
    say(graph, "کار دیگه هم دارم")  # a new task at the review: the switch question
    graph.invoke(Command(resume="switch=yes"), CONFIG)  # starts «کار دیگه هم دارم», a card transfer
    state = graph.get_state(CONFIG).values
    assert [item["from"] for item in state["turns"]][:3] == ["user", "assistant", "user"]
    assert all("6104336383233566" not in prompt for prompt in chat.routed)  # masked for the model


def test_a_second_task_in_the_same_message_is_said_at_the_review() -> None:
    routed = {"kind": "new_task", "standalone": None, "rest": "قبض آبمو بده"}
    chat = FakeChat([routed, CARD, card()])
    result = build_agent(chat, RecordingApp()).invoke(new_request("c", FULL + "، بعدش قبض آبمو بده"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert replies.SECOND_TASK.format(rest="قبض آبمو بده") in result["reply"]


def test_a_router_that_fails_reads_the_message_as_before() -> None:
    chat = FakeChat([CARD, card()])
    chat.route = lambda prompt: (_ for _ in ()).throw(ModelError("chat: down"))
    result = build_agent(chat, RecordingApp()).invoke(new_request("c", FULL), CONFIG)
    assert result["task_state"] == "ready_for_user"  # with no task open, the message is the request


def test_the_guard_keeps_a_records_full_label_when_the_user_named_part_of_it() -> None:
    conversation = Conversation(FakeChat([]), FixtureLookups())
    turns = [{"from": "user", "text": "از پس‌انداز برای 09121234567 شارژ ۲۰ هزار تومنی بخر"}]
    rewrite = "خرید شارژ ۵۰ هزار تومنی برای 09121234567 از سپرده پس‌انداز"
    assert conversation.guarded(rewrite, turns, "یه شارژ ۵۰ هزار تومنی دیگه هم بخر") is not None


def test_a_new_task_the_chooser_is_not_sure_of_still_asks_to_switch() -> None:
    chat = FakeChat([CARD, card(None), {"kind": "new_task"}, {"choice": "not_sure"}, {"choice": "not_sure"}])
    graph = build_agent(chat, RecordingApp())
    graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    switch = pending(graph.invoke(Command(resume="یه کار دیگه دارم"), CONFIG))
    assert switch["question"] == replies.SWITCH.format(old="کارت به کارت", new=replies.NEW_TASK)
    asked = pending(graph.invoke(Command(resume="switch=yes"), CONFIG))
    assert asked["question"] == replies.NOT_SURE  # understood again: the closest features


def test_the_guard_keeps_the_users_last_four_and_drops_a_number_only_the_assistant_showed() -> None:
    conversation = Conversation(FakeChat([]), FixtureLookups())
    turns = [{"from": "user", "text": "۱۵۰ هزار تومن از کارت 4544 به رضا کارت به کارت کن"}]
    rewrite = "۱,۵۰۰,۰۰۰ ریال از کارت ****4544 به رضا محمدی ****8880 کارت به کارت کن"
    guarded = conversation.guarded(rewrite, turns, "یکی دیگه هم عین همین بزن")
    assert guarded is not None and "4544" in guarded and "8880" not in guarded


def test_the_guard_gives_the_rewrite_back_in_its_own_letters() -> None:
    conversation = Conversation(FakeChat([]), FixtureLookups())
    turns = [{"from": "user", "text": "از پس‌انداز برای 09121234567 شارژ ۲۰ هزار تومنی بخر"}]
    guarded = conversation.guarded(
        "از پس‌انداز برای 09121234567 شارژ ۲۰ هزار تومنی بخر", turns, "همونو دوباره بخر"
    )
    assert "پس‌انداز" in guarded  # the ZWNJ stays, so the record is found by its label


def test_two_tasks_in_a_first_message_prepare_the_first_alone() -> None:
    first = "۲۵۰ هزار تومان از کارت 6104336383233566 به رضا کارت به کارت کن"
    routed = {"kind": "new_task", "standalone": first, "rest": "قبض آبمو بده"}
    chat = FakeChat([routed, CARD, card()])
    graph = build_agent(chat, RecordingApp())
    result = graph.invoke(new_request("c", first + "، بعدش قبض آبمو بده"), CONFIG)
    # The chooser and the reader see the first task alone (its digits as the guard gives them back).
    assert len(result["messages"]) == 1 and "قبض" not in result["messages"][0]
    assert replies.SECOND_TASK.format(rest="قبض آبمو بده") in result["reply"]


def test_a_rewrite_that_keeps_the_second_task_has_it_taken_out() -> None:
    conversation = Conversation(FakeChat([]), FixtureLookups())
    text = "قسط بعدی وامم رو از پس‌انداز بده، یه کارت به کارت هم به رضا دارم"
    routed = {
        "standalone": "قسط بعدی وامم رو از پس‌انداز بده و یه کارت به کارت هم به رضا بده",
        "rest": "یه کارت به کارت هم به رضا بده",
    }
    assert conversation.standalone(routed, [], text) == "قسط بعدی وامم رو از پس‌انداز بده"


@pytest.mark.parametrize(
    ("text", "wanted"),
    [
        ("قسط بعدی وام شخصیمو بده", "loan_repayment"),
        ("قسط وام خودرو رو بپرداز", "loan_repayment"),
        ("قسط تسهیلاتمو پرداخت کن", "loan_repayment"),
        ("قسط‌های وامم رو نشون بده", "loans"),  # seeing them stays the page
        ("وام‌هامو ببینم", "loans"),
    ],
)
def test_a_page_that_shows_loans_gives_way_to_paying_an_installment(text: str, wanted: str) -> None:
    from assistant.intent import choose

    offered = [
        {"intent": "loans", "title": "", "required_slots": []},
        {"intent": "loan_repayment", "title": "", "required_slots": []},
    ]
    assert choose(FakeChat([{"choice": "loans"}]), text, offered) == wanted


def test_the_paying_feature_is_chosen_only_when_it_was_offered() -> None:
    from assistant.intent import choose

    offered = [{"intent": "loans", "title": "", "required_slots": []}]
    assert choose(FakeChat([{"choice": "loans"}]), "قسط وامم رو بده", offered) == "loans"


def test_a_rewrite_that_changes_paying_into_seeing_is_refused() -> None:
    conversation = Conversation(FakeChat([]), FixtureLookups())
    turns = [{"from": "user", "text": "برو به خانه"}]
    assert conversation.guarded("قسط بعدی وام شخصی رو نشون بده", turns, "قسط بعدی وام شخصیمو بده") is None
    assert (
        conversation.guarded("قسط بعدی وام شخصیم رو پرداخت کن", turns, "قسط بعدی وام شخصیمو بده") is not None
    )
