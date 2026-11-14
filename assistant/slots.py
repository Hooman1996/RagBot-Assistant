"""Fills a plan's requirements from the user's words, then checks every value in code.

Called by the agent's extract and check nodes. The model sees only the offered requirements (ref,
type, Persian ask, description and, for a choice, its Persian option labels) and the user's words,
with every long number shown as **** and its last 4 digits, so it never sees a full card number.
The check keeps nothing the plan does not ask for: money only when it is an amount the user wrote,
and a choice only as one of its allowed keys, so no other text is ever sent to the app as a key.
"""

import re

from assistant.amounts import amounts
from assistant.lookups import GENERIC
from assistant.models import Chat
from assistant.task_runner import PERSIAN_DIGITS, TaskStopped, money_value

LONG_NUMBER = re.compile(r"\d{12,}")
SHOWN = re.compile(r"\*{4}(\d{4})")  # how a long number is shown to the model
PLAIN_KEY = re.compile(r"[\w-]+")
UNITS = {"IRR": 1, "IRT": 10}  # IRT is the toman: ten rials
MONEY = {
    "type": "object",
    "properties": {"value": {"type": "integer"}, "unit": {"enum": list(UNITS)}},
    "required": ["value", "unit"],
    "additionalProperties": False,
}
# Words that may stand around a number the model copied: «کارت ****0091», «چک صیادی ****0311».
AROUND_NUMBER = GENERIC | {"چک", "صیادی", "صیاد", "دریافتی", "شناسه", "قبض", "IR"}


def numbers(messages: list[str]) -> list[str]:
    """Every run of 12 or more digits in the messages, in order, in Latin digits."""
    return [found.translate(PERSIAN_DIGITS) for text in messages for found in LONG_NUMBER.findall(text)]


def hidden(messages: list[str]) -> list[str]:
    """The messages with each long number shown only as **** and its last 4 digits."""
    return [
        LONG_NUMBER.sub(lambda found: "****" + found.group()[-4:].translate(PERSIAN_DIGITS), t)
        for t in messages
    ]


def extract_prompt(messages: list[str], wanted: list[dict]) -> str:
    """The question for the model: the offered requirements and the user's words."""
    lines = [
        "Fill in these values from the user's Persian messages. Use null for any value the user did",
        "not give. Never guess.",
        "Values:",
    ]
    for item in wanted:
        line = f"- {item['ref']} ({item['type']}): {item['ask']} — {item['description']}"
        lines.append(line + (f" Choose one of: {' | '.join(item['options'])}" if item.get("options") else ""))
    lines += [
        'Money is {"value": the number as the user said it, "unit": "IRT" for تومان or "IRR" for ریال}.',
        "هزار is 1000 and میلیون is 1000000. When the user names no unit, use IRR.",
        "For a person, card, account or record, copy the words or the ****1234 number the user wrote.",
        "Later messages answer the assistant's questions and replace anything they change.",
        "Messages:",
    ]
    lines += [f"{index}. «{text}»" for index, text in enumerate(hidden(messages), start=1)]
    return "\n".join(lines)


def shape(item: dict) -> dict:
    """The JSON shape of one value: money, one of a choice's labels, or text."""
    if item["type"] == "money":
        return MONEY
    return {"enum": item["options"]} if item.get("options") else {"type": "string"}


def extract(chat: Chat, messages: list[str], requirements: list[dict]) -> dict:
    """The model's values for the offered requirements, leaving out the ones it set to null."""
    schema = {
        "type": "object",
        "properties": {item["ref"]: {"anyOf": [shape(item), {"type": "null"}]} for item in requirements},
        "required": [item["ref"] for item in requirements],
        "additionalProperties": False,
    }
    answer = chat.json(extract_prompt(messages, requirements), schema)
    return {ref: value for ref, value in answer.items() if value is not None}


def money(value: object, messages: list[str]) -> dict | None:
    """An amount in rials: the one in the user's newest answer, if it names one; else the model's,
    only if the user wrote it; else the one amount the user wrote."""
    said = [found for text in messages for found in amounts(text)]
    newest = amounts(messages[-1]) if len(messages) > 1 else []
    model = None
    if isinstance(value, dict) and type(value.get("value")) is int and value.get("unit") in UNITS:
        model = value["value"] * UNITS[value["unit"]]
    if len(set(newest)) == 1:
        model = newest[0]  # a correction: the newest amount the user wrote wins
    elif said and model not in said:
        model = said[0] if len(set(said)) == 1 else None  # a model amount the user never wrote is not used
    elif not said and model is not None:
        # No unit or scale word: only a number the user typed as it is counts.
        typed = re.findall(r"\d+", " ".join(messages).translate(PERSIAN_DIGITS))
        model = model if str(model) in typed else None
    try:
        return money_value(str(model)) if model is not None else None
    except TaskStopped:
        return None


def text(value: object, messages: list[str]) -> str | None:
    """A non-empty text with each ****1234 put back as the user's number; a number with only
    generic words around it becomes just the number."""
    if not isinstance(value, str) or not value.strip():
        return None
    found = numbers(messages)

    def back(shown: re.Match) -> str:
        whole = [number for number in found if number.endswith(shown.group(1).translate(PERSIAN_DIGITS))]
        return whole[0] if whole else shown.group()

    value = SHOWN.sub(back, value.strip())
    words = value.replace("IR ", "IR").split()
    number = [word for word in words if LONG_NUMBER.search(word)]
    if len(number) == 1 and all(word in AROUND_NUMBER for word in words if word != number[0]):
        return number[0]
    return None if SHOWN.search(value) else value


def choice(value: object, options: list[dict]) -> str | None:
    """The app's key for a choice: an allowed key or its label; with no list, a plain key only."""
    if not isinstance(value, str):
        return None
    for item in options:
        if value.strip() in (item["key"], item["label"]):
            return item["key"]
    return value.strip() if not options and PLAIN_KEY.fullmatch(value.strip()) else None


def checked(values: dict, requirements: list[dict], messages: list[str]) -> dict:
    """Only the values the plan asks for, each in the form the task runner takes."""
    wanted = {item["ref"]: item for item in requirements}
    kept = {}
    for ref, value in values.items():
        item = wanted.get(ref)
        if item is None:
            continue
        if item["type"] == "money":
            good = money(value, messages)
        elif item["type"] == "choice":
            good = choice(value, item.get("options", []))
        else:
            good = text(value, messages)
        if good is not None:
            kept[ref] = good
    return kept
