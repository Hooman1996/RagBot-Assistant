"""The task's life around the review: cancel, a message after the review, and a run the user stops.

Called by the agent's nodes, agent_chat and main_debug. A cancel needs no model: the «لغو» option, or
a message that is only a cancel phrase. After the review the model only says whether a new message
corrects the prepared task or is a new request, held to an enum; code does the rest. Nothing here
can confirm, submit or pay, and a command already sent is never undone.
"""

from collections.abc import Callable

from assistant.agent_state import new_request
from assistant.lookups import plain
from assistant.models import Chat
from assistant.slots import hidden
from assistant.task_runner import Bridge, Cancelled

CANCEL = {"ref": "cancel", "label": "لغو"}  # offered with every question
CANCEL_WORDS = {
    "لغو", "لغو کن", "لغوش کن", "کنسل", "کنسل کن", "کنسلش کن", "بیخیال", "بی خیال", "بیخیالش",
    "منصرف شدم", "ولش کن", "انصراف", "نمیخوام", "نمی خوام", "دیگه نمیخوام", "cancel",
}  # fmt: skip
FOLLOW_UP = ("ready_for_user", "paused")  # a new message continues these tasks instead of replacing them
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


def next_input(previous: dict, conversation_id: str, text: str) -> dict:
    """What a new message starts: a follow-up of a task at its review or paused, else a new request."""
    if previous.get("task_state") in FOLLOW_UP:
        return {"messages": previous["messages"] + [text], "reply": None}
    return new_request(conversation_id, text)


def follow_up_prompt(title: str, messages: list[str]) -> str:
    """The question for the model: does the newest message change the prepared task?"""
    lines = [f"The assistant prepared «{title}» for the user from these messages:"]
    lines += [f"- «{text}»" for text in hidden(messages[:-1])]
    lines += [
        f"Then the user wrote: «{hidden(messages)[-1]}»",
        "Answer correction when it changes or adds a value of that same task (an amount, an account,",
        "a card, a person, a date), and new_request when it asks for something else.",
    ]
    return "\n".join(lines)


def corrects(chat: Chat, title: str, messages: list[str]) -> bool:
    """True when the model reads the newest message as a correction of the prepared task."""
    schema = {
        "type": "object",
        "properties": {"answer": {"enum": ["correction", "new_request"]}},
        "required": ["answer"],
        "additionalProperties": False,
    }
    return chat.json(follow_up_prompt(title, messages), schema).get("answer") == "correction"


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
