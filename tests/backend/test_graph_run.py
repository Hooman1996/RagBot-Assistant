"""A whole run over the real routes, with a fake app that speaks the contract."""

import time
from threading import Event, Thread

import pytest
from fastapi.testclient import TestClient
from samples import observation

from assistant.app import DEVICE_ID, create_app
from assistant.command_queue import CommandQueue
from assistant.contract_check import problems
from assistant.graph_data import read_json
from assistant.graph_tasks import GraphTasks, RunInProgress
from assistant.task_runner import TaskReport

RESUME = f"/assistant/v1/sessions/{DEVICE_ID}/resume"
VALUES = {
    "card-transfer-source": "card-1",
    "card-transfer-destination": "رضا",
    "card-transfer-amount": "250000",
}


class ContractApp:
    """Polls like CommandPoller, and answers each command as the real bridge does."""

    def __init__(self, client: TestClient) -> None:
        self.client = client
        self.version = 1
        self.seen = observation(1, "S-026")
        self.commands: list[dict] = []
        self.stop = Event()

    def act(self, command: dict) -> None:
        """Changes the fake screen as the command would."""
        self.version += 1
        screen = self.seen["screen"]
        arguments = command["arguments"]
        fields = []
        if command["operation"] == "navigate":
            screen = arguments["destination"]
        if command["operation"] == "fill_form":
            for ref, value in arguments["values"].items():
                shown = str(value["value"]) if isinstance(value, dict) else value
                masked = f"****{shown[-4:]}" if shown.isdigit() and len(shown) >= 12 else shown
                fields.append({"ref": ref, "value": masked})
        if command["operation"] == "show_review":
            screen = "S-176"
        self.seen = {**observation(self.version, screen), "fields": fields}

    def loop(self) -> None:
        """Polls until told to stop, running whatever arrives."""
        while not self.stop.is_set():
            reply = self.client.post(RESUME, json=self.seen).json()
            if reply["kind"] != "command":
                time.sleep(0.01)
                continue
            command = reply["command"]
            assert problems("command.schema.json", command) == []
            self.commands.append(command)
            self.act(command)
            result = {
                "command_id": command["command_id"],
                "outcome": "succeeded",
                "postcondition": "done",
                "observation": self.seen,
                "draft_version": 0,
            }
            assert self.client.post("/assistant/v1/results", json=result).status_code == 200


@pytest.fixture
def app() -> ContractApp:
    fake = ContractApp(TestClient(create_app()))
    thread = Thread(target=fake.loop, daemon=True)
    thread.start()
    yield fake
    fake.stop.set()
    thread.join(timeout=2)


def wait_until_finished(client: TestClient) -> dict:
    deadline = time.monotonic() + 5
    while time.monotonic() < deadline:
        progress = client.get("/graph/run").json()
        if progress["state"] != "running":
            return progress
        time.sleep(0.01)
    raise AssertionError("the run did not end")


def seen_by_backend(client: TestClient) -> None:
    deadline = time.monotonic() + 5
    while client.get("/graph/state").json()["observation"] is None:
        assert time.monotonic() < deadline
        time.sleep(0.01)


def test_card_to_reza_runs_to_the_review_and_stops(app: ContractApp) -> None:
    seen_by_backend(app.client)
    assert app.client.post("/graph/run", json={"screen": "S-174", "values": VALUES}).status_code == 202
    progress = wait_until_finished(app.client)
    assert progress["state"] == "review"
    assert [item["operation"] for item in app.commands] == ["navigate", "fill_form", "show_review"]
    values = app.commands[1]["arguments"]["values"]
    assert values["card-transfer-destination"] == "0000000000000091"
    assert values["card-transfer-amount"] == {"value": 250000, "unit": "IRR"}
    assert app.commands[1]["expected_draft_version"] == 0


def test_a_second_run_while_one_goes_is_refused() -> None:
    tasks = GraphTasks(CommandQueue(), DEVICE_ID, read_json("replica.profile.json"))
    tasks.report = TaskReport()
    with pytest.raises(RunInProgress):
        tasks.start("S-174", VALUES)
