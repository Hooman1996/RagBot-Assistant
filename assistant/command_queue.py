"""One outstanding command per session, following contracts/transitions/command.json.

Called by mobile_api when the app reports in, and by test_harness_api when a command is queued.
It holds state in memory: this is a development loop, not the task store of later stages.
It builds only the operations of OPERATIONS. It must never build one that submits or confirms.
"""

import os
import uuid
from collections.abc import Callable
from dataclasses import dataclass, field
from datetime import UTC, datetime, timedelta
from threading import RLock

from assistant.steps import show

PROTOCOL_VERSION = 1
PROFILE_VERSION = 1
DEBUGGING = os.environ.get("ASSISTANT_DEBUG") == "1"
COMMAND_LIFETIME = timedelta(hours=1) if DEBUGGING else timedelta(seconds=30)
FINISHED_KEPT = 50
OPERATIONS = ("observe", "navigate", "fill_form", "show_review")
NEEDS_DRAFT_VERSION = ("fill_form", "show_review")


class DeviceNotSeen(Exception):
    """The device has not reported in, so no observation version is known."""


class DeviceBusy(Exception):
    """The device already has a different outstanding command."""


class UnknownCommand(Exception):
    """A result arrived for a command this queue never issued."""


class StaleResult(Exception):
    """A result cannot finish the outstanding command."""


class DeviceMismatch(Exception):
    """The session is already bound to another device."""


class CommandCancelled(Exception):
    """The turn stopped before the command was queued."""


@dataclass
class Device:
    """What the backend knows about a session's device."""

    device_id: str | None = None
    observation: dict | None = None
    outstanding: dict | None = None
    delivered: bool = False
    last_result: dict | None = None
    finished: list[str] = field(default_factory=list)


def utc_now() -> datetime:
    """The current time. Tests pass their own clock instead."""
    return datetime.now(UTC)


def format_time(moment: datetime) -> str:
    """Writes a time the way the contract's expires_at pattern requires."""
    return moment.strftime("%Y-%m-%dT%H:%M:%SZ")


class CommandQueue:
    """The command loop's state, partitioned by session."""

    def __init__(self, now: Callable[[], datetime] = utc_now) -> None:
        self.now = now
        self.devices: dict[str, Device] = {}
        self.lock = RLock()

    def device(self, session_id: str) -> Device:
        """Returns the session's device record, creating it the first time."""
        with self.lock:
            return self.devices.setdefault(session_id, Device())

    def bind_device(self, session_id: str, device_id: str) -> None:
        """Binds the device named on the first accepted message."""
        with self.lock:
            device = self.device(session_id)
            if device.device_id not in (None, device_id):
                raise DeviceMismatch
            device.device_id = device_id

    def queue_command(
        self, session_id: str, operation: str, arguments: dict, *, stopped: Callable[[], bool] = bool
    ) -> dict:
        """Queues a command. The same request while it waits returns the same command."""
        if operation not in OPERATIONS:
            raise ValueError(operation)
        with self.lock:
            if stopped():
                raise CommandCancelled
            return self._queue_command(session_id, operation, arguments)

    def _queue_command(self, session_id: str, operation: str, arguments: dict) -> dict:
        device = self.device(session_id)
        if device.observation is None:
            raise DeviceNotSeen(session_id)
        self.drop_if_expired(device)
        waiting = device.outstanding
        if waiting is not None:
            if waiting["operation"] == operation and waiting["arguments"] == arguments:
                return waiting
            raise DeviceBusy(session_id)
        command = {
            "protocol_version": PROTOCOL_VERSION,
            "profile_version": PROFILE_VERSION,
            "task_id": f"graph-{uuid.uuid4().hex}",
            "task_revision": 1,
            "command_id": uuid.uuid4().hex,
            "device_id": device.device_id or session_id,
            "session_id": session_id,
            "operation": operation,
            "arguments": arguments,
            "expected_observation_version": device.observation["observation_version"],
            "expires_at": format_time(self.now() + COMMAND_LIFETIME),
        }
        if operation in NEEDS_DRAFT_VERSION:
            draft = device.observation["draft"]
            command["expected_draft_version"] = draft["draft_version"] if draft else 0
        show(
            "command queued",
            command_id=command["command_id"][:8],
            operation=operation,
            arguments=arguments,
            expected_observation_version=command["expected_observation_version"],
            expires_at=command["expires_at"],
        )
        device.outstanding = command
        device.delivered = False
        return command

    def next_command(self, session_id: str, observation: dict) -> dict | None:
        """Records the app's observation and returns its next command, if one may still run."""
        with self.lock:
            return self._next_command(self.device(session_id), observation)

    def _next_command(self, device: Device, observation: dict) -> dict | None:
        seen_version = device.observation["observation_version"] if device.observation else -1
        if device.delivered and observation["observation_version"] < seen_version:
            return None
        device.observation = observation
        self.drop_if_expired(device)
        command = device.outstanding
        if command is None or device.delivered:
            return None
        if command["expected_observation_version"] != observation["observation_version"]:
            self.close_unsent(device, "rejected_stale", "stale_observation")
            return None
        device.delivered = True
        show(
            "command handed to the app",
            command_id=command["command_id"][:8],
            observation_version=observation["observation_version"],
        )
        return command

    def record_result(self, result: dict, *, session_id: str | None = None) -> None:
        """Records a delivered result. HTTP always supplies its session; the stand-in runs in-process."""
        with self.lock:
            devices = self.devices.values() if session_id is None else (self.devices.get(session_id),)
            self._record_result(result, devices)

    def _record_result(self, result: dict, devices) -> None:
        command_id = result["command_id"]
        for device in devices:
            if device is None:
                continue
            if command_id in device.finished:
                return
            waiting = device.outstanding
            if waiting is not None and waiting["command_id"] == command_id:
                observation = result["observation"]
                if (
                    not device.delivered
                    or format_time(self.now()) >= waiting["expires_at"]
                    or observation["observation_version"] < device.observation["observation_version"]
                ):
                    raise StaleResult(command_id)
                device.observation = observation
                self.finish(device, result)
                device.finished = (device.finished + [command_id])[-FINISHED_KEPT:]
                return
        raise UnknownCommand(command_id)

    def cancel_unsent(self, session_id: str) -> None:
        """Closes work that has yet to leave the queue. A delivered command still needs its result."""
        with self.lock:
            device = self.devices.get(session_id)
            if device is not None and device.outstanding is not None and not device.delivered:
                self.close_unsent(device, "cancelled", "cancelled")

    def drop_if_expired(self, device: Device) -> None:
        """Closes a command past its expiry, so it can never run late."""
        command = device.outstanding
        if command is None or format_time(self.now()) < command["expires_at"]:
            return
        # A delivered command may have run, and only the app can say what happened.
        if device.delivered:
            self.close_unsent(device, "uncertain", "no_result_before_expiry")
        else:
            self.close_unsent(device, "rejected_expired", "expired")

    def close_unsent(self, device: Device, outcome: str, error_code: str) -> None:
        """Closes the outstanding command with a result written here, not by the app."""
        command = device.outstanding
        show("command closed here", outcome=outcome, error_code=error_code)
        result = {
            "command_id": command["command_id"],
            "outcome": outcome,
            "postcondition": None,
            "observation": device.observation,
            "draft_version": 0,
            "error_code": error_code,
        }
        self.finish(device, result)

    def finish(self, device: Device, result: dict) -> None:
        """Ends the outstanding command and wakes the bridge waiting for its result."""
        device.last_result = result
        device.outstanding = None
        device.delivered = False
