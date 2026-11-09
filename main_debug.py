"""Runs the agent top to bottom in one process, so every line can be stepped through.

`python main_debug.py ["turn" ...]` drives a stand-in app on this thread, with no web server.
`python main_debug.py --replica` drives the real replica on the emulator instead. Both use the
real Gemma and TEI services. The run stops at the review: nothing here can confirm or pay.
"""

import os
import sys

os.environ["ASSISTANT_TRACE"] = "1"  # print every step the assistant takes
os.environ["ASSISTANT_DEBUG"] = (
    "1"  # set before the imports below: a paused breakpoint must not expire a command
)

from threading import Thread

import uvicorn
from langgraph.types import Command

from assistant.agent_graph import build_graph
from assistant.agent_nodes import Tools
from assistant.agent_state import new_request
from assistant.app import DEVICE_ID, create_app
from assistant.bridge_link import BridgeLink
from assistant.command_queue import CommandQueue
from assistant.graph_data import read_json
from assistant.intent import FeatureIndex
from assistant.lookups import FixtureLookups
from assistant.models import Chat, Embeddings
from assistant.navigator import HOME, Navigator
from tools.stand_in_app import StandInApp

EXAMPLE = ["۲۵۰ هزار تومان به رضا کارت به کارت کن", "1"]  # the request, then option 1 for the source card
CONVERSATION = "main-debug"


def answer_for(turn: str, pending: dict) -> str:
    """An option's number becomes that option's ref. Anything else is the user's own words."""
    options = pending["options"]
    return options[int(turn) - 1]["ref"] if turn.isdigit() and 1 <= int(turn) <= len(options) else turn


def said(result: dict) -> dict | None:
    """Prints what the agent says, and returns its pending question, if it asked one."""
    pending = result["__interrupt__"][0].value if "__interrupt__" in result else None
    print("\nassistant:", pending["question"] if pending else result["reply"])
    for number, option in enumerate(pending["options"] if pending else [], start=1):
        print(f"  {number}. {option['label']}")
    print("task_state:", pending["task_state"] if pending else result["task_state"], "\n")
    return pending


def main(chat: Chat, embeddings: Embeddings, turns: list[str], replica: bool = False) -> dict:
    """Builds the same objects as app.py, except the bridge, and talks to the agent turn by turn."""
    profile = read_json("replica.profile.json")  # screens, edges, forms and features
    navigator = Navigator(profile)  # plans routes and lists requirements
    queue = CommandQueue()  # the real command queue
    if replica:
        # The backend's own app, so the replica can poll it. Its thread pauses with any breakpoint.
        server = uvicorn.Server(uvicorn.Config(create_app(queue), host="127.0.0.1", port=8100))
        Thread(target=server.run, daemon=True).start()
        bridge = BridgeLink(queue, DEVICE_ID)  # waits for the replica, exactly as the server does

        def screen() -> str:
            """Where the replica is; Home until it reports in."""
            seen = queue.device(DEVICE_ID).observation
            return seen["screen"] if seen else HOME
    else:
        bridge = StandInApp(profile, queue, DEVICE_ID)  # plays the app through the same queue
        screen = bridge.current_screen
    index = FeatureIndex(profile["features"], embeddings)  # embeds every feature once, on first use
    tools = Tools(chat, index, navigator, FixtureLookups(), bridge, screen)
    graph = build_graph(tools)  # the compiled agent, with its in-memory checkpointer
    config = {"configurable": {"thread_id": CONVERSATION}}  # one conversation

    pending = None
    for turn in turns:
        print("user:", turn)
        if pending is None:
            result = graph.invoke(new_request(CONVERSATION, turn), config)  # a new request
        else:
            result = graph.invoke(Command(resume=answer_for(turn, pending)), config)  # answers the question
        pending = said(result)
    while pending is not None:  # the turns ran out, but the agent is still asking
        result = graph.invoke(Command(resume=answer_for(input("> "), pending)), config)
        pending = said(result)
    return result


if __name__ == "__main__":
    replica = "--replica" in sys.argv
    turns = [arg for arg in sys.argv[1:] if arg != "--replica"] or EXAMPLE
    main(Chat(), Embeddings(), turns, replica)
