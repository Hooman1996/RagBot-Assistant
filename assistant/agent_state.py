"""What the agent remembers during one request, and the only way its task state may change.

Used by agent_graph's nodes, agent_chat, main_debug.py and the tests. The checkpointer stores this
state per conversation. Every change of task_state goes through moved(), which follows
contracts/transitions/task.json and refuses any change that table does not allow.
"""

import json
from pathlib import Path
from typing import TypedDict

TRANSITIONS = json.loads(
    (Path(__file__).resolve().parent.parent / "contracts" / "transitions" / "task.json").read_text(
        encoding="utf-8"
    )
)


class AgentState(TypedDict):
    """One request: the user's words, the chosen feature, its plan and values, and how it ends."""

    conversation_id: str
    messages: list[str]  # this request's user messages, oldest first
    offered: list[str]  # the intents offered to the model
    chosen: str | None  # the chosen intent, "knowledge_question" or "not_sure"
    plan: dict | None  # the navigator's plan as JSON, with its start screen and intent
    values: dict[str, str | dict]  # each requirement's value; money is {"value": int, "unit": "IRR"}
    missing: list[str]  # the refs still without a value
    question: str | None  # a note for the next question: a name not found or matched twice
    options: list[dict]  # choices for that question: {"ref", "label"}
    task_state: str  # a state of contracts/transitions/task.json
    report: dict | None  # the task runner's report
    reply: str | None  # the last thing said to the user


class BadTransition(Exception):
    """A node tried a change of task state that task.json does not allow."""


def new_request(conversation_id: str, text: str) -> AgentState:
    """The state a new user request starts from. Nothing of an earlier request carries over."""
    return AgentState(
        conversation_id=conversation_id,
        messages=[text],
        offered=[],
        chosen=None,
        plan=None,
        values={},
        missing=[],
        question=None,
        options=[],
        task_state="created",
        report=None,
        reply=None,
    )


def moved(state: AgentState, *path: str) -> dict:
    """Checks each step of path from the current task state, and returns the last as an update."""
    current = state["task_state"]
    for step in path:
        if step != current and step not in TRANSITIONS[current]:
            raise BadTransition(f"{current} -> {step}")
        current = step
    return {"task_state": current}
