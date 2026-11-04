"""Check that S-119 and S-120 lose CVV2 and the second password when the process is killed.

An instrumented test runs inside the app's process, so this one drives the device from the host.
Install the debug APK first, then run: python android/tools/check_secret_state.py
"""

import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ElementTree

ADB = ["adb", "-s", "emulator-5554"]
PACKAGE = "org.ragbot.replica"
EXPIRY = "05/08"


def shell(*args: str) -> str:
    return subprocess.run([*ADB, "shell", *args], capture_output=True, text=True).stdout


def nodes() -> list[ElementTree.Element]:
    for _ in range(5):
        shell("uiautomator", "dump", "/sdcard/replica-window.xml")
        dump = shell("cat", "/sdcard/replica-window.xml")
        if "<hierarchy" in dump:
            return list(ElementTree.fromstring(dump[dump.index("<hierarchy") :]).iter("node"))
        time.sleep(1)
    raise SystemExit("could not read the window")


def label(node: ElementTree.Element) -> str:
    return node.get("text") or node.get("content-desc") or ""


def tap_node(node: ElementTree.Element) -> None:
    left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds", "")))
    shell("input", "tap", str((left + right) // 2), str((top + bottom) // 2))
    time.sleep(1.5)


def tap(text: str) -> None:
    """Tap the first node with this text, scrolling down then up to find it."""
    for start, end in [("1800", "900")] * 8 + [("900", "1800")] * 16:
        found = [node for node in nodes() if label(node) == text]
        if found:
            tap_node(found[0])
            return
        shell("input", "swipe", "540", start, "540", end, "400")
        time.sleep(0.8)
    raise SystemExit(f"not found: {text}")


def fields() -> list[ElementTree.Element]:
    return [node for node in nodes() if "EditText" in node.get("class", "")]


def type_into(node: ElementTree.Element, text: str) -> None:
    tap_node(node)
    shell("input", "text", text)
    time.sleep(1)
    shell("input", "keyevent", "4")
    time.sleep(1)


def launch() -> None:
    shell("am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(4)


def check(screen: str) -> None:
    """Fill the open form, kill the process, and compare what comes back."""
    secrets = [node for node in fields() if node.get("password") == "true"]
    assert len(secrets) == 2, f"{screen}: expected 2 secret fields"
    for node in secrets:
        type_into(node, "1234")
    type_into(fields()[-2], EXPIRY)
    typed = [node.get("text") for node in fields() if node.get("password") == "true"]
    assert all(typed), f"{screen}: secrets were not typed"

    shell("input", "keyevent", "3")
    time.sleep(2)
    shell("am", "kill", PACKAGE)
    time.sleep(2)
    assert not shell("pidof", PACKAGE).strip(), f"{screen}: process is still alive"
    launch()

    restored = fields()
    assert any(node.get("text") == EXPIRY for node in restored), f"{screen}: state not restored"
    kept = [node.get("text") for node in restored if node.get("password") == "true" and node.get("text")]
    if kept:
        sys.exit(f"FAIL {screen}: {len(kept)} secret field(s) came back after the process was killed")
    print(f"PASS {screen}: secrets empty after the process was killed; expiry restored")


def main() -> None:
    shell("am", "force-stop", PACKAGE)
    launch()
    tap("موجودی بدون ورود")
    check("S-120")

    shell("am", "force-stop", PACKAGE)
    launch()
    tap("ورود به خانه")
    tap("کارت")
    tap("بن کارت")
    tap("موجودی بن کارت")
    check("S-119")


if __name__ == "__main__":
    main()
