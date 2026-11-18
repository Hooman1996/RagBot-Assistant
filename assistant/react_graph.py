"""The ReAct agent: code understands the request, then the model picks each next step for the task,
one tool call at a time ([D-032], [D-033]).

Chosen by ASSISTANT_AGENT=react in app.py, `main_debug.py --agent react` and
`tools/evaluate_agent.py --agent react`. The default is the workflow agent of agent_graph.py, which
this file does not change. A new request is first understood by the workflow's own code: a greeting,
thanks or a question about what the assistant does gets a fixed reply; TEI and the workflow's chooser
pick the feature, `not_sure` (the 4 closest as options) or a knowledge question; a feature is
planned, and a page to see is opened at once. Then `think` asks the model for {"thought", "tool",
"arguments"}, held to the tools the task allows, and `act` runs that tool in code (react_tools.py).
A turn ends when something sets the reply (the review reached, a cancel, a failure or a pause), or
after STEP_BUDGET tool calls. A message to a task at its review or paused goes to the workflow's own
follow-up. Every question the user sees is code's. No tool can confirm, submit or pay.

[D-032]: docs/decisions/0032-react-agent-beside-the-workflow.md
[D-033]: docs/decisions/0033-react-uses-the-workflows-feature-choice.md
"""

import json
import re

from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.graph.state import CompiledStateGraph
from langgraph.types import interrupt

from assistant import replies
from assistant.agent_graph import build_graph
from assistant.agent_lifecycle import Lifecycle
from assistant.agent_nodes import Nodes, Tools
from assistant.agent_state import AgentState, moved
from assistant.intent import KNOWLEDGE, NOT_SURE
from assistant.lifecycle import CANCEL, FOLLOW_UP, wants_cancel
from assistant.lookups import plain
from assistant.models import ModelError
from assistant.react_tools import TEXTS, ReactTools
from assistant.slots import hidden
from assistant.steps import masked, show

STEP_BUDGET = 12  # tool calls in one turn, counted from the user's last answer
TOO_MANY_STEPS = "دستیار نتوانست این کار را در چند گام آماده کند؛ چیزی ارسال یا تأیید نشد."
AGENTS = ("workflow", "react")
REPEATED = "the same call as your last step: its result is above; do the next thing"
NOT_SURE_OPTIONS = 4  # the closest features offered when the request is not clear (owner, 18 Nov 2026)
GREETING = "سلام! بنویسید چه کاری انجام دهم؛ مثلاً «۵۰۰ هزار تومن به رضا کارت به کارت کن»."
THANKS = "خواهش می‌کنم. هر وقت کاری داشتید بنویسید."
HELP = (
    "کارهای برنامه را تا صفحه بازبینی آماده می‌کنم و تأیید همیشه با خود شماست. مثلاً: «۵۰۰ هزار تومن"
    " به رضا کارت به کارت کن»، «قسط بعدی وامم را از پس‌انداز بده»، «عوارض آلودگی هوای ماشینم را"
    " ببین»، «برو به خانه»."
)
UNDERSTOOD = "تشخیص کار درخواستی"
ASKED = "پرسیدن منظور کاربر"
OPENED = "باز کردن صفحه"
RULES = [
    "Rules:",
    "- The task's feature is already chosen: work only on it.",
    "- First set_values with every value the user gave.",
    "- When something is missing: move_app, then ask_user. When nothing is missing: open_review.",
    "- After the user answers, set_values with what the answer gives.",
    "- Never guess a value the user did not give, and never pick a record or an option for the user.",
]


def spoken(text: str) -> str:
    """The words of a message as the fixed lists hold them: plain, without punctuation."""
    return " ".join(re.sub(r"[^\w\s]", " ", plain(text)).split())


# Whole messages that get a fixed reply instead of the chooser, which calls them not_sure.
GREETINGS = {spoken(text) for text in ("سلام", "سلام خوبی", "سلام وقت بخیر", "درود", "وقت بخیر",
             "صبح بخیر", "عصر بخیر", "شب بخیر", "سلام دستیار")}  # fmt: skip
