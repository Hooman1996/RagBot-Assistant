"""The end-to-end click check on the emulator.

Run by hand as part of #94's acceptance, with the backend running and the app's loop enabled.
It does what a node click does, over the same API, and checks that the replica arrives.
It uses the standard library only. It asks only for navigate, the one thing the harness offers.
"""

import json
import sys
import time
import urllib.error
import urllib.request

BACKEND = "http://127.0.0.1:8100"
DEVICE_ID = "replica-dev"
TIMEOUT_SECONDS = 40


def state() -> dict:
    """Reads what the graph page reads."""
    with urllib.request.urlopen(f"{BACKEND}/graph/state", timeout=5) as reply:
        return json.load(reply)


def queue(destination: str) -> dict:
    """Queues a navigate command, as a node click does."""
    body = json.dumps({"device_id": DEVICE_ID, "destination": destination}).encode()
    request = urllib.request.Request(
        f"{BACKEND}/test-harness/v1/commands", body, {"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(request, timeout=5) as reply:
        return json.load(reply)


def wait_for_result(command_id: str) -> dict:
    """Waits until the app has posted this command's result."""
    deadline = time.time() + TIMEOUT_SECONDS
    while time.time() < deadline:
        result = state()["last_result"]
        if result and result["command_id"] == command_id:
            return result
        time.sleep(0.3)
    raise SystemExit(f"FAIL  no result for {command_id}")


def check(destination: str, outcome: str, screen: str) -> None:
    """Sends one click and compares the result and the live observation."""
    result = wait_for_result(queue(destination)["command_id"])
    seen = state()["observation"]["screen"]
    if result["outcome"] != outcome or seen != screen:
        raise SystemExit(f"FAIL  {destination}: {result['outcome']} at {seen}, wanted {outcome} at {screen}")
    print(f"PASS  {destination}: {outcome} at {seen}")


def refused_when_not_built() -> None:
    """A click on a screen the replica does not have never becomes a command."""
    try:
        queue("S-003")
    except urllib.error.HTTPError as error:
        if error.code == 404:
            print("PASS  S-003: refused, not built")
            return
    raise SystemExit("FAIL  S-003 was accepted")


def main() -> None:
    """Runs the checks: two moves, a receipt behind the user's step, and an unbuilt screen."""
    if state()["observation"] is None:
        sys.exit("FAIL  the replica has not reported in; see docs/bridge/try-it.md")
    check("S-174", "succeeded", "S-174")
    check("S-192", "succeeded", "S-192")
    before = state()["observation"]["screen"]
    check("S-093", "blocked", before)
    refused_when_not_built()


if __name__ == "__main__":
    main()
