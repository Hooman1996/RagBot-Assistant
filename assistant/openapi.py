"""Bundles the standalone contract so FastAPI can serve it as one OpenAPI document."""

import json
from pathlib import Path

from assistant.contract_check import SCHEMAS


def local_refs(value: object, schema: str | None = None) -> object:
    """Moves schema file references and their fragments into OpenAPI's components."""
    if isinstance(value, list):
        return [local_refs(item, schema) for item in value]
    if isinstance(value, dict):
        value = {key: local_refs(item, schema) for key, item in value.items()}
        if "$ref" in value:
            ref = value["$ref"]
            file, _, fragment = ref.partition("#")
            target = Path(file).name.removesuffix(".schema.json") if file.endswith(".schema.json") else None
            if target or (not file and schema):
                value["$ref"] = f"#/components/schemas/{target or schema}{fragment}"
    return value


def standalone_openapi() -> dict:
    """Reads the canonical API and makes its request and response schemas available to clients."""
    path = SCHEMAS.parent / "openapi" / "assistant_mobile.json"
    api = local_refs(json.loads(path.read_text(encoding="utf-8")))
    components = api["components"]["schemas"]
    for path in sorted(SCHEMAS.glob("*.schema.json")):
        name = path.name.removesuffix(".schema.json")
        components[name] = local_refs(json.loads(path.read_text(encoding="utf-8")), name)
    return api
