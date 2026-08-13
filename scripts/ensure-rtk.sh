#!/usr/bin/env bash
set -uo pipefail

DRY_RUN=0
QUIET=0
RTK_GIT_REVISION="b34be37caf3796b69a50952a28e60e32b5daad43"

usage() {
  echo "Usage: ./scripts/ensure-rtk.sh [--dry-run] [--quiet]"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1 ;;
    --quiet) QUIET=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
  shift
done

rtk_ready() {
  command -v rtk >/dev/null 2>&1 &&
    rtk --version >/dev/null 2>&1 &&
    rtk gain >/dev/null 2>&1
}

say() {
  if [[ "$QUIET" -ne 1 ]]; then
    echo "$*"
  fi
}

if rtk_ready; then
  say "[ok] Levyra RTK is ready."
  exit 0
fi

if ! command -v cargo >/dev/null 2>&1; then
  echo "[warn] Levyra RTK is unavailable and Cargo is not installed; continuing without RTK." >&2
  exit 1
fi

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[dry-run] cargo install --git https://github.com/rtk-ai/rtk --rev $RTK_GIT_REVISION --force"
  exit 0
fi

say "[run] Installing the owner-authorized pinned RTK build for Levyra."
if ! cargo install --git https://github.com/rtk-ai/rtk --rev "$RTK_GIT_REVISION" --force; then
  echo "[warn] RTK installation failed; continuing without RTK." >&2
  exit 1
fi

if ! rtk_ready; then
  echo "[warn] RTK installed but validation failed; continuing without RTK." >&2
  exit 1
fi

say "[ok] Levyra RTK installed and verified."
