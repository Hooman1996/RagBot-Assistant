"""Every Persian sentence the agent says, and the masked labels of the options it offers.

Called by agent_graph's nodes. Plain text building, with no model and no app. Every value and
label goes through steps.masked, so a full card, account or IBAN number is never shown. Nothing
here asks the user to let the assistant confirm: only the user confirms, on the phone.
"""

from assistant import words
from assistant.lookups import Candidate, FixtureLookups
from assistant.steps import masked

KNOWLEDGE = "پاسخ به پرسش‌های عمومی در این مرحله فعال نیست"
CHAT_SAFE = "در خدمتم؛ بنویسید چه کاری در برنامه انجام دهم."
NOT_SURE = "منظورتان کدام است؟"
MODEL_FAILED = "ارتباط با مدل زبانی برقرار نشد؛ کاری انجام نشد."
NOT_POSSIBLE = "دستیار مجاز نیست به این صفحه برود؛ این کار را خودتان انجام دهید."
REVIEW_OPEN = "صفحه بازبینی باز است. آن را بررسی کنید و اگر درست بود، خودتان تأیید کنید."
FORM_FILLED = "فرم پر شد. آن را بررسی کنید و خودتان ادامه دهید."
PAGE_OPEN = "صفحه «{title}» باز شد."
FAILED = "انجام نشد: {reason}"
NOT_FOUND = "«{text}» پیدا نشد."
WHICH = "چند مورد با «{text}» پیدا شد؛ کدام؟"
NOT_OFFERED = "«{text}» برای این کار در برنامه قابل انتخاب نیست."
NOT_A_CHARGE = "این مبلغ برای این اپراتور در برنامه نیست؛ مبلغ‌های شارژ: {amounts} ریال."
OUT_OF_RANGE = "مبلغ باید بین {low} و {high} ریال باشد."
TOP_UP_LIMITS = "مبلغ شارژ این اپراتور باید بین {low} و {high} ریال و ضریبی از {step} ریال باشد."
OVER_BALANCE = "مبلغ از موجودی مبدا ({balance} ریال) بیشتر است."
NOT_THERE = "برنامه به {expected} نرسید و در {screen} ماند."
CANCELLED = "کار لغو شد؛ چیزی ارسال یا تأیید نشد."
CANCELLED_OPEN = "کار لغو شد؛ صفحه همان‌طور باز ماند و چیزی ارسال یا تأیید نشد."
# What the user is told when a run stops on its own terms: cancelled, out of reach, or uncertain.
STOPPED = {
    "cancelled": "کار لغو شد. برنامه همان‌جا که بود ماند؛ چیزی تأیید یا ارسال نشد.",
    "unreachable": "برنامه در دسترس نیست؛ کار نگه داشته شد. وقتی برنامه باز بود بنویسید «ادامه»، یا «لغو».",
    "uncertain": None,
}
UNCERTAIN = "معلوم نشد آخرین فرمان در برنامه انجام شد یا نه"
# The conversation flow (D-034): the fast path's refusals, the switch question and its options.
CONFIRM_BY_TOUCH = "تأیید فقط با لمس دکمه در صفحه بازبینی انجام می‌شود؛ من فقط آماده می‌کنم."
NO_SECRETS = "رمز یا کد را هرگز اینجا ننویسید؛ آن را فقط خودتان در صفحه برنامه وارد کنید."
NOTHING_OPEN = "کاری در جریان نیست؛ بنویسید چه کاری انجام دهم."
SWITCH = "کار «{old}» نیمه‌کاره است؛ لغو شود و «{new}» شروع شود؟"
SWITCH_YES = {"ref": "switch=yes", "label": "بله، شروع کن"}
SWITCH_NO = {"ref": "switch=no", "label": "نه، ادامه بده"}
GOING_ON = "باشه، کار «{title}» ادامه دارد."
NEW_TASK = "کار تازه"  # the switch question's name for a request the chooser was not sure of
SECOND_TASK = "درخواست دوم را بعد از این بنویسید: «{rest}»."
REMOVED = "[حذف شد]"  # a secret, as the history keeps it
FEW, MANY = 2, 8  # a lookup with this many records offers them as options


def option(ref: str, item: Candidate) -> dict:
    """One record as a choice: its ref says which field and record, its label is masked."""
    label = item.label if item.value in (item.key, item.label) else f"{item.label} {item.value}"
    return {"ref": f"{ref}={item.key}", "label": masked(label)}


def offered(lookups: FixtureLookups, item: dict) -> list[Candidate]:
    """The records the app lists for this field, the only ones the assistant offers or accepts. A
    policy or loan whose installments are all paid has nothing the app lets the user pay."""
    records = lookups.candidates(item["lookup"], words.offered(item["lookup"], item["ref"]))
    if words.has_installments(item["ref"]):
        return [record for record in records if lookups.installments(item["lookup"], record.key)]
    return records


def lookup_options(lookups: FixtureLookups, wanted: list[dict]) -> list[dict]:
    """Each missing choice's allowed keys, and each missing ref's records the field may offer
    when there are between FEW and MANY of them, as options."""
    options = []
    for item in wanted:
        if item["type"] == "choice":
            options += [
                {"ref": f"{item['ref']}={o['key']}", "label": masked(o["label"])}
                for o in item.get("options", [])
            ]
        if item["type"] == "ref" and item["lookup"]:
            records = offered(lookups, item)
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
