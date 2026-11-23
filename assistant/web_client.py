"""Read-only data used by the standalone browser executor."""

from datetime import date
from pathlib import Path
from typing import Literal

from fastapi import APIRouter, Depends, FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles

from assistant.http_lookups import HttpLookups
from assistant.lookups import DIGITS, LookupUnavailable
from assistant.mobile_api import standalone_session
from assistant.words import options
from mock_services.lookup_models import Statement


def mount_web_client(app: FastAPI, profile: dict, lookups: HttpLookups) -> None:
    router = APIRouter(prefix="/assistant/v1/web", dependencies=[Depends(standalone_session)])

    @router.get("/profile")
    def graph() -> dict:
        return profile

    @router.get("/sources/{operation}")
    def sources(operation: Literal["accounts", "cards"]) -> dict[str, str]:
        try:
            return {
                row["key"]: "****" + row["number"].translate(DIGITS)[-4:]
                for row in lookups.records(operation)
            }
        except LookupUnavailable as error:
            raise HTTPException(503, "lookup_unavailable") from error

    @router.get("/reasons")
    def reasons() -> dict[str, str]:
        return {item["key"]: item["label"] for item in options("S-192", "account-transfer-reason", lookups)}

    @router.get("/statement")
    def statement(account: str, start: date, end: date) -> Statement:
        if start > end:
            raise HTTPException(422, "invalid_range")
        try:
            return lookups.statement(account, start, end)
        except LookupUnavailable as error:
            raise HTTPException(503, "lookup_unavailable") from error

    app.include_router(router)
    platform = Path(__file__).resolve().parent.parent / "negahorbit-platform"
    app.mount("/", StaticFiles(directory=platform, html=True))
