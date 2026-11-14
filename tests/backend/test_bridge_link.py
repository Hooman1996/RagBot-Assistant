"""Sending one command and waiting for its result."""

import pytest

from assistant.bridge_link import STALE_TRIES, BridgeLink
from assistant.command_queue import Device, DeviceBusy, DeviceNotSeen
from assistant.task_runner import TaskStopped, Uncertain, Unreachable


class AnsweringQueue:
    """Answers each queued command at once, with the error codes it is given."""

    def __init__(self, codes: list[str | None] | None = None, refuse: Exception | None = None) -> None:
        self.codes = codes or [None]
        self.refuse = refuse
        self.record = Device()
        self.sent = 0

    def device(self, _: str) -> Device:
        return self.record

    def queue_command(self, device_id: str, operation: str, arguments: dict) -> dict:
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
    queue.queue_command = lambda *_: {"command_id": "lost"}
    with pytest.raises(Uncertain):
        BridgeLink(queue, "d", timeout=0.01).send("observe", {})
