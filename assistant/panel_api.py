"""The replica panel's calls of contracts/openapi/assistant_mobile.json: a message, the history, cancel.

Called by the replica's assistant panel. Each message starts or resumes the agent for its conversation
on a background thread, as the graph page's chat box does, and the panel reads progress from the
history. The observation in a message is checked against the contract but not stored: the bridge
loop owns the device's observation. Nothing here can confirm, submit or pay.
"""

from typing import Annotated

from fastapi import APIRouter, Body, HTTPException

from assistant.agent_chat import AgentChat, Chats, TurnRefused
from assistant.mobile_api import require_valid


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
            "options": pending["options"]
        }
    return status(chat) if chat.task_state else None


def steps(chat: AgentChat) -> list[dict]:
    """The commands of the last run and how each ended, for the panel's progress."""
    report = chat.report or {"steps": []}
    return [{key: step[key] for key in ("operation", "screen", "outcome")} for step in report["steps"]]


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
            }
        return {
            "conversation_id": conversation_id,
            "busy": chat.busy(),
            "messages": chat.lines,
            "current": current(chat),
            "steps": steps(chat),
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
