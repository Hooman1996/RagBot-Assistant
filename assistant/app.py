"""Shared FastAPI wiring for the standalone service and the development app.

Started by `python -m assistant`, by tests through create_app, and by `main_debug.py --replica`.
It listens on a local address only. ASSISTANT_AGENT picks the agent: `workflow` (the default) or
`react` (D-032). The agent reads the Gemma model and the TEI embeddings, and only when a turn
runs. There is no RAG and no route to a bank or a mock bank. Its only data is the read-only
fixtures behind lookups.
"""

import os
import subprocess
from threading import Event

from fastapi import FastAPI

from assistant.agent_chat import Chats
from assistant.agent_nodes import Tools
from assistant.bridge_link import BridgeLink
from assistant.command_queue import CommandQueue
from assistant.graph_data import read_json
from assistant.intent import FeatureIndex
from assistant.lookups import FixtureLookups
from assistant.mobile_api import DEVICE_ID, build_mobile_router, development_session, standalone_session
from assistant.models import Chat, Embeddings
from assistant.navigator import HOME, Navigator
from assistant.openapi import standalone_openapi
from assistant.panel_api import build_panel_router
from assistant.react_graph import AGENTS, build_agent


def source_revision() -> str:
    """The checkout's short revision, so a running backend can be matched to the code."""
    done = subprocess.run(["git", "rev-parse", "--short", "HEAD"], capture_output=True, text=True, check=True)
    return done.stdout.strip()


def create_app(queue: CommandQueue | None = None) -> FastAPI:
    """Builds the development app with the graph viewer and test harness."""
    return build_app(queue or CommandQueue(), development=True)


def create_standalone_app() -> FastAPI:
    """Builds the standalone Assistant API for Uvicorn's factory entry point."""
    return build_app(CommandQueue(), development=False)


def build_app(commands: CommandQueue, *, development: bool) -> FastAPI:
    """Wires the workflow to the session's command loop in both environments."""
    profile = read_json("replica.profile.json")
    tasks = None
    if development:
        from assistant.graph_tasks import GraphTasks

        tasks = GraphTasks(commands, DEVICE_ID, profile)
    navigator = tasks.navigator if tasks else Navigator(profile)
    lookups = tasks.lookups if tasks else FixtureLookups()
    index = FeatureIndex(profile["features"], Embeddings())
    chat = Chat()
    agent = os.environ.get("ASSISTANT_AGENT", "workflow")
    if agent not in AGENTS:
        raise ValueError(f"ASSISTANT_AGENT must be one of {AGENTS}, not {agent!r}")

    def graph_for(session_id: str, stop: Event):
        def screen() -> str:
            seen = commands.device(session_id).observation
            return seen["screen"] if seen else HOME

        bridge = BridgeLink(commands, session_id, stopped=stop.is_set)
        return build_agent(Tools(chat, index, navigator, lookups, bridge, screen, stop.is_set), agent)

    def run_busy() -> bool:
        return tasks is not None and tasks.report.state == "running"

    title = "RagBot Assistant development backend" if development else "RagBot Assistant standalone API"
    app = FastAPI(
        title=title,
        docs_url="/docs" if development else None,
        redoc_url="/redoc" if development else None,
    )
    session = development_session if development else standalone_session
    chats = Chats(commands, graph_for, run_busy)
    app.include_router(build_mobile_router(commands, session))
    app.include_router(build_panel_router(chats, session))
    if tasks is None:
        app.openapi_schema = standalone_openapi()
        return app

    from assistant.graph_data import graph_payload
    from assistant.graph_page import build_graph_router
    from assistant.test_harness_api import build_harness_router

    chats.page = chats.new_chat(DEVICE_ID)
    tasks.chat_busy = chats.busy
    app.include_router(build_harness_router(commands, set(profile["screens"])))
    app.include_router(
        build_graph_router(commands, tasks, chats.page, graph_payload(), DEVICE_ID, source_revision())
    )
    return app
