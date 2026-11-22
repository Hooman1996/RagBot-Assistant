"""The synthetic values exposed by the two read-only lookup endpoints."""

from typing import Literal

from pydantic import BaseModel, ConfigDict


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
