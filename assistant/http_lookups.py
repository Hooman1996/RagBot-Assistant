"""Typed, read-only HTTP lookups for the configured demo operations."""

from ipaddress import ip_address
from typing import Literal

import httpx
from pydantic import BaseModel, ConfigDict, TypeAdapter, ValidationError, field_validator

from assistant.lookups import FixtureLookups, LookupUnavailable
from mock_services.lookup_models import AccountRecord, SavedAccountRecord

LookupOperation = Literal["accounts", "contacts.account"]
Adapter = Literal["accounts-http", "saved-accounts-http"]
ADAPTER_FOR: dict[LookupOperation, Adapter] = {
    "accounts": "accounts-http",
    "contacts.account": "saved-accounts-http",
}
RESPONSES = {
    "accounts-http": TypeAdapter(list[AccountRecord]),
    "saved-accounts-http": TypeAdapter(list[SavedAccountRecord]),
}


class ServiceBinding(BaseModel):
    """One configured operation's adapter and HTTP endpoint."""

    model_config = ConfigDict(extra="forbid", strict=True)

    adapter: Adapter
    endpoint: str

    @field_validator("endpoint")
    @classmethod
    def loopback_endpoint(cls, value: str) -> str:
        """Accepts an explicit loopback address and port for a local mock service."""
        try:
            url = httpx.URL(value)
            loopback = ip_address(url.host).is_loopback
        except (ValueError, httpx.InvalidURL):
            loopback = False
        if (
            not loopback
            or url.scheme != "http"
            or not url.port
            or url.path == "/"
            or url.userinfo
            or url.query
            or url.fragment
        ):
            raise ValueError("expected a loopback HTTP endpoint with a port and path")
        return str(url)


class HttpLookups(FixtureLookups):
    """Uses the HTTP binding for an exact lookup name and the existing fixtures for the rest."""

    def __init__(self, bindings: dict[LookupOperation, ServiceBinding]) -> None:
        super().__init__()
        self.bindings = bindings

    def records(self, lookup: str) -> list[dict]:
        binding = self.bindings.get(lookup)
        if binding is None:
            return super().records(lookup)
        try:
            response = httpx.get(binding.endpoint, timeout=3, trust_env=False, follow_redirects=False)
            response.raise_for_status()
            records = RESPONSES[binding.adapter].validate_json(response.content)
        except (httpx.HTTPError, ValidationError) as error:
            raise LookupUnavailable(lookup) from error
        return [record.model_dump() for record in records]

    def people(self, lookup: str = "contacts") -> list[dict]:
        if lookup in self.bindings:
            return [row for row in self.records(lookup) if row.get("person")]
        return super().people(lookup)
