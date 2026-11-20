"""The conversation around a task: the history lines, the turn router and its rewrite guard (D-034).

Called by the workflow's route node (a new message) and by its waiting nodes (an answer to their
question). The router is one model call, held to a schema: it says what the newest message is
(an answer, a correction, a new task, chat, only looking, or a cancel), and for a new task writes it
as one standalone request. Code acts on that. The rewrite guard keeps a rewrite only when every
amount, number and record name in it is the user's own, so a rewrite can drop a reference but
never invent a value. Every prompt is masked, and a secret never reaches one: the fast path in
lifecycle.py replaced it in the history before. The chat reply is the model's own short answer to
chat (CF-3); code replaces any reply with a number, an amount or a claim that something was done.
"""

import re

from assistant import replies
from assistant.amounts import DIGITS, amounts
from assistant.intent import PAY, SEE
from assistant.lookups import GENERIC, FixtureLookups, plain
from assistant.models import Chat, ModelError
from assistant.steps import masked, show

KINDS = ("answer", "correction", "new_task", "chat", "only_looking", "cancel")
TURNS = 8  # history lines kept: about the last 4 turns, the user's and the assistant's
LINE_CHARACTERS = 200
SHOWN = re.compile(r"\*{4}(\d{4})")  # a long number as the model is shown it
LONG = re.compile(r"\d{12,}")
TEXT_OR_NULL = {"anyOf": [{"type": "string"}, {"type": "null"}]}
PLAIN = GENERIC | {
    "کن",
    "بکن",
    "بده",
    "بزن",
    "برای",
    "برا",
    "هم",
    "رو",
    "را",
    "یک",
    "یه",
    "این",
    "همین",
    "همون",
}
SCHEMA = {
    "type": "object",
    "properties": {"kind": {"enum": list(KINDS)}, "standalone": TEXT_OR_NULL, "rest": TEXT_OR_NULL},
    "required": ["kind", "standalone", "rest"],
    "additionalProperties": False,
}
CHAT_CHARACTERS = 300
CHAT_SCHEMA = {
    "type": "object",
    "properties": {"reply": {"type": "string", "maxLength": CHAT_CHARACTERS}},
    "required": ["reply"],
    "additionalProperties": False,
}
# A claim that something was done: sent, paid, confirmed, deposited, transferred or bought.
CLAIMED = re.compile(
    r"(ارسال|پرداخت|انجام|تایید|تأیید|واریز|منتقل|انتقال|خرید|ثبت)\s*(شد|کردم|دادم|گردید|نمودم)"
)
CHAT_LINES = [
    "You are the friendly assistant of a Persian mobile banking app. Reply to the user's newest message",
    "in one or two short, warm Persian sentences.",
    "What you can do: prepare tasks in this app (transfers, bill payments, top-ups, loan installments,",
    "cards, cheques and more) up to a review page that only the user confirms, by touch.",
    "- A greeting or thanks: answer it warmly and offer help.",
    "- Something the app cannot do: say kindly that you cannot do it here, and say what you can do.",
    "- A question about rates, fees, rules or anything else: say you cannot answer that here yet.",
    "- Rude words: stay calm and polite, do not repeat them, and offer help.",
    "- Greet only when the user greets.",
    "- When a task is open, do not ask for its values: its question follows your reply.",
    "Never give numbers, amounts, account details or advice. Never say anything was sent, paid or confirmed.",
]
KIND_LINES = [
    "kind:",
    "- answer: it answers the pending question, or gives values for the open task.",
    "- correction: it changes a value the open task already holds («نه، ۳۰۰ هزار بشه», «از اون یکی کارتم»).",
    "- new_task: it asks to do or see something in the app («قبض برقمو بده», «برو به خانه»), even while a"
    " task is open, and even when it starts with «نه», «ولش» or «بیخیال» («ولش، یه شارژ بخر»).",
    "- chat: a greeting, thanks, small talk, a question, a request the app cannot do, or rude words.",
    "  The app does banking only: transfers, bills, top-ups, loans, cards, cheques, wallets, road tolls;",
    "  a request for anything else (a ticket, shopping, stocks, food, the weather) is chat.",
    "- only_looking: the user only wanted to see the page and wants nothing done («فقط میخواستم ببینم»).",
    "- cancel: the user wants to stop the open task and asks for nothing else («ولش کن»، «نمیخوام دیگه»).",
    "When the open task is none, nothing is waiting for an answer: a message asking to do or see",
    "something is new_task, and so is a vague request about the user's money, cards or accounts",
    "(«یه کاری با کارتم دارم»).",
    "standalone: only for new_task. The request as one full Persian sentence in the user's own words,",
    "filling in what it refers to from the conversation («همونو برای سارا هم بزن» → «۵۰۰ هزار تومان",
    "به سارا کارت به کارت کن»). When it refers to an earlier request («همین», «همون», «عین همین»),",
    "copy that request's amount, card, account, number and person into the sentence, as the user",
    "wrote them («از کارت 4544», «به رضا»), not from the assistant's lines. Never add an amount, name",
    "or number the conversation does not have.",
    "rest: if the message asks for a second task too, that task as one sentence; else null.",
]


