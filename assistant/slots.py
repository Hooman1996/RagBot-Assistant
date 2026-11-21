"""Fills a plan's requirements from the user's words, then checks every value in code.

Called by the agent's extract and check nodes. The model sees only the offered requirements (ref,
type, Persian ask, description and, for a choice, its Persian option labels) and the user's words,
with every long number shown as **** and its last 4 digits, so it never sees a full card number.
The check keeps nothing the plan does not ask for: money only when it is an amount the user wrote,
and a choice only as one of its allowed keys, so no other text is ever sent to the app as a key.
"""

import re

from assistant.amounts import amounts, scale_of
from assistant.lookups import GENERIC
from assistant.models import Chat
from assistant.task_runner import PERSIAN_DIGITS, TaskStopped, money_value

LONG_NUMBER = re.compile(r"\d{12,}")
SHOWN = re.compile(r"\*{4}(\d{4})")  # how a long number is shown to the model
PLAIN_KEY = re.compile(r"[\w-]+")
UNITS = {"IRR": 1, "IRT": 10}  # IRT is the toman: ten rials
# Under 1,000 toman, a written amount is usually shorthand («شصت تمن» for sixty thousand), so it is asked.
SMALLEST = 10_000
BARE = re.compile(r"(?<!\d)\d{1,3}(?!\d)")  # a short number with no scale or unit: «۱۵۰»
MONEY = {
    "type": "object",
    "properties": {"value": {"type": "integer"}, "unit": {"enum": list(UNITS)}},
    "required": ["value", "unit"],
    "additionalProperties": False,
}
# Words that may stand around a number the model copied: «کارت ****8880», «چک صیادی ****3498».
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
        "هزار is 1000 and میلیون is 1000000. When the user names no unit, use IRT.",
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
    """An amount in rials: the one in the user's newest answer, if it names one; else a bare number
    correcting an amount, in that amount's scale and unit; else the model's, only if the user wrote
    it; else the one amount the user wrote."""
    said = written(messages)
    newest = [found for found in amounts(messages[-1]) if found >= SMALLEST] if len(messages) > 1 else []
    bare = BARE.findall(messages[-1].translate(PERSIAN_DIGITS)) if len(messages) > 1 and not newest else []
    model = None
    if isinstance(value, dict) and type(value.get("value")) is int and value.get("unit") in UNITS:
        model = value["value"] * UNITS[value["unit"]]
    if len(set(newest)) == 1:
        model = newest[0]  # a correction: the newest amount the user wrote wins
    elif said and model not in said and bare:
        model = shorthand(model, value, bare, messages)
    elif said and model not in said:
        model = said[0] if len(set(said)) == 1 else None  # a model amount the user never wrote is not used
    elif not said and model is not None:
        # No unit or scale word. In a request «30» may mean rials, tomans or thousands, so the agent
        # asks; a bare number counts only as the answer to its question («چند ریال…؟»), in rials.
        typed = re.findall(r"\d+", messages[-1].translate(PERSIAN_DIGITS)) if len(messages) > 1 else []
        model = value["value"] if str(value["value"]) in typed else None
    try:
        return money_value(str(model)) if model is not None else None
    except TaskStopped:
        return None


def written(messages: list[str]) -> list[int]:
    """Every amount the user wrote, in rials, in order. A later message with no amount and one bare
    number counts that number in the scale and unit of the amount before it (D-037), so a corrected
    amount stays the user's in the turns after it."""
    found: list[int] = []
    for index, text in enumerate(messages):
        own = [amount for amount in amounts(text) if amount >= SMALLEST]
        bare = set(BARE.findall(text.translate(PERSIAN_DIGITS))) if index and not own else set()
        scale = next((each for earlier in reversed(messages[:index]) if (each := scale_of(earlier))), None)
        found += [int(bare.pop()) * scale] if len(bare) == 1 and scale else own
    return found


def shorthand(model: int | None, value: object, bare: list[str], messages: list[str]) -> int | None:
    """A bare number correcting an amount («نه ۱۵۰ بشه» after «۱۰۰ هزار تومن», D-037): the number in
    the scale and unit of the amount it replaces, when the model read that number, as written or
    already scaled. Anything else is None, to be asked: never the old amount."""
    scale = next((found for text in reversed(messages[:-1]) if (found := scale_of(text))), None)
    if scale is None or len(set(bare)) != 1 or not isinstance(value, dict):
        return None
    number = int(bare[0])
    return number * scale if value.get("value") == number or model == number * scale else None


def text(value: object, messages: list[str]) -> str | None:
    """A non-empty text with each ****1234 put back as the user's number; a number with only
    generic words around it becomes just the number."""
    if not isinstance(value, str) or not value.strip():
        return None
    found = numbers(messages)

    said = " ".join(messages).translate(PERSIAN_DIGITS)

    def back(shown: re.Match) -> str:
        """The user's number for a ****1234: a long one they typed, or the four digits they wrote on
        their own («کارتی که آخرش ۳۵۶۶»); else it stays masked, and is refused."""
        digits = shown.group(1).translate(PERSIAN_DIGITS)
        whole = [number for number in found if number.endswith(digits)]
        if whole:
            return whole[0]
        return digits if re.search(rf"(?<!\d){digits}(?!\d)", said) else shown.group()

    value = SHOWN.sub(back, value.strip())
    words = value.replace("IR ", "IR").split()
    number = [word for word in words if LONG_NUMBER.search(word)]
    if len(number) == 1 and all(word in AROUND_NUMBER for word in words if word != number[0]):
        return number[0]
    return None if SHOWN.search(value) else value


def unlabelled(value: str | None, item: dict) -> str | None:
    """The value without its field's own label word in front: «عنوان محل کار» is «محل کار»."""
    words, label = (value or "").split(), (item.get("description") or "").split()[:1]
    return " ".join(words[1:]) if label and len(words) > 1 and words[0] == label[0] else value


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
            good = unlabelled(good, item) if item["type"] == "text" else good
        if good is not None:
            kept[ref] = good
    return kept
