#!/usr/bin/env python3
"""Durable, non-repository task checkpoint and retry guard for Levyra agents."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from scripts import agent_harness as harness

CHECKPOINT_ROOT = Path(os.environ.get("LEVYRA_AGENT_CHECKPOINT_DIR", "")) if os.environ.get("LEVYRA_AGENT_CHECKPOINT_DIR") else None
SHELL_TOOLS = {"bash", "shell", "exec_command", "powershell", "command"}
BROAD_INSTALL_RE = re.compile(
    r"(?:apt(?:-get)?\s+(?:dist-)?upgrade|dnf\s+upgrade|yum\s+update|pacman\s+-Syu|"
    r"winget\s+upgrade\s+--all|choco\s+upgrade\s+all|scoop\s+update\s+\*)",
    re.I,
)
BLOCKED_ENV_RE = re.compile(
    r"(?:command not found|is not recognized as an internal or external command|"
    r"no devices?/emulators? found|no connected devices?|device offline|sdk location not found|"
    r"android_home.{0,20}(?:not set|missing)|java_home.{0,20}(?:not set|missing)|"
    r"(?:jadx|java|javac|adb|bundletool|apktool|frida|sdkmanager).{0,40}(?:not found|missing|unavailable)|"
    r"(?:missing|requires?).{0,30}(?:jdk|android sdk|adb|device|emulator))",
    re.I,
)
SENSITIVE_RE = re.compile(
    r"(?i)(authorization\s*:\s*bearer\s+)\S+|"
    r"((?:pass(?:word)?|secret|access[_-]?key|api[_-]?key|auth[_-]?value)\s*[:=]\s*)\S+|"
    r"\b[A-Za-z0-9_-]{24,}\.[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{12,}\b"
)


def _payload() -> dict[str, Any]:
    try:
        value = json.load(sys.stdin)
    except Exception:
        return {}
    return value if isinstance(value, dict) else {}


def _tool_name(data: dict[str, Any]) -> str:
    return str(data.get("tool_name") or data.get("toolName") or "")


def _tool_input(data: dict[str, Any]) -> dict[str, Any]:
    value = data.get("tool_input") or data.get("toolInput") or {}
    return value if isinstance(value, dict) else {}


def _command(data: dict[str, Any]) -> str:
    value = _tool_input(data).get("command") or _tool_input(data).get("cmd") or ""
    if isinstance(value, list):
        return " ".join(str(part) for part in value)
    return str(value)


def _is_shell(data: dict[str, Any]) -> bool:
    return _tool_name(data).lower() in SHELL_TOOLS


def _run(argv: list[str]) -> tuple[int, str]:
    try:
        result = subprocess.run(argv, cwd=ROOT, check=False, text=True, capture_output=True, timeout=10)
    except (OSError, subprocess.TimeoutExpired) as exc:
        return 1, str(exc)
    return result.returncode, (result.stdout + result.stderr).strip()


def _checkpoint_root() -> Path:
    if CHECKPOINT_ROOT is not None:
        return CHECKPOINT_ROOT
    rc, value = _run(["git", "rev-parse", "--git-path", "levyra-agent-state"])
    if rc == 0 and value:
        path = Path(value.splitlines()[-1].strip())
        return path if path.is_absolute() else ROOT / path
    repo_key = hashlib.sha256(str(ROOT.resolve()).encode()).hexdigest()[:16]
    return Path(tempfile.gettempdir()) / "levyra-agent-checkpoint" / repo_key


def _branch_key() -> str:
    rc, value = _run(["git", "branch", "--show-current"])
    raw = value.splitlines()[-1].strip() if rc == 0 and value else "default"
    if not raw:
        rc, value = _run(["git", "rev-parse", "--short", "HEAD"])
        raw = f"detached-{value.splitlines()[-1].strip()}" if rc == 0 and value else "detached"
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", raw)[:120] or "default"


def _path() -> Path:
    return _checkpoint_root() / f"{_branch_key()}.json"


def _hygiene_path() -> Path:
    return _checkpoint_root() / f"{_branch_key()}.context-hygiene.json"


def _blank() -> dict[str, Any]:
    return {
        "status": "ACTIVE",
        "goal": "",
        "next_action": "",
        "edited_paths": [],
        "edit_generation": 0,
        "validation_generation": -1,
        "diff_review_generation": -1,
        "failure_key": "",
        "failure_count": 0,
        "failure_generation": -1,
        "failure_class": "",
        "blocked_reason": "",
        "updated_by_session": "",
    }


def _load_json(path: Path, default: dict[str, Any]) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        value = {}
    result = dict(default)
    if isinstance(value, dict):
        result.update(value)
    return result


def _load() -> dict[str, Any]:
    return _load_json(_path(), _blank())


def _save_json(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")


def _save(state: dict[str, Any]) -> None:
    _save_json(_path(), state)


def _clear() -> None:
    try:
        _path().unlink()
    except OSError:
        pass


def _load_hygiene() -> dict[str, Any]:
    return _load_json(
        _hygiene_path(),
        {
            "completed_since_clear": 0,
            "last_completed_signature": "",
            "clear_recommended": False,
        },
    )


def _save_hygiene(value: dict[str, Any]) -> None:
    _save_json(_hygiene_path(), value)


def _reset_hygiene() -> None:
    _save_hygiene({"completed_since_clear": 0, "last_completed_signature": "", "clear_recommended": False})


def _redact(text: str) -> str:
    def replace(match: re.Match[str]) -> str:
        prefix = match.group(1) or match.group(2) or ""
        return f"{prefix}<redacted>"

    return SENSITIVE_RE.sub(replace, text)[:1200]


def _restore_session_if_empty(data: dict[str, Any], state: dict[str, Any]) -> None:
    session = harness._load(data)
    if int(session.get("edit_generation", 0)) > 0 or session.get("edited_paths"):
        return
    if int(state.get("edit_generation", 0)) <= 0 and not state.get("edited_paths"):
        return
    session["edited_paths"] = list(state.get("edited_paths", []))
    session["edit_generation"] = int(state.get("edit_generation", 0))
    session["validation_generation"] = int(state.get("validation_generation", -1))
    session["diff_review_generation"] = int(state.get("diff_review_generation", -1))
    session["task_complete"] = False
    harness._save(data, session)


def _sync(data: dict[str, Any], state: dict[str, Any]) -> dict[str, Any]:
    session = harness._load(data)
    previous_generation = int(state.get("edit_generation", 0))
    generation = int(session.get("edit_generation", 0))
    state["edited_paths"] = list(session.get("edited_paths", []))
    state["edit_generation"] = generation
    state["validation_generation"] = int(session.get("validation_generation", -1))
    state["diff_review_generation"] = int(session.get("diff_review_generation", -1))
    state["updated_by_session"] = str(data.get("session_id") or data.get("sessionId") or "default")[:120]
    if generation != previous_generation:
        state["failure_key"] = ""
        state["failure_count"] = 0
        state["failure_generation"] = -1
        state["failure_class"] = ""
        state["blocked_reason"] = ""
        state["status"] = "ACTIVE"
    return state


def _context(event: str, text: str) -> None:
    if text:
        print(json.dumps({"hookSpecificOutput": {"hookEventName": event, "additionalContext": text}}))


def _deny(reason: str) -> None:
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": reason}}))


def _summary(state: dict[str, Any]) -> str:
    paths = ", ".join(state.get("edited_paths", [])) or "none"
    return (
        "Levyra durable task checkpoint:\n"
        f"- status: {state.get('status', 'ACTIVE')}\n"
        f"- goal: {state.get('goal') or 'not recorded'}\n"
        f"- edited paths: {paths}\n"
        f"- validation generation: {state.get('validation_generation', -1)}\n"
        f"- diff-review generation: {state.get('diff_review_generation', -1)}\n"
        f"- next action: {state.get('next_action') or 'inspect current repository evidence'}"
    )


def _signature(command: str, generation: int) -> str:
    normalized = re.sub(r"\s+", " ", command.strip())
    return hashlib.sha256(f"{generation}|{normalized}".encode()).hexdigest()


def _response(data: dict[str, Any]) -> Any:
    return data.get("tool_response") or data.get("toolResponse") or data.get("tool_result") or data.get("toolResult") or {}


def _response_text(value: Any) -> str:
    if isinstance(value, dict):
        parts = [value.get(key) for key in ("stderr", "stdout", "error", "message", "content")]
        return "\n".join(str(part) for part in parts if part)
    return str(value or "")


def _response_failed(value: Any) -> bool:
    if isinstance(value, dict):
        if value.get("success") is False or value.get("isError") is True:
            return True
        for key in ("exit_code", "exitCode", "returncode", "code"):
            current = value.get(key)
            if isinstance(current, int) and current != 0:
                return True
    return False


def _record_failure(data: dict[str, Any], text: str) -> None:
    if not _is_shell(data):
        return
    state = _sync(data, _load())
    command = _command(data)
    if not command:
        return
    generation = int(state.get("edit_generation", 0))
    key = _signature(command, generation)
    count = int(state.get("failure_count", 0)) + 1 if state.get("failure_key") == key else 1
    state["failure_key"] = key
    state["failure_count"] = count
    state["failure_generation"] = generation
    if BLOCKED_ENV_RE.search(text):
        state["failure_class"] = "BLOCKED"
        state["blocked_reason"] = "required tool, Android runtime prerequisite, device, SDK, or executable is unavailable"
        state["status"] = "BLOCKED"
        state["next_action"] = "install the single required tool if useful and safely authorized, otherwise report the BLOCKED prerequisite"
    else:
        state["failure_class"] = "FAIL"
        state["blocked_reason"] = ""
        state["status"] = "ACTIVE"
        state["next_action"] = "change the diagnosis, input, or command before another identical retry"
    _save(state)


def session_start(data: dict[str, Any]) -> int:
    if str(data.get("source") or "").lower() == "clear":
        _reset_hygiene()
    state = _load()
    if state.get("status") == "PASS":
        _clear()
        return 0
    _restore_session_if_empty(data, state)
    if state.get("goal") or state.get("edited_paths") or state.get("status") == "BLOCKED":
        _context("SessionStart", _summary(state))
    return 0


def user_prompt(data: dict[str, Any]) -> int:
    session = harness._load(data)
    state = _load()
    if (session.get("task_complete") and state.get("status") != "BLOCKED") or state.get("status") == "PASS":
        _clear()
        state = _blank()
    _restore_session_if_empty(data, state)
    prompt = str(data.get("prompt") or "").strip()
    if prompt:
        state["goal"] = _redact(prompt)
        if not state.get("next_action"):
            state["next_action"] = "inspect the narrowest current evidence and define acceptance gates"
    state = _sync(data, state)
    _save(state)
    if state.get("edited_paths") or state.get("status") == "BLOCKED":
        _context("UserPromptSubmit", _summary(state))
    return 0


def pre_tool(data: dict[str, Any]) -> int:
    if not _is_shell(data):
        return 0
    command = _command(data)
    if BROAD_INSTALL_RE.search(command):
        _deny("Levyra permits installing a required useful tool, not broad package/system upgrades. Install only the specific dependency needed for the active task.")
        return 0
    state = _sync(data, _load())
    generation = int(state.get("edit_generation", 0))
    if command and state.get("failure_key") == _signature(command, generation) and int(state.get("failure_count", 0)) >= 2:
        _deny("This exact command already failed twice without a material edit. Change the hypothesis, input, diagnostic, or installation state before retrying; do not burn another identical attempt.")
    return 0


def post_tool(data: dict[str, Any]) -> int:
    value = _response(data)
    if _is_shell(data) and _response_failed(value):
        _record_failure(data, _response_text(value))
        return 0
    state = _sync(data, _load())
    command = _command(data)
    generation = int(state.get("edit_generation", 0))
    if _is_shell(data) and command:
        if state.get("failure_key") == _signature(command, generation):
            state["failure_key"] = ""
            state["failure_count"] = 0
            state["failure_generation"] = -1
            state["failure_class"] = ""
            state["blocked_reason"] = ""
            state["status"] = "ACTIVE"
        if harness.VALIDATION_RE.search(command):
            state["next_action"] = "inspect the actual final diff after the latest material edit"
        if harness._is_diff_review(command):
            state["next_action"] = "run the completion audit and report exact evidence" if int(state.get("validation_generation", -1)) >= generation else "run focused validation after the latest material edit"
    if _tool_name(data).lower() in {"edit", "write", "apply_patch", "multi_edit", "multiedit"}:
        state["next_action"] = "run focused validation for the latest edit, then inspect the final diff"
    _save(state)
    return 0


def post_failure(data: dict[str, Any]) -> int:
    _record_failure(data, _response_text(_response(data)))
    return 0


def post_compact(data: dict[str, Any]) -> int:
    state = _sync(data, _load())
    _save(state)
    _context("PostCompact", _summary(state))
    return 0


def _comment_guard_passes() -> bool:
    checker = ROOT / "scripts" / "check_ai_comment_slop.py"
    if not checker.is_file():
        return True
    rc, _ = _run([sys.executable, str(checker)])
    return rc == 0


def _record_completed_boundary(state: dict[str, Any]) -> bool:
    hygiene = _load_hygiene()
    signature = hashlib.sha256(
        f"{state.get('goal', '')}|{state.get('edit_generation', 0)}|{','.join(state.get('edited_paths', []))}".encode()
    ).hexdigest()
    if hygiene.get("last_completed_signature") != signature:
        hygiene["completed_since_clear"] = int(hygiene.get("completed_since_clear", 0)) + 1
        hygiene["last_completed_signature"] = signature
    if int(hygiene.get("completed_since_clear", 0)) >= 2:
        hygiene["clear_recommended"] = True
    _save_hygiene(hygiene)
    return bool(hygiene.get("clear_recommended"))


def stop(data: dict[str, Any]) -> int:
    state = _sync(data, _load())
    session = harness._load(data)
    generation = int(state.get("edit_generation", 0))
    if state.get("status") == "BLOCKED" and int(state.get("failure_generation", -1)) == generation:
        session["validation_generation"] = max(int(session.get("validation_generation", -1)), generation)
        harness._save(data, session)
        state["validation_generation"] = generation
        state["next_action"] = "report the exact BLOCKED prerequisite and leave the affected acceptance gate open"
        _save(state)
        _context(
            "Stop",
            "A required validation is BLOCKED by an unavailable tool/runtime prerequisite. Do not claim the task complete. You may end the turn after the final diff review and remaining checks, but report the blocked gate explicitly and preserve this checkpoint for a later resume.",
        )
        return 0
    if generation <= 0:
        _clear()
        return 0
    ready = (
        int(session.get("validation_generation", -1)) >= generation
        and int(session.get("diff_review_generation", -1)) >= generation
        and not harness._diff_check_failures()
        and _comment_guard_passes()
    )
    if ready:
        state["status"] = "PASS"
        state["next_action"] = "none; completion evidence is current"
        due = _record_completed_boundary(state)
        _save(state)
        if due:
            _context(
                "Stop",
                "Claude context-hygiene checkpoint reached: the current task is at a safe completed boundary. If the next owner request is unrelated, prefer `/clear` before starting it, then let SessionStart reload Levyra guards. Do not clear an open task, an unresolved BLOCKED task, or unpublished evidence that has not been durably checkpointed. Command hooks cannot execute slash commands, so never pretend `/clear` ran when it did not.",
            )
    else:
        state["status"] = "ACTIVE"
        if int(session.get("validation_generation", -1)) < generation:
            state["next_action"] = "run focused validation after the latest material edit"
        elif int(session.get("diff_review_generation", -1)) < generation:
            state["next_action"] = "inspect the actual final diff after the latest material edit"
        _save(state)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("event", choices=("session-start", "user-prompt", "pre-tool", "post-tool", "post-failure", "post-compact", "stop"))
    args = parser.parse_args()
    data = _payload()
    return {
        "session-start": session_start,
        "user-prompt": user_prompt,
        "pre-tool": pre_tool,
        "post-tool": post_tool,
        "post-failure": post_failure,
        "post-compact": post_compact,
        "stop": stop,
    }[args.event](data)


if __name__ == "__main__":
    raise SystemExit(main())
