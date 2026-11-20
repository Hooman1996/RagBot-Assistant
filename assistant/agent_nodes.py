"""The agent's nodes: each one plain method that takes the state and returns only what it changes.

Wired into one graph by agent_graph.py. The model only returns JSON, through intent.py and
slots.py; code plans, resolves, runs and checks. Before a question, the app is moved to the screen
the question is about. The app is reached only through TaskRunner, so the only commands are
navigate, fill_form and show_review. A question's typed answer is read in the conversation first
(D-034, door B): the fast path, then the turn router; an answer goes on as before, and anything else
leaves the question with where to go next. Nothing here can confirm, submit or pay.
"""

from collections.abc import Callable
from copy import deepcopy
from dataclasses import dataclass

from langgraph.types import interrupt

from assistant import replies
from assistant.agent_state import AgentState, moved
from assistant.conversation import Conversation, kept, line
from assistant.intent import KNOWLEDGE, NOT_SURE, FeatureIndex, choose
from assistant.lifecycle import AFTER_STOP, CANCEL, StoppableBridge, fast, wants_cancel
from assistant.lookups import Candidate, FixtureLookups, plain, source_of, typed
from assistant.models import Chat, ModelError
from assistant.navigator import CrossesUserOnly, Navigator, NoRoute, Plan
from assistant.slots import checked, extract, hidden
from assistant.steps import masked, show
from assistant.task_runner import Bridge, TaskReport, TaskRunner
from assistant.words import field_words, fixed, installment_of, limits, options, plates, when

MODEL_SEES = ("ref", "type", "ask", "description")
NOT_SURE_OPTIONS = 4  # the closest features a not-sure question offers (owner, 19 Nov 2026)
REFUSALS = {"secret": replies.NO_SECRETS, "confirm": replies.CONFIRM_BY_TOUCH}


@dataclass
class Tools:
    """What the nodes work with. Only the bridge and the screen differ between server and debug run."""

    chat: Chat
    index: FeatureIndex
    navigator: Navigator
    lookups: FixtureLookups
    bridge: Bridge
    screen: Callable[[], str]  # where the app is now
    stopped: Callable[[], bool] = lambda: False  # true once the user cancelled a running task


def plan_for(navigator: Navigator, start: str, feature: dict, values: dict | None = None) -> Plan:
    """The feature's plan: through its form when it has one, as the graph page plans it, and the
    way and fields the app takes for the values chosen so far."""
    if feature["form"] is None:
        return navigator.plan(start, feature["destination"])
    through, fields = when(values or {})
    return navigator.plan_feature(start, feature, through, frozenset(fields))


