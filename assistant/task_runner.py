"""Runs a plan on the app: navigate, check, fill, check, and at the end show the review.

Called by graph_tasks, and by tests with a fake bridge. Scripted, with no LLM. It resolves every
value before the app moves, checks each page from the observation, and stops with a plain reason
on any mismatch. It only ever sends navigate, fill_form and show_review. It never confirms, and
it fills nothing the plan does not list, so a secret field can never be sent.
"""

from dataclasses import asdict, dataclass, field
from typing import Protocol

from assistant.lookups import Ambiguous, FixtureLookups, NotFound
from assistant.navigator import Leg, Navigator, Plan, Requirement
from assistant.steps import show

PERSIAN_DIGITS = str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")
NO_REVIEW = "no_review_here"


class Bridge(Protocol):
    """What the runner needs from the command loop."""

    def send(self, operation: str, arguments: dict) -> dict:
        """Sends one command and returns its result."""
        ...


class TaskStopped(Exception):
    """The task cannot go on. Its message says why; state is how the report names the stop."""

    state = "stopped"


class Uncertain(TaskStopped):
    """A command was sent, but whether it ran is unknown: no result came, or the app said uncertain."""

    state = "uncertain"


class Unreachable(TaskStopped):
    """The app cannot be reached, so the command was not sent."""

    state = "unreachable"


class Cancelled(TaskStopped):
    """The user cancelled; the run stopped before its next command."""

    state = "cancelled"


@dataclass
class TaskReport:
    """How a run is going: its state, why it stopped, and each command and what came back."""

    state: str = "running"
    reason: str | None = None
    steps: list[dict] = field(default_factory=list)

    def as_json(self) -> dict:
        """The report as plain JSON, for the graph page."""
        return asdict(self)


def plain_digits(text: str) -> str:
    """Only the digits of a value, in Latin form."""
    return "".join(ch for ch in text.translate(PERSIAN_DIGITS) if ch.isdigit())


def money_value(text: str) -> dict:
    """A whole number of rials as the contract writes it. Refuses anything else."""
    digits = text.strip().translate(PERSIAN_DIGITS)
    if not digits.isdigit():
        raise TaskStopped(f"مبلغ باید عدد صحیح به ریال باشد: {text}")
    return {"value": int(digits), "unit": "IRR"}


def shows(requirement: Requirement, sent: str | dict, shown: str | None) -> bool:
    """True when the observation agrees. A choice or picker shows the app's own words, if any."""
    if shown is None:
        return requirement.type in ("ref", "choice")
    if requirement.type == "money":
        return plain_digits(shown) == str(sent["value"])
    if shown.startswith("****") and str(sent).isdigit():
        return str(sent).endswith(shown[4:])
    if requirement.type == "text":
        return shown == sent
    return shown != ""


