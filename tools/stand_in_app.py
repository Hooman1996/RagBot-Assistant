"""A stand-in for the replica, driven only by the profile, for main_debug.py and the tests.

The task runner calls it as its Bridge. Each send goes through the real CommandQueue and the real
contract checks, all on one thread, so stepping through one command shows the whole loop. It
plays the app and decides nothing for the assistant. Like the replica, it never takes a user-only
edge, never fills a secret field, and has no way to confirm, submit or pay.
"""

from assistant.command_queue import OPERATIONS, CommandQueue
from assistant.contract_check import problems
from assistant.navigator import HOME, Navigator, search
from assistant.task_runner import plain_digits


class StandInError(Exception):
    """The queue or the contract refused something the stand-in was handed or built."""


def must_match(schema_file: str, value: dict) -> None:
    """Stops the run when value breaks its contract schema."""
    found = problems(schema_file, value)
    if found:
        raise StandInError(f"{schema_file}: {found}")


def shown(value: str | dict) -> str:
    """What the app shows for a value: money as its digits, a 12+ digit number as **** and 4 digits."""
    text = str(value["value"]) if isinstance(value, dict) else value
    digits = plain_digits(text)
    return "****" + digits[-4:] if len(digits) >= 12 else text


class StandInApp:
    """One pretend device: a screen, its fields, a draft and an observation version."""

    def __init__(self, profile: dict, queue: CommandQueue, device_id: str, screen: str = HOME) -> None:
        self.navigator = Navigator(profile)
        self.screens = set(profile["screens"])
        self.features = profile["features"]
        self.queue = queue
        self.device_id = device_id
        self.screen = screen
        self.fields: list[dict] = []
        self.draft: dict | None = None
        self.version = 0
        self.observation = self.observe()
        # Reporting in, as the replica's first poll does, so commands can be queued for it.
        queue.next_command(device_id, self.observation)

    def current_screen(self) -> str:
        """Where the stand-in is."""
        return self.screen

    def send(self, operation: str, arguments: dict) -> dict:
        """Queues one command, takes it as the app would, runs it and reports its result."""
        command = self.queue.queue_command(self.device_id, operation, arguments)
        taken = self.queue.next_command(self.device_id, self.observation)
        if taken is None or taken["command_id"] != command["command_id"]:
            raise StandInError("the queue did not hand over the command")
        must_match("command.schema.json", taken)
        result = self.run(taken)
        must_match("result.schema.json", result)
        self.queue.record_result(result)
        return result

    def run(self, command: dict) -> dict:
        """Performs the command and wraps the outcome with a fresh observation."""
        arguments = command["arguments"]
        if command["operation"] == "navigate":
            ending = self.navigate(arguments["destination"], arguments.get("arguments", {}))
        elif command["operation"] == "fill_form":
            ending = self.fill(arguments["form"], arguments["values"])
        elif command["operation"] == "show_review":
            ending = self.show_review()
        else:
            ending = {"outcome": "succeeded", "postcondition": "observed"}
        self.observation = self.observe()
        draft_version = self.draft["draft_version"] if self.draft else 0
        return {
            "command_id": command["command_id"],
            **ending,
            "observation": self.observation,
            "draft_version": draft_version,
        }

    def observe(self) -> dict:
        """A contract-valid observation. Its version rises after every operation."""
        self.version += 1
        return {
            "observation_version": self.version,
            "app_version": 1,
            "profile_version": 1,
            "screen": self.screen,
            "modal": None,
            "loading": False,
            "fields": self.fields,
            "draft": self.draft,
            "supported_operations": list(OPERATIONS),
        }

    def route(self, destination: str, arguments: dict) -> list[dict] | None:
        """The shortest walk, preferring edges the arguments can complete, else by way of Home."""
        usable = [
            edge
            for edge in self.navigator.edges
            if all(need["ref"] in arguments for need in edge.get("requires", []))
        ]
        for start in (self.screen, HOME):
            for edges in (usable, self.navigator.edges):
                found = search(start, destination, edges)
                if found is not None:
                    back = (
                        [] if start == self.screen else [{"from": self.screen, "to": HOME, "action": "back"}]
                    )
                    return back + found
        return None

    def navigate(self, destination: str, arguments: dict) -> dict:
        """Walks the profile's edges, putting each row's key in place of *, and records the trace."""
        if destination not in self.screens:
            return {"outcome": "rejected_policy", "postcondition": None, "error_code": "unknown_screen"}
        route = self.route(destination, arguments)
        if route is None:
            return {"outcome": "blocked", "postcondition": None, "needs": ["no_executor_route"]}
        trace = []
        for edge in route:
            action = edge["action"]
            if "*" in action:
                key = arguments.get(edge["requires"][0]["ref"])
                if key is None:
                    return {
                        "outcome": "blocked",
                        "postcondition": None,
                        "needs": ["missing_argument"],
                        "trace": trace,
                    }
                action = action.replace("*", key)
            trace.append({"from": edge["from"], "to": edge["to"], "action": action})
            self.screen = edge["to"]
            self.fields = []
        return {"outcome": "succeeded", "postcondition": f"screen_is_{destination}", "trace": trace}

    def fill(self, form: str, values: dict) -> dict:
        """Shows each value on the form, refusing any field the form lacks or that is secret."""
        fields = {item["ref"]: item for item in self.navigator.forms.get(form, [])}
        for ref in values:
            if ref not in fields or fields[ref].get("secret") or self.navigator.is_user_only(ref):
                return {
                    "outcome": "rejected_policy",
                    "postcondition": None,
                    "error_code": "field_not_fillable",
                }
        if self.screen != form:
            return {"outcome": "blocked", "postcondition": None, "needs": ["navigate"]}
        now = {item["ref"]: item["value"] for item in self.fields} | {
            ref: shown(v) for ref, v in values.items()
        }
        self.fields = [{"ref": ref, "value": value} for ref, value in now.items()]
        version = self.draft["draft_version"] + 1 if self.draft else 1
        self.draft = {"draft_id": "draft-1", "draft_version": version, "review_version": 0}
        return {"outcome": "succeeded", "postcondition": f"form_filled_{form}"}

    def show_review(self) -> dict:
        """Moves from a feature's form to its review page. Anywhere else there is no review."""
        review = next(
            (item["review"] for item in self.features if item["form"] == self.screen and item["review"]),
            self.screen,
        )
        if review == self.screen:
            return {"outcome": "blocked", "postcondition": None, "needs": ["no_review_here"]}
        self.screen = review
        self.fields = []
        if self.draft:
            self.draft = self.draft | {"review_version": self.draft["draft_version"]}
        return {"outcome": "succeeded", "postcondition": f"review_shown_{review}"}
