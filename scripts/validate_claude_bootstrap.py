#!/usr/bin/env python3
"""Validate Levyra's automatic Claude Code bootstrap and context-loading contract."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

CLAUDE_BRIDGES = (
    "levyra-project-manager",
    "levyra-desktop",
    "levyra-engineering",
    "levyra-openclaw-orchestrator",
)


def read_json(relative_path: str, errors: list[str]) -> dict:
    path = ROOT / relative_path
    if not path.is_file():
        errors.append(f"missing required Claude bootstrap file: {relative_path}")
        return {}
    try:
        document = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        errors.append(f"{relative_path} is invalid JSON: {exc}")
        return {}
    if not isinstance(document, dict):
        errors.append(f"{relative_path} must contain a JSON object")
        return {}
    return document


def require_terms(relative_path: str, terms: tuple[str, ...], errors: list[str]) -> None:
    path = ROOT / relative_path
    if not path.is_file():
        errors.append(f"missing required Claude bootstrap file: {relative_path}")
        return
    text = path.read_text(encoding="utf-8")
    for term in terms:
        if term not in text:
            errors.append(f"{relative_path}: missing Claude bootstrap contract {term!r}")


def reject_terms(relative_path: str, terms: tuple[str, ...], errors: list[str]) -> None:
    path = ROOT / relative_path
    if not path.is_file():
        return
    text = path.read_text(encoding="utf-8")
    for term in terms:
        if term in text:
            errors.append(f"{relative_path}: compact Claude bootstrap must not preload {term!r}")


def main() -> int:
    errors: list[str] = []

    mcp = read_json(".mcp.json", errors)
    servers = mcp.get("mcpServers") if isinstance(mcp, dict) else None
    server = servers.get("jcodemunch") if isinstance(servers, dict) else None
    if not isinstance(server, dict):
        errors.append(".mcp.json must define project-scoped jcodemunch")
    else:
        if server.get("command") != "bash":
            errors.append(".mcp.json jcodemunch must launch through bash")
        args = server.get("args")
        if not isinstance(args, list) or not any(
            "scripts/claude-jcodemunch-mcp.sh" in str(item) for item in args
        ):
            errors.append(".mcp.json jcodemunch must use the checked-in Claude launcher")

    settings = read_json(".claude/settings.json", errors)
    permissions = settings.get("permissions") if isinstance(settings, dict) else None
    allowed = permissions.get("allow") if isinstance(permissions, dict) else None
    if not isinstance(allowed, list) or "mcp__jcodemunch" not in allowed:
        errors.append(".claude/settings.json must allow the project jcodemunch MCP server")

    skill_budget = settings.get("skillListingBudgetFraction") if isinstance(settings, dict) else None
    if not isinstance(skill_budget, (int, float)) or skill_budget != 0.01:
        errors.append(
            ".claude/settings.json skillListingBudgetFraction must stay at the compact 0.01 budget"
        )
    description_budget = settings.get("maxSkillDescriptionChars") if isinstance(settings, dict) else None
    if description_budget != 768:
        errors.append(
            ".claude/settings.json maxSkillDescriptionChars must stay at 768"
        )
    if settings.get("includeGitInstructions") is not False:
        errors.append(
            ".claude/settings.json includeGitInstructions must be false to avoid duplicate Git context"
        )

    hooks = settings.get("hooks") if isinstance(settings, dict) else None
    groups = hooks.get("SessionStart") if isinstance(hooks, dict) else None
    matching_group = None
    if isinstance(groups, list):
        matching_group = next(
            (
                group
                for group in groups
                if isinstance(group, dict) and group.get("matcher") == "startup|resume"
            ),
            None,
        )
    if matching_group is None:
        errors.append(".claude/settings.json must run SessionStart on startup and resume")
    else:
        handlers = matching_group.get("hooks")
        commands = [
            handler
            for handler in handlers or []
            if isinstance(handler, dict) and handler.get("type") == "command"
        ]
        command_text = "\n".join(str(handler.get("command", "")) for handler in commands)
        for required_hook in ("session-start.sh", "jcodemunch-start.sh"):
            if required_hook not in command_text:
                errors.append(
                    f"Claude SessionStart is missing automatic hook {required_hook!r}"
                )
        jcodemunch_handler = next(
            (
                handler
                for handler in commands
                if "jcodemunch-start.sh" in str(handler.get("command", ""))
            ),
            None,
        )
        if jcodemunch_handler is not None and jcodemunch_handler.get("timeout") != 600:
            errors.append("Claude jCodeMunch SessionStart timeout must be 600 seconds")

    prompt_groups = hooks.get("UserPromptSubmit") if isinstance(hooks, dict) else None
    prompt_commands: list[str] = []
    if isinstance(prompt_groups, list):
        for group in prompt_groups:
            if not isinstance(group, dict):
                continue
            for handler in group.get("hooks") or []:
                if isinstance(handler, dict) and handler.get("type") == "command":
                    prompt_commands.append(str(handler.get("command", "")))
    if not any("user-prompt-submit.sh" in command for command in prompt_commands):
        errors.append(
            ".claude/settings.json must run user-prompt-submit.sh on UserPromptSubmit"
        )

    claude_path = ROOT / ".claude/CLAUDE.md"
    if claude_path.is_file() and len(claude_path.read_bytes()) > 7000:
        errors.append(".claude/CLAUDE.md must stay at or below 7000 bytes")
    require_terms(
        ".claude/CLAUDE.md",
        (
            "AGENTS.md",
            "EVIDENCE_GATED_COMPLETION.md",
            "Deterministic skill loading",
            "Mandatory skill load",
            "AI_ENGINEERING_GUARDRAILS.md",
            "Subagent token discipline",
            "/code-review",
        ),
        errors,
    )
    reject_terms(
        ".claude/CLAUDE.md",
        (
            "@../AGENTS.md",
            "@../docs/ai/EVIDENCE_GATED_COMPLETION.md",
            "@../docs/ai/ALWAYS_ON_AGENT_GUARDS.md",
        ),
        errors,
    )
    require_terms(
        ".claude/agents/levyra-android-developer.md",
        (
            "tools: Read, Grep, Glob, Edit, Write, Bash, Skill",
            "model: inherit",
            "effort: high",
            "Project instructions are already loaded automatically",
        ),
        errors,
    )
    reject_terms(
        ".claude/agents/levyra-android-developer.md",
        ("Read `.claude/CLAUDE.md`",),
        errors,
    )
    require_terms(
        ".claude/hooks/user-prompt-submit.sh",
        (
            "command -v python3",
            "command -v python",
            "command -v py",
            "Mandatory skill load",
            "Root AGENTS.md remains canonical",
            "EVIDENCE_GATED_COMPLETION.md",
            "levyra-project-manager",
            "levyra-desktop",
            "levyra-engineering",
            "levyra-openclaw-orchestrator",
        ),
        errors,
    )
    require_terms(
        "docs/ai/EVIDENCE_GATED_COMPLETION.md",
        (
            "Acceptance gates",
            "PASS",
            "FAIL",
            "BLOCKED",
            "UNRUN",
            "Do not create `GATES.md`",
        ),
        errors,
    )

    for skill in CLAUDE_BRIDGES:
        require_terms(
            f".claude/skills/{skill}/SKILL.md",
            (f".agents/skills/{skill}/SKILL.md",),
            errors,
        )

    require_terms(
        "scripts/claude-jcodemunch-mcp.sh",
        ("codex_jcodemunch.py", "serve", "python3", "python", "py"),
        errors,
    )
    require_terms(
        ".claude/hooks/jcodemunch-start.sh",
        (
            "codex_jcodemunch.py",
            "index --quiet",
            "Read/Grep/Glob/Bash",
            "Token savings never override correctness",
            "exit 0",
        ),
        errors,
    )
    require_terms(
        ".claude/rules/context-efficiency.md",
        (
            "jCodeMunch",
            "Claude native Read/Grep/Glob/Bash",
            ".rtk/filters.toml",
            "Rerun the exact command raw",
            "Token savings never override correctness",
        ),
        errors,
    )

    for relative_path in (
        ".mcp.json",
        ".claude/settings.json",
        ".claude/hooks/jcodemunch-start.sh",
        ".claude/hooks/user-prompt-submit.sh",
        "scripts/claude-jcodemunch-mcp.sh",
    ):
        path = ROOT / relative_path
        if path.is_file() and "dangerously-skip-permissions" in path.read_text(
            encoding="utf-8"
        ):
            errors.append(f"{relative_path}: must not bypass Claude permission safety")

    if errors:
        print("Claude bootstrap validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "Claude bootstrap validation passed: compact project context, automatic "
        "skill routing, focused high-effort subagent tools, evidence-gated "
        "completion, jCodeMunch startup/resume indexing, RTK/native-tool fallback, "
        "and permission safety verified."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
