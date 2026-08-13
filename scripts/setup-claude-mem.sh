#!/usr/bin/env bash
set -uo pipefail

DRY_RUN=0
CLAUDE_MEM_PACKAGE="claude-mem@13.15.0"
CLAUDE_MEM_MODEL="claude-haiku-4-5-20251001"

if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=1
  shift
fi
if [[ $# -gt 0 ]]; then
  echo "Usage: ./scripts/setup-claude-mem.sh [--dry-run]" >&2
  exit 2
fi

has_command() {
  command -v "$1" >/dev/null 2>&1
}

run_cmem() {
  local label="$1"
  local allow_failure="$2"
  shift 2

  if [[ "$DRY_RUN" -eq 1 ]]; then
    printf '[dry-run] %s: npx --yes %q' "$label" "$CLAUDE_MEM_PACKAGE"
    printf ' %q' "$@"
    printf '\n'
    return 0
  fi

  echo "[run] $label"
  if printf '' | npx --yes "$CLAUDE_MEM_PACKAGE" "$@"; then
    return 0
  fi

  if [[ "$allow_failure" -eq 1 ]]; then
    echo "[warn] $label failed; continuing so claude-mem cannot block Levyra setup." >&2
    return 1
  fi

  echo "[blocked] $label failed." >&2
  return 1
}

echo "Levyra claude-mem setup"
echo "Package: $CLAUDE_MEM_PACKAGE"

if ! has_command npx; then
  echo "[blocked] npx is required for claude-mem. Install Node.js/npm, then rerun this script." >&2
  exit 1
fi

ides=()
if has_command claude; then
  ides+=("claude-code")
fi
if has_command codex || [[ -d "$HOME/.codex" ]]; then
  ides+=("codex-cli")
fi
if has_command agy || [[ -d "$HOME/.gemini/antigravity" ]]; then
  ides+=("antigravity")
fi

if [[ "${#ides[@]}" -eq 0 ]]; then
  echo "[skip] No local Claude Code, Codex CLI, or Antigravity installation was detected."
  echo "ChatGPT uses claude-mem only through a separately connected MCP app; see docs/ai/CLAUDE_MEM.md."
  exit 0
fi

failed=()
for ide in "${ides[@]}"; do
  if ! run_cmem "Install claude-mem for $ide" 1 \
    install \
    --ide "$ide" \
    --provider claude \
    --model "$CLAUDE_MEM_MODEL" \
    --runtime worker \
    --no-auto-start
  then
    failed+=("$ide")
  fi
done

if [[ "${#failed[@]}" -eq "${#ides[@]}" ]]; then
  echo "[blocked] claude-mem failed for every detected runtime: ${failed[*]}" >&2
  exit 1
fi

run_cmem "Disable claude-mem anonymous telemetry" 1 telemetry disable || true
run_cmem "Start claude-mem worker" 1 start || true

healthy=0
if run_cmem "Check claude-mem health" 1 doctor; then
  healthy=1
else
  echo "[warn] claude-mem health check failed. Running one official repair attempt." >&2
  if run_cmem "Repair claude-mem runtime" 1 repair; then
    run_cmem "Restart claude-mem worker after repair" 1 start || true
    if run_cmem "Re-check claude-mem health" 1 doctor; then
      healthy=1
    fi
  fi
fi

echo
if [[ "$healthy" -eq 1 ]]; then
  echo "[ok] claude-mem is installed and healthy."
else
  echo "[warn] claude-mem remains unhealthy. Levyra agents must continue without memory instead of blocking work." >&2
fi

if [[ "${#failed[@]}" -gt 0 ]]; then
  echo "[blocked] Integration failed for: ${failed[*]}" >&2
  exit 1
fi
if [[ "$healthy" -ne 1 ]]; then
  exit 1
fi

echo "Configured runtimes: ${ides[*]}"
echo "Cloud sync was not enabled. Experimental semantic injection was not enabled."
echo "Start a new coding-agent conversation so hooks and MCP tools are reloaded."
