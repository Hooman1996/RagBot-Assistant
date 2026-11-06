"""Development control: queue a navigate command for a device.

Called by the graph page, tools/bridge_e2e.py and tests. It follows
contracts/openapi/test_harness.json. It is never an assistant tool.
It accepts a destination and nothing else, so it cannot ask for a fill, a confirm or a payment.
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, ConfigDict

from assistant.command_queue import CommandQueue, DeviceBusy, DeviceNotSeen


class QueueRequest(BaseModel):
    """The only things a caller may say. Any other field is refused."""

    model_config = ConfigDict(extra="forbid")

    device_id: str
    destination: str


def build_harness_router(queue: CommandQueue, built_screens: set[str]) -> APIRouter:
    """Builds the harness route around one queue and the profile's screens."""
    router = APIRouter(prefix="/test-harness/v1")

    @router.post("/commands")
    def queue_command(request: QueueRequest) -> dict:
        """Queues a navigate command to a built screen."""
        if request.destination not in built_screens:
            raise HTTPException(status_code=404, detail="screen_not_built")
        try:
            return queue.queue_navigate(request.device_id, request.destination)
        except DeviceNotSeen as error:
            raise HTTPException(status_code=409, detail="device_not_seen") from error
        except DeviceBusy as error:
            raise HTTPException(status_code=409, detail="device_busy") from error

    return router
