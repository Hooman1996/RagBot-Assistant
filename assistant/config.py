"""Loads and validates the standalone graph and its two supported service bindings."""

import json
from pathlib import Path

from jsonschema import Draft202012Validator
from pydantic import TypeAdapter, ValidationError

from assistant.command_queue import PROFILE_VERSION
from assistant.graph_data import read_json
from assistant.http_lookups import ADAPTER_FOR, HttpLookups, LookupOperation, ServiceBinding
from assistant.lookups import is_known
from assistant.navigator import HOME, CrossesUserOnly, Navigator, NoRoute, search

BINDINGS = TypeAdapter(dict[LookupOperation, ServiceBinding])
DEMO_FEATURE = "internal_transfer"


class ConfigError(ValueError):
    """A configuration file is missing or invalid."""


def read_config(path: Path) -> object:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, ValueError) as error:
        raise ConfigError(f"{path}: {error}") from error


def load_graph(path: Path) -> dict:
    profile = read_config(path)
    validator = Draft202012Validator(read_json("profile.schema.json"))
    error = next(validator.iter_errors(profile), None)
    if error:
        raise ConfigError(f"{path}: invalid graph at {error.json_path}: {error.message}")
    if profile["profile_version"] != PROFILE_VERSION:
        raise ConfigError(f"{path}: unsupported profile version, expected {PROFILE_VERSION}")

    # A file can add restrictions while the replica's declared user-only actions stay protected.
    replica_policy = read_json("replica.profile.json")["user_only_actions"]
    profile["user_only_actions"] = list(dict.fromkeys(profile["user_only_actions"] + replica_policy))
    try:
        validate_graph(profile)
    except ConfigError as error:
        raise ConfigError(f"{path}: {error}") from error
    return profile


def validate_graph(profile: dict) -> None:
    screens = set(profile["screens"])
    navigator = Navigator(profile)
    if HOME not in screens:
        raise ConfigError(f"graph must contain Home ({HOME})")
    for edge in profile["edges"]:
        if not {edge["from"], edge["to"]} <= screens:
            raise ConfigError(f"edge {edge['from']} → {edge['to']} references an unknown screen")
        if navigator.crosses_user_only(edge):
            raise ConfigError(f"edge {edge['from']} → {edge['to']} uses a user-only action")
        if ("*" in edge["action"]) != ("requires" in edge):
            raise ConfigError(f"edge {edge['from']} → {edge['to']} has invalid selection requirements")
        for requirement in edge.get("requires", []):
            validate_lookup(requirement)
    if len(navigator.forms) != len(profile["forms"]):
        raise ConfigError("duplicate form ID")
    for form, fields in navigator.forms.items():
        if form not in screens:
            raise ConfigError(f"form {form} references an unknown screen")
        if len({field["ref"] for field in fields}) != len(fields):
            raise ConfigError(f"form {form} contains a duplicate field")
        for field in fields:
            if field.get("secret") and (field.get("requires") or field.get("ask")):
                raise ConfigError(f"form {form} offers secret field {field['ref']}")
            if field.get("requires") and not field.get("ask"):
                raise ConfigError(f"form {form} has an invalid required field {field['ref']}")
            if (field.get("requires") or field.get("ask")) and navigator.is_user_only(field["ref"]):
                raise ConfigError(f"form {form} offers user-only field {field['ref']}")
            validate_lookup(field)
    features = profile["features"]
    if len({feature["intent"] for feature in features}) != len(features):
        raise ConfigError("duplicate feature intent")
    for feature in features:
        validate_feature(navigator, screens, feature)


def validate_lookup(field: dict) -> None:
    lookup = field.get("lookup")
    if (lookup is not None or field["type"] == "ref") and not is_known(lookup or ""):
        raise ConfigError(f"unknown lookup for {field['ref']}")


def validate_feature(navigator: Navigator, screens: set[str], feature: dict) -> None:
    intent, form, review = (feature[key] for key in ("intent", "form", "review"))
    fields = {field["ref"] for field in navigator.forms.get(form, []) if not field.get("secret")}
    if (
        feature["destination"] not in screens
        or navigator.is_user_only(feature["destination"])
        or not set(feature["required_slots"]) <= fields
        or not all(is_known(lookup) for lookup in feature["lookups"])
    ):
        raise ConfigError(f"feature {intent} has an invalid destination, slot or lookup")
    if (form is None) != (review is None) or (
        form is not None
        and (
            form not in navigator.forms
            or review not in screens
            or search(form, review, navigator.edges) is None
        )
    ):
        raise ConfigError(f"feature {intent} has an invalid form or unreachable review")
    try:
        navigator.plan_feature(HOME, feature)
    except (NoRoute, CrossesUserOnly) as error:
        raise ConfigError(f"feature {intent} has no allowed route from Home") from error


def load_lookups(path: Path, profile: dict) -> HttpLookups:
    try:
        bindings = BINDINGS.validate_python(read_config(path))
    except ValidationError as error:
        first = error.errors(include_input=False, include_url=False, include_context=False)[0]
        location = ".".join(map(str, first["loc"])) or "$"
        raise ConfigError(f"{path}: invalid service binding at {location}: {first['msg']}") from error
    for operation, binding in bindings.items():
        if binding.adapter != ADAPTER_FOR[operation]:
            raise ConfigError(f"{path}: {operation} requires the {ADAPTER_FOR[operation]} adapter")
    demo = next((feature for feature in profile["features"] if feature["intent"] == DEMO_FEATURE), None)
    if demo is not None:
        required = {
            item.lookup for item in Navigator(profile).plan_feature(HOME, demo).requirements if item.lookup
        }
        missing = required - bindings.keys()
        if missing:
            raise ConfigError(f"{path}: missing binding for {DEMO_FEATURE}: {', '.join(sorted(missing))}")
    return HttpLookups(bindings)
