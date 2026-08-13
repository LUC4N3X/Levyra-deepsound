#!/usr/bin/env python3
"""Validate Levyra's pinned claude-mem integration contract."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

PIN = "claude-mem@13.15.0"
MODEL = "claude-haiku-4-5-20251001"

REQUIRED_FILES = (
    "docs/ai/CLAUDE_MEM.md",
    ".agents/skills/levyra-context-efficiency/SKILL.md",
    "scripts/setup-claude-mem.ps1",
    "scripts/setup-claude-mem.sh",
    "scripts/setup-ai.ps1",
    "scripts/setup-ai.sh",
)

REQUIRED_MEMORY_TERMS = (
    "claude-mem",
    "search",
    "timeline",
    "get_observations",
    "current repository",
    "cloud sync",
    "ChatGPT",
)

FORBIDDEN_SETUP_TERMS = (
    "CLAUDE_MEM_CLOUD_SYNC_TOKEN=",
    "CLAUDE_MEM_CLOUD_SYNC_HUB_URL=",
    "CLAUDE_MEM_SEMANTIC_INJECT=true",
)


def read(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def require_terms(
    errors: list[str],
    relative_path: str,
    terms: tuple[str, ...],
) -> None:
    path = ROOT / relative_path
    if not path.is_file():
        return
    text = read(relative_path)
    for term in terms:
        if term not in text:
            errors.append(f"{relative_path}: missing required term {term!r}")


def main() -> int:
    errors: list[str] = []

    for relative_path in REQUIRED_FILES:
        if not (ROOT / relative_path).is_file():
            errors.append(f"missing claude-mem integration file: {relative_path}")

    for relative_path in (
        "scripts/setup-claude-mem.ps1",
        "scripts/setup-claude-mem.sh",
        "docs/ai/CLAUDE_MEM.md",
    ):
        path = ROOT / relative_path
        if path.is_file() and PIN not in read(relative_path):
            errors.append(f"{relative_path}: claude-mem pin must be {PIN}")

    for relative_path in ("scripts/setup-claude-mem.ps1", "scripts/setup-claude-mem.sh"):
        if not (ROOT / relative_path).is_file():
            continue
        text = read(relative_path)
        for term in (
            "claude-code",
            "codex-cli",
            "antigravity",
            "telemetry",
            "doctor",
            "repair",
            MODEL,
        ):
            if term not in text:
                errors.append(f"{relative_path}: missing runtime/setup contract {term!r}")
        for forbidden in FORBIDDEN_SETUP_TERMS:
            if forbidden in text:
                errors.append(
                    f"{relative_path}: must not enable cloud sync or semantic injection: {forbidden!r}"
                )

    ps_setup_path = ROOT / "scripts/setup-claude-mem.ps1"
    if ps_setup_path.is_file():
        ps_setup = read("scripts/setup-claude-mem.ps1")
        for term in (
            "CLAUDE_MEM_HOOK_FAIL_LOUD_THRESHOLD",
            "999999999",
            "Set-WindowsFailOpenGuard",
        ):
            if term not in ps_setup:
                errors.append(
                    f"scripts/setup-claude-mem.ps1: missing Windows fail-open guard {term!r}"
                )

    for relative_path in (
        "docs/ai/CLAUDE_MEM.md",
        ".agents/skills/levyra-context-efficiency/SKILL.md",
    ):
        require_terms(errors, relative_path, REQUIRED_MEMORY_TERMS)

    require_terms(
        errors,
        "scripts/setup-ai.ps1",
        (
            "[switch] $ClaudeMem",
            "setup-claude-mem.ps1",
            "validate_claude_mem.py",
            "Continuing Levyra AI setup without persistent memory.",
        ),
    )
    require_terms(
        errors,
        "scripts/setup-ai.sh",
        (
            "--claude-mem",
            "setup-claude-mem.sh",
            "validate_claude_mem.py",
            "continuing Levyra AI setup without persistent memory.",
        ),
    )

    skill_path = ROOT / ".agents/skills/levyra-context-efficiency/SKILL.md"
    if skill_path.is_file():
        skill = read(".agents/skills/levyra-context-efficiency/SKILL.md")
        for term in (
            "Fail open",
            "Repository configuration alone cannot make ChatGPT reach a",
            "local claude-mem worker.",
            "scripts/setup-ai.ps1 -ClaudeMem",
            "./scripts/setup-ai.sh --claude-mem",
        ):
            if term not in skill:
                errors.append(
                    f".agents/skills/levyra-context-efficiency/SKILL.md: missing memory safety contract {term!r}"
                )

    doc_path = ROOT / "docs/ai/CLAUDE_MEM.md"
    if doc_path.is_file():
        doc = read("docs/ai/CLAUDE_MEM.md")
        for term in (
            "CLAUDE_MEM_HOOK_FAIL_LOUD_THRESHOLD=999999999",
            "thedotmack/claude-mem#3481",
            "Secure MCP Tunnel",
            "Cloud sync is an owner choice outside this integration.",
        ):
            if term not in doc:
                errors.append(
                    f"docs/ai/CLAUDE_MEM.md: missing documented integration invariant {term!r}"
                )

    for relative_path in (
        "scripts/setup-claude-mem.ps1",
        "scripts/setup-claude-mem.sh",
    ):
        if not (ROOT / relative_path).is_file():
            continue
        text = read(relative_path)
        install_calls = len(re.findall(r"\binstall\b", text))
        if install_calls == 0:
            errors.append(f"{relative_path}: no claude-mem install path found")

    if errors:
        print("claude-mem integration validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "claude-mem integration validation passed: "
        f"pin={PIN}, model={MODEL}, Claude Code/Codex/Antigravity setup present, "
        "ChatGPT MCP fallback documented, privacy and fail-open guards verified."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
