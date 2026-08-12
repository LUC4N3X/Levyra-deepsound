#!/usr/bin/env python3
"""Validate Levyra's repository-local AI planning and agent configuration."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

REQUIRED_FILES = (
    "AGENTS.md",
    "app/AGENTS.md",
    "desktop/AGENTS.md",
    ".github/AGENTS.md",
    "docs/README.md",
    "docs/AGENTS.md",
    "docs/ARCHITECTURE.md",
    "docs/project/README.md",
    "docs/project/SPEC.md",
    "docs/project/ROADMAP.md",
    "docs/project/TASKS.md",
    ".agents/README.md",
    ".agents/rules/levyra-workspace.md",
    ".agents/skills/levyra-design-taste/SKILL.md",
    ".agents/skills/levyra-android-performance/SKILL.md",
    ".agents/skills/levyra-r8-proguard/SKILL.md",
    ".agents/skills/levyra-android-intent-security/SKILL.md",
    ".claude/CLAUDE.md",
    ".claude/hooks/user-prompt-submit.sh",
    ".claude/skills/levyra-real-engineering/SKILL.md",
    ".claude/skills/levyra-compose/SKILL.md",
    ".claude/skills/levyra-design-taste/SKILL.md",
    ".claude/skills/levyra-android-performance/SKILL.md",
    ".claude/skills/levyra-r8-proguard/SKILL.md",
    ".claude/skills/levyra-android-intent-security/SKILL.md",
    ".claude/skills/levyra-ci-workflows/SKILL.md",
    ".claude/skills/levyra-context-efficiency/SKILL.md",
    ".claude/skills/levyra-release-check/SKILL.md",
    ".claude/skills/levyra-pr-review/SKILL.md",
    "docs/ai/README.md",
    "docs/ai/WORKFLOW.md",
    "docs/ai/AI_ENGINEERING_GUARDRAILS.md",
    "docs/ai/ANTIGRAVITY.md",
    "docs/ai/OPENCLAW.md",
    "docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md",
)

FORBIDDEN_DUPLICATE_FILES = (
    "SPEC.md",
    "ROADMAP.md",
    "TASKS.md",
)

REFERENCE_FILES = (
    "AGENTS.md",
    ".agents/README.md",
    ".agents/rules/levyra-workspace.md",
    "docs/AGENTS.md",
    "docs/ai/README.md",
    "docs/ai/WORKFLOW.md",
    "docs/ai/ANTIGRAVITY.md",
    "docs/ai/OPENCLAW.md",
    "docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md",
)

ANTIGRAVITY_RULE_PATH = ".agents/rules/levyra-workspace.md"
ANTIGRAVITY_RULE_ROOT_REFERENCE = "@../../AGENTS.md"
ANTIGRAVITY_SKILLS_PATH = ".agents/skills/"
CLAUDE_INSTRUCTIONS_PATH = ".claude/CLAUDE.md"
CLAUDE_ROUTER_PATH = ".claude/hooks/user-prompt-submit.sh"
CHATGPT_INSTRUCTIONS_PATH = "docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md"
GUARDRAILS_PATH = "docs/ai/AI_ENGINEERING_GUARDRAILS.md"

AUTOMATIC_ROUTED_SKILLS = (
    "levyra-real-engineering",
    "levyra-compose",
    "levyra-design-taste",
    "levyra-android-performance",
    "levyra-r8-proguard",
    "levyra-android-intent-security",
    "levyra-ci-workflows",
    "levyra-context-efficiency",
    "levyra-pr-review",
    "levyra-release-check",
)

CLAUDE_CANONICAL_BRIDGES = (
    "levyra-real-engineering",
    "levyra-compose",
    "levyra-design-taste",
    "levyra-android-performance",
    "levyra-r8-proguard",
    "levyra-android-intent-security",
    "levyra-ci-workflows",
    "levyra-context-efficiency",
    "levyra-release-check",
)

SKILL_NAME_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
SKILL_REFERENCE_RE = re.compile(r"`(levyra-[a-z0-9-]+)`")
DOCUMENTED_AGENT_IDS = {
    "levyra-ci",
    "levyra-reviewer",
    "levyra-worker",
}


def read_text(relative_path: str) -> str:
    path = ROOT / relative_path
    return path.read_text(encoding="utf-8")


def parse_front_matter(path: Path) -> dict[str, str]:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()

    if not lines or lines[0].strip() != "---":
        raise ValueError("missing opening YAML front matter delimiter")

    try:
        end_index = next(
            index
            for index, line in enumerate(lines[1:], start=1)
            if line.strip() == "---"
        )
    except StopIteration as exc:
        raise ValueError("missing closing YAML front matter delimiter") from exc

    metadata: dict[str, str] = {}
    for line in lines[1:end_index]:
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if ":" not in line:
            raise ValueError(f"invalid front matter line: {line!r}")
        key, value = line.split(":", 1)
        metadata[key.strip()] = value.strip()

    return metadata


def missing_skill_references(
    referenced_skills: set[str],
    actual_skills: set[str],
) -> list[str]:
    return sorted(referenced_skills - actual_skills - DOCUMENTED_AGENT_IDS)


def require_skill_references(
    errors: list[str],
    relative_path: str,
    text: str,
    skills: tuple[str, ...],
    runtime: str,
) -> None:
    for skill in skills:
        if skill not in text:
            errors.append(
                f"{relative_path}: missing {runtime} automatic route for {skill}"
            )


def require_terms(
    errors: list[str],
    relative_path: str,
    text: str,
    terms: tuple[str, ...],
    label: str,
) -> None:
    for term in terms:
        if term not in text:
            errors.append(f"{relative_path}: missing {label}: {term}")


def main() -> int:
    errors: list[str] = []

    for relative_path in REQUIRED_FILES:
        path = ROOT / relative_path
        if not path.is_file():
            errors.append(f"missing required file: {relative_path}")

    for relative_path in FORBIDDEN_DUPLICATE_FILES:
        path = ROOT / relative_path
        if path.exists():
            errors.append(
                f"obsolete root planning file must be removed: {relative_path}"
            )

    antigravity_rule_path = ROOT / ANTIGRAVITY_RULE_PATH
    if antigravity_rule_path.is_file():
        try:
            antigravity_rule = antigravity_rule_path.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            errors.append(f"{ANTIGRAVITY_RULE_PATH}: cannot read file: {exc}")
        else:
            if ANTIGRAVITY_RULE_ROOT_REFERENCE not in antigravity_rule:
                errors.append(
                    f"{ANTIGRAVITY_RULE_PATH}: missing canonical root reference "
                    f"{ANTIGRAVITY_RULE_ROOT_REFERENCE!r}"
                )
            require_skill_references(
                errors,
                ANTIGRAVITY_RULE_PATH,
                antigravity_rule,
                AUTOMATIC_ROUTED_SKILLS,
                "shared workspace",
            )
            require_terms(
                errors,
                ANTIGRAVITY_RULE_PATH,
                antigravity_rule,
                ("AI_ENGINEERING_GUARDRAILS.md", "levyra-context-efficiency"),
                "Antigravity shared guardrail/context route",
            )

    antigravity_guide_path = ROOT / "docs/ai/ANTIGRAVITY.md"
    if antigravity_guide_path.is_file():
        try:
            antigravity_guide = antigravity_guide_path.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            errors.append(f"docs/ai/ANTIGRAVITY.md: cannot read file: {exc}")
        else:
            if ANTIGRAVITY_SKILLS_PATH not in antigravity_guide:
                errors.append(
                    "docs/ai/ANTIGRAVITY.md: missing canonical workspace skills path"
                )
            if ANTIGRAVITY_RULE_PATH not in antigravity_guide:
                errors.append(
                    "docs/ai/ANTIGRAVITY.md: missing workspace rule reference"
                )

    codex_instructions_path = ROOT / "AGENTS.md"
    if codex_instructions_path.is_file():
        try:
            codex_instructions = codex_instructions_path.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            errors.append(f"AGENTS.md: cannot read file: {exc}")
        else:
            require_skill_references(
                errors,
                "AGENTS.md",
                codex_instructions,
                AUTOMATIC_ROUTED_SKILLS,
                "Codex",
            )
            require_terms(
                errors,
                "AGENTS.md",
                codex_instructions,
                (
                    "Always-on context budget",
                    "levyra-context-efficiency",
                    "AI_ENGINEERING_GUARDRAILS.md",
                    "Do not add explanatory source-code comments",
                ),
                "Codex context/code-quality contract",
            )

    chatgpt_instructions_path = ROOT / CHATGPT_INSTRUCTIONS_PATH
    if chatgpt_instructions_path.is_file():
        try:
            chatgpt_instructions = chatgpt_instructions_path.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            errors.append(f"{CHATGPT_INSTRUCTIONS_PATH}: cannot read file: {exc}")
        else:
            require_skill_references(
                errors,
                CHATGPT_INSTRUCTIONS_PATH,
                chatgpt_instructions,
                AUTOMATIC_ROUTED_SKILLS,
                "ChatGPT",
            )
            require_terms(
                errors,
                CHATGPT_INSTRUCTIONS_PATH,
                chatgpt_instructions,
                ("AI_ENGINEERING_GUARDRAILS.md", "code-review"),
                "ChatGPT shared guardrail/review route",
            )

    claude_instructions_path = ROOT / CLAUDE_INSTRUCTIONS_PATH
    if claude_instructions_path.is_file():
        try:
            claude_instructions = claude_instructions_path.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            errors.append(f"{CLAUDE_INSTRUCTIONS_PATH}: cannot read file: {exc}")
        else:
            require_skill_references(
                errors,
                CLAUDE_INSTRUCTIONS_PATH,
                claude_instructions,
                AUTOMATIC_ROUTED_SKILLS,
                "Claude",
            )
            require_terms(
                errors,
                CLAUDE_INSTRUCTIONS_PATH,
                claude_instructions,
                (
                    "Immediate context budget",
                    "AI_ENGINEERING_GUARDRAILS.md",
                    "levyra-context-efficiency",
                    "/code-review",
                    "Do not add explanatory source-code comments",
                ),
                "Claude immediate context/review contract",
            )

    claude_router_path = ROOT / CLAUDE_ROUTER_PATH
    if claude_router_path.is_file():
        try:
            claude_router = claude_router_path.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            errors.append(f"{CLAUDE_ROUTER_PATH}: cannot read file: {exc}")
        else:
            require_skill_references(
                errors,
                CLAUDE_ROUTER_PATH,
                claude_router,
                AUTOMATIC_ROUTED_SKILLS,
                "Claude hook",
            )
            require_terms(
                errors,
                CLAUDE_ROUTER_PATH,
                claude_router,
                ("Levyra context budget", "code-review"),
                "Claude hook context/review reminder",
            )

    guardrails_path = ROOT / GUARDRAILS_PATH
    if guardrails_path.is_file():
        try:
            guardrails = guardrails_path.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            errors.append(f"{GUARDRAILS_PATH}: cannot read file: {exc}")
        else:
            require_terms(
                errors,
                GUARDRAILS_PATH,
                guardrails,
                (
                    "Source-code comment discipline",
                    "Mandatory pre-delivery code review",
                    "/code-review",
                    "Claude Code",
                    "Codex",
                    "ChatGPT",
                    "Google Antigravity",
                ),
                "cross-runtime pre-delivery review contract",
            )

    for skill in CLAUDE_CANONICAL_BRIDGES:
        relative_path = f".claude/skills/{skill}/SKILL.md"
        path = ROOT / relative_path
        if not path.is_file():
            continue
        try:
            bridge = path.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            errors.append(f"{relative_path}: cannot read file: {exc}")
            continue

        canonical_reference = f".agents/skills/{skill}/SKILL.md"
        if canonical_reference not in bridge:
            errors.append(
                f"{relative_path}: missing canonical bridge to {canonical_reference}"
            )

    skills_root = ROOT / ".agents" / "skills"
    skill_paths = sorted(skills_root.glob("*/SKILL.md"))
    if not skill_paths:
        errors.append("no native skills found under .agents/skills")

    actual_skills: set[str] = set()
    for skill_path in skill_paths:
        relative_path = skill_path.relative_to(ROOT).as_posix()
        directory_name = skill_path.parent.name

        try:
            metadata = parse_front_matter(skill_path)
        except (OSError, UnicodeError, ValueError) as exc:
            errors.append(f"{relative_path}: {exc}")
            continue

        name = metadata.get("name", "")
        description = metadata.get("description", "")

        if not name:
            errors.append(f"{relative_path}: missing front matter name")
        elif not SKILL_NAME_RE.fullmatch(name):
            errors.append(f"{relative_path}: invalid skill name {name!r}")
        elif name != directory_name:
            errors.append(
                f"{relative_path}: front matter name {name!r} "
                f"does not match directory {directory_name!r}"
            )
        elif name in actual_skills:
            errors.append(f"{relative_path}: duplicate skill name {name!r}")
        else:
            actual_skills.add(name)

        if not description:
            errors.append(f"{relative_path}: missing front matter description")

    missing_automatic_skills = sorted(set(AUTOMATIC_ROUTED_SKILLS) - actual_skills)
    for name in missing_automatic_skills:
        errors.append(f"automatic route points to missing native skill: {name}")

    referenced_skills: set[str] = set()
    for relative_path in REFERENCE_FILES:
        path = ROOT / relative_path
        if not path.is_file():
            continue
        try:
            referenced_skills.update(
                SKILL_REFERENCE_RE.findall(read_text(relative_path))
            )
        except (OSError, UnicodeError) as exc:
            errors.append(f"{relative_path}: cannot read file: {exc}")

    missing_skills = missing_skill_references(referenced_skills, actual_skills)
    for name in missing_skills:
        errors.append(f"documented skill does not exist: {name}")

    inventory_path = ROOT / ".agents" / "README.md"
    if inventory_path.is_file():
        inventory_skills = set(
            SKILL_REFERENCE_RE.findall(inventory_path.read_text(encoding="utf-8"))
        )
        undocumented_skills = sorted(actual_skills - inventory_skills)
        for name in undocumented_skills:
            errors.append(f"skill missing from .agents/README.md inventory: {name}")

    if errors:
        print("Agent configuration validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "Agent configuration validation passed: "
        f"{len(REQUIRED_FILES)} required files, "
        f"{len(actual_skills)} native skills, "
        f"{len(referenced_skills)} documented skill references, "
        f"{len(AUTOMATIC_ROUTED_SKILLS)} automatic cross-runtime routes, "
        "Codex/ChatGPT/Claude/Antigravity context budget and pre-delivery review verified, "
        "no duplicate root planning files."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
