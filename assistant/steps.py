"""Prints each step the assistant takes, numbered, when ASSISTANT_TRACE=1.

Called by task_runner and command_queue. It is for a person reading along while debugging. Off by
default, it prints nothing and changes nothing. It must never print a full card, account or IBAN
number: every run of 12 or more digits is masked here, so no caller can forget it.
"""

import itertools
import json
import os
import re

LONG_NUMBER = re.compile(r"\d{12,}")
counter = itertools.count(1)


def masked(text: str) -> str:
    """The text with every run of 12 or more digits cut to **** and its last 4 digits."""
    return LONG_NUMBER.sub(lambda found: "****" + found.group()[-4:], text)


def show(title: str, **values: object) -> None:
    """Prints a numbered header and one aligned line per value, only when ASSISTANT_TRACE is 1."""
    if os.environ.get("ASSISTANT_TRACE") != "1":
        return
    lines = [f"── {next(counter)} · {title} ".ljust(48, "─")]
    width = max((len(name) for name in values), default=0)
    for name, value in values.items():
        text = json.dumps(value, ensure_ascii=False) if isinstance(value, dict | list) else str(value)
        lines.append(f"   {name.ljust(width)}   " + text.replace("\n", "\n" + " " * (width + 6)))
    print(masked("\n".join(lines)), flush=True)
