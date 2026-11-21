"""The ReAct agent: the model reads every message in the conversation and picks each next step,
one tool call at a time ([D-032], [D-036]).

Chosen by ASSISTANT_AGENT=react in app.py, `main_debug.py --agent react` and
`tools/evaluate_agent.py --agent react`. The default is the workflow agent of agent_graph.py, which
this file does not change. A new message goes through the fast path first (door A, D-034): a secret
or a confirm request is refused and a cancel phrase cancels, all in code. Everything else goes to the
model, with the recent conversation: `think` asks it for {"thought", "tool", "arguments"}, held to
the tools this state allows, and `act` runs that tool in code (react_tools.py). The model replies to
chat, finds and starts a feature, asks which one is meant, sets a task's values, corrects a task at
its review, and cancels. A turn ends when something sets the reply (a reply, the review reached, a
cancel, a failure or a pause), or after STEP_BUDGET tool calls. Every question shown with options is
code's. No tool can confirm, submit or pay.

[D-032]: docs/decisions/0032-react-agent-beside-the-workflow.md
[D-036]: docs/decisions/0036-real-react-agent.md
"""

import json

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph

from assistant import replies
from assistant.agent_graph import build_graph
from assistant.agent_lifecycle import Lifecycle
from assistant.agent_nodes import REFUSALS, Nodes, Tools
from assistant.agent_state import AgentState
from assistant.conversation import kept, line
from assistant.lifecycle import FOLLOW_UP, fast
from assistant.models import ModelError
from assistant.react_tools import TEXTS, ReactTools
from assistant.slots import hidden
from assistant.steps import masked, show

STEP_BUDGET = 12  # tool calls in one turn, counted from the user's last answer
TOO_MANY_STEPS = "دستیار نتوانست این کار را در چند گام آماده کند؛ چیزی ارسال یا تأیید نشد."
AGENTS = ("workflow", "react")
REPEATED = "the same call as your last step: its result is above; do the next thing"
ASKING = ("ask_user", "ask_which")  # tools that wait for the user, so the same call again is fine
HEARD = {"answer", "picked", "switch"}  # a result that holds the user's newest words
REOPENING = ("set_values", "move_app", "open_review")  # acts that open a task at its review again
RULES = [
    "Rules:",
    "- Read the newest user line in the conversation and act on what it is:",
    "  - It asks to do or see something in the app, even while a task is open and even when it starts"
    " with «نه», «ولش» or «بیخیال» («نه، اول یه شارژ بخرم»): find_features with the request as one full"
    " sentence, then start_task with the feature it means. When it refers to an earlier request («همین»,"
    " «همون», «عین همین», «دوباره», «یکی دیگه»), the sentence copies that request's amount, card, account,"
    " number and person as the user wrote them, and changes only what the newest message changes"
    " («همینو برای سارا هم بزن» after «۵۰۰ هزار تومن از کارت 3566 به رضا کارت به کارت کن» is «۵۰۰ هزار"
    " تومن از کارت 3566 به سارا کارت به کارت کن»); never an amount, name or number the user"
    " did not write. A request to see, check or look"
    " up something the app shows (a statement, a toll, a rating, a balance, a list) is a task too.",
    "  - It gives values for the open task or answers its question: set_values.",
    "  - It only changes a value of the open task, even at its review, and asks for no task again:"
    " set_values with the new value, then open_review.",
    "  - A greeting, thanks, small talk, a question for information or advice (rates, fees, rules),"
    " something the app cannot do, or rude words: reply.",
    "  - It wants the open task stopped and asks for nothing else («ولش کن», «نمیخوام دیگه»): cancel_task."
    " Never cancel_task to start another task: start_task asks the user before leaving the open one.",
    "- Choosing a feature: read each found feature's title and about. Start the one the user's words"
    " fit best: where the money comes from («از حساب», «سپرده», «پس‌انداز», «کارت» is the bank;"
    " a feature that uses the wallet only when the user says «کیف»), a feature whose title the user"
    ' names ("named": true, such as «انتقال داخلی»), and what it is for. ask_which only when the words'
    " fit two or more features equally well and nothing in them tells them apart.",
    "- For the open task: when something is missing, ask_user (the app moves to the question's page"
    " first); when nothing is, open_review.",
    "- Never guess a value the user did not give, and never pick a record or an option for the user.",
    "- A reply is one or two short, warm Persian sentences. You prepare tasks in this app (transfers,"
    " bills, top-ups, loans, cards, cheques and more) up to a review page that only the user confirms,"
    " by touch. Say kindly what you cannot do here, and that you cannot answer questions about rates,"
    " fees or rules yet. Never give numbers, amounts, account details or advice, and never say anything"
    " was sent, paid, cancelled or confirmed.",
]
# Said with the open task at its review: the old follow-up's distinction, which held up (D-032).
AT_REVIEW = (
    "The task is prepared and at its review. The newest message is a new task when it asks for a task"
    " of its own: it names a kind of task, or asks for one again or for someone else («یکی دیگه»،"
    " «عین همین»، «دوباره»، «برای سارا هم»), even of the same kind: find_features, then start_task (code"
    " asks the user before leaving this task). It is a correction only when it just changes or adds a"
    " value of this task without asking for a task again («نه، ۳۰۰ هزار بشه»، «از اون یکی کارتم»):"
    " set_values."
)
WHERE = {"ready_for_user": " It is at its review.", "paused": " It is paused: the app was out of reach."}


