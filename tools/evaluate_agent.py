"""Scores the agent on a Persian case file, with the real models, on the stand-in app.

Run by a person: `python tools/evaluate_agent.py tests/evaluation/heldout.json`, with
`--agent react` for the ReAct agent (D-032). A conversation file (D-035) is run turn by turn and
scored on every turn: `python tools/evaluate_agent.py tests/evaluation/conversations_heldout.json`.
The tests call main() with fake models. Each case or conversation gets a fresh app and graph, built
as main_debug.py builds them.
It reaches the app only through the agent and the stand-in, never the replica, and it never
changes the agent, the cases or a model service. Each record also keeps the case's model calls and
seconds. It writes one record file under local/scratch/, named by the cases and the agent.
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

from assistant.agent_nodes import Tools, plan_for
from assistant.agent_state import new_request
from assistant.app import DEVICE_ID
from assistant.command_queue import CommandQueue
from assistant.graph_data import read_json
from assistant.intent import FeatureIndex
from assistant.lifecycle import next_input
from assistant.lookups import FixtureLookups
from assistant.models import Chat, Embeddings, ModelError
from assistant.navigator import HOME, Navigator
from assistant.react_graph import build_agent
from assistant.task_runner import Unreachable
from tools.evaluation_score import conversation_report, report, score, score_conversation
from tools.stand_in_app import StandInApp

RECORDS = Path("local/scratch/p6-02")  # where each run's full record is written
OPTION = "option:"


class CountingChat:
    """The chat model, counting the calls one case makes and keeping every prompt it was shown, so a
    conversation can be checked for a secret the model must never see. Chat.step goes through json too."""

    step = Chat.step

    def __init__(self, chat: Chat) -> None:
        self.chat = chat
        self.calls = 0
        self.prompts: list[str] = []

    def json(self, *question: object) -> dict:
        """Counts the call, keeps its prompt, then asks the model."""
        self.calls += 1
        self.prompts.append(str(question[0]))
        return self.chat.json(*question)


class RecordingApp(StandInApp):
    """The stand-in app, keeping the name of every operation it was sent. While down, it cannot be
    reached, as the replica cannot when the phone is away, so nothing is sent."""

    def __init__(self, profile: dict) -> None:
        self.operations: list[str] = []
        self.down = False
        super().__init__(profile, CommandQueue(), DEVICE_ID)

    def send(self, operation: str, arguments: dict) -> dict:
        """Records the operation, then plays it as the stand-in does."""
        if self.down:
            raise Unreachable("the stand-in app is down for this turn")
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


def prepared(report: dict | None) -> bool:
    """True once a run went for the review. Before a question the app only moves to the question's
    screen (its report waits), which prepares nothing."""
    return report is not None and report["state"] != "waiting"


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
                record["asked_before_prepare"] = not prepared(graph.get_state(config).values["report"])
            record["questions"] += 1
    return pending


def run_case(
    case: dict, profile: dict, navigator: Navigator, chat: Chat, index: FeatureIndex, agent: str = "workflow"
) -> dict:
    """Runs one case on a fresh app and graph. A crash is recorded, never raised."""
    record = {"id": case["id"], "tags": case["tags"], "expect": case["expect"], "offered": [], "chosen": None}
    record |= {"questions": 0, "asked_before_prepare": False, "failure": None, "crash": None}
    app = RecordingApp(profile)
    counting = CountingChat(chat)
    tools = Tools(counting, index, navigator, FixtureLookups(), app, app.current_screen)
    graph = build_agent(tools, agent)
    config = {"configurable": {"thread_id": case["id"]}}
    started = time.monotonic()
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
            "prepared": prepared(final["report"]),
            "reply": pending["question"] if pending else final["reply"],
        }
    except Exception as error:  # any crash is a result to report, not a stop
        seen = graph.get_state(config).values  # the last step the agent finished
        record["offered"] = record["offered"] or seen.get("offered", [])  # if the first turn crashed
        record["chosen"] = record["chosen"] or seen.get("chosen")
        record |= {"crash": f"{type(error).__name__}: {error}", "task_state": None, "missing": []}
        record |= {"values": {}, "planned": None, "planned_types": {}, "prepared": False, "reply": None}
    record |= {"operations": app.operations, "last_screen": app.current_screen()}
    return record | {"model_calls": counting.calls, "seconds": round(time.monotonic() - started, 2)}


def observed(graph, config: dict, pending: dict | None, features: set[str]) -> dict:
    """What a person sees after a turn: how it ended, the open task's feature, its values, what is
    asked, and the assistant's words."""
    state = graph.get_state(config).values
    if pending is not None:
        refs = [option["ref"] for option in pending["options"] if option["ref"] != "cancel"]
        end = (
            "switch"
            if "switch=yes" in refs
            else "not_sure"
            if refs and all(ref in features for ref in refs)
            else "asking"
        )
    else:
        ends = {"ready_for_user": "review", "completed": "page", "cancelled": "cancelled", "paused": "paused"}
        end = ends.get(state.get("task_state"), "failed" if state.get("task_state") == "failed" else "idle")
    plan = state.get("plan") if end not in ("idle", "not_sure") else None
    return {
        "end": end,
        "intent": plan["intent"] if plan else None,
        "values": state.get("values", {}) if plan else {},
        "missing": state.get("missing", []) if end == "asking" else [],
        "text": pending["question"] if pending else state.get("reply") or "",
    }


