"""The agent: one LangGraph StateGraph from a Persian sentence to the review, and no further.

Built by app.py for the graph page's chat box, and by main_debug.py with a stand-in app. The nodes
are in agent_nodes.py; this file only wires them. A paused question keeps its conversation in the
in-memory checkpointer, by thread_id. No edge leads past the review: only the user confirms.
"""

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph

from assistant.agent_nodes import Nodes, Tools
from assistant.agent_state import AgentState
from assistant.intent import KNOWLEDGE, NOT_SURE


def after_understand(state: AgentState) -> str:
    """A feature is planned, a question or "not sure" is replied to, and a failure ends."""
    if state["task_state"] == "failed":
        return END
    return "reply" if state["chosen"] in (KNOWLEDGE, NOT_SURE) else "plan"


def after_reply(state: AgentState) -> str:
    """A feature picked from the options is planned, new words are understood again, else it ends."""
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
    """On to ready_for_user, unless the app is not where it should be."""
    return END if state["task_state"] == "failed" else "ready_for_user"


def until_complete(state: AgentState) -> str:
    """Back to clarify while something is missing, else on to the next step."""
    return "clarify" if state["missing"] else "next"


def build_graph(tools: Tools) -> CompiledStateGraph:
    """Wires the nodes. The in-memory checkpointer keeps each conversation by its thread_id."""
    nodes = Nodes(tools)
    graph = StateGraph(AgentState)
    graph.add_node("understand", nodes.understand)
    graph.add_node("plan", nodes.plan)
    graph.add_node("extract", nodes.extract)
    graph.add_node("check", nodes.check)
    graph.add_node("clarify", nodes.clarify)
    graph.add_node("resolve", nodes.resolve)
    graph.add_node("prepare", nodes.prepare)
    graph.add_node("verify", nodes.verify)
    graph.add_node("ready_for_user", nodes.ready_for_user)
    graph.add_node("reply", nodes.reply)

    graph.add_edge(START, "understand")
    graph.add_conditional_edges("understand", after_understand, ["plan", "reply", END])
    graph.add_conditional_edges("reply", after_reply, ["understand", "plan", END])
    graph.add_conditional_edges("plan", after_plan, ["extract", END])
    graph.add_conditional_edges("extract", after_extract, ["check", END])
    graph.add_conditional_edges("check", until_complete, {"clarify": "clarify", "next": "resolve"})
    graph.add_edge("clarify", "extract")
    graph.add_conditional_edges("resolve", until_complete, {"clarify": "clarify", "next": "prepare"})
    graph.add_edge("prepare", "verify")
    graph.add_conditional_edges("verify", after_verify, ["ready_for_user", END])
    graph.add_edge("ready_for_user", END)
    return graph.compile(checkpointer=InMemorySaver())
