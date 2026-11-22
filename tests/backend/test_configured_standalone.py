"""The configured standalone process changes plans and uses the selected real mock endpoint."""

import json
import subprocess
import sys
import time
from pathlib import Path
from tempfile import TemporaryDirectory
from uuid import uuid4

import httpx
import pytest
from standalone_fakes import PROFILE, SERVICES, assistant_command, demo_models, started_server

from assistant.agent_chat import LOOKUP_ERROR
from assistant.command_queue import CommandQueue
from assistant.graph_data import read_json
from assistant.lookups import FIXTURES
from tools.stand_in_app import StandInApp

INTERNAL = "۳۰۰ هزار تومان از سپرده پس‌انداز به حساب رضا انتقال داخلی بزن بابت اجاره، شناسه واریز 12345"


@pytest.fixture
def config_dir():
    scratch = Path("local/scratch")
    scratch.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(dir=scratch, prefix="demo-3-") as folder:
        yield Path(folder)


def write_json(path: Path, data: dict) -> Path:
    path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    return path


def provider_command(folder: Path) -> list[str]:
    return [
        sys.executable,
        "-c",
        "import sys, uvicorn; from pathlib import Path; from mock_services.app import create_app; "
        "uvicorn.run(create_app(Path(sys.argv[1])), host='127.0.0.1', port=0)",
        str(folder),
    ]


def with_endpoints(accounts: httpx.URL, saved_accounts: httpx.URL) -> dict:
    bindings = json.loads(SERVICES.read_text(encoding="utf-8"))
    bindings["accounts"]["endpoint"] = str(accounts)
    bindings["contacts.account"]["endpoint"] = str(saved_accounts)
    return bindings


def drive(api: httpx.Client, text: str) -> tuple[dict, list[dict]]:
    """Uses the unchanged bridge implementation and the published mobile operations."""
    session = uuid4().hex
    device = "synthetic-bridge"
    header = {"X-Assistant-Session": session}
    bridge = StandInApp(read_json(PROFILE.name), CommandQueue(), device)
    conversation = {"conversation_id": "synthetic-panel"}
    resume = f"/assistant/v1/sessions/{session}/resume"

    def poll() -> dict:
        return api.post(resume, json=bridge.observation, headers=header).raise_for_status().json()

    poll()
    api.post(
        "/assistant/v1/messages",
        headers=header,
        json=conversation
        | {
            "request_id": uuid4().hex,
            "session_id": session,
            "device_id": device,
            "user_context": {"user_ref": "synthetic-user", "locale": "fa-IR"},
            "observation": bridge.observation,
            "message": text,
        },
    ).raise_for_status()

    commands = []
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        reply = poll()
        if reply["kind"] == "command":
            command = reply["command"]
            commands.append(command)
            api.post("/assistant/v1/results", json=bridge.run(command), headers=header).raise_for_status()
        history = (
            api.get("/assistant/v1/history", params=conversation, headers=header).raise_for_status().json()
        )
        if not history["busy"]:
            return history, commands
        time.sleep(0.03)
    pytest.fail("The configured standalone turn did not finish")


def filled_at_review(history: dict, commands: list[dict]) -> dict:
    assert history["current"]["task_state"] == "ready_for_user", history["messages"]
    assert [command["operation"] for command in commands] == ["navigate", "fill_form", "show_review"]
    assert history["steps"][-1] == {"operation": "show_review", "screen": "S-173", "outcome": "succeeded"}
    values = commands[1]["arguments"]["values"]
    assert values["account-transfer-source"] == "account-2"
    assert values["account-transfer-amount"] == {"value": 3000000, "unit": "IRR"}
    return values


