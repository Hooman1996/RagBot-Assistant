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
    reviewed,
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


def test_a_missing_source_card_is_one_question_with_the_pickers_cards() -> None:
    chat = FakeChat([CARD, card_values()])
    app = RecordingApp()
    graph = build_agent(chat, app)
    result = graph.invoke(new_request("c", CARD_SENTENCE), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"] == "از کدام کارت انتقال انجام شود؟"
    assert pending["task_state"] == "clarifying"
    assert pending["options"][0] == {"ref": "card-transfer-source=card-1", "label": "****0018"}
    # Only the active debit cards the app's picker lists, then «لغو».
    assert [o["label"] for o in pending["options"]] == ["****0018", "****0026", "لغو"]
    # The app is already on the form, with what is known filled, while the source is asked.
    assert app.screen == "S-174" and not reviewed(app)
    assert set(app.sent[1][1]["values"]) == {"card-transfer-destination", "card-transfer-amount"}
    result = graph.invoke(Command(resume=pending["options"][0]["ref"]), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert operations(app) == ["navigate", "fill_form", "navigate", "fill_form", "show_review"]
    assert app.sent[3][1]["values"]["card-transfer-amount"] == {"value": 2500000, "unit": "IRR"}
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
    assert not reviewed(app)


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
    assert not reviewed(app)


def test_a_card_the_app_does_not_list_is_refused_and_the_listed_cards_are_offered() -> None:
    sentence = "يك ميليون ريال از كارت 0042 به رضا كارت به كارت كن"  # 0042 is a credit card
    chat = FakeChat([CARD, card_values("کارت 0042", amount={"value": 1000000, "unit": "IRR"})])
    app = RecordingApp()
    result = build_agent(chat, app).invoke(new_request("c", sentence), CONFIG)
    pending = result["__interrupt__"][0].value
    refused = replies.NOT_OFFERED.format(text="کارت 0042")
    assert pending["question"] == refused + "\nاز کدام کارت انتقال انجام شود؟"
    assert [item["label"] for item in pending["options"]] == ["****0018", "****0026", "لغو"]
    assert not reviewed(app)


def test_an_installment_the_app_greys_out_is_asked_again_with_the_payable_ones() -> None:
    said = {"insurance-policy": "DEMO-POLICY-0001", "installment": "قسط اول", "insurance-source": "پس‌انداز"}
    chat = FakeChat([{"choice": "insurance_installment"}, said])
    app = RecordingApp()
    request = new_request("c", "اقساط بیمه نامه: قسط اول DEMO-POLICY-0001")
    result = build_agent(chat, app).invoke(request, CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"] == replies.NOT_OFFERED.format(text="قسط اول") + "\nکدام قسط؟"
    assert [item["label"] for item in pending["options"]] == ["قسط سوم", "قسط چهارم", "لغو"]
    assert not reviewed(app)


def test_a_loans_next_installment_is_set_by_code() -> None:
    said = {"loan": "تسهیلات شخصی", "loan-installment": None, "loan-source": "حساب نمایشی"}
    chat = FakeChat([{"choice": "loan_repayment"}, said])
    app = RecordingApp()
    request = new_request("c", "تسهیلات در جریان و پرداخت: قسط تسهیلات شخصی")
    result = build_agent(chat, app).invoke(request, CONFIG)
    assert result["task_state"] == "ready_for_user"
    fills = [arguments["values"] for operation, arguments in app.sent if operation == "fill_form"]
    assert fills[-1]["loan-installment"] == "1"


def test_a_wallet_top_up_amount_the_operator_does_not_list_is_asked_again() -> None:
    said = {
        "wallet-op-source": "کیف نمایشی شخصی",
        "wallet-op-mobile": "09000000003",
        "wallet-op-operator": "رایتل",
        "wallet-op-amount": {"value": 10000, "unit": "IRT"},
    }
    chat = FakeChat([{"choice": "wallet_mobile_top_up"}, said])
    app = RecordingApp()
    title = next(item["title"] for item in PROFILE["features"] if item["intent"] == "wallet_mobile_top_up")
    result = build_agent(chat, app).invoke(new_request("c", f"{title} رایتل ۱۰ هزار تومن"), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"].startswith(replies.NOT_A_CHARGE.format(amounts="200,000، 500,000، 1,000,000"))
    assert not reviewed(app)


@pytest.mark.parametrize(
    ("intent", "words", "said", "refused"),
    [
        (
            "charity",
            "۲۰۰ میلیون تومن",
            {
                "charity": "خیریه نمایشی",
                "charity-amount": {"value": 200_000_000, "unit": "IRT"},
                "charity-source": "حساب نمایشی",
            },
            replies.OUT_OF_RANGE.format(low="10,000", high="100,000,000"),
        ),
        (
            "mobile_top_up",
            "۵۵۵۵۵ ریال",
            {
                "topup-source": "حساب نمایشی",
                "topup-phone": "09000000001",
                "topup-amount": {"value": 55555, "unit": "IRR"},
            },
            replies.TOP_UP_LIMITS.format(low="50,000", high="5,000,000", step="50,000"),
        ),
        (
            "wallet_purchase",
            "۵۰۰ هزار تومن",
            {"wallet-op-source": "کیف نمایشی شخصی", "wallet-op-amount": {"value": 500_000, "unit": "IRT"}},
            replies.OVER_BALANCE.format(balance="3,250,000"),
        ),
    ],
)
def test_an_amount_the_app_would_refuse_is_asked_again_with_the_reason(
    intent: str, words: str, said: dict, refused: str
) -> None:
    app = RecordingApp()
    title = next(item["title"] for item in PROFILE["features"] if item["intent"] == intent)
    request = new_request("c", f"{title} {words}")
    result = build_agent(FakeChat([{"choice": intent}, said]), app).invoke(request, CONFIG)
    assert result["__interrupt__"][0].value["question"].startswith(refused)
    assert not reviewed(app)


def test_a_policy_with_nothing_to_pay_is_not_offered() -> None:
    chat = FakeChat([{"choice": "insurance_installment"}, {"insurance-policy": None, "installment": None}])
    app = RecordingApp()
    result = build_agent(chat, app).invoke(new_request("c", "اقساط بیمه نامه"), CONFIG)
    labels = [item["label"] for item in result["__interrupt__"][0].value["options"]]
    assert "DEMO-POLICY-0002" not in " ".join(labels)


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
    assert not reviewed(app)


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
    assert not reviewed(app)


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
    assert not reviewed(app)


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
    assert not reviewed(app)


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
    assert not reviewed(app)


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
