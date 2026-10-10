#!/usr/bin/env bash
set -uo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if command -v python3 >/dev/null 2>&1; then
  exec python3 "$root/scripts/comment_guard_hook.py"
elif command -v python >/dev/null 2>&1; then
  exec python "$root/scripts/comment_guard_hook.py"
elif command -v py >/dev/null 2>&1; then
  exec py -3 "$root/scripts/comment_guard_hook.py"
fi
exit 0