THANKS_WORDS = {spoken(text) for text in ("ممنون", "ممنونم", "مرسی", "متشکرم", "خیلی ممنون", "تشکر",
                "ممنون از کمکت", "مرسی، خیلی کمک کردی", "دستت درد نکنه", "دمت گرم", "خداحافظ")}  # fmt: skip
HELP_ASKS = {spoken(text) for text in ("چیکار میتونی بکنی", "چه کار میتونی بکنی", "چیکارا میتونی بکنی",
             "چه کارهایی میتونی انجام بدی", "چه کمکی میتونی بکنی", "چه کمکی از دستت برمیاد", "چیکار بلدی",
             "تو کی هستی", "تو چی هستی", "راهنما", "راهنمایی", "چطوری ازت استفاده کنم")}  # fmt: skip
FIXED = ((GREETINGS, GREETING), (THANKS_WORDS, THANKS), (HELP_ASKS, HELP))


class ReactState(AgentState):
    """The workflow's state, so the chat box, the panel and the evaluator read both agents alike,
    plus this turn's steps."""

    steps: list[dict]  # each {"thought", "tool", "arguments", "result"}, oldest first


def called(step: dict) -> tuple[str, str]:
    """A step's tool and arguments, to tell the same call made twice."""
    return step["tool"], json.dumps(step["arguments"], sort_keys=True, ensure_ascii=False)


def since_answer(steps: list[dict]) -> list[dict]:
    """The steps after the user's last answer to ask_user."""
    asked = [index for index, step in enumerate(steps) if step["tool"] == "ask_user"]
    return steps[asked[-1] + 1 :] if asked else steps


def step_line(step: dict) -> str:
    """One step as the model reads it back: the call and its result."""
    called = json.dumps(step["arguments"], ensure_ascii=False)
    return f"- {step['tool']}({called}) → {json.dumps(step.get('result', {}), ensure_ascii=False)}"


def code_step(thought: str, tool: str, result: dict) -> dict:
    """A step code took itself, shown with the model's steps."""
    return {"thought": thought, "tool": tool, "arguments": {}, "result": result}


def task_lines(state: ReactState, held: dict) -> list[str]:
    """The task: its feature, the fields the model may set, the values held as the user reads them,
    and what is missing."""
    asks = {item["ref"]: item["ask"] for item in state["plan"]["requirements"]}
    missing = [f"{ref} («{asks[ref]}»)" for ref in held["missing"]]
    return [
        f"The task: «{held['feature']}».",
        f"Fields: {json.dumps(held['fields'], ensure_ascii=False)}",
        f"Held values: {json.dumps(held['held'], ensure_ascii=False)}",
        f"Missing: {', '.join(missing) or 'nothing'}",
    ]


