#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME="$ROOT/scripts/codex_jcodemunch.py"

if [[ ! -f "$RUNTIME" ]]; then
  printf 'Levyra jCodeMunch launcher: missing %s\n' "$RUNTIME" >&2
  exit 1
fi

for candidate in python3 python py; do
  if command -v "$candidate" >/dev/null 2>&1; then
    exec "$candidate" "$RUNTIME" serve
  fi
done

printf 'Levyra jCodeMunch launcher: Python is unavailable.\n' >&2
exit 1
