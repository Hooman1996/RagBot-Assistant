"""The ReAct agent: the model picks each next step itself, one tool call at a time ([D-032]).

Chosen by ASSISTANT_AGENT=react in app.py, `main_debug.py --agent react` and
`tools/evaluate_agent.py --agent react`. The default is the workflow agent of agent_graph.py, which
this file does not change. `think` asks the model for {"thought", "tool", "arguments"}, held to the
tools the state allows; `act` runs that tool in code (react_tools.py) and keeps its short result.
A turn ends when a tool sets the reply (a finish, the review reached, a cancel, a failure or a
pause), or after STEP_BUDGET tool calls. A message to a task at its review or paused goes to the
workflow's own follow-up. No tool can confirm, submit or pay.

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
from assistant.react_tools import TEXTS, ReactTools
from assistant.slots import hidden
from assistant.steps import masked, show

STEP_BUDGET = 12  # tool calls in one turn, counted from the user's last answer
TOO_MANY_STEPS = "دستیار نتوانست این کار را در چند گام آماده کند؛ چیزی ارسال یا تأیید نشد."
AGENTS = ("workflow", "react")
RULES = [
    "Rules:",
    "- For anything the user wants to do or see in the app: find_features, then feature_requirements",
    "  with the one feature that fits, then set_values with every value the user gave.",
    "- When something is missing: move_app, then ask_user. When nothing is missing: open_review.",
    "- After the user answers, set_values with what the answer gives.",
    "- Never guess a value the user did not give, and never pick a record or an option for the user.",
    "- Call finish to answer a greeting or thanks.",
    f"- A question asking for information or advice gets finish with «{replies.KNOWLEDGE}».",
    "- Without a task, call ask_user only when you cannot go on without the user's answer.",
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


def task_lines(state: ReactState, held: dict | None) -> list[str]:
    """The task so far: its feature, the values held as the user reads them, and what is missing."""
    if held is None:
        return ["The task: none yet."]
    asks = {item["ref"]: item["ask"] for item in state["plan"]["requirements"]}
    missing = [f"{ref} («{asks[ref]}»)" for ref in held["missing"]]
    return [
        f"The task: «{held['feature']}». Held values: {json.dumps(held['held'], ensure_ascii=False)}",
        f"Missing: {', '.join(missing) or 'nothing'}",
    ]


def prompt(state: ReactState, names: list[str], held: dict | None) -> str:
    """The model's view: the rules, the tools it may call now, the user's words, the task and the
    steps of this turn."""
    lines = [
        "You are the assistant inside a Persian mobile banking app. You work by calling one tool per",
        "step. You may prepare a task up to its review page; only the user confirms it, on the phone,",
        "so never say that anything was sent, paid or confirmed.",
        "Tools you can call now:",
    ]
    lines += [f"- {TEXTS[name]}" for name in names]
    lines += RULES
    lines += ["The user's messages, oldest first:"]
    lines += [f"{index}. «{text}»" for index, text in enumerate(hidden(state["messages"]), start=1)]
    lines += task_lines(state, held)
    lines += ["Your steps so far:"] + ([step_line(step) for step in state["steps"]] or ["none"])
    lines += ["Write the thought in Persian, in one short sentence, then call one tool."]
    return masked("\n".join(lines))


class ReactAgent:
    """The loop's nodes, over the workflow's nodes and tools."""

    def __init__(self, tools: Tools) -> None:
        self.nodes = Nodes(tools)
        self.lifecycle = Lifecycle(self.nodes)
        self.tools = ReactTools(self.nodes, self.lifecycle)
        self.chat = tools.chat

    def begin(self, state: ReactState) -> dict:
        """A new message starts a new turn: its steps start empty."""
        return {"steps": []}

    def follow_up(self, state: ReactState) -> dict:
        """The workflow's follow-up: cancel, resume, a correction or a new request. A correction is
        read again by the workflow's own reader, as in the workflow, and a task going on is settled
        at once, so the model sees what it holds and what is missing."""
        update = self.lifecycle.follow_up(state)
        task = state | update
        if task["task_state"] != "resolving":
            return update
        if state["task_state"] == "ready_for_user":  # a correction: every value is read again
            update["values"] = task["values"] | self.tools.read(task, {})
        return update | self.tools.settle(state | update)

    def think(self, state: ReactState) -> dict:
        """Asks the model for the next step, held to the tools this state allows."""
        if len(since_answer(state["steps"])) >= STEP_BUDGET:
            return self.nodes.failed(state, TOO_MANY_STEPS)
        tools = self.tools.available(state)
        held = self.tools.held(state) if state["plan"] else None
        try:
            step = self.chat.step(prompt(state, list(tools), held), tools)
        except ModelError:
            return self.nodes.failed(state, replies.MODEL_FAILED)
        show("think", thought=step["thought"], tool=step["tool"], arguments=step["arguments"])
        return {"steps": state["steps"] + [step]}

    def act(self, state: ReactState) -> dict:
        """Runs the step's tool in code and keeps its result with the step. A question about a task
        whose app has not moved yet first moves it (D-029), as a step of its own, so the move is
        never run again when the answer resumes the question."""
        step = state["steps"][-1]
        if step["tool"] == "ask_user" and state["task_state"] == "resolving":
            result, update = self.tools.move_app(state, {})
            steps = state["steps"][:-1] + [step | {"tool": "move_app", "arguments": {}, "result": result}]
            if (state | update)["task_state"] == "preparing":
                steps.append({key: step[key] for key in ("thought", "tool", "arguments")})
            show("act", tool="move_app", result=result)
            return update | {"steps": steps}
        result, update = getattr(self.tools, step["tool"])(state, step["arguments"])
        show("act", tool=step["tool"], result=result)
        return update | {"steps": state["steps"][:-1] + [step | {"result": result}]}


def start(state: ReactState) -> str:
    """A message to a task at its review or paused is a follow-up; anything else is thought about."""
    return "follow_up" if state["task_state"] in FOLLOW_UP else "think"


def go_on(state: ReactState) -> str:
    """The turn ends once something set the reply; a question waiting on its moved app is asked;
    else the model thinks again."""
    if state["reply"] is not None:
        return END
    return "act" if state["steps"] and "result" not in state["steps"][-1] else "think"


def after_think(state: ReactState) -> str:
    """The step is run, unless the turn already ended (the model failed, or too many steps)."""
    return END if state["reply"] is not None else "act"


def build_react_graph(tools: Tools) -> CompiledStateGraph:
    """Wires the loop. The in-memory checkpointer keeps each conversation by its thread_id."""
    agent = ReactAgent(tools)
    graph = StateGraph(ReactState)
    graph.add_node("begin", agent.begin)
    graph.add_node("follow_up", agent.follow_up)
    graph.add_node("think", agent.think)
    graph.add_node("act", agent.act)
    graph.add_edge(START, "begin")
    graph.add_conditional_edges("begin", start, ["follow_up", "think"])
    graph.add_conditional_edges("follow_up", go_on, ["think", END])
    graph.add_conditional_edges("think", after_think, ["act", END])
    graph.add_conditional_edges("act", go_on, ["think", "act", END])
    return graph.compile(checkpointer=InMemorySaver())


def build_agent(tools: Tools, agent: str) -> CompiledStateGraph:
    """The agent the switch names: "workflow" (the default) or "react"."""
    if agent not in AGENTS:
        raise ValueError(f"ASSISTANT_AGENT must be one of {AGENTS}, not {agent!r}")
    return build_react_graph(tools) if agent == "react" else build_graph(tools)
