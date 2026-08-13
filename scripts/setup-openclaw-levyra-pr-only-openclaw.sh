#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash "$ROOT/scripts/setup-openclaw-levyra-pr-only.sh"
bash "$ROOT/scripts/openclaw/configure-evidence-runtime.sh"
