"""The ReAct agent's tools: what the model is told each one does, the shape of its arguments, and the
code that runs it.

Called by react_graph's act node, which runs the tool the model named. A tool returns a short result
for the model, and the state keys it changes. The model sees Persian titles, questions and labels,
never an app key, a screen ID or a full card, account or IBAN number: every result is masked. The
read tools change nothing in the app. No tool can confirm, submit or pay.
"""

from langgraph.types import interrupt

from assistant import replies
from assistant.agent_nodes import Nodes
from assistant.agent_state import AgentState, moved
from assistant.lifecycle import CANCEL, wants_cancel
from assistant.models import ModelError
from assistant.navigator import HOME, CrossesUserOnly, NoRoute
from assistant.slots import hidden
from assistant.steps import masked
from assistant.words import feature_words, field_words

TEXT = {"type": "string"}
NEXT_AFTER_PLAN = "find_records for each field whose record the user named; then ask_user for what is missing"
# What the model is told each tool does, in the order it is shown.
TEXTS = {
    "find_features": "find_features(text): the app's features closest to the text: intent, title, words.",
    "feature_requirements": (
        "feature_requirements(intent): plans that feature from the app's current page and lists the"
        " values it needs: each field's ref, type, question and options, and what is held or missing."
    ),
    "find_records": (
        "find_records(ref, words): the records the app lists for that field that match the words"
        " (all of them when words is empty)."
    ),
    "current_screen": "current_screen(): the page the app is on, and the values filled there.",
    "ask_user": "ask_user(question): asks the user, in Persian, and waits for the answer.",
    "finish": "finish(message): says the message to the user, in Persian, and ends the turn.",
}


def arguments(**properties: dict) -> dict:
    """An arguments schema where every property is required and nothing else is allowed."""
    return {
        "type": "object",
        "properties": properties,
        "required": list(properties),
        "additionalProperties": False,
    }


class ReactTools:
    """The tools, over the workflow's nodes, so every act goes through the workflow's own code."""

    def __init__(self, nodes: Nodes) -> None:
        self.nodes = nodes
        self.lookups = nodes.tools.lookups

    def available(self, state: AgentState) -> dict[str, dict]:
        """The tools the model may call in this state, each with the schema of its arguments."""
        tools = {
            "find_features": arguments(text=TEXT),
            "feature_requirements": arguments(intent={"enum": sorted(self.nodes.features)}),
        }
        records = self.with_records(state)
        if records:
            tools["find_records"] = arguments(ref={"enum": records}, words=TEXT)
        tools |= {
            "current_screen": arguments(),
            "ask_user": arguments(question=TEXT),
            "finish": arguments(message=TEXT),
        }
        return tools

    def with_records(self, state: AgentState) -> list[str]:
        """The planned fields whose value is a record the app lists."""
        plan = state["plan"] or {"requirements": []}
        return [item["ref"] for item in plan["requirements"] if item["lookup"]]

    def find_features(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """The closest features by TEI, as the workflow's understand narrows them."""
        text = hidden([given["text"].strip() or state["messages"][-1]])[0]
        try:
            found = self.nodes.tools.index.closest(text)
        except ModelError:
            return {"error": "the feature search is not reachable"}, {}
        features = [
            {"intent": item["intent"], "title": item["title"], "words": feature_words(item["intent"])}
            for item in found
        ]
        return {"features": features}, {"offered": [item["intent"] for item in found]}

    def feature_requirements(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """Plans the feature from the app's current screen with the workflow's own planning, keeping
        the values held for it. Values the feature decides itself are set and not shown."""
        intent = given["intent"]
        held = state["values"] if state["plan"] and state["plan"]["intent"] == intent else {}
        try:
            planned = self.nodes.planned(self.nodes.features[intent], held)
        except (NoRoute, CrossesUserOnly):
            return {"error": replies.NOT_POSSIBLE}, {}
        update = moved(state, "resolving") | planned | {"chosen": intent, "question": None, "options": []}
        task = state | update
        fields = [
            {"ref": item["ref"], "type": item["type"], "question": item["ask"]}
            | {"about": field_words(item["ref"]) or item["description"] or ""}
            | ({"options": [o["label"] for o in item["options"]]} if item.get("options") else {})
            for item in task["plan"]["requirements"]
            if item["ref"] not in task["plan"]["preset"]
        ]
        return self.held(task) | {"fields": fields, "next": NEXT_AFTER_PLAN}, update

    def held(self, state: AgentState) -> dict:
        """The task's feature, its values as the user reads them, and what is still missing."""
        items = {item["ref"]: item for item in state["plan"]["requirements"]}
        shown = {
            ref: replies.shown(value, items[ref], self.lookups)
            for ref, value in state["values"].items()
            if ref in items and ref not in state["plan"]["preset"]
        }
        title = self.nodes.features[state["plan"]["intent"]]["title"]
        return {"feature": title, "held": shown, "missing": state["missing"]}

    def find_records(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """The records the app lists for the field that the words match, as masked labels. The model
        only learns what is there: a record reaches the app only from the user's words or pick."""
        item = next(item for item in state["plan"]["requirements"] if item["ref"] == given["ref"])
        listed = replies.offered(self.lookups, item)
        words = given["words"].strip()
        if words:
            keys = {record.key for record in self.lookups.matching(item["lookup"], words)}
            listed = [record for record in listed if record.key in keys]
        labels = [replies.option(item["ref"], record)["label"] for record in listed[: replies.MANY]]
        return {"records": labels, "count": len(listed)}, {}

    def current_screen(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """The page the app is on by its Persian title, and the values given on it."""
        screen = self.nodes.tools.screen()
        found = {"page": self.page_title(screen)}
        if state["plan"]:
            items = {item["ref"]: item for item in state["plan"]["requirements"]}
            found["filled"] = {
                items[ref]["ask"]: replies.shown(value, items[ref], self.lookups)
                for ref, value in state["given"].items()
                if ref in items and items[ref]["screen"] == screen
            }
        return found, {}

    def page_title(self, screen: str) -> str:
        """A screen as the user knows it: Home, a feature's page or its review, never its ID."""
        if screen == HOME:
            return "خانه"
        for feature in self.nodes.features.values():
            if screen == feature["review"]:
                return f"بازبینی «{feature['title']}»"
            if screen in (feature["form"], feature["destination"]):
                return f"«{feature['title']}»"
        return "صفحه‌ای دیگر از برنامه"

    def ask_user(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """Asks the model's question with «لغو», and waits. «لغو» cancels; words are a new message.
        A task is `clarifying` while it waits, and goes on `resolving`."""
        question = masked(given["question"].strip())
        if not question:
            return {"error": "the question is empty"}, {}
        asked = moved(state, "clarifying") if state["plan"] else {}
        task = state | asked
        answer = interrupt(
            {"question": question, "options": [CANCEL], "fields": [], "task_state": task["task_state"]}
        )
        if wants_cancel(answer):
            return {"answer": "cancel"}, moved(task, "cancelled") | {"reply": replies.CANCELLED}
        back = moved(task, "resolving") if state["plan"] else {}
        return {"answer": hidden([answer])[0]}, back | {"messages": state["messages"] + [answer]}

    def finish(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """Ends the turn with the model's message."""
        message = masked(given["message"].strip())
        if not message:
            return {"error": "the message is empty"}, {}
        return {}, {"reply": message}
