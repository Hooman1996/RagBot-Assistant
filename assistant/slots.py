"""Fills a plan's requirements from the user's words, then checks every value in code.

Called by agent_graph's extract and check nodes. The model sees only the offered requirements
(ref, type, Persian ask and description) and the user's words, with every long number replaced by
a placeholder, so it never sees a full card number. Its JSON may name only the offered refs. The
check keeps nothing the plan does not ask for, and money only as a whole number of rials.
"""

import re

from assistant.models import Chat
from assistant.task_runner import PERSIAN_DIGITS, TaskStopped, money_value

LONG_NUMBER = re.compile(r"\d{12,}")
PLACEHOLDER = re.compile(r"\[number (\d+)\]")
UNITS = {"IRR": 1, "IRT": 10}  # IRT is the toman: ten rials
MONEY = {
    "type": "object",
    "properties": {"value": {"type": "integer"}, "unit": {"enum": list(UNITS)}},
    "required": ["value", "unit"],
    "additionalProperties": False,
}


def numbers(messages: list[str]) -> list[str]:
    """Every run of 12 or more digits in the messages, in order, in Latin digits."""
    return [found.translate(PERSIAN_DIGITS) for text in messages for found in LONG_NUMBER.findall(text)]


def hidden(messages: list[str]) -> list[str]:
    """The messages with each long number replaced by [number N], counting across them."""
    counter = iter(range(1, len(numbers(messages)) + 1))
    return [LONG_NUMBER.sub(lambda _: f"[number {next(counter)}]", text) for text in messages]


def extract_prompt(messages: list[str], wanted: list[dict]) -> str:
    """The question for the model: the offered requirements and the user's words."""
    lines = [
        "Fill in these values from the user's Persian messages. Use null for any value the user did",
        "not give. Never guess.",
        "Values:",
    ]
    lines += [f"- {item['ref']} ({item['type']}): {item['ask']} — {item['description']}" for item in wanted]
    lines += [
        'Money is {"value": a whole number, "unit": "IRT" for تومان or "IRR" for ریال}. هزار is 1000 and',
        "میلیون is 1000000. When the user names no unit, use IRR.",
        "For a person, card, account or record, copy the words or [number N] the user wrote.",
        "Later messages answer the assistant's questions.",
        "Messages:",
    ]
    lines += [f"{index}. «{text}»" for index, text in enumerate(hidden(messages), start=1)]
    return "\n".join(lines)


def extract(chat: Chat, messages: list[str], requirements: list[dict]) -> dict:
    """The model's values for the offered requirements, leaving out the ones it set to null."""
    shapes = {item["ref"]: MONEY if item["type"] == "money" else {"type": "string"} for item in requirements}
    schema = {
        "type": "object",
        "properties": {ref: {"anyOf": [shape, {"type": "null"}]} for ref, shape in shapes.items()},
        "required": [item["ref"] for item in requirements],
        "additionalProperties": False,
    }
    answer = chat.json(extract_prompt(messages, requirements), schema)
    return {ref: value for ref, value in answer.items() if value is not None}


def money(value: object) -> dict | None:
    """A whole amount with a known unit, as rials the way the contract writes them, or None."""
    if not isinstance(value, dict) or type(value.get("value")) is not int or value.get("unit") not in UNITS:
        return None
    try:
        return money_value(str(value["value"] * UNITS[value["unit"]]))
    except TaskStopped:
        return None


def text(value: object, messages: list[str]) -> str | None:
    """A non-empty text, with a [number N] placeholder put back as the number, or None."""
    if not isinstance(value, str) or not value.strip():
        return None
    placeholder = PLACEHOLDER.fullmatch(value.strip())
    if placeholder is None:
        return value.strip()
    found = numbers(messages)
    index = int(placeholder.group(1)) - 1
    return found[index] if 0 <= index < len(found) else None


def checked(values: dict, requirements: list[dict], messages: list[str]) -> dict:
    """Only the values the plan asks for, each in the form the task runner takes."""
    wanted = {item["ref"]: item for item in requirements}
    kept = {}
    for ref, value in values.items():
        if ref not in wanted:
            continue
        good = money(value) if wanted[ref]["type"] == "money" else text(value, messages)
        if good is not None:
            kept[ref] = good
    return kept
