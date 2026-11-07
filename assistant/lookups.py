"""Finds records for the task runner, such as turning "رضا" into a card number.

Called by task_runner, demo_values and the profile check. This one module stands between the
assistant and the data, so a real-API adapter can replace it with the same three methods. It reads
mock_services/fixtures/ and never writes. It uses the standard library only.
"""

import json
from dataclasses import dataclass
from pathlib import Path

FIXTURES = Path(__file__).resolve().parent.parent / "mock_services" / "fixtures"


@dataclass(frozen=True)
class Source:
    """Where one lookup's records are: a fixture file, its list, and the key and label fields."""

    file: str
    rows: str
    key: str
    label: str


SOURCES = {
    "contacts": Source("contacts", "people", "id", "name"),
    "cards": Source("cards", "cards", "key", "number"),
    "accounts": Source("accounts", "accounts", "key", "type"),
    "bills": Source("bills", "bills", "billId", "company"),
    "wallets": Source("wallets", "wallets", "number", "title"),
    "charities": Source("charities", "charities", "id", "name"),
    "loans": Source("loans", "loans", "id", "title"),
    "loan_groups": Source("loans", "groups", "key", "title"),
    "insurance": Source("insurance", "policies", "id", "number"),
    "transactions": Source("transactions", "recent", "key", "title"),
    "statements": Source("transactions", "statement", "key", "description"),
    "standing_orders": Source("transactions", "standingOrders", "traceId", "name"),
    "group_history": Source("transactions", "groupHistory", "traceId", "traceId"),
    "repetitive": Source("transactions", "repetitive", "id", "title"),
    "wallet_statement": Source("wallets", "statement", "traceId", "typeDescription"),
    "cheque_books": Source("cheques", "books", "reference", "reference"),
    "cardboard_tasks": Source("cheques", "tasks", "id", "id"),
    "digital_cheques": Source("cheques", "digital", "sayadId", "sayadId"),
    "sayad_cheques": Source("cheques", "sayad", "sayadId", "sayadId"),
    "vehicles": Source("vehicles", "records", "id", "id"),
}


class NotFound(Exception):
    """No record matches what the user said."""


class Ambiguous(Exception):
    """Several records match, so the user must choose."""


@dataclass(frozen=True)
class Candidate:
    """One record: its key, the value to send to the app, and what to show the user."""

    key: str
    value: str
    label: str


def source_of(lookup: str) -> str:
    """The lookup's name: `contacts` for `contacts.card`."""
    return lookup.split(".")[0]


def is_known(lookup: str) -> bool:
    """True when this adapter can serve the lookup."""
    return source_of(lookup) in SOURCES


class FixtureLookups:
    """Lookups over the mock fixtures. Read-only."""

    def __init__(self, folder: Path = FIXTURES) -> None:
        self.folder = folder

    def records(self, lookup: str) -> list[dict]:
        """Every record of a lookup."""
        source = SOURCES[source_of(lookup)]
        data = json.loads((self.folder / f"{source.file}.json").read_text(encoding="utf-8"))
        return data[source.rows]

    def candidates(self, lookup: str) -> list[Candidate]:
        """The lookup's records. `contacts.card` sends the card, plain `contacts` the ID."""
        source = SOURCES[source_of(lookup)]
        attribute = lookup.split(".")[1] if "." in lookup else source.key
        rows = [row for row in self.records(lookup) if row.get(attribute) is not None]
        return [Candidate(str(row[source.key]), str(row[attribute]), str(row[source.label])) for row in rows]

    def resolve(self, lookup: str, text: str) -> str:
        """Turns what the user said into the value to send. Refuses a miss or a tie."""
        # A typed number needs no record: the user may send to someone not saved.
        if source_of(lookup) == "contacts" and text.isdigit():
            return text
        found = self.matching(lookup, text)
        if not found:
            raise NotFound(f"{lookup}: {text}")
        if len(found) > 1:
            raise Ambiguous(f"{lookup}: {text}: {[item.label for item in found]}")
        return found[0].value

    def matching(self, lookup: str, text: str) -> list[Candidate]:
        """The records whose key, value or label is the text, else whose label contains it."""
        candidates = self.candidates(lookup)
        exact = [item for item in candidates if text in (item.key, item.value, item.label)]
        return exact or [item for item in candidates if text in item.label]
