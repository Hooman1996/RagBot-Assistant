"""Every Persian sentence the agent says, and the masked labels of the options it offers.

Called by agent_graph's nodes. Plain text building, with no model and no app. Every value and
label goes through steps.masked, so a full card, account or IBAN number is never shown. Nothing
here asks the user to let the assistant confirm: only the user confirms, on the phone.
"""

from assistant.lookups import Candidate, FixtureLookups
from assistant.steps import masked

KNOWLEDGE = "پاسخ به پرسش‌های عمومی در این مرحله فعال نیست"
NOT_SURE = "منظورتان کدام است؟"
MODEL_FAILED = "ارتباط با مدل زبانی برقرار نشد؛ کاری انجام نشد."
NOT_POSSIBLE = "دستیار مجاز نیست به این صفحه برود؛ این کار را خودتان انجام دهید."
REVIEW_OPEN = "صفحه بازبینی باز است. آن را بررسی کنید و اگر درست بود، خودتان تأیید کنید."
FORM_FILLED = "فرم پر شد. آن را بررسی کنید و خودتان ادامه دهید."
PAGE_OPEN = "صفحه «{title}» باز شد."
FAILED = "انجام نشد: {reason}"
NOT_FOUND = "«{text}» پیدا نشد."
WHICH = "چند مورد با «{text}» پیدا شد؛ کدام؟"
NOT_THERE = "برنامه به {expected} نرسید و در {screen} ماند."
FEW, MANY = 2, 8  # a lookup with this many records offers them as options


def option(ref: str, item: Candidate) -> dict:
    """One record as a choice: its ref says which field and record, its label is masked."""
    label = item.label if item.value in (item.key, item.label) else f"{item.label} {item.value}"
    return {"ref": f"{ref}={item.key}", "label": masked(label)}


def lookup_options(lookups: FixtureLookups, wanted: list[dict]) -> list[dict]:
    """Each missing choice's allowed keys, and each missing ref's records when its lookup has
    between FEW and MANY of them, as options."""
    options = []
    for item in wanted:
        if item["type"] == "choice":
            options += [
                {"ref": f"{item['ref']}={o['key']}", "label": masked(o["label"])}
                for o in item.get("options", [])
            ]
        if item["type"] == "ref" and item["lookup"]:
            records = lookups.candidates(item["lookup"])
            if FEW <= len(records) <= MANY:
                options += [option(item["ref"], record) for record in records]
    return options


def question(note: str | None, asks: list[str]) -> str:
    """One message: the note, if any, then every missing value's question."""
    return "\n".join(([note] if note else []) + asks)


def shown(value: str | dict, item: dict, lookups: FixtureLookups) -> str:
    """A value the way the user reads it: money in rials, a record by its label, all masked."""
    if isinstance(value, dict):
        return f"{value['value']:,} ریال"
    for choice in item.get("options", []):
        if choice["key"] == value:
            return masked(choice["label"])
    if item.get("lookup"):
        for record in lookups.candidates(item["lookup"]):
            if record.value == value:
                return option(item["ref"], record)["label"]
    return masked(value)


def ready(report: dict, requirements: list[dict], lookups: FixtureLookups) -> str:
    """The reply on the review, or on a filled form without one: every value sent, by its description."""
    sent: dict = {}
    for step in report["steps"]:
        sent |= step["arguments"].get("values", {}) | step["arguments"].get("arguments", {})
    wanted = {item["ref"]: item for item in requirements}
    lines = [
        f"{wanted[ref]['description'] or ref}: {shown(value, wanted[ref], lookups)}"
        for ref, value in sent.items()
    ]
    return question(REVIEW_OPEN if report["state"] == "review" else FORM_FILLED, lines)
