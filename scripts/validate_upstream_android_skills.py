#!/usr/bin/env python3
"""Validate Levyra's external Kotlin and Material 3 skill integration."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

REQUIRED = (
    "AGENTS.md",
    "app/AGENTS.md",
    ".agents/claude/settings.json",
    ".agents/skills/levyra-compose/SKILL.md",
    ".agents/skills/levyra-design-taste/SKILL.md",
    "docs/ai/UPSTREAM_ANDROID_SKILLS.md",
    "scripts/agent_skill_router.py",
    "scripts/setup-ai.ps1",
    "scripts/setup-ai.sh",
    "scripts/ensure-codex-tooling.ps1",
    "scripts/ensure-codex-tooling.sh",
)

MARKETPLACES = {
    "maxrave": ("maxrave-dev/kotlin-footguns", "kotlin-footguns@maxrave"),
    "material-3-skill": ("hamen/material-3-skill", "material-3@material-3-skill"),
}


def missing_required_files() -> list[str]:
    return [
        f"missing required integration file: {relative}"
        for relative in REQUIRED
        if not (ROOT / relative).is_file()
    ]


def validate_claude_plugins(errors: list[str]) -> None:
    settings = json.loads((ROOT / ".agents/claude/settings.json").read_text(encoding="utf-8"))
    sources = settings.get("extraKnownMarketplaces", {})
    plugins = settings.get("enabledPlugins", {})
    for marketplace, (repository, plugin) in MARKETPLACES.items():
        source = sources.get(marketplace, {}).get("source", {})
        if source.get("source") != "github" or source.get("repo") != repository:
            errors.append(f"Claude marketplace missing or invalid: {marketplace}")
        if plugins.get(plugin) is not True:
            errors.append(f"Claude plugin not enabled: {plugin}")


def require_terms(errors: list[str], relative: str, terms: tuple[str, ...], label: str) -> None:
    content = (ROOT / relative).read_text(encoding="utf-8")
    errors.extend(f"{relative} missing {label}: {term}" for term in terms if term not in content)


def validate_documentation(errors: list[str]) -> None:
    terms = ("kotlin-footguns", "material-3")
    for relative in ("AGENTS.md", "app/AGENTS.md", "docs/ai/UPSTREAM_ANDROID_SKILLS.md"):
        require_terms(errors, relative, terms, "reference")


def validate_codex_bootstrap(errors: list[str]) -> None:
    terms = ("maxrave-dev/kotlin-footguns", "hamen/material-3-skill", "-a codex")
    for relative in (
        "scripts/setup-ai.ps1",
        "scripts/setup-ai.sh",
        "scripts/ensure-codex-tooling.ps1",
        "scripts/ensure-codex-tooling.sh",
    ):
        require_terms(errors, relative, terms, "Codex bootstrap")


def validate_skill_router(errors: list[str]) -> None:
    terms = ("MAX_ACTIVE_SKILLS = 2", "material-3 SKILL.md", "kotlin-footguns")
    require_terms(errors, "scripts/agent_skill_router.py", terms, "skill router")


def report_errors(errors: list[str]) -> int:
    print("Upstream Android skills integration validation failed:", file=sys.stderr)
    for error in errors:
        print(f"- {error}", file=sys.stderr)
    return 1


def main() -> int:
    errors = missing_required_files()
    if errors:
        return report_errors(errors)

    validate_claude_plugins(errors)
    validate_documentation(errors)
    validate_codex_bootstrap(errors)
    validate_skill_router(errors)
    if errors:
        return report_errors(errors)

    print("Upstream Kotlin footguns and Material 3 skills integration validated.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
