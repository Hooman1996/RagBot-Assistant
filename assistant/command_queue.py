"""One outstanding command per device, following contracts/transitions/command.json.

Called by mobile_api when the app reports in, and by test_harness_api when a command is queued.
It holds state in memory: this is a development loop, not the task store of later stages.
It builds only the operations of OPERATIONS. It must never build one that submits or confirms.
"""

import os
import uuid
from collections.abc import Callable
from dataclasses import dataclass, field
from datetime import UTC, datetime, timedelta

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


@dataclass
class Device:
    """What the backend knows about one device."""

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
    """The command loop's state, for every device."""

    def __init__(self, now: Callable[[], datetime] = utc_now) -> None:
        self.now = now
        self.devices: dict[str, Device] = {}

    def device(self, device_id: str) -> Device:
        """Returns the device's record, creating it the first time."""
        return self.devices.setdefault(device_id, Device())

    def queue_command(self, device_id: str, operation: str, arguments: dict) -> dict:
        """Queues a command. The same request while it waits returns the same command."""
        if operation not in OPERATIONS:
            raise ValueError(operation)
        device = self.device(device_id)
        if device.observation is None:
            raise DeviceNotSeen(device_id)
        self.drop_if_expired(device)
        waiting = device.outstanding
        if waiting is not None:
            if waiting["operation"] == operation and waiting["arguments"] == arguments:
                return waiting
            raise DeviceBusy(device_id)
        command = {
            "protocol_version": PROTOCOL_VERSION,
            "profile_version": PROFILE_VERSION,
            "task_id": f"graph-{uuid.uuid4().hex}",
            "task_revision": 1,
            "command_id": uuid.uuid4().hex,
            "device_id": device_id,
            "session_id": device_id,
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

    def next_command(self, device_id: str, observation: dict) -> dict | None:
        """Records the app's observation and returns its next command, if one may still run."""
        device = self.device(device_id)
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

    def record_result(self, result: dict) -> None:
        """Records a result. A repeat of a finished command changes nothing."""
        command_id = result["command_id"]
        for device in self.devices.values():
            if command_id in device.finished:
                return
            waiting = device.outstanding
            if waiting is not None and waiting["command_id"] == command_id:
                device.observation = result["observation"]
                self.finish(device, result)
                return
        raise UnknownCommand(command_id)

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
        """Ends the outstanding command and keeps its ID, so a repeat is recognized."""
        device.last_result = result
        device.outstanding = None
        device.delivered = False
        device.finished = (device.finished + [result["command_id"]])[-FINISHED_KEPT:]
