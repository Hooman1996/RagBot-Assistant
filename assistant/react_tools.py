"""The ReAct agent's tools: what the model is told each one does, the shape of its arguments, and the
code that runs it.

Called by react_graph's act node, which runs the tool the model named. A tool returns a short result
for the model, and the state keys it changes. Every value shown to the user or the model is masked.
No tool can confirm, submit or pay.
"""

from dataclasses import dataclass

from langgraph.types import interrupt

from assistant import replies
from assistant.agent_nodes import Nodes
from assistant.agent_state import AgentState, moved
from assistant.lifecycle import CANCEL, wants_cancel
from assistant.slots import hidden
from assistant.steps import masked


@dataclass(frozen=True)
class Tool:
    """One tool as the model sees it."""

    text: str  # what the model is told it does
    arguments: dict  # the JSON schema of its arguments


def arguments(**properties: dict) -> dict:
    """An arguments schema where every property is required and nothing else is allowed."""
    return {
        "type": "object",
        "properties": properties,
        "required": list(properties),
        "additionalProperties": False,
    }


TEXT = {"type": "string"}
TOOLS = {
    "ask_user": Tool(
        "ask_user(question): asks the user, in Persian, and waits for the answer.", arguments(question=TEXT)
    ),
    "finish": Tool(
        "finish(message): says the message to the user, in Persian, and ends the turn.",
        arguments(message=TEXT),
    ),
}


class ReactTools:
    """The tools, over the workflow's nodes, so every act goes through the workflow's own code."""

    def __init__(self, nodes: Nodes) -> None:
        self.nodes = nodes

    def available(self, state: AgentState) -> list[str]:
        """The tools the model may call in this state."""
        return ["ask_user", "finish"]

    def ask_user(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """Asks the model's question with «لغو», and waits. «لغو» cancels; words are a new message."""
        question = masked(given["question"].strip())
        if not question:
            return {"error": "the question is empty"}, {}
        pending = {"question": question, "options": [CANCEL], "fields": [], "task_state": state["task_state"]}
        answer = interrupt(pending)
        if wants_cancel(answer):
            return {"answer": "cancel"}, moved(state, "cancelled") | {"reply": replies.CANCELLED}
        return {"answer": hidden([answer])[0]}, {"messages": state["messages"] + [answer]}

    def finish(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """Ends the turn with the model's message."""
        message = masked(given["message"].strip())
        if not message:
            return {"error": "the message is empty"}, {}
        return {}, {"reply": message}
