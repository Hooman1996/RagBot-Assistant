"""The replica panel's calls of contracts/openapi/assistant_mobile.json: a message, the history, cancel.

Called by the replica's assistant panel. Each message starts or resumes the agent for its conversation
on a background thread, as the graph page's chat box does, and the panel reads progress from the
history, with the agent's own steps (agent_steps, also read by graph_page). The observation in a
message is checked against the contract but not stored: the bridge loop owns the device's
observation. Nothing here can confirm, submit or pay.
"""

import json
from typing import Annotated

from fastapi import APIRouter, Body, HTTPException

from assistant.agent_chat import AgentChat, Chats, TurnRefused
from assistant.mobile_api import require_valid
from assistant.steps import masked

RESULT_CHARACTERS = 160  # a step's result, cut short for the panel
# What each of the workflow agent's nodes does, as its steps show it.
NODE_WORDS = {
    "route": "خواندن پیام در گفت‌وگو",
    "switch": "پرسیدن درباره تغییر کار",
    "follow_up": "خواندن پیام پس از بازبینی",
    "understand": "تشخیص کار درخواستی",
    "reply": "پاسخ به کاربر",
    "plan": "برنامه‌ریزی مسیر در برنامه",
    "extract": "خواندن مقدارها از پیام",
    "check": "بررسی مقدارها",
    "resolve": "یافتن رکوردهای نام‌برده",
    "advance": "رفتن به صفحه پرسش",
    "clarify": "پرسش از کاربر",
    "prepare": "پر کردن فرم و رفتن به بازبینی",
    "verify": "بررسی صفحه بازبینی",
    "reconcile": "برنامه‌ریزی دوباره",
    "ready_for_user": "آماده برای تأیید کاربر",
}


def status(chat: AgentChat, state: str | None = None) -> dict:
    """The contract's status response for the conversation's task."""
    return {"kind": "status", "task_state": state or chat.task_state or "created"}


def current(chat: AgentChat) -> dict | None:
    """What the panel shows under the messages: the task while it runs, the question, or the task."""
    if chat.busy():
        return status(chat, chat.live_state())
    if chat.pending is not None:
        pending = chat.pending
        return {"kind": "clarification", "question_id": chat.question_id(), "text": pending["question"]} | {
            "options": pending["options"],
            "fields": pending.get("fields", []),
        }
    return status(chat) if chat.task_state else None


def steps(chat: AgentChat) -> list[dict]:
    """The commands of the last run and how each ended, for the panel's progress."""
    report = chat.report or {"steps": []}
    return [{key: step[key] for key in ("operation", "screen", "outcome")} for step in report["steps"]]


def brief(result: object) -> str:
    """A step's result as one short masked line; empty while the step still runs."""
    if result is None:
        return ""
    text = masked(json.dumps(result, ensure_ascii=False))
    return text if len(text) <= RESULT_CHARACTERS else text[: RESULT_CHARACTERS - 1] + "…"


def agent_steps(chat: AgentChat) -> list[dict]:
    """The agent's steps for the conversation's request, oldest first, each {thought, action,
    result}: the ReAct agent's own (its Persian thought, its tool and the tool's result), or the
    workflow's nodes (what each does, and the task state after it)."""
    config = {"configurable": {"thread_id": chat.conversation_id}}
    values = chat.graph.get_state(config).values
    if "steps" in values:
        return [
            {"thought": masked(step["thought"]), "action": step["tool"], "result": brief(step.get("result"))}
            for step in values["steps"]
        ]
    found = []
    after = None  # the checkpoint written after a node: newest first, so it comes before its node's
    for checkpoint in chat.graph.get_state_history(config):
        for node in checkpoint.next:
            if node in NODE_WORDS:
                done = after.values.get("task_state") if after else None
                found.append({"thought": NODE_WORDS[node], "action": node, "result": brief(done)})
        if checkpoint.metadata.get("source") == "input":
            break  # the request's first checkpoint
        after = checkpoint
    return found[::-1]


def build_panel_router(chats: Chats) -> APIRouter:
    """Builds the panel's routes around the backend's conversations."""
    router = APIRouter(prefix="/assistant/v1")

    @router.post("/messages")
    def send_message(request: Annotated[dict, Body()]) -> dict:
        """Starts or resumes the agent with the user's words, or an option of the pending question."""
        require_valid("conversation_request.schema.json", request)
        chat = chats.panel(request["conversation_id"])
        try:
            if "selection" in request:
                if request["selection"]["question_id"] != chat.question_id():
                    raise TurnRefused("stale_question")
                chat.start(None, request["selection"]["ref"])
            else:
                chat.start(request["message"], None)
        except TurnRefused as error:
            raise HTTPException(status_code=409, detail=str(error)) from error
        chats.latest = chat
        return status(chat)

    @router.get("/history")
    def history(conversation_id: str) -> dict:
        """The conversation as the panel shows it on reopening: messages, question or task, progress."""
        chat = chats.panels.get(conversation_id)
        if chat is None:
            return {
                "conversation_id": conversation_id,
                "busy": False,
                "messages": [],
                "current": None,
                "steps": [],
                "agent_steps": [],
            }
        return {
            "conversation_id": conversation_id,
            "busy": chat.busy(),
            "messages": chat.lines,
            "current": current(chat),
            "steps": steps(chat),
            "agent_steps": agent_steps(chat),
        }

    @router.post("/sessions/{session_id}/cancel")
    def cancel(session_id: str) -> dict:
        """Cancels the task the panel's user wrote about last: stops a run, or answers with «لغو»."""
        chat = chats.latest
        if chat is None:
            raise HTTPException(status_code=409, detail="nothing_to_cancel")
        try:
            chat.cancel()
        except TurnRefused as error:
            raise HTTPException(status_code=409, detail=str(error)) from error
        return status(chat, chat.live_state())

    return router
