"""The two calls of contracts/openapi/assistant_mobile.json that the replica's loop uses.

Called by the app's CommandPoller. Every body is checked against the contract before it is used.
The panel's calls of the same API are in panel_api.py. There is no route that submits,
confirms or pays, and there must never be one.
"""

from collections.abc import Callable
from typing import Annotated

from fastapi import APIRouter, Body, Depends, Header, HTTPException

from assistant.command_queue import CommandQueue, StaleResult, UnknownCommand
from assistant.contract_check import problems

IDLE_REPLY = {"kind": "status", "task_state": "paused"}
DEVICE_ID = "replica-dev"
SessionHeader = Annotated[str, Header(alias="X-Assistant-Session", min_length=1)]


def standalone_session(session_header: SessionHeader) -> str:
    """The standalone client identifies its session on every call."""
    return session_header


def development_session(session_header: SessionHeader = DEVICE_ID) -> str:
    """The development replica already uses this fixed local session."""
    return session_header


def require_session(caller: str, target: str) -> None:
    """Checks ownership before a request can reach a session's state."""
    if caller != target:
        raise HTTPException(status_code=403, detail="session_mismatch")


def reply_with(command: dict | None) -> dict:
    """Wraps the next command in the contract's response, or says there is nothing to do."""
    if command is None:
        return IDLE_REPLY
    return {"kind": "command", "command": command}


def require_valid(schema_file: str, value: dict) -> None:
    """Refuses a body the contract does not allow."""
    found = problems(schema_file, value)
    if found:
        raise HTTPException(status_code=422, detail=found)


def build_mobile_router(queue: CommandQueue, session: Callable[..., str] = development_session) -> APIRouter:
    """Builds the app's routes around one queue."""
    router = APIRouter(prefix="/assistant/v1")

    @router.post("/sessions/{session_id}/resume")
    def resume(
        session_id: str, observation: Annotated[dict, Body()], caller: Annotated[str, Depends(session)]
    ) -> dict:
        """Records an observation and polls for that session's command."""
        require_valid("observation.schema.json", observation)
        require_session(caller, session_id)
        return reply_with(queue.next_command(session_id, observation))

    @router.post("/results")
    def post_result(result: Annotated[dict, Body()], caller: Annotated[str, Depends(session)]) -> dict:
        """The app reports a command's result, with a fresh observation."""
        require_valid("result.schema.json", result)
        try:
            queue.record_result(result, session_id=caller)
        except UnknownCommand as error:
            raise HTTPException(status_code=409, detail="unknown_command") from error
        except StaleResult as error:
            raise HTTPException(status_code=409, detail="stale_result") from error
        return IDLE_REPLY

    return router
