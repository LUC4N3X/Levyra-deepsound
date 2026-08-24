#!/usr/bin/env python3
"""Validate Levyra's non-optional cross-runtime agent harness contract."""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

REQUIRED_FILES = (
    "docs/ai/ALWAYS_ON_AGENT_GUARDS.md",
    "scripts/agent_harness.py",
    "scripts/run-agent-harness.sh",
    "scripts/run-agent-harness.ps1",
    "scripts/check_ai_comment_slop.py",
    "scripts/comment_guard_hook.py",
    "scripts/run-comment-guard.sh",
    "scripts/run-comment-guard.ps1",
    ".claude/rules/always-on-agent-guards.md",
)


def require_terms(path: str, terms: tuple[str, ...], errors: list[str]) -> None:
    target = ROOT / path
    if not target.is_file():
        errors.append(f"missing always-on harness file: {path}")
        return
    text = target.read_text(encoding="utf-8")
    for term in terms:
        if term not in text:
            errors.append(f"{path}: missing always-on harness contract {term!r}")


def reject_terms(path: str, terms: tuple[str, ...], errors: list[str]) -> None:
    target = ROOT / path
    if not target.is_file():
        return
    text = target.read_text(encoding="utf-8").lower()
    for term in terms:
        if term.lower() in text:
            errors.append(f"{path}: forbidden agent-harness setting {term!r}")


def read_json(path: str, errors: list[str]) -> dict:
    target = ROOT / path
    try:
        value = json.loads(target.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        errors.append(f"{path}: invalid JSON: {exc}")
        return {}
    if not isinstance(value, dict):
        errors.append(f"{path}: expected JSON object")
        return {}
    return value


def hook_names(settings: dict) -> set[str]:
    hooks = settings.get("hooks")
    return set(hooks) if isinstance(hooks, dict) else set()


def commands(settings: dict) -> str:
    hooks = settings.get("hooks")
    if not isinstance(hooks, dict):
        return ""
    result: list[str] = []
    for groups in hooks.values():
        if not isinstance(groups, list):
            continue
        for group in groups:
            if not isinstance(group, dict):
                continue
            for hook in group.get("hooks") or []:
                if isinstance(hook, dict):
                    result.append(str(hook.get("command", "")))
                    result.append(str(hook.get("commandWindows", "")))
    return "\n".join(result)


def main() -> int:
    errors: list[str] = []
    for path in REQUIRED_FILES:
        if not (ROOT / path).is_file():
            errors.append(f"missing always-on harness file: {path}")

    require_terms(
        "docs/ai/ALWAYS_ON_AGENT_GUARDS.md",
        (
            "These guards are not skills",
            "Current file before mutation",
            "Acceptance gates are always active",
            "Compaction and resume must re-anchor state",
            "AI-comment slop is rejected",
            "Structural navigation is deterministic",
        ),
        errors,
    )
    require_terms(
        "docs/ai/AI_ENGINEERING_GUARDRAILS.md",
        ("Always-on execution harness", "ALWAYS_ON_AGENT_GUARDS.md", "not a skill"),
        errors,
    )
    require_terms(
        "docs/ai/EVIDENCE_GATED_COMPLETION.md",
        ("Mandatory applicability", "always active", "final material edit"),
        errors,
    )
    require_terms(
        "docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md",
        ("AI_ENGINEERING_GUARDRAILS.md", "Apply the shared AI guardrails automatically"),
        errors,
    )
    require_terms(
        ".agents/rules/levyra-workspace.md",
        ("ALWAYS_ON_AGENT_GUARDS.md", "Non-optional harness contract", "Always On"),
        errors,
    )
    require_terms(
        ".claude/rules/always-on-agent-guards.md",
        ("ALWAYS_ON_AGENT_GUARDS.md", "not a skill", "lifecycle hooks"),
        errors,
    )

    required_hooks = {"SessionStart", "UserPromptSubmit", "PreToolUse", "PostToolUse", "PostCompact", "Stop"}
    for path in (".claude/settings.json", ".codex/hooks.json"):
        settings = read_json(path, errors)
        missing = sorted(required_hooks - hook_names(settings))
        if missing:
            errors.append(f"{path}: missing always-on hook(s): {', '.join(missing)}")
        command_text = commands(settings)
        for event in ("session-start", "user-prompt", "pre-tool", "post-tool", "post-compact", "stop"):
            if event not in command_text:
                errors.append(f"{path}: harness event {event!r} is not wired")
        if "run-agent-harness" not in command_text:
            errors.append(f"{path}: checked-in agent harness wrapper is not wired")
        if "run-comment-guard" not in command_text:
            errors.append(f"{path}: immediate post-mutation comment guard is not wired")

    require_terms(
        "scripts/agent_harness.py",
        (
            "permissionDecision",
            "read_hashes",
            "diff_review_generation",
            "validation_generation",
            "completed_generation",
            "reanchor_pending",
            "task_complete",
            "check_ai_comment_slop.py",
        ),
        errors,
    )

    for path in (".codex/config.toml", ".codex/hooks.json", ".claude/settings.json"):
        reject_terms(
            path,
            ("danger-full-access", "dangerously-skip-permissions", "approval_policy = \"never\"", "oh-my-openagent"),
            errors,
        )

    checker = ROOT / "scripts" / "check_ai_comment_slop.py"
    if checker.is_file():
        result = subprocess.run(
            [sys.executable, str(checker)],
            cwd=ROOT,
            check=False,
            text=True,
            capture_output=True,
            timeout=60,
        )
        if result.returncode != 0:
            errors.append((result.stdout + result.stderr).strip() or "AI comment slop checker failed")

    if errors:
        print("Always-on agent harness validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print("Always-on agent harness validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
