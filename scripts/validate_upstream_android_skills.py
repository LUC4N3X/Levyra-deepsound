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


def main() -> int:
    errors: list[str] = []
    for relative in REQUIRED:
        if not (ROOT / relative).is_file():
            errors.append(f"missing required integration file: {relative}")
    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1

    settings = json.loads((ROOT / ".agents/claude/settings.json").read_text(encoding="utf-8"))
    for marketplace, (repository, plugin) in MARKETPLACES.items():
        source = settings.get("extraKnownMarketplaces", {}).get(marketplace, {}).get("source", {})
        if source.get("source") != "github" or source.get("repo") != repository:
            errors.append(f"Claude marketplace missing or invalid: {marketplace}")
        if settings.get("enabledPlugins", {}).get(plugin) is not True:
            errors.append(f"Claude plugin not enabled: {plugin}")

    for relative in (
        "AGENTS.md",
        "app/AGENTS.md",
        "docs/ai/UPSTREAM_ANDROID_SKILLS.md",
    ):
        content = (ROOT / relative).read_text(encoding="utf-8")
        for term in ("kotlin-footguns", "material-3"):
            if term not in content:
                errors.append(f"{relative} missing {term}")

    for relative in (
        "scripts/setup-ai.ps1",
        "scripts/setup-ai.sh",
        "scripts/ensure-codex-tooling.ps1",
        "scripts/ensure-codex-tooling.sh",
    ):
        content = (ROOT / relative).read_text(encoding="utf-8")
        for term in ("maxrave-dev/kotlin-footguns", "hamen/material-3-skill", "-a codex"):
            if term not in content:
                errors.append(f"{relative} missing Codex bootstrap: {term}")

    router = (ROOT / "scripts/agent_skill_router.py").read_text(encoding="utf-8")
    for term in ("MAX_ACTIVE_SKILLS = 2", "material-3 SKILL.md", "kotlin-footguns"):
        if term not in router:
            errors.append(f"skill router missing: {term}")

    if errors:
        print("Upstream Android skills integration validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("Upstream Kotlin footguns and Material 3 skills integration validated.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
