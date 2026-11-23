"""Local model stand-in and real loopback processes used by the standalone HTTP checks."""

import json
import os
import re
import subprocess
import sys
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from queue import Queue
from threading import Thread

import httpx

PROFILE = Path("app_profiles/replica.profile.json")
SERVICES = Path("mock_services/service-bindings.example.json")


def provider_command(folder: Path) -> list[str]:
    return [
        sys.executable,
        "-c",
        "import sys, uvicorn; from pathlib import Path; from mock_services.app import create_app; "
        "uvicorn.run(create_app(Path(sys.argv[1])), host='127.0.0.1', port=0)",
        str(folder),
    ]


def bindings_at(base_url: httpx.URL) -> dict:
    bindings = json.loads(SERVICES.read_text(encoding="utf-8"))
    for binding in bindings.values():
        binding["endpoint"] = str(base_url.join(httpx.URL(binding["endpoint"]).path))
    return bindings


def assistant_command(graph: Path, services: Path) -> list[str]:
    return [
        sys.executable,
        "-m",
        "assistant",
        "--graph",
        str(graph),
        "--services",
        str(services),
        "--port",
        "0",
    ]


class DemoModels(BaseHTTPRequestHandler):
    """Deterministic answers for the synthetic card and internal transfer journeys."""

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
                text = body["messages"][0]["content"].rsplit("User message:", 1)[-1]
                intent = "internal_transfer" if "انتقال داخلی" in text else "card_transfer"
                answer = {"choice": intent}
            else:
                values = {
                    "card-transfer-destination": "رضا",
                    "card-transfer-amount": {"value": 250000, "unit": "IRT"},
                    "account-transfer-source": "سپرده پس‌انداز",
                    "account-transfer-destination": "رضا",
                    "account-transfer-amount": {"value": 300000, "unit": "IRT"},
                    "account-transfer-reason": "پرداخت اجاره",
                    "account-transfer-payment-id": "12345",
                }
                answer = {ref: values.get(ref) for ref in fields}
            self.send_json({"choices": [{"message": {"content": json.dumps(answer)}}]})
        else:
            self.send_error(404)

    def log_message(self, *_: object) -> None:
        pass


@contextmanager
def demo_models():
    with ThreadingHTTPServer(("127.0.0.1", 0), DemoModels) as models:
        thread = Thread(target=models.serve_forever, daemon=True)
        thread.start()
        url = f"http://127.0.0.1:{models.server_port}"
        try:
            yield {
                "ASSISTANT_AGENT": "workflow",
                "ASSISTANT_LLM_URL": url,
                "ASSISTANT_LLM_MODEL": "synthetic-test-model",
                "ASSISTANT_EMBED_URL": url,
                "ASSISTANT_TRACE": "0",
            }
        finally:
            models.shutdown()
            thread.join(timeout=2)


@contextmanager
def started_server(args: list[str], env: dict[str, str] | None = None):
    """Starts Uvicorn on a free port, and keeps its output for the check."""
    with subprocess.Popen(
        args,
        env=os.environ | {"PYTHONUNBUFFERED": "1", "PYTHONIOENCODING": "utf-8"} | (env or {}),
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
    ) as process:
        startup = Queue()
        lines: list[str] = []

        def read_output() -> None:
            started = False
            for line in process.stdout:
                lines.append(line)
                if not started:
                    match = re.search(r"Uvicorn running on (http://127\.0\.0\.1:\d+)", line)
                    if match:
                        startup.put(match[1])
                        started = True
            if not started:
                startup.put(None)

        reader = Thread(target=read_output, daemon=True)
        reader.start()
        try:
            url = startup.get(timeout=15)
            assert url, f"Server startup failed:\n{''.join(lines)}"
            with httpx.Client(base_url=url, timeout=5, trust_env=False) as client:
                yield client, lines
        finally:
            if process.poll() is None:
                process.terminate()
            process.wait(timeout=10)
            reader.join(timeout=2)
