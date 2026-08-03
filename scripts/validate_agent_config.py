#!/usr/bin/env python3
"""Validate Levyra's repository-local AI planning and skill configuration."""

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
    "docs/AGENTS.md",
    "SPEC.md",
    "ROADMAP.md",
    "TASKS.md",
    "docs/ARCHITECTURE.md",
    ".agents/README.md",
    "docs/ai/README.md",
    "docs/ai/WORKFLOW.md",
    "docs/ai/OPENCLAW.md",
    "docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md",
)

REFERENCE_FILES = (
    "AGENTS.md",
    ".agents/README.md",
    "docs/ai/README.md",
    "docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md",
)

SKILL_NAME_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
SKILL_REFERENCE_RE = re.compile(r"`(levyra-[a-z0-9-]+)`")


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


def main() -> int:
    errors: list[str] = []

    for relative_path in REQUIRED_FILES:
        path = ROOT / relative_path
        if not path.is_file():
            errors.append(f"missing required file: {relative_path}")

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

    missing_skills = sorted(referenced_skills - actual_skills)
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
        f"{len(referenced_skills)} documented skill references."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
