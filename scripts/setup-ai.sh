#!/usr/bin/env bash
set -euo pipefail

DRY_RUN=0
INSTALL_RTK=0
INSTALL_PLUGINS=0
SKIP_HOOKS=0

usage() {
  cat <<'EOF'
Usage: ./scripts/setup-ai.sh [options]

Options:
  --dry-run       Print planned actions without changing the machine
  --install-rtk   Install RTK through Cargo when it is missing
  --plugins       Install plugins listed in codex-plugins.txt
  --skip-hooks    Do not initialize RTK instructions/hooks/integrations
  -h, --help      Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1 ;;
    --install-rtk) INSTALL_RTK=1 ;;
    --plugins) INSTALL_PLUGINS=1 ;;
    --skip-hooks) SKIP_HOOKS=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
  shift
done

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
PLUGIN_MANIFEST="$REPO_ROOT/codex-plugins.txt"

has_command() {
  command -v "$1" >/dev/null 2>&1
}

run_step() {
  local label="$1"
  shift
  if [[ "$DRY_RUN" -eq 1 ]]; then
    printf '[dry-run] %s:' "$label"
    printf ' %q' "$@"
    printf '\n'
    return 0
  fi
  echo "[run] $label"
  "$@"
}

echo "Levyra AI efficiency setup"
echo "Repository: $REPO_ROOT"

if ! has_command rtk; then
  if [[ "$INSTALL_RTK" -ne 1 ]]; then
    echo "[warn] RTK is not installed. Re-run with --install-rtk to install it through Cargo." >&2
  elif ! has_command cargo; then
    echo "Cargo is required for --install-rtk. Install Rust/Cargo or install an official RTK binary manually." >&2
    exit 1
  else
    run_step "Install RTK from rtk-ai/rtk" cargo install --git https://github.com/rtk-ai/rtk
  fi
fi

if has_command rtk; then
  run_step "Verify RTK" rtk --version

  if [[ "$SKIP_HOOKS" -ne 1 ]]; then
    if has_command codex; then
      run_step "Install global RTK instructions for Codex" rtk init -g --codex
    else
      echo "[skip] Codex command not detected"
    fi

    if has_command claude; then
      run_step "Configure the global RTK hook for Claude Code" rtk init -g
    else
      echo "[skip] Claude Code command not detected"
    fi

    if has_command opencode; then
      run_step "Configure the global RTK integration for OpenCode" rtk init -g --opencode
    else
      echo "[skip] OpenCode command not detected"
    fi

    if [[ "$DRY_RUN" -eq 1 ]]; then
      echo "[dry-run] Configure the repository-local RTK integration for Antigravity: (cd '$REPO_ROOT' && rtk init --agent antigravity)"
    else
      echo "[run] Configure the repository-local RTK integration for Antigravity"
      (cd "$REPO_ROOT" && rtk init --agent antigravity)
    fi
  fi

  run_step "Show the active RTK configuration" rtk init --show
fi

if [[ "$INSTALL_PLUGINS" -eq 1 ]]; then
  if [[ ! -f "$PLUGIN_MANIFEST" ]]; then
    echo "Plugin manifest not found: $PLUGIN_MANIFEST" >&2
    exit 1
  fi
  if ! has_command codex; then
    echo "Codex is required when using --plugins." >&2
    exit 1
  fi

  while IFS= read -r line || [[ -n "$line" ]]; do
    plugin="${line#"${line%%[![:space:]]*}"}"
    plugin="${plugin%"${plugin##*[![:space:]]}"}"
    [[ -z "$plugin" || "$plugin" == \#* ]] && continue
    run_step "Install Codex plugin $plugin" codex plugin add "$plugin"
  done < "$PLUGIN_MANIFEST"
fi

PYTHON_COMMAND=""
if has_command python3; then
  PYTHON_COMMAND="python3"
elif has_command python; then
  PYTHON_COMMAND="python"
fi

if [[ -z "$PYTHON_COMMAND" ]]; then
  echo "[blocked] Python is required to verify Levyra agent and AI-efficiency configuration." >&2
  exit 1
fi

for validation_script in \
  scripts/validate_agent_config.py \
  scripts/validate_ai_efficiency.py
do
  if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "[dry-run] Validate with $validation_script: (cd '$REPO_ROOT' && $PYTHON_COMMAND $validation_script)"
  else
    echo "[run] Validate with $validation_script"
    (cd "$REPO_ROOT" && "$PYTHON_COMMAND" "$validation_script")
  fi
done

echo
echo "Setup complete."
echo "Restart each detected coding agent or start a new conversation so instructions, hooks, rules, plugins, and Levyra skills are reloaded."
echo 'Use `rtk gain` and `rtk discover --all --since 7` to measure real command-output savings.'
