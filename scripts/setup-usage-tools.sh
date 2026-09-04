#!/usr/bin/env bash
set -euo pipefail

DRY_RUN=0
if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=1
  shift
fi
if [[ $# -ne 0 ]]; then
  echo "Usage: bash ./scripts/setup-usage-tools.sh [--dry-run]" >&2
  exit 2
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
TOOL_ROOT="$REPO_ROOT/.levyra-tools"
HEADROOM_PREFIX="$TOOL_ROOT/headroom"
HEADROOM_VERSION="v0.3.0"
CODEBURN_VERSION="0.9.24"
HEADROOM_INSTALLER_URL="https://raw.githubusercontent.com/anthonybo/headroom/$HEADROOM_VERSION/install.sh"

echo "Levyra AI usage tooling setup"
echo "Repository: $REPO_ROOT"

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[dry-run] Install Headroom $HEADROOM_VERSION into $HEADROOM_PREFIX without changing global Claude settings or PATH"
else
  if ! command -v curl >/dev/null 2>&1; then
    echo "curl is required to install Headroom." >&2
    exit 1
  fi
  tmp_dir="$(mktemp -d)"
  trap 'rm -rf "$tmp_dir"' EXIT
  installer="$tmp_dir/install.sh"
  curl -fsSL "$HEADROOM_INSTALLER_URL" -o "$installer"
  PREFIX="$HEADROOM_PREFIX" VERSION="$HEADROOM_VERSION" sh "$installer" --no-wire --no-path
  "$HEADROOM_PREFIX/headroom" --version
  rm -rf "$tmp_dir"
  trap - EXIT
fi

if ! command -v npx >/dev/null 2>&1; then
  echo "[warn] CodeBurn $CODEBURN_VERSION requires Node.js 22.13+ with npm/npx. Headroom setup can still be used." >&2
elif [[ "$DRY_RUN" -eq 1 ]]; then
  echo "[dry-run] Verify CodeBurn $CODEBURN_VERSION through npx without installing it globally"
else
  npx -y "codeburn@$CODEBURN_VERSION" --version
fi

echo
echo "Setup complete."
echo "Headroom is project-local under .levyra-tools/ and is wired by the tracked Claude statusLine configuration."
echo "CodeBurn stays uninstalled globally and is invoked through scripts/codeburn-levyra.sh."
