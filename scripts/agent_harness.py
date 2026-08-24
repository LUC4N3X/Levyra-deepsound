#!/usr/bin/env python3
"""Always-on runtime guard for Levyra coding agents."""

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
STATE_ROOT = Path(
    os.environ.get(
        "LEVYRA_AGENT_HARNESS_STATE_DIR",
        str(Path(tempfile.gettempdir()) / "levyra-agent-harness"),
    )
)
PROTECTED_NAMES = {"local.properties", ".env"}
PROTECTED_SUFFIXES = {".jks", ".keystore", ".p12", ".pfx", ".pem", ".key"}
DOC_SUFFIXES = {".md", ".txt", ".rst"}
AGENT_CONFIG_PREFIXES = (
    ".agents/",
    ".claude/",
    ".codex/",
    "docs/ai/",
    "scripts/agent_harness.py",
    "scripts/validate_agent_harness.py",
    "scripts/check_ai_comment_slop.py",
    "scripts/comment_guard_hook.py",
)
VALIDATION_RE = re.compile(
    r"(?:gradlew(?:\.bat)?|pytest|unittest|\btest\b|\blint\b|assemble|compile|"
    r"validate_[\w.-]+\.py|check_[\w.-]+\.py|npm\s+(?:run\s+)?test|cargo\s+test)",
    re.I,
)
DIFF_REVIEW_RE = re.compile(r"(?:\bgit\s+diff\b|\bgh\s+pr\s+diff\b)", re.I)


def _payload() -> dict[str, Any]:
    try:
        data = json.load(sys.stdin)
    except Exception:
        return {}
    return data if isinstance(data, dict) else {}


def _session(data: dict[str, Any]) -> str:
    raw = str(data.get("session_id") or data.get("sessionId") or "default")
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", raw)[:120] or "default"


def _state_path(data: dict[str, Any]) -> Path:
    repo = hashlib.sha256(str(ROOT).encode()).hexdigest()[:16]
    return STATE_ROOT / repo / f"{_session(data)}.json"


def _load(data: dict[str, Any]) -> dict[str, Any]:
    path = _state_path(data)
    try:
        state = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        state = {}
    state.setdefault("read_hashes", {})
    state.setdefault("edited_paths", [])
    state.setdefault("edit_generation", 0)
    state.setdefault("diff_review_generation", -1)
    state.setdefault("validation_generation", -1)
    state.setdefault("completed_generation", -1)
    state.setdefault("latest_prompt", "")
    state.setdefault("reanchor_pending", False)
    state.setdefault("task_complete", False)
    return state


def _save(data: dict[str, Any], state: dict[str, Any]) -> None:
    path = _state_path(data)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")


def _tool_name(data: dict[str, Any]) -> str:
    return str(data.get("tool_name") or data.get("toolName") or "")


def _tool_input(data: dict[str, Any]) -> dict[str, Any]:
    value = data.get("tool_input") or data.get("toolInput") or {}
    return value if isinstance(value, dict) else {}


def _resolve(raw: str) -> Path | None:
    if not raw:
        return None
    path = Path(raw)
    if not path.is_absolute():
        path = ROOT / path
    try:
        resolved = path.resolve()
        resolved.relative_to(ROOT.resolve())
    except (OSError, ValueError):
        return None
    return resolved


def _relative(path: Path) -> str:
    return path.resolve().relative_to(ROOT.resolve()).as_posix()


def _protected(path: Path) -> bool:
    return path.name.lower() in PROTECTED_NAMES or path.suffix.lower() in PROTECTED_SUFFIXES


