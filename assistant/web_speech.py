"""Same-origin speech routes for the standalone web demo."""

import os
import re
from ipaddress import IPv6Address
from pathlib import Path

import httpx
from dotenv import dotenv_values
from fastapi import APIRouter, HTTPException, Request
from starlette.datastructures import UploadFile


def speech_origin(value: str) -> str | None:
    match = re.fullmatch(
        r"(?:(https?)://)?(\[[0-9a-f:.]+\]|[a-z0-9.-]+)(?::([0-9]{1,5}))?/?",
        value.strip(),
        re.IGNORECASE | re.ASCII,
    )
    if not match:
        return None
    scheme, host, port = match.groups()
    if (not scheme and not port) or (port and not 1 <= int(port) <= 65535):
        return None
    if host.startswith("["):
        try:
            host = f"[{IPv6Address(host[1:-1])}]"
        except ValueError:
            return None
    elif len(host) > 253 or not all(
        re.fullmatch(r"[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?", label) for label in host.lower().split(".")
    ):
        return None
    return f"{(scheme or 'http').lower()}://{host.lower()}" + (f":{int(port)}" if port else "")


def speech_router() -> APIRouter:
    configured = os.environ.get("ASR_URL")
    if configured is None:
        settings = Path(__file__).resolve().parents[1] / ".env"
        configured = dotenv_values(settings, interpolate=False).get("ASR_URL")
    origin = speech_origin(configured or "")
    router = APIRouter(prefix="/speech", include_in_schema=False)

    async def provider(method: str, path: str, timeout: int, **kwargs) -> dict:
        if not origin:
            raise HTTPException(503)
        try:
            async with httpx.AsyncClient(timeout=timeout, trust_env=False) as client:
                response = await client.request(method, origin + path, **kwargs)
            if response.status_code == 429:
                retry = response.headers.get("Retry-After")
                raise HTTPException(429, headers={"Retry-After": retry} if retry else None)
            response.raise_for_status()
            result = response.json()
            if isinstance(result, dict):
                return result
        except httpx.TimeoutException as error:
            raise HTTPException(504) from error
        except (httpx.HTTPError, ValueError) as error:
            raise HTTPException(503) from error
        raise HTTPException(502)

    @router.get("/health")
    async def health() -> dict:
        result = await provider("GET", "/api/health", 3)
        if result.get("status") != "ok" or result.get("model_loaded") is not True:
            raise HTTPException(503)
        return {"status": "ok", "model_loaded": True}

    @router.post("/transcribe")
    async def transcribe(request: Request) -> dict:
        if not origin:
            raise HTTPException(503)
        if request.headers.get("sec-fetch-site") == "cross-site":
            raise HTTPException(403)
        length = request.headers.get("content-length", "")
        if not length.isdecimal() or int(length) > 4 * 1024 * 1024:
            raise HTTPException(413)
        async with request.form(max_files=1, max_fields=0) as form:
            clip = form.get("file")
            if not isinstance(clip, UploadFile) or not clip.size:
                raise HTTPException(422)
            mime = (clip.content_type or "").split(";")[0]
            ext = {"audio/webm": "webm", "audio/ogg": "ogg", "audio/mp4": "mp4"}.get(mime)
            if not ext:
                raise HTTPException(415)
            files = {"file": (f"dictation.{ext}", await clip.read(), mime)}
        result = await provider("POST", "/api/transcribe", 360, files=files)
        text = result.get("text")
        return {"text": text.strip() if isinstance(text, str) else ""}

    return router
