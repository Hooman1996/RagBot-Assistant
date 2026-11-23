"""The web provider scopes synthetic statements to the requested account and inclusive dates."""

from datetime import date, timedelta
from pathlib import Path

import httpx
import pytest
from fastapi.testclient import TestClient

from assistant.config import load_graph, load_lookups
from assistant.lookups import LookupUnavailable
from mock_services.app import create_app

GRAPH = Path("app_profiles/web.profile.json")
SERVICES = Path("mock_services/web-services.json")


def test_statement_range_account_and_empty_result():
    api = TestClient(create_app())
    today = date.today()
    day = today - timedelta(days=2)
    params = {"account": "account-2", "start": day, "end": day}
    response = api.get("/statement", params=params).raise_for_status().json()
    assert response["account"] == "account-2"
    assert response["rows"] == [
        {
            "date": day.isoformat(),
            "amount": -750000,
            "balance": 1750000,
            "unit": "IRR",
            "description": "برداشت از خودپرداز",
        }
    ]
    other = api.get("/statement", params=params | {"account": "account-1", "end": today}).json()
    assert other["account"] == "account-1"
    assert [(row["amount"], row["balance"]) for row in other["rows"]] == [
        (2000000, 12500000),
        (-750000, 10500000),
    ]
    assert api.get("/statement", params=params | {"start": today, "end": today}).json()["rows"] == []
    assert api.get("/statement", params=params | {"account": "unknown"}).status_code == 404
    assert api.get("/statement", params=params | {"start": today}).status_code == 422


@pytest.mark.parametrize("fault", ["account", "range", "row_date", "amount", "offline"])
def test_statement_adapter_rejects_bad_provider_results(monkeypatch, fault):
    lookups = load_lookups(SERVICES, load_graph(GRAPH))
    day = date(2026, 11, 21)
    data = {
        "account": "account-2",
        "start": day.isoformat(),
        "end": day.isoformat(),
        "rows": [
            {"date": day.isoformat(), "amount": 123, "balance": None, "unit": "IRR", "description": "test"}
        ],
    }
    if fault == "account":
        data["account"] = "account-1"
    elif fault == "range":
        data["start"] = "2026-11-20"
    elif fault == "row_date":
        data["rows"][0]["date"] = "2026-11-22"
    elif fault == "amount":
        data["rows"][0]["amount"] = 1.5

    def provider(url, **kwargs):
        assert kwargs["params"] == {"account": "account-2", "start": day.isoformat(), "end": day.isoformat()}
        if fault == "offline":
            raise httpx.ReadTimeout("timeout")
        return httpx.Response(200, json=data, request=httpx.Request("GET", url))

    monkeypatch.setattr(httpx, "get", provider)
    with pytest.raises(LookupUnavailable):
        lookups.statement("account-2", day, day)
