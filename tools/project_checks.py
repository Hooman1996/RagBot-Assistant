"""Project check runner (plan §16, Appendix B).

Standard library only, so it runs the same way on Windows and Linux. The one exception is the
contracts suite, which needs jsonschema.

Usage:
    python tools/project_checks.py foundation
    python tools/project_checks.py baseline
    python tools/project_checks.py contracts
    python tools/project_checks.py isolation
    python tools/project_checks.py apk-baseline
    python tools/project_checks.py rea-environment
    python tools/project_checks.py navgraph
    python tools/project_checks.py all
    python tools/project_checks.py all --root path/to/checkout
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from urllib.parse import urlsplit

RAGBOT_BASELINE_SHA = "fc2f6a1dc85be71f782868f18833a895e502226a"

TASK_TEMPLATE = ".github/ISSUE_TEMPLATE/task.yml"
TASK_TEMPLATE_FIELDS = (
    "owner",
    "goal",
    "inputs",
    "allowed_changes",
    "out_of_scope",
    "acceptance_command",
    "stop_condition",
    "evidence",
)
CI_WORKFLOW = ".github/workflows/ci.yml"
PLAN_FILE = "docs/plan/RagBot_Assistant_Final_Project_Plan_v2.md"
SOURCE_MANIFEST = "docs/source-manifest.md"
OVERVIEW = "docs/overview.md"
DECISION_INDEX = "docs/decisions/README.md"

GITIGNORE_REQUIRED = ("*.apk", "*.aab", "*.dex", ".env", "evidence/", "decompiled/")

FORBIDDEN_SUFFIXES = {
    ".apk": "Android package",
    ".apks": "Android split package set",
    ".aab": "Android app bundle",
    ".xapk": "Android package bundle",
    ".dex": "Dalvik bytecode",
    ".jks": "Java keystore",
    ".keystore": "keystore",
    ".p12": "certificate bundle",
    ".pem": "key or certificate",
    ".key": "private key",
}
FORBIDDEN_DIR_NAMES = {"decompiled", "jadx-output", "rea-output", "evidence", "private"}
SKIPPED_DIR_NAMES = {
    ".git",
    ".venv",
    "venv",
    "node_modules",
    "__pycache__",
    ".pytest_cache",
    ".ruff_cache",
    ".mypy_cache",
    ".gradle",
}
DATA_DUMP_SUFFIXES = {".sql", ".dump", ".bak", ".sqlite", ".db"}

SECRET_PATTERNS = {
    "private key block": re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
    "AWS access key": re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
    "GitHub token": re.compile(r"\bgh[pousr]_[A-Za-z0-9]{36,}\b"),
    "Google API key": re.compile(r"\bAIza[0-9A-Za-z_\-]{35}\b"),
    "Slack token": re.compile(r"\bxox[abprs]-[A-Za-z0-9\-]{10,}\b"),
}
MAX_TEXT_SCAN_BYTES = 2_000_000

SCHEMA_DIR = "contracts/schemas"
EXAMPLE_DIR = "contracts/examples"
OPENAPI_DIR = "contracts/openapi"
OPENAPI_FILES = (
    "assistant_mobile.json",
    "chatbot_compat.json",
    "mock_banking.json",
    "user_only_app.json",
    "test_harness.json",
)
USER_ONLY_API = "user_only_app.json"
FINAL_ACTION_WORDS = ("submit", "authoriz", "confirm")
HTTP_METHODS = ("get", "put", "post", "delete", "patch")
COMMAND_OPERATIONS = ["observe", "navigate", "fill_form", "select_candidate", "show_review"]
PROFILE_DIR = "app_profiles"
PROFILE_SCHEMA = "app_profiles/profile.schema.json"
NAV_GRAPH = "app_profiles/navigation-graph.json"
NAV_LABELS = ("Observed", "Recovered", "Inferred", "Unknown")

# Plan §12 and §15: the project's own resources on the shared dev PostgreSQL and Qdrant.
PROJECT_TARGETS = {
    "database": {"ragbot_assistant_dev"},
    "schema": {"assistant", "mock_bank"},
    "role": {"assistant_app", "mock_bank_app"},
    "collection": {"ragbot_assistant_knowledge"},
    "bucket": {"ragbot-assistant-dev"},
}
RAGBOT_TARGETS = {"hihelp_db", "hihelp_embeddings", "hihelp-documents"}
DEVELOPMENT_HOSTS = {"localhost", "127.0.0.1"}
ENV_EXAMPLE = ".env.example"
ENV_TARGETS = {
    "POSTGRES_DB": "database",
    "ASSISTANT_DB_SCHEMA": "schema",
    "MOCK_BANK_DB_SCHEMA": "schema",
    "ASSISTANT_DB_USER": "role",
    "MOCK_BANK_DB_USER": "role",
    "QDRANT_COLLECTION": "collection",
    "MINIO_BUCKET": "bucket",
}
SECRET_SETTING_WORDS = ("PASSWORD", "SECRET", "TOKEN", "KEY")
SECRET_PLACEHOLDER = "change-me"
INIT_SQL = "deployment/init_project_db.sql"

APK_RECORD = "docs/apk-baseline.md"
EVIDENCE_DIR = "local/evidence"
APK_DIR = "local/evidence/apk"
APK_ROW = re.compile(r"^\| `\w+` \| (\d+) \| `([0-9a-f]{64})` \|$", flags=re.MULTILINE)

# P0-04: the REA toolchain pins, recorded in docs/runbook/rea-setup.md.
TOOLCHAIN_DIR = "local/toolchain"
REA_PACKAGE = "rea/node_modules/rea-agents/package.json"
# (tool, file under local/toolchain, pattern whose groups join with "." into the version, pin)
TOOLCHAIN_VERSIONS = (
    (
        "Node.js",
        "node/include/node/node_version.h",
        r"NODE_MAJOR_VERSION (\d+)\n#define NODE_MINOR_VERSION (\d+)\n#define NODE_PATCH_VERSION (\d+)",
        "22.23.4",
    ),
    ("JDK", "jdk/release", r'^JAVA_RUNTIME_VERSION="(.+)"$', "21.0.13+9-LTS"),
    ("JDK module", "jdk/release", r'^MODULES=".*\b(jdk\.compiler)\b', "jdk.compiler"),
    (
        "Ghidra",
        "ghidra/ghidra_12.1.4_PUBLIC/Ghidra/application.properties",
        r"^application\.version=(.+)$",
        "12.1.4",
    ),
    ("rea-agents", REA_PACKAGE, r'^  "version": "(.+)",$', "6.8.0"),
    ("rea-agents source", REA_PACKAGE, r'^    "url": "(.+)"$', "git+https://github.com/morluto/rea.git"),
)
# (tool, file under local/toolchain, SHA-256)
TOOLCHAIN_HASHES = (
    (
        "jadx-headless-mcp 0.7.1",
        "jadx/jadx-headless-mcp-0.7.1-all.jar",
        "6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75",
    ),
    (
        "apktool 2.12.1",
        "apktool/apktool_2.12.1.jar",
        "66cf4524a4a45a7f56567d08b2c9b6ec237bcdd78cee69fd4a59c8a0243aeafa",
    ),
)


@dataclass
class Report:
    failures: list[str] = field(default_factory=list)
    passes: list[str] = field(default_factory=list)
    not_applicable: list[str] = field(default_factory=list)

    def check(self, ok: bool, name: str, detail: str = "") -> None:
        if ok:
            self.passes.append(name)
        else:
            self.failures.append(f"{name}: {detail}" if detail else name)


def repo_files(root: Path) -> list[Path]:
    """Files git would commit (tracked plus untracked, not ignored); a walk outside git."""
    try:
        out = subprocess.run(
            ["git", "-C", str(root), "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
            capture_output=True,
            check=True,
        ).stdout
        inside = subprocess.run(
            ["git", "-C", str(root), "rev-parse", "--show-toplevel"],
            capture_output=True,
            check=True,
            text=True,
        ).stdout.strip()
        if Path(inside).resolve() == root.resolve():
            return [root / p for p in out.decode("utf-8").split("\0") if p and (root / p).is_file()]
    except (OSError, subprocess.CalledProcessError):
        pass
    files = []
    for path in root.rglob("*"):
        rel = path.relative_to(root)
        if any(part in SKIPPED_DIR_NAMES for part in rel.parts):
            continue
        if path.is_file():
            files.append(path)
    return files


def looks_like_android_package(path: Path) -> bool:
    try:
        with path.open("rb") as handle:
            head = handle.read(4)
        if head.startswith(b"dex\n"):
            return True
        if head != b"PK\x03\x04":
            return False
        with zipfile.ZipFile(path) as archive:
            names = archive.namelist()
        return "AndroidManifest.xml" in names or any(
            re.fullmatch(r"classes\d*\.dex", Path(n).name) for n in names
        )
    except (OSError, zipfile.BadZipFile):
        return False


def scan_forbidden(root: Path, report: Report) -> None:
    problems = []
    for path in repo_files(root):
        rel = path.relative_to(root)
        dirs = {part.lower() for part in rel.parts[:-1]}
        hit = dirs & FORBIDDEN_DIR_NAMES
        if hit:
            problems.append(f"{rel.as_posix()} (inside private-evidence folder '{sorted(hit)[0]}')")
            continue
        suffix = path.suffix.lower()
        if suffix in FORBIDDEN_SUFFIXES:
            problems.append(f"{rel.as_posix()} ({FORBIDDEN_SUFFIXES[suffix]})")
            continue
        if looks_like_android_package(path):
            problems.append(f"{rel.as_posix()} (Android package or bytecode content)")
            continue
        if path.stat().st_size > MAX_TEXT_SCAN_BYTES:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        for label, pattern in SECRET_PATTERNS.items():
            if pattern.search(text):
                problems.append(f"{rel.as_posix()} ({label})")
                break
    report.check(not problems, "no forbidden artifacts or secrets", "; ".join(problems))


def read(root: Path, rel: str) -> str | None:
    path = root / rel
    return path.read_text(encoding="utf-8") if path.is_file() else None


def check_foundation(root: Path, report: Report) -> None:
    agents, claude = read(root, "AGENTS.md"), read(root, "CLAUDE.md")
    report.check(agents is not None, "AGENTS.md exists")
    report.check(
        agents is not None and agents == claude,
        "CLAUDE.md mirrors AGENTS.md",
        "copy AGENTS.md to CLAUDE.md" if agents is not None else "",
    )
    if agents is not None:
        report.check(
            RAGBOT_BASELINE_SHA in agents,
            "AGENTS.md names the RagBot baseline commit",
            RAGBOT_BASELINE_SHA,
        )

    template = read(root, TASK_TEMPLATE)
    if template is None:
        report.check(False, "task issue template exists", TASK_TEMPLATE)
    else:
        ids = set(re.findall(r"^\s*id:\s*([a-z_]+)\s*$", template, flags=re.MULTILINE))
        missing = [f for f in TASK_TEMPLATE_FIELDS if f not in ids]
        report.check(not missing, "task template has every required field", f"missing {missing}")

    report.check(
        (root / ".github/pull_request_template.md").is_file(),
        "pull request template exists",
    )

    workflow = read(root, CI_WORKFLOW)
    report.check(
        workflow is not None and "tools/project_checks.py all" in workflow,
        "CI runs the project checks",
        CI_WORKFLOW,
    )

    gitignore = read(root, ".gitignore") or ""
    lines = {line.strip() for line in gitignore.splitlines()}
    missing = [p for p in GITIGNORE_REQUIRED if p not in lines]
    report.check(not missing, ".gitignore blocks private material", f"missing {missing}")

    scan_forbidden(root, report)


def check_baseline(root: Path, report: Report) -> None:
    plan = read(root, PLAN_FILE)
    report.check(plan is not None, "master plan is stored", PLAN_FILE)
    report.check((root / OVERVIEW).is_file(), "overview exists", OVERVIEW)
    report.check((root / DECISION_INDEX).is_file(), "decision index exists", DECISION_INDEX)

    manifest = read(root, SOURCE_MANIFEST)
    if manifest is None:
        report.check(False, "source manifest exists", SOURCE_MANIFEST)
    else:
        report.check(
            RAGBOT_BASELINE_SHA in manifest,
            "source manifest pins the full RagBot SHA",
            RAGBOT_BASELINE_SHA,
        )
        lowered = manifest.lower()
        for heading in ("## retained", "## excluded", "## evidence labels"):
            report.check(heading in lowered, f"source manifest has '{heading[3:]}' section")
        for label in ("Observed", "Recovered", "Inferred", "Designed for mock", "Unknown"):
            report.check(label in manifest, f"source manifest defines label '{label}'")

    dumps = [
        p.relative_to(root).as_posix()
        for p in repo_files(root)
        if p.suffix.lower() in DATA_DUMP_SUFFIXES and "docs" in p.relative_to(root).parts
    ]
    report.check(not dumps, "no database dumps under docs/", ", ".join(dumps))


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def file_refs(node) -> list[str]:
    """Every "$ref" in a JSON document that points to another file."""
    if isinstance(node, list):
        return [ref for item in node for ref in file_refs(item)]
    if not isinstance(node, dict):
        return []
    refs = [ref for value in node.values() for ref in file_refs(value)]
    ref = node.get("$ref")
    if isinstance(ref, str) and not ref.startswith("#"):
        refs.append(ref)
    return refs


def check_contracts(root: Path, report: Report) -> None:
    # Imported here so the other suites stay standard library only.
    from jsonschema import Draft202012Validator, validators
    from jsonschema.exceptions import SchemaError, best_match
    from referencing import Registry, Resource

    # JSON Schema counts 1500.0 as an integer. Money and versions must be real integers (rule 6).
    strict_types = Draft202012Validator.TYPE_CHECKER.redefine("integer", lambda _, value: type(value) is int)
    Validator = validators.extend(Draft202012Validator, type_checker=strict_types)

    schemas = {p.name: load_json(p) for p in sorted((root / SCHEMA_DIR).glob("*.schema.json"))}
    report.check(bool(schemas), "contract schemas exist", SCHEMA_DIR)
    for name, schema in {**schemas, PROFILE_SCHEMA: load_json(root / PROFILE_SCHEMA)}.items():
        try:
            Validator.check_schema(schema)
            problem = ""
        except SchemaError as exc:
            problem = exc.message
        report.check(not problem, f"{name} is a valid JSON Schema", problem)
    registry = Registry().with_resources(
        (name, Resource.from_contents(schema)) for name, schema in schemas.items()
    )

    covered = set()
    for kind in ("valid", "invalid"):
        for path in sorted((root / EXAMPLE_DIR / kind).glob("*.json")):
            schema_name = path.name.split(".")[0] + ".schema.json"
            if schema_name not in schemas:
                report.check(False, f"{kind} example {path.name} names a known schema", schema_name)
                continue
            covered.add((schema_name, kind))
            example = load_json(path)
            instance = example if kind == "valid" else example["instance"]
            errors = list(Validator(schemas[schema_name], registry=registry).iter_errors(instance))
            if kind == "valid":
                messages = "; ".join(e.message for e in errors)
                report.check(not errors, f"valid example {path.name} passes", messages)
            else:
                expected = example["expected_error"]
                found = best_match(errors).message if errors else "no error"
                report.check(
                    len(errors) == 1 and expected in found,
                    f"invalid example {path.name} is rejected for its reason",
                    f"expected one error containing {expected!r}, got {len(errors)}: {found}",
                )
    for name in schemas:
        for kind in ("valid", "invalid"):
            report.check((name, kind) in covered, f"{name} has {kind} examples")

    operations = schemas.get("command.schema.json", {}).get("$defs", {}).get("operation", {}).get("enum")
    report.check(
        operations == COMMAND_OPERATIONS,
        "command operations are exactly the five agreed ones",
        f"found {operations}",
    )

    for name in OPENAPI_FILES:
        path = root / OPENAPI_DIR / name
        if not path.is_file():
            report.check(False, f"OpenAPI file {name} exists")
            continue
        api = load_json(path)
        report.check(api.get("openapi") == "3.1.0", f"{name} is OpenAPI 3.1.0")
        missing = [ref for ref in file_refs(api) if not (path.parent / ref.split("#")[0]).is_file()]
        report.check(not missing, f"{name} references existing schema files", ", ".join(missing))
        final_actions = []
        for route, item in api.get("paths", {}).items():
            for method in HTTP_METHODS:
                if method in item:
                    label = f"{method.upper()} {route} ({item[method].get('operationId')})"
                    if any(word in label.lower() for word in FINAL_ACTION_WORDS):
                        final_actions.append(label)
        if name == USER_ONLY_API:
            report.check(bool(final_actions), f"{name} holds the submit operation")
        else:
            report.check(
                not final_actions, f"{name} has no submit or authorize operation", "; ".join(final_actions)
            )

    profiles = sorted((root / PROFILE_DIR).glob("*.profile.json"))
    report.check(bool(profiles), "an app profile exists", PROFILE_DIR)
    profile_validator = Validator(load_json(root / PROFILE_SCHEMA))
    for path in profiles:
        profile = load_json(path)
        messages = "; ".join(e.message for e in profile_validator.iter_errors(profile))
        report.check(not messages, f"{path.name} matches the profile schema", messages)
        user_only = set(profile.get("user_only_actions", []))
        crossing = [
            f"{edge['from']} -> {edge['to']} ({edge['action']})"
            for edge in profile.get("edges", [])
            if edge.get("action") in user_only
        ]
        report.check(not crossing, f"{path.name}: no edge uses a user-only action", "; ".join(crossing))


def guard_target(kind: str, name: str) -> None:
    """Raise unless name is one of the project's own resources of that kind.

    Reset and migration code calls this before changing anything.
    """
    if name in RAGBOT_TARGETS:
        raise ValueError(f"{name!r} is a RagBot resource")
    if name not in PROJECT_TARGETS.get(kind, set()):
        raise ValueError(f"{name!r} is not a project {kind}")


def is_development_host(value: str) -> bool:
    """True if a host name or URL points at a development host."""
    host = urlsplit(value).hostname if "://" in value else value
    return host in DEVELOPMENT_HOSTS


def check_isolation(root: Path, report: Report) -> None:
    text = read(root, ENV_EXAMPLE)
    report.check(text is not None, f"{ENV_EXAMPLE} exists")
    settings = {}
    for line in (text or "").splitlines():
        if line.strip() and not line.startswith("#"):
            key, _, value = line.partition("=")
            settings[key.strip()] = value.strip()

    report.check(settings.get("MINIO_ENABLED") == "false", "MinIO is off by default")
    for key, kind in ENV_TARGETS.items():
        try:
            guard_target(kind, settings.get(key, ""))
            problem = ""
        except ValueError as exc:
            problem = str(exc)
        report.check(not problem, f"{key} is a project {kind}", problem)
    for key, value in settings.items():
        if key.endswith(("_HOST", "_URL")):
            report.check(is_development_host(value), f"{key} is a development host", value)
        if any(word in key for word in SECRET_SETTING_WORDS):
            report.check(value == SECRET_PLACEHOLDER, f"{key} is a placeholder", f"use {SECRET_PLACEHOLDER}")

    sql = read(root, INIT_SQL)
    report.check(sql is not None, f"{INIT_SQL} exists")
    sql = sql or ""
    report.check("hihelp" not in sql.lower(), "init SQL names no RagBot resource")
    created = {kind: set() for kind in ("database", "schema", "role")}
    for kind, name in re.findall(r"^CREATE (DATABASE|SCHEMA|ROLE) (\w+)", sql, flags=re.MULTILINE):
        created[kind.lower()].add(name)
    for kind, names in created.items():
        report.check(
            names == PROJECT_TARGETS[kind],
            f"init SQL creates exactly the project {kind} names",
            f"found {sorted(names)}",
        )
    connects = re.findall(r"^\\connect (\w+)", sql, flags=re.MULTILINE)
    report.check(
        connects == ["ragbot_assistant_dev"], "init SQL connects only to the project database", f"{connects}"
    )


def check_navgraph(root: Path, report: Report) -> None:
    """Integrity of the P1a-04 navigation graph. Standard library only."""
    text = read(root, NAV_GRAPH)
    if text is None:
        report.check(False, "navigation graph exists", NAV_GRAPH)
        return
    try:
        graph = json.loads(text)
    except json.JSONDecodeError as exc:
        report.check(False, "navigation graph is valid JSON", str(exc))
        return
    report.check(True, "navigation graph is valid JSON")

    nodes = graph.get("nodes") if isinstance(graph, dict) else None
    edges = graph.get("edges") if isinstance(graph, dict) else None
    if not (isinstance(nodes, list) and isinstance(edges, list)):
        report.check(False, "navigation graph has node and edge lists")
        return
    report.check(True, "navigation graph has node and edge lists")

    ids, bad_nodes, seen, dupes = [], [], set(), set()
    for i, node in enumerate(nodes):
        if not isinstance(node, dict) or not all(
            isinstance(node.get(k), str) and node.get(k) for k in ("id", "name", "label")
        ):
            bad_nodes.append(str(i))
            continue
        node_id = node["id"]
        (dupes if node_id in seen else seen).add(node_id)
        ids.append(node_id)
    report.check(not bad_nodes, "every node has id, name and label", f"nodes {bad_nodes}")
    report.check(not dupes, "node IDs are unique", ", ".join(sorted(dupes)))

    known = set(ids)
    found_labels = {
        n.get("label") for n in nodes if isinstance(n, dict) and n.get("label") not in NAV_LABELS
    } | {e.get("label") for e in edges if isinstance(e, dict) and e.get("label") not in NAV_LABELS}
    bad_labels = sorted((label for label in found_labels if label is not None), key=str)
    report.check(not bad_labels, "every label is one of the four", ", ".join(map(str, bad_labels)))

    dangling, null_not_dynamic = [], []
    for edge in edges:
        if not isinstance(edge, dict):
            dangling.append("<non-object edge>")
            continue
        frm, to = edge.get("from"), edge.get("to")
        if frm not in known:
            dangling.append(f"from {frm!r}")
        if to is None:
            if edge.get("dynamic") is not True:
                null_not_dynamic.append(f"{frm!r}->null")
        elif to not in known:
            dangling.append(f"to {to!r}")
    report.check(not dangling, "every edge links known nodes (no dangling edges)", "; ".join(dangling))
    report.check(
        not null_not_dynamic,
        "edges with no destination are marked dynamic",
        "; ".join(null_not_dynamic),
    )


def check_apk_baseline(root: Path, report: Report) -> None:
    record = read(root, APK_RECORD)
    report.check(record is not None, f"{APK_RECORD} exists")
    recorded = {digest: int(size) for size, digest in APK_ROW.findall(record or "")}
    report.check(bool(recorded), "APK record lists the APK files")
    if not (root / EVIDENCE_DIR).is_dir():
        report.not_applicable.append(f"APK hashes: no local APK ({EVIDENCE_DIR}/ is absent)")
        return
    found = {}
    for path in sorted(p for p in (root / APK_DIR).glob("*") if p.is_file()):
        with path.open("rb") as handle:
            found[hashlib.file_digest(handle, "sha256").hexdigest()] = path.stat().st_size
    report.check(bool(found), f"APK found in {APK_DIR}/")
    report.check(
        found == recorded,
        "local APK files match the recorded sizes and SHA-256 hashes",
        f"found {found}, recorded {recorded}",
    )


def check_rea_environment(root: Path, report: Report) -> None:
    if not (root / TOOLCHAIN_DIR).is_dir():
        report.not_applicable.append(f"REA toolchain: {TOOLCHAIN_DIR}/ is absent")
        return
    for tool, rel, pattern, pin in TOOLCHAIN_VERSIONS:
        match = re.search(pattern, read(root, f"{TOOLCHAIN_DIR}/{rel}") or "", flags=re.MULTILINE)
        found = ".".join(match.groups()) if match else "nothing"
        report.check(found == pin, f"{tool} is {pin}", f"found {found} in {TOOLCHAIN_DIR}/{rel}")
    for tool, rel, pin in TOOLCHAIN_HASHES:
        path = root / TOOLCHAIN_DIR / rel
        found = "nothing"
        if path.is_file():
            with path.open("rb") as handle:
                found = hashlib.file_digest(handle, "sha256").hexdigest()
        report.check(found == pin, f"{tool} SHA-256 is {pin}", f"found {found} at {TOOLCHAIN_DIR}/{rel}")


SUITES = {
    "foundation": (check_foundation,),
    "baseline": (check_baseline,),
    "contracts": (check_contracts,),
    "isolation": (check_isolation,),
    "apk-baseline": (check_apk_baseline, scan_forbidden),
    "rea-environment": (check_rea_environment,),
    "navgraph": (check_navgraph,),
}
SUITES["all"] = (
    SUITES["foundation"]
    + SUITES["baseline"]
    + SUITES["contracts"]
    + SUITES["isolation"]
    + (check_apk_baseline, check_rea_environment, check_navgraph)
)


def run(suite: str, root: Path) -> Report:
    report = Report()
    for check in SUITES[suite]:
        check(root, report)
    return report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("suite", choices=sorted(SUITES))
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parent.parent)
    args = parser.parse_args(argv)

    report = run(args.suite, args.root)
    for name in report.passes:
        print(f"PASS  {name}")
    for failure in report.failures:
        print(f"FAIL  {failure}")
    for item in report.not_applicable:
        print(f"N/A   {item}")
    print(
        f"\n{args.suite}: {len(report.passes)} passed, {len(report.failures)} failed, "
        f"{len(report.not_applicable)} not applicable"
    )
    return 1 if report.failures else 0


if __name__ == "__main__":
    sys.exit(main())