def run_conversation(
    conversation: dict, profile: dict, navigator: Navigator, chat: Chat, index: FeatureIndex, agent: str
) -> dict:
    """Runs one conversation turn by turn on a fresh app and graph, as the chat box would: an option
    resumes the question, other words resume it too or start a turn. A turn that cannot run (an
    option not offered, a crash) ends the conversation, and the turns after it are not run."""
    app = RecordingApp(profile)
    counting = CountingChat(chat)
    tools = Tools(counting, index, navigator, FixtureLookups(), app, app.current_screen)
    graph = build_agent(tools, agent)
    config = {"configurable": {"thread_id": conversation["id"]}}
    features = {item["intent"] for item in profile["features"]}
    pending, turns = None, []
    for turn in conversation["turns"]:
        app.down = turn.get("app_down", False)
        before, calls, started = len(app.operations), counting.calls, time.monotonic()
        record = {"say": turn["say"], "expect": turn["expect"], "failure": None, "crash": None}
        try:
            if turn["say"].startswith(OPTION):
                ref = turn["say"][len(OPTION) :]
                if pending is None or ref not in [option["ref"] for option in pending["options"]]:
                    record["failure"] = f"option not offered: {ref}"
                    turns.append(record)
                    break
                result = graph.invoke(Command(resume=ref), config)
            elif pending is not None:
                result = graph.invoke(Command(resume=turn["say"]), config)
            else:
                previous = graph.get_state(config).values
                result = graph.invoke(next_input(previous, conversation["id"], turn["say"]), config)
            pending = pending_of(result)
            record |= observed(graph, config, pending, features)
        except Exception as error:  # a crash is a result to report, not a stop
            record["crash"] = f"{type(error).__name__}: {error}"
            turns.append(record)
            break
        record |= {"operations": app.operations[before:], "model_calls": counting.calls - calls}
        turns.append(record | {"seconds": round(time.monotonic() - started, 2)})
    return {"id": conversation["id"], "tags": conversation["tags"], "turns": turns} | {
        "planned": len(conversation["turns"]),
        "prompts": counting.prompts,
    }


def main(chat: Chat, embeddings: Embeddings, cases_path: str, agent: str = "workflow") -> int:
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
    facts = feature_facts(profile, navigator)
    if cases and isinstance(cases[0]["turns"][0], dict):  # a conversation file (D-035)
        return conversations(cases, cases_path, profile, navigator, chat, index, agent, facts, started)
    records = []
    for number, case in enumerate(cases, start=1):
        print(f"[{number}/{len(cases)}] {case['id']}", flush=True)
        records.append(run_case(case, profile, navigator, chat, index, agent))
    for record in records:
        record["score"] = score(record, facts)
    RECORDS.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
    out = RECORDS / f"{Path(cases_path).stem}-{agent}-{stamp}.json"
    out.write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")
    passed = report(records, time.monotonic() - started)
    print(f"Full record: {out}")
    return 0 if passed else 1


def conversations(cases, cases_path, profile, navigator, chat, index, agent, facts, started) -> int:
    """Runs and scores every conversation, writes the record and prints the report."""
    records = []
    for number, conversation in enumerate(cases, start=1):
        print(f"[{number}/{len(cases)}] {conversation['id']}", flush=True)
        record = run_conversation(conversation, profile, navigator, chat, index, agent)
        records.append(record | {"score": score_conversation(record, facts)})
    RECORDS.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
    out = RECORDS / f"{Path(cases_path).stem}-{agent}-{stamp}.json"
    written = [{key: value for key, value in record.items() if key != "prompts"} for record in records]
    out.write_text(json.dumps(written, ensure_ascii=False, indent=2), encoding="utf-8")
    passed = conversation_report(records, time.monotonic() - started)
    print(f"Full record: {out}")
    return 0 if passed else 1


if __name__ == "__main__":
    args = sys.argv[1:]
    agent = args[args.index("--agent") + 1] if "--agent" in args else "workflow"
    paths = [arg for arg in args if arg not in ("--agent", agent)]
    sys.exit(main(Chat(), Embeddings(), paths[0], agent))