class Nodes:
    """The graph's nodes, over one set of tools."""

    def __init__(self, tools: Tools) -> None:
        self.tools = tools
        self.features = {item["intent"]: item for item in tools.navigator.features}
        self.conversation = Conversation(tools.chat, tools.lookups)

    def detect(self, text: str) -> dict:
        """The feature a request means, as understand finds it: TEI's closest, then the chooser."""
        offered = self.tools.index.closest(hidden([text])[0])
        chosen = choose(self.tools.chat, hidden([text])[0], offered)
        return {"text": text, "offered": [item["intent"] for item in offered], "chosen": chosen}

    def names_a_feature(self, text: str) -> bool:
        """True when the chooser finds a feature in words the router read as a cancel or, with no task
        open, as chat: such words ask for a task. A model error keeps the router's reading."""
        try:
            return self.detect(text)["chosen"] in self.features
        except ModelError:
            return False

    def heard(self, state: AgentState, asked: str, answer: str) -> tuple[str | None, list[dict]]:
        """A typed answer to a question: its fast-path reading, and the history with the question and
        the answer, a secret replaced so no prompt ever has it."""
        kind = fast(answer)
        said = replies.REMOVED if kind == "secret" else answer
        return kind, kept(state["turns"], line("assistant", masked(asked)), line("user", said))

    def elsewhere(
        self, state: AgentState, turns: list[dict], answer: str, pending: str, here: str
    ) -> dict | None:
        """Where a typed answer that is not an answer goes, as the router reads it, or None for an
        answer: chat gets its reply before the question again; a new task, the switch question;
        a cancel, the cancel. here is the waiting node, to ask again."""
        routed = self.conversation.route(
            turns[:-1], self.conversation.summary(state, self.features), pending, answer
        )
        kind = routed["kind"]
        if kind in ("cancel", "only_looking") and self.names_a_feature(answer):
            kind = "new_task"  # «ولش، کارت به کارت میخوام» asks for another task
        if kind in ("cancel", "only_looking"):
            return {"route": "cancel"}
        if kind == "chat":
            return {"route": here, "lead": replies.KNOWLEDGE}
        if kind != "new_task" or here != "clarify":
            return None
        try:
            found = self.detect(self.conversation.standalone(routed, turns[:-1], answer))
        except ModelError:
            return {"route": here, "lead": replies.MODEL_FAILED}
        if found["chosen"] not in self.features and found["chosen"] != NOT_SURE:
            return {"route": here, "lead": replies.KNOWLEDGE}
        rest = self.conversation.guarded(routed["rest"], turns[:-1], answer, second=True)
        return {"route": "switch", "switch_to": found | {"rest": rest}}

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
        """Plans the chosen feature from the app's current screen."""
        try:
            return moved(state, "resolving") | self.planned(self.features[state["chosen"]], {})
        except (NoRoute, CrossesUserOnly):
            return self.failed(state, replies.NOT_POSSIBLE)

    def planned(self, feature: dict, held: dict, start: str | None = None) -> dict:
        """The plan from start, by default the app's current screen, keeping held values the route
        still asks for. A choice gets its allowed keys; a value the feature decides, or a choice with
        one key, is set by code and never asked. Raises NoRoute or CrossesUserOnly."""
        start = start or self.tools.screen()
        plan = plan_for(self.tools.navigator, start, feature, held)
        found = plan.as_json()
        through, fields = when(held)
        for item in found["requirements"]:
            if item["type"] == "choice":
                item["options"] = fields.get(item["ref"]) or options(
                    item["screen"], item["ref"], self.tools.lookups
                )
        preset = fixed(feature["intent"]) | {
            item["ref"]: item["options"][0]["key"]
            for item in found["requirements"]
            if len(item.get("options", [])) == 1
        }
        show("plan", start=start, target=plan.target, requirements=[item.ref for item in plan.requirements])
        refs = [item["ref"] for item in found["requirements"]]
        return {
            "plan": found
            | {
                "start": start,
                "intent": feature["intent"],
                "preset": sorted(preset),
                "when": [through, sorted(fields)],
            },
            "values": {ref: value for ref, value in held.items() if ref in refs} | preset,
            "missing": [ref for ref in refs if ref not in preset],
        }

    def extract(self, state: AgentState) -> dict:
        """Asks the model for the missing values only, showing it nothing but what it may see: a
        choice's labels, never its keys. A saved plate is the user's pick alone, never the model's."""
        offered = [
            {key: item[key] for key in MODEL_SEES}
            | {"description": field_words(item["ref"]) or item["description"]}
            | {"options": [o["label"] for o in item.get("options", [])]}
            for item in state["plan"]["requirements"]
            if item["ref"] in state["missing"] and item["ref"] not in plates()
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
        plan = state["plan"]
        values = checked(state["values"], plan["requirements"], state["messages"])
        values |= self.named(plan, values, state["messages"])
        through, fields = when(values)
        if [through, sorted(fields)] != plan["when"]:
            # A chosen value sends the app another way or makes it ask more (a weekly day): plan again.
            replanned = self.planned(self.features[plan["intent"]], values, plan["start"])
            plan, values = replanned["plan"], replanned["values"]
        plan, values, note = self.payable(plan, values, state["messages"])
        note = self.amounts(plan, values) or note
        values |= self.plate(plan, values)
        missing = [item["ref"] for item in plan["requirements"] if item["ref"] not in values]
        for ref, parts in plates().items():
            if ref in missing:  # the saved plates are offered first; a new plate's parts after
                missing = [item for item in missing if item not in parts]
        show("check", values=values, missing=missing)
        update = moved(state, "resolving") | {"plan": plan, "values": values, "missing": missing}
        return update | ({"question": note, "options": []} if note else {})

    def plate(self, plan: dict, values: dict) -> dict:
        """Code only: a saved plate fills the plate's parts with its own, as the app does when it is
        tapped; a plate the user typed, in whole or in part, is a new one, so a correction counts."""
        refs = {item["ref"] for item in plan["requirements"]}
        records = {row["id"]: row["plate"] for row in self.tools.lookups.records("vehicles")}
        found = {}
        for ref, parts in plates().items():
            chosen = values.get(ref)
            if ref not in refs or chosen == "new":
                continue
            saved = dict(zip(parts, records[chosen], strict=True)) if chosen else {}
            if any(part in values and values[part] != saved.get(part) for part in parts):
                found[ref] = "new"
            else:
                found |= saved
        if found:
            show("plate", found=found)
        return found

    def named(self, plan: dict, values: dict, messages: list[str]) -> dict:
        """Code only: a record field the model left empty, when the user's words name exactly one
        record the app lists for it by its label, and no other field takes records of that lookup."""
        refs = [item for item in plan["requirements"] if item["type"] == "ref" and item["lookup"]]
        found = {}
        for item in refs:
            lookup = source_of(item["lookup"])
            if item["ref"] in values or lookup == "contacts":
                continue
            if sum(source_of(other["lookup"]) == lookup for other in refs) > 1:
                continue  # a source and a destination of the same kind: only the model may tell them apart
            records = self.tools.lookups.named(" ".join(messages), replies.offered(self.tools.lookups, item))
            if len(records) == 1:
                found[item["ref"]] = records[0].key
        if found:
            show("named", found=found)
        return found

    def payable(self, plan: dict, values: dict, messages: list[str]) -> tuple[dict, dict, str | None]:
        """Code only: an installment choice keeps the installments the app lets the user pick for
        the named policy or loan. The only one is set; one the app greys out is dropped, and named
        when the user wrote it."""
        plan, values, note = deepcopy(plan), dict(values), None
        items = {item["ref"]: item for item in plan["requirements"]}
        for item in plan["requirements"]:
            parent = items.get(installment_of(item["ref"]) or "")
            said = values.get(parent["ref"]) if parent else None
            if not isinstance(said, str):
                continue
            found = self.record(parent, said)
            if found is None:
                continue  # resolve asks which record
            keys = self.tools.lookups.installments(parent["lookup"], found.key)
            chosen = values.get(item["ref"])
            named = False
            if chosen is not None and chosen not in keys:
                label = next(option["label"] for option in item["options"] if option["key"] == chosen)
                # Named only when the user wrote it; else it was the model's reading («قسط بعدی»).
                named = plain(label) in plain(" ".join(messages))
                note = replies.NOT_OFFERED.format(text=label) if named else note
                del values[item["ref"]]
            item["options"] = [option for option in item["options"] if option["key"] in keys]
            if not named and item["ref"] not in values and len(item["options"]) == 1:
                values[item["ref"]] = item["options"][0]["key"]
        return plan, values, note

    def record(self, item: dict, said: object) -> Candidate | None:
        """The one record the app lists for this field that the user's words name, or None."""
        if not isinstance(said, str):
            return None
        listed = {record.key for record in replies.offered(self.tools.lookups, item)}
        found = [
            record for record in self.tools.lookups.matching(item["lookup"], said) if record.key in listed
        ]
        return found[0] if len(found) == 1 else None

    def amounts(self, plan: dict, values: dict) -> str | None:
        """Code only: an amount the app would refuse is dropped, and the reason is said."""
        items = {item["ref"]: item for item in plan["requirements"]}
        for ref, rule in limits().items():
            amount = values.get(ref)
            problem = self.refused(rule, amount["value"], values, items) if isinstance(amount, dict) else None
            if problem:
                del values[ref]
                return problem
        return None

    def refused(self, rule: dict, amount: int, values: dict, items: dict) -> str | None:
        """Why the app would refuse this amount, or None: a range, a wallet top-up's charge list, a
        top-up's operator limits, or the source's balance."""
        lookups = self.tools.lookups
        if "range" in rule and not rule["range"][0] <= amount <= rule["range"][1]:
            return replies.OUT_OF_RANGE.format(low=f"{rule['range'][0]:,}", high=f"{rule['range'][1]:,}")
        charges = lookups.operator(name=str(values.get(rule.get("charges_of"), "")))
        if charges and amount not in charges["defaults"]:
            return replies.NOT_A_CHARGE.format(
                amounts="، ".join(f"{value:,}" for value in charges["defaults"])
            )
        limit = lookups.operator(phone=str(values.get(rule.get("operator_by"), "")))
        if limit and not (limit["min"] <= amount <= limit["max"] and amount % limit["multiplier"] == 0):
            return replies.TOP_UP_LIMITS.format(
                low=f"{limit['min']:,}", high=f"{limit['max']:,}", step=f"{limit['multiplier']:,}"
            )
        source = items.get(rule.get("balance_of"))
        found = self.record(source, values.get(source["ref"])) if source else None
        balance = lookups.balance(source["lookup"], found.key) if found else None
        if balance is not None and amount > balance:
            return replies.OVER_BALANCE.format(balance=f"{balance:,}")
        return None

    def clarify(self, state: AgentState) -> dict:
        """Asks once for everything missing, and waits for the user's text or an option's ref. The
        question names its form fields, so the app can bring the first one into view. A typed answer
        is read in the conversation first (door B): a refusal, chat or a new task leave the
        question with where to go, and its note and options stay for asking it again."""
        asking = moved(state, "clarifying") | {"lead": None, "route": None}
        wanted = [item for item in state["plan"]["requirements"] if item["ref"] in state["missing"]]
        options = (state["options"] or replies.lookup_options(self.tools.lookups, wanted)) + [CANCEL]
        text = replies.question(state["question"], [item["ask"] for item in wanted])
        shown = "\n".join(part for part in (state["lead"], text) if part)
        fields = [item["ref"] for item in wanted if item["kind"] == "field"]
        answer = interrupt(
            {"question": shown, "options": options, "fields": fields, "task_state": asking["task_state"]}
        )
        update = asking | {"question": None, "options": []}
        if answer in [item["ref"] for item in options] and not wants_cancel(answer):
            ref, key = answer.split("=", 1)
            missing = [item for item in state["missing"] if item != ref]
            return update | {"values": state["values"] | {ref: key}, "missing": missing}
        kind, turns = (
            self.heard(state, shown, answer) if answer != CANCEL["ref"] else ("cancel", state["turns"])
        )
        if kind in REFUSALS:
            return asking | {"turns": turns, "route": "clarify", "lead": REFUSALS[kind]}
        pending = f"«{text}» Options: {' | '.join(item['label'] for item in options)}"
        away = None if kind else self.elsewhere(state, turns, answer, pending, "clarify")
        if kind in ("cancel", "only_looking") or (away and away["route"] == "cancel"):
            reply = replies.CANCELLED_OPEN if state["report"] else replies.CANCELLED  # the app moved
            return update | moved(update, "cancelled") | {"reply": reply, "turns": turns}
        if away:
            return asking | {"turns": turns} | away
        update |= {"turns": turns}
        record = self.record(wanted[0], answer) if len(wanted) == 1 and wanted[0]["lookup"] else None
        if record:
            # A typed answer to one record question that names exactly one listed record is that
            # record, as an option would be, so a name the model kept from before can't win.
            missing = [item for item in state["missing"] if item != wanted[0]["ref"]]
            values = state["values"] | {wanted[0]["ref"]: record.key}
            return update | {"messages": state["messages"] + [answer], "values": values, "missing": missing}
        # Typed words may answer the question or change any value given before: all are read again.
        again = [
            item["ref"]
            for item in state["plan"]["requirements"]
            if item["ref"] not in state["plan"]["preset"]
        ]
        return update | {"messages": state["messages"] + [answer], "missing": again}

    def resolve(self, state: AgentState) -> dict:
        """Code only: each named record must match exactly one record the app lists for the field."""
        values = dict(state["values"])
        for item in state["plan"]["requirements"]:
            said = values.get(item["ref"])
            if item["lookup"] is None or not isinstance(said, str):
                continue
            found = self.tools.lookups.matching(item["lookup"], said)
            if typed(item["lookup"], said) and (said.isdigit() or not found):
                continue  # a typed number or IBAN needs no saved record
            listed = {record.key for record in replies.offered(self.tools.lookups, item)}
            usable = [record for record in found if record.key in listed]
            if len(usable) == 1:
                values[item["ref"]] = usable[0].key
                continue
            del values[item["ref"]]
            # A record the app does not list here (a credit card as a transfer source) is not used:
            # the user picks from the records the app lists.
            note = replies.WHICH if usable else replies.NOT_OFFERED if found else replies.NOT_FOUND
            show("resolve", ref=item["ref"], said=said, matches=len(found), usable=len(usable))
            return moved(state, "resolving") | {
                "values": values,
                "missing": [item["ref"]],
                "question": note.format(text=masked(said)),
                "options": [replies.option(item["ref"], record) for record in usable],
            }
        return moved(state, "resolving") | {"values": values}

    def advance(self, state: AgentState) -> dict:
        """Before a question: moves the app as far as the values allow, to the screen where the answer
        is needed, so the user sees what the question is about, and asks only that screen's values.
        It fills what is known there and opens no review. A cancel, a pause or an uncertain command
        ends it as it ends a run."""
        values = self.usable(state)
        update = moved(state, "preparing") | self.run(state, values, final=False)
        if update["report"]["state"] != "waiting":
            return update | self.verify(state | update)
        # Only what this screen needs is asked; a later screen's fields are asked there.
        here = [ref for ref in state["missing"] if ref in update["report"]["waiting_for"]]
        return update | {"missing": here or state["missing"]}

    def usable(self, state: AgentState) -> dict:
        """The values the app may take before every answer is in: a record once it is one the app
        lists for the field, or the user's words name exactly one, or a typed number. A record not
        settled yet is left out, so the app stops at its field and it is asked."""
        found = {}
        for item in state["plan"]["requirements"]:
            said = state["values"].get(item["ref"])
            if item["lookup"] and isinstance(said, str):
                listed = {record.key for record in replies.offered(self.tools.lookups, item)}
                record = self.record(item, said)
                said = (
                    said
                    if said in listed
                    else record.key
                    if record
                    else said
                    if typed(item["lookup"], said)
                    else None
                )
            if said is not None:
                found[item["ref"]] = said
        return found

    def prepare(self, state: AgentState) -> dict:
        """Runs the task runner with the resolved values, as the graph page's Run does."""
        return moved(state, "preparing") | self.run(state, state["values"], final=True)

    def run(self, state: AgentState, values: dict, final: bool) -> dict:
        """Runs from the app's current screen: to the review when final, else only as far as the values
        allow. From the task's start instead when a value the app was given on a screen it has passed
        has changed, so that screen is filled again. Steps taken before a question are kept."""
        feature = self.features[state["plan"]["intent"]]
        plan = plan_for(self.tools.navigator, self.tools.screen(), feature, values)
        refs = {item.ref for item in plan.requirements}
        if any(values.get(ref) != value for ref, value in state["given"].items() if ref not in refs):
            plan = plan_for(self.tools.navigator, state["plan"]["start"], feature, values)
            refs = {item.ref for item in plan.requirements}
        # The runner reads money as text and turns it back into the same whole number of rials.
        sent = {
            ref: str(v["value"]) if isinstance(v, dict) else v for ref, v in values.items() if ref in refs
        }
        bridge = StoppableBridge(self.tools.bridge, self.tools.stopped)  # a cancel stops between commands
        runner = TaskRunner(bridge, self.tools.navigator, self.tools.lookups)
        waiting = state["report"] is not None and state["report"]["state"] == "waiting"
        report = TaskReport(steps=list(state["report"]["steps"]) if waiting else [])
        if final:
            runner.run(plan, sent, feature["form"] is not None, report)
        else:
            runner.advance(plan, sent, report)
        return {"report": report.as_json(), "given": state["given"] | {ref: values[ref] for ref in sent}}

    def verify(self, state: AgentState) -> dict:
        """Code only: the app must be on the feature's review, or its destination without a form. A
        cancel ends the task, an app out of reach pauses it, an uncertain command is reconciled."""
        feature = self.features[state["plan"]["intent"]]
        report = state["report"]
        if report["state"] in AFTER_STOP:
            return moved(state, *AFTER_STOP[report["state"]]) | {"reply": replies.STOPPED[report["state"]]}
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
        if state.get("rest"):
            reply += "\n" + replies.SECOND_TASK.format(rest=masked(state["rest"]))
        return moved(state, "ready_for_user") | {"reply": reply}

    def reply(self, state: AgentState) -> dict:
        """A knowledge question gets the fixed message; "not sure" asks with the 4 closest features.
        A typed answer is read in the conversation first (door B): a refusal or chat ask again, a
        cancel cancels, and other words are understood again as the request."""
        if state["chosen"] == KNOWLEDGE:
            return {"reply": replies.KNOWLEDGE}
        shown = state["offered"][:NOT_SURE_OPTIONS]
        options = [{"ref": intent, "label": self.features[intent]["title"]} for intent in shown]
        question = "\n".join(part for part in (state["lead"], replies.NOT_SURE) if part)
        pending = {"question": question, "options": options + [CANCEL], "task_state": state["task_state"]}
        answer = interrupt(pending)
        if answer in shown:
            return {"chosen": answer, "lead": None, "route": None}
        kind, turns = (
            self.heard(state, question, answer) if answer != CANCEL["ref"] else ("cancel", state["turns"])
        )
        if kind in REFUSALS:
            return {"turns": turns, "route": "reply", "lead": REFUSALS[kind]}
        labels = " | ".join(item["label"] for item in options)
        away = (
            None
            if kind
            else self.elsewhere(state, turns, answer, f"«{replies.NOT_SURE}» Options: {labels}", "reply")
        )
        if kind in ("cancel", "only_looking") or (away and away["route"] == "cancel"):
            return moved(state, "cancelled") | {"chosen": None, "reply": replies.CANCELLED, "turns": turns}
        if away:
            return {"turns": turns} | away
        return {
            "chosen": None,
            "messages": state["messages"] + [answer],
            "turns": turns,
            "lead": None,
            "route": None,
        }
