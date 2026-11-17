"""Finds records for the task runner, such as turning "رضا" into a card number.

Called by task_runner, demo_values and the profile check. This one module stands between the
assistant and the data, so a real-API adapter can replace it with the same three methods. It reads
mock_services/fixtures/ and never writes. It uses the standard library only.
"""

import json
import re
from dataclasses import dataclass
from pathlib import Path

FIXTURES = Path(__file__).resolve().parent.parent / "mock_services" / "fixtures"
IBAN = re.compile(r"IR\d{24}")
DIGITS = str.maketrans("۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩", "01234567890123456789")
# Words a user puts before a name or number («کارت 3566», «حساب رضا»); they name no record.
GENERIC = {"کارت", "کارتم", "حساب", "حسابم", "کیف", "کیفم", "پول", "پولم", "شبا", "شبای", "شماره", "به", "از"}
ENDINGS = ("مو", "تو", "شو", "م", "ت", "ش")  # spoken endings such as «تجاریمو»


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


def plain(text: str) -> str:
    """The text as matching compares it: Latin digits, Persian ی and ک, no ZWNJ, «ٔ» or extra spaces."""
    text = text.translate(DIGITS).replace("ي", "ی").replace("ك", "ک").replace("\u0654", "")
    return " ".join(text.replace("\u200c", " ").split())


def typed(lookup: str, text: str) -> bool:
    """A contact the user typed instead of naming, in the field's own format: an IBAN for an IBAN
    field, else a card, account or wallet number. Anything else must match a saved record."""
    if source_of(lookup) != "contacts":
        return False
    return IBAN.fullmatch(text) is not None if lookup.endswith(".iban") else text.isdigit()


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
        """Every record of a lookup. Contacts are the people and the saved destinations («مقصدهای
        آشنا»), each destination a contact with its one number: «نگار صادقی» has an account."""
        source = SOURCES[source_of(lookup)]
        data = json.loads((self.folder / f"{source.file}.json").read_text(encoding="utf-8"))
        if source_of(lookup) != "contacts":
            return data[source.rows]
        saved = [
            {"id": row["id"], "name": row["name"], row["type"].lower(): row["destination"]}
            for row in data.get("destinations", [])
        ]
        return data[source.rows] + saved

    def people(self) -> list[dict]:
        """The contacts that are people, without the saved destinations."""
        return json.loads((self.folder / "contacts.json").read_text(encoding="utf-8"))["people"]

    def candidates(self, lookup: str, where: dict | None = None) -> list[Candidate]:
        """The lookup's records, only those with every field in `where` when it is given.
        `contacts.card` sends the card, plain `contacts` the ID."""
        source = SOURCES[source_of(lookup)]
        attribute = lookup.split(".")[1] if "." in lookup else source.key
        rows = [
            row
            for row in self.records(lookup)
            if row.get(attribute) is not None and all(row.get(k) == v for k, v in (where or {}).items())
        ]
        return [Candidate(str(row[source.key]), str(row[attribute]), str(row[source.label])) for row in rows]

    def installments(self, lookup: str, key: str) -> list[str]:
        """The choice keys of a policy's or loan's installments that the app lets the user pick
        (Recovered): an insurance installment neither paid nor waiting for verification; for a loan
        only the next one, the first overdue else the first unpaid, as the app ticks them in order."""
        source = SOURCES[source_of(lookup)]
        items = next(row for row in self.records(lookup) if str(row[source.key]) == key)["installments"]
        if source_of(lookup) == "insurance":
            closed = ("PAID", "VERIFICATION_WAITING")
            return [str(item["number"]) for item in items if item["status"] not in closed]
        overdue = [n for n, item in enumerate(items) if item["status"] == "TRACKABLE_OVERDUE"]
        unpaid = [n for n, item in enumerate(items) if item["status"] != "PAID"]
        return [str(n) for n in (overdue or unpaid)[:1]]

    def operator(self, name: str = "", phone: str = "") -> dict | None:
        """An operator's charge list (`defaults`) and limits (`min`, `max`, `multiplier`), by its key
        or by a phone number's prefix as the app detects it; None when neither names one."""
        digits = plain(phone)
        operators = json.loads((self.folder / "operators.json").read_text(encoding="utf-8"))["operators"]
        for item in operators:
            if item["operator"] == name or (len(digits) > 3 and digits.startswith(tuple(item["prefixes"]))):
                return item
        return None

    def balance(self, lookup: str, key: str) -> int | None:
        """A record's balance in rials, or None when the app doesn't know it."""
        source = SOURCES[source_of(lookup)]
        return next(row for row in self.records(lookup) if str(row[source.key]) == key).get("balance")

    def named(self, text: str, records: list[Candidate]) -> list[Candidate]:
        """The records the text names by their whole label, or by the words of it no other record
        has («پس‌انداز» names «سپرده پس‌انداز» among «سپرده کوتاه‌مدت» and the rest)."""
        said = " " + " ".join(re.sub(r"[^\w\s]", " ", plain(text)).split()) + " "
        labels = {item.key: plain(item.label).split() for item in records}
        found = []
        for item in records:
            shared = {word for key, words in labels.items() if key != item.key for word in words} | GENERIC
            own = " ".join(word for word in labels[item.key] if word not in shared)
            if any(f" {phrase} " in said for phrase in (" ".join(labels[item.key]), own) if phrase):
                found.append(item)
        return found

    def resolve(self, lookup: str, text: str) -> str:
        """Turns what the user said into the value to send. Refuses a miss or a tie."""
        found = self.matching(lookup, text)
        # A typed number needs no record: the user may send to someone not saved.
        if typed(lookup, text) and (text.isdigit() or not found):
            return text
        if not found:
            raise NotFound(f"{lookup}: {text}")
        if len(found) > 1:
            raise Ambiguous(f"{lookup}: {text}: {[item.label for item in found]}")
        return found[0].value

    def matching(self, lookup: str, text: str) -> list[Candidate]:
        """The records whose key, value or label is the text; else those holding each word that is
        not generic (a label containing the whole text when every word is generic)."""
        candidates = self.candidates(lookup)
        said = plain(text)
        if lookup.endswith(".iban") and said.isdigit():
            return []  # a bare number is no IBAN: never matched inside one, so the user is asked
        if typed(lookup, said):
            # A typed number is the user's own: it names a person who has it, never a saved destination.
            people = {row["id"] for row in self.people()}
            candidates = [item for item in candidates if item.key in people]
        exact = [
            item for item in candidates if said in (plain(item.key), plain(item.value), plain(item.label))
        ]
        if exact:
            return exact
        # Generic words name no record, and a word no record holds is the user's own («بگیر»).
        words = [word for word in said.split() if word not in GENERIC]
        words = [word for word in words if any(holds(item, word) for item in candidates)]
        if not words:
            return [item for item in candidates if said in plain(item.label)]
        return [item for item in candidates if all(holds(item, word) for word in words)]


def holds(item: Candidate, word: str) -> bool:
    """True when the record's key, value or label contains the word, or the word without an ending."""
    text = " ".join(plain(part) for part in (item.key, item.value, item.label))
    stems = [word] + [
        word[: -len(end)] for end in ENDINGS if word.endswith(end) and len(word) - len(end) >= 2
    ]
    return any(stem in text for stem in stems)
