#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"

for candidate in \
  "$REPO_ROOT/.levyra-tools/headroom/headroom" \
  "$REPO_ROOT/.levyra-tools/headroom/headroom.exe"
do
  if [[ -x "$candidate" ]]; then
    exec "$candidate"
  fi
done

exit 0
