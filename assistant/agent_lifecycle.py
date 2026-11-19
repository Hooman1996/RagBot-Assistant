"""The agent's nodes around a task: every new message in the conversation, the switch question,
the follow-up of the ReAct agent, and a reconcile.

Wired into the graph by agent_graph.py, beside the nodes of agent_nodes.py. route is door A of D-034:
each new message goes through the fast path, then the turn router, and code acts on what it is
(a new request, a correction or resume of the task at its review or paused, chat, a cancel, or a new
task while one is open, which asks the switch question). follow_up is the ReAct agent's (D-032).
A reconcile plans again from wherever the app is after an uncertain command, and tries once more.
Code acts; nothing here can confirm or pay.
"""

from langgraph.types import interrupt

from assistant import replies
from assistant.agent_nodes import MODEL_SEES, REFUSALS, Nodes
from assistant.agent_state import AgentState, moved, new_request
from assistant.conversation import kept, line
from assistant.intent import NOT_SURE
from assistant.lifecycle import CANCEL, FOLLOW_UP, NO_WORDS, YES_WORDS, corrects, fast, wants_cancel
from assistant.lookups import plain
from assistant.models import ModelError
from assistant.navigator import CrossesUserOnly, NoRoute
from assistant.slots import extract
from assistant.steps import masked
from assistant.words import feature_words, field_words, plates


