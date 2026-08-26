#!/usr/bin/env python3
"""Validate Levyra's deterministic Codex bootstrap contract."""

from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    import tomllib
except ModuleNotFoundError:  # pragma: no cover
    tomllib = None  # type: ignore[assignment]

ROOT = Path(__file__).resolve().parents[1]
CODEX_ROOT = ".agents/codex"
EXPECTED_VERSION = "1.108.279"
EXPECTED_WHEEL_SHA256 = "d192998a9abcd8afa1474f2a1637e260a9ab48fdd0080b6b690632b2af1e30f1"
REQUIRED_FILES = (
    f"{CODEX_ROOT}/config.toml",
    f"{CODEX_ROOT}/hooks.json",
    ".agents/skills/levyra-codex-bootstrap/SKILL.md",
    "scripts/codex_jcodemunch.py",
    "scripts/ensure-codex-tooling.ps1",
    "scripts/ensure-codex-tooling.sh",
    "scripts/sync_agent_runtime.py",
    "docs/ai/CODEX_AUTO_BOOTSTRAP.md",
)


def main() -> int:
    errors: list[str] = []

    for relative in REQUIRED_FILES:
        if not (ROOT / relative).is_file():
            errors.append(f"missing Codex bootstrap file: {relative}")

    config_relative = f"{CODEX_ROOT}/config.toml"
    config_path = ROOT / config_relative
    if config_path.is_file():
        if tomllib is None:
            errors.append(f"Python 3.11+ is required to parse {config_relative}")
        else:
            try:
                config = tomllib.loads(config_path.read_text(encoding="utf-8"))
            except tomllib.TOMLDecodeError as exc:
                errors.append(f"{config_relative} is invalid TOML: {exc}")
            else:
                server = config.get("mcp_servers", {}).get("jcodemunch", {})
                if server.get("required") is not False:
                    errors.append("jCodeMunch MCP must remain optional/fail-open")
                if server.get("command") != "python":
                    errors.append("jCodeMunch MCP must launch through the checked-in Python bootstrap")
                if server.get("args") != ["scripts/codex_jcodemunch.py", "serve"]:
                    errors.append("jCodeMunch MCP launcher arguments changed unexpectedly")

    hooks_relative = f"{CODEX_ROOT}/hooks.json"
    hooks_path = ROOT / hooks_relative
    if hooks_path.is_file():
        try:
            hooks = json.loads(hooks_path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            errors.append(f"{hooks_relative} is invalid JSON: {exc}")
        else:
            session = hooks.get("hooks", {}).get("SessionStart", [])
            serialized = json.dumps(session)
            for required in (
                "ensure-codex-tooling.ps1",
                "ensure-codex-tooling.sh",
                "Token savings never override correctness",
            ):
                if required not in serialized:
                    errors.append(f"SessionStart hook is missing: {required}")

    runtime_path = ROOT / "scripts/codex_jcodemunch.py"
    if runtime_path.is_file():
        runtime = runtime_path.read_text(encoding="utf-8")
        if f'JCODEMUNCH_VERSION = "{EXPECTED_VERSION}"' not in runtime:
            errors.append("jCodeMunch version pin changed unexpectedly")
        if f'JCODEMUNCH_WHEEL_SHA256 = "{EXPECTED_WHEEL_SHA256}"' not in runtime:
            errors.append("jCodeMunch wheel SHA-256 pin changed unexpectedly")
        if "#sha256=" not in runtime:
            errors.append("jCodeMunch wheel URL is not hash-bound")
        if "os.execv" not in runtime:
            errors.append("MCP launcher must hand stdio directly to the verified server")

    skill_path = ROOT / ".agents/skills/levyra-codex-bootstrap/SKILL.md"
    if skill_path.is_file():
        skill = skill_path.read_text(encoding="utf-8")
        for required in (
            "fail-open",
            "Do not add read/search blocking rules",
            "Token savings never override correctness",
            "Do not add Token Optimizer",
        ):
            if required not in skill:
                errors.append(f"Codex bootstrap skill is missing safety rule: {required}")

    sync_path = ROOT / "scripts/sync_agent_runtime.py"
    if sync_path.is_file():
        sync = sync_path.read_text(encoding="utf-8")
        for required in (".agents", ".codex", "config.toml", "hooks.json"):
            if required not in sync:
                errors.append(f"runtime sync is missing Codex projection contract: {required}")

    if errors:
        print("Codex bootstrap validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "Codex bootstrap validation passed: canonical .agents/codex config, "
        "runtime projection, pinned jCodeMunch, fail-open behavior, and quality-first policy verified."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
