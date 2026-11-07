"""End-to-end checks on the emulator, over the same API the graph page uses.

Run by hand, with the backend running and the app's loop enabled. With no arguments it runs the
acceptance journey: card to card to رضا for 250000 IRR, reaching the review and staying there.
With --features it runs every feature with its demo values and prints how each ended.
It uses the standard library only. It starts runs and moves, and has no way to confirm.
"""

import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from urllib.parse import quote

BACKEND = "http://127.0.0.1:8100"
DEVICE_ID = "replica-dev"
TIMEOUT_SECONDS = 180
PROFILE = "app_profiles/replica.profile.json"
ADB = ["adb", "-s", "emulator-5554", "shell"]
DIGITS = str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")


def get(path: str) -> dict:
    """Reads one JSON route."""
    with urllib.request.urlopen(f"{BACKEND}{path}", timeout=5) as reply:
        return json.load(reply)


def post(path: str, body: dict) -> dict:
    """Posts JSON and reads the reply."""
    data = json.dumps(body).encode()
    request = urllib.request.Request(f"{BACKEND}{path}", data, {"Content-Type": "application/json"})
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
    for value in ("0000000000000018", "0000000000000091", "250000"):
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


def main() -> None:
    """Runs what the command line asks for."""
    if get("/graph/state")["observation"] is None:
        sys.exit("FAIL  the replica has not reported in; see docs/bridge/try-it.md")
    if "--features" in sys.argv:
        every_feature()
        return
    card_to_reza()
    refused_when_not_built()


if __name__ == "__main__":
    main()
