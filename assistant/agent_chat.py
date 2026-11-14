"""The graph page's chat box: one conversation with the agent per page load.

Called by graph_page. Like graph_tasks, it runs each turn on a background thread, because the
prepare node waits for the app through BridgeLink. It refuses a turn while a graph-page Run is
going, and graph_tasks refuses a Run while a turn is going. A message to a task at its review or
paused continues that task. Cancel stops a running turn between commands, or answers the question
or the task with «لغو». It only starts or resumes the agent graph, so it adds no way to confirm,
submit or pay.
"""

import traceback
import uuid
from threading import Event, Thread

from langgraph.graph.state import CompiledStateGraph
from langgraph.types import Command

from assistant.graph_tasks import GraphTasks
from assistant.lifecycle import CANCEL, FOLLOW_UP, next_input

INTERNAL_ERROR = "خطای داخلی پیش آمد؛ کاری انجام نشد."


class TurnRefused(Exception):
    """A turn cannot start now. The message is the reason, as a short code."""


class AgentChat:
    """The conversation shown on the graph page."""

    def __init__(self, graph: CompiledStateGraph, tasks: GraphTasks, stop: Event | None = None) -> None:
        self.graph = graph
        self.tasks = tasks
        self.stop = stop or Event()  # the agent's tools read it: set, a running task stops
        self.thread: Thread | None = None
        self.new_conversation()

    def new_conversation(self) -> None:
        """Forgets the shown conversation and starts another, unless a turn is still running."""
        if self.busy():
            return
        self.conversation_id = uuid.uuid4().hex
        self.lines: list[dict] = []
        self.pending: dict | None = None
        self.task_state: str | None = None
        self.report: dict | None = None

    def busy(self) -> bool:
        """True while a turn runs."""
        return self.thread is not None and self.thread.is_alive()

    def start(self, text: str | None, option: str | None) -> None:
        """Starts a turn with the user's text, or with an option of the pending question."""
        if self.busy():
            raise TurnRefused("turn_in_progress")
        if self.tasks.report.state == "running":
            raise TurnRefused("run_in_progress")
        if option is not None:
            labels = {item["ref"]: item["label"] for item in (self.pending or {}).get("options", [])}
            if option not in labels:
                raise TurnRefused("unknown_option")
            text = labels[option]
        self.lines.append({"from": "user", "text": text})
        self.stop.clear()
        self.thread = Thread(target=self.turn, args=(option or text,), daemon=True)
        self.thread.start()

    def cancel(self) -> None:
        """Stops a running turn between commands, or cancels the question or the task with «لغو»."""
        if self.busy():
            self.stop.set()
            self.lines.append({"from": "user", "text": CANCEL["label"]})
        elif self.pending is not None:
            self.start(None, CANCEL["ref"])
        elif self.task_state in FOLLOW_UP:
            self.start(CANCEL["label"], None)
        else:
            raise TurnRefused("nothing_to_cancel")

    def turn(self, answer: str) -> None:
        """Resumes the paused question with the answer, or sends the message: a follow-up of a task
        at its review or paused, else a new request."""
        config = {"configurable": {"thread_id": self.conversation_id}}
        try:
            if self.pending is not None:
                result = self.graph.invoke(Command(resume=answer), config)
            else:
                previous = self.graph.get_state(config).values
                result = self.graph.invoke(next_input(previous, self.conversation_id, answer), config)
        except Exception:
            traceback.print_exc()
            self.pending = None
            self.lines.append({"from": "assistant", "text": INTERNAL_ERROR})
            return
        self.pending = result["__interrupt__"][0].value if "__interrupt__" in result else None
        self.task_state = self.pending["task_state"] if self.pending else result["task_state"]
        self.report = result["report"]
        self.lines.append(
            {"from": "assistant", "text": self.pending["question"] if self.pending else result["reply"]}
        )

    def progress(self) -> dict:
        """The messages, the pending question and options, the task state and the runner's report."""
        return {
            "messages": self.lines,
            "question": self.pending["question"] if self.pending else None,
            "options": self.pending["options"] if self.pending else [],
            "task_state": self.task_state,
            "report": self.report,
            "busy": self.busy(),
        }