def line(speaker: str, text: str) -> dict:
    """One history line, cut short. Long numbers stay whole here and are masked in prompts."""
    return {"from": speaker, "text": text[:LINE_CHARACTERS]}


def kept(turns: list[dict], *added: dict) -> list[dict]:
    """The history with the added lines, the oldest dropped beyond TURNS."""
    return (turns + list(added))[-TURNS:]


def user_words(turns: list[dict], text: str) -> str:
    """Everything the user wrote in the history and now, as matching compares it."""
    return plain(" ".join([item["text"] for item in turns if item["from"] == "user"] + [text]))


def router_prompt(turns: list[dict], task: str, pending: str, text: str) -> str:
    """The router's question: the recent conversation, the open task, the pending question and the
    newest message, all masked."""
    lines = [
        "You read the newest message in the chat of a Persian banking app's assistant and say what it is.",
        "The assistant prepares tasks in the app up to a review page; only the user confirms, by touch.",
        "",
        "Recent conversation, oldest first:",
    ]
    lines += [f"- {item['from']}: «{item['text']}»" for item in turns] or ["- none"]
    lines += [f"Open task: {task}", f"Pending question: {pending}", f"Newest message: «{text}»", ""]
    return masked("\n".join(lines + KIND_LINES))


def chat_prompt(turns: list[dict], task: str, text: str) -> str:
    """The chat reply's question: what the assistant can do, the recent conversation, the open task
    and the newest message, all masked."""
    lines = CHAT_LINES + ["", "Recent conversation, oldest first:"]
    lines += [f"- {item['from']}: «{item['text']}»" for item in turns] or ["- none"]
    lines += [f"Open task: {task}", f"Newest message: «{text}»"]
    return masked("\n".join(lines))


def safe_chat(reply: object) -> str:
    """The model's chat reply, masked, or CHAT_SAFE when it is empty, too long, or holds a number,
    an amount or a claim that something was done."""
    if not isinstance(reply, str) or not reply.strip() or len(reply) > CHAT_CHARACTERS:
        return replies.CHAT_SAFE
    said = plain(reply)
    if re.search(r"[0-9۰-۹٠-٩]", reply) or amounts(said) or CLAIMED.search(said):
        return replies.CHAT_SAFE
    return masked(reply.strip())


