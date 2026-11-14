"""The replica panel's calls: a message, the history it reopens with, an option, and cancel."""

import time
from threading import Event
from types import SimpleNamespace

import pytest
from agent_fakes import CARD_SENTENCE, PROFILE, FakeChat, RecordingApp, build_agent
from fastapi import FastAPI
from fastapi.testclient import TestClient
from samples import observation

from assistant.agent_chat import Chats
from assistant.app import DEVICE_ID
from assistant.command_queue import CommandQueue
from assistant.contract_check import problems
from assistant.graph_tasks import GraphTasks, RunInProgress
from assistant.panel_api import build_panel_router

CARD = {"choice": "card_transfer"}
FULL = "۲۵۰ هزار تومان از کارت 0018 به رضا کارت به کارت کن"


def card(source: str | None = "کارت 0018") -> dict:
    amount = {"value": 250000, "unit": "IRT"}
    return {
        "card-transfer-source": source,
        "card-transfer-destination": "رضا",
        "card-transfer-amount": amount,
    }


def panel(answers: list) -> tuple[TestClient, Chats]:
    """The panel's routes over the real agent graph, a scripted model and the stand-in app."""
    tasks = GraphTasks(CommandQueue(), DEVICE_ID, PROFILE)
    chats = Chats(build_agent(FakeChat(answers), RecordingApp()), tasks, Event())
    app = FastAPI()
    app.include_router(build_panel_router(chats))
    return TestClient(app), chats


def message(text: str | None = None, selection: dict | None = None) -> dict:
    """A conversation request as the app sends it."""
    body = {
        "request_id": f"r-{time.monotonic_ns()}",
        "conversation_id": "panel-1",
        "session_id": DEVICE_ID,
        "device_id": DEVICE_ID,
        "user_context": {"user_ref": "synthetic-user", "locale": "fa-IR"},
        "observation": observation(),
    }
    return body | ({"message": text} if text is not None else {"selection": selection})


def settled(client: TestClient) -> dict:
    """The history once the turn has ended."""
    for _ in range(200):
        history = client.get("/assistant/v1/history", params={"conversation_id": "panel-1"}).json()
        if not history["busy"]:
            return history
        time.sleep(0.02)
    raise AssertionError("the turn did not end")


def test_a_message_reaches_the_review_and_the_history_shows_it() -> None:
    client, _ = panel([CARD, card()])
    reply = client.post("/assistant/v1/messages", json=message(FULL))
    assert reply.status_code == 200 and problems("response.schema.json", reply.json()) == []
    history = settled(client)
    assert [line["from"] for line in history["messages"]] == ["user", "assistant"]
    assert history["current"] == {"kind": "status", "task_state": "ready_for_user"}
    assert [step["operation"] for step in history["steps"]] == ["navigate", "fill_form", "show_review"]


def test_a_question_is_answered_by_its_option_and_a_stale_answer_is_refused() -> None:
    client, _ = panel([CARD, card(None)])
    client.post("/assistant/v1/messages", json=message(CARD_SENTENCE))
    question = settled(client)["current"]
    assert question["kind"] == "clarification" and problems("response.schema.json", question) == []
    assert question["options"][-1] == {"ref": "cancel", "label": "لغو"}
    stale = {"question_id": "q0", "ref": "card-transfer-source=card-1"}
    assert (
        client.post("/assistant/v1/messages", json=message(selection=stale)).json()["detail"]
        == "stale_question"
    )
    answer = {"question_id": question["question_id"], "ref": "card-transfer-source=card-1"}
    assert client.post("/assistant/v1/messages", json=message(selection=answer)).status_code == 200
    assert settled(client)["current"]["task_state"] == "ready_for_user"


def test_cancel_ends_the_pending_question() -> None:
    client, _ = panel([CARD, card(None)])
    assert client.post(f"/assistant/v1/sessions/{DEVICE_ID}/cancel").status_code == 409  # nothing yet
    client.post("/assistant/v1/messages", json=message(CARD_SENTENCE))
    settled(client)
    assert client.post(f"/assistant/v1/sessions/{DEVICE_ID}/cancel").status_code == 200
    assert settled(client)["current"] == {"kind": "status", "task_state": "cancelled"}


def test_a_body_outside_the_contract_is_refused() -> None:
    client, _ = panel([])
    body = message(FULL)
    del body["observation"]
    assert client.post("/assistant/v1/messages", json=body).status_code == 422


def test_an_unknown_conversation_reopens_empty() -> None:
    client, _ = panel([])
    history = client.get("/assistant/v1/history", params={"conversation_id": "never"}).json()
    assert history["messages"] == [] and history["current"] is None


class SlowGraph:
    """A graph whose turn lasts until the test lets it end."""

    def __init__(self) -> None:
        self.release = Event()

    def invoke(self, *_: object) -> dict:
        self.release.wait(timeout=5)
        return {"task_state": "created", "reply": "done", "report": None}

    def get_state(self, *_: object) -> SimpleNamespace:
        return SimpleNamespace(values={})


def test_the_panel_the_graph_page_and_a_run_take_one_turn_at_a_time() -> None:
    graph = SlowGraph()
    tasks = GraphTasks(CommandQueue(), DEVICE_ID, PROFILE)
    chats = Chats(graph, tasks, Event())
    tasks.chat_busy = chats.busy
    app = FastAPI()
    app.include_router(build_panel_router(chats))
    client = TestClient(app)
    chats.page.start("از صفحه گراف", None)
    assert client.post("/assistant/v1/messages", json=message(FULL)).json()["detail"] == "turn_in_progress"
    with pytest.raises(RunInProgress):
        tasks.start("S-174", {})
    graph.release.set()
    chats.page.thread.join(timeout=5)
    assert client.post("/assistant/v1/messages", json=message(FULL)).status_code == 200
