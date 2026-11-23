"""End-to-end checks on the emulator, over the same API the graph page uses.

Run by hand, with the backend running and the app's loop enabled. With no arguments it runs the
acceptance journey: card to card to رضا for 250000 IRR, reaching the review and staying there.
With --features it runs every feature with its demo values and prints how each ended.
With --standalone it checks the configured internal transfer through the mobile API.
"""

import argparse
import json
import re
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import quote
from uuid import uuid4

BACKEND = "http://127.0.0.1:8100"
STANDALONE = "http://127.0.0.1:8101"
DEVICE_ID = "replica-dev"
TIMEOUT_SECONDS = 180
PROFILE = "app_profiles/replica.profile.json"
ADB = ["adb", "-s", "emulator-5554", "shell"]
DIGITS = str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")
TRANSFER = "۳۰۰ هزار تومن از سپرده پس‌انداز به حساب رضا انتقال داخلی بزن بابت اجاره"


def get(path: str, *, backend: str = BACKEND, session: str = "") -> dict:
    """Reads one JSON route."""
    request = urllib.request.Request(
        f"{backend}{path}", headers={"X-Assistant-Session": session} if session else {}
    )
    with urllib.request.urlopen(request, timeout=5) as reply:
        return json.load(reply)


def post(path: str, body: dict, *, backend: str = BACKEND, session: str = "") -> dict:
    """Posts JSON and reads the reply."""
    data = json.dumps(body).encode()
    headers = {"Content-Type": "application/json"}
    if session:
        headers["X-Assistant-Session"] = session
    request = urllib.request.Request(f"{backend}{path}", data, headers)
    with urllib.request.urlopen(request, timeout=5) as reply:
        return json.load(reply)


def run(screen: str, values: dict[str, str]) -> dict:
    """Starts a run, as the page's Run button does, and waits for it to end."""
    post("/graph/run", {"screen": screen, "values": values})
    deadline = time.time() + TIMEOUT_SECONDS
    while time.time() < deadline:
        progress = get("/graph/run")
        if progress["state"] != "running":
            return progress
        time.sleep(0.3)
    raise SystemExit(f"FAIL  {screen}: the run did not end")


def restart_app() -> None:
    """Starts the app afresh, so each feature begins from the front page."""
    adb = ["adb", "-s", "emulator-5554", "shell"]
    subprocess.run([*adb, "am", "force-stop", "org.ragbot.replica"], check=True)
    subprocess.run(
        [*adb, "am", "start", "-n", "org.ragbot.replica/.MainActivity"], check=True, capture_output=True
    )
    time.sleep(4)


def go_home() -> None:
    """Moves to Home, so each run starts from the same place."""
    post("/test-harness/v1/commands", {"device_id": DEVICE_ID, "destination": "S-026"})
    deadline = time.time() + 40
    while time.time() < deadline:
        state = get("/graph/state")
        if not state["waiting"] and state["observation"]["screen"] == "S-026":
            return
        time.sleep(0.2)
    raise SystemExit("FAIL  could not go back to Home")


def screen_text() -> str:
    """Everything the screen shows now, with Latin digits and no separators."""
    dump = "/sdcard/Download/window.xml"
    subprocess.run([*ADB, "uiautomator", "dump", dump], check=True, capture_output=True)
    xml = subprocess.run([*ADB, "cat", dump], check=True, capture_output=True, text=True).stdout
    texts = re.findall(r'text="([^"]*)"', xml)
    return " ".join(texts).translate(DIGITS).replace(",", "").replace("٬", "")


def card_to_reza() -> None:
    """The acceptance journey. The replica must stay on the review afterwards."""
    go_home()
    plan = get("/graph/plan/S-174")
    values = {**plan["demo"], "card-transfer-destination": "رضا", "card-transfer-amount": "250000"}
    report = run("S-174", values)
    screen = get("/graph/state")["observation"]["screen"]
    if report["state"] != "review" or screen != "S-176":
        raise SystemExit(f"FAIL  card to Reza: {report['state']} at {screen}: {report['reason']}")
    time.sleep(3)
    if get("/graph/state")["observation"]["screen"] != "S-176":
        raise SystemExit("FAIL  the replica left the review")
    shown = screen_text()
    for value in ("6104336383233566", "6037990836388880", "250000"):
        if value not in shown:
            raise SystemExit(f"FAIL  the review does not show {value}: {shown}")
    print("PASS  card to Reza: review at S-176 shows the source, رضا's card and 250000, and it stayed there")
    for step in report["steps"]:
        print(f"      {step['operation']}: {step['outcome']} at {step['screen']} {step['fields']}")


def refused_when_not_built() -> None:
    """A click on a screen the replica does not have never becomes a command."""
    try:
        post("/test-harness/v1/commands", {"device_id": DEVICE_ID, "destination": "S-003"})
    except urllib.error.HTTPError as error:
        if error.code == 404:
            print("PASS  S-003: refused, not built")
            return
    raise SystemExit("FAIL  S-003 was accepted")


