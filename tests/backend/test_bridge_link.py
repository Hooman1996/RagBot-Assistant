"""Sending one command and waiting for its result."""

from datetime import UTC, datetime

import pytest
from samples import observation

from assistant.bridge_link import STALE_TRIES, BridgeLink
from assistant.command_queue import COMMAND_LIFETIME, CommandQueue, Device, DeviceBusy, DeviceNotSeen
from assistant.task_runner import TaskStopped, Uncertain, Unreachable


class AnsweringQueue:
    """Answers each queued command at once, with the error codes it is given."""

    def __init__(self, codes: list[str | None] | None = None, refuse: Exception | None = None) -> None:
        self.codes = codes or [None]
        self.refuse = refuse
        self.record = Device()
        self.sent = 0

    def result_for(self, _: str, command_id: str) -> dict | None:
        result = self.record.last_result
        return result if result and result["command_id"] == command_id else None

    def queue_command(self, device_id: str, operation: str, arguments: dict, *, stopped=bool) -> dict:
        if self.refuse:
            raise self.refuse
        self.sent += 1
        code = self.codes[min(self.sent, len(self.codes)) - 1]
        self.record.last_result = {"command_id": str(self.sent), "outcome": "x", "error_code": code}
        return {"command_id": str(self.sent)}


def test_a_result_comes_back() -> None:
    queue = AnsweringQueue()
    assert BridgeLink(queue, "d").send("observe", {})["outcome"] == "x"
    assert queue.sent == 1


def test_a_stale_observation_is_sent_again() -> None:
    queue = AnsweringQueue(["stale_observation", None])
    assert BridgeLink(queue, "d").send("observe", {})["error_code"] is None
    assert queue.sent == 2


def test_it_gives_up_on_a_screen_that_keeps_moving() -> None:
    queue = AnsweringQueue(["stale_observation"])
    assert BridgeLink(queue, "d").send("observe", {})["error_code"] == "stale_observation"
    assert queue.sent == STALE_TRIES


def test_a_stale_draft_is_not_sent_again() -> None:
    queue = AnsweringQueue(["stale_draft"])
    assert BridgeLink(queue, "d").send("observe", {})["error_code"] == "stale_draft"
    assert queue.sent == 1


@pytest.mark.parametrize(
    ("refusal", "stop"), [(DeviceNotSeen("d"), Unreachable), (DeviceBusy("d"), TaskStopped)]
)
def test_a_device_that_cannot_take_it_stops_the_task(refusal: Exception, stop: type) -> None:
    # A device never seen is out of reach, so the task pauses rather than fails.
    with pytest.raises(stop):
        BridgeLink(AnsweringQueue(refuse=refusal), "d").send("observe", {})


def test_no_result_makes_the_command_uncertain() -> None:
    queue = AnsweringQueue()
    queue.record.last_result = None
    queue.queue_command = lambda *_, **__: {"command_id": "lost"}
    with pytest.raises(Uncertain):
        BridgeLink(queue, "d", timeout=0.01).send("observe", {})


@pytest.mark.parametrize("delivered", [False, True])
def test_expired_command_stops_until_the_app_reports_again(delivered: bool) -> None:
    now = datetime(2026, 1, 1, tzinfo=UTC)
    queue = CommandQueue(now=lambda: now)
    queue.next_command("d", observation(1))
    original = queue.queue_command("d", "navigate", {"destination": "S-174"})
    if delivered:
        assert queue.next_command("d", observation(1)) == original

    def expire_after_queueing(*_, **__):
        nonlocal now
        now += COMMAND_LIFETIME
        return original

    bridge = BridgeLink(queue, "d")
    bridge.queued = expire_after_queueing
    with pytest.raises(TaskStopped) as error:
        bridge.send("navigate", {"destination": "S-174"})
    assert type(error.value) is (TaskStopped if delivered else Unreachable)
    with pytest.raises(DeviceNotSeen):
        queue.queue_command("d", "navigate", {"destination": "S-174"})
    assert queue.next_command("d", observation(2, "S-193")) is None
    fresh = queue.queue_command("d", "navigate", {"destination": "S-174"})
    assert fresh["command_id"] != original["command_id"]
    assert fresh["expected_observation_version"] == 2
