"""The agent's steps in the panel's history and the graph page's chat (RA-4): the ReAct agent's own
thought, tool and result, or the workflow's nodes, always masked."""

import time
from threading import Event

from agent_fakes import CARD_SENTENCE, PROFILE, FakeChat, FakeEmbeddings, RecordingApp, build_agent
from fastapi import FastAPI
from fastapi.testclient import TestClient
from samples import observation

from assistant.agent_chat import Chats
from assistant.agent_nodes import Tools
from assistant.app import DEVICE_ID
from assistant.command_queue import CommandQueue
from assistant.graph_page import build_graph_router
from assistant.graph_tasks import GraphTasks
from assistant.intent import FeatureIndex
from assistant.lookups import FixtureLookups
from assistant.models import Chat
from assistant.navigator import Navigator
from assistant.panel_api import RESULT_CHARACTERS, brief, build_panel_router
from assistant.react_graph import build_react_graph

CARD_VALUES = {
    "card-transfer-source": None,
    "card-transfer-destination": "رضا",
    "card-transfer-amount": {"value": 250000, "unit": "IRT"},
}


class StepChat(FakeChat):
    """The scripted model, read through the real Chat.step."""

    step = Chat.step

    def json(self, prompt: str, schema: dict, tokens: int = 0) -> dict:
        return super().json(prompt, schema)


def call(tool: str, thought: str, **arguments: object) -> dict:
    return {"thought": thought, "tool": tool, "arguments": arguments}


def react(answers: list) -> object:
    app = RecordingApp()
    index = FeatureIndex(PROFILE["features"], FakeEmbeddings())
    return build_react_graph(
        Tools(StepChat(answers), index, Navigator(PROFILE), FixtureLookups(), app, app.current_screen)
    )


def served(graph) -> TestClient:
    """The panel's and the graph page's routes over one agent graph."""
    queue = CommandQueue()
    tasks = GraphTasks(queue, DEVICE_ID, PROFILE)
    chats = Chats(graph, tasks, Event())
    app = FastAPI()
    app.include_router(build_panel_router(chats))
    app.include_router(build_graph_router(queue, tasks, chats.page, {}, DEVICE_ID, "test"))
    return TestClient(app)


def message(text: str) -> dict:
    return {
        "request_id": f"r-{time.monotonic_ns()}",
        "conversation_id": "panel-1",
        "session_id": DEVICE_ID,
        "device_id": DEVICE_ID,
        "user_context": {"user_ref": "synthetic-user", "locale": "fa-IR"},
        "observation": observation(),
        "message": text,
    }


def settled(client: TestClient) -> dict:
    for _ in range(200):
        history = client.get("/assistant/v1/history", params={"conversation_id": "panel-1"}).json()
        if not history["busy"]:
            return history
        time.sleep(0.02)
    raise AssertionError("the turn did not end")


def test_the_react_agents_steps_show_its_thoughts_tools_and_results() -> None:
    answers = [
        call("find_features", "کار را پیدا می‌کنم.", request="کارت به کارت"),
        call("start_task", "کارت به کارت را شروع می‌کنم.", feature="card_transfer", request="…", second=None),
        call("set_values", "مقدارها را می‌گذارم.", values=CARD_VALUES),
        CARD_VALUES,  # the workflow's reader
        call("ask_user", "مبدا را می‌پرسم.", question="کدام کارت؟"),
    ]
    client = served(react(answers))
    client.post("/assistant/v1/messages", json=message(CARD_SENTENCE))
    steps = settled(client)["agent_steps"]
    assert [step["action"] for step in steps] == [
        "find_features",
        "start_task",
        "set_values",
        "move_app",
        "ask_user",
    ]
    assert steps[1]["thought"] == "کارت به کارت را شروع می‌کنم."
    assert '"started": "کارت به کارت"' in steps[1]["result"]
    assert "رضا محمدی ****8880" in steps[2]["result"]
    assert steps[-1]["result"] == ""  # the question still waits
    assert all(len(step["result"]) <= RESULT_CHARACTERS for step in steps)
    assert "6037990836388880" not in str(steps)


def test_the_workflows_steps_are_its_nodes() -> None:
    chat = FakeChat([{"choice": "card_transfer"}, CARD_VALUES])
    client = served(build_agent(chat, RecordingApp()))
    client.post("/assistant/v1/messages", json=message(CARD_SENTENCE))
    steps = settled(client)["agent_steps"]
    assert [step["action"] for step in steps] == [
        "route",
        "understand",
        "plan",
        "extract",
        "check",
        "advance",
        "clarify",
    ]
    assert steps[0]["action"] == "route" and steps[0]["thought"] == "خواندن پیام در گفت‌وگو"
    assert steps[1] == {"thought": "تشخیص کار درخواستی", "action": "understand", "result": '"resolving"'}
    assert steps[-1]["result"] == ""  # the question still waits


def test_the_graph_pages_chat_shows_the_same_steps() -> None:
    client = served(react([call("reply", "سلام می‌کنم.", text="سلام! بفرمایید.")]))
    client.post("/graph/chat", json={"text": "سلام"})
    for _ in range(200):
        progress = client.get("/graph/chat").json()
        if not progress["busy"]:
            break
        time.sleep(0.02)
    assert progress["agent_steps"] == [
        {"thought": "سلام می‌کنم.", "action": "reply", "result": '{"said": "سلام! بفرمایید."}'}
    ]


def test_a_long_result_is_cut_and_masked() -> None:
    text = brief({"records": ["6104336383233566"] * 20})
    assert len(text) == RESULT_CHARACTERS and text.endswith("…")
    assert "6104336383233566" not in text and "****3566" in text
