"""Sends one command through the queue and waits for its result.

Called by task_runner through graph_tasks. The app pulls the command on its next poll, so this
only waits. It adds no decisions of its own, apart from sending again when the screen moved
between queueing and delivery, which the contract says did not run the command.
"""

import time
from collections.abc import Callable

from assistant.command_queue import DEBUGGING, CommandCancelled, CommandQueue, DeviceBusy, DeviceNotSeen
from assistant.task_runner import Cancelled, TaskStopped, Uncertain, Unreachable

RESULT_TIMEOUT_SECONDS = 3600 if DEBUGGING else 40
POLL_SECONDS = 0.1
STALE_TRIES = 3


class BridgeLink:
    """One session's end of the command loop."""

    def __init__(
        self,
        queue: CommandQueue,
        session_id: str,
        timeout: float = RESULT_TIMEOUT_SECONDS,
        *,
        stopped: Callable[[], bool] = bool,
    ) -> None:
        self.queue = queue
        self.session_id = session_id
        self.timeout = timeout
        self.stopped = stopped

    def send(self, operation: str, arguments: dict) -> dict:
        """Sends a command and returns its result, repeating only a stale observation."""
        for _ in range(STALE_TRIES):
            result = self.wait_for(self.queued(operation, arguments)["command_id"])
            if self.stopped() or result["outcome"] == "cancelled":
                raise Cancelled("کاربر لغو کرد")
            if result.get("error_code") != "stale_observation":
                return result
        return result

    def queued(self, operation: str, arguments: dict) -> dict:
        """Queues the command, or stops the task when the device cannot take it."""
        try:
            return self.queue.queue_command(self.session_id, operation, arguments, stopped=self.stopped)
        except CommandCancelled as error:
            raise Cancelled("کاربر لغو کرد") from error
        except DeviceNotSeen as error:
            raise Unreachable("برنامه هنوز به سرور وصل نشده است") from error
        except DeviceBusy as error:
            raise TaskStopped("فرمان دیگری در حال اجراست") from error

    def wait_for(self, command_id: str) -> dict:
        """Waits until the app has reported this command."""
        deadline = time.monotonic() + self.timeout
        device = self.queue.device(self.session_id)
        while time.monotonic() < deadline:
            result = device.last_result
            if result is not None and result["command_id"] == command_id:
                return result
            time.sleep(POLL_SECONDS)
        raise Uncertain("پاسخی از برنامه نرسید")
