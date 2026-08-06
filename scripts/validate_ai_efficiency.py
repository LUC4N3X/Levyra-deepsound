#!/usr/bin/env python3
"""Validate Levyra's repository-local RTK, plugin, and AI security setup."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import Any

try:
    import tomllib
except ModuleNotFoundError:  # Python < 3.11
    tomllib = None  # type: ignore[assignment]

ROOT = Path(__file__).resolve().parents[1]

REQUIRED_FILES = (
    ".rtk/filters.toml",
    ".agents/README.md",
    ".agents/rules/levyra-workspace.md",
    ".agents/skills/levyra-context-efficiency/SKILL.md",
    ".agents/skills/levyra-security-review/SKILL.md",
    ".claude/rules/context-efficiency.md",
    ".github/workflows/dependency-review.yml",
    ".github/workflows/pr-check.yml",
    "codex-plugins.txt",
    "docs/README.md",
    "docs/ai/README.md",
    "docs/ai/CODEX_SECURITY.md",
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
EXPECTED_PLUGINS = (
    "superpowers@openai-curated",
    "codex-security@openai-curated",
)
EXPECTED_FILTERS = (
    "levyra-agent-config",
    "levyra-coderabbit",
    "levyra-adb-logcat",
    "levyra-agent-tests",
)
FORBIDDEN_GRADLE_MARKERS = ("BUILD SUCCESSFUL", "BUILD FAILED")


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


def iter_strings(value: Any) -> list[str]:
    if isinstance(value, str):
        return [value]
    if isinstance(value, dict):
        strings: list[str] = []
        for key, child in value.items():
            strings.append(str(key))
            strings.extend(iter_strings(child))
        return strings
    if isinstance(value, list):
        strings = []
        for child in value:
            strings.extend(iter_strings(child))
        return strings
    return []


def validate_filters(errors: list[str]) -> None:
    filters_path = ROOT / ".rtk/filters.toml"
    if not filters_path.is_file():
        return
    if tomllib is None:
        fail(errors, "Python 3.11 or newer is required to parse .rtk/filters.toml")
        return

    try:
        with filters_path.open("rb") as source:
            document = tomllib.load(source)
    except tomllib.TOMLDecodeError as exc:
        fail(errors, f".rtk/filters.toml is invalid TOML: {exc}")
        return

    if document.get("schema_version") != 1:
        fail(errors, ".rtk/filters.toml must declare integer schema_version = 1")

    filters = document.get("filters")
    if not isinstance(filters, dict):
        fail(errors, ".rtk/filters.toml must contain a filters table")
        return

    for filter_name in EXPECTED_FILTERS:
        if filter_name not in filters:
            fail(errors, f"missing RTK filter: {filter_name}")

    for filter_name, config in filters.items():
        if not isinstance(config, dict):
            fail(errors, f"RTK filter {filter_name!r} must be a table")
            continue
        match_command = config.get("match_command")
        if not isinstance(match_command, str) or not match_command:
            fail(errors, f"RTK filter {filter_name!r} is missing match_command")
            continue
        if re.search(r"\bgradle(?:w|w\.bat)?\b", match_command, re.IGNORECASE):
            fail(
                errors,
                f"RTK filter {filter_name!r} must not replace RTK's Gradle handler",
            )
        filtered_values = {
            key: value
            for key, value in config.items()
            if key not in {"description"}
        }
        parsed_strings = iter_strings(filtered_values)
        for marker in FORBIDDEN_GRADLE_MARKERS:
            if any(marker in value for value in parsed_strings):
                fail(
                    errors,
                    f"RTK filter {filter_name!r} must not encode Gradle marker {marker!r}",
                )

    setup_filter = filters.get("levyra-agent-tests")
    if isinstance(setup_filter, dict):
        pattern = setup_filter.get("match_command")
        if isinstance(pattern, str):
            try:
                matcher = re.compile(pattern)
            except re.error as exc:
                fail(errors, f"levyra-agent-tests match_command is invalid regex: {exc}")
            else:
                for command in (
                    r".\scripts\setup-ai.ps1 -DryRun",
                    "./scripts/setup-ai.sh --dry-run",
                    r"pwsh .\scripts\setup-ai.ps1 -DryRun",
                    "bash ./scripts/setup-ai.sh --dry-run",
                ):
                    if matcher.search(command) is None:
                        fail(
                            errors,
                            "levyra-agent-tests does not match documented command: "
                            f"{command}",
                        )


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

    validate_filters(errors)

    plugin_path = ROOT / "codex-plugins.txt"
    if plugin_path.is_file():
        plugins = tuple(
            line.strip()
            for line in plugin_path.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.lstrip().startswith("#")
        )
        if plugins != EXPECTED_PLUGINS:
            fail(
                errors,
                "codex-plugins.txt must contain exactly the approved plugins in "
                f"order: {EXPECTED_PLUGINS!r}",
            )

    require_terms(
        errors,
        "scripts/setup-ai.ps1",
        "Windows setup script",
        (
            "[switch] $DryRun",
            "[switch] $InstallRtk",
            "[switch] $Plugins",
            "Install global RTK instructions for Codex",
            "rtk init -g --codex",
            "rtk init --agent antigravity",
            "rtk init -g --opencode",
            "codex plugin add",
            "Validation blocked: Python",
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
            "Install global RTK instructions for Codex",
            "rtk init -g --codex",
            "rtk init --agent antigravity",
            "rtk init -g --opencode",
            "codex plugin add",
            "[blocked] Python",
            "validate_agent_config.py",
            "validate_ai_efficiency.py",
        ),
    )
    require_terms(
        errors,
        "docs/ai/RTK.md",
        "RTK documentation",
        (
            "instruction-based Codex setup",
            "levyra-context-efficiency",
            ".rtk/filters.toml",
            "setup-ai.ps1",
            "setup-ai.sh",
            "codex-plugins.txt",
            "codex-security@openai-curated",
            "rtk gain",
            "ChrisTitusTech/titus-ai",
            "rtk-ai/rtk",
        ),
    )
    require_terms(
        errors,
        "docs/ai/CODEX_SECURITY.md",
        "Codex Security documentation",
        (
            "threat model",
            "identification",
            "validation",
            "remediation",
            "human review",
            "codex-security@openai-curated",
            "dependency-review-action",
        ),
    )
    require_terms(
        errors,
        ".agents/skills/levyra-security-review/SKILL.md",
        "security review skill",
        (
            "Threat model",
            "Identification",
            "Validation",
            "Remediation",
            "Revalidation",
            "Codex Security",
        ),
    )
    require_terms(
        errors,
        ".agents/README.md",
        "agent inventory",
        (
            "instruction-based Codex setup",
            "levyra-context-efficiency",
            "Codex Security",
            "docs/ai/CODEX_SECURITY.md",
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
            "levyra-security-review",
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
        ".github/workflows/dependency-review.yml",
        "dependency review workflow",
        (
            "actions/dependency-review-action@",
            "fail-on-severity: high",
            "permissions:",
            "contents: read",
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
        f"{len(EXPECTED_PLUGINS)} approved plugins, "
        "automatic cross-runtime discovery, security workflow, dependency review, "
        "and no local-model profiles."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
