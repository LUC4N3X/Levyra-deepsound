#!/usr/bin/env python3
"""Validate Levyra's repository-local agent configuration."""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLAUDE_ROOT = ".agents/claude"
CODEX_ROOT = ".agents/codex"

REQUIRED_FILES = (
    "AGENTS.md",
    "CLAUDE.md",
    "app/AGENTS.md",
    "desktop/AGENTS.md",
    ".github/AGENTS.md",
    ".github/pull_request_template.md",
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
    f"{CLAUDE_ROOT}/CLAUDE.md",
    f"{CLAUDE_ROOT}/settings.json",
    f"{CLAUDE_ROOT}/hooks/user-prompt-submit.sh",
    f"{CODEX_ROOT}/config.toml",
    f"{CODEX_ROOT}/hooks.json",
    "scripts/sync_agent_runtime.py",
    "scripts/agent_skill_router.py",
    "scripts/setup-ai.ps1",
    "scripts/setup-ai.sh",
    "docs/ai/README.md",
    "docs/ai/WORKFLOW.md",
    "docs/ai/AI_ENGINEERING_GUARDRAILS.md",
    "docs/ai/ANTIGRAVITY.md",
    "docs/ai/OPENCLAW.md",
    "docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md",
)

FORBIDDEN_DUPLICATE_FILES = ("SPEC.md", "ROADMAP.md", "TASKS.md")
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
CLAUDE_INSTRUCTIONS_PATH = f"{CLAUDE_ROOT}/CLAUDE.md"
CLAUDE_ROUTER_PATH = f"{CLAUDE_ROOT}/hooks/user-prompt-submit.sh"
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

SKILL_NAME_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
SKILL_REFERENCE_RE = re.compile(r"`(levyra-[a-z0-9-]+)`")
DOCUMENTED_AGENT_IDS = {"levyra-ci", "levyra-reviewer", "levyra-worker"}


