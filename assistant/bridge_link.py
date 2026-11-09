"""Sends one command through the queue and waits for its result.

Called by task_runner through graph_tasks. The app pulls the command on its next poll, so this
only waits. It adds no decisions of its own, apart from sending again when the screen moved
between queueing and delivery, which the contract says did not run the command.
"""

import time

from assistant.command_queue import DEBUGGING, CommandQueue, DeviceBusy, DeviceNotSeen
from assistant.task_runner import TaskStopped

RESULT_TIMEOUT_SECONDS = 3600 if DEBUGGING else 40
POLL_SECONDS = 0.1
STALE_TRIES = 3


class BridgeLink:
    """One device's end of the command loop."""

    def __init__(self, queue: CommandQueue, device_id: str, timeout: float = RESULT_TIMEOUT_SECONDS) -> None:
        self.queue = queue
        self.device_id = device_id
        self.timeout = timeout

    def send(self, operation: str, arguments: dict) -> dict:
        """Sends a command and returns its result, repeating only a stale observation."""
        for _ in range(STALE_TRIES):
            result = self.wait_for(self.queued(operation, arguments)["command_id"])
            if result.get("error_code") != "stale_observation":
                return result
        return result

    def queued(self, operation: str, arguments: dict) -> dict:
        """Queues the command, or stops the task when the device cannot take it."""
        try:
            return self.queue.queue_command(self.device_id, operation, arguments)
        except DeviceNotSeen as error:
            raise TaskStopped("برنامه هنوز به سرور وصل نشده است") from error
        except DeviceBusy as error:
            raise TaskStopped("فرمان دیگری در حال اجراست") from error

    def wait_for(self, command_id: str) -> dict:
        """Waits until the app has reported this command."""
        deadline = time.monotonic() + self.timeout
        device = self.queue.device(self.device_id)
        while time.monotonic() < deadline:
            result = device.last_result
            if result is not None and result["command_id"] == command_id:
                return result
            time.sleep(POLL_SECONDS)
        raise TaskStopped("پاسخی از برنامه نرسید")
