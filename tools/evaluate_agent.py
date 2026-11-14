"""Scores the agent on a Persian case file, with the real models, on the stand-in app.

Run by a person: `python tools/evaluate_agent.py tests/evaluation/heldout.json`. The tests call
main() with fake models. Each case gets a fresh app and graph, built as main_debug.py builds them.
It reaches the app only through the agent and the stand-in, never the replica, and it never
changes the agent, the cases or a model service. It writes one record file under local/scratch/.
"""

# ruff: noqa: E402 - the repository's packages are imported after the root is put on the path
import json
import sys
import time
from datetime import UTC, datetime
from pathlib import Path

# Run as `python tools/evaluate_agent.py`, Python only sees tools/, so the root is added first.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from langgraph.types import Command

from assistant.agent_graph import build_graph
from assistant.agent_nodes import Tools, plan_for
from assistant.agent_state import new_request
from assistant.app import DEVICE_ID
from assistant.command_queue import CommandQueue
from assistant.graph_data import read_json
from assistant.intent import FeatureIndex
from assistant.lookups import FixtureLookups
from assistant.models import Chat, Embeddings, ModelError
from assistant.navigator import HOME, Navigator
from tools.evaluation_score import report, score
from tools.stand_in_app import StandInApp

RECORDS = Path("local/scratch/p6-02")  # where each run's full record is written
OPTION = "option:"


class RecordingApp(StandInApp):
    """The stand-in app, keeping the name of every operation it was sent."""

    def __init__(self, profile: dict) -> None:
        self.operations: list[str] = []
        super().__init__(profile, CommandQueue(), DEVICE_ID)

    def send(self, operation: str, arguments: dict) -> dict:
        """Records the operation, then plays it as the stand-in does."""
        self.operations.append(operation)
        return super().send(operation, arguments)


def feature_facts(profile: dict, navigator: Navigator) -> dict:
    """Each feature's requirement types and the screen its run must end on, planned from Home."""
    facts = {}
    for feature in profile["features"]:
        plan = plan_for(navigator, HOME, feature)
        facts[feature["intent"]] = {
            "types": {item.ref: item.type for item in plan.requirements},
            "has_form": feature["form"] is not None,
            "end_screen": feature["review"] or feature["destination"],
        }
    return facts


def pending_of(result: dict) -> dict | None:
    """The agent's pending question after a turn, or None when it answered."""
    return result["__interrupt__"][0].value if "__interrupt__" in result else None


def talk(graph, config: dict, case: dict, record: dict) -> dict | None:
    """Sends the case's turns, one graph call each, notes the questions and the first choice, and
    returns the question still pending at the end, if any."""
    pending = None
    for index, turn in enumerate(case["turns"]):
        if index == 0:
            result = graph.invoke(new_request(case["id"], turn), config)
            first = graph.get_state(config).values
            record["offered"], record["chosen"] = first["offered"], first["chosen"]
        elif pending is None:
            return None  # the agent answered before the turns ran out
        elif turn.startswith(OPTION) and turn[len(OPTION) :] not in [o["ref"] for o in pending["options"]]:
            record["failure"] = f"option not offered: {turn[len(OPTION) :]}"
            return pending
        else:
            result = graph.invoke(Command(resume=turn.removeprefix(OPTION)), config)
        pending = pending_of(result)
        if pending is not None:
            if record["questions"] == 0:
                record["asked_before_prepare"] = graph.get_state(config).values["report"] is None
            record["questions"] += 1
    return pending


def run_case(case: dict, profile: dict, navigator: Navigator, chat: Chat, index: FeatureIndex) -> dict:
    """Runs one case on a fresh app and graph. A crash is recorded, never raised."""
    record = {"id": case["id"], "tags": case["tags"], "expect": case["expect"], "offered": [], "chosen": None}
    record |= {"questions": 0, "asked_before_prepare": False, "failure": None, "crash": None}
    app = RecordingApp(profile)
    tools = Tools(chat, index, navigator, FixtureLookups(), app, app.current_screen)
    graph = build_graph(tools)
    config = {"configurable": {"thread_id": case["id"]}}
    try:
        pending = talk(graph, config, case, record)
        final = graph.get_state(config).values
        plan = final.get("plan")
        record |= {
            # While a question waits, the agent's state is the one it reports with the question.
            "task_state": pending["task_state"] if pending else final["task_state"],
            "missing": final["missing"],
            "values": final["values"],
            "planned": plan["intent"] if plan else None,
            "planned_types": {item["ref"]: item["type"] for item in plan["requirements"]} if plan else {},
            "prepared": final["report"] is not None,
            "reply": pending["question"] if pending else final["reply"],
        }
    except Exception as error:  # any crash is a result to report, not a stop
        seen = graph.get_state(config).values  # the last step the agent finished
        record["offered"] = record["offered"] or seen.get("offered", [])  # if the first turn crashed
        record["chosen"] = record["chosen"] or seen.get("chosen")
        record |= {"crash": f"{type(error).__name__}: {error}", "task_state": None, "missing": []}
        record |= {"values": {}, "planned": None, "planned_types": {}, "prepared": False, "reply": None}
    record |= {"operations": app.operations, "last_screen": app.current_screen()}
    return record


def main(chat: Chat, embeddings: Embeddings, cases_path: str) -> int:
    """Runs every case and prints the report. 0: every gate passes; 1: a gate fails; 2: cannot run."""
    started = time.monotonic()
    profile = read_json("replica.profile.json")
    navigator = Navigator(profile)
    cases = json.loads(Path(cases_path).read_text(encoding="utf-8"))
    try:
        embeddings.embed(["کارت"], query=True)  # one word first: are the embeddings reachable?
    except ModelError as error:
        print(f"Cannot run: the embeddings are not reachable ({error}).")
        return 2
    index = FeatureIndex(profile["features"], embeddings)  # one index: the features are embedded once
    records = []
    for number, case in enumerate(cases, start=1):
        print(f"[{number}/{len(cases)}] {case['id']}", flush=True)
        records.append(run_case(case, profile, navigator, chat, index))
    facts = feature_facts(profile, navigator)
    for record in records:
        record["score"] = score(record, facts)
    RECORDS.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
    out = RECORDS / f"{Path(cases_path).stem}-{stamp}.json"
    out.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")
    passed = report(records, time.monotonic() - started)
    print(f"Full record: {out}")
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main(Chat(), Embeddings(), sys.argv[1]))
