#!/usr/bin/env python3
"""Validate Levyra's canonical Claude Code bootstrap and context-loading contract."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLAUDE_ROOT = ".agents/claude"
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

    require_terms(
        "CLAUDE.md",
        ("@.agents/claude/CLAUDE.md", "sync_agent_runtime.py", "--runtime claude"),
        errors,
    )

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

    settings_relative = f"{CLAUDE_ROOT}/settings.json"
    settings = read_json(settings_relative, errors)
    permissions = settings.get("permissions") if isinstance(settings, dict) else None
    allowed = permissions.get("allow") if isinstance(permissions, dict) else None
    if not isinstance(allowed, list) or "mcp__jcodemunch" not in allowed:
        errors.append(f"{settings_relative} must allow the project jcodemunch MCP server")
    if settings.get("skillListingBudgetFraction") != 0.01:
        errors.append(f"{settings_relative} skillListingBudgetFraction must stay at 0.01")
    if settings.get("maxSkillDescriptionChars") != 768:
        errors.append(f"{settings_relative} maxSkillDescriptionChars must stay at 768")
    if settings.get("includeGitInstructions") is not False:
        errors.append(f"{settings_relative} includeGitInstructions must remain false")

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
        errors.append(f"{settings_relative} must run SessionStart on startup and resume")
    else:
        handlers = matching_group.get("hooks") or []
        commands = [
            handler for handler in handlers
            if isinstance(handler, dict) and handler.get("type") == "command"
        ]
        command_text = "\n".join(str(handler.get("command", "")) for handler in commands)
        for required_hook in ("session-start.sh", "jcodemunch-start.sh"):
            if required_hook not in command_text:
                errors.append(f"Claude SessionStart is missing automatic hook {required_hook!r}")
        jcodemunch_handler = next(
            (
                handler for handler in commands
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
        errors.append(f"{settings_relative} must run user-prompt-submit.sh on UserPromptSubmit")

    claude_relative = f"{CLAUDE_ROOT}/CLAUDE.md"
    claude_path = ROOT / claude_relative
    if claude_path.is_file() and len(claude_path.read_bytes()) > 7000:
        errors.append(f"{claude_relative} must stay at or below 7000 bytes")
    require_terms(
        claude_relative,
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
        claude_relative,
        ("@../AGENTS.md", "@../docs/ai/EVIDENCE_GATED_COMPLETION.md", "@../docs/ai/ALWAYS_ON_AGENT_GUARDS.md"),
        errors,
    )

    require_terms(
        f"{CLAUDE_ROOT}/agents/levyra-android-developer.md",
        (
            "tools: Read, Grep, Glob, Edit, Write, Bash, Skill",
            "model: inherit",
            "effort: high",
            "Project instructions are already loaded automatically",
        ),
        errors,
    )
    require_terms(
        f"{CLAUDE_ROOT}/hooks/user-prompt-submit.sh",
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

    for skill in CLAUDE_BRIDGES:
        require_terms(
            f"{CLAUDE_ROOT}/skills/{skill}/SKILL.md",
            (f".agents/skills/{skill}/SKILL.md",),
            errors,
        )

    require_terms(
        f"{CLAUDE_ROOT}/hooks/jcodemunch-start.sh",
        ("codex_jcodemunch.py", "index --quiet", "Read/Grep/Glob/Bash", "Token savings never override correctness", "exit 0"),
        errors,
    )
    require_terms(
        f"{CLAUDE_ROOT}/rules/context-efficiency.md",
        ("jCodeMunch", "Claude native Read/Grep/Glob/Bash", ".rtk/filters.toml", "Rerun the exact command raw", "Token savings never override correctness"),
        errors,
    )
    require_terms(
        "scripts/sync_agent_runtime.py",
        (".agents", ".claude", "settings.json", "hooks", "skills"),
        errors,
    )

    for relative_path in (
        ".mcp.json",
        settings_relative,
        f"{CLAUDE_ROOT}/hooks/jcodemunch-start.sh",
        f"{CLAUDE_ROOT}/hooks/user-prompt-submit.sh",
        "scripts/claude-jcodemunch-mcp.sh",
    ):
        path = ROOT / relative_path
        if path.is_file() and "dangerously-skip-permissions" in path.read_text(encoding="utf-8"):
            errors.append(f"{relative_path}: must not bypass Claude permission safety")

    if errors:
        print("Claude bootstrap validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "Claude bootstrap validation passed: root bridge, canonical .agents/claude config, "
        "runtime projection, skill routing, evidence gates, jCodeMunch, and permission safety verified."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
