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
            groups = hooks.get("SessionStart") if isinstance(hooks, dict) else None
            if not isinstance(groups, list) or not groups:
                errors.append(f"{HOOKS_RELATIVE} must define SessionStart hooks")
            else:
                matching_group = next(
                    (
                        group
                        for group in groups
                        if isinstance(group, dict)
                        and group.get("matcher") == "^(startup|resume)$"
                    ),
                    None,
                )
                if matching_group is None:
                    errors.append(f"{HOOKS_RELATIVE} must match startup and resume sessions")
                else:
                    handlers = matching_group.get("hooks")
                    if not isinstance(handlers, list):
                        errors.append("Codex SessionStart hooks must be a list")
                    else:
                        command_handler = next(
                            (
                                handler
                                for handler in handlers
                                if isinstance(handler, dict)
                                and handler.get("type") == "command"
                            ),
                            None,
                        )
                        if command_handler is None:
                            errors.append("Codex SessionStart must contain a command hook")
                        else:
                            command = command_handler.get("command", "")
                            command_windows = command_handler.get("commandWindows", "")
                            for term in (
                                "git rev-parse --show-toplevel",
                                "scripts/ensure-codex-tooling.sh",
                                "|| true",
                            ):
                                if term not in command:
                                    errors.append(f"Codex Unix SessionStart command is missing {term!r}")
                            for term in (
                                "git rev-parse --show-toplevel",
                                "scripts/ensure-codex-tooling.ps1",
                                "exit 0",
                            ):
                                if term not in command_windows:
                                    errors.append(f"Codex Windows SessionStart command is missing {term!r}")
                            if command_handler.get("timeout") != 600:
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
        "Codex hook validation passed: canonical .agents/codex hooks preserve "
        "startup/resume tooling bootstrap and the pinned RTK contract."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
