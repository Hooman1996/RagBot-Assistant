"""The agent's nodes: each one plain method that takes the state and returns only what it changes.

Wired into one graph by agent_graph.py. The model only returns JSON, through intent.py and
slots.py; code plans, resolves, runs and checks. The app is reached only through TaskRunner, so
the only commands are navigate, fill_form and show_review. Nothing here can confirm, submit or pay.
"""

from collections.abc import Callable
from dataclasses import dataclass

from langgraph.types import interrupt

from assistant import replies
from assistant.agent_state import AgentState, moved
from assistant.intent import KNOWLEDGE, NOT_SURE, FeatureIndex, choose
from assistant.lookups import FixtureLookups, typed
from assistant.models import Chat, ModelError
from assistant.navigator import CrossesUserOnly, Navigator, NoRoute, Plan
from assistant.slots import checked, extract, hidden
from assistant.steps import masked, show
from assistant.task_runner import Bridge, TaskReport, TaskRunner
from assistant.words import field_words, fixed, options

MODEL_SEES = ("ref", "type", "ask", "description")


@dataclass
class Tools:
    """What the nodes work with. Only the bridge and the screen differ between server and debug run."""

    chat: Chat
    index: FeatureIndex
    navigator: Navigator
    lookups: FixtureLookups
    bridge: Bridge
    screen: Callable[[], str]  # where the app is now


def plan_for(navigator: Navigator, start: str, feature: dict) -> Plan:
    """The feature's plan: through its form when it has one, as the graph page plans it."""
    if feature["form"] is None:
        return navigator.plan(start, feature["destination"])
    return navigator.plan_feature(start, feature)


