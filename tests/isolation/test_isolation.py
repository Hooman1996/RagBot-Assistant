"""Tests for the reset guard, the development-host allowlist and the isolation suite."""

from __future__ import annotations

import importlib.util
import shutil
import sys
from pathlib import Path

import pytest

REPO = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("project_checks", REPO / "tools" / "project_checks.py")
project_checks = importlib.util.module_from_spec(spec)
sys.modules["project_checks"] = project_checks  # dataclasses resolve their module by name
spec.loader.exec_module(project_checks)

KINDS = sorted(project_checks.PROJECT_TARGETS)


@pytest.fixture
def checkout(tmp_path: Path) -> Path:
    root = tmp_path / "repo"
    root.mkdir()
    shutil.copy2(REPO / ".env.example", root / ".env.example")
    shutil.copytree(REPO / "deployment", root / "deployment")
    return root


def replace(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    assert old in text
    path.write_text(text.replace(old, new), encoding="utf-8")


def failures(root: Path) -> list[str]:
    return project_checks.run("isolation", root).failures


@pytest.mark.parametrize("kind", KINDS)
@pytest.mark.parametrize("name", ["hihelp_db", "hihelp_embeddings", "hihelp-documents"])
def test_guard_rejects_ragbot_targets(kind: str, name: str):
    with pytest.raises(ValueError, match="RagBot resource"):
        project_checks.guard_target(kind, name)


@pytest.mark.parametrize(
    ("kind", "name"),
    [
        ("database", "postgres"),
        ("database", "ragbot_assistant_prod"),
        ("schema", "public"),
        ("role", "postgres"),
        ("collection", "embeddings"),
        ("bucket", "documents"),
        ("database", ""),
        ("schema", "ragbot_assistant_dev"),
        ("table", "assistant"),
    ],
)
def test_guard_rejects_targets_outside_the_allowlist(kind: str, name: str):
    with pytest.raises(ValueError, match="not a project"):
        project_checks.guard_target(kind, name)


@pytest.mark.parametrize("kind", KINDS)
def test_guard_accepts_project_targets(kind: str):
    for name in project_checks.PROJECT_TARGETS[kind]:
        project_checks.guard_target(kind, name)


@pytest.mark.parametrize(
    "value", ["localhost", "127.0.0.1", "http://localhost:6333", "http://127.0.0.1:8010/api"]
)
def test_development_hosts_are_accepted(value: str):
    assert project_checks.is_development_host(value)


@pytest.mark.parametrize(
    "value",
    [
        "https://api.bank.example",
        "http://10.0.0.5:8080",
        "http://localhost.bank.example",
        "https://bank.example/?next=http://localhost",
        "http://localhost@bank.example",
        "db.internal",
        "",
    ],
)
def test_other_hosts_are_rejected(value: str):
    assert not project_checks.is_development_host(value)


def test_repository_passes_isolation():
    assert failures(REPO) == []


@pytest.mark.parametrize(
    ("old", "new", "expected"),
    [
        ("POSTGRES_DB=ragbot_assistant_dev", "POSTGRES_DB=hihelp_db", "POSTGRES_DB"),
        ("QDRANT_COLLECTION=ragbot_assistant_knowledge", "QDRANT_COLLECTION=hihelp_embeddings", "QDRANT"),
        ("MINIO_BUCKET=ragbot-assistant-dev", "MINIO_BUCKET=hihelp-documents", "MINIO_BUCKET"),
        ("ASSISTANT_DB_USER=assistant_app", "ASSISTANT_DB_USER=postgres", "ASSISTANT_DB_USER"),
        ("MINIO_ENABLED=false", "MINIO_ENABLED=true", "MinIO is off"),
        ("QDRANT_URL=http://localhost:6333", "QDRANT_URL=https://qdrant.bank.example", "QDRANT_URL"),
        ("MOCK_BANK_URL=http://localhost:8010", "MOCK_BANK_URL=https://api.bank.example", "MOCK_BANK_URL"),
        ("ASSISTANT_DB_PASSWORD=change-me", "ASSISTANT_DB_PASSWORD=s3cret-value", "PASSWORD"),
    ],
)
def test_unsafe_env_example_fails(checkout: Path, old: str, new: str, expected: str):
    replace(checkout / ".env.example", old, new)
    assert any(expected in f for f in failures(checkout))


def test_missing_setting_fails(checkout: Path):
    replace(checkout / ".env.example", "QDRANT_COLLECTION=ragbot_assistant_knowledge\n", "")
    assert any("QDRANT_COLLECTION" in f for f in failures(checkout))


@pytest.mark.parametrize(
    ("old", "new", "expected"),
    [
        ("CREATE DATABASE ragbot_assistant_dev;", "CREATE DATABASE hihelp_db;", "RagBot"),
        ("CREATE SCHEMA mock_bank", "CREATE SCHEMA public", "schema names"),
        ("CREATE ROLE mock_bank_app", "CREATE ROLE ragbot_admin", "role names"),
        ("\\connect ragbot_assistant_dev", "\\connect postgres", "connects only"),
    ],
)
def test_unsafe_init_sql_fails(checkout: Path, old: str, new: str, expected: str):
    replace(checkout / "deployment/init_project_db.sql", old, new)
    assert any(expected in f for f in failures(checkout))
