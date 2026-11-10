"""What the graph page asks of the assistant: plan a click, and run it.

Called by graph_page. It holds at most one run, on a background thread, because a run waits for
the app. Everything it does goes through navigator and task_runner, so it adds no way to touch a
confirm, submit or pay action, and nothing here chooses what is required. It refuses a run while
the page's chat is taking a turn, because both drive the same replica.
"""

from collections.abc import Callable
from dataclasses import asdict
from threading import Thread

from assistant.bridge_link import BridgeLink
from assistant.command_queue import CommandQueue
from assistant.demo_values import demo_value
from assistant.lookups import FixtureLookups
from assistant.navigator import HOME, CrossesUserOnly, Navigator, NoRoute, Plan
from assistant.task_runner import TaskReport, TaskRunner


class RunInProgress(Exception):
    """A run is already going."""


class GraphTasks:
    """The planning and the one run of a graph page."""

    def __init__(self, queue: CommandQueue, device_id: str, profile: dict) -> None:
        self.queue = queue
        self.device_id = device_id
        self.navigator = Navigator(profile)
        self.lookups = FixtureLookups()
        self.report = TaskReport(state="idle")
        self.chat_busy: Callable[[], bool] | None = None  # set by app.py to the chat's busy()

    def current_screen(self) -> str:
        """Where the replica is. Before it reports in, plans start from Home."""
        seen = self.queue.device(self.device_id).observation
        return seen["screen"] if seen else HOME

    def plan_for(self, screen: str) -> tuple[Plan, dict | None]:
        """The plan to a screen. A feature's own route is used when one ends there."""
        feature = self.navigator.feature_for(screen)
        current = self.current_screen()
        if feature is None or feature["form"] is None:
            return self.navigator.plan(current, screen), feature
        return self.navigator.plan_feature(current, feature), feature

    def preview(self, screen: str) -> dict:
        """What a click shows: the route, each question and its demo value, or why not."""
        try:
            plan, feature = self.plan_for(screen)
        except CrossesUserOnly:
            return {"status": "user_only"}
        except NoRoute:
            return {"status": "no_route"}
        demo = {item.ref: demo_value(item, screen, self.lookups) for item in plan.requirements}
        return {"status": "ok", **plan.as_json(), "feature": feature and feature["intent"], "demo": demo}

    def start(self, screen: str, values: dict[str, str]) -> None:
        """Starts a run on a background thread. Only one may run at a time."""
        if self.report.state == "running" or (self.chat_busy is not None and self.chat_busy()):
            raise RunInProgress(screen)
        plan, feature = self.plan_for(screen)
        review = feature is not None and feature["form"] is not None
        runner = TaskRunner(BridgeLink(self.queue, self.device_id), self.navigator, self.lookups)
        self.report = TaskReport()
        Thread(target=runner.run, args=(plan, values, review, self.report), daemon=True).start()

    def progress(self) -> dict:
        """The run so far."""
        return asdict(self.report)
