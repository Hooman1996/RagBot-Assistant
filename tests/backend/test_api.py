"""The app's routes, the harness endpoint and what the graph page serves."""

import json
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from samples import observation, result

from assistant.app import DEVICE_ID, create_app
from assistant.graph_data import graph_payload, read_json

RESUME = f"/assistant/v1/sessions/{DEVICE_ID}/resume"
QUEUE = "/test-harness/v1/commands"


@pytest.fixture
def client() -> TestClient:
    return TestClient(create_app())


def report_in(client: TestClient) -> dict:
    reply = client.post(RESUME, json=observation())
    assert reply.status_code == 200
    return reply.json()


def test_with_nothing_queued_the_app_hears_a_status(client: TestClient) -> None:
    assert report_in(client) == {"kind": "status", "task_state": "paused"}


def test_a_click_reaches_the_app_and_its_result_reaches_the_page(client: TestClient) -> None:
    report_in(client)
    command = client.post(QUEUE, json={"device_id": DEVICE_ID, "destination": "S-174"}).json()
    assert report_in(client) == {"kind": "command", "command": command}
    assert client.post("/assistant/v1/results", json=result(command["command_id"])).status_code == 200
    state = client.get("/graph/state").json()
    assert state["observation"]["screen"] == "S-174"
    assert state["last_result"]["command_id"] == command["command_id"]
    assert state["waiting"] is False


def test_a_body_outside_the_contract_is_refused(client: TestClient) -> None:
    assert client.post(RESUME, json={"screen": "S-026"}).status_code == 422
    assert client.post("/assistant/v1/results", json={"command_id": "x"}).status_code == 422


def test_a_result_for_an_unknown_command_is_refused(client: TestClient) -> None:
    assert client.post("/assistant/v1/results", json=result("never-issued")).status_code == 409


def test_an_unbuilt_screen_is_refused(client: TestClient) -> None:
    report_in(client)
    reply = client.post(QUEUE, json={"device_id": DEVICE_ID, "destination": "S-003"})
    assert reply.status_code == 404
    assert reply.json()["detail"] == "screen_not_built"


def test_the_harness_takes_a_destination_and_nothing_else(client: TestClient) -> None:
    report_in(client)
    body = {"device_id": DEVICE_ID, "destination": "S-176", "operation": "submit"}
    assert client.post(QUEUE, json=body).status_code == 422
    assert client.post(QUEUE, json={"device_id": DEVICE_ID, "action": "confirm"}).status_code == 422


def test_a_device_that_never_reported_is_refused(client: TestClient) -> None:
    reply = client.post(QUEUE, json={"device_id": DEVICE_ID, "destination": "S-174"})
    assert reply.status_code == 409
    assert reply.json()["detail"] == "device_not_seen"


def test_a_second_destination_waits_its_turn(client: TestClient) -> None:
    report_in(client)
    client.post(QUEUE, json={"device_id": DEVICE_ID, "destination": "S-174"})
    reply = client.post(QUEUE, json={"device_id": DEVICE_ID, "destination": "S-192"})
    assert reply.status_code == 409
    assert reply.json()["detail"] == "device_busy"


def test_no_route_can_confirm_submit_or_pay(client: TestClient) -> None:
    paths = set(client.get("/openapi.json").json()["paths"])
    assert paths == {
        RESUME.replace(DEVICE_ID, "{session_id}"),
        "/assistant/v1/results",
        QUEUE,
        "/graph",
        "/graph/data",
        "/graph/state",
        "/graph/graph_map.js",
        "/graph/graph_task.js",
        "/graph/plan/{screen}",
        "/graph/run",
        "/graph/chat",
        "/graph/chat/cancel",
        "/graph/graph_chat.js",
    }


def test_the_harness_route_is_in_the_contract() -> None:
    contract = json.loads(Path("contracts/openapi/test_harness.json").read_text(encoding="utf-8"))
    assert "post" in contract["paths"][QUEUE]


def test_the_graph_page_needs_nothing_from_the_internet(client: TestClient) -> None:
    page = client.get("/graph")
    assert page.status_code == 200
    scripts = client.get("/graph/graph_map.js").text + client.get("/graph/graph_task.js").text
    assert "http://" not in (page.text + scripts).replace("http://www.w3.org/2000/svg", "")
    assert "https://" not in page.text + scripts
    assert "https://" not in page.text


def test_the_graph_has_every_screen_with_built_ones_marked() -> None:
    payload = graph_payload()
    graph = read_json("navigation-graph.json")
    profile = read_json("replica.profile.json")
    ids = {node["id"] for node in payload["nodes"]}
    assert ids == {node["id"] for node in graph["nodes"]} | set(profile["screens"])
    built = {node["id"] for node in payload["nodes"] if node["built"]}
    assert built == set(profile["screens"])
    known = {(edge["from"], edge["to"]) for edge in payload["edges"]}
    assert {(edge["from"], edge["to"]) for edge in profile["edges"]} <= known


def test_a_click_shows_the_route_questions_and_demo_values(client: TestClient) -> None:
    report_in(client)
    plan = client.get("/graph/plan/S-174").json()
    assert plan["status"] == "ok"
    assert plan["feature"] == "card_transfer"
    refs = [item["ref"] for item in plan["requirements"]]
    assert refs == ["card-transfer-source", "card-transfer-destination", "card-transfer-amount"]
    assert all(item["ask"] for item in plan["requirements"])
    assert plan["demo"]["card-transfer-destination"] == "رضا"


def test_a_screen_behind_the_users_step_has_no_plan(client: TestClient) -> None:
    assert client.get("/graph/plan/S-093").json() == {"status": "no_route"}


def test_a_run_refuses_anything_but_a_screen_and_values(client: TestClient) -> None:
    reply = client.post("/graph/run", json={"screen": "S-174", "values": {}, "operation": "confirm"})
    assert reply.status_code == 422


def test_a_run_to_a_screen_with_no_route_is_refused(client: TestClient) -> None:
    assert client.post("/graph/run", json={"screen": "S-093", "values": {}}).status_code == 404


def test_a_run_missing_a_value_stops_without_a_command(client: TestClient) -> None:
    report_in(client)
    assert client.post("/graph/run", json={"screen": "S-174", "values": {}}).status_code == 202
    progress = client.get("/graph/run").json()
    assert progress["steps"] == []
    assert client.post(RESUME, json=observation()).json() == {"kind": "status", "task_state": "paused"}