def every_feature() -> None:
    """Runs every feature with its demo values, and prints how each ended."""
    with open(PROFILE, encoding="utf-8") as file:
        features = json.load(file)["features"]
    for feature in features:
        screen = feature["destination"]
        restart_app()
        plan = get(f"/graph/plan/{quote(screen)}")
        if plan["status"] != "ok":
            print(f"{feature['intent']}\t{screen}\tno plan\t{plan['status']}")
            continue
        report = run(screen, plan["demo"])
        last = report["steps"][-1]["screen"] if report["steps"] else "-"
        reason = report["reason"] or ""
        print(f"{feature['intent']}\t{screen}\t{report['state']}\t{last}\t{reason}", flush=True)


def standalone_transfer(session: str, recipient: str) -> None:
    """Runs the configured transfer against the standalone mobile surface."""
    try:
        get("/graph/state", backend=STANDALONE)
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
    else:
        raise SystemExit("FAIL  development graph routes are mounted")

    conversation = uuid4().hex
    # The message schema requires context. The bridge's real resume supplies the live observation.
    observation = {
        "observation_version": 0,
        "app_version": 1,
        "profile_version": 1,
        "screen": "S-026",
        "modal": None,
        "loading": False,
        "fields": [],
        "draft": None,
        "supported_operations": ["observe", "navigate", "fill_form", "show_review"],
    }
    post(
        "/assistant/v1/messages",
        {
            "request_id": uuid4().hex,
            "conversation_id": conversation,
            "session_id": session,
            "device_id": DEVICE_ID,
            "user_context": {"user_ref": "synthetic-user", "locale": "fa-IR"},
            "observation": observation,
            "message": TRANSFER,
        },
        backend=STANDALONE,
        session=session,
    )
    deadline = time.monotonic() + TIMEOUT_SECONDS
    while time.monotonic() < deadline:
        history = get(
            f"/assistant/v1/history?conversation_id={conversation}", backend=STANDALONE, session=session
        )
        if not history["busy"]:
            break
        time.sleep(0.3)
    else:
        raise SystemExit("FAIL  standalone transfer did not end")
    steps = [(step["operation"], step["screen"], step["outcome"]) for step in history["steps"]]
    expected = [
        ("navigate", "S-192", "succeeded"),
        ("fill_form", "S-192", "succeeded"),
        ("show_review", "S-173", "succeeded"),
    ]
    if (history["current"] or {}).get("task_state") != "ready_for_user" or steps != expected:
        raise SystemExit(f"FAIL  standalone transfer did not reach review: {steps}")
    time.sleep(3)
    shown = screen_text()
    endings = {number[-4:] for number in re.findall(r"\b[0-9]{10,}\b", shown)}
    if (
        not {"0579", recipient} <= endings
        or not re.search(r"\b3000000\b", shown)
        or "پرداخت اجاره" not in shown
    ):
        raise SystemExit("FAIL  the review did not render the expected accounts, amount and reason")
    print(f"PASS  standalone internal transfer: S-173 showed ****0579 to ****{recipient}, 3000000 IRR")
    for operation, screen, outcome in steps:
        print(f"      {operation}: {outcome} at {screen}")
    print("PASS  development graph state is absent from standalone")


def serve_with_observe(session: str, graph: Path, services: Path) -> None:
    """Runs the normal standalone surface and queues one read-only observe for device QA."""
    from threading import Thread

    import uvicorn

    from assistant.app import create_standalone_app
    from assistant.bridge_link import BridgeLink
    from assistant.command_queue import CommandQueue

    queue = CommandQueue()
    app = create_standalone_app(graph, services, queue=queue)

    def probe() -> None:
        deadline = time.monotonic() + TIMEOUT_SECONDS
        while queue.device(session).observation is None:
            if time.monotonic() >= deadline:
                print("FAIL  replica did not report in for the observe probe", flush=True)
                return
            time.sleep(0.1)
        queue.bind_device(session, DEVICE_ID)
        result = BridgeLink(queue, session).send("observe", {})
        if result["outcome"] != "succeeded" or result["postcondition"] != "observed":
            print("FAIL  replica did not complete the observe probe", flush=True)
            return
        print(f"PASS  standalone observe: succeeded at {result['observation']['screen']}", flush=True)

    Thread(target=probe, daemon=True).start()
    uvicorn.run(app, host="127.0.0.1", port=8101)


def main() -> None:
    """Runs what the command line asks for."""
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--features", action="store_true")
    mode.add_argument("--standalone", metavar="SESSION")
    mode.add_argument("--serve-standalone", metavar="SESSION")
    parser.add_argument("--recipient-ending", default="7800")
    parser.add_argument("--graph", type=Path)
    parser.add_argument("--services", type=Path)
    args = parser.parse_args()
    if args.serve_standalone:
        if args.graph is None or args.services is None:
            parser.error("--serve-standalone requires --graph and --services")
        serve_with_observe(args.serve_standalone, args.graph, args.services)
        return
    if args.standalone:
        if not re.fullmatch(r"[0-9]{4}", args.recipient_ending):
            parser.error("--recipient-ending requires four digits")
        standalone_transfer(args.standalone, args.recipient_ending)
        return
    if get("/graph/state")["observation"] is None:
        raise SystemExit("FAIL  the replica has not reported in; see docs/bridge/try-it.md")
    if args.features:
        every_feature()
        return
    card_to_reza()
    refused_when_not_built()


if __name__ == "__main__":
    main()
