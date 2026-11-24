"""The synthetic values exposed by the read-only demo lookup endpoints."""

from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class AccountRecord(BaseModel):
    """An account the assistant can select by its replica key."""

    model_config = ConfigDict(extra="forbid", strict=True)

    key: str
    number: str
    type: str
    balance: int | None
    unit: Literal["IRR"]


class SavedAccountRecord(BaseModel):
    """A named account from the person list or saved destinations."""

    model_config = ConfigDict(extra="forbid", strict=True)

    id: str
    name: str
    account: str
    person: bool


class CardRecord(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    key: str
    number: str
    kind: str
    status: str
    primaryAccount: str | None = None


class SavedCardRecord(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    id: str
    name: str
    card: str
    person: bool


class OperatorRecord(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    operator: str
    prefixes: list[str]
    min: int
    max: int
    multiplier: int = Field(gt=0)
    defaults: list[int]
    unit: Literal["IRR"]


class SavedPlateRecord(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    id: str
    plate: list[str] = Field(min_length=4, max_length=4)


class LoanInstallmentRecord(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    number: int
    status: Literal["PAID", "UNPAID", "TRACKABLE_OVERDUE"]


class LoanRecord(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    id: str
    title: str
    installments: list[LoanInstallmentRecord]


class StatementRow(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    date: date
    description: str
    amount: int
    balance: int | None
    unit: Literal["IRR"]


class Statement(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    account: str
    start: date
    end: date
    rows: list[StatementRow]
