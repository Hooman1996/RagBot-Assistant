"""The task's life around the review: cancel, a message after the review, and a run the user stops.

Called by the agent's nodes, agent_chat and main_debug. A cancel needs no model: the «لغو» option, or
a message that is only a cancel phrase. After the review the model only says whether a new message
corrects the prepared task or is a new request, held to an enum; code does the rest. Nothing here
can confirm, submit or pay, and a command already sent is never undone.
"""

import re
from collections.abc import Callable

from assistant.agent_state import new_request
from assistant.amounts import DIGITS
from assistant.conversation import kept, line
from assistant.lookups import plain
from assistant.models import Chat
from assistant.slots import hidden
from assistant.steps import masked
from assistant.task_runner import Bridge, Cancelled

CANCEL = {"ref": "cancel", "label": "لغو"}  # offered with every question
CANCEL_WORDS = {
    "لغو", "لغو کن", "لغوش کن", "کنسل", "کنسل کن", "کنسلش کن", "بیخیال", "بی خیال", "بیخیالش",
    "منصرف شدم", "ولش کن", "انصراف", "نمیخوام", "نمی خوام", "دیگه نمیخوام", "cancel",
}  # fmt: skip
# While a question is asked: the user only wanted to see the page, so the task stops there.
LOOKING = ("فقط ببینم", "میخواستم ببینم", "می خواستم ببینم", "فقط میخواستم", "فقط می خواستم", "فقط نگاه")
FOLLOW_UP = ("ready_for_user", "paused")  # a new message continues these tasks instead of replacing them
# The fast path (D-034), before any model: a whole-message request to confirm or submit, which only
# the user does, by touch; and a secret (a password, PIN, CVV2 or code) with its digits.
CONFIRM_WORDS = {
    "تأیید کن", "تایید کن", "تأییدش کن", "تاییدش کن", "بزن بره", "پرداختش کن", "انجامش بده", "ثبت کن",
    "ثبتش کن", "نهایی کن", "نهاییش کن", "ارسال کن", "ارسالش کن", "تأیید", "تایید",
}  # fmt: skip
# A secret word starts a word: «رمز دومم» is one, the fee in «کارمزدش» is not.
SECRET_WORDS = re.compile(r"(?<!\w)(رمز|پین|pin|cvv|سی وی وی|کد تایید|کد تأیید|کد یکبار|کد یک بار|otp)")
SECRET_DIGITS = re.compile(r"(?<!\d)\d{3,8}(?!\d)")
# Typed answers to the switch question.
YES_WORDS = {"بله", "آره", "اره", "باشه", "اوکی", "حتما", "بله شروع کن", "آره شروع کن", "شروع کن"}
NO_WORDS = {"نه", "خیر", "نه ادامه بده", "ادامه بده", "نه ادامه", "همونو ادامه بده"}
# What the runner's stop means for the task: the path of states, and what to tell the user.
AFTER_STOP = {
    "cancelled": ("cancelling", "cancelled"),
    "unreachable": ("paused",),
    "uncertain": ("reconciling",),
}


def wants_cancel(answer: str) -> bool:
    """True for the «لغو» option, or a message that is nothing but a cancel phrase."""
    words = plain(answer).strip(" .!؟?،,")
    return answer == CANCEL["ref"] or words in CANCEL_WORDS


def fast(text: str) -> str | None:
    """The fast path's reading of a typed message: "secret", "confirm", "cancel", "only_looking", or
    None for the router."""
    words = plain(text).strip(" .!؟?،,")
    lowered = words.lower()
    if SECRET_WORDS.search(lowered) and SECRET_DIGITS.search(lowered.translate(DIGITS)):
        return "secret"
    if words in CONFIRM_WORDS:
        return "confirm"
    if wants_cancel(text):
        return "cancel"
    return "only_looking" if only_looking(text) else None


def only_looking(answer: str) -> bool:
    """True for an answer that says the user only wanted to see the page («میخواستم ببینم فقط»)."""
    return any(phrase in plain(answer) for phrase in LOOKING)


def next_input(previous: dict, conversation_id: str, text: str) -> dict:
    """What a new message starts: a follow-up of a task at its review or paused, else a new request.
    Either way the conversation's history carries over, with what the assistant said last, and the
    message is the turn's `said` (D-034)."""
    said = [line("assistant", masked(previous["reply"]))] if previous.get("reply") else []
    memory = {"said": text, "turns": kept(previous.get("turns", []), *said)}
    if previous.get("task_state") in FOLLOW_UP:
        return {"messages": previous["messages"] + [text], "reply": None} | memory
    return new_request(conversation_id, text) | memory


def follow_up_prompt(title: str, words: str, messages: list[str]) -> str:
    """The question for the model: does the newest message change the prepared task?"""
    lines = [f"The assistant prepared the task «{title}» ({words}) for the user from these messages:"]
    lines += [f"- «{text}»" for text in hidden(messages[:-1])]
    lines += [
        f"Then the user wrote: «{hidden(messages)[-1]}»",
        "Answer new_request when the newest message asks for a task of its own: it names a kind of task",
        "(a transfer, a payment, a page to see) and that task's own words, even if it is the same kind.",
        "Answer correction only when it just changes or adds a value of the prepared task (an amount,",
        "an account, a card, a person, a date) without asking for a task again.",
    ]
    return "\n".join(lines)


def corrects(chat: Chat, title: str, words: str, messages: list[str]) -> bool:
    """True when the model reads the newest message as a correction of the prepared task."""
    schema = {
        "type": "object",
        "properties": {"answer": {"enum": ["correction", "new_request"]}},
        "required": ["answer"],
        "additionalProperties": False,
    }
    return chat.json(follow_up_prompt(title, words, messages), schema).get("answer") == "correction"


class StoppableBridge:
    """The bridge, refusing to send the next command once the user has cancelled."""

    def __init__(self, bridge: Bridge, stopped: Callable[[], bool]) -> None:
        self.bridge = bridge
        self.stopped = stopped

    def send(self, operation: str, arguments: dict) -> dict:
        """Sends the command, unless the user cancelled; a command already sent is not undone."""
        if self.stopped():
            raise Cancelled("کاربر لغو کرد")
        return self.bridge.send(operation, arguments)
