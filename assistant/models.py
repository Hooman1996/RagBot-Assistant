"""The two model clients: the Gemma chat model on vLLM, and the TEI embeddings.

Called by intent.py and slots.py, through the agent graph. Both are read-only HTTP clients over
httpx, with their addresses from ASSISTANT_LLM_URL, ASSISTANT_LLM_MODEL and ASSISTANT_EMBED_URL.
They never configure, restart or write to either service. Any failure raises ModelError, so the
task ends as failed instead of going on with a guess.
"""

import json
import os

import httpx

TIMEOUT_SECONDS = 30
MAX_ANSWER_TOKENS = 400


class ModelError(Exception):
    """A model service could not be reached, or its answer could not be used."""


class Chat:
    """Asks the chat model one question and reads a JSON answer of a fixed shape."""

    def __init__(self) -> None:
        self.url = os.environ.get("ASSISTANT_LLM_URL", "")
        self.model = os.environ.get("ASSISTANT_LLM_MODEL", "")

    def json(self, prompt: str, schema: dict) -> dict:
        """Sends the prompt as one user turn, at temperature 0, with the answer held to schema."""
        body = {
            "model": self.model,
            "temperature": 0,
            "max_tokens": MAX_ANSWER_TOKENS,
            "messages": [{"role": "user", "content": prompt}],
            "response_format": {
                "type": "json_schema",
                "json_schema": {"name": "answer", "strict": True, "schema": schema},
            },
        }
        try:
            reply = httpx.post(f"{self.url}/chat/completions", json=body, timeout=TIMEOUT_SECONDS)
            reply.raise_for_status()
            answer = json.loads(reply.json()["choices"][0]["message"]["content"])
        except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError) as error:
            raise ModelError(f"chat: {type(error).__name__}") from error
        if not isinstance(answer, dict):
            raise ModelError("chat: the answer is not a JSON object")
        return answer


class Embeddings:
    """Turns texts into vectors with the TEI embedding service."""

    def __init__(self) -> None:
        self.url = os.environ.get("ASSISTANT_EMBED_URL", "")
        self.batch_size = 0

    def embed(self, texts: list[str], query: bool = False) -> list[list[float]]:
        """One unit-length vector per text. A user's words are a query; a feature's text a document."""
        vectors: list[list[float]] = []
        try:
            if not self.batch_size:
                info = httpx.get(f"{self.url}/info", timeout=TIMEOUT_SECONDS)
                info.raise_for_status()
                self.batch_size = info.json()["max_client_batch_size"]
            for start in range(0, len(texts), self.batch_size):
                body: dict = {"inputs": texts[start : start + self.batch_size], "normalize": True}
                # The served model's own prompts: "query" for questions, as RagBot does, and "document"
                # for what is searched (measured: more right features among the closest 8).
                body["prompt_name"] = "query" if query else "document"
                reply = httpx.post(f"{self.url}/embed", json=body, timeout=TIMEOUT_SECONDS)
                reply.raise_for_status()
                vectors += reply.json()
        except (httpx.HTTPError, KeyError, TypeError, ValueError) as error:
            raise ModelError(f"embeddings: {type(error).__name__}") from error
        if len(vectors) != len(texts):
            raise ModelError("embeddings: wrong number of vectors")
        return vectors