class TaskRunner:
    """Carries out plans over one bridge."""

    def __init__(self, bridge: Bridge, navigator: Navigator, lookups: FixtureLookups) -> None:
        self.bridge = bridge
        self.navigator = navigator
        self.lookups = lookups

    def missing(self, plan: Plan, values: dict[str, str]) -> list[Requirement]:
        """The requirements still without a value, so all can be asked at once."""
        return [item for item in plan.requirements if not values.get(item.ref, "").strip()]

    def prepared(self, plan: Plan, values: dict[str, str]) -> dict[str, str | dict]:
        """Every value as the app takes it. Runs before the app moves, so a bad one moves nothing."""
        wanted = {item.ref for item in plan.requirements}
        unknown = sorted(set(values) - wanted)
        if unknown:
            raise TaskStopped(f"این مقادیر در مسیر نیستند: {unknown}")
        lacking = self.missing(plan, values)
        if lacking:
            raise TaskStopped(f"مقدار کم است: {[item.ref for item in lacking]}")
        return {item.ref: self.prepared_value(item, values[item.ref]) for item in plan.requirements}

    def prepared_value(self, item: Requirement, text: str) -> str | dict:
        """One value, resolved through its lookup when it has one."""
        if item.type == "money":
            return money_value(text)
        if item.lookup is None:
            return text.strip()
        try:
            value = self.lookups.resolve(item.lookup, text.strip())
        except (NotFound, Ambiguous) as problem:
            raise TaskStopped(f"{item.description}: {problem}") from problem
        show("resolve value", field=item.ref, lookup=item.lookup, said=text.strip(), sends=value)
        return value

    def run(self, plan: Plan, values: dict[str, str], review: bool, report: TaskReport) -> TaskReport:
        """Runs the plan into report, which the caller may read while this runs."""
        route = "\n".join(f"{step['from']} → {step['to']} ({step['action']})" for step in plan.route)
        show("plan", target=plan.target, route=route, requirements=[item.ref for item in plan.requirements])
        try:
            sent = self.prepared(plan, values)
            for leg in plan.legs:
                self.move(leg, sent, report)
                if leg.fields:
                    self.fill(leg, sent, report)
            report.state = self.show_review(report) if review else "arrived"
        except TaskStopped as stop:
            report.state = stop.state
            report.reason = str(stop)
        show("run ended", state=report.state, **({"reason": report.reason} if report.reason else {}))
        return report

    def command(self, operation: str, arguments: dict, report: TaskReport) -> dict:
        """Sends one command and records it and its result for the side panel."""
        show(f"send {operation}", arguments=arguments)
        result = self.bridge.send(operation, arguments)
        seen = result["observation"]
        answer = {"outcome": result["outcome"], "screen": seen["screen"], "fields": seen["fields"]}
        answer |= {key: result[key] for key in ("needs", "error_code") if result.get(key)}
        show(f"answer to {operation}", **answer)
        report.steps.append(
            {
                "operation": operation,
                "arguments": arguments,
                "outcome": result["outcome"],
                "screen": seen["screen"],
                "fields": seen["fields"],
                "needs": result.get("needs", []),
                "error_code": result.get("error_code"),
                "trace": result.get("trace", []),
            }
        )
        if result["outcome"] == "uncertain":
            raise Uncertain(f"معلوم نیست {operation} انجام شد یا نه: {why(result)}")
        return result

    def move(self, leg: Leg, sent: dict, report: TaskReport) -> None:
        """Navigates to the leg's screen and checks that the app is there."""
        arguments: dict = {"destination": leg.screen}
        if leg.selections:
            arguments["arguments"] = {item.ref: sent[item.ref] for item in leg.selections}
        self.check_keys(leg, sent)
        result = self.command("navigate", arguments, report)
        screen = result["observation"]["screen"]
        if result["outcome"] != "succeeded" or screen != leg.screen:
            raise TaskStopped(f"به {leg.screen} نرسید و در {screen} ماند: {why(result)}")

    def check_keys(self, leg: Leg, sent: dict) -> None:
        """Refuses a chosen key that would spell a user-only tag, before the app sees it."""
        for item in leg.selections:
            for edge in self.navigator.edges:
                if edge["from"] == item.screen and not self.navigator.key_is_allowed(edge, sent[item.ref]):
                    raise TaskStopped(f"این انتخاب مجاز نیست: {item.ref}")

    def fill(self, leg: Leg, sent: dict, report: TaskReport) -> None:
        """Fills the leg's fields and checks each one from the observation."""
        values = {item.ref: sent[item.ref] for item in leg.fields}
        result = self.command("fill_form", {"form": leg.screen, "values": values}, report)
        seen = result["observation"]
        if result["outcome"] != "succeeded" or seen["screen"] != leg.screen:
            raise TaskStopped(f"فرم {leg.screen} پر نشد: {why(result)}")
        shown = {item["ref"]: item["value"] for item in seen["fields"]}
        wrong = [item.ref for item in leg.fields if not shows(item, sent[item.ref], shown.get(item.ref))]
        if wrong:
            raise TaskStopped(f"مقدار این فیلدها روی صفحه درست نیست: {wrong}")
        show("fields checked", shown=shown)

    def show_review(self, report: TaskReport) -> str:
        """Opens the review and stops there. Only the user goes further."""
        result = self.command("show_review", {}, report)
        if result["outcome"] == "succeeded":
            return "review"
        if result.get("needs") == [NO_REVIEW]:
            return "arrived"
        raise TaskStopped(f"صفحه بازبینی باز نشد: {why(result)}")


def why(result: dict) -> str:
    """The app's own reason, as it reported it."""
    return ", ".join(result.get("needs", [])) or result.get("error_code") or result["outcome"]
