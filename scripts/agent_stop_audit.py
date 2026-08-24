#!/usr/bin/env python3
"""Single stop-time completion audit for Levyra agent runtimes."""

from __future__ import annotations

import json
import sys
from typing import Any

from scripts import agent_checkpoint as checkpoint
from scripts import agent_harness as harness


def _payload() -> dict[str, Any]:
    try:
        value = json.load(sys.stdin)
    except Exception:
        return {}
    return value if isinstance(value, dict) else {}


def _context(text: str) -> None:
    print(json.dumps({"hookSpecificOutput": {"hookEventName": "Stop", "additionalContext": text}}))


def main() -> int:
    data = _payload()
    state = checkpoint._sync(data, checkpoint._load())
    session = harness._load(data)
    generation = int(state.get("edit_generation", 0))

    if generation <= 0:
        session["task_complete"] = True
        harness._save(data, session)
        checkpoint._clear()
        return 0

    if state.get("status") == "BLOCKED" and int(state.get("failure_generation", -1)) == generation:
        state["next_action"] = "report the exact BLOCKED prerequisite and leave the affected acceptance gate open"
        checkpoint._save(state)
        _context(
            "A required Levyra gate is BLOCKED by an unavailable tool/runtime prerequisite. "
            "Do not claim completion. End the turn only after the remaining possible checks and final diff review, "
            "report the blocked prerequisite exactly, and preserve the durable checkpoint for resume."
        )
        return 0

    failures: list[str] = []
    if int(session.get("diff_review_generation", -1)) < generation:
        failures.append("inspect the actual final diff after the latest material edit")
    if harness._needs_validation(list(session.get("edited_paths", []))) and int(session.get("validation_generation", -1)) < generation:
        failures.append("run focused validation after the latest material edit")
    for output in harness._diff_check_failures():
        failures.append(f"fix diff whitespace/conflict validation: {output}")
    if not checkpoint._comment_guard_passes():
        failures.append("remove newly added AI-narration comments")

    if failures:
        state["status"] = "ACTIVE"
        state["next_action"] = failures[0]
        checkpoint._save(state)
        print(json.dumps({"decision": "block", "reason": "Levyra completion audit is still open:\n- " + "\n- ".join(failures)}))
        return 0

    session["completed_generation"] = generation
    session["task_complete"] = True
    harness._save(data, session)
    state["status"] = "PASS"
    state["validation_generation"] = int(session.get("validation_generation", -1))
    state["diff_review_generation"] = int(session.get("diff_review_generation", -1))
    state["next_action"] = "none; completion evidence is current"
    clear_due = checkpoint._record_completed_boundary(state)
    checkpoint._save(state)

    if clear_due:
        _context(
            "Claude context-hygiene checkpoint reached at a verified completed-task boundary. "
            "If the next owner request is unrelated, prefer `/clear` before starting it. "
            "Do not clear an ACTIVE/BLOCKED task. Project hooks cannot execute slash commands, so never claim `/clear` ran unless it actually did; "
            "SessionStart(clear) will reload Levyra guards and any open durable checkpoint."
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
