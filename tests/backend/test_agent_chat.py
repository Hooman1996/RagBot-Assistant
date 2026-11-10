"""The graph page's chat box: it and a graph-page Run refuse each other, and its routes are strict."""

from threading import Event

import pytest
from fastapi.testclient import TestClient

from assistant.agent_chat import AgentChat, TurnRefused
from assistant.app import DEVICE_ID, create_app
from assistant.command_queue import CommandQueue
from assistant.graph_data import read_json
from assistant.graph_tasks import GraphTasks, RunInProgress
from assistant.task_runner import TaskReport


class SlowGraph:
    """A graph whose turn lasts until the test lets it end."""

    def __init__(self) -> None:
        self.release = Event()

    def invoke(self, *_: object) -> dict:
        self.release.wait(timeout=5)
        return {"task_state": "created", "reply": "done", "report": None}


def test_a_run_and_a_chat_turn_refuse_each_other() -> None:
    tasks = GraphTasks(CommandQueue(), DEVICE_ID, read_json("replica.profile.json"))
    graph = SlowGraph()
    chat = AgentChat(graph, tasks)
    tasks.chat_busy = chat.busy
    chat.start("سلام", None)
    with pytest.raises(RunInProgress):
        tasks.start("S-174", {})
    with pytest.raises(TurnRefused, match="turn_in_progress"):
        chat.start("دوباره", None)
    graph.release.set()
    chat.thread.join(timeout=5)
    assert chat.progress()["messages"][-1] == {"from": "assistant", "text": "done"}
    tasks.report = TaskReport()
    with pytest.raises(TurnRefused, match="run_in_progress"):
        chat.start("سلام", None)


def test_an_option_must_be_one_the_agent_offered() -> None:
    chat = AgentChat(SlowGraph(), GraphTasks(CommandQueue(), DEVICE_ID, read_json("replica.profile.json")))
    with pytest.raises(TurnRefused, match="unknown_option"):
        chat.start(None, "card-transfer-source=card-1")


@pytest.mark.parametrize(
    "body", [{}, {"text": "a", "option": "b"}, {"text": ""}, {"text": "a", "confirm": True}, {"option": 1}]
)
def test_a_turn_takes_exactly_one_of_text_or_option(body: dict) -> None:
    assert TestClient(create_app()).post("/graph/chat", json=body).status_code == 422


def test_a_fresh_page_shows_an_empty_conversation() -> None:
    client = TestClient(create_app())
    assert client.get("/graph").status_code == 200
    assert client.get("/graph/chat").json() == {
        "messages": [],
        "question": None,
        "options": [],
        "task_state": None,
        "report": None,
        "busy": False,
    }


def test_the_chat_script_needs_nothing_from_the_internet() -> None:
    script = TestClient(create_app()).get("/graph/graph_chat.js").text
    assert 'fetch("/graph/chat"' in script
    assert "http://" not in script and "https://" not in script
