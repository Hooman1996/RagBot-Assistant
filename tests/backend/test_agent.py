"""The agent graph end to end, on a scripted model, fixed embeddings and the stand-in app."""

import re

import pytest
from agent_fakes import (
    CARD_SENTENCE,
    CHANGES,
    PROFILE,
    DownEmbeddings,
    FakeChat,
    RecordingApp,
    build_agent,
    every_change_is_allowed,  # noqa: F401 - an autouse fixture: every state change is checked
)
from langgraph.types import Command

from assistant import replies
from assistant.agent_state import new_request
from assistant.slots import money

CONFIG = {"configurable": {"thread_id": "test"}}
CARD = {"choice": "card_transfer"}
AMOUNT = {"value": 250000, "unit": "IRT"}
TYPED_SOURCE = "۲۵۰ هزار تومان از کارت 0000000000000018 به رضا کارت به کارت کن"


def operations(app: RecordingApp) -> list[str]:
    return [operation for operation, _ in app.sent]


def card_values(source: str | None = None, destination: str = "رضا", amount: object = AMOUNT) -> dict:
    return {
        "card-transfer-source": source,
        "card-transfer-destination": destination,
        "card-transfer-amount": amount,
    }


def test_card_to_reza_in_one_sentence_reaches_the_review_and_stops() -> None:
    chat = FakeChat([CARD, card_values("****0018")])
    app = RecordingApp()
    result = build_agent(chat, app).invoke(new_request("c", TYPED_SOURCE), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert operations(app) == ["navigate", "fill_form", "show_review"]
    values = app.sent[1][1]["values"]
    assert values["card-transfer-amount"] == {"value": 2500000, "unit": "IRR"}
    assert values["card-transfer-destination"] == "0000000000000091"
    assert values["card-transfer-source"] == "card-1"
    assert app.screen == "S-176"
    assert "****0091" in result["reply"] and "2,500,000" in result["reply"]
    assert ("verifying_review", "ready_for_user") in CHANGES


def test_a_missing_source_card_is_one_question_with_card_options() -> None:
    chat = FakeChat([CARD, card_values()])
    app = RecordingApp()
    graph = build_agent(chat, app)
    result = graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"] == "از کدام کارت انتقال انجام شود؟"
    assert pending["task_state"] == "clarifying"
    assert pending["options"][0] == {"ref": "card-transfer-source=card-1", "label": "****0018"}
    assert len(pending["options"]) == 8 and pending["options"][-1] == {"ref": "cancel", "label": "لغو"}
    assert app.sent == []
    result = graph.invoke(Command(resume=pending["options"][0]["ref"]), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert operations(app) == ["navigate", "fill_form", "show_review"]
    assert app.sent[1][1]["values"]["card-transfer-amount"] == {"value": 2500000, "unit": "IRR"}
    assert len(chat.prompts) == 2


def test_an_ambiguous_name_gets_the_matching_records_as_options() -> None:
    sentence = "۵۰۰ هزار ریال از حساب نمایشی به رضا انتقال داخلی بده"
    found = {
        "account-transfer-source": "نمایشی",
        "account-transfer-destination": "رضا",
        "account-transfer-amount": {"value": 500000, "unit": "IRR"},
        "account-transfer-reason": "0",
    }
    app = RecordingApp()
    result = build_agent(FakeChat([{"choice": "internal_transfer"}, found]), app).invoke(
        new_request("c", sentence), CONFIG
    )
    pending = result["__interrupt__"][0].value
    assert pending["question"].startswith(replies.WHICH.format(text="نمایشی"))
    assert [item["label"] for item in pending["options"]] == ["حساب نمایشی", "پس‌انداز نمایشی", "لغو"]
    assert app.sent == []


def test_a_name_that_is_not_found_asks_again_and_sends_nothing() -> None:
    chat = FakeChat([CARD, card_values("card-1", "علی"), {"card-transfer-destination": "علی"}])
    app = RecordingApp()
    graph = build_agent(chat, app)
    result = graph.invoke(new_request("c", CARD_SENTENCE.replace("رضا", "علی")), CONFIG)
    first = result["__interrupt__"][0].value
    assert first["question"].startswith(replies.NOT_FOUND.format(text="علی"))
    assert first["options"] == [{"ref": "cancel", "label": "لغو"}]
    result = graph.invoke(Command(resume="علی"), CONFIG)
    assert result["__interrupt__"][0].value["question"] == first["question"]
    assert app.sent == []


def test_a_choice_that_was_not_offered_becomes_not_sure() -> None:
    app = RecordingApp()
    result = build_agent(FakeChat([{"choice": "pay_everything"}]), app).invoke(
        new_request("c", "همه را بده"), CONFIG
    )
    pending = result["__interrupt__"][0].value
    assert pending["question"] == replies.NOT_SURE
    assert [item["label"] for item in pending["options"]] == [
        *(item["title"] for item in PROFILE["features"][:8]),
        "لغو",
    ]
    assert app.sent == []


def test_a_not_sure_option_goes_on_to_that_feature() -> None:
    chat = FakeChat([{"choice": "not_sure"}, card_values("card-1")])
    app = RecordingApp()
    graph = build_agent(chat, app)
    graph.invoke(new_request("c", "۲۵۰ هزار تومان برای رضا"), CONFIG)
    result = graph.invoke(Command(resume="card_transfer"), CONFIG)
    assert result["task_state"] == "ready_for_user"


def test_a_slot_that_was_not_offered_is_dropped() -> None:
    chat = FakeChat([CARD, card_values() | {"card-transfer-cvv2": "123", "otp-code": "999"}])
    app = RecordingApp()
    graph = build_agent(chat, app)
    graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    schema = chat.schemas[1]
    assert schema["additionalProperties"] is False
    assert set(schema["properties"]) == set(card_values())
    assert set(graph.get_state(CONFIG).values["values"]) == {
        "card-transfer-destination",
        "card-transfer-amount",
    }
    assert app.sent == []


@pytest.mark.parametrize(
    "amount",
    [
        {"value": 2500.5, "unit": "IRT"},
        {"value": "250000", "unit": "IRT"},
        {"value": True, "unit": "IRR"},
        {"value": -5, "unit": "IRR"},
        {"value": 250000, "unit": "USD"},
        {"value": 250000},
        "۲۵۰ هزار",
    ],
)
def test_money_that_is_not_a_whole_number_in_a_known_unit_is_refused(amount: object) -> None:
    app = RecordingApp()
    result = build_agent(FakeChat([CARD, card_values("card-1", amount=amount)]), app).invoke(
        new_request("c", "از کارت 0018 به رضا کارت به کارت کن"), CONFIG
    )
    assert result["__interrupt__"][0].value["question"] == "چند ریال منتقل شود؟"
    assert app.sent == []


def test_toman_becomes_exact_rials() -> None:
    said = ["۲۵۰ هزار تومان به رضا"]
    assert money({"value": 250000, "unit": "IRT"}, said) == {"value": 2500000, "unit": "IRR"}
    assert money({"value": 2500000, "unit": "IRR"}, said) == {"value": 2500000, "unit": "IRR"}
    assert type(money({"value": 3000, "unit": "IRT"}, ["سه هزار تومان"])["value"]) is int


def test_an_amount_that_may_be_shorthand_is_asked() -> None:
    # «شصت تمن» usually means sixty thousand toman, and a bare «30» in a request has no unit.
    assert money({"value": 60, "unit": "IRT"}, ["شصت تمن بزن برا رضا"]) is None
    assert money({"value": 30, "unit": "IRR"}, ["30 جابجا شه برا رضا"]) is None
    answered = ["30 جابجا شه برا رضا", "300000"]  # a bare number answers «چند ریال…؟»
    assert money({"value": 300000, "unit": "IRR"}, answered) == {"value": 300000, "unit": "IRR"}


def test_a_model_amount_the_user_never_wrote_is_not_used() -> None:
    # The baseline's factor of 1,000: «۵۰۰ هزار تومان» came back as 500,000,000 toman.
    said = ["۵۰۰ هزار تومان از کیف برداشت کن"]
    assert money({"value": 500000000, "unit": "IRT"}, said) == {"value": 5000000, "unit": "IRR"}
    # A correction: the newest answer's amount wins over the one the model kept.
    corrected = ["۲۰۰ هزار تومن به رضا", "نه، ۳۰۰ هزار تومن"]
    assert money({"value": 200000, "unit": "IRT"}, corrected) == {"value": 3000000, "unit": "IRR"}
    assert money({"value": 5, "unit": "IRT"}, ["۲۰۰ هزار تومن یا ۳۰۰ هزار تومن"]) is None


def test_a_knowledge_question_gets_the_fixed_message_and_sends_nothing() -> None:
    app = RecordingApp()
    result = build_agent(FakeChat([{"choice": "knowledge_question"}]), app).invoke(
        new_request("c", "نرخ سود سپرده چند است؟"), CONFIG
    )
    assert result["reply"] == "پاسخ به پرسش‌های عمومی در این مرحله فعال نیست"
    assert result["task_state"] == "created"
    assert app.sent == []


def test_a_feature_without_a_form_ends_completed_on_its_destination() -> None:
    app = RecordingApp()
    result = build_agent(FakeChat([{"choice": "notifications"}]), app).invoke(
        new_request("c", "اعلان‌ها را نشان بده"), CONFIG
    )
    assert result["task_state"] == "completed"
    assert app.screen == "S-075"
    assert operations(app) == ["navigate"]
    assert result["reply"] == replies.PAGE_OPEN.format(title="اعلان‌ها")
    assert ("ready_for_user", "completed") in CHANGES


def test_verify_fails_when_the_app_lands_on_another_screen() -> None:
    class Wandering(RecordingApp):
        def show_review(self) -> dict:
            self.screen = "S-192"
            return {"outcome": "succeeded", "postcondition": "review_shown_S-192"}

    app = Wandering()
    result = build_agent(FakeChat([CARD, card_values("card-1")]), app).invoke(
        new_request("c", CARD_SENTENCE), CONFIG
    )
    assert result["task_state"] == "failed"
    assert "S-176" in result["reply"] and "S-192" in result["reply"]


def test_a_model_that_cannot_be_reached_fails_the_task_and_sends_nothing() -> None:
    app = RecordingApp()
    result = build_agent(FakeChat([]), app, DownEmbeddings()).invoke(new_request("c", CARD_SENTENCE), CONFIG)
    assert result["task_state"] == "failed"
    assert result["reply"] == replies.MODEL_FAILED
    assert app.sent == []


def test_the_model_never_sees_a_screen_an_action_a_user_only_tag_or_a_full_number() -> None:
    chats = [FakeChat([CARD, card_values()]), FakeChat([{"choice": "not_sure"}])]
    build_agent(chats[0], RecordingApp()).invoke(new_request("c", TYPED_SOURCE), CONFIG)
    build_agent(chats[1], RecordingApp()).invoke(new_request("c", "0000000000000091"), CONFIG)
    prompts = "\n".join(prompt for chat in chats for prompt in chat.prompts)
    assert "****0018" in prompts and "****0091" in prompts
    assert not re.search(r"S-\d{3}", prompts)
    assert not re.search(r"\d{16}", prompts)
    assert not [
        edge["action"] for edge in PROFILE["edges"] if "*" not in edge["action"] and edge["action"] in prompts
    ]
    assert not [tag for tag in PROFILE["user_only_actions"] if tag.split("*")[0] in prompts]
