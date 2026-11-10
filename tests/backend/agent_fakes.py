"""Fakes for the agent tests: a scripted chat model, fixed embeddings and a recording stand-in app.

No test here touches the network. FakeChat answers in order and keeps every prompt it was shown.
"""

import json
from pathlib import Path

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


class FakeChat:
    """Returns the scripted answers in order, and records each prompt and schema it was given."""

    def __init__(self, answers: list[dict | Exception]) -> None:
        self.answers = list(answers)
        self.prompts: list[str] = []
        self.schemas: list[dict] = []

    def json(self, prompt: str, schema: dict) -> dict:
        self.prompts.append(prompt)
        self.schemas.append(schema)
        answer = self.answers.pop(0)
        if isinstance(answer, Exception):
            raise answer
        return answer


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


def build_agent(chat: FakeChat, app: RecordingApp, embeddings: object | None = None):
    """The real agent graph over the fakes."""
    index = FeatureIndex(PROFILE["features"], embeddings or FakeEmbeddings())
    tools = Tools(chat, index, Navigator(PROFILE), FixtureLookups(), app, app.current_screen)
    return build_graph(tools)
