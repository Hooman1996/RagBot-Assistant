"""The agent: one LangGraph StateGraph from a Persian sentence to the review, and no further. Before
each question the app is moved to the screen the question is about. Every message is read in the
conversation first (D-034): a new one at route (door A), an answer inside its waiting node (door B).

Built by app.py for the graph page's chat box, and by main_debug.py with a stand-in app. The nodes
are in agent_nodes.py and agent_lifecycle.py; this file only wires them. A paused question, and a
task at its review or paused, keep their conversation in the in-memory checkpointer, by thread_id.
No edge leads past the review: only the user confirms.
"""

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph

from assistant.agent_lifecycle import Lifecycle
from assistant.agent_nodes import Nodes, Tools
from assistant.agent_state import AgentState
from assistant.intent import KNOWLEDGE, NOT_SURE


def after_route(state: AgentState) -> str:
    """Where the route sends the message: a new request is understood, a corrected or resumed task
    read again, a new task while one is open asks the switch question; a reply ends the turn."""
    return state["route"] if state["route"] in ("understand", "extract", "switch") else END


def after_switch(state: AgentState) -> str:
    """«بله» plans the new task, «نه» asks the open question again, any other answer asks again; a
    reply (a cancel, or the review kept) ends the turn."""
    return state["route"] if state["route"] in ("plan", "understand", "clarify", "switch") else END


def after_clarify(state: AgentState) -> str:
    """The answer is read; a refusal or chat asks again; a new task asks the switch question; a cancel
    ends."""
    if state["task_state"] == "cancelled":
        return END
    return state["route"] if state["route"] in ("clarify", "switch") else "extract"


def after_understand(state: AgentState) -> str:
    """A feature is planned, a question or "not sure" is replied to, and a failure ends."""
    if state["task_state"] == "failed":
        return END
    return "reply" if state["chosen"] in (KNOWLEDGE, NOT_SURE) else "plan"


def after_reply(state: AgentState) -> str:
    """A feature picked from the options is planned, new words are understood again, a refusal or chat
    asks again, else it ends."""
    if state["task_state"] == "cancelled":
        return END
    if state["route"] == "reply":
        return "reply"
    if state["chosen"] is None:
        return "understand"
    return END if state["chosen"] == KNOWLEDGE else "plan"


def after_plan(state: AgentState) -> str:
    """On to extract, unless no allowed route exists."""
    return END if state["task_state"] == "failed" else "extract"


def after_extract(state: AgentState) -> str:
    """On to check, unless the model failed."""
    return END if state["task_state"] == "failed" else "check"


def after_verify(state: AgentState) -> str:
    """On to ready_for_user; an uncertain command is reconciled; a failure, cancel or pause ends."""
    if state["task_state"] == "reconciling":
        return "reconcile"
    return "ready_for_user" if state["task_state"] == "verifying_review" else END


def after_reconcile(state: AgentState) -> str:
    """Read again and prepared once more, unless it failed."""
    return END if state["task_state"] == "failed" else "extract"


def until_complete(state: AgentState) -> str:
    """Back to a question while something is missing, else on to the next step."""
    return "ask" if state["missing"] else "next"


def after_advance(state: AgentState) -> str:
    """To the question once the app waits on its screen; an uncertain command is reconciled; a
    failure, cancel or pause ends."""
    if state["task_state"] == "reconciling":
        return "reconcile"
    return "clarify" if state["task_state"] == "preparing" else END


def build_graph(tools: Tools) -> CompiledStateGraph:
    """Wires the nodes. The in-memory checkpointer keeps each conversation by its thread_id."""
    nodes = Nodes(tools)
    lifecycle = Lifecycle(nodes)
    graph = StateGraph(AgentState)
    graph.add_node("route", lifecycle.route)
    graph.add_node("switch", lifecycle.switch)
    graph.add_node("reconcile", lifecycle.reconcile)
    graph.add_node("understand", nodes.understand)
    graph.add_node("plan", nodes.plan)
    graph.add_node("extract", nodes.extract)
    graph.add_node("check", nodes.check)
    graph.add_node("advance", nodes.advance)
    graph.add_node("clarify", nodes.clarify)
    graph.add_node("resolve", nodes.resolve)
    graph.add_node("prepare", nodes.prepare)
    graph.add_node("verify", nodes.verify)
    graph.add_node("ready_for_user", nodes.ready_for_user)
    graph.add_node("reply", nodes.reply)

    graph.add_edge(START, "route")
    graph.add_conditional_edges("route", after_route, ["understand", "extract", "switch", END])
    graph.add_conditional_edges("switch", after_switch, ["plan", "understand", "clarify", "switch", END])
    graph.add_conditional_edges("understand", after_understand, ["plan", "reply", END])
    graph.add_conditional_edges("reply", after_reply, ["understand", "plan", "reply", END])
    graph.add_conditional_edges("plan", after_plan, ["extract", END])
    graph.add_conditional_edges("extract", after_extract, ["check", END])
    graph.add_conditional_edges("check", until_complete, {"ask": "advance", "next": "resolve"})
    graph.add_conditional_edges("advance", after_advance, ["clarify", "reconcile", END])
    graph.add_conditional_edges("clarify", after_clarify, ["extract", "clarify", "switch", END])
    graph.add_conditional_edges("resolve", until_complete, {"ask": "advance", "next": "prepare"})
    graph.add_edge("prepare", "verify")
    graph.add_conditional_edges("verify", after_verify, ["ready_for_user", "reconcile", END])
    graph.add_conditional_edges("reconcile", after_reconcile, ["extract", END])
    graph.add_edge("ready_for_user", END)
    return graph.compile(checkpointer=InMemorySaver())