class ReactState(AgentState):
    """The workflow's state, so the chat box, the panel and the evaluator read both agents alike,
    plus this turn's steps."""

    steps: list[dict]  # each {"thought", "tool", "arguments", "result"}, oldest first


def called(step: dict) -> tuple[str, str]:
    """A step's tool and arguments, to tell the same call made twice."""
    return step["tool"], json.dumps(step["arguments"], sort_keys=True, ensure_ascii=False)


def since_answer(steps: list[dict]) -> list[dict]:
    """The steps after the user's last answer: to a question, a feature pick or the switch."""
    heard = [index for index, step in enumerate(steps) if HEARD & step.get("result", {}).keys()]
    return steps[heard[-1] + 1 :] if heard else steps


def step_line(step: dict) -> str:
    """One step as the model reads it back: the call and its result."""
    called = json.dumps(step["arguments"], ensure_ascii=False)
    return f"- {step['tool']}({called}) → {json.dumps(step.get('result', {}), ensure_ascii=False)}"


def task_lines(state: ReactState, held: dict) -> list[str]:
    """The open task: its feature and where it is, its messages, the fields the model may set, the
    values held as the user reads them, and what is missing."""
    asks = {item["ref"]: item["ask"] for item in state["plan"]["requirements"]}
    missing = [f"{ref} («{asks[ref]}»)" for ref in held["missing"]]
    lines = [f"The open task: «{held['feature']}».{WHERE.get(state['task_state'], '')}"]
    lines += [AT_REVIEW] if state["task_state"] == "ready_for_user" else []
    lines += ["Its messages, oldest first:"]
    lines += [f"{index}. «{text}»" for index, text in enumerate(hidden(state["messages"]), start=1)]
    return lines + [
        f"Fields: {json.dumps(held['fields'], ensure_ascii=False)}",
        f"Held values: {json.dumps(held['held'], ensure_ascii=False)}",
        f"Missing: {', '.join(missing) or 'nothing'}",
    ]


