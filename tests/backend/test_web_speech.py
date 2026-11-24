import httpx
import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from assistant.web_speech import speech_origin, speech_router


@pytest.mark.parametrize(
    ("value", "expected"),
    [
        ("", None),
        ("speech:9081", "http://speech:9081"),
        (" https://Speech/ ", "https://speech"),
        ("http://[::1]:9081", "http://[::1]:9081"),
        ("speech", None),
        ("http://speech:", None),
        ("speech:65536", None),
        ("ftp://speech:80", None),
        ("http://user@speech", None),
        ("speech:9081/path", None),
        ("https://speech?", None),
        ("a b:10", None),
    ],
)
def test_origin(value, expected):
    assert speech_origin(value) == expected


def test_gateway_exposes_only_speech(monkeypatch):
    monkeypatch.setenv("ASR_URL", "speech:9081")
    requests = []
    replies = [
        (200, {"status": "ok", "model_loaded": 1}),
        (200, {"status": "ok", "model_loaded": True, "jobs_on_disk": 2}),
        (200, {"text": " متن ", "job_dir": "/private"}),
        (429, {}),
    ]

    async def provider(request):
        requests.append(request)
        status, data = replies.pop(0)
        return httpx.Response(status, json=data, headers={"Retry-After": "4"})

    client_class = httpx.AsyncClient
    monkeypatch.setattr(
        httpx, "AsyncClient", lambda **kw: client_class(**kw, transport=httpx.MockTransport(provider))
    )
    app = FastAPI()
    app.include_router(speech_router())
    client = TestClient(app, cookies={"session": "test"}, headers={"X-Assistant-Session": "demo"})
    assert client.get("/speech/health").status_code == 503
    assert client.get("/speech/health").json() == {"status": "ok", "model_loaded": True}
    files = {"file": ("private-name.webm", b"synthetic clip", "audio/webm")}
    assert client.post("/speech/transcribe", files=files, data={"keep_files": "true"}).status_code == 400
    assert client.post("/speech/transcribe", files=files).json() == {"text": "متن"}
    busy = client.post("/speech/transcribe", files=files)
    assert busy.status_code == 429 and busy.headers["retry-after"] == "4"
    assert client.get("/speech/api/jobs").status_code == 404
    assert [r.url.path for r in requests] == ["/api/health"] * 2 + ["/api/transcribe"] * 2
    for request in requests:
        assert "cookie" not in request.headers and "x-assistant-session" not in request.headers
        assert b"private-name" not in request.content and b"keep_files" not in request.content
    assert b'name="file"; filename="dictation.webm"' in requests[-1].content
