"""Serves the interactive graph: the page, its data, the replica's state, its runs and its chat box.

Called by the browser. A plain move goes through test_harness_api, a run through graph_tasks and a
chat turn through agent_chat. It offers no way to reach a confirm, submit or pay action: a run
takes only a target screen and the values its plan asks for, and a turn only the user's words or
one of the options the agent offered.
"""

from pathlib import Path

from fastapi import APIRouter, HTTPException
from fastapi.responses import HTMLResponse, PlainTextResponse
from pydantic import BaseModel, ConfigDict, Field, model_validator

from assistant.agent_chat import AgentChat, TurnRefused
from assistant.command_queue import CommandQueue
from assistant.graph_tasks import GraphTasks, RunInProgress
from assistant.navigator import CrossesUserOnly, NoRoute

STATIC = Path(__file__).resolve().parent / "static"
PAGE = STATIC / "graph.html"


class RunRequest(BaseModel):
    """What a run may be given. Any other field is refused."""

    model_config = ConfigDict(extra="forbid")

    screen: str
    values: dict[str, str]


class ChatRequest(BaseModel):
    """One chat turn: the user's text, or the ref of an offered option. Exactly one of them."""

    model_config = ConfigDict(extra="forbid")

    text: str | None = Field(default=None, min_length=1)
    option: str | None = Field(default=None, min_length=1)

    @model_validator(mode="after")
    def exactly_one(self) -> "ChatRequest":
        """Refuses a turn with both or neither."""
        if (self.text is None) == (self.option is None):
            raise ValueError("send exactly one of text or option")
        return self


def build_graph_router(
    queue: CommandQueue, tasks: GraphTasks, chat: AgentChat, payload: dict, device_id: str, revision: str
) -> APIRouter:
    """Builds the page's routes around one queue, its tasks, its chat and the graph data."""
    router = APIRouter(prefix="/graph")

    @router.get("", response_class=HTMLResponse)
    def page() -> str:
        """The page itself, with a new chat conversation. It loads nothing from the internet."""
        chat.new_conversation()
        return PAGE.read_text(encoding="utf-8")

    @router.get("/graph_map.js", response_class=PlainTextResponse)
    def map_script() -> str:
        """The script that draws the map."""
        return (STATIC / "graph_map.js").read_text(encoding="utf-8")

    @router.get("/graph_task.js", response_class=PlainTextResponse)
    def task_script() -> str:
        """The script that shows a click's route and runs it."""
        return (STATIC / "graph_task.js").read_text(encoding="utf-8")

    @router.get("/graph_chat.js", response_class=PlainTextResponse)
    def chat_script() -> str:
        """The script of the chat box."""
        return (STATIC / "graph_chat.js").read_text(encoding="utf-8")

    @router.get("/data")
    def data() -> dict:
        """Every screen and edge, placed."""
        return {**payload, "device_id": device_id}

    @router.get("/state")
    def state() -> dict:
        """Where the replica is, its last result, and the backend's source revision."""
        device = queue.device(device_id)
        return {
            "observation": device.observation,
            "last_result": device.last_result,
            "waiting": device.outstanding is not None,
            "source_revision": revision,
        }

    @router.get("/plan/{screen}")
    def plan(screen: str) -> dict:
        """The route from the replica's screen to this one, and every question along it."""
        return tasks.preview(screen)

    @router.get("/run")
    def run_progress() -> dict:
        """The current run: its state, and each command and observation so far."""
        return tasks.progress()

    @router.post("/run", status_code=202)
    def run(request: RunRequest) -> dict:
        """Starts the task runner. It stops at the review, and never confirms."""
        try:
            tasks.start(request.screen, request.values)
        except RunInProgress as error:
            raise HTTPException(status_code=409, detail="run_in_progress") from error
        except (NoRoute, CrossesUserOnly) as error:
            raise HTTPException(status_code=404, detail="no_route") from error
        return tasks.progress()

    @router.get("/chat")
    def chat_progress() -> dict:
        """The conversation: its messages, the pending question and options, and the task so far."""
        return chat.progress()

    @router.post("/chat", status_code=202)
    def chat_turn(request: ChatRequest) -> dict:
        """Starts or resumes the agent with the user's words or an offered option."""
        try:
            chat.start(request.text, request.option)
        except TurnRefused as error:
            raise HTTPException(status_code=409, detail=str(error)) from error
        return chat.progress()

    @router.post("/chat/cancel", status_code=202)
    def chat_cancel() -> dict:
        """Cancels the chat's task: stops a running turn between commands, or answers with «لغو»."""
        try:
            chat.cancel()
        except TurnRefused as error:
            raise HTTPException(status_code=409, detail=str(error)) from error
        return chat.progress()

    return router
