#!/usr/bin/env python3
"""Validate Levyra's canonical Codex lifecycle hook contract."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOOKS_RELATIVE = ".agents/codex/hooks.json"
HOOKS_PATH = ROOT / HOOKS_RELATIVE
PIN = "b34be37caf3796b69a50952a28e60e32b5daad43"
SUPPORTED_EVENTS = {
    "SessionStart",
    "UserPromptSubmit",
    "PreToolUse",
    "PostToolUse",
    "PostCompact",
    "Stop",
}


def require_terms(errors: list[str], relative_path: str, terms: tuple[str, ...]) -> None:
    path = ROOT / relative_path
    if not path.is_file():
        errors.append(f"missing Codex bootstrap script: {relative_path}")
        return
    text = path.read_text(encoding="utf-8")
    for term in terms:
        if term not in text:
            errors.append(f"{relative_path}: missing bootstrap contract {term!r}")


def main() -> int:
    errors: list[str] = []

    if not HOOKS_PATH.is_file():
        errors.append(f"missing canonical Codex project hook file: {HOOKS_RELATIVE}")
    else:
        try:
            document = json.loads(HOOKS_PATH.read_text(encoding="utf-8"))
        except (OSError, UnicodeError, json.JSONDecodeError) as exc:
            errors.append(f"{HOOKS_RELATIVE} is invalid: {exc}")
        else:
            hooks = document.get("hooks")
            if isinstance(hooks, dict):
                unsupported = sorted(set(hooks) - SUPPORTED_EVENTS)
                if unsupported:
                    errors.append(
                        f"{HOOKS_RELATIVE}: unsupported Codex hook events: {', '.join(unsupported)}"
                    )
                for event, event_groups in hooks.items():
                    for group in event_groups if isinstance(event_groups, list) else []:
                        for handler in group.get("hooks", []) if isinstance(group, dict) else []:
                            command_windows = handler.get("commandWindows", "") if isinstance(handler, dict) else ""
                            if "scripts/" in command_windows and "git rev-parse --show-toplevel" not in command_windows:
                                errors.append(
                                    f"{HOOKS_RELATIVE}: {event} commandWindows must resolve scripts from the Git root"
                                )
            groups = hooks.get("SessionStart") if isinstance(hooks, dict) else None
            matching_group = next(
                (
                    group
                    for group in groups or []
                    if isinstance(group, dict) and group.get("matcher") == "^(startup|resume)$"
                ),
                None,
            )
            if matching_group is None:
                errors.append(f"{HOOKS_RELATIVE} must match startup and resume sessions")
            else:
                handlers = [
                    handler
                    for handler in matching_group.get("hooks") or []
                    if isinstance(handler, dict) and handler.get("type") == "command"
                ]
                serialized = json.dumps(handlers)
                for term in (
                    "sync_agent_runtime.py",
                    "--runtime codex",
                    "ensure-codex-tooling.sh",
                    "ensure-codex-tooling.ps1",
                    "git rev-parse --show-toplevel",
                ):
                    if term not in serialized:
                        errors.append(f"Codex SessionStart hooks are missing {term!r}")

                sync_handler = next(
                    (handler for handler in handlers if "sync_agent_runtime.py" in str(handler.get("command", ""))),
                    None,
                )
                if sync_handler is None:
                    errors.append("Codex SessionStart must refresh the generated .codex projection")
                elif sync_handler.get("timeout") != 30:
                    errors.append("Codex runtime refresh hook timeout must be 30 seconds")

                tooling_handler = next(
                    (handler for handler in handlers if "ensure-codex-tooling.sh" in str(handler.get("command", ""))),
                    None,
                )
                if tooling_handler is None:
                    errors.append("Codex SessionStart must preserve automatic tooling bootstrap")
                elif tooling_handler.get("timeout") != 600:
                    errors.append("Codex tooling bootstrap hook timeout must be 600 seconds")

    require_terms(
        errors,
        "scripts/ensure-codex-tooling.sh",
        ("scripts/ensure-rtk.sh", "codex_jcodemunch.py", "mattpocock/skills"),
    )
    require_terms(
        errors,
        "scripts/ensure-codex-tooling.ps1",
        ("ensure-rtk.ps1", "codex_jcodemunch.py", "mattpocock/skills"),
    )

    for relative_path in ("scripts/ensure-rtk.sh", "scripts/ensure-rtk.ps1"):
        path = ROOT / relative_path
        if not path.is_file():
            errors.append(f"missing RTK ensure script: {relative_path}")
            continue
        text = path.read_text(encoding="utf-8")
        for term in (PIN, "rtk --version", "rtk gain"):
            if term not in text:
                errors.append(f"{relative_path}: missing RTK contract {term!r}")

    if errors:
        print("Codex hook validation failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "Codex hook validation passed: canonical .agents/codex hooks refresh the ignored "
        "native projection, preserve startup/resume tooling bootstrap, and retain the pinned RTK contract."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