def read_text(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def parse_front_matter(path: Path) -> dict[str, str]:
    lines = path.read_text(encoding="utf-8").splitlines()
    if not lines or lines[0].strip() != "---":
        raise ValueError("missing opening YAML front matter delimiter")
    try:
        end_index = next(i for i, line in enumerate(lines[1:], start=1) if line.strip() == "---")
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


def missing_skill_references(referenced_skills: set[str], actual_skills: set[str]) -> list[str]:
    return sorted(referenced_skills - actual_skills - DOCUMENTED_AGENT_IDS)


def require_skill_references(
    errors: list[str], relative_path: str, text: str, skills: tuple[str, ...], runtime: str
) -> None:
    for skill in skills:
        if skill not in text:
            errors.append(f"{relative_path}: missing {runtime} automatic route for {skill}")


def require_terms(
    errors: list[str], relative_path: str, text: str, terms: tuple[str, ...], label: str
) -> None:
    for term in terms:
        if term not in text:
            errors.append(f"{relative_path}: missing {label}: {term}")


def reject_terms(
    errors: list[str], relative_path: str, text: str, terms: tuple[str, ...], label: str
) -> None:
    for term in terms:
        if term in text:
            errors.append(f"{relative_path}: contains {label}: {term}")


def tracked_generated_runtime_paths(errors: list[str]) -> list[str]:
    try:
        result = subprocess.run(
            ["git", "ls-files", "--", ".claude", ".codex"],
            cwd=ROOT,
            check=False,
            text=True,
            capture_output=True,
            timeout=30,
        )
    except OSError as exc:
        errors.append(f"unable to inspect tracked runtime surfaces with Git: {exc}")
        return []
    except subprocess.TimeoutExpired:
        errors.append("timed out while inspecting tracked runtime surfaces with Git")
        return []
    if result.returncode != 0:
        detail = result.stderr.strip() or f"exit code {result.returncode}"
        errors.append(f"Git failed while inspecting tracked runtime surfaces: {detail}")
        return []
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def main() -> int:
    errors: list[str] = []

    for relative_path in REQUIRED_FILES:
        if not (ROOT / relative_path).is_file():
            errors.append(f"missing required file: {relative_path}")

    for relative_path in FORBIDDEN_DUPLICATE_FILES:
        if (ROOT / relative_path).exists():
            errors.append(f"obsolete root planning file must be removed: {relative_path}")

    tracked_adapters = tracked_generated_runtime_paths(errors)
    if tracked_adapters:
        errors.append(
            "generated .claude/.codex runtime surfaces must not be tracked; canonical sources belong under .agents: "
            + ", ".join(tracked_adapters)
        )

    root_claude_path = ROOT / "CLAUDE.md"
    if root_claude_path.is_file():
        root_claude = root_claude_path.read_text(encoding="utf-8")
        if "@AGENTS.md" not in root_claude:
            errors.append("CLAUDE.md: missing native @AGENTS.md import")
        if len(root_claude_path.read_bytes()) > 1200:
            errors.append("CLAUDE.md: native startup bridge must stay at or below 1200 bytes")

    duplicate_claude_skills = ROOT / CLAUDE_ROOT / "skills"
    if duplicate_claude_skills.exists():
        errors.append(
            f"{CLAUDE_ROOT}/skills must not be tracked as a second skill tree; "
            ".agents/skills is canonical and scripts/sync_agent_runtime.py projects it to .claude/skills locally"
        )

    antigravity_rule_path = ROOT / ANTIGRAVITY_RULE_PATH
    if antigravity_rule_path.is_file():
        antigravity_rule = antigravity_rule_path.read_text(encoding="utf-8")
        if ANTIGRAVITY_RULE_ROOT_REFERENCE not in antigravity_rule:
            errors.append(
                f"{ANTIGRAVITY_RULE_PATH}: missing canonical root reference {ANTIGRAVITY_RULE_ROOT_REFERENCE!r}"
            )
        require_skill_references(
            errors, ANTIGRAVITY_RULE_PATH, antigravity_rule, AUTOMATIC_ROUTED_SKILLS, "shared workspace"
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
        antigravity_guide = antigravity_guide_path.read_text(encoding="utf-8")
        if ANTIGRAVITY_SKILLS_PATH not in antigravity_guide:
            errors.append("docs/ai/ANTIGRAVITY.md: missing canonical workspace skills path")
        if ANTIGRAVITY_RULE_PATH not in antigravity_guide:
            errors.append("docs/ai/ANTIGRAVITY.md: missing workspace rule reference")

    codex_instructions_path = ROOT / "AGENTS.md"
    if codex_instructions_path.is_file():
        codex_instructions = codex_instructions_path.read_text(encoding="utf-8")
        if len(codex_instructions.splitlines()) > 200:
            errors.append("AGENTS.md: compact always-loaded contract must stay at or below 200 lines")
        require_skill_references(
            errors, "AGENTS.md", codex_instructions, AUTOMATIC_ROUTED_SKILLS, "Codex"
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
                ".agents/claude/",
                ".agents/codex/",
                ".github/pull_request_template.md",
                "levyra-humanizer",
            ),
            "Codex context/code-quality/runtime-source contract",
        )

    chatgpt_path = ROOT / CHATGPT_INSTRUCTIONS_PATH
    if chatgpt_path.is_file():
        chatgpt = chatgpt_path.read_text(encoding="utf-8")
        require_skill_references(
            errors, CHATGPT_INSTRUCTIONS_PATH, chatgpt, AUTOMATIC_ROUTED_SKILLS, "ChatGPT"
        )
        require_terms(
            errors,
            CHATGPT_INSTRUCTIONS_PATH,
            chatgpt,
            (
                "AI_ENGINEERING_GUARDRAILS.md",
                "code-review",
                ".agents/claude/rules/",
                ".github/pull_request_template.md",
                "levyra-humanizer",
            ),
            "ChatGPT shared guardrail/review/canonical-rule route",
        )
        reject_terms(
            errors,
            CHATGPT_INSTRUCTIONS_PATH,
            chatgpt,
            ("under `.claude/skills/`", "under `.claude/rules/`"),
            "legacy generated Claude source reference",
        )

    claude_path = ROOT / CLAUDE_INSTRUCTIONS_PATH
    if claude_path.is_file():
        claude = claude_path.read_text(encoding="utf-8")
        require_skill_references(
            errors, CLAUDE_INSTRUCTIONS_PATH, claude, AUTOMATIC_ROUTED_SKILLS, "Claude"
        )
        require_terms(
            errors,
            CLAUDE_INSTRUCTIONS_PATH,
            claude,
            (
                "Immediate context budget",
                "AI_ENGINEERING_GUARDRAILS.md",
                "levyra-context-efficiency",
                "/code-review",
                "Do not add explanatory source-code comments",
                ".github/pull_request_template.md",
                "levyra-humanizer",
            ),
            "Claude immediate context/review contract",
        )

    claude_router_path = ROOT / CLAUDE_ROUTER_PATH
    if claude_router_path.is_file():
        claude_router = claude_router_path.read_text(encoding="utf-8")
        require_skill_references(
            errors, CLAUDE_ROUTER_PATH, claude_router, AUTOMATIC_ROUTED_SKILLS, "Claude hook"
        )
        require_terms(
            errors,
            CLAUDE_ROUTER_PATH,
            claude_router,
            ("Levyra context budget", "code-review", "levyra-humanizer"),
            "Claude hook context/review reminder",
        )

    for relative_path, terms, label in (
        (
            "scripts/agent_skill_router.py",
            ("levyra-humanizer", "pull request description"),
            "shared PR-description skill route",
        ),
        (
            ".github/pull_request_template.md",
            ("complete Levyra", "levyra-humanizer", "without changing"),
            "mandatory PR-description schema/humanizer contract",
        ),
        (
            "docs/ai/WORKFLOW.md",
            (".github/pull_request_template.md", "levyra-humanizer", "leave unperformed"),
            "shared PR publication prose contract",
        ),
    ):
        if (ROOT / relative_path).is_file():
            require_terms(errors, relative_path, read_text(relative_path), terms, label)

    guardrails_path = ROOT / GUARDRAILS_PATH
    if guardrails_path.is_file():
        guardrails = guardrails_path.read_text(encoding="utf-8")
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

    sync_path = ROOT / "scripts/sync_agent_runtime.py"
    if sync_path.is_file():
        sync_text = sync_path.read_text(encoding="utf-8")
        require_terms(
            errors,
            "scripts/sync_agent_runtime.py",
            sync_text,
            (
                'ROOT / ".agents" / "skills"',
                'Path("skills")',
                'ROOT / ".agents" / "claude"',
                'ROOT / ".agents" / "codex"',
                'ROOT / ".claude"',
                'ROOT / ".codex"',
                "MANIFEST_NAME",
                "MANIFEST_SCHEMA_VERSION",
                "ProjectionError",
                "FILE_ATTRIBUTE_REPARSE_POINT",
                "--check",
            ),
            "runtime projection contract",
        )

    for setup_relative in ("scripts/setup-ai.ps1", "scripts/setup-ai.sh"):
        setup_path = ROOT / setup_relative
        if setup_path.is_file():
            require_terms(
                errors,
                setup_relative,
                setup_path.read_text(encoding="utf-8"),
                ("sync_agent_runtime.py", "--runtime all", ".agents", ".claude", ".codex"),
                "runtime bootstrap contract",
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
                f"{relative_path}: front matter name {name!r} does not match directory {directory_name!r}"
            )
        elif name in actual_skills:
            errors.append(f"{relative_path}: duplicate skill name {name!r}")
        else:
            actual_skills.add(name)
        if not description:
            errors.append(f"{relative_path}: missing front matter description")

    for name in sorted(set(AUTOMATIC_ROUTED_SKILLS) - actual_skills):
        errors.append(f"automatic route points to missing native skill: {name}")

    referenced_skills: set[str] = set()
    for relative_path in REFERENCE_FILES:
        path = ROOT / relative_path
        if path.is_file():
            referenced_skills.update(SKILL_REFERENCE_RE.findall(path.read_text(encoding="utf-8")))

    for name in missing_skill_references(referenced_skills, actual_skills):
        errors.append(f"documented skill does not exist: {name}")

    inventory_path = ROOT / ".agents" / "README.md"
    if inventory_path.is_file():
        inventory_skills = set(SKILL_REFERENCE_RE.findall(inventory_path.read_text(encoding="utf-8")))
        for name in sorted(actual_skills - inventory_skills):
            errors.append(f"skill missing from .agents/README.md inventory: {name}")

    if errors:
        print("Agent configuration validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "Agent configuration validation passed: compact root contract, native Claude bridge, one tracked .agents runtime tree, "
        f"{len(actual_skills)} canonical skills, {len(referenced_skills)} documented skill references, "
        f"{len(AUTOMATIC_ROUTED_SKILLS)} automatic cross-runtime routes, and no tracked generated Claude/Codex runtime surfaces."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
