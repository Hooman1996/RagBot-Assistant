"""Exercise the real standalone process over HTTP with local model and bridge stand-ins."""

import json
import os
import re
import subprocess
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from queue import Queue
from threading import Thread
from uuid import uuid4

import httpx
import pytest
from jsonschema import Draft202012Validator
from samples import observation

from assistant.app import create_standalone_app
from assistant.command_queue import CommandQueue
from assistant.graph_data import read_json
from tools.stand_in_app import StandInApp

API = "/assistant/v1"
HEADER = "X-Assistant-Session"
CONVERSATION = "synthetic-panel"
DEVICE = "synthetic-bridge"
CARD = "۲۵۰ هزار تومان به رضا کارت به کارت کن"
EXPECTED = {
    ("POST", f"{API}/messages"),
    ("GET", f"{API}/history"),
    ("POST", f"{API}/sessions/{{session_id}}/cancel"),
    ("POST", f"{API}/sessions/{{session_id}}/resume"),
    ("POST", f"{API}/results"),
}


class DemoModels(BaseHTTPRequestHandler):
    """The two HTTP model protocols with deterministic answers for the card journey."""

    def send_json(self, body: object) -> None:
        data = json.dumps(body).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self) -> None:
        if self.path == "/info":
            self.send_json({"max_client_batch_size": 128})
        else:
            self.send_error(404)

    def do_POST(self) -> None:
        body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        if self.path == "/embed":
            self.send_json([[1.0] for _ in body["inputs"]])
        elif self.path == "/chat/completions":
            fields = body["response_format"]["json_schema"]["schema"]["properties"]
            if "kind" in fields:
                answer = {"kind": "new_task", "standalone": None, "rest": None}
            elif "choice" in fields:
                answer = {"choice": "card_transfer"}
            else:
                values = {
                    "card-transfer-destination": "رضا",
                    "card-transfer-amount": {"value": 250000, "unit": "IRT"},
                }
                answer = {ref: values.get(ref) for ref in fields}
            self.send_json({"choices": [{"message": {"content": json.dumps(answer)}}]})
        else:
            self.send_error(404)

    def log_message(self, *_: object) -> None:
        pass


@pytest.fixture(scope="module")
def api():
    with ThreadingHTTPServer(("127.0.0.1", 0), DemoModels) as models:
        model_thread = Thread(target=models.serve_forever, daemon=True)
        model_thread.start()
        model_url = f"http://127.0.0.1:{models.server_port}"
        env = os.environ | {
            "ASSISTANT_AGENT": "workflow",
            "ASSISTANT_LLM_URL": model_url,
            "ASSISTANT_LLM_MODEL": "synthetic-test-model",
            "ASSISTANT_EMBED_URL": model_url,
            "ASSISTANT_TRACE": "0",
        }
        args = [
            sys.executable,
            "-m",
            "uvicorn",
            "assistant.app:create_standalone_app",
            "--factory",
            "--host",
            "127.0.0.1",
            "--port",
            "0",
            "--no-access-log",
            "--timeout-graceful-shutdown",
            "5",
        ]
        try:
            with subprocess.Popen(
                args, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True
            ) as process:
                output = Queue()
                reader = Thread(target=lambda: [output.put(line) for line in process.stdout], daemon=True)
                reader.start()
                try:
                    while line := output.get(timeout=15):
                        started = re.search(r"Uvicorn running on (http://127\.0\.0\.1:\d+)", line)
                        if started:
                            break
                        assert "ERROR:" not in line, "Standalone startup failed"
                    with httpx.Client(base_url=started[1], timeout=5, trust_env=False) as client:
                        yield client
                finally:
                    process.terminate()
                    process.wait(timeout=10)
                    reader.join(timeout=2)
        finally:
            models.shutdown()
            model_thread.join(timeout=2)


def contract_response(spec: dict, response: httpx.Response, path: str) -> dict:
    """Verifies the documented response against the JSON sent over the wire."""
    method = response.request.method.lower()
    entry = spec["paths"][path][method]["responses"][str(response.status_code)]
    if "$ref" in entry:
        entry = spec["components"]["responses"][entry["$ref"].rsplit("/", 1)[-1]]
    schema = entry["content"]["application/json"]["schema"]
    body = response.json()
    assert Draft202012Validator(schema | {"components": spec["components"]}).is_valid(body)
    return body


def test_standalone_advertises_only_the_supported_routes(api: httpx.Client) -> None:
    spec = api.get("/openapi.json").raise_for_status().json()
    assert {
        (method.upper(), path)
        for path, item in spec["paths"].items()
        for method in item
        if method in {"get", "post"}
    } == EXPECTED
    assert {
        (method, route.path)
        for route in create_standalone_app().routes
        for method in route.methods
        if route.path != "/openapi.json"
    } == EXPECTED
    for method, path in EXPECTED:
        reply = api.request(
            method, path.replace("{session_id}", "absent"), json={} if method == "POST" else None
        )
        assert reply.status_code == 422
        contract_response(spec, reply, path)
    for operation in spec["x-planned-operations"]:
        path = re.sub(r"\{[^}]+\}", "example", operation["path"])
        assert api.request(operation["method"], path).status_code == 404
    assert api.get("/graph").status_code == 404
    assert api.get("/graph/state").status_code == 404
    assert api.post("/test-harness/v1/commands", json={}).status_code == 404
    print("PASS five standalone routes matched OpenAPI and development and planned routes returned 404")


