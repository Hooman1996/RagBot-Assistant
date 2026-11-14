"""The FastAPI app: the command loop, the test harness, the graph page, its runs and its chat box.

Started by `python -m assistant`, by tests through create_app, and by `main_debug.py --replica`.
It listens on a local address only. The chat box's agent reads the Gemma model and the TEI
embeddings, and only when a turn runs. There is no RAG and no route to a bank or a mock bank. Its
only data is the read-only fixtures behind lookups.
"""

import subprocess
from threading import Event

from fastapi import FastAPI

from assistant.agent_chat import Chats
from assistant.agent_graph import build_graph
from assistant.agent_nodes import Tools
from assistant.bridge_link import BridgeLink
from assistant.command_queue import CommandQueue
from assistant.graph_data import built_screens, graph_payload, read_json
from assistant.graph_page import build_graph_router
from assistant.graph_tasks import GraphTasks
from assistant.intent import FeatureIndex
from assistant.mobile_api import build_mobile_router
from assistant.models import Chat, Embeddings
from assistant.panel_api import build_panel_router
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
    profile = read_json("replica.profile.json")
    tasks = GraphTasks(commands, DEVICE_ID, profile)
    index = FeatureIndex(profile["features"], Embeddings())
    bridge = BridgeLink(commands, DEVICE_ID)
    stop = Event()  # the chat box's cancel, read by the agent between commands
    tools = Tools(Chat(), index, tasks.navigator, tasks.lookups, bridge, tasks.current_screen, stop.is_set)
    chats = Chats(build_graph(tools), tasks, stop)  # the graph page's and the panel's conversations
    tasks.chat_busy = chats.busy
    app.include_router(build_panel_router(chats))
    app.include_router(
        build_graph_router(commands, tasks, chats.page, graph_payload(), DEVICE_ID, source_revision())
    )
    return app
