"""The ReAct agent's tools: what the model is told each one does, the shape of its arguments, and the
code that runs it.

Called by react_graph's act node, which runs the tool the model named. A tool returns a short result
for the model, and the state keys it changes. The model sees Persian titles, questions and labels,
never a screen ID or a full card, account or IBAN number: every result is masked. The model handles
the conversation itself (D-036): it replies (guarded by CF-3's safe_chat), finds and starts a feature
(its request guarded by CF-2's rewrite guard, a switch from an open task confirmed by the user),
asks which feature is meant, and cancels. The read tools change nothing in the app. The act tools
run the workflow's own code: set_values its reader, check and resolve, move_app its advance,
open_review its prepare and verify, and a task's question is the workflow's, with its options. A
typed answer goes through the fast path first: a secret or a confirm request is refused and never
kept. The app is reached only through TaskRunner, so the only commands are navigate, fill_form and
show_review. No tool can confirm, submit or pay.
"""

import re

from langgraph.types import interrupt

from assistant import replies
from assistant.agent_lifecycle import Lifecycle
from assistant.agent_nodes import REFUSALS, Nodes
from assistant.agent_state import AgentState, moved, new_request
from assistant.conversation import kept, line, safe_chat
from assistant.intent import KNOWLEDGE
from assistant.lifecycle import CANCEL, FOLLOW_UP, NO_WORDS, YES_WORDS, fast
from assistant.lookups import GENERIC, plain
from assistant.models import ModelError
from assistant.navigator import HOME
from assistant.slots import extract, hidden, shape
from assistant.steps import masked
from assistant.words import feature_words, field_words, plates

TEXT = {"type": "string"}
TEXT_OR_NULL = {"anyOf": [{"type": "string"}, {"type": "null"}]}
OPEN = ("resolving", "ready_for_user", "paused")  # a task the model works on
FEATURES = 8  # the features find_features shows, as many as the workflow's chooser sees
ASK_WHICH = 4  # the most features ask_which offers (as the not-sure question, owner 18 Nov 2026)
SHOWN = re.compile(r"\*{4}(\d{4})")  # a long number as the model is shown it
NEXT_MISSING = "ask_user"
NEXT_READY = "open_review"
NEXT_ANSWER = "read it: set_values when it gives values; else reply, find_features or cancel_task"
NEXT_FEATURE = "start_task with the feature the request means, or ask_which when two or more could"
NEXT_PICKED = "start_task with the picked feature"
NEXT_ASK = "go on with the task: its question is asked after your reply"
NOT_KEPT = "a value not kept: use only the user's own words, or ask"
NOT_NAMED = "a record must be in the user's own words, not one you read; else ask"
# What the model is told each tool does, in the order it is shown.
TEXTS = {
    "reply": (
        "reply(text): your own short Persian reply to the user. It ends your turn; with the task's"
        " question open, the question is asked again after it."
    ),
    "find_features": (
        "find_features(request): the app's features closest to the request, written as one full Persian"
        " sentence in the user's own words."
    ),
    "start_task": (
        "start_task(feature, request, second): starts a feature find_features found, for the request"
        " sentence; second is a second task the same message asks for, else null. With another task"
        " open, code asks the user first."
    ),
    "ask_which": "ask_which(features): asks the user which of 2 to 4 found features they mean.",
    "cancel_task": "cancel_task(): cancels the open task, only when the user's newest words ask to stop it.",
    "find_records": (
        "find_records(ref, words): the records the app lists for that field that match the words"
        " (all of them when words is empty)."
    ),
    "current_screen": "current_screen(): the page the app is on, and the values filled there.",
    "set_values": (
        "set_values(values): sets the task's values, null for any you do not set. Copy the user's own"
        " words for a person, card, account or record, and a ****1234 number exactly as you see it."
        " Give a text without its field's label words («عنوان», «نشانی»)."
        ' Money is {"value": the number as the user said it, "unit": "IRT" for تومان or "IRR" for'
        " ریال}; هزار is 1000, میلیون is 1000000, no unit is IRT. Code checks every value and says what"
        " it kept, refused and still misses. At the review: only to change a value of this task, never"
        " for a new task."
    ),
    "move_app": "move_app(): takes the app to the page of the next missing value (ask_user does it too).",
    "open_review": "open_review(): fills the task in the app and opens its review, for the user to check.",
    "ask_user": (
        "ask_user(question): moves the app to the page of what is missing, then asks the user about it"
        " (code words the question)."
    ),
}