def _hash(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _targets(data: dict[str, Any]) -> list[Path]:
    tool = _tool_name(data)
    item = _tool_input(data)
    result: list[Path] = []
    for key in ("file_path", "path", "filePath"):
        value = item.get(key)
        if isinstance(value, str):
            path = _resolve(value)
            if path:
                result.append(path)
    if tool.lower() == "apply_patch":
        command = str(item.get("command") or item.get("patch") or "")
        for match in re.finditer(r"^\*\*\* (?:Update|Delete) File: (.+)$", command, re.M):
            path = _resolve(match.group(1).strip())
            if path:
                result.append(path)
    unique: list[Path] = []
    seen: set[str] = set()
    for path in result:
        key = str(path)
        if key not in seen:
            seen.add(key)
            unique.append(path)
    return unique


def _scoped_agents(path: Path) -> str:
    parts: list[str] = []
    current = path.parent if path.suffix else path
    chain: list[Path] = []
    root = ROOT.resolve()
    while current.resolve() != root:
        chain.append(current)
        if current.parent == current:
            break
        current = current.parent
    for directory in reversed(chain):
        candidate = directory / "AGENTS.md"
        if candidate.is_file():
            text = candidate.read_text(encoding="utf-8", errors="replace")
            parts.append(f"Applicable scoped instructions: {_relative(candidate)}\n{text}")
    return "\n\n".join(parts)


def _patch_anchor(data: dict[str, Any]) -> str:
    item = _tool_input(data)
    old = str(item.get("old_string") or "")
    for line in old.splitlines():
        if len(line.strip()) >= 4:
            return line.strip()
    command = str(item.get("command") or item.get("patch") or "")
    for line in command.splitlines():
        if line.startswith(("***", "@@", "+++", "---")):
            continue
        if line.startswith((" ", "-")):
            anchor = line[1:].strip()
            if len(anchor) >= 6:
                return anchor
    return ""


def _current_context(path: Path, data: dict[str, Any]) -> str:
    if not path.is_file() or _protected(path):
        return ""
    text = path.read_text(encoding="utf-8", errors="replace")
    if len(text) <= 14000:
        return f"Current {_relative(path)}:\n{text}"
    lines = text.splitlines()
    anchor = _patch_anchor(data)
    index = 0
    if anchor:
        index = next((i for i, line in enumerate(lines) if anchor in line), 0)
    start = max(0, index - 35)
    end = min(len(lines), index + 36)
    excerpt = "\n".join(f"{i + 1}: {lines[i]}" for i in range(start, end))
    return f"Current bounded region from {_relative(path)}:\n{excerpt}"


def _reanchor(state: dict[str, Any]) -> str:
    edited = ", ".join(state.get("edited_paths", [])) or "none"
    return (
        "Levyra always-on re-anchor after compaction/resume:\n"
        f"- latest owner request: {state.get('latest_prompt') or 'not recorded'}\n"
        f"- edited paths: {edited}\n"
        f"- edit generation: {state.get('edit_generation', 0)}\n"
        f"- validation generation: {state.get('validation_generation', -1)}\n"
        f"- final-diff review generation: {state.get('diff_review_generation', -1)}\n"
        "- re-read current repository evidence before relying on compacted memory; "
        "ALWAYS_ON_AGENT_GUARDS.md remains mandatory."
    )


def _context_output(event: str, text: str) -> None:
    if not text:
        return
    print(json.dumps({"hookSpecificOutput": {"hookEventName": event, "additionalContext": text}}))


def _deny(event: str, reason: str) -> None:
    print(json.dumps({"hookSpecificOutput": {"hookEventName": event, "permissionDecision": "deny", "permissionDecisionReason": reason}}))


def _reset_task(state: dict[str, Any]) -> None:
    state["read_hashes"] = {}
    state["edited_paths"] = []
    state["edit_generation"] = 0
    state["diff_review_generation"] = -1
    state["validation_generation"] = -1
    state["completed_generation"] = -1
    state["reanchor_pending"] = False
    state["task_complete"] = False


def user_prompt(data: dict[str, Any]) -> int:
    state = _load(data)
    if state.get("task_complete"):
        _reset_task(state)
    prompt = str(data.get("prompt") or "").strip()
    if prompt:
        state["latest_prompt"] = prompt
    state["task_complete"] = False
    _save(data, state)
    _context_output(
        "UserPromptSubmit",
        "Levyra always-on guards are active for this entire task and are not skill-routed: scoped AGENTS context, current-file-before-mutation, evidence gates, final-diff review, anti-AI-comment checks, and compaction re-anchoring are mandatory. For symbol/call-flow/reference work, use jCodeMunch first when available, then an already-available LSP/AST-aware tool when it answers the question more directly, then bounded native search/read.",
    )
    return 0


def pre_tool(data: dict[str, Any]) -> int:
    state = _load(data)
    tool = _tool_name(data)
    targets = _targets(data)
    for path in targets:
        if _protected(path):
            _deny("PreToolUse", f"Protected Levyra local/secret file may not be injected or mutated: {_relative(path)}")
            return 0
    if tool == "Write":
        for path in targets:
            if path.exists():
                recorded = state["read_hashes"].get(_relative(path))
                if recorded != _hash(path):
                    _deny(
                        "PreToolUse",
                        f"Whole-file replacement requires one fresh full Read of existing {_relative(path)} in the active task. Read it once, then retry Write. The guard tracks the current hash and will not demand redundant reads.",
                    )
                    return 0
    chunks: list[str] = []
    if state.get("reanchor_pending"):
        chunks.append(_reanchor(state))
        state["reanchor_pending"] = False
    for path in targets:
        scoped = _scoped_agents(path)
        if scoped:
            chunks.append(scoped)
        current = _current_context(path, data)
        if current and tool.lower() in {"edit", "write", "apply_patch", "multi_edit", "multiedit"}:
            chunks.append(current)
    _save(data, state)
    _context_output("PreToolUse", "\n\n".join(chunks))
    return 0


def _command(data: dict[str, Any]) -> str:
    item = _tool_input(data)
    value = item.get("command") or item.get("cmd") or ""
    if isinstance(value, list):
        return " ".join(str(part) for part in value)
    return str(value)


def _is_diff_review(command: str) -> bool:
    return bool(DIFF_REVIEW_RE.search(command)) and "--check" not in command.lower()


def post_tool(data: dict[str, Any]) -> int:
    state = _load(data)
    low = _tool_name(data).lower()
    targets = _targets(data)
    if low == "read":
        item = _tool_input(data)
        full = item.get("offset") in (None, 0) and item.get("limit") in (None, 0)
        if full:
            for path in targets:
                if path.is_file() and not _protected(path):
                    state["read_hashes"][_relative(path)] = _hash(path)
    if low in {"edit", "write", "apply_patch", "multi_edit", "multiedit"}:
        state["edit_generation"] += 1
        state["task_complete"] = False
        edited = set(state.get("edited_paths", []))
        for path in targets:
            edited.add(_relative(path))
        state["edited_paths"] = sorted(edited)
    command = _command(data)
    if command:
        if _is_diff_review(command):
            state["diff_review_generation"] = state["edit_generation"]
        if VALIDATION_RE.search(command):
            state["validation_generation"] = state["edit_generation"]
    _save(data, state)
    return 0


def compact(data: dict[str, Any]) -> int:
    state = _load(data)
    state["reanchor_pending"] = True
    _save(data, state)
    _context_output("PostCompact", _reanchor(state))
    return 0


def _needs_validation(paths: list[str]) -> bool:
    if not paths:
        return False
    if len(paths) > 1:
        return True
    for raw in paths:
        if raw.startswith(AGENT_CONFIG_PREFIXES):
            return True
        if Path(raw).suffix.lower() not in DOC_SUFFIXES:
            return True
    return False


def _run(argv: list[str]) -> tuple[int, str]:
    try:
        result = subprocess.run(argv, cwd=ROOT, check=False, text=True, capture_output=True, timeout=60)
    except (OSError, subprocess.TimeoutExpired) as exc:
        return 1, str(exc)
    text = (result.stdout + result.stderr).strip()
    return result.returncode, text[-4000:]


def _branch_base() -> str:
    for candidate in ("origin/main", "main", "HEAD~1"):
        rc, commit = _run(["git", "rev-parse", "--verify", f"{candidate}^{{commit}}"])
        if rc != 0 or not commit.strip():
            continue
        rc, base = _run(["git", "merge-base", "HEAD", commit.strip()])
        if rc == 0 and base.strip():
            return base.strip().splitlines()[-1]
    return ""


def _diff_check_failures() -> list[str]:
    failures: list[str] = []
    commands = [
        ["git", "diff", "--check"],
        ["git", "diff", "--cached", "--check"],
    ]
    base = _branch_base()
    if base:
        commands.append(["git", "diff", "--check", f"{base}...HEAD"])
    for argv in commands:
        rc, output = _run(argv)
        if rc != 0:
            failures.append(output or " ".join(argv))
    return failures


def stop(data: dict[str, Any]) -> int:
    state = _load(data)
    generation = int(state.get("edit_generation", 0))
    if generation <= 0:
        state["task_complete"] = True
        _save(data, state)
        return 0
    failures: list[str] = []
    if int(state.get("diff_review_generation", -1)) < generation:
        failures.append("inspect the actual final diff after the latest edit (`git diff ...`, not only `git diff --check`)")
    if _needs_validation(list(state.get("edited_paths", []))) and int(state.get("validation_generation", -1)) < generation:
        failures.append("run focused validation after the latest edit")
    for output in _diff_check_failures():
        failures.append(f"fix diff whitespace/conflict validation: {output}")
    checker = ROOT / "scripts" / "check_ai_comment_slop.py"
    if checker.is_file():
        rc, output = _run([sys.executable, str(checker)])
        if rc != 0:
            failures.append(f"remove newly added AI-narration comments: {output or 'checker failed'}")
    if failures:
        print(json.dumps({"decision": "block", "reason": "Levyra completion audit is still open:\n- " + "\n- ".join(failures)}))
        return 0
    state["completed_generation"] = generation
    state["task_complete"] = True
    _save(data, state)
    return 0


def session_start(data: dict[str, Any]) -> int:
    state = _load(data)
    if str(data.get("source") or "").lower() == "resume":
        state["reanchor_pending"] = True
        _save(data, state)
        _context_output("SessionStart", _reanchor(state))
    else:
        _context_output("SessionStart", "Levyra ALWAYS_ON_AGENT_GUARDS.md is active for the full session and is not optional skill routing.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("event", choices=("session-start", "user-prompt", "pre-tool", "post-tool", "post-compact", "stop"))
    args = parser.parse_args()
    data = _payload()
    return {
        "session-start": session_start,
        "user-prompt": user_prompt,
        "pre-tool": pre_tool,
        "post-tool": post_tool,
        "post-compact": compact,
        "stop": stop,
    }[args.event](data)


if __name__ == "__main__":
    raise SystemExit(main())
