#!/usr/bin/env bash
set -u

DRY_RUN=0
QUIET=0
SKIP_INDEX=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1 ;;
    --quiet) QUIET=1 ;;
    --skip-index) SKIP_INDEX=1 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
  shift
done

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FAILURES=()
MATT_SKILLS=(
  setup-matt-pocock-skills grill-with-docs wayfinder to-spec to-tickets
  implement tdd diagnosing-bugs code-review domain-modeling
)

log() {
  if [[ "$QUIET" -eq 0 ]]; then printf '%s\n' "$1"; fi
}

fail_open() {
  local label="$1"
  shift
  if [[ "$DRY_RUN" -eq 1 ]]; then
    log "[dry-run] $label"
    return 0
  fi
  if ! "$@" >/dev/null 2>&1; then
    FAILURES+=("$label")
    return 1
  fi
  return 0
}

if [[ -f "$ROOT/scripts/ensure-rtk.sh" ]]; then
  fail_open "Ensure pinned RTK" "$ROOT/scripts/ensure-rtk.sh" --quiet || true
else
  FAILURES+=("Ensure pinned RTK: scripts/ensure-rtk.sh is missing")
fi

PYTHON=""
for candidate in python3 python; do
  if command -v "$candidate" >/dev/null 2>&1; then PYTHON="$candidate"; break; fi
done

if [[ -z "$PYTHON" ]]; then
  FAILURES+=("Ensure jCodeMunch: Python is unavailable")
else
  fail_open "Ensure pinned jCodeMunch" "$PYTHON" "$ROOT/scripts/codex_jcodemunch.py" ensure --quiet || true
  if [[ "$SKIP_INDEX" -eq 0 ]]; then
    fail_open "Refresh Levyra jCodeMunch index" "$PYTHON" "$ROOT/scripts/codex_jcodemunch.py" index --quiet || true
  fi
fi

SKILL_ROOT="$HOME/.agents/skills"
MISSING=0
for skill in "${MATT_SKILLS[@]}"; do
  if [[ ! -f "$SKILL_ROOT/$skill/SKILL.md" ]]; then MISSING=1; break; fi
done

if [[ "$MISSING" -eq 1 ]]; then
  if ! command -v npx >/dev/null 2>&1; then
    FAILURES+=("Ensure Matt Pocock skills: npx is unavailable")
  elif [[ "$DRY_RUN" -eq 1 ]]; then
    log "[dry-run] Install missing Matt Pocock Codex skills"
  else
    ARGS=(skills@latest add mattpocock/skills -g -a codex -y)
    for skill in "${MATT_SKILLS[@]}"; do ARGS+=(-s "$skill"); done
    if ! npx "${ARGS[@]}" >/dev/null 2>&1; then
      FAILURES+=("Install missing Matt Pocock Codex skills")
    fi
  fi
fi

if [[ ${#FAILURES[@]} -gt 0 ]]; then
  if [[ "$QUIET" -eq 0 ]]; then
    for failure in "${FAILURES[@]}"; do printf 'warning: %s\n' "$failure" >&2; done
  fi
  exit 1
fi

log '[ok] Levyra Codex tooling is ready.'
