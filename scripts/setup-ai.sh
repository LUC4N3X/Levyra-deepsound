#!/usr/bin/env bash
set -euo pipefail

DRY_RUN=0
INSTALL_RTK=0
INSTALL_PLUGINS=0
INSTALL_CLAUDE_MEM=0
SKIP_HOOKS=0
SKIP_MATT_SKILLS=0

usage() {
  cat <<'EOF'
Usage: ./scripts/setup-ai.sh [options]

Options:
  --dry-run           Print planned actions without changing the machine
  --install-rtk       Install RTK through Cargo when it is missing
  --plugins           Install plugins listed in .agents/config/codex-plugins.txt
  --claude-mem        Explicitly install pinned claude-mem for detected supported runtimes
  --skip-hooks        Do not initialize RTK instructions/hooks/integrations
  --skip-matt-skills  Do not install Matt Pocock engineering skills for Codex
  -h, --help          Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1 ;;
    --install-rtk) INSTALL_RTK=1 ;;
    --plugins) INSTALL_PLUGINS=1 ;;
    --claude-mem) INSTALL_CLAUDE_MEM=1 ;;
    --skip-hooks) SKIP_HOOKS=1 ;;
    --skip-matt-skills) SKIP_MATT_SKILLS=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
  shift
done

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
PLUGIN_MANIFEST="$REPO_ROOT/.agents/config/codex-plugins.txt"
RTK_GIT_REVISION="b34be37caf3796b69a50952a28e60e32b5daad43"
MATT_SKILL_SOURCE="mattpocock/skills"
MATT_SKILLS=(
  setup-matt-pocock-skills
  grill-with-docs
  wayfinder
  to-spec
  to-tickets
  implement
  tdd
  diagnosing-bugs
  code-review
  domain-modeling
)

has_command() {
  command -v "$1" >/dev/null 2>&1
}

is_token_killer_rtk() {
  has_command rtk && rtk gain >/dev/null 2>&1
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

if [[ "$INSTALL_CLAUDE_MEM" -eq 1 ]]; then
  claude_mem_args=()
  if [[ "$DRY_RUN" -eq 1 ]]; then
    claude_mem_args+=(--dry-run)
  fi
  if ! "$SCRIPT_DIR/setup-claude-mem.sh" "${claude_mem_args[@]}"; then
    echo "[warn] claude-mem setup did not complete; continuing Levyra AI setup without persistent memory." >&2
  fi
fi

if ! is_token_killer_rtk; then
  if [[ "$INSTALL_RTK" -ne 1 ]]; then
    echo "[warn] The official RTK Token Killer is missing. Re-run with --install-rtk to install the pinned build through Cargo." >&2
  elif ! has_command cargo; then
    echo "Cargo is required for --install-rtk. Install Rust/Cargo or install an official RTK binary manually." >&2
    exit 1
  else
    run_step "Install RTK from rtk-ai/rtk" cargo install --git https://github.com/rtk-ai/rtk --rev "$RTK_GIT_REVISION" --force
  fi
fi

if is_token_killer_rtk; then
  run_step "Verify RTK" rtk --version
  run_step "Verify RTK Token Killer commands" rtk gain

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

if [[ "$SKIP_MATT_SKILLS" -ne 1 ]]; then
  if ! has_command codex; then
    echo "[skip] Codex command not detected; Matt Pocock Codex skills were not installed"
  elif ! has_command npx; then
    echo "[warn] Codex is installed but npx is unavailable. Matt Pocock skills bootstrap is blocked; install Node.js/npm or re-run with --skip-matt-skills." >&2
  else
    matt_args=(skills@latest add "$MATT_SKILL_SOURCE" -g -a codex -y)
    for skill in "${MATT_SKILLS[@]}"; do
      matt_args+=(-s "$skill")
    done
    run_step "Install focused Matt Pocock engineering skills for Codex" npx "${matt_args[@]}"
  fi
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
  scripts/validate_ai_efficiency.py \
  scripts/validate_matt_skills.py \
  scripts/validate_claude_mem.py
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
echo "Claude Code will discover the project-enabled mattpocock-skills plugin through .claude/settings.json and may request normal marketplace trust/installation approval."
echo "Use --claude-mem once when you explicitly want the pinned claude-mem integration for detected Claude Code, Codex CLI, and Antigravity runtimes."
echo "ChatGPT uses claude-mem only when a compatible MCP app is connected; see docs/ai/CLAUDE_MEM.md."
echo "Antigravity and ChatGPT use the repository-native levyra-real-engineering adapter; see docs/ai/MATT_POCOCK_SKILLS.md."
echo 'Use `rtk gain` and `rtk discover --all --since 7` to measure real command-output savings.'