class Lifecycle:
    """The route, switch, follow-up and reconcile nodes, over the same tools as the other nodes."""

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
        intent = state["plan"]["intent"]
        title = self.nodes.features[intent]["title"]
        try:
            correction = corrects(self.nodes.tools.chat, title, feature_words(intent), state["messages"])
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

    def stopped(self, state: AgentState) -> dict:
        """A cancel or "only looking" at door A: the open task is cancelled and the app stays as it is;
        with nothing open, the fixed line."""
        if state["task_state"] not in FOLLOW_UP:
            return {"reply": replies.NOTHING_OPEN}
        reply = replies.CANCELLED_OPEN if state["report"] else replies.CANCELLED
        return moved(state, "cancelled") | {"reply": reply}

    def route(self, state: AgentState) -> dict:
        """Door A (D-034): a new message, read in the conversation. The fast path refuses a confirm
        request or a secret and cancels; the router says what else it is, and code acts. With no task
        open, anything but chat or a cancel is a new request (the rewrite, guarded, else the message).
        With a task at its review or paused, a correction is prepared again and a paused task resumes,
        as the follow-up did; a new task asks the switch question."""
        text = state["said"] if state.get("said") is not None else state["messages"][-1]
        open_task = state["task_state"] in FOLLOW_UP
        kind = fast(text)
        turns = kept(state["turns"], line("user", replies.REMOVED if kind == "secret" else text))
        base = {"turns": turns, "said": None, "route": None, "lead": None, "switch_to": None}
        # next_input put the message in the task's messages; it stays there only as an answer.
        taken = {"messages": state["messages"][:-1] if open_task else []}
        if kind in REFUSALS:
            return base | taken | {"reply": REFUSALS[kind]}
        if kind:
            return base | taken | self.stopped(state)
        conversation = self.nodes.conversation
        task = conversation.summary(state, self.nodes.features)
        routed = conversation.route(state["turns"], task, "none.", text)
        kind = routed["kind"] or ("answer" if open_task else "new_task")  # the model failed: as before
        if kind in ("cancel", "only_looking") and self.nodes.names_a_feature(text):
            kind = "new_task"  # «ولش، کارت به کارت میخوام» asks for another task
        if kind == "chat" and not open_task and self.nodes.names_a_feature(text):
            kind = "new_task"  # with no task open, the chooser decides what is a request, as before
        if kind in ("cancel", "only_looking"):
            return base | taken | self.stopped(state)
        if kind == "chat":
            return base | taken | {"reply": replies.KNOWLEDGE}
        rest = conversation.guarded(routed["rest"], state["turns"], text)
        if not open_task:
            request = conversation.standalone(routed, state["turns"], text) if kind == "new_task" else text
            return (
                dict(new_request(state["conversation_id"], request))
                | base
                | {"rest": rest, "route": "understand"}
            )
        if kind == "new_task":
            return (
                base
                | taken
                | self.switching(state, conversation.standalone(routed, state["turns"], text), rest)
            )
        if state["task_state"] == "paused":
            return base | self.again(state, ("reconciling", "resolving"), reread=False) | {"route": "extract"}
        return base | self.corrected(state, text) | {"route": "extract"}

    def corrected(self, state: AgentState, text: str) -> dict:
        """A correction after the review: planned again from the app's current screen with the values
        held, and the newest message read on its own, so what it changes replaces the old value and
        the rest stays (re-reading every message let the model bring the old value back)."""
        update = self.again(state, ("resolving",), reread=False)
        task = state | update
        offered = [
            {key: item[key] for key in MODEL_SEES}
            | {"description": field_words(item["ref"]) or item["description"]}
            | {"options": [option["label"] for option in item.get("options", [])]}
            for item in task["plan"]["requirements"]
            if item["ref"] not in task["plan"]["preset"] and item["ref"] not in plates()
        ]
        try:
            found = extract(self.nodes.tools.chat, [text], offered) if offered else {}
        except ModelError:
            found = {}
        # A record the message names by its own words («از کوتاه‌مدت»), as the workflow's named does.
        named = self.nodes.named(task["plan"], {}, [text])
        return update | {"values": task["values"] | named | found}

    def switching(self, state: AgentState, request: str, rest: str | None) -> dict:
        """A new task while one is open: its feature first, then the switch question. A knowledge
        question gets the fixed reply, and the open task stays as it is; a request the chooser is not
        sure of is still asked about, and understood again after «بله»."""
        try:
            found = self.nodes.detect(request)
        except ModelError:
            return {"reply": replies.MODEL_FAILED}
        if found["chosen"] not in self.nodes.features and found["chosen"] != NOT_SURE:
            return {"reply": replies.KNOWLEDGE}
        return {"route": "switch", "switch_to": found | {"rest": rest}}

    def switch(self, state: AgentState) -> dict:
        """The switch question: «بله» cancels the open task and starts the new one from where the app
        is; «نه» goes back to the open task (its question asked again, or the review as it is);
        «لغو» cancels the open task. Any other answer asks it again."""
        new, old = state["switch_to"], self.nodes.features[state["plan"]["intent"]]["title"]
        known = new["chosen"] in self.nodes.features
        title = self.nodes.features[new["chosen"]]["title"] if known else replies.NEW_TASK
        question = replies.SWITCH.format(old=old, new=title)
        shown = "\n".join(part for part in (state["lead"], question) if part)
        options = [replies.SWITCH_YES, replies.SWITCH_NO, CANCEL]
        answer = interrupt(
            {"question": shown, "options": options, "fields": [], "task_state": state["task_state"]}
        )
        typed = answer not in [item["ref"] for item in options]
        kind = fast(answer) if typed else None
        heard = replies.REMOVED if kind == "secret" else answer
        turns = (
            kept(state["turns"], line("assistant", masked(shown)), line("user", heard))
            if typed
            else state["turns"]
        )
        words = plain(answer).strip(" .!؟?،,")
        if answer == CANCEL["ref"] or wants_cancel(answer):
            reply = replies.CANCELLED_OPEN if state["report"] else replies.CANCELLED
            return moved(state, "cancelled") | {
                "reply": reply,
                "turns": turns,
                "switch_to": None,
                "route": None,
            }
        if answer == replies.SWITCH_YES["ref"] or words in YES_WORDS:
            moved(state, "cancelled")  # the open task may be left: refused here if task.json did not allow it
            fresh = dict(new_request(state["conversation_id"], new["text"]))
            return (
                fresh
                | {"turns": turns, "offered": new["offered"], "chosen": new["chosen"]}
                # A request the chooser was not sure of is understood again, and its options asked.
                | {"rest": new["rest"], "route": "plan" if known else "understand"}
            )
        if answer == replies.SWITCH_NO["ref"] or words in NO_WORDS:
            if state["task_state"] == "clarifying":
                return {"turns": turns, "switch_to": None, "route": "clarify", "lead": None}
            return {
                "turns": turns,
                "switch_to": None,
                "route": None,
                "reply": replies.GOING_ON.format(title=old),
            }
        return {"turns": turns, "route": "switch", "lead": REFUSALS.get(kind)}
