"""Plans a move: the route from one screen to another and every requirement along it.

Called by task_runner, graph_tasks and the profile check in tools/project_checks.py. Plain
breadth-first search over the profile's edges, with no LLM and no app. It uses the standard
library only, so the check can import it. It never plans over a user-only action, and it never
lists a secret field as a requirement.
"""

from collections import deque
from dataclasses import asdict, dataclass, field
from fnmatch import fnmatchcase

HOME = "S-026"
BACK = "back"


class NoRoute(Exception):
    """No route the assistant may take leads to the target."""


class CrossesUserOnly(Exception):
    """The only route to the target crosses an action only the user may perform."""


@dataclass(frozen=True)
class Requirement:
    """One value the route needs: a form field on a screen, or a selection before an edge."""

    screen: str
    ref: str
    kind: str
    type: str
    ask: str
    description: str | None = None
    lookup: str | None = None


@dataclass(frozen=True)
class Leg:
    """One stop of a plan: navigate to screen with arguments, then fill its fields."""

    screen: str
    selections: tuple[Requirement, ...] = ()
    fields: tuple[Requirement, ...] = ()


@dataclass
class Plan:
    """A route, its requirements, and the stops the task runner makes."""

    target: str
    route: list[dict]
    legs: list[Leg] = field(default_factory=list)

    @property
    def requirements(self) -> list[Requirement]:
        """Every requirement of the route, in route order, each listed once."""
        found = [item for leg in self.legs for item in leg.selections + leg.fields]
        return list(dict.fromkeys(found))

    def as_json(self) -> dict:
        """The plan as plain JSON, for the graph page."""
        return {
            "target": self.target,
            "route": self.route,
            "requirements": [asdict(r) for r in self.requirements],
        }


def tags(action: str) -> list[str]:
    """The test tags of an edge's action, in tap order."""
    return action.split(" > ")


def search(start: str, target: str, edges: list[dict]) -> list[dict] | None:
    """Breadth-first search. Returns the edges of a shortest route, or None."""
    if start == target:
        return []
    came_by: dict[str, dict] = {}
    queue = deque([start])
    while queue:
        screen = queue.popleft()
        for edge in edges:
            if edge["from"] != screen or edge["to"] == start or edge["to"] in came_by:
                continue
            came_by[edge["to"]] = edge
            if edge["to"] == target:
                return walk_back(start, target, came_by)
            queue.append(edge["to"])
    return None


def walk_back(start: str, target: str, came_by: dict[str, dict]) -> list[dict]:
    """Lists the route in order from the search's records."""
    route: list[dict] = []
    screen = target
    while screen != start:
        route.insert(0, came_by[screen])
        screen = came_by[screen]["from"]
    return route


class Navigator:
    """Answers route questions over one profile."""

    def __init__(self, profile: dict) -> None:
        self.user_only: list[str] = profile["user_only_actions"]
        self.all_edges: list[dict] = profile["edges"]
        self.edges = [edge for edge in self.all_edges if not self.crosses_user_only(edge)]
        self.forms = {form["id"]: form["fields"] for form in profile["forms"]}
        self.features = profile["features"]

    def is_user_only(self, tag: str) -> bool:
        """True when tag is on the profile's list of actions only the user may perform."""
        return any(fnmatchcase(tag, pattern) for pattern in self.user_only)

    def crosses_user_only(self, edge: dict) -> bool:
        """True when any tap of this edge is the user's alone."""
        return any(self.is_user_only(tag) for tag in tags(edge["action"]))

    def key_is_allowed(self, edge: dict, key: str) -> bool:
        """False when a chosen key would spell a user-only tag on this edge."""
        return not any(self.is_user_only(tag.replace("*", key)) for tag in tags(edge["action"]))

    def feature_for(self, screen: str) -> dict | None:
        """The first feature that ends on this screen, or None."""
        return next((item for item in self.features if item["destination"] == screen), None)

    def route(self, start: str, target: str) -> list[dict]:
        """A shortest route, preferring one with no selection, else one by way of Home."""
        plain = [edge for edge in self.edges if "requires" not in edge]
        found = search(start, target, plain)
        if found is None:
            found = search(start, target, self.edges)
        if found is not None:
            return found
        if search(start, target, self.all_edges) is not None:
            raise CrossesUserOnly(target)
        if start == HOME:
            raise NoRoute(target)
        return [{"from": start, "to": HOME, "action": BACK}, *self.route(HOME, target)]

    def fields_of(self, screen: str) -> tuple[Requirement, ...]:
        """The fields this screen needs. A secret field is never one of them."""
        fields = [
            item for item in self.forms.get(screen, []) if item.get("requires") and not item.get("secret")
        ]
        return tuple(self.requirement(screen, "field", item) for item in fields)

    def requirement(self, screen: str, kind: str, item: dict) -> Requirement:
        """Reads one field or selection of the profile."""
        return Requirement(
            screen, item["ref"], kind, item["type"], item["ask"], item.get("description"), item.get("lookup")
        )

    def plan(self, current: str, target: str, via: str | None = None) -> Plan:
        """Plans the move from current to target, passing through via when given."""
        route = self.route(current, via) if via else []
        route += self.route(via or current, target)
        return Plan(target, route, self.legs(current, target, route))

    def plan_feature(self, current: str, feature: dict) -> Plan:
        """Plans a feature: through its form, so a shared page is reached the right way."""
        form = feature["form"]
        return self.plan(current, feature["destination"], form if form != feature["destination"] else None)

    def legs(self, current: str, target: str, route: list[dict]) -> list[Leg]:
        """Cuts the route at every screen with fields. Other screens pass inside navigate."""
        legs: list[Leg] = []
        first_fields = () if route and route[0]["action"] == BACK else self.fields_of(current)
        if first_fields:
            legs.append(Leg(current, (), first_fields))
        selections: list[Requirement] = []
        for edge in route:
            selections += [
                self.requirement(edge["from"], "selection", item) for item in edge.get("requires", [])
            ]
            fields = self.fields_of(edge["to"])
            if fields:
                legs.append(Leg(edge["to"], tuple(selections), fields))
                selections = []
        if not legs or legs[-1].screen != target or selections:
            legs.append(Leg(target, tuple(selections)))
        return legs