def named_by_user(value: str, messages: list[str]) -> bool:
    """True when the value names a record by words of the user's messages: a ****1234 number (or an
    IR****1234 IBAN) by its last digits, and the generic words («کارت», «حساب») aside. A value of
    generic words only names no record («حساب» alone), so it is asked, never matched."""
    said = plain(" ".join(messages))
    words = [word for word in plain(value).split() if word not in GENERIC]
    shown = [SHOWN.search(word) for word in words]
    named = all((found.group(1) if found else word) in said for word, found in zip(words, shown, strict=True))
    return bool(words) and named


def shown_again(value: object, messages: list[str]) -> object:
    """A bare last-4 of a long number the user wrote («3691» for a bill ID they typed in full), as
    the ****3691 the model was shown, so the workflow's check puts the whole number back. Four digits
    the user wrote on their own («از کارت 4544») stay as they are."""
    if not isinstance(value, str) or not re.fullmatch(r"\d{4}", plain(value)):
        return value
    said = plain(" ".join(messages))
    alone = re.search(rf"(?<!\d){plain(value)}(?!\d)", said)
    ends = any(number.endswith(plain(value)) for number in re.findall(r"\d{12,}", said))
    return value if alone or not ends else "****" + plain(value)


def read_since_words(steps: list[dict]) -> bool:
    """True when the user's newest words in this turn were read: the request the task was started
    from (start_task reads it), or a typed answer that set_values read after it. A correction after
    the review was read by the workflow's own reader already, when the task was opened again."""
    words = [
        index
        for index, step in enumerate(steps)
        if step["tool"] == "start_task" or "answer" in step.get("result", {})
    ]
    # start_task reads its request itself, so it is words and their reading at once.
    read = [
        index
        for index, step in enumerate(steps)
        if step["tool"] == "set_values" or "kept" in step.get("result", {})
    ]
    return not words or bool(read) and read[-1] >= words[-1]


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

    def __init__(self, nodes: Nodes, lifecycle: Lifecycle) -> None:
        self.nodes = nodes
        self.lifecycle = lifecycle
        self.lookups = nodes.tools.lookups

    def available(self, state: AgentState) -> dict[str, dict]:
        """The tools the model may call now, each with the schema of its arguments. While the app
        waits on a question's page, only ask_user (and current_screen). Else reply and find_features
        always, start_task and ask_which over the features found this turn, and with a task open,
        cancel_task and its own tools: move_app and ask_user while something is missing, open_review
        once nothing is."""
        if state["task_state"] == "preparing":
            return {"current_screen": arguments(), "ask_user": arguments(question=TEXT)}
        tools = {"reply": arguments(text=TEXT), "find_features": arguments(request=TEXT)}
        found = self.found(state)
        if found:
            tools["start_task"] = arguments(feature={"enum": found}, request=TEXT, second=TEXT_OR_NULL)
            listed = {"type": "array", "items": {"enum": found}, "minItems": 2, "maxItems": ASK_WHICH}
            tools["ask_which"] = arguments(features=listed)
        tools["current_screen"] = arguments()
        if not self.open(state):
            return tools
        if not self.just_started(state):
            tools["cancel_task"] = arguments()
        records = self.with_records(state)
        if records:
            tools["find_records"] = arguments(ref={"enum": records}, words=TEXT)
        settable = self.settable(state)
        if settable:
            values = {item["ref"]: {"anyOf": [shape(item), {"type": "null"}]} for item in settable}
            tools["set_values"] = arguments(values=arguments(**values))
        if not state["missing"]:
            return tools | {"open_review": arguments()}
        return tools | {"move_app": arguments(), "ask_user": arguments(question=TEXT)}

    def open(self, state: AgentState) -> bool:
        """True while a task is open: planned, and not ended."""
        return state["plan"] is not None and state["task_state"] in OPEN

    def just_started(self, state: AgentState) -> bool:
        """True when the open task was started in this turn after the user's last words: the user
        has not asked since to stop it, so it is not offered to be cancelled."""
        for step in reversed(state["steps"]):
            if step["tool"] == "start_task":
                return True
            if {"answer", "picked"} & step.get("result", {}).keys():
                return False
        return False

    def found(self, state: AgentState) -> list[str]:
        """The features the newest find_features of this turn found."""
        for step in reversed(state["steps"]):
            if step["tool"] == "find_features" and "features" in step.get("result", {}):
                return [item["feature"] for item in step["result"]["features"]]
        return []

    def settable(self, state: AgentState) -> list[dict]:
        """The fields the model may set, as slots.shape reads them: not one the feature decides, and
        never a saved plate, which only the user picks."""
        return [
            {
                "ref": item["ref"],
                "type": item["type"],
                "options": [o["label"] for o in item.get("options", [])],
            }
            for item in state["plan"]["requirements"]
            if item["ref"] not in state["plan"]["preset"] and item["ref"] not in plates()
        ]

    def with_records(self, state: AgentState) -> list[str]:
        """The planned fields whose value is a record the app lists."""
        return [item["ref"] for item in state["plan"]["requirements"] if item["lookup"]]

    def held(self, state: AgentState) -> dict:
        """The task's feature, the fields the model may set (ref, type, Persian question and words, a
        choice's labels, never its keys), the values as the user reads them, and what is missing."""
        items = {item["ref"]: item for item in state["plan"]["requirements"]}
        settable = {item["ref"] for item in self.settable(state)}
        fields = [
            {"ref": item["ref"], "type": item["type"], "question": item["ask"]}
            | {"about": field_words(item["ref"]) or item["description"] or ""}
            | ({"options": [o["label"] for o in item["options"]]} if item.get("options") else {})
            for item in state["plan"]["requirements"]
            if item["ref"] in settable
        ]
        shown = {
            ref: self.label(items[ref], value)
            for ref, value in state["values"].items()
            if ref in items and ref not in state["plan"]["preset"]
        }
        title = self.nodes.features[state["plan"]["intent"]]["title"]
        return {"feature": title, "fields": fields, "held": shown, "missing": state["missing"]}

    def label(self, item: dict, value: str | dict) -> str:
        """A value as the user reads it, masked: a record by its label, never its key."""
        if item["lookup"] and isinstance(value, str):
            for record in self.lookups.candidates(item["lookup"]):
                if value in (record.key, record.value):
                    return replies.option(item["ref"], record)["label"]
        return replies.shown(value, item, self.lookups)

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
                items[ref]["ask"]: self.label(items[ref], value)
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

    def set_values(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """The workflow's reading of the missing fields, and the model's new values for fields held
        before, through the workflow's check and resolve: what is kept, refused and still missing."""
        said = {
            ref: shown_again(value, state["messages"])
            for ref, value in given["values"].items()
            if value is not None
        }
        lookups = {item["ref"]: item["lookup"] for item in state["plan"]["requirements"]}
        # A record comes only from the user's words, never from a label the model read in find_records.
        unnamed = [
            ref
            for ref, value in said.items()
            if lookups.get(ref) and isinstance(value, str) and not named_by_user(value, state["messages"])
        ]
        said = {ref: value for ref, value in said.items() if ref not in unnamed}
        # A missing field is the workflow's reader's alone (tuned on the frozen set): what it finds
        # nothing for is asked. The model's value counts for a value held before (a correction).
        held = {ref: value for ref, value in said.items() if ref not in state["missing"]}
        update = self.settle(state | {"values": state["values"] | held | self.read(state, {})})
        task = state | update
        items = {item["ref"]: item for item in task["plan"]["requirements"]}
        kept = {ref: self.label(items[ref], value) for ref, value in task["values"].items() if ref in said}
        refused = unnamed + [ref for ref in said if ref not in task["values"]]
        note = task["question"] or (NOT_NAMED if unnamed else NOT_KEPT if refused else None)
        return {"kept": kept, "refused": refused, "note": note} | self.next_step(task), update

    def read(self, state: AgentState, said: dict) -> dict:
        """The workflow's own reading (slots.extract) of the missing fields, but those in said, so no
        value the user wrote is lost when the model misses it. A model error reads nothing."""
        offered = [
            {"ref": item["ref"], "type": item["type"], "ask": item["ask"]}
            | {"description": field_words(item["ref"]) or item["description"]}
            | {"options": [o["label"] for o in item.get("options", [])]}
            for item in state["plan"]["requirements"]
            if item["ref"] in state["missing"]
            and item["ref"] not in said
            and item["ref"] not in plates()
            and item["ref"] not in state["plan"]["preset"]
        ]
        try:
            return extract(self.nodes.tools.chat, state["messages"], offered) if offered else {}
        except ModelError:
            return {}

    def settle(self, state: AgentState) -> dict:
        """The workflow's check (values the plan asks for, in the app's form, amount limits, plates),
        then its resolve (each named record must be one the app lists). Its note and options are for
        the next question."""
        update = self.nodes.check(state | {"question": None, "options": []})
        task = state | update
        resolved = self.nodes.resolve(task)
        notes = [note for note in (task["question"], resolved.get("question")) if note]
        missing = set(task["missing"]) | set(resolved.get("missing", []))
        refs = [item["ref"] for item in task["plan"]["requirements"] if item["ref"] in missing]
        return (
            update
            | resolved
            | {
                "missing": refs,
                "question": "\n".join(notes) or None,
                "options": resolved.get("options", []),
            }
        )

    def next_step(self, task: AgentState) -> dict:
        """What is missing, and what the model should do next."""
        return {"missing": task["missing"], "next": NEXT_MISSING if task["missing"] else NEXT_READY}

    def unread(self, state: AgentState) -> dict:
        """The values after the workflow's reading of the user's newest words, when set_values did
        not read them, settled; so no word of the user is lost whatever the model calls."""
        if read_since_words(state["steps"][:-1]):
            return self.settle(state)
        return self.settle(state | {"values": state["values"] | self.read(state, {})})

    def move_app(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """The workflow's advance, after reading and settling: the app goes as far as the values
        allow and waits on the page of the first missing value, filling what is known. No review
        opens."""
        update = self.unread(state)
        if not update["missing"]:
            return self.next_step(state | update), update
        update |= self.nodes.advance(state | update)
        task = state | update
        if task["task_state"] != "preparing":
            return self.stopped(state, update)
        return {"page": self.page_title(self.nodes.tools.screen()), "asking": task["missing"]}, update

    def open_review(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """Once nothing is missing, after the workflow's check and resolve: its prepare runs the app to
        the review, its verify checks the app is there, and its reply lists what was prepared."""
        update = self.unread(state)
        task = state | update
        if task["missing"]:
            return {"error": "something is missing", "note": task["question"]} | self.next_step(task), update
        update |= self.nodes.prepare(task)
        update |= self.nodes.verify(state | update)
        if (state | update)["task_state"] != "verifying_review":
            return self.stopped(state, update)
        done = self.nodes.ready_for_user(state | update)
        if state["lead"]:  # the model's reply comes before what was prepared
            done["reply"] = state["lead"] + "\n" + done["reply"]
        return {"review": "open"}, update | done | {"lead": None}

    def stopped(self, state: AgentState, update: dict) -> tuple[dict, dict]:
        """A run that did not end where it should: an uncertain command is planned again once from
        where the app is (the workflow's reconcile); a failure, cancel or pause ends the turn."""
        task = state | update
        if task["task_state"] != "reconciling":
            return {"stopped": task["reply"]}, update
        update |= self.lifecycle.reconcile(task)
        task = state | update
        if task["task_state"] == "failed":
            return {"stopped": task["reply"]}, update
        return {"note": replies.UNCERTAIN} | self.next_step(task), update

    def ask_user(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """Asks and waits. The question and options are the workflow's, never the model's words: every
        missing value of the page the app waits on, its records or allowed choices, and «لغو», after
        the model's reply if it gave one. «لغو» or a cancel phrase cancels; a secret or a confirm
        request is refused and never kept; a picked option or a typed answer naming one listed record
        sets that value; other words are a new message for the model to read."""
        wanted = [item for item in state["plan"]["requirements"] if item["ref"] in state["missing"]]
        options = (state["options"] or replies.lookup_options(self.lookups, wanted)) + [CANCEL]
        asked = moved(state, "clarifying") | {"question": None, "options": [], "lead": None}
        question = replies.question(state["question"], [item["ask"] for item in wanted])
        shown = "\n".join(part for part in (state["lead"], question) if part)
        fields = [item["ref"] for item in wanted if item["kind"] == "field"]
        pending = {"question": shown, "options": options, "fields": fields, "task_state": asked["task_state"]}
        answer, kind, turns = self.asked(state, pending, question)
        task = state | asked
        refs = [option["ref"] for option in options]
        if kind in ("cancel", "only_looking"):
            reply = replies.CANCELLED_OPEN if state["report"] else replies.CANCELLED  # the app moved
            return {"answer": "cancel"}, asked | moved(task, "cancelled") | {"reply": reply, "turns": turns}
        update = asked | moved(task, "resolving") | {"turns": turns}
        picked = None
        if answer in refs:
            picked = tuple(answer.split("=", 1))
        elif len(wanted) == 1 and wanted[0]["lookup"]:
            record = self.nodes.record(wanted[0], answer)
            picked = (wanted[0]["ref"], record.key) if record else None
            update["messages"] = state["messages"] + [answer]
        else:
            update["messages"] = state["messages"] + [answer]
        if picked is None:
            return {"answer": hidden([answer])[0], "next": NEXT_ANSWER}, update
        ref, key = picked
        update |= {
            "values": state["values"] | {ref: key},
            "missing": [r for r in state["missing"] if r != ref],
        }
        item = next(item for item in wanted if item["ref"] == ref)
        return {"picked": {ref: self.label(item, key)}} | self.next_step(state | update), update

    def asked(self, state: AgentState, pending: dict, bare: str) -> tuple[str, str | None, list[dict]]:
        """Asks the question and waits. A typed secret or confirm request is refused at once and the
        question (bare, without a reply said before it) asked again with the refusal first, so the
        model never reads it. Returns the answer, the fast path's reading of it (None for a picked
        option or words for the model), and the history with the question and each typed answer, a
        secret replaced so no prompt ever has it."""
        refs = [option["ref"] for option in pending["options"]]
        turns, lead = state["turns"], None
        while True:
            question = "\n".join(part for part in (lead, pending["question"]) if part)
            answer = interrupt(pending | {"question": question})
            if answer in refs:
                return answer, ("cancel" if answer == CANCEL["ref"] else None), turns
            kind = fast(answer)
            said = replies.REMOVED if kind == "secret" else answer
            turns = kept(turns, line("assistant", masked(question)), line("user", said))
            if kind not in REFUSALS:
                return answer, kind, turns
            pending = pending | {"question": bare}
            lead = REFUSALS[kind]

    def reply(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """The model's own reply, guarded in code (CF-3's safe_chat: masked, and a fixed line instead
        of one with a number, an amount or a claim that something was done). It ends the turn, or,
        with a task going on, is said before the task's next question. A reply with no task open
        counts as the knowledge answer: nothing is started."""
        said = safe_chat(given["text"])
        if state["task_state"] == "resolving":
            return {"said": said, "next": NEXT_ASK}, {"lead": said}
        return {"said": said}, {"reply": said} | ({} if self.open(state) else {"chosen": KNOWLEDGE})

    def find_features(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """TEI's closest features, each with its name, title and what it is for: the user's newest
        words first, which TEI ranks best, then the closest to the model's request sentence (what a
        reference like «همونو» means). Nothing is started."""
        said = state["messages"][-1] if state["messages"] else given["request"]
        index = self.nodes.tools.index
        try:
            words, request = index.closest(hidden([said])[0]), index.closest(hidden([given["request"]])[0])
        except ModelError:
            return {"error": "the search failed; reply to the user"}, {}
        merged = {item["intent"]: item for item in words[:6] + request[:2] + words[6:] + request[2:]}
        closest = list(merged.values())[:FEATURES]
        features = [
            {"feature": item["intent"], "title": item["title"]}
            | {"about": feature_words(item["intent"]) or item["description"]}
            for item in closest
        ]
        return {"features": features, "next": NEXT_FEATURE}, {"offered": list(merged)[:FEATURES]}

    def start_task(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """Starts a found feature, planned from the app's current screen. Its request is the model's
        sentence once the rewrite guard keeps it (every amount, number and name the user's own), and
        only when the conversation has earlier words or a second task, as the workflow's standalone;
        else the user's own message. With another task open, the user is asked first. A feature with
        nothing for the model to set (a page to see) is opened at once."""
        conversation = self.nodes.conversation
        text = state["messages"][-1] if state["messages"] else given["request"]
        earlier = state["turns"][:-1]
        routed = {"standalone": given["request"], "rest": given["second"]}
        request = conversation.standalone(routed, earlier, text)
        rest = conversation.guarded(given["second"], earlier, text, second=True)
        planned = {}
        if self.open(state):
            result, update = self.switch(state, given["feature"])
            if result["switch"] != "yes":
                return result, update
            planned = dict(new_request(state["conversation_id"], request)) | update
        planned |= {"chosen": given["feature"], "rest": rest, "messages": [request], "said": None}
        planned |= self.nodes.plan(state | planned)
        task = state | planned
        if task["task_state"] != "resolving":
            return {"stopped": task["reply"]}, planned
        if not task["missing"] and not self.settable(task):
            result, opened = self.open_review(task, {})
            return result, planned | opened
        # The workflow's reader reads the request at once, so no step is spent on it.
        planned |= self.settle(task | {"values": task["values"] | self.read(task, {})})
        task = state | planned
        items = {item["ref"]: item for item in task["plan"]["requirements"]}
        kept = {
            ref: self.label(items[ref], value)
            for ref, value in task["values"].items()
            if ref in items and ref not in task["plan"]["preset"]
        }
        title = self.nodes.features[given["feature"]]["title"]
        result = {"started": title, "kept": kept, "note": task["question"]} | self.next_step(task)
        return result, planned

    def switch(self, state: AgentState, feature: str) -> tuple[dict, dict]:
        """The switch question while a task is open: «بله» leaves it for the new one, from where the
        app is; «نه» keeps it (its review stays, or its question comes next); «لغو» cancels it. Other
        words are for the model to read."""
        old = self.nodes.features[state["plan"]["intent"]]["title"]
        question = replies.SWITCH.format(old=old, new=self.nodes.features[feature]["title"])
        options = [replies.SWITCH_YES, replies.SWITCH_NO, CANCEL]
        pending = {"question": question, "options": options, "fields": [], "task_state": state["task_state"]}
        answer, kind, turns = self.asked(state, pending, question)
        words = plain(answer).strip(" .!؟?،,")
        if kind in ("cancel", "only_looking"):
            return {"switch": "cancel"}, self.cancelled(state) | {"turns": turns}
        if answer == replies.SWITCH_YES["ref"] or words in YES_WORDS:
            self.cancelled(state)  # the open task may be left: refused here if task.json did not allow it
            return {"switch": "yes"}, {"turns": turns}
        if answer == replies.SWITCH_NO["ref"] or words in NO_WORDS:
            if state["task_state"] in FOLLOW_UP:
                return {"switch": "no"}, {"turns": turns, "reply": replies.GOING_ON.format(title=old)}
            return {"switch": "no", "next": "go on with the open task"}, {"turns": turns}
        return {"switch": "unclear", "answer": hidden([answer])[0], "next": NEXT_ANSWER}, {"turns": turns}

    def ask_which(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """Asks which found feature the user means, with «لغو». A pick goes back to the model, «لغو»
        cancels (the open task too), and typed words are a new message for the model to read."""
        shown = list(dict.fromkeys(given["features"]))[:ASK_WHICH]
        options = [{"ref": intent, "label": self.nodes.features[intent]["title"]} for intent in shown]
        question = "\n".join(part for part in (state["lead"], replies.NOT_SURE) if part)
        pending = {"question": question, "options": options + [CANCEL], "fields": []}
        answer, kind, turns = self.asked(
            state, pending | {"task_state": state["task_state"]}, replies.NOT_SURE
        )
        update = {"turns": turns, "lead": None}
        if kind in ("cancel", "only_looking"):
            return {"answer": "cancel"}, update | self.cancelled(state)
        if answer in shown:
            return {"picked": self.nodes.features[answer]["title"], "next": NEXT_PICKED}, update
        update["messages"] = state["messages"] + [answer]
        return {"answer": hidden([answer])[0], "next": NEXT_ANSWER}, update

    def cancel_task(self, state: AgentState, given: dict) -> tuple[dict, dict]:
        """Cancels the open task; the app stays as it is."""
        return {"cancelled": True}, self.cancelled(state)

    def cancelled(self, state: AgentState) -> dict:
        """The task cancelled from where it is (a request not started yet too), and the line that
        says so."""
        running = state["task_state"] in ("resolving", "preparing")
        path = ("cancelling", "cancelled") if running else ("cancelled",)
        reply = replies.CANCELLED_OPEN if state["report"] else replies.CANCELLED
        return moved(state, *path) | {"reply": reply}

    def reopened(self, state: AgentState) -> dict:
        """A task at its review or paused, opened again before an act: a paused task planned again
        from where the app is; a task at its review planned again with the newest message read on
        its own by the workflow's reader, so a correction replaces the old value (the workflow's
        corrected)."""
        if state["task_state"] == "paused":
            return self.lifecycle.again(state, ("reconciling", "resolving"), reread=False)
        return self.lifecycle.corrected(state, state["messages"][-1])
