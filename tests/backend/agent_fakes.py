"""Fakes for the agent tests: a scripted chat model, fixed embeddings and a recording stand-in app.

No test here touches the network. FakeChat answers in order and keeps every prompt it was shown.
every_change_is_allowed, imported by a test module, checks each task-state change against task.json.
"""

import json
from collections.abc import Callable
from pathlib import Path

import pytest

from assistant import agent_lifecycle, agent_nodes
from assistant.agent_graph import build_graph
from assistant.agent_nodes import Tools
from assistant.app import DEVICE_ID
from assistant.command_queue import CommandQueue
from assistant.intent import FeatureIndex
from assistant.lookups import FixtureLookups
from assistant.models import ModelError
from assistant.navigator import HOME, Navigator
from tools.stand_in_app import StandInApp

PROFILE = json.loads(Path("app_profiles/replica.profile.json").read_text(encoding="utf-8"))
TRANSITIONS = json.loads(Path("contracts/transitions/task.json").read_text(encoding="utf-8"))
CARD_SENTENCE = "۲۵۰ هزار تومان به رضا کارت به کارت کن"
CHANGES: list[tuple[str, str]] = []  # every task-state change of the running test


@pytest.fixture(autouse=True)
def every_change_is_allowed(monkeypatch: pytest.MonkeyPatch):
    """Records each task-state change every node makes, and checks it against task.json."""
    real = agent_nodes.moved

    def recording(state: dict, *path: str) -> dict:
        current = state["task_state"]
        for step in path:
            if step != current:
                CHANGES.append((current, step))
            current = step
        return real(state, *path)

    CHANGES.clear()
    monkeypatch.setattr(agent_nodes, "moved", recording)
    monkeypatch.setattr(agent_lifecycle, "moved", recording)
    yield
    assert all(step in TRANSITIONS[current] for current, step in CHANGES), CHANGES


FOLLOW_UP_KINDS = {"correction": "correction", "new_request": "new_task"}  # the old follow-up's answers


class FakeChat:
    """Returns the scripted answers in order, and records each prompt and schema it was given. The
    turn router (D-034) is answered as the code before it behaved, unless the script names a kind: a
    new task with no task open, else an answer; an old follow-up answer becomes its kind. Its
    prompts are kept apart, in routed."""

    def __init__(self, answers: list[dict | Exception]) -> None:
        self.answers = list(answers)
        self.prompts: list[str] = []
        self.schemas: list[dict] = []
        self.routed: list[str] = []

    def json(self, prompt: str, schema: dict) -> dict:
        if "kind" in schema.get("properties", {}):
            return self.route(prompt)
        self.prompts.append(prompt)
        self.schemas.append(schema)
        answer = self.answers.pop(0)
        if isinstance(answer, Exception):
            raise answer
        return answer

    def route(self, prompt: str) -> dict:
        """The router's answer: the scripted one if it is next, else as the code before it behaved."""
        self.routed.append(prompt)
        following = self.answers[0] if self.answers and isinstance(self.answers[0], dict) else {}
        if "kind" in following:
            return {"standalone": None, "rest": None} | self.answers.pop(0)
        if following.get("answer") in FOLLOW_UP_KINDS:
            return {"kind": FOLLOW_UP_KINDS[self.answers.pop(0)["answer"]], "standalone": None, "rest": None}
        kind = "new_task" if "Open task: none." in prompt else "answer"
        return {"kind": kind, "standalone": None, "rest": None}


class FakeEmbeddings:
    """One axis per feature. A query points at every feature whose Persian title it contains."""

    def __init__(self) -> None:
        self.titles = [item["title"] for item in PROFILE["features"]]

    def embed(self, texts: list[str], query: bool = False) -> list[list[float]]:
        if not query:
            return [
                [1.0 if index == row else 0.0 for index in range(len(self.titles))]
                for row in range(len(texts))
            ]
        return [[1.0 if title in text else 0.0 for title in self.titles] for text in texts]


class DownEmbeddings:
    """An embedding service that cannot be reached."""

    def embed(self, texts: list[str], query: bool = False) -> list[list[float]]:
        raise ModelError("embeddings: ConnectError")


class RecordingApp(StandInApp):
    """The stand-in app, keeping every operation it was sent."""

    def __init__(self, screen: str = HOME) -> None:
        self.sent: list[tuple[str, dict]] = []
        super().__init__(PROFILE, CommandQueue(), DEVICE_ID, screen)

    def send(self, operation: str, arguments: dict) -> dict:
        self.sent.append((operation, arguments))
        return super().send(operation, arguments)


def reviewed(app: RecordingApp) -> bool:
    """True once the app was asked to open a review; before a question it only moves and fills."""
    return any(operation == "show_review" for operation, _ in app.sent)


def given(app: RecordingApp) -> dict:
    """Every value and selection the app was sent, by ref."""
    found = {}
    for _, arguments in app.sent:
        found |= arguments.get("values", {}) | arguments.get("arguments", {})
    return found


def build_agent(
    chat: FakeChat, app: RecordingApp, embeddings: object | None = None, stopped: Callable[[], bool] = bool
):
    """The real agent graph over the fakes. stopped says when the user has cancelled."""
    index = FeatureIndex(PROFILE["features"], embeddings or FakeEmbeddings())
    tools = Tools(chat, index, Navigator(PROFILE), FixtureLookups(), app, app.current_screen, stopped)
    return build_graph(tools)
