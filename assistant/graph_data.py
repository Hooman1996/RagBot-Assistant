"""Builds what the graph page draws: every screen, every edge and where to put them.

Called once at start by app.py. It reads app_profiles/ and changes nothing.
Built screens are the profile's screens. Everything else in the navigation graph is grey.
"""

import json
from collections import deque
from pathlib import Path

PROFILES = Path(__file__).resolve().parent.parent / "app_profiles"
START = "D-000"
COLUMN_WIDTH = 300
ROW_HEIGHT = 24
GREY_ROWS = 90


def read_json(name: str) -> dict:
    """Reads one file of app_profiles/."""
    return json.loads((PROFILES / name).read_text(encoding="utf-8"))


def built_screens() -> set[str]:
    """The screens the replica has."""
    return set(read_json("replica.profile.json")["screens"])


def depths(edges: list[dict]) -> dict[str, int]:
    """How many steps each screen is from the front page, over the profile's edges."""
    found = {START: 0}
    queue = deque([START])
    while queue:
        screen = queue.popleft()
        for edge in edges:
            if edge["from"] == screen and edge["to"] not in found:
                found[edge["to"]] = found[screen] + 1
                queue.append(edge["to"])
    return found


def place_nodes(nodes: list[dict], reachable: dict[str, int]) -> None:
    """Sets x and y: one column per step from the front page, then the rest in grey columns."""
    columns: dict[int, int] = {}
    by_area = sorted(nodes, key=lambda node: (node["area"], node["id"]))
    for node in by_area:
        if node["id"] in reachable:
            column = reachable[node["id"]]
            node["x"] = column * COLUMN_WIDTH
            node["y"] = columns.get(column, 0) * ROW_HEIGHT
            columns[column] = columns.get(column, 0) + 1
    first_free = max(reachable.values()) + 1
    rest = [node for node in by_area if node["id"] not in reachable]
    for index, node in enumerate(rest):
        node["x"] = (first_free + index // GREY_ROWS) * COLUMN_WIDTH
        node["y"] = (index % GREY_ROWS) * ROW_HEIGHT


def graph_payload() -> dict:
    """Every node and edge of the graph page, as plain JSON."""
    graph = read_json("navigation-graph.json")
    profile = read_json("replica.profile.json")
    built = set(profile["screens"])
    names = {node["id"]: node for node in graph["nodes"]}
    designed = [
        {"id": screen, "name": screen, "area": "F-000"} for screen in sorted(built) if screen not in names
    ]
    nodes = [
        {"id": node["id"], "name": node["name"], "area": node["area"], "built": node["id"] in built}
        for node in graph["nodes"] + designed
    ]
    place_nodes(nodes, depths(profile["edges"]))
    walked = {(edge["from"], edge["to"]) for edge in profile["edges"]}
    known = {node["id"] for node in nodes}
    # A dynamic edge may have no fixed target, so it has nothing to draw.
    recovered = {(edge["from"], edge["to"]) for edge in graph["edges"] if edge["to"] in known}
    edges = [
        {"from": start, "to": end, "walked": (start, end) in walked}
        for start, end in sorted(walked | recovered)
        if start in known
    ]
    return {"nodes": nodes, "edges": edges}
