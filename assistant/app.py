"""The FastAPI app: the command loop, the test harness, the graph page and its task runs.

Started by `python -m assistant`, and by tests through create_app. It listens on a local address
only. It has no LLM, no RAG and no route to a bank or a mock bank. Its only data is the read-only
fixtures behind lookups.
"""

import subprocess

from fastapi import FastAPI

from assistant.command_queue import CommandQueue
from assistant.graph_data import built_screens, graph_payload, read_json
from assistant.graph_page import build_graph_router
from assistant.graph_tasks import GraphTasks
from assistant.mobile_api import build_mobile_router
from assistant.test_harness_api import build_harness_router

DEVICE_ID = "replica-dev"


def source_revision() -> str:
    """The checkout's short revision, so a running backend can be matched to the code."""
    done = subprocess.run(["git", "rev-parse", "--short", "HEAD"], capture_output=True, text=True, check=True)
    return done.stdout.strip()


def create_app(queue: CommandQueue | None = None) -> FastAPI:
    """Builds the app. Tests pass a queue with their own clock."""
    commands = queue or CommandQueue()
    app = FastAPI(title="RagBot Assistant development backend")
    app.include_router(build_mobile_router(commands))
    app.include_router(build_harness_router(commands, built_screens()))
    tasks = GraphTasks(commands, DEVICE_ID, read_json("replica.profile.json"))
    app.include_router(build_graph_router(commands, tasks, graph_payload(), DEVICE_ID, source_revision()))
    return app
