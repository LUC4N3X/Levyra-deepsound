#!/usr/bin/env bash
set -euo pipefail

PROJECT_FILTER="Levyra-deepsound"

if ! command -v npx >/dev/null 2>&1; then
  echo "CodeBurn requires Node.js 22.13+ with npm/npx." >&2
  exit 1
fi

if [[ $# -eq 0 ]]; then
  set -- overview -p week
fi

exec npx -y "codeburn@0.9.24" "$@" --project "$PROJECT_FILTER"
