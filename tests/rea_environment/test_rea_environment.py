"""Tests for the rea-environment suite, run against a throwaway toolchain folder."""

from __future__ import annotations

import hashlib
import importlib.util
import sys
from pathlib import Path

import pytest

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("project_checks", REPO / "tools" / "project_checks.py")
project_checks = importlib.util.module_from_spec(spec)
sys.modules["project_checks"] = project_checks  # dataclasses resolve their module by name
spec.loader.exec_module(project_checks)

PINNED_FILES = {
    "node/include/node/node_version.h": (
        "#define NODE_MAJOR_VERSION 22\n#define NODE_MINOR_VERSION 23\n#define NODE_PATCH_VERSION 4\n"
    ),
    "jdk/release": 'JAVA_RUNTIME_VERSION="21.0.13+9-LTS"\nMODULES="java.base jdk.compiler jdk.jfr"\n',
    "ghidra/ghidra_12.1.4_PUBLIC/Ghidra/application.properties": "application.version=12.1.4\n",
    "rea/node_modules/rea-agents/package.json": (
        '{\n  "name": "rea-agents",\n  "version": "6.8.0",\n  "repository": {\n'
        '    "type": "git",\n    "url": "git+https://github.com/morluto/rea.git"\n  }\n}\n'
    ),
}
JARS = {
    "jadx/jadx-headless-mcp-0.7.1-all.jar": b"stand-in for the JADX jar",
    "apktool/apktool_2.12.1.jar": b"stand-in for the apktool jar",
}


@pytest.fixture
def toolchain(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> Path:
    root = tmp_path / "repo"
    folder = root / "local/toolchain"
    for rel, text in PINNED_FILES.items():
        (folder / rel).parent.mkdir(parents=True, exist_ok=True)
        (folder / rel).write_text(text, encoding="utf-8")
    for rel, data in JARS.items():
        (folder / rel).parent.mkdir(parents=True, exist_ok=True)
        (folder / rel).write_bytes(data)
    hashes = tuple(
        (tool, rel, hashlib.sha256(JARS[rel]).hexdigest()) for tool, rel, _ in project_checks.TOOLCHAIN_HASHES
    )
    monkeypatch.setattr(project_checks, "TOOLCHAIN_HASHES", hashes)
    return root


def check(root: Path) -> project_checks.Report:
    report = project_checks.Report()
    project_checks.check_rea_environment(root, report)
    return report


def test_pinned_toolchain_passes(toolchain: Path):
    report = check(toolchain)
    assert report.failures == []
    assert len(report.passes) == 8


def test_no_toolchain_is_not_applicable(tmp_path: Path):
    report = check(tmp_path)
    assert report.failures == []
    assert report.passes == []
    assert report.not_applicable == ["REA toolchain: local/toolchain/ is absent"]


def test_wrong_node_version_fails(toolchain: Path):
    header = toolchain / "local/toolchain/node/include/node/node_version.h"
    header.write_text(header.read_text(encoding="utf-8").replace("PATCH_VERSION 4", "PATCH_VERSION 1"))
    assert check(toolchain).failures == [
        "Node.js is 22.23.4: found 22.23.1 in local/toolchain/node/include/node/node_version.h"
    ]


def test_jdk_without_compiler_fails(toolchain: Path):
    release = toolchain / "local/toolchain/jdk/release"
    release.write_text(release.read_text(encoding="utf-8").replace(" jdk.compiler", ""))
    assert any(f.startswith("JDK module is jdk.compiler") for f in check(toolchain).failures)


def test_missing_ghidra_fails(toolchain: Path):
    (toolchain / "local/toolchain/ghidra/ghidra_12.1.4_PUBLIC/Ghidra/application.properties").unlink()
    assert any(f.startswith("Ghidra is 12.1.4: found nothing") for f in check(toolchain).failures)


def test_rea_from_another_repository_fails(toolchain: Path):
    package = toolchain / "local/toolchain/rea/node_modules/rea-agents/package.json"
    package.write_text(package.read_text(encoding="utf-8").replace("morluto/rea", "someone/rea"))
    assert any(f.startswith("rea-agents source is") for f in check(toolchain).failures)


def test_missing_jadx_jar_fails(toolchain: Path):
    (toolchain / "local/toolchain/jadx/jadx-headless-mcp-0.7.1-all.jar").unlink()
    assert any(f.startswith("jadx-headless-mcp 0.7.1 SHA-256") for f in check(toolchain).failures)


def test_changed_apktool_jar_fails(toolchain: Path):
    (toolchain / "local/toolchain/apktool/apktool_2.12.1.jar").write_bytes(b"another jar")
    assert any(f.startswith("apktool 2.12.1 SHA-256") for f in check(toolchain).failures)