def test_restarting_with_a_new_graph_and_binding_changes_the_real_turn(config_dir: Path) -> None:
    # Test-only shortcut combines the same two safe taps. The client keeps the generated profile.
    variant = read_json(PROFILE.name)
    variant["edges"].append({"from": "S-026", "to": "S-192", "action": "menu-transfer > transfer-internal"})
    form = next(item for item in variant["forms"] if item["id"] == "S-192")
    next(item for item in form["fields"] if item["ref"] == "account-transfer-payment-id")["requires"] = True
    variant["features"] = [item for item in variant["features"] if item["intent"] != "card_transfer"]
    graph_b = write_json(config_dir / "graph-b.json", variant)

    fixtures_b = config_dir / "provider-b"
    fixtures_b.mkdir()
    contacts = json.loads((FIXTURES / "contacts.json").read_text(encoding="utf-8"))
    original = contacts["people"][0]["account"]
    contacts["people"][0]["account"] = original[:-4] + "5821"
    write_json(fixtures_b / "contacts.json", contacts)

    with (
        demo_models() as env,
        started_server(provider_command(FIXTURES)) as (provider_a, requests_a),
        started_server(provider_command(fixtures_b)) as (provider_b, requests_b),
    ):
        accounts_url = provider_a.base_url.join("/accounts")
        service_a = write_json(
            config_dir / "bindings-a.json",
            with_endpoints(accounts_url, provider_a.base_url.join("/saved-accounts")),
        )
        service_b = write_json(
            config_dir / "bindings-b.json",
            with_endpoints(accounts_url, provider_b.base_url.join("/saved-accounts")),
        )
        env |= {"ASSISTANT_TRACE": "1"}
        with started_server(assistant_command(PROFILE, service_a), env) as (api, trace_a):
            history_a, commands_a = drive(api, INTERNAL)
        calls_a = sum("GET /saved-accounts " in line for line in requests_a)
        assert calls_a > 0
        assert not any("GET /saved-accounts " in line for line in requests_b)
        with started_server(assistant_command(graph_b, service_b), env) as (api, trace_b):
            history_b, commands_b = drive(api, INTERNAL)
            disabled, commands = drive(api, "۲۵۰ هزار تومان به رضا کارت به کارت کن")
            assert disabled["current"]["kind"] == "clarification"
            assert not commands
        assert any("GET /accounts " in line for line in requests_a)
        assert sum("GET /saved-accounts " in line for line in requests_a) == calls_a
        assert any("GET /saved-accounts " in line for line in requests_b)

    values_a = filled_at_review(history_a, commands_a)
    values_b = filled_at_review(history_b, commands_b)
    assert values_a["account-transfer-destination"].endswith("7800")
    assert values_b["account-transfer-destination"].endswith("5821")
    assert "****7800" in history_a["messages"][-1]["text"]
    assert "****5821" in history_b["messages"][-1]["text"]
    assert "account-transfer-payment-id" not in values_a
    assert values_b["account-transfer-payment-id"] == "12345"
    assert "S-026 → S-193 (menu-transfer)" in "".join(trace_a)
    assert "S-193 → S-192 (transfer-internal)" in "".join(trace_a)
    assert "S-026 → S-192 (menu-transfer > transfer-internal)" in "".join(trace_b)
    print("PASS graph A planned S-026 → S-193 → S-192 and provider A supplied recipient ****7800")
    print("PASS graph B planned S-026 → S-192, required the payment ID, and provider B supplied ****5821")
    print("PASS both runs reached S-173 at 3,000,000 IRR with the same bridge implementation")


@pytest.mark.parametrize("path", ["/missing", "/accounts"])
def test_mock_failure_is_reported_without_fixture_fallback(config_dir: Path, path: str) -> None:
    with demo_models() as env, started_server(provider_command(FIXTURES)) as (provider, access_log):
        services = write_json(
            config_dir / "bindings.json",
            with_endpoints(provider.base_url.join("/accounts"), provider.base_url.join(path)),
        )
        with started_server(assistant_command(PROFILE, services), env) as (api, trace):
            history, commands = drive(api, INTERNAL)
        assert any(f"GET {path} " in line for line in access_log)
    assert history["current"]["task_state"] == "failed"
    assert history["messages"][-1]["text"] == LOOKUP_ERROR
    assert not commands
    assert any("Banking lookup unavailable: contacts.account" in line for line in trace)
    print(f"PASS {path} failure surfaced in history without a fixture fallback or bridge command")


@pytest.mark.parametrize(
    ("problem", "message"),
    [
        ("graph", "references an unknown screen"),
        ("operation", "invalid graph at $.edges"),
        ("user_only", "uses a user-only action"),
        ("endpoint", "expected a loopback HTTP endpoint"),
        ("adapter", "contacts.account requires the saved-accounts-http adapter"),
        ("missing", "missing binding for internal_transfer: contacts.account"),
    ],
)
def test_invalid_configuration_fails_before_listening(config_dir: Path, problem: str, message: str) -> None:
    graph = read_json(PROFILE.name)
    bindings = json.loads(SERVICES.read_text(encoding="utf-8"))
    if problem == "graph":
        graph["edges"][0]["to"] = "unknown-configured-screen"
    elif problem == "operation":
        graph["edges"][0]["operation"] = "unsupported"
    elif problem == "user_only":
        graph["user_only_actions"] = ["secret-*"]
        graph["edges"][0]["action"] = "account-transfer-submit"
    elif problem == "endpoint":
        bindings["contacts.account"]["endpoint"] = "http://example.invalid:8110/saved-accounts"
    elif problem == "adapter":
        bindings["contacts.account"]["adapter"] = "accounts-http"
    else:
        del bindings["contacts.account"]
    cmd = assistant_command(
        write_json(config_dir / "profile.json", graph), write_json(config_dir / "bindings.json", bindings)
    )
    stopped = subprocess.run(cmd, capture_output=True, text=True, timeout=15, check=False)
    assert stopped.returncode == 2
    assert message in stopped.stderr
    assert "Uvicorn running on" not in stopped.stderr
