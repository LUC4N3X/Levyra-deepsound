#!/usr/bin/env python3
"""Validate Levyra's repository-local RTK and AI-efficiency integration."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

REQUIRED_FILES = (
    ".rtk/filters.toml",
    ".agents/README.md",
    ".agents/rules/levyra-workspace.md",
    ".agents/skills/levyra-context-efficiency/SKILL.md",
    ".claude/rules/context-efficiency.md",
    ".github/workflows/pr-check.yml",
    "codex-plugins.txt",
    "docs/README.md",
    "docs/ai/README.md",
    "docs/ai/RTK.md",
    "scripts/setup-ai.ps1",
    "scripts/setup-ai.sh",
)

FORBIDDEN_PATHS = (
    "codex-home/ollama.config.toml",
    "codex-home/llamacpp.config.toml",
    ".codex/ollama.config.toml",
    ".codex/llamacpp.config.toml",
)

EXPECTED_SKILL_NAME = "levyra-context-efficiency"
EXPECTED_PLUGIN = "superpowers@openai-curated"
EXPECTED_FILTERS = (
    "levyra-agent-config",
    "levyra-coderabbit",
    "levyra-adb-logcat",
    "levyra-agent-tests",
)


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def require_terms(
    errors: list[str],
    relative_path: str,
    label: str,
    terms: tuple[str, ...],
) -> None:
    path = ROOT / relative_path
    if not path.is_file():
        return
    text = path.read_text(encoding="utf-8")
    for term in terms:
        if term not in text:
            fail(errors, f"{label} is missing: {term}")


def main() -> int:
    errors: list[str] = []

    for relative_path in REQUIRED_FILES:
        if not (ROOT / relative_path).is_file():
            fail(errors, f"missing required AI-efficiency file: {relative_path}")

    for relative_path in FORBIDDEN_PATHS:
        if (ROOT / relative_path).exists():
            fail(errors, f"local-model profile is outside the approved scope: {relative_path}")

    skill_path = ROOT / ".agents/skills/levyra-context-efficiency/SKILL.md"
    if skill_path.is_file():
        skill = skill_path.read_text(encoding="utf-8")
        if not skill.startswith("---\n"):
            fail(errors, "context-efficiency skill is missing YAML front matter")
        if f"name: {EXPECTED_SKILL_NAME}" not in skill:
            fail(errors, "context-efficiency skill has the wrong front matter name")
        for required_term in (
            "RTK",
            "Automatic routing",
            "rerun",
            "exit status",
            "danger-full-access",
            "scripts/setup-ai.ps1",
            "scripts/setup-ai.sh",
        ):
            if required_term not in skill:
                fail(errors, f"context-efficiency skill is missing: {required_term}")

    filters_path = ROOT / ".rtk/filters.toml"
    if filters_path.is_file():
        filters = filters_path.read_text(encoding="utf-8")
        if not re.search(r"(?m)^schema_version\s*=\s*1\s*$", filters):
            fail(errors, ".rtk/filters.toml must declare schema_version = 1")
        for filter_name in EXPECTED_FILTERS:
            if f"[filters.{filter_name}]" not in filters:
                fail(errors, f"missing RTK filter: {filter_name}")
        if "BUILD SUCCESSFUL" in filters or "BUILD FAILED" in filters:
            fail(
                errors,
                "project-local filters must not replace RTK's dedicated Gradle handler",
            )

    plugin_path = ROOT / "codex-plugins.txt"
    if plugin_path.is_file():
        plugins = [
            line.strip()
            for line in plugin_path.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.lstrip().startswith("#")
        ]
        if plugins != [EXPECTED_PLUGIN]:
            fail(
                errors,
                "codex-plugins.txt must contain only the approved initial plugin "
                f"{EXPECTED_PLUGIN!r}",
            )

    require_terms(
        errors,
        "scripts/setup-ai.ps1",
        "Windows setup script",
        (
            "[switch] $DryRun",
            "[switch] $InstallRtk",
            "[switch] $Plugins",
            "rtk init -g --codex",
            "rtk init --agent antigravity",
            "rtk init -g --opencode",
            "codex plugin add",
            "validate_agent_config.py",
            "validate_ai_efficiency.py",
        ),
    )
    require_terms(
        errors,
        "scripts/setup-ai.sh",
        "Unix setup script",
        (
            "set -euo pipefail",
            "--dry-run",
            "--install-rtk",
            "--plugins",
            "rtk init -g --codex",
            "rtk init --agent antigravity",
            "rtk init -g --opencode",
            "codex plugin add",
            "validate_agent_config.py",
            "validate_ai_efficiency.py",
        ),
    )
    require_terms(
        errors,
        "docs/ai/RTK.md",
        "RTK documentation",
        (
            "levyra-context-efficiency",
            ".rtk/filters.toml",
            "scripts/setup-ai.ps1",
            "scripts/setup-ai.sh",
            "codex-plugins.txt",
            "rtk gain",
            "ChrisTitusTech/titus-ai",
            "rtk-ai/rtk",
        ),
    )
    require_terms(
        errors,
        ".agents/README.md",
        "agent inventory",
        (
            "levyra-context-efficiency",
            ".rtk/filters.toml",
            "scripts/setup-ai.ps1",
            "scripts/setup-ai.sh",
        ),
    )
    require_terms(
        errors,
        ".agents/rules/levyra-workspace.md",
        "workspace rule",
        (
            "levyra-context-efficiency",
            "RTK",
            "rerun the exact command",
        ),
    )
    require_terms(
        errors,
        ".claude/rules/context-efficiency.md",
        "Claude context-efficiency rule",
        (
            "levyra-context-efficiency",
            ".rtk/filters.toml",
            "Rerun the exact command raw",
        ),
    )
    require_terms(
        errors,
        ".github/workflows/pr-check.yml",
        "PR workflow",
        (
            "Validate Agent Configuration",
            "Validate AI Efficiency Layer",
            "python3 scripts/validate_ai_efficiency.py",
        ),
    )

    if errors:
        print("AI efficiency validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "AI efficiency validation passed: "
        f"{len(REQUIRED_FILES)} required files, "
        f"{len(EXPECTED_FILTERS)} project filters, "
        "automatic cross-runtime discovery, plugin manifest, and no local-model profiles."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
