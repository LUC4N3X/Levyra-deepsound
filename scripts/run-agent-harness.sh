#!/usr/bin/env bash
set -uo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
script="$root/scripts/agent_harness.py"
if [[ "${1:-}" == "checkpoint" ]]; then
  shift
  script="$root/scripts/agent_checkpoint.py"
elif [[ "${1:-}" == "router" ]]; then
  shift
  script="$root/scripts/agent_skill_router.py"
elif [[ "${1:-}" == "audit" ]]; then
  shift
  script="$root/scripts/agent_stop_audit.py"
fi
if command -v python3 >/dev/null 2>&1; then
  exec python3 "$script" "$@"
elif command -v python >/dev/null 2>&1; then
  exec python "$script" "$@"
elif command -v py >/dev/null 2>&1; then
  exec py -3 "$script" "$@"
fi
exit 0
