"""Tests for tools/project_checks.py, run against throwaway copies of the repository."""

from __future__ import annotations

import importlib.util
import shutil
import sys
import zipfile
from pathlib import Path

import pytest

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("project_checks", REPO / "tools" / "project_checks.py")
project_checks = importlib.util.module_from_spec(spec)
sys.modules["project_checks"] = project_checks  # dataclasses resolve their module by name
spec.loader.exec_module(project_checks)

COPIED = (
    "AGENTS.md",
    "CLAUDE.md",
    ".gitignore",
    ".github",
    "app_profiles",
    "contracts",
    "docs",
    "tools",
)


@pytest.fixture
def checkout(tmp_path: Path) -> Path:
    root = tmp_path / "repo"
    root.mkdir()
    for name in COPIED:
        src = REPO / name
        if src.is_dir():
            shutil.copytree(src, root / name)
        else:
            shutil.copy2(src, root / name)
    return root


def failures(suite: str, root: Path) -> list[str]:
    return project_checks.run(suite, root).failures


def test_repository_passes_all_checks():
    assert failures("all", REPO) == []


def test_clean_copy_passes(checkout: Path):
    assert failures("all", checkout) == []


def test_mirror_drift_fails(checkout: Path):
    (checkout / "CLAUDE.md").write_text("drifted\n", encoding="utf-8")
    assert any("CLAUDE.md mirrors AGENTS.md" in f for f in failures("foundation", checkout))


def test_missing_template_field_fails(checkout: Path):
    template = checkout / ".github/ISSUE_TEMPLATE/task.yml"
    text = template.read_text(encoding="utf-8").replace("id: stop_condition", "id: something_else")
    template.write_text(text, encoding="utf-8")
    assert any("stop_condition" in f for f in failures("foundation", checkout))


@pytest.mark.parametrize("name", ["app-release.apk", "bundle.aab", "classes.dex", "release.jks"])
def test_forbidden_extension_fails(checkout: Path, name: str):
    (checkout / name).write_bytes(b"not a real package")
    assert any(name in f for f in failures("foundation", checkout))


def test_renamed_android_package_fails(checkout: Path):
    fake = checkout / "fixture.zip"
    with zipfile.ZipFile(fake, "w") as archive:
        archive.writestr("AndroidManifest.xml", "<manifest/>")
        archive.writestr("classes.dex", "dex\n035")
    assert any("fixture.zip" in f for f in failures("foundation", checkout))


def test_dex_magic_fails(checkout: Path):
    (checkout / "blob.bin").write_bytes(b"dex\n035\0" + b"\0" * 32)
    assert any("blob.bin" in f for f in failures("foundation", checkout))


def test_evidence_folder_fails(checkout: Path):
    folder = checkout / "notes" / "decompiled"
    folder.mkdir(parents=True)
    (folder / "Main.java").write_text("class Main {}\n", encoding="utf-8")
    assert any("decompiled" in f for f in failures("foundation", checkout))


def test_secret_in_text_fails(checkout: Path):
    # Built at runtime so this test file itself never contains a key-shaped string.
    token = "gh" + "p_" + "A" * 36
    (checkout / "settings.py").write_text(f'TOKEN = "{token}"\n', encoding="utf-8")
    assert any("GitHub token" in f for f in failures("foundation", checkout))


def test_private_key_block_fails(checkout: Path):
    block = "-----BEGIN " + "RSA PRIVATE KEY-----\nabc\n"
    (checkout / "id_rsa.txt").write_text(block, encoding="utf-8")
    assert any("private key block" in f for f in failures("foundation", checkout))


def test_manifest_without_sha_fails(checkout: Path):
    manifest = checkout / "docs/source-manifest.md"
    text = manifest.read_text(encoding="utf-8").replace(project_checks.RAGBOT_BASELINE_SHA, "main")
    manifest.write_text(text, encoding="utf-8")
    assert any("full RagBot SHA" in f for f in failures("baseline", checkout))


def test_missing_plan_fails(checkout: Path):
    (checkout / project_checks.PLAN_FILE).unlink()
    assert any("master plan" in f for f in failures("baseline", checkout))


def test_database_dump_in_docs_fails(checkout: Path):
    (checkout / "docs" / "export.sql").write_text("select 1;\n", encoding="utf-8")
    assert any("export.sql" in f for f in failures("baseline", checkout))


def test_cli_exit_codes(checkout: Path):
    assert project_checks.main(["all", "--root", str(checkout)]) == 0
    (checkout / "CLAUDE.md").unlink()
    assert project_checks.main(["foundation", "--root", str(checkout)]) == 1
