"""The queue's rules: one command per device, deduplication, expiry and stale commands."""

from datetime import UTC, datetime, timedelta

import pytest
from samples import observation, result

from assistant.command_queue import CommandQueue, DeviceBusy, DeviceNotSeen, UnknownCommand
from assistant.contract_check import problems

DEVICE = "replica-dev"


class Clock:
    """A clock the tests move by hand."""

    def __init__(self) -> None:
        self.moment = datetime(2026, 11, 6, 12, 0, tzinfo=UTC)

    def __call__(self) -> datetime:
        return self.moment


@pytest.fixture
def clock() -> Clock:
    return Clock()


@pytest.fixture
def queue(clock: Clock) -> CommandQueue:
    commands = CommandQueue(now=clock)
    commands.next_command(DEVICE, observation())
    return commands


def test_a_device_that_never_reported_cannot_be_given_a_command() -> None:
    with pytest.raises(DeviceNotSeen):
        CommandQueue().queue_command(DEVICE, "navigate", {"destination": "S-174"})


def test_a_queued_command_matches_the_contract(queue: CommandQueue) -> None:
    command = queue.queue_command(DEVICE, "navigate", {"destination": "S-174"})
    assert problems("command.schema.json", command) == []
    assert command["operation"] == "navigate"
    assert command["expected_observation_version"] == 3


def test_the_command_is_delivered_once(queue: CommandQueue) -> None:
    command = queue.queue_command(DEVICE, "navigate", {"destination": "S-174"})
    assert queue.next_command(DEVICE, observation()) == command
    assert queue.next_command(DEVICE, observation()) is None


def test_only_one_command_is_outstanding_per_device(queue: CommandQueue) -> None:
    queue.queue_command(DEVICE, "navigate", {"destination": "S-174"})
    with pytest.raises(DeviceBusy):
        queue.queue_command(DEVICE, "navigate", {"destination": "S-192"})


def test_the_same_request_while_waiting_returns_the_same_command(queue: CommandQueue) -> None:
    first = queue.queue_command(DEVICE, "navigate", {"destination": "S-174"})
    assert queue.queue_command(DEVICE, "navigate", {"destination": "S-174"}) is first


def test_a_result_frees_the_device_and_updates_the_observation(queue: CommandQueue) -> None:
    command = queue.queue_command(DEVICE, "navigate", {"destination": "S-174"})
    queue.next_command(DEVICE, observation())
    queue.record_result(result(command["command_id"]))
    device = queue.device(DEVICE)
    assert device.outstanding is None
    assert device.observation["screen"] == "S-174"
    assert device.last_result["outcome"] == "succeeded"
    queue.queue_command(DEVICE, "navigate", {"destination": "S-192"})


def test_a_repeated_result_changes_nothing(queue: CommandQueue) -> None:
    command = queue.queue_command(DEVICE, "navigate", {"destination": "S-174"})
    queue.next_command(DEVICE, observation())
    queue.record_result(result(command["command_id"]))
    queue.record_result(result(command["command_id"], "failed"))
    assert queue.device(DEVICE).last_result["outcome"] == "succeeded"


def test_a_result_for_an_unknown_command_is_refused(queue: CommandQueue) -> None:
    with pytest.raises(UnknownCommand):
        queue.record_result(result("never-issued"))


def test_an_expired_command_is_never_delivered(queue: CommandQueue, clock: Clock) -> None:
    queue.queue_command(DEVICE, "navigate", {"destination": "S-174"})
    clock.moment += timedelta(seconds=31)
    assert queue.next_command(DEVICE, observation()) is None
    assert queue.device(DEVICE).last_result["outcome"] == "rejected_expired"


def test_a_delivered_command_with_no_result_ends_uncertain(queue: CommandQueue, clock: Clock) -> None:
    queue.queue_command(DEVICE, "navigate", {"destination": "S-174"})
    queue.next_command(DEVICE, observation())
    clock.moment += timedelta(seconds=31)
    queue.next_command(DEVICE, observation())
    assert queue.device(DEVICE).last_result["outcome"] == "uncertain"


def test_a_stale_command_is_closed_instead_of_delivered(queue: CommandQueue) -> None:
    queue.queue_command(DEVICE, "navigate", {"destination": "S-174"})
    assert queue.next_command(DEVICE, observation(version=9)) is None
    assert queue.device(DEVICE).last_result["outcome"] == "rejected_stale"
    assert problems("result.schema.json", queue.device(DEVICE).last_result) == []


@pytest.mark.parametrize(
    ("operation", "arguments"),
    [
        ("navigate", {"destination": "S-284", "arguments": {"charity": "1"}}),
        (
            "fill_form",
            {"form": "S-174", "values": {"card-transfer-amount": {"value": 250000, "unit": "IRR"}}},
        ),
        ("show_review", {}),
        ("observe", {}),
    ],
)
def test_every_operation_builds_a_valid_command(operation: str, arguments: dict) -> None:
    queue = CommandQueue()
    queue.next_command("device-1", observation())
    command = queue.queue_command("device-1", operation, arguments)
    assert problems("command.schema.json", command) == []


@pytest.mark.parametrize("operation", ["submit", "confirm", "authorize", "pay", "select_candidate"])
def test_no_other_operation_can_be_built(operation: str) -> None:
    queue = CommandQueue()
    queue.next_command("device-1", observation())
    with pytest.raises(ValueError, match=operation):
        queue.queue_command("device-1", operation, {})
