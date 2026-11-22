"""Loopback demo provider for the internal transfer's read-only lookups."""

from pathlib import Path

from fastapi import FastAPI

from assistant.lookups import FIXTURES, FixtureLookups
from mock_services.lookup_models import AccountRecord, SavedAccountRecord


def create_app(folder: Path = FIXTURES) -> FastAPI:
    fixtures = FixtureLookups(folder)
    app = FastAPI(title="Synthetic banking lookups", docs_url=None, redoc_url=None)

    @app.get("/accounts")
    def accounts() -> list[AccountRecord]:
        return [
            AccountRecord(**{key: row[key] for key in AccountRecord.model_fields})
            for row in fixtures.records("accounts")
        ]

    @app.get("/saved-accounts")
    def saved_accounts() -> list[SavedAccountRecord]:
        people = {row["id"] for row in fixtures.people()}
        return [
            SavedAccountRecord(
                id=row["id"], name=row["name"], account=row["account"], person=row["id"] in people
            )
            for row in fixtures.records("contacts.account")
            if row.get("account")
        ]

    return app
