"""How the agent reads the user's answers: corrections, hidden numbers, choices and fixed selections."""

from agent_fakes import CARD_SENTENCE, FakeChat, RecordingApp, build_agent
from langgraph.types import Command

from assistant.agent_state import new_request

CONFIG = {"configurable": {"thread_id": "answers"}}
CARD = {"choice": "card_transfer"}


def card(source: str | None, destination: str = "رضا", toman: int = 250000) -> dict:
    amount = {"value": toman, "unit": "IRT"}
    return {
        "card-transfer-source": source,
        "card-transfer-destination": destination,
        "card-transfer-amount": amount,
    }


def sent_values(app: RecordingApp) -> dict:
    return next(arguments["values"] for operation, arguments in app.sent if operation == "fill_form")


def test_a_correction_in_an_answer_replaces_the_value_held_before() -> None:
    # The second model answer keeps the old amount: the newest amount the user wrote still wins.
    chat = FakeChat([CARD, card(None, toman=200000), card("کارت 0026", toman=200000)])
    app = RecordingApp()
    graph = build_agent(chat, app)
    graph.invoke(new_request("c", "۲۰۰ هزار تومن به رضا کارت به کارت کن"), CONFIG)
    result = graph.invoke(Command(resume="از کارت 0026، مبلغ رو ۳۰۰ هزار تومن کن"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert set(chat.schemas[2]["properties"]) == set(card(None))  # every value was read again
    values = sent_values(app)
    assert values["card-transfer-source"] == "card-2"
    assert values["card-transfer-amount"] == {"value": 3000000, "unit": "IRR"}


def test_a_hidden_number_inside_words_is_put_back() -> None:
    sentence = "۱۵۰ هزار تومان از کارت 0018 به کارت 0000000000000091 بریز"
    chat = FakeChat([CARD, card("کارت 0018", "کارت ****0091", 150000)])
    app = RecordingApp()
    build_agent(chat, app).invoke(new_request("c", sentence), CONFIG)
    assert "****0091" in chat.prompts[1] and "0000000000000091" not in chat.prompts[1]
    assert sent_values(app)["card-transfer-destination"] == "0000000000000091"


def test_a_typed_iban_not_saved_reaches_the_review() -> None:
    sentence = "۴۰۰ هزار تومن از حساب نمایشی به شبا IR380000000000000000000004 بفرست، بابت نمونه اول"
    found = {
        "account-transfer-source": "حساب نمایشی",
        "account-transfer-destination": "شبا IR****0004",
        "account-transfer-amount": {"value": 400000, "unit": "IRT"},
        "account-transfer-reason": "بابت نمونهٔ اول — ساختگی",
    }
    app = RecordingApp()
    result = build_agent(FakeChat([{"choice": "interbank_transfer"}, found]), app).invoke(
        new_request("c", sentence), CONFIG
    )
    assert result["task_state"] == "ready_for_user"
    assert sent_values(app)["account-transfer-destination"] == "IR380000000000000000000004"
    assert sent_values(app)["account-transfer-reason"] == "0"


SCHEDULE = {"schedule-title": "اجاره نمونه", "schedule-start": "1405/09/01", "schedule-end": "1405/12/01"}


def test_a_choice_is_one_of_its_labels_and_reaches_the_app_as_its_key() -> None:
    chat = FakeChat([{"choice": "recurring_transfer"}, SCHEDULE | {"schedule-frequency": "ماهانه"}])
    app = RecordingApp()
    result = build_agent(chat, app).invoke(new_request("c", "انتقال خودکار ماهانه اجاره نمونه"), CONFIG)
    frequency = chat.schemas[1]["properties"]["schedule-frequency"]["anyOf"][0]
    assert frequency == {"enum": ["روزانه", "هفتگی", "ماهانه"]}
    assert "MONTHLY" not in chat.prompts[1]
    assert sent_values(app)["schedule-frequency"] == "MONTHLY"
    assert "دوره تکرار: ماهانه" in result["reply"]


def test_text_that_is_no_allowed_key_is_asked_again_with_the_options() -> None:
    chat = FakeChat([{"choice": "recurring_transfer"}, SCHEDULE | {"schedule-frequency": "هر ماه یکبار"}])
    app = RecordingApp()
    graph = build_agent(chat, app)
    pending = graph.invoke(new_request("c", "انتقال خودکار اجاره نمونه"), CONFIG)["__interrupt__"][0].value
    assert [item["ref"] for item in pending["options"]] == [
        "schedule-frequency=DAILY",
        "schedule-frequency=WEEKLY",
        "schedule-frequency=MONTHLY",
        "cancel",
    ]
    assert app.sent == []
    result = graph.invoke(Command(resume="schedule-frequency=MONTHLY"), CONFIG)
    assert result["task_state"] == "ready_for_user"


def test_a_selection_the_feature_decides_is_set_by_code_and_never_asked() -> None:
    chat = FakeChat([{"choice": "credit_report"}, {"svc-source": "حساب نمایشی"}])
    app = RecordingApp()
    result = build_agent(chat, app).invoke(new_request("c", "گزارش اعتباری بگیر از حساب نمایشی"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert set(chat.schemas[1]["properties"]) == {"svc-source"}
    navigate = next(arguments for operation, arguments in app.sent if operation == "navigate")
    assert navigate["arguments"]["inquiry-kind"] == "credit-report"


def test_the_card_sentence_still_asks_once_for_the_source() -> None:
    result = build_agent(FakeChat([CARD, card(None)]), RecordingApp()).invoke(
        new_request("c", CARD_SENTENCE), CONFIG
    )
    assert result["__interrupt__"][0].value["question"] == "از کدام کارت انتقال انجام شود؟"


def test_an_account_number_given_for_an_iban_is_asked_again() -> None:
    sentence = "یک میلیون ریال از پس‌انداز به شماره حساب 0000000000077 واریز کن، بابت نمونه دوم"
    found = {
        "account-transfer-source": "پس‌انداز",
        "account-transfer-destination": "****0077",
        "account-transfer-amount": {"value": 1000000, "unit": "IRR"},
        "account-transfer-reason": "بابت نمونهٔ دوم — ساختگی",
    }
    app = RecordingApp()
    result = build_agent(FakeChat([{"choice": "interbank_transfer"}, found]), app).invoke(
        new_request("c", sentence), CONFIG
    )
    assert result["__interrupt__"][0].value["question"].endswith("شماره حساب یا شبای مقصد چیست؟")
    assert app.sent == []
