#!/usr/bin/env python3
"""Validate Levyra's Matt Pocock real-engineering integration."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLAUDE_ROOT = ".agents/claude"

REQUIRED_FILES = (
    "AGENTS.md",
    ".agents/skills/levyra-real-engineering/SKILL.md",
    ".agents/rules/levyra-workspace.md",
    f"{CLAUDE_ROOT}/settings.json",
    f"{CLAUDE_ROOT}/hooks/user-prompt-submit.sh",
    "scripts/sync_agent_runtime.py",
    "docs/agents/issue-tracker.md",
    "docs/agents/domain.md",
    "docs/ai/MATT_POCOCK_SKILLS.md",
    "docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md",
    "docs/ai/ANTIGRAVITY.md",
    "scripts/setup-ai.ps1",
    "scripts/setup-ai.sh",
)

EXPECTED_STAGES = (
    "grill-with-docs",
    "wayfinder",
    "to-spec",
    "to-tickets",
    "implement",
    "tdd",
    "diagnosing-bugs",
    "code-review",
    "domain-modeling",
)


def text(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def require(errors: list[str], relative_path: str, terms: tuple[str, ...]) -> None:
    path = ROOT / relative_path
    if not path.is_file():
        return
    body = text(relative_path)
    for term in terms:
        if term not in body:
            errors.append(f"{relative_path} is missing: {term}")


def main() -> int:
    errors: list[str] = []

    for relative_path in REQUIRED_FILES:
        if not (ROOT / relative_path).is_file():
            errors.append(f"missing Matt skills integration file: {relative_path}")

    if (ROOT / CLAUDE_ROOT / "skills").exists():
        errors.append("Claude must not keep a second tracked skill tree under .agents/claude/skills")

    canonical_path = ROOT / ".agents/skills/levyra-real-engineering/SKILL.md"
    if canonical_path.is_file():
        canonical = text(".agents/skills/levyra-real-engineering/SKILL.md")
        if "name: levyra-real-engineering" not in canonical:
            errors.append("canonical real-engineering skill has the wrong name")
        for stage in EXPECTED_STAGES:
            if stage not in canonical:
                errors.append(f"canonical real-engineering skill is missing stage: {stage}")
        for required in (
            "tiny obvious edits",
            "Levyra wins",
            "fresh context",
            "owner did not approve",
            "scripts/ai_quality_gate.py --profile fast",
            "scripts/ai_quality_gate.py --profile full",
        ):
            if required not in canonical:
                errors.append(f"canonical real-engineering contract is missing: {required}")

    require(
        errors,
        "AGENTS.md",
        ("## Agent skills", "### Issue tracker", "docs/agents/issue-tracker.md", "### Domain docs", "docs/agents/domain.md", "## Matt Pocock skills bootstrap", "levyra-real-engineering"),
    )
    require(errors, "docs/agents/issue-tracker.md", ("LUC4N3X/Levyra-deepsound", "owner explicitly authorizes", "PRs as a request surface: no"))
    require(errors, "docs/agents/domain.md", ("CONTEXT.md", "docs/adr/", "created lazily"))

    settings_relative = f"{CLAUDE_ROOT}/settings.json"
    settings_path = ROOT / settings_relative
    if settings_path.is_file():
        try:
            settings = json.loads(settings_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            errors.append(f"{settings_relative} is invalid: {exc}")
        else:
            enabled = settings.get("enabledPlugins", {}).get("mattpocock-skills@claude-plugins-official")
            if enabled is not True:
                errors.append("Claude mattpocock-skills plugin must be project-enabled from claude-plugins-official")

    require(
        errors,
        "scripts/sync_agent_runtime.py",
        ('ROOT / ".agents" / "skills"', 'Path("skills")', 'ROOT / ".claude"'),
    )

    for relative_path, skip_flag in (
        ("scripts/setup-ai.ps1", "SkipMattSkills"),
        ("scripts/setup-ai.sh", "skip-matt-skills"),
    ):
        path = ROOT / relative_path
        if not path.is_file():
            continue
        setup = text(relative_path)
        if "mattpocock/skills" not in setup:
            errors.append(f"{relative_path} does not bootstrap mattpocock/skills")
        if skip_flag not in setup:
            errors.append(f"{relative_path} is missing the Matt skills opt-out")
        if "scripts/validate_matt_skills.py" not in setup:
            errors.append(f"{relative_path} does not run the Matt integration validator")
        if "sync_agent_runtime.py" not in setup or "--runtime all" not in setup:
            errors.append(f"{relative_path} does not materialize the shared skill tree for Claude")
        for stage in ("setup-matt-pocock-skills", *EXPECTED_STAGES):
            if stage not in setup:
                errors.append(f"{relative_path} is missing focused skill: {stage}")

    for relative_path in (
        ".agents/rules/levyra-workspace.md",
        f"{CLAUDE_ROOT}/hooks/user-prompt-submit.sh",
        "docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md",
        "docs/ai/ANTIGRAVITY.md",
    ):
        path = ROOT / relative_path
        if path.is_file() and "levyra-real-engineering" not in text(relative_path):
            errors.append(f"{relative_path} does not route through levyra-real-engineering")

    if errors:
        print("Matt skills integration validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "Matt skills integration validation passed: upstream setup substrate, one canonical .agents skill tree, "
        "Claude skill projection, Codex bootstrap, ChatGPT routing, and Antigravity routing verified."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
