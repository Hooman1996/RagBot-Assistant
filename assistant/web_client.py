"""Read-only data used by the standalone browser executor."""

from datetime import date
from hashlib import sha256
from pathlib import Path
from typing import Literal

from fastapi import APIRouter, Depends, FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles

from assistant.http_lookups import HttpLookups
from assistant.lookups import DIGITS, LookupUnavailable
from assistant.mobile_api import standalone_session
from assistant.web_speech import speech_router
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

    @router.get("/portfolio")
    def portfolio() -> dict:
        try:
            accounts = lookups.records("accounts")
            keys = {r["number"].translate(DIGITS): r["key"] for r in accounts}
            cards = lookups.records("cards")
            recipients = {}
            for kind in ("card", "account"):
                recipients[kind] = [
                    {
                        "key": sha256(r[kind].translate(DIGITS).encode()).hexdigest(),
                        "number": "****" + r[kind].translate(DIGITS)[-4:],
                        "name": r["name"],
                    }
                    for r in lookups.records("contacts." + kind)
                ]
            return {
                "accounts": [
                    {
                        "key": r["key"],
                        "number": "****" + r["number"].translate(DIGITS)[-4:],
                        "identity": sha256(r["number"].translate(DIGITS).encode()).hexdigest(),
                        "label": r["type"],
                        "balance": r["balance"],
                        "unit": r["unit"],
                    }
                    for r in accounts
                ],
                "cards": [
                    {
                        "key": r["key"],
                        "number": "****" + r["number"].translate(DIGITS)[-4:],
                        "identity": sha256(r["number"].translate(DIGITS).encode()).hexdigest(),
                        "account": keys.get((r["primaryAccount"] or "").translate(DIGITS)),
                        "kind": r["kind"],
                        "status": r["status"],
                    }
                    for r in cards
                ],
                "recipients": recipients,
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
    app.include_router(speech_router())
    platform = Path(__file__).resolve().parent.parent / "negahorbit-platform"
    app.mount("/", StaticFiles(directory=platform, html=True))