def test_http_turns_and_bridge_results_stay_in_their_session(api: httpx.Client) -> None:
    spec = api.get("/openapi.json").raise_for_status().json()
    owner, other = uuid4().hex, uuid4().hex
    profile = read_json("replica.profile.json")
    bridges = {session: StandInApp(profile, CommandQueue(), DEVICE) for session in (owner, other)}
    idle = {"kind": "status", "task_state": "paused"}

    def request(method: str, path: str, session: str, status: int = 200, **kwargs) -> dict:
        path_template = path.replace(owner, "{session_id}").replace(other, "{session_id}")
        reply = api.request(method, API + path, headers={HEADER: session}, **kwargs)
        assert reply.status_code == status, (method, path_template, status, reply.status_code)
        return contract_response(spec, reply, API + path_template)

    def history(session: str = owner) -> dict:
        return request("GET", "/history", session, params={"conversation_id": CONVERSATION})

    def resume(session: str = owner, *, seen: dict | None = None) -> dict:
        return request(
            "POST", f"/sessions/{session}/resume", session, json=seen or bridges[session].observation
        )

    def message(session: str, **said) -> dict:
        return {
            "request_id": uuid4().hex,
            "session_id": session,
            "conversation_id": CONVERSATION,
            "device_id": DEVICE,
            "user_context": {"user_ref": "synthetic-user", "locale": "fa-IR"},
            "observation": bridges[session].observation,
        } | said

    def drive_turn(session: str = owner, *, wait_for_command: bool = False) -> dict:
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            next_item = resume(session)
            if next_item["kind"] == "command":
                command = next_item["command"]
                assert (command["session_id"], command["device_id"]) == (session, DEVICE)
                if wait_for_command:
                    return command
                request("POST", "/results", session, json=bridges[session].run(command))
            state = history(session)
            if not state["busy"] and not wait_for_command:
                return state
            time.sleep(0.02)
        pytest.fail("The standalone turn did not reach its next command or finish")

    request("GET", "/history", owner, 422)
    request("POST", "/messages", owner, 422, json={})
    request("POST", "/results", owner, 422, json={})
    request("POST", f"/sessions/{owner}/resume", owner, 422, json={"screen": "S-026"})
    assert resume() == idle
    request("POST", "/messages", owner, json=message(owner, message=CARD))

    assert history(other)["current"] is None
    assert history(other)["messages"] == []
    assert resume(other, seen=observation(99, "S-211")) == idle
    assert request("POST", f"/sessions/{owner}/resume", other, 403, json=observation(99)) == {
        "detail": "session_mismatch"
    }
    request("POST", f"/sessions/{owner}/cancel", other, 403)
    request("POST", f"/sessions/{other}/cancel", other, 409)
    request("POST", "/messages", other, 403, json=message(owner, message=CARD))

    command = drive_turn(wait_for_command=True)
    result = bridges[owner].run(command)
    before = history()
    assert before["busy"]
    assert resume(other, seen=observation(100, "S-211")) == idle
    assert request("POST", "/messages", other, 409, json=message(other, message=CARD)) == {
        "detail": "turn_in_progress"
    }
    assert request("POST", "/results", other, 409, json=result) == {"detail": "unknown_command"}
    assert request("POST", "/results", owner, 409, json=result | {"observation": observation(0)}) == {
        "detail": "stale_result"
    }
    assert history() == before
    assert request("POST", "/results", owner, json=result) == idle
    assert request("POST", "/results", owner, json=result) == idle
    assert request("POST", "/results", other, 409, json=result) == {"detail": "unknown_command"}

    pending = drive_turn()
    question = pending["current"]
    assert question["kind"] == "clarification"
    assert question["fields"] == ["card-transfer-source"]
    option = {"question_id": question["question_id"], "ref": "card-transfer-source=card-1"}
    stale = option | {"question_id": "old"}
    assert request("POST", "/messages", owner, 409, json=message(owner, selection=stale)) == {
        "detail": "stale_question"
    }
    assert request("POST", "/messages", other, 409, json=message(other, selection=option)) == {
        "detail": "stale_question"
    }
    wrong_device = message(owner, selection=option) | {"device_id": "different-bridge"}
    assert request("POST", "/messages", owner, 409, json=wrong_device) == {"detail": "device_mismatch"}
    assert history() == pending

    request("POST", "/messages", owner, json=message(owner, selection=option))
    review = drive_turn()
    assert review["current"]["task_state"] == "ready_for_user"
    assert bridges[owner].screen == "S-176"
    assert review["steps"][-1] == {"operation": "show_review", "screen": "S-176", "outcome": "succeeded"}
    assert "2,500,000" in review["messages"][-1]["text"]
    assert "****3566" in review["messages"][-1]["text"]
    assert "****8880" in review["messages"][-1]["text"]

    assert resume(other) == idle
    request("POST", "/messages", other, json=message(other, message=CARD))
    other_pending = drive_turn(other)
    assert other_pending["current"]["kind"] == "clarification"
    assert other_pending["current"]["question_id"] != question["question_id"]
    assert request("POST", "/messages", other, 409, json=message(other, selection=option)) == {
        "detail": "stale_question"
    }
    assert history(other) == other_pending
    assert history() == review
    request("POST", f"/sessions/{owner}/cancel", owner)
    cancelled = drive_turn()
    assert cancelled["current"]["task_state"] == "cancelled"
    assert bridges[owner].screen == "S-176"
    assert history(other) == other_pending
    request("POST", f"/sessions/{other}/cancel", other)
    assert drive_turn(other)["current"]["task_state"] == "cancelled"
    assert history() == cancelled

    print("PASS messages, history, resume, results and cancel on the standalone process")
    print("PASS invalid and stale inputs, session isolation, and review S-176 at 2,500,000 IRR")
