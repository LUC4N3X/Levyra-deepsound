#!/usr/bin/env python3
"""Post-mutation hook that turns comment-slop findings into agent feedback."""

from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MUTATIONS = {"edit", "write", "apply_patch", "multi_edit", "multiedit"}


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except Exception:
        return 0
    tool = str(payload.get("tool_name") or payload.get("toolName") or "").lower()
    if tool not in MUTATIONS:
        return 0
    checker = ROOT / "scripts" / "check_ai_comment_slop.py"
    result = subprocess.run(
        [sys.executable, str(checker), "--working-tree"],
        cwd=ROOT,
        check=False,
        text=True,
        capture_output=True,
        timeout=30,
    )
    if result.returncode != 0:
        reason = (result.stdout + result.stderr).strip() or "AI comment slop detected"
        print(json.dumps({"decision": "block", "reason": reason[-4000:]}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
