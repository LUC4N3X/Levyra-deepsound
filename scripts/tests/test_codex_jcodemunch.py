from __future__ import annotations

import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / "scripts" / "codex_jcodemunch.py"


def load_module():
    spec = importlib.util.spec_from_file_location("codex_jcodemunch", MODULE_PATH)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_jcodemunch_release_is_pinned_by_version_and_sha256():
    module = load_module()
    assert module.JCODEMUNCH_VERSION == "1.108.279"
    assert len(module.JCODEMUNCH_WHEEL_SHA256) == 64
    assert module.JCODEMUNCH_WHEEL_URL.endswith(
        f"#sha256={module.JCODEMUNCH_WHEEL_SHA256}"
    )


def test_jcodemunch_cache_is_version_scoped():
    module = load_module()
    assert module.JCODEMUNCH_VERSION in str(module.cache_root())


def test_repo_root_resolves_from_script_location():
    module = load_module()
    assert module.repo_root() == ROOT
