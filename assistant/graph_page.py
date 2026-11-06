"""Serves the interactive graph: the page, its data and the replica's live state.

Called by the browser. Clicking a node goes through test_harness_api, so this file only reads.
It offers no way to reach a confirm, submit or pay action.
"""

from pathlib import Path

from fastapi import APIRouter
from fastapi.responses import HTMLResponse

from assistant.command_queue import CommandQueue

PAGE = Path(__file__).resolve().parent / "static" / "graph.html"


def build_graph_router(queue: CommandQueue, payload: dict, device_id: str, revision: str) -> APIRouter:
    """Builds the page's routes around one queue and the graph data."""
    router = APIRouter(prefix="/graph")

    @router.get("", response_class=HTMLResponse)
    def page() -> str:
        """The page itself. It loads nothing from the internet."""
        return PAGE.read_text(encoding="utf-8")

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

    return router
