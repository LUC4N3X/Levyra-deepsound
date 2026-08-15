#!/usr/bin/env bash
set -u

project_dir="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
runtime="$project_dir/scripts/codex_jcodemunch.py"
status="jCodeMunch: unavailable; continue with Claude native Read/Grep/Glob/Bash tools. Token savings never override correctness."

python_cmd=""
for candidate in python3 python py; do
  if command -v "$candidate" >/dev/null 2>&1; then
    python_cmd="$candidate"
    break
  fi
done

if [[ -n "$python_cmd" && -f "$runtime" ]]; then
  if "$python_cmd" "$runtime" index --quiet >/dev/null 2>&1; then
    status="jCodeMunch: pinned runtime ready and Levyra index refreshed. Prefer symbol-level discovery first; expand to native reads/search whenever correctness needs broader context. Token savings never override correctness."
  else
    status="jCodeMunch: automatic install/index failed. Continue with Claude native Read/Grep/Glob/Bash tools. Token savings never override correctness."
  fi
elif [[ -z "$python_cmd" ]]; then
  status="jCodeMunch: Python unavailable. Continue with Claude native tools; do not block coding. Token savings never override correctness."
else
  status="jCodeMunch: repository runtime missing. Continue with Claude native tools and report the tooling mismatch once. Token savings never override correctness."
fi

if [[ -n "$python_cmd" ]]; then
  "$python_cmd" - "$status" <<'PY' 2>/dev/null || true
import json
import sys

print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "SessionStart",
        "additionalContext": sys.argv[1],
    }
}))
PY
fi

exit 0
