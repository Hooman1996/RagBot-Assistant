"""The ReAct agent: the model picks each next step itself, one tool call at a time ([D-032]).

Chosen by ASSISTANT_AGENT=react in app.py, `main_debug.py --agent react` and
`tools/evaluate_agent.py --agent react`. The default is the workflow agent of agent_graph.py, which
this file does not change. `think` asks the model for {"thought", "tool", "arguments"}, held to the
tools the state allows; `act` runs that tool in code (react_tools.py) and keeps its short result.
A turn ends when a tool sets the reply, or after STEP_BUDGET tool calls. A message to a task at its
review or paused goes to the workflow's own follow-up. No tool can confirm, submit or pay.

[D-032]: docs/decisions/0032-react-agent-beside-the-workflow.md
"""

import json

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph

from assistant import replies
from assistant.agent_graph import build_graph
from assistant.agent_lifecycle import Lifecycle
from assistant.agent_nodes import Nodes, Tools
from assistant.agent_state import AgentState
from assistant.lifecycle import FOLLOW_UP
from assistant.models import ModelError
from assistant.react_tools import TOOLS, ReactTools
from assistant.slots import hidden
from assistant.steps import masked, show

STEP_BUDGET = 12  # tool calls in one turn, counted from the user's last answer
TOO_MANY_STEPS = "دستیار نتوانست این کار را در چند گام آماده کند؛ چیزی ارسال یا تأیید نشد."
AGENTS = ("workflow", "react")
RULES = [
    "Rules:",
    "- Call finish to answer a greeting or thanks.",
    f"- A question asking for information or advice gets finish with «{replies.KNOWLEDGE}».",
    "- Call ask_user only when you cannot go on without the user's answer.",
    "- Never guess a value the user did not give.",
]


class ReactState(AgentState):
    """The workflow's state, so the chat box, the panel and the evaluator read both agents alike,
    plus this turn's steps."""

    steps: list[dict]  # each {"thought", "tool", "arguments", "result"}, oldest first


def since_answer(steps: list[dict]) -> list[dict]:
    """The steps after the user's last answer to ask_user."""
    asked = [index for index, step in enumerate(steps) if step["tool"] == "ask_user"]
    return steps[asked[-1] + 1 :] if asked else steps


def step_line(step: dict) -> str:
    """One step as the model reads it back: the call and its result."""
    called = json.dumps(step["arguments"], ensure_ascii=False)
    return f"- {step['tool']}({called}) → {json.dumps(step.get('result', {}), ensure_ascii=False)}"


def prompt(state: ReactState, names: list[str]) -> str:
    """The model's view: the rules, the tools it may call now, the user's words and its steps."""
    lines = [
        "You are the assistant inside a Persian mobile banking app. You work by calling one tool per",
        "step. You may prepare a task up to its review page; only the user confirms it, on the phone,",
        "so never say that anything was sent, paid or confirmed.",
        "Tools you can call now:",
    ]
    lines += [f"- {TOOLS[name].text}" for name in names]
    lines += RULES
    lines += ["The user's messages, oldest first:"]
    lines += [f"{index}. «{text}»" for index, text in enumerate(hidden(state["messages"]), start=1)]
    lines += ["Your steps so far:"] + ([step_line(step) for step in state["steps"]] or ["none"])
    lines += ["Write the thought in Persian, in one short sentence, then call one tool."]
    return masked("\n".join(lines))


class ReactAgent:
    """The loop's nodes, over the workflow's nodes and tools."""

    def __init__(self, tools: Tools) -> None:
        self.nodes = Nodes(tools)
        self.lifecycle = Lifecycle(self.nodes)
        self.tools = ReactTools(self.nodes)
        self.chat = tools.chat

    def begin(self, state: ReactState) -> dict:
        """A new message starts a new turn: its steps start empty."""
        return {"steps": []}

    def think(self, state: ReactState) -> dict:
        """Asks the model for the next step, held to the tools this state allows."""
        if len(since_answer(state["steps"])) >= STEP_BUDGET:
            return self.nodes.failed(state, TOO_MANY_STEPS)
        names = self.tools.available(state)
        try:
            step = self.chat.step(prompt(state, names), {name: TOOLS[name].arguments for name in names})
        except ModelError:
            return self.nodes.failed(state, replies.MODEL_FAILED)
        show("think", thought=step["thought"], tool=step["tool"], arguments=step["arguments"])
        return {"steps": state["steps"] + [step]}

    def act(self, state: ReactState) -> dict:
        """Runs the step's tool in code and keeps its result with the step."""
        step = state["steps"][-1]
        result, update = getattr(self.tools, step["tool"])(state, step["arguments"])
        show("act", tool=step["tool"], result=result)
        return update | {"steps": state["steps"][:-1] + [step | {"result": result}]}


def start(state: ReactState) -> str:
    """A message to a task at its review or paused is a follow-up; anything else is thought about."""
    return "follow_up" if state["task_state"] in FOLLOW_UP else "think"


def go_on(state: ReactState) -> str:
    """The turn ends once something set the reply; else the model thinks again."""
    return END if state["reply"] is not None else "think"


def after_think(state: ReactState) -> str:
    """The step is run, unless the turn already ended (the model failed, or too many steps)."""
    return END if state["reply"] is not None else "act"


def build_react_graph(tools: Tools) -> CompiledStateGraph:
    """Wires the loop. The in-memory checkpointer keeps each conversation by its thread_id."""
    agent = ReactAgent(tools)
    graph = StateGraph(ReactState)
    graph.add_node("begin", agent.begin)
    graph.add_node("follow_up", agent.lifecycle.follow_up)
    graph.add_node("think", agent.think)
    graph.add_node("act", agent.act)
    graph.add_edge(START, "begin")
    graph.add_conditional_edges("begin", start, ["follow_up", "think"])
    graph.add_conditional_edges("follow_up", go_on, ["think", END])
    graph.add_conditional_edges("think", after_think, ["act", END])
    graph.add_conditional_edges("act", go_on, ["think", END])
    return graph.compile(checkpointer=InMemorySaver())


def build_agent(tools: Tools, agent: str) -> CompiledStateGraph:
    """The agent the switch names: "workflow" (the default) or "react"."""
    if agent not in AGENTS:
        raise ValueError(f"ASSISTANT_AGENT must be one of {AGENTS}, not {agent!r}")
    return build_react_graph(tools) if agent == "react" else build_graph(tools)