class Nodes:
    """The graph's nodes, over one set of tools."""

    def __init__(self, tools: Tools) -> None:
        self.tools = tools
        self.features = {item["intent"]: item for item in tools.navigator.features}

    def failed(self, state: AgentState, reply: str) -> dict:
        """Ends the task with a plain Persian reason."""
        show("task failed", reason=reply)
        return moved(state, "failed") | {"reply": masked(reply)}

    def understand(self, state: AgentState) -> dict:
        """Narrows the features with TEI, then lets the model choose one of them."""
        text = hidden(state["messages"])[-1]
        try:
            offered = self.tools.index.closest(text)
            chosen = choose(self.tools.chat, text, offered)
        except ModelError:
            return self.failed(state, replies.MODEL_FAILED)
        show("understand", offered=[item["intent"] for item in offered], chosen=chosen)
        update = {"offered": [item["intent"] for item in offered], "chosen": chosen, "reply": None}
        # A knowledge question or a "not sure" creates no task yet (plan §13).
        return update if chosen in (KNOWLEDGE, NOT_SURE) else update | moved(state, "resolving")

    def plan(self, state: AgentState) -> dict:
        """Plans the chosen feature from the app's current screen. A choice gets its allowed keys; a
        value the feature decides, or a choice with one key, is set by code and never asked."""
        feature = self.features[state["chosen"]]
        start = self.tools.screen()
        try:
            plan = plan_for(self.tools.navigator, start, feature)
        except (NoRoute, CrossesUserOnly):
            return self.failed(state, replies.NOT_POSSIBLE)
        found = plan.as_json()
        for item in found["requirements"]:
            if item["type"] == "choice":
                item["options"] = options(item["screen"], item["ref"], self.tools.lookups)
        preset = fixed(feature["intent"]) | {
            item["ref"]: item["options"][0]["key"]
            for item in found["requirements"]
            if len(item.get("options", [])) == 1
        }
        show("plan", start=start, target=plan.target, requirements=[item.ref for item in plan.requirements])
        return moved(state, "resolving") | {
            "plan": found | {"start": start, "intent": feature["intent"], "preset": sorted(preset)},
            "values": preset,
            "missing": [item["ref"] for item in found["requirements"] if item["ref"] not in preset],
        }

    def extract(self, state: AgentState) -> dict:
        """Asks the model for the missing values only, showing it nothing but what it may see: a
        choice's labels, never its keys."""
        offered = [
            {key: item[key] for key in MODEL_SEES}
            | {"description": field_words(item["ref"]) or item["description"]}
            | {"options": [o["label"] for o in item.get("options", [])]}
            for item in state["plan"]["requirements"]
            if item["ref"] in state["missing"]
        ]
        if not offered:
            return moved(state, "resolving")
        try:
            found = extract(self.tools.chat, state["messages"], offered)
        except ModelError:
            return self.failed(state, replies.MODEL_FAILED)
        show("extract", offered=[item["ref"] for item in offered], found=found)
        # A new value wins: after a typed answer the model re-reads every value, so a correction counts.
        return moved(state, "resolving") | {"values": state["values"] | found}

    def check(self, state: AgentState) -> dict:
        """Code only: keeps the values the plan asks for, in the app's form, and lists what is missing."""
        requirements = state["plan"]["requirements"]
        values = checked(state["values"], requirements, state["messages"])
        missing = [item["ref"] for item in requirements if item["ref"] not in values]
        show("check", values=values, missing=missing)
        return moved(state, "resolving") | {"values": values, "missing": missing}

    def clarify(self, state: AgentState) -> dict:
        """Asks once for everything missing, and waits for the user's text or an option's ref."""
        update = moved(state, "clarifying") | {"question": None, "options": []}
        wanted = [item for item in state["plan"]["requirements"] if item["ref"] in state["missing"]]
        options = state["options"] or replies.lookup_options(self.tools.lookups, wanted)
        text = replies.question(state["question"], [item["ask"] for item in wanted])
        answer = interrupt({"question": text, "options": options, "task_state": update["task_state"]})
        if answer in [item["ref"] for item in options]:
            ref, key = answer.split("=", 1)
            missing = [item for item in state["missing"] if item != ref]
            return update | {"values": state["values"] | {ref: key}, "missing": missing}
        # Typed words may answer the question or change any value given before: all are read again.
        again = [
            item["ref"]
            for item in state["plan"]["requirements"]
            if item["ref"] not in state["plan"]["preset"]
        ]
        return update | {"messages": state["messages"] + [answer], "missing": again}

    def resolve(self, state: AgentState) -> dict:
        """Code only: each named record must match exactly one record of its lookup."""
        values = dict(state["values"])
        for item in state["plan"]["requirements"]:
            said = values.get(item["ref"])
            if item["lookup"] is None or not isinstance(said, str):
                continue
            found = self.tools.lookups.matching(item["lookup"], said)
            if typed(item["lookup"], said) and (said.isdigit() or not found):
                continue  # a typed number or IBAN needs no saved record
            if len(found) == 1:
                values[item["ref"]] = found[0].key
                continue
            del values[item["ref"]]
            note = replies.WHICH if found else replies.NOT_FOUND
            show("resolve", ref=item["ref"], said=said, matches=len(found))
            return moved(state, "resolving") | {
                "values": values,
                "missing": [item["ref"]],
                "question": note.format(text=masked(said)),
                "options": [replies.option(item["ref"], record) for record in found],
            }
        return moved(state, "resolving") | {"values": values}

    def prepare(self, state: AgentState) -> dict:
        """Runs the task runner with the resolved values, as the graph page's Run does."""
        feature = self.features[state["plan"]["intent"]]
        plan = plan_for(self.tools.navigator, state["plan"]["start"], feature)
        # The runner reads money as text and turns it back into the same whole number of rials.
        values = {ref: str(v["value"]) if isinstance(v, dict) else v for ref, v in state["values"].items()}
        runner = TaskRunner(self.tools.bridge, self.tools.navigator, self.tools.lookups)
        report = runner.run(plan, values, feature["form"] is not None, TaskReport())
        return moved(state, "preparing") | {"report": report.as_json()}

    def verify(self, state: AgentState) -> dict:
        """Code only: the app must be on the feature's review, or its destination without a form."""
        feature = self.features[state["plan"]["intent"]]
        report = state["report"]
        if report["state"] == "stopped":
            return self.failed(state, replies.FAILED.format(reason=report["reason"]))
        expected = feature["review"] or feature["destination"]
        screen = report["steps"][-1]["screen"]
        if screen != expected:
            reason = replies.NOT_THERE.format(expected=expected, screen=screen)
            return self.failed(state, replies.FAILED.format(reason=reason))
        return moved(state, "verifying_review")

    def ready_for_user(self, state: AgentState) -> dict:
        """Tells the user what was prepared. Only the user checks and confirms it."""
        feature = self.features[state["plan"]["intent"]]
        if feature["form"] is None:
            reply = replies.PAGE_OPEN.format(title=feature["title"])
            return moved(state, "ready_for_user", "completed") | {"reply": reply}
        reply = replies.ready(state["report"], state["plan"]["requirements"], self.tools.lookups)
        return moved(state, "ready_for_user") | {"reply": reply}

    def reply(self, state: AgentState) -> dict:
        """A knowledge question gets the fixed message; "not sure" asks with the closest features."""
        if state["chosen"] == KNOWLEDGE:
            return {"reply": replies.KNOWLEDGE}
        options = [{"ref": intent, "label": self.features[intent]["title"]} for intent in state["offered"]]
        pending = {"question": replies.NOT_SURE, "options": options, "task_state": state["task_state"]}
        answer = interrupt(pending)
        if answer in state["offered"]:
            return {"chosen": answer}
        return {"chosen": None, "messages": state["messages"] + [answer]}
