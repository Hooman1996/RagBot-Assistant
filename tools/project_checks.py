"""Project check runner (plan §16, Appendix B).

Standard library only, so it runs the same way on Windows and Linux.

Usage:
    python tools/project_checks.py foundation
    python tools/project_checks.py baseline
    python tools/project_checks.py all
    python tools/project_checks.py all --root path/to/checkout
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

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


@dataclass
class Report:
    failures: list[str] = field(default_factory=list)
    passes: list[str] = field(default_factory=list)

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


SUITES = {"foundation": (check_foundation,), "baseline": (check_baseline,)}
SUITES["all"] = SUITES["foundation"] + SUITES["baseline"]


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
    print(f"\n{args.suite}: {len(report.passes)} passed, {len(report.failures)} failed")
    return 1 if report.failures else 0


if __name__ == "__main__":
    sys.exit(main())
