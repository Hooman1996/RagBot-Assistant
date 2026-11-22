"""Conversations with the agent: the graph page's chat box, and the replica's assistant panel.

Called by graph_page and panel_api. Like graph_tasks, it runs each turn on a background thread, because the
prepare node waits for the app through BridgeLink. It refuses a turn while a graph-page Run is
going, and graph_tasks refuses a Run while a turn is going. A message to a task at its review or
paused continues that task. Cancel stops a running turn between commands, or answers the question
or the task with «لغو». It only starts or resumes the agent graph, so it adds no way to confirm,
submit or pay.
"""

from __future__ import annotations

import traceback
import uuid
from collections.abc import Callable
from threading import Event, RLock, Thread

from langgraph.graph.state import CompiledStateGraph
from langgraph.types import Command

from assistant.command_queue import CommandQueue, DeviceMismatch
from assistant.lifecycle import CANCEL, FOLLOW_UP, next_input

INTERNAL_ERROR = "خطای داخلی پیش آمد؛ کاری انجام نشد."


class TurnRefused(Exception):
    """A turn cannot start now. The message is the reason, as a short code."""


class AgentChat:
    """One conversation: the graph page's, or one the replica's panel started."""

    def __init__(
        self,
        graph: CompiledStateGraph,
        *,
        stop: Event | None = None,
        conversation_id: str | None = None,
        desk_busy: Callable[[], bool] | None = None,
        run_busy: Callable[[], bool] = bool,
        lock: RLock | None = None,
    ) -> None:
        self.graph = graph
        self.stop = stop or Event()  # the agent's tools read it: set, a running task stops
        self.desk_busy = desk_busy or self.busy  # true while any conversation of this backend runs a turn
        self.run_busy = run_busy
        self.lock = lock or RLock()
        self.thread: Thread | None = None
        self.new_conversation(conversation_id)

    def new_conversation(self, conversation_id: str | None = None) -> None:
        """Forgets the shown conversation and starts another, unless a turn is still running."""
        with self.lock:
            if self.busy():
                return
            self.conversation_id = conversation_id or uuid.uuid4().hex
            self.lines: list[dict] = []
            self.pending: dict | None = None
            self.pending_id: str | None = None
            self.task_state: str | None = None
            self.report: dict | None = None

    def busy(self) -> bool:
        """True while a turn runs."""
        return self.thread is not None and self.thread.is_alive()

    def start(self, text: str | None, option: str | None) -> None:
        """Starts a turn with the user's text, or with an option of the pending question."""
        with self.lock:
            self.launch_turn(self.prepare_turn(text, option), option)

    def prepare_turn(self, text: str | None, option: str | None) -> str:
        """Checks that a turn can start and finds the text to display for a selected option."""
        if self.busy() or self.desk_busy():
            raise TurnRefused("turn_in_progress")
        if self.run_busy():
            raise TurnRefused("run_in_progress")
        if option is not None:
            labels = {item["ref"]: item["label"] for item in (self.pending or {}).get("options", [])}
            if option not in labels:
                raise TurnRefused("unknown_option")
            return labels[option]
        assert text is not None
        return text

    def launch_turn(self, text: str, option: str | None) -> None:
        """Starts an accepted turn while the caller holds the dispatch lock."""
        self.lines.append({"from": "user", "text": text})
        self.stop.clear()
        self.thread = Thread(target=self.turn, args=(option or text,), daemon=True)
        self.thread.start()

    def cancel(self) -> None:
        """Stops a running turn between commands, or cancels the question or the task with «لغو»."""
        with self.lock:
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
            with self.lock:
                self.pending = None
                self.pending_id = None
                self.lines.append({"from": "assistant", "text": INTERNAL_ERROR})
            return
        with self.lock:
            self.pending = result["__interrupt__"][0].value if "__interrupt__" in result else None
            self.pending_id = uuid.uuid4().hex if self.pending is not None else None
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

    def question_id(self) -> str | None:
        """The pending question's ID, which an answer by option must name."""
        return self.pending_id

    def live_state(self) -> str | None:
        """The task state of the last step the agent finished, readable while a turn runs."""
        state = self.graph.get_state({"configurable": {"thread_id": self.conversation_id}}).values
        return state.get("task_state", self.task_state)


class Chats:
    """Session-owned panel conversations and the optional development page. One turn runs at a time."""

    def __init__(
        self,
        queue: CommandQueue,
        graph_for: Callable[[str, Event], CompiledStateGraph],
        run_busy: Callable[[], bool] = bool,
    ) -> None:
        self.queue = queue
        self.graph_for = graph_for
        self.run_busy = run_busy
        self.lock = RLock()
        self.panels: dict[tuple[str, str], AgentChat] = {}
        self.active: dict[str, AgentChat] = {}
        self.page: AgentChat | None = None

    def new_chat(self, session_id: str, conversation_id: str | None = None) -> AgentChat:
        """Creates a conversation whose graph and stop event belong to this session."""
        stop = Event()
        return AgentChat(
            self.graph_for(session_id, stop),
            stop=stop,
            conversation_id=conversation_id,
            desk_busy=self.busy,
            run_busy=self.run_busy,
            lock=self.lock,
        )

    def busy(self) -> bool:
        """Reports whether any conversation is running a turn."""
        with self.lock:
            return (self.page is not None and self.page.busy()) or any(
                chat.busy() for chat in self.active.values()
            )

    def send(self, session_id: str, request: dict) -> AgentChat:
        """Starts or answers a conversation within the request's session."""
        key = (session_id, request["conversation_id"])
        with self.lock:
            chat = self.panels.get(key)
            selection = request.get("selection")
            if selection is not None and (chat is None or selection["question_id"] != chat.question_id()):
                raise TurnRefused("stale_question")
            if chat is None:
                chat = self.new_chat(*key)
            option = selection["ref"] if selection else None
            text = chat.prepare_turn(request.get("message"), option)
            try:
                self.queue.bind_device(session_id, request["device_id"])
            except DeviceMismatch as error:
                raise TurnRefused("device_mismatch") from error
            chat.launch_turn(text, option)
            self.panels[key] = chat
            self.active[session_id] = chat
            return chat

    def cancel(self, session_id: str) -> AgentChat:
        """Cancels only the current conversation of the named session."""
        with self.lock, self.queue.lock:
            chat = self.active.get(session_id)
            if chat is None:
                raise TurnRefused("nothing_to_cancel")
            chat.cancel()
            if chat.stop.is_set():
                self.queue.cancel_unsent(session_id)
            return chat
