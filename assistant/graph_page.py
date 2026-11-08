"""Serves the interactive graph: the page, its data, the replica's state and its task runs.

Called by the browser. A plain move goes through test_harness_api, and a run through graph_tasks.
It offers no way to reach a confirm, submit or pay action: a run takes only a target screen and
the values its plan asks for.
"""

from pathlib import Path

from fastapi import APIRouter, HTTPException
from fastapi.responses import HTMLResponse, PlainTextResponse
from pydantic import BaseModel, ConfigDict

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


def build_graph_router(
    queue: CommandQueue, tasks: GraphTasks, payload: dict, device_id: str, revision: str
) -> APIRouter:
    """Builds the page's routes around one queue, its tasks and the graph data."""
    router = APIRouter(prefix="/graph")

    @router.get("", response_class=HTMLResponse)
    def page() -> str:
        """The page itself. It loads nothing from the internet."""
        return PAGE.read_text(encoding="utf-8")

    @router.get("/graph_map.js", response_class=PlainTextResponse)
    def map_script() -> str:
        """The script that draws the map."""
        return (STATIC / "graph_map.js").read_text(encoding="utf-8")

    @router.get("/graph_task.js", response_class=PlainTextResponse)
    def task_script() -> str:
        """The script that shows a click's route and runs it."""
        return (STATIC / "graph_task.js").read_text(encoding="utf-8")

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

    return router
