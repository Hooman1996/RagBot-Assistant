"""The agent's nodes: each one plain method that takes the state and returns only what it changes.

Wired into one graph by agent_graph.py. The model only returns JSON, through intent.py and
slots.py; code plans, resolves, runs and checks. The app is reached only through TaskRunner, so
the only commands are navigate, fill_form and show_review. Nothing here can confirm, submit or pay.
"""

from collections.abc import Callable
from copy import deepcopy
from dataclasses import dataclass

from langgraph.types import interrupt

from assistant import replies
from assistant.agent_state import AgentState, moved
from assistant.intent import KNOWLEDGE, NOT_SURE, FeatureIndex, choose
from assistant.lifecycle import AFTER_STOP, CANCEL, StoppableBridge, wants_cancel
from assistant.lookups import Candidate, FixtureLookups, source_of, typed
from assistant.models import Chat, ModelError
from assistant.navigator import CrossesUserOnly, Navigator, NoRoute, Plan
from assistant.slots import checked, extract, hidden
from assistant.steps import masked, show
from assistant.task_runner import Bridge, TaskReport, TaskRunner
from assistant.words import field_words, fixed, installment_of, limits, options, plates, when

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

    def planned(self, feature: dict, held: dict) -> dict:
        """The plan from the app's current screen, keeping held values the route still asks for. A
        choice gets its allowed keys; a value the feature decides, or a choice with one key, is set
        by code and never asked. Raises NoRoute or CrossesUserOnly."""
        start = self.tools.screen()
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
            replanned = self.planned(self.features[plan["intent"]], values)
            plan, values = replanned["plan"], replanned["values"]
        plan, values, note = self.payable(plan, values)
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

    def payable(self, plan: dict, values: dict) -> tuple[dict, dict, str | None]:
        """Code only: an installment choice keeps the installments the app lets the user pick for
        the named policy or loan. The only one is set; one the app greys out is dropped and named."""
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
            if chosen is not None and chosen not in keys:
                label = next(option["label"] for option in item["options"] if option["key"] == chosen)
                note = replies.NOT_OFFERED.format(text=label)
                del values[item["ref"]]
            item["options"] = [option for option in item["options"] if option["key"] in keys]
            if chosen is None and len(item["options"]) == 1:
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
        """Asks once for everything missing, and waits for the user's text or an option's ref."""
        update = moved(state, "clarifying") | {"question": None, "options": []}
        wanted = [item for item in state["plan"]["requirements"] if item["ref"] in state["missing"]]
        options = (state["options"] or replies.lookup_options(self.tools.lookups, wanted)) + [CANCEL]
        text = replies.question(state["question"], [item["ask"] for item in wanted])
        answer = interrupt({"question": text, "options": options, "task_state": update["task_state"]})
        if wants_cancel(answer):
            return update | moved(update, "cancelled") | {"reply": replies.CANCELLED}
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

    def prepare(self, state: AgentState) -> dict:
        """Runs the task runner with the resolved values, as the graph page's Run does."""
        feature = self.features[state["plan"]["intent"]]
        plan = plan_for(self.tools.navigator, state["plan"]["start"], feature, state["values"])
        # The runner reads money as text and turns it back into the same whole number of rials.
        values = {ref: str(v["value"]) if isinstance(v, dict) else v for ref, v in state["values"].items()}
        bridge = StoppableBridge(self.tools.bridge, self.tools.stopped)  # a cancel stops between commands
        runner = TaskRunner(bridge, self.tools.navigator, self.tools.lookups)
        report = runner.run(plan, values, feature["form"] is not None, TaskReport())
        return moved(state, "preparing") | {"report": report.as_json()}

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
        return moved(state, "ready_for_user") | {"reply": reply}

    def reply(self, state: AgentState) -> dict:
        """A knowledge question gets the fixed message; "not sure" asks with the closest features."""
        if state["chosen"] == KNOWLEDGE:
            return {"reply": replies.KNOWLEDGE}
        options = [{"ref": intent, "label": self.features[intent]["title"]} for intent in state["offered"]]
        pending = {
            "question": replies.NOT_SURE,
            "options": options + [CANCEL],
            "task_state": state["task_state"],
        }
        answer = interrupt(pending)
        if wants_cancel(answer):
            return moved(state, "cancelled") | {"chosen": None, "reply": replies.CANCELLED}
        if answer in state["offered"]:
            return {"chosen": answer}
        return {"chosen": None, "messages": state["messages"] + [answer]}
