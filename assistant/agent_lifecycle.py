"""The agent's nodes for a task that goes on after a pause: a message after the review, and a reconcile.

Wired into the graph by agent_graph.py, beside the nodes of agent_nodes.py. A follow-up cancels, resumes
a paused task, or lets the model say whether a message corrects the prepared task (then it is prepared
again from the app's current screen) or is a new request. A reconcile plans again from wherever the app
is after an uncertain command, and tries once more. Code acts; nothing here can confirm or pay.
"""

from assistant import replies
from assistant.agent_nodes import Nodes
from assistant.agent_state import AgentState, moved, new_request
from assistant.lifecycle import corrects, wants_cancel
from assistant.models import ModelError
from assistant.navigator import CrossesUserOnly, NoRoute


class Lifecycle:
    """The follow-up and reconcile nodes, over the same tools as the other nodes."""

    def __init__(self, nodes: Nodes) -> None:
        self.nodes = nodes

    def again(self, state: AgentState, path: tuple[str, ...], reread: bool) -> dict:
        """Moves along path, then plans again from the app's current screen, keeping the values. With
        reread, every value that is not preset is read again from the messages."""
        update = moved(state, *path)
        try:
            planned = self.nodes.planned(self.nodes.features[state["plan"]["intent"]], state["values"])
        except (NoRoute, CrossesUserOnly):
            return self.nodes.failed(state | update, replies.NOT_POSSIBLE)
        plan = planned["plan"]
        skipped = plan["preset"] if reread else planned["values"]
        planned["missing"] = [item["ref"] for item in plan["requirements"] if item["ref"] not in skipped]
        return update | planned | {"report": None, "question": None, "options": []}

    def follow_up(self, state: AgentState) -> dict:
        """A new message to a task at its review or paused: cancel, resume, correct, or a new request."""
        text = state["messages"][-1]
        if wants_cancel(text):
            return moved(state, "cancelled") | {"reply": replies.CANCELLED}
        if state["task_state"] == "paused":
            return self.again(state, ("reconciling", "resolving"), reread=False)
        title = self.nodes.features[state["plan"]["intent"]]["title"]
        try:
            correction = corrects(self.nodes.tools.chat, title, state["messages"])
        except ModelError:
            return {"reply": replies.MODEL_FAILED}  # the review stays as it is
        if correction:
            return self.again(state, ("resolving",), reread=True)
        return dict(new_request(state["conversation_id"], text))

    def reconcile(self, state: AgentState) -> dict:
        """After an uncertain command: plan again from where the app is and prepare once more."""
        if state["plan"].get("reconciled"):
            return self.nodes.failed(state, replies.FAILED.format(reason=replies.UNCERTAIN))
        update = self.again(state, ("resolving",), reread=False)
        if update.get("plan"):
            update["plan"] |= {"reconciled": True}
        return update