def prompt(state: ReactState, names: list[str], held: dict | None) -> str:
    """The model's view: the rules, the tools it may call now, the recent conversation, the open
    task and the steps of this turn, all masked."""
    lines = [
        "You are the assistant inside a Persian mobile banking app. You talk with the user and prepare",
        "their tasks by calling one tool per step, up to the review page; only the user confirms it.",
        "Tools you can call now:",
    ]
    lines += [f"- {TEXTS[name]}" for name in names]
    lines += RULES
    lines += ["The conversation, oldest first (the newest user line is the one to act on):"]
    lines += [f"- {item['from']}: «{item['text']}»" for item in state["turns"]] or ["- none"]
    lines += task_lines(state, held) if held else ["The open task: none."]
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
        """Door A (D-034): a new message starts a turn with no steps, and goes into the history. The
        fast path refuses a secret (kept out of the history) or a confirm request, and a cancel
        phrase cancels the open task; the message then stays out of the task's messages."""
        text = state["messages"][-1]
        kind = fast(text)
        turns = kept(state["turns"], line("user", replies.REMOVED if kind == "secret" else text))
        base = {"steps": [], "turns": turns, "said": None, "lead": None}
        if kind is None:
            return base
        taken = {"messages": state["messages"][:-1] if state["task_state"] in FOLLOW_UP else []}
        if kind in REFUSALS:
            return base | taken | {"reply": REFUSALS[kind]}
        return base | taken | self.lifecycle.stopped(state)

    def ended(self, state: ReactState, reason: str) -> dict:
        """The turn ends with the reason. A task at its review or paused stays as it is; any other
        ends as failed."""
        if state["task_state"] in FOLLOW_UP:
            return {"reply": reason}
        return self.nodes.failed(state, reason)

    def think(self, state: ReactState) -> dict:
        """Asks the model for the next step, held to the tools this state allows."""
        if len(since_answer(state["steps"])) >= STEP_BUDGET:
            return self.ended(state, TOO_MANY_STEPS)
        tools = self.tools.available(state)
        held = self.tools.held(state) if self.tools.open(state) else None
        try:
            step = self.chat.step(prompt(state, list(tools), held), tools)
        except ModelError:
            return self.ended(state, replies.MODEL_FAILED)
        show("think", thought=step["thought"], tool=step["tool"], arguments=step["arguments"])
        return {"steps": state["steps"] + [step]}

    def act(self, state: ReactState) -> dict:
        """Runs the step's tool in code and keeps its result with the step. A question about a task
        whose app has not moved yet first moves it (D-029), as a step of its own, so the move is
        never run again when the answer resumes the question. An act on a task at its review or
        paused opens it again first."""
        step = state["steps"][-1]
        if (
            len(state["steps"]) > 1
            and step["tool"] not in ASKING
            and called(step) == called(state["steps"][-2])
        ):
            # The same call again changes nothing: the model is told so instead, and goes on.
            return {"steps": state["steps"][:-1] + [step | {"result": {"error": REPEATED}}]}
        if step["tool"] == "ask_user" and state["task_state"] == "resolving":
            result, update = self.tools.move_app(state, {})
            steps = state["steps"][:-1] + [step | {"tool": "move_app", "arguments": {}, "result": result}]
            if (state | update)["task_state"] == "preparing":
                steps.append({key: step[key] for key in ("thought", "tool", "arguments")})
            show("act", tool="move_app", result=result)
            return update | {"steps": steps}
        if step["tool"] == "reply" and state["task_state"] == "resolving" and state["missing"]:
            # Chat during a task: the reply is said with the task's question, asked at once.
            result, update = self.tools.reply(state, step["arguments"])
            asking = {"thought": step["thought"], "tool": "ask_user", "arguments": {"question": ""}}
            return update | {"steps": state["steps"][:-1] + [step | {"result": result}, asking]}
        before = {}
        if step["tool"] in REOPENING and state["task_state"] in FOLLOW_UP:
            before = self.tools.reopened(state)
            if before["task_state"] != "resolving":
                stopped = {"stopped": (state | before)["reply"]}
                return before | {"steps": state["steps"][:-1] + [step | {"result": stopped}]}
            if step["tool"] == "set_values":
                # A correction: the workflow's reader read the newest message as the task opened
                # again, so what it changes is the reader's, not the model's copy of it. Each value
                # the model named stands as the reader left it, and is reported back as held.
                values = self.tools.as_held(state | before, step["arguments"]["values"])
                step = step | {"arguments": {"values": values}}
        result, update = getattr(self.tools, step["tool"])(state | before, step["arguments"])
        show("act", tool=step["tool"], result=result)
        return before | update | {"steps": state["steps"][:-1] + [step | {"result": result}]}


def after_begin(state: ReactState) -> str:
    """The fast path's reply ends the turn; anything else goes to the model."""
    return END if state["reply"] is not None else "think"


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
    graph.add_node("think", agent.think)
    graph.add_node("act", agent.act)
    graph.add_edge(START, "begin")
    graph.add_conditional_edges("begin", after_begin, ["think", END])
    graph.add_conditional_edges("think", after_think, ["act", END])
    graph.add_conditional_edges("act", go_on, ["think", "act", END])
    return graph.compile(checkpointer=InMemorySaver())


def build_agent(tools: Tools, agent: str) -> CompiledStateGraph:
    """The agent the switch names: "workflow" (the default) or "react"."""
    if agent not in AGENTS:
        raise ValueError(f"ASSISTANT_AGENT must be one of {AGENTS}, not {agent!r}")
    return build_react_graph(tools) if agent == "react" else build_graph(tools)
