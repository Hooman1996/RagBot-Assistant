"""Loopback demo provider for the candidates' read-only lookups."""

from pathlib import Path

from fastapi import FastAPI

from assistant.lookups import FIXTURES, FixtureLookups
from mock_services.lookup_models import (
    AccountRecord,
    CardRecord,
    LoanInstallmentRecord,
    LoanRecord,
    OperatorRecord,
    SavedAccountRecord,
    SavedCardRecord,
    SavedPlateRecord,
)


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

    @app.get("/cards")
    def cards() -> list[CardRecord]:
        return [
            CardRecord(**{key: row[key] for key in CardRecord.model_fields})
            for row in fixtures.records("cards")
        ]

    @app.get("/saved-cards")
    def saved_cards() -> list[SavedCardRecord]:
        people = {row["id"] for row in fixtures.people()}
        return [
            SavedCardRecord(id=row["id"], name=row["name"], card=row["card"], person=row["id"] in people)
            for row in fixtures.records("contacts.card")
            if row.get("card")
        ]

    @app.get("/operators")
    def operators() -> list[OperatorRecord]:
        return [
            OperatorRecord(
                **{key: row[key] for key in OperatorRecord.model_fields if key != "unit"}, unit="IRR"
            )
            for row in fixtures.records("operators")
        ]

    @app.get("/saved-plates")
    def saved_plates() -> list[SavedPlateRecord]:
        return [SavedPlateRecord(id=key, plate=plate) for key, plate in fixtures.saved_plates().items()]

    @app.get("/loans")
    def loans() -> list[LoanRecord]:
        return [
            LoanRecord(
                id=row["id"],
                title=row["title"],
                installments=[
                    LoanInstallmentRecord(number=item["number"], status=item["status"])
                    for item in row["installments"]
                ],
            )
            for row in fixtures.records("loans")
        ]

    return app