def prompt(state: ReactState, names: list[str], held: dict) -> str:
    """The model's view: the rules, the tools it may call now, the user's words, the task and the
    steps of this turn."""
    lines = [
        "You are the assistant inside a Persian mobile banking app. You prepare the user's task by",
        "calling one tool per step, up to its review page; only the user confirms it, on the phone.",
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

    def understand(self, state: ReactState) -> dict:
        """A new request, understood by code: a greeting, thanks or a question about what the
        assistant does gets its fixed reply; else the workflow's understand (TEI's closest 8, then its
        chooser) picks a feature, not_sure or a knowledge question, which gets the workflow's reply."""
        said = spoken(state["messages"][-1])
        for words, reply in FIXED:
            if said in words:
                return {"reply": reply, "steps": state["steps"] + [code_step(UNDERSTOOD, "understand", {})]}
        update = self.nodes.understand(state)
        chosen = update.get("chosen")
        if chosen == KNOWLEDGE:
            update["reply"] = replies.KNOWLEDGE
        shown = self.nodes.features[chosen]["title"] if chosen in self.nodes.features else chosen
        return update | {"steps": state["steps"] + [code_step(UNDERSTOOD, "understand", {"chosen": shown})]}

    def not_sure(self, state: ReactState) -> dict:
        """The workflow's not-sure question with the 4 closest features and «لغو». A picked feature is
        planned, «لغو» cancels, and other words are understood again as the request."""
        shown = state["offered"][:NOT_SURE_OPTIONS]
        options = [{"ref": intent, "label": self.nodes.features[intent]["title"]} for intent in shown]
        pending = {"question": replies.NOT_SURE, "options": options + [CANCEL], "fields": []}
        answer = interrupt(pending | {"task_state": state["task_state"]})
        if wants_cancel(answer):
            return moved(state, "cancelled") | {"reply": replies.CANCELLED}
        if answer in shown:
            step = code_step(ASKED, "ask_user", {"picked": self.nodes.features[answer]["title"]})
            return {"chosen": answer, "steps": state["steps"] + [step]}
        step = code_step(ASKED, "ask_user", {"answer": hidden([answer])[0]})
        return {"chosen": None, "messages": state["messages"] + [answer], "steps": state["steps"] + [step]}

    def plan(self, state: ReactState) -> dict:
        """The workflow's plan of the chosen feature. A feature with nothing for the model to set (a
        page to see) is opened at once, with no model step."""
        update = self.nodes.plan(state)
        task = state | update
        if task["task_state"] != "resolving" or task["missing"] or self.tools.settable(task):
            return update
        result, opened = self.tools.open_review(task, {})
        return update | opened | {"steps": task["steps"] + [code_step(OPENED, "open_review", result)]}

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
        held = self.tools.held(state)
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
        if (
            len(state["steps"]) > 1
            and step["tool"] != "ask_user"
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
        result, update = getattr(self.tools, step["tool"])(state, step["arguments"])
        show("act", tool=step["tool"], result=result)
        return update | {"steps": state["steps"][:-1] + [step | {"result": result}]}


def start(state: ReactState) -> str:
    """A message to a task at its review or paused is a follow-up; anything else is a new request."""
    return "follow_up" if state["task_state"] in FOLLOW_UP else "understand"


def after_follow_up(state: ReactState) -> str:
    """A new request is understood; a corrected or resumed task goes on; a cancel or failure ends."""
    if state["reply"] is not None:
        return END
    return "understand" if state["task_state"] == "created" else "think"


def after_understand(state: ReactState) -> str:
    """A feature is planned, a request not clear is asked about, and a reply ends the turn."""
    if state["reply"] is not None:
        return END
    return "not_sure" if state["chosen"] == NOT_SURE else "plan"


def after_not_sure(state: ReactState) -> str:
    """A picked feature is planned; new words are understood again; a cancel ends."""
    if state["reply"] is not None:
        return END
    return "understand" if state["chosen"] is None else "plan"


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
    graph.add_node("understand", agent.understand)
    graph.add_node("not_sure", agent.not_sure)
    graph.add_node("plan", agent.plan)
    graph.add_node("think", agent.think)
    graph.add_node("act", agent.act)
    graph.add_edge(START, "begin")
    graph.add_conditional_edges("begin", start, ["follow_up", "understand"])
    graph.add_conditional_edges("follow_up", after_follow_up, ["understand", "think", END])
    graph.add_conditional_edges("understand", after_understand, ["not_sure", "plan", END])
    graph.add_conditional_edges("not_sure", after_not_sure, ["understand", "plan", END])
    graph.add_conditional_edges("plan", go_on, ["think", END])
    graph.add_conditional_edges("think", after_think, ["act", END])
    graph.add_conditional_edges("act", go_on, ["think", "act", END])
    return graph.compile(checkpointer=InMemorySaver())


def build_agent(tools: Tools, agent: str) -> CompiledStateGraph:
    """The agent the switch names: "workflow" (the default) or "react"."""
    if agent not in AGENTS:
        raise ValueError(f"ASSISTANT_AGENT must be one of {AGENTS}, not {agent!r}")
    return build_react_graph(tools) if agent == "react" else build_graph(tools)