class Conversation:
    """The router and the guard, over the chat model and the lookups."""

    def __init__(self, chat: Chat, lookups: FixtureLookups) -> None:
        self.chat = chat
        self.lookups = lookups
        self.names = self.record_words()

    def record_words(self) -> dict[str, list[set[str]]]:
        """Each word of a person's, destination's or account's label, with the labels that hold it,
        for the guard."""
        found: dict[str, list[set[str]]] = {}
        # People and the saved card, account, IBAN and wallet destinations, and the user's own accounts.
        for lookup in ("contacts.card", "contacts.account", "contacts.iban", "contacts.wallet", "accounts"):
            for record in self.lookups.candidates(lookup):
                words = {word for word in plain(record.label).split() if word not in GENERIC}
                for word in words:
                    found.setdefault(word, []).append(words)
        return found

    def route(self, turns: list[dict], task: str, pending: str, text: str) -> dict:
        """The router's answer: {kind, standalone, rest}. A model error gives None."""
        try:
            answer = self.chat.json(router_prompt(turns, task, pending, text), SCHEMA)
        except ModelError:
            return {"kind": None, "standalone": None, "rest": None}
        kind = answer.get("kind") if answer.get("kind") in KINDS else None
        show("router", kind=kind, standalone=str(answer.get("standalone")), rest=str(answer.get("rest")))
        return {"kind": kind, "standalone": answer.get("standalone"), "rest": answer.get("rest")}

    def chat_reply(self, turns: list[dict], task: str, text: str) -> str:
        """The chat reply node (CF-3): one model call for a greeting, thanks, small talk, a question
        or a request the app cannot do, held to a schema and guarded in code."""
        try:
            answer = self.chat.json(chat_prompt(turns, task, text), CHAT_SCHEMA)
        except ModelError:
            return replies.CHAT_SAFE
        reply = safe_chat(answer.get("reply"))
        show("chat", reply=reply)
        return reply

    def guarded(
        self, rewrite: object, turns: list[dict], text: str, rest: str | None = None, second: bool = False
    ) -> str | None:
        """The rewrite with each ****1234 put back as the user's number (or dropped when the user never
        typed it), if every amount, number and record name in it is the user's own; else None."""
        if not isinstance(rewrite, str) or not rewrite.strip():
            return None
        said = user_words(turns, text)
        numbers = LONG.findall(said)

        def back(shown: re.Match) -> str:
            """The user's own number for a ****1234: typed in full, or as its last 4 digits; a masked
            number the user never typed (one the assistant showed) is dropped."""
            whole = [number for number in numbers if number.endswith(shown.group(1))]
            if len(set(whole)) == 1:
                return whole[0]
            return shown.group(1) if re.search(rf"(?<!\d){shown.group(1)}(?!\d)", said) else ""

        # Compared as matching compares text; given back in the router's own letters («پس‌انداز»).
        rewrite = " ".join(SHOWN.sub(back, rewrite.strip().translate(DIGITS)).split())
        compared = plain(rewrite)
        # A rewrite never changes what the user asks to do: no word of seeing the user did not say
        # («… بده» rewritten as «… نشون بده»), and no word of paying the user said dropped.
        if not second and any(word in compared and word not in said for word in SEE):
            return None
        if not second and PAY.search(plain(text)) and not PAY.search(compared + " " + plain(rest or "")):
            return None  # a word of paying may go with the second task of the message
        if not set(amounts(compared)) <= set(amounts(said)):
            return None
        if any(run not in said for run in re.findall(r"\d{4,}", compared)):
            return None
        # A record's word the user did not say is kept only beside another word of that record's label
        # the user did say («سپرده» for the «پس‌انداز» the user named), never as a name of its own.
        spoken = set(said.split())
        for word in compared.split():
            if (
                word in self.names
                and word not in spoken
                and not any(label & spoken for label in self.names[word])
            ):
                return None
        return rewrite

    def standalone(self, routed: dict, turns: list[dict], text: str) -> str:
        """The new task's request: the guarded rewrite once the conversation has earlier user words
        (what «همونو» refers to, or a request without its «نه ولش،»), or when the message holds a
        second task, which the rewrite leaves out; else the user's own message: a first message with
        one task stands on its own, and a rewrite could only change it."""
        earlier = any(item["from"] == "user" for item in turns)
        # Two tasks in one message: the rewrite is the first task alone, the chooser's one request; a
        # rewrite that still holds the second task has it taken out.
        rest = self.guarded(routed["rest"], turns, text, second=True)
        rewrite = self.guarded(routed["standalone"], turns, text, rest)
        if rewrite and rest and rest in rewrite and rest != rewrite:
            rewrite = rewrite.replace(rest, "").strip(" ،,و") or rewrite
        return rewrite if rewrite and (earlier or rest) else text

    def label(self, item: dict, value: str | dict) -> str:
        """A value as the user reads it, masked: a record by its label, never its key."""
        if item["lookup"] and isinstance(value, str):
            for record in self.lookups.candidates(item["lookup"]):
                if value in (record.key, record.value):
                    return replies.option(item["ref"], record)["label"]
        return replies.shown(value, item, self.lookups)

    def summary(self, state: dict, features: dict) -> str:
        """The open task as the router reads it: its feature, the values held, what is missing."""
        plan = state.get("plan")
        if not plan or state["task_state"] in ("created", "cancelled", "failed", "completed"):
            return "none."
        items = {item["ref"]: item for item in plan["requirements"]}
        held = [
            f"{items[ref]['description'] or ref} «{self.label(items[ref], value)}»"
            for ref, value in state["values"].items()
            if ref in items and ref not in plan["preset"]
        ]
        missing = [items[ref]["description"] or ref for ref in state["missing"] if ref in items]
        title = features[plan["intent"]]["title"]
        where = " It is at its review." if state["task_state"] == "ready_for_user" else ""
        held_text, missing_text = ", ".join(held) or "nothing", ", ".join(missing) or "nothing"
        return f"«{title}».{where} Held: {held_text}. Missing: {missing_text}."
