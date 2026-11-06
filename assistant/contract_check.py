"""Checks JSON against contracts/schemas.

Called by mobile_api for everything the app posts, and by tests for every command the queue
builds. It only reads the schemas. It must never loosen or copy them.
"""

import json
from pathlib import Path

from jsonschema import Draft202012Validator
from referencing import Registry, Resource

SCHEMAS = Path(__file__).resolve().parent.parent / "contracts" / "schemas"


def load_registry() -> Registry:
    """Registers every schema by file name, because they refer to each other that way."""
    resources = []
    for path in SCHEMAS.glob("*.schema.json"):
        schema = json.loads(path.read_text(encoding="utf-8"))
        resources.append((path.name, Resource.from_contents(schema)))
    return Registry().with_resources(resources)


REGISTRY = load_registry()


def problems(schema_file: str, value: object) -> list[str]:
    """Returns what is wrong with value, as short messages. An empty list means it is valid."""
    schema = json.loads((SCHEMAS / schema_file).read_text(encoding="utf-8"))
    validator = Draft202012Validator(schema, registry=REGISTRY)
    return [error.message for error in validator.iter_errors(value)]
