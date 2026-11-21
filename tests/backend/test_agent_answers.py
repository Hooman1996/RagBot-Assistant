"""How the agent reads the user's answers: corrections, hidden numbers, choices and fixed selections."""

from agent_fakes import CARD_SENTENCE, PROFILE, FakeChat, RecordingApp, build_agent, reviewed
from langgraph.types import Command

from assistant.agent_state import new_request
from assistant.lifecycle import next_input

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
    """The last form fill: the one before the review."""
    return [arguments["values"] for operation, arguments in app.sent if operation == "fill_form"][-1]


def test_a_correction_in_an_answer_replaces_the_value_held_before() -> None:
    # The second model answer keeps the old amount: the newest amount the user wrote still wins.
    chat = FakeChat([CARD, card(None, toman=200000), card("کارت 4544", toman=200000)])
    app = RecordingApp()
    graph = build_agent(chat, app)
    graph.invoke(new_request("c", "۲۰۰ هزار تومن به رضا کارت به کارت کن"), CONFIG)
    result = graph.invoke(Command(resume="از کارت 4544، مبلغ رو ۳۰۰ هزار تومن کن"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert set(chat.schemas[2]["properties"]) == set(card(None))  # every value was read again
    values = sent_values(app)
    assert values["card-transfer-source"] == "card-2"
    assert values["card-transfer-amount"] == {"value": 3000000, "unit": "IRR"}


def test_a_hidden_number_inside_words_is_put_back() -> None:
    sentence = "۱۵۰ هزار تومان از کارت 3566 به کارت 6037990836388880 بریز"
    chat = FakeChat([CARD, card("کارت 3566", "کارت ****8880", 150000)])
    app = RecordingApp()
    build_agent(chat, app).invoke(new_request("c", sentence), CONFIG)
    assert "****8880" in chat.prompts[1] and "6037990836388880" not in chat.prompts[1]
    assert sent_values(app)["card-transfer-destination"] == "6037990836388880"


def test_a_typed_iban_not_saved_reaches_the_review() -> None:
    sentence = "۴۰۰ هزار تومن از سپرده کوتاه‌مدت به شبا IR910180319412535974461725 بفرست، بابت اجاره"
    found = {
        "account-transfer-source": "سپرده کوتاه‌مدت",
        "account-transfer-destination": "شبا IR****1725",
        "account-transfer-amount": {"value": 400000, "unit": "IRT"},
        "account-transfer-reason": "پرداخت اجاره",
    }
    app = RecordingApp()
    result = build_agent(FakeChat([{"choice": "interbank_transfer"}, found]), app).invoke(
        new_request("c", sentence), CONFIG
    )
    assert result["task_state"] == "ready_for_user"
    assert sent_values(app)["account-transfer-destination"] == "IR910180319412535974461725"
    assert sent_values(app)["account-transfer-reason"] == "0"


SCHEDULE = {"schedule-title": "اجاره آپارتمان", "schedule-start": "1405/09/01", "schedule-end": "1405/12/01"}


def test_a_choice_is_one_of_its_labels_and_reaches_the_app_as_its_key() -> None:
    chat = FakeChat([{"choice": "recurring_transfer"}, SCHEDULE | {"schedule-frequency": "روزانه"}])
    app = RecordingApp()
    result = build_agent(chat, app).invoke(new_request("c", "انتقال خودکار روزانه اجاره آپارتمان"), CONFIG)
    frequency = chat.schemas[1]["properties"]["schedule-frequency"]["anyOf"][0]
    assert frequency == {"enum": ["روزانه", "هفتگی", "ماهانه"]}
    assert "DAILY" not in chat.prompts[1]
    assert sent_values(app)["schedule-frequency"] == "DAILY"
    assert "schedule-day" not in sent_values(app)  # a daily transfer has no day
    assert "دوره تکرار: روزانه" in result["reply"]


def test_a_weekly_transfer_asks_its_day_and_sends_it_after_the_frequency() -> None:
    chat = FakeChat([{"choice": "recurring_transfer"}, SCHEDULE | {"schedule-frequency": "هفتگی"}])
    app = RecordingApp()
    graph = build_agent(chat, app)
    result = graph.invoke(new_request("c", "انتقال خودکار هفتگی اجاره آپارتمان"), CONFIG)
    pending = result["__interrupt__"][0].value
    assert pending["question"] == "در چه روزی انجام شود؟"
    assert [item["label"] for item in pending["options"]][:2] == ["شنبه", "یکشنبه"]
    result = graph.invoke(Command(resume="schedule-day=6"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert list(sent_values(app))[-2:] == ["schedule-frequency", "schedule-day"]
    assert sent_values(app)["schedule-day"] == "6"


def test_text_that_is_no_allowed_key_is_asked_again_with_the_options() -> None:
    chat = FakeChat([{"choice": "recurring_transfer"}, SCHEDULE | {"schedule-frequency": "هر ماه یکبار"}])
    app = RecordingApp()
    graph = build_agent(chat, app)
    pending = graph.invoke(new_request("c", "انتقال خودکار اجاره آپارتمان"), CONFIG)["__interrupt__"][0].value
    assert [item["ref"] for item in pending["options"]] == [
        "schedule-frequency=DAILY",
        "schedule-frequency=WEEKLY",
        "schedule-frequency=MONTHLY",
        "cancel",
    ]
    assert not reviewed(app)
    pending = graph.invoke(Command(resume="schedule-frequency=MONTHLY"), CONFIG)["__interrupt__"][0].value
    assert pending["question"] == "در چه روزی انجام شود؟" and len(pending["options"]) == 32  # 31 days, «لغو»
    result = graph.invoke(Command(resume="schedule-day=5"), CONFIG)
    assert result["task_state"] == "ready_for_user"


def test_a_card_sent_by_post_asks_the_address_and_goes_by_the_address_page() -> None:
    said = {"source": "سپرده کوتاه‌مدت", "delivery": "ارسال پستی", "color": "سبز"}
    chat = FakeChat([{"choice": "card_request"}, said])
    app = RecordingApp()
    graph = build_agent(chat, app)
    pending = graph.invoke(new_request("c", "درخواست کارت با ارسال پستی"), CONFIG)["__interrupt__"][0].value
    assert "به کدام آدرس ارسال شود؟" in pending["question"]
    assert "address=address-1" in [item["ref"] for item in pending["options"]]
    graph.invoke(Command(resume="address=address-1"), CONFIG)
    forms = [arguments["form"] for operation, arguments in app.sent if operation == "fill_form"]
    assert "S-129" in forms


def test_a_label_word_before_a_text_value_is_dropped() -> None:
    said = {"field-title": "عنوان محل کار", "field-postal": "1969833114", "field-address": "میدان ونک"}
    chat = FakeChat([{"choice": "saved_addresses"}, said])
    app = RecordingApp()
    title = next(item["title"] for item in PROFILE["features"] if item["intent"] == "saved_addresses")
    build_agent(chat, app).invoke(new_request("c", f"{title}: عنوان محل کار، کد پستی 1969833114"), CONFIG)
    assert sent_values(app)["field-title"] == "محل کار"


def test_an_account_the_model_left_out_is_taken_when_the_user_named_only_that_one() -> None:
    chat = FakeChat(
        [{"choice": "wage_advance"}, {"svc-amount": {"value": 1000000, "unit": "IRT"}, "svc-target": None}]
    )
    app = RecordingApp()
    title = next(item["title"] for item in PROFILE["features"] if item["intent"] == "wage_advance")
    result = build_agent(chat, app).invoke(
        new_request("c", f"{title} یک میلیون تومن بریز به پس‌انداز"), CONFIG
    )
    assert result["task_state"] == "ready_for_user"
    assert sent_values(app)["svc-target"] == "account-2"


def test_a_selection_the_feature_decides_is_set_by_code_and_never_asked() -> None:
    chat = FakeChat([{"choice": "credit_report"}, {"svc-source": "سپرده کوتاه‌مدت"}])
    app = RecordingApp()
    result = build_agent(chat, app).invoke(new_request("c", "گزارش اعتباری بگیر از سپرده کوتاه‌مدت"), CONFIG)
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
    sentence = "یک میلیون ریال از پس‌انداز به شماره حساب 0970836557800 واریز کن، بابت خرید کالا"
    found = {
        "account-transfer-source": "پس‌انداز",
        "account-transfer-destination": "****6115",
        "account-transfer-amount": {"value": 1000000, "unit": "IRR"},
        "account-transfer-reason": "خرید کالا و خدمات",
    }
    app = RecordingApp()
    result = build_agent(FakeChat([{"choice": "interbank_transfer"}, found]), app).invoke(
        new_request("c", sentence), CONFIG
    )
    assert result["__interrupt__"][0].value["question"].endswith("شماره حساب یا شبای مقصد چیست؟")
    assert not reviewed(app)


PLATE = {"toll-plate-0": "12", "toll-letter": "ب", "toll-plate-2": "345", "toll-plate-3": "67"}
NO_PLATE = dict.fromkeys(PLATE)


def test_with_no_plate_named_the_saved_plates_are_offered_first() -> None:
    chat = FakeChat([{"choice": "pollution_inquiry"}, NO_PLATE])
    app = RecordingApp()
    pending = (
        build_agent(chat, app)
        .invoke(new_request("c", "تردد طرح آلودگی هوا"), CONFIG)["__interrupt__"][0]
        .value
    )
    assert pending["question"] == "کدام پلاک؟"
    assert [item["ref"] for item in pending["options"]] == [
        "toll-vehicle=plate-1",
        "toll-vehicle=new",
        "cancel",
    ]
    assert "toll-vehicle" not in chat.schemas[1]["properties"]  # only the user picks a saved plate
    assert not reviewed(app)


def test_a_saved_plate_is_tapped_in_the_plate_list_and_fills_the_plate() -> None:
    app = RecordingApp()
    graph = build_agent(FakeChat([{"choice": "pollution_inquiry"}, NO_PLATE]), app)
    graph.invoke(new_request("c", "تردد طرح آلودگی هوا"), CONFIG)
    result = graph.invoke(Command(resume="toll-vehicle=plate-1"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    navigate = [arguments for operation, arguments in app.sent if operation == "navigate"]
    # Went as far as the plate's rows and waited there, then went on with the plate.
    assert navigate == [
        {"destination": "S-257"},
        {"destination": "S-257", "arguments": {"toll-vehicle": "plate-1"}},
    ]
    assert sent_values(app) == PLATE


def test_a_new_plate_asks_its_parts_and_a_typed_plate_is_a_new_one() -> None:
    chat = FakeChat([{"choice": "pollution_inquiry"}, NO_PLATE, PLATE])
    app = RecordingApp()
    graph = build_agent(chat, app)
    graph.invoke(new_request("c", "تردد طرح آلودگی هوا"), CONFIG)
    pending = graph.invoke(Command(resume="toll-vehicle=new"), CONFIG)["__interrupt__"][0].value
    assert pending["question"].split("\n")[0] == "دو رقم اول پلاک چیست؟"
    result = graph.invoke(Command(resume="12 ب 345 ایران 67"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert result["values"]["toll-vehicle"] == "new"
    assert sent_values(app) == PLATE


def test_a_plate_corrected_after_a_saved_one_replaces_it_on_the_form() -> None:
    other = {"toll-plate-0": "22", "toll-letter": "ج", "toll-plate-2": "111", "toll-plate-3": "11"}
    chat = FakeChat([{"choice": "pollution_inquiry"}, NO_PLATE, {"answer": "correction"}, other])
    app = RecordingApp()
    graph = build_agent(chat, app)
    graph.invoke(new_request("c", "تردد طرح آلودگی هوا"), CONFIG)
    graph.invoke(Command(resume="toll-vehicle=plate-1"), CONFIG)
    correction = next_input(graph.get_state(CONFIG).values, "c", "نه، پلاک 22 ج 111 ایران 11")
    result = graph.invoke(correction, CONFIG)
    assert result["task_state"] == "ready_for_user"
    # Planned again from the form the app is on: the typed plate replaces the saved one there.
    assert "toll-vehicle" not in result["values"]
    assert [arguments["values"] for operation, arguments in app.sent if operation == "fill_form"][-1] == other


def test_a_new_plate_is_asked_on_the_plate_form() -> None:
    app = RecordingApp()
    graph = build_agent(FakeChat([{"choice": "pollution_inquiry"}, NO_PLATE]), app)
    graph.invoke(new_request("c", "تردد طرح آلودگی هوا"), CONFIG)
    assert app.screen == "S-253"  # «کدام پلاک؟» waits on the vehicle menu
    graph.invoke(Command(resume="toll-vehicle=new"), CONFIG)
    assert app.screen == "S-257" and not reviewed(app)  # the plate's parts are asked on its form


LOAN = {"choice": "loan_request"}
LOAN_SAID = {
    "svc-product": "personal",
    "svc-amount": {"value": 100000000, "unit": "IRT"},
    "svc-months": "12",
}


def loan_request(answers: list) -> tuple:
    title = next(item["title"] for item in PROFILE["features"] if item["intent"] == "loan_request")
    app = RecordingApp()
    graph = build_agent(FakeChat([LOAN, LOAN_SAID, *answers]), app)
    pending = graph.invoke(new_request("c", f"{title} شخصی ۱۰۰ میلیون تومن در 12 قسط"), CONFIG)
    return app, graph, pending["__interrupt__"][0].value


def filled_forms(app: RecordingApp) -> list[str]:
    return [arguments["form"] for operation, arguments in app.sent if operation == "fill_form"]


def test_each_screen_is_asked_on_that_screen_after_the_ones_before_are_filled() -> None:
    app, graph, pending = loan_request([{"svc-job": "کارمند"}])
    assert pending["question"] == "شغل چیست؟"  # not the guarantor, who is asked on a later page
    assert app.screen == "S-286" and filled_forms(app) == ["S-287", "S-308"]
    pending = graph.invoke(Command(resume="کارمند"), CONFIG)["__interrupt__"][0].value
    assert pending["question"] == "نام ضامن چیست؟\nکد ملی ضامن چیست؟"
    assert app.screen == "S-335" and filled_forms(app)[-1] == "S-286" and not reviewed(app)


def test_a_value_changed_for_a_page_already_passed_is_filled_there_again() -> None:
    guarantor = {"svc-guarantor": "حمید نوروزی", "svc-guarantorCode": "5840573851"}
    raised = guarantor | {"svc-amount": {"value": 200000000, "unit": "IRT"}}
    app, graph, _ = loan_request([{"svc-job": "کارمند"}, raised])
    graph.invoke(Command(resume="کارمند"), CONFIG)
    result = graph.invoke(Command(resume="ضامن حمید نوروزی 5840573851، مبلغ رو ۲۰۰ میلیون تومن کن"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    loan = [arguments["values"] for operation, arguments in app.sent if operation == "fill_form"]
    assert loan[-3]["svc-amount"] == {"value": 2000000000, "unit": "IRR"}  # S-308 filled again
    assert filled_forms(app)[-4:] == ["S-287", "S-308", "S-286", "S-335"]


def test_values_given_in_another_order_are_each_asked_once() -> None:
    # P6-14, the owner's conversation: «هزارتومن» joined, the card by its last four digits, and a
    # pick that a later typed answer reads again from its label.
    nothing = {"card-transfer-source": None, "card-transfer-destination": None, "card-transfer-amount": None}
    chat = FakeChat(
        [
            CARD,
            nothing,  # «برو تو صفحه کارت به کارت»: nothing to read yet
            card("****3566", toman=500000),  # the reader copies the masked style for «آخرش ۳۵۶۶»
        ]
    )
    app = RecordingApp()
    graph = build_agent(chat, app)
    graph.invoke(new_request("c", "برو تو صفحه کارت به کارت"), CONFIG)
    result = graph.invoke(Command(resume="از کارتی که آخرش ۳۵۶۶ هست به رضا ۵۰۰ هزارتومن"), CONFIG)
    assert result["task_state"] == "ready_for_user"  # nothing asked again
    values = sent_values(app)
    assert values["card-transfer-source"] == "card-1"
    assert values["card-transfer-amount"] == {"value": 5000000, "unit": "IRR"}


def test_a_picked_option_survives_a_later_typed_answer() -> None:
    nothing = {"card-transfer-source": None, "card-transfer-destination": None, "card-transfer-amount": None}
    chat = FakeChat(
        [
            CARD,
            nothing | {"card-transfer-destination": "رضا"},
            {"card-transfer-amount": None},  # after the pick, only the amount is read: none yet
            # After the typed amount every value is read again; the pick is in the words as its label.
            card("****3566", toman=500000),
        ]
    )
    app = RecordingApp()
    graph = build_agent(chat, app)
    graph.invoke(new_request("c", "کارت به کارت به رضا"), CONFIG)
    result = graph.invoke(Command(resume="card-transfer-source=card-1"), CONFIG)
    assert result["__interrupt__"][0].value["question"] == "چند ریال منتقل شود؟"
    assert graph.get_state(CONFIG).values["messages"][-1] == "****3566"  # the pick, as the user reads it
    result = graph.invoke(Command(resume="۵۰۰ هزار تومن"), CONFIG)
    assert result["task_state"] == "ready_for_user"
    assert sent_values(app)["card-transfer-source"] == "card-1"
