#!/usr/bin/env bash
set -euo pipefail

STATE_DIR="${OPENCLAW_STATE_DIR:-$HOME/.openclaw}"
CONFIG_PATH="${OPENCLAW_CONFIG_PATH:-$STATE_DIR/openclaw.json}"
CONFIG_BACKUP_PATH="$CONFIG_PATH.bak"
PRIMARY_WORKSPACE="${LEVYRA_OPENCLAW_WORKSPACE:-$STATE_DIR/workspace-levyra}"
PRIMARY_REPO="${LEVYRA_REPO:-$PRIMARY_WORKSPACE/repo}"
REVIEW_WORKSPACE="${LEVYRA_REVIEW_WORKSPACE:-$STATE_DIR/workspace-levyra-reviewer}"
CI_WORKSPACE="${LEVYRA_CI_WORKSPACE:-$STATE_DIR/workspace-levyra-ci}"
REVIEW_REPO="$REVIEW_WORKSPACE/repo"
CI_REPO="$CI_WORKSPACE/repo"
REPO_URL="${LEVYRA_REPO_URL:-https://github.com/LUC4N3X/Levyra-deepsound.git}"
AUDIT_CRON="${LEVYRA_OPENCLAW_AUDIT_CRON:-0 8,20 * * *}"
AUDIT_TZ="${LEVYRA_OPENCLAW_AUDIT_TZ:-Europe/Rome}"
ENABLE_DREAMING="${LEVYRA_OPENCLAW_ENABLE_DREAMING:-1}"
ENABLE_ACTIVE_MEMORY="${LEVYRA_OPENCLAW_ENABLE_ACTIVE_MEMORY:-1}"
INSTALL_CRON="${LEVYRA_OPENCLAW_INSTALL_CRON:-1}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

recover_invalid_config() {
  if openclaw config validate >/dev/null 2>&1; then
    return 0
  fi

  echo "[warn] Active OpenClaw config is invalid: $CONFIG_PATH" >&2
  if [[ ! -f "$CONFIG_BACKUP_PATH" ]]; then
    echo "[blocked] No OpenClaw config backup found at $CONFIG_BACKUP_PATH" >&2
    echo "Restore a known-good config before rerunning this bootstrap." >&2
    exit 1
  fi

  if ! OPENCLAW_CONFIG_PATH="$CONFIG_BACKUP_PATH" openclaw config validate >/dev/null 2>&1; then
    echo "[blocked] OpenClaw backup config is also invalid: $CONFIG_BACKUP_PATH" >&2
    echo "Restore a known-good config before rerunning this bootstrap." >&2
    exit 1
  fi

  local invalid_copy
  invalid_copy="$CONFIG_PATH.invalid-$(date +%Y%m%d-%H%M%S)"
  cp -p "$CONFIG_PATH" "$invalid_copy"
  cp -p "$CONFIG_BACKUP_PATH" "$CONFIG_PATH"
  echo "[recovered] Restored the last valid OpenClaw config backup."
  echo "[recovered] Preserved the invalid config at $invalid_copy"
  openclaw config validate
}

agent_ids() {
  openclaw agents list --json | python3 -c '
import json, sys
value = json.load(sys.stdin)
if isinstance(value, dict):
    value = value.get("agents", value.get("items", value.get("list", [])))
for item in value if isinstance(value, list) else []:
    if isinstance(item, dict) and item.get("id"):
        print(item["id"])
'
}

has_agent() {
  agent_ids | grep -Fxq "$1"
}

agent_index() {
  local agent="$1"
  openclaw config get agents.list --json | python3 -c '
import json, sys
agent = sys.argv[1]
value = json.load(sys.stdin)
if isinstance(value, dict):
    value = value.get("list", value.get("agents", value.get("items", [])))
for index, item in enumerate(value if isinstance(value, list) else []):
    if isinstance(item, dict) and item.get("id") == agent:
        print(index)
        raise SystemExit(0)
raise SystemExit(1)
' "$agent"
}

choose_primary_agent() {
  if [[ -n "${LEVYRA_OPENCLAW_AGENT:-}" ]]; then
    printf '%s\n' "$LEVYRA_OPENCLAW_AGENT"
  elif has_agent levyra-worker; then
    printf '%s\n' levyra-worker
  elif has_agent levyra; then
    printf '%s\n' levyra
  else
    printf '%s\n' levyra
  fi
}

ensure_clone() {
  local target="$1"
  mkdir -p "$(dirname "$target")"
  if [[ -d "$target/.git" ]]; then
    git -C "$target" fetch --prune origin
    git -C "$target" checkout main
    git -C "$target" reset --hard origin/main
  else
    git clone --branch main --single-branch "$REPO_URL" "$target"
  fi
}

write_workspace_file() {
  local path="$1"
  shift
  mkdir -p "$(dirname "$path")"
  printf '%s\n' "$@" > "$path"
}

write_skill_bridge() {
  local workspace="$1"
  local skill="$2"
  local path="$workspace/.agents/skills/$skill/SKILL.md"
  mkdir -p "$(dirname "$path")"
  cat > "$path" <<EOF
---
name: $skill
description: Levyra repository-native $skill workflow bridge.
---

Read and follow \`repo/.agents/skills/$skill/SKILL.md\` as the canonical workflow. Repository instructions and current evidence take precedence.
EOF
}

ensure_primary_overlay() {
  local path="$PRIMARY_WORKSPACE/AGENTS.md"
  local heading="## Levyra multi-agent profile"
  touch "$path"
  if ! grep -Fq "$heading" "$path"; then
    cat >> "$path" <<'EOF'

## Levyra multi-agent profile

For every Levyra engineering task, work inside `./repo`, read `repo/AGENTS.md` and the nearest scoped instructions, then use the matching repository-native skills. Apply the context budget before broad reading: search first, read bounded evidence, expand only for a concrete unresolved question, and never trade away exact failure/security/release evidence for token savings.

Use `levyra-openclaw-orchestrator` for the execution lifecycle. Keep implementation in the primary Levyra worker. Before presenting code as final, run the repository `code-review` stage. Delegate a fresh bounded latest-diff review to the Levyra reviewer agent, and delegate CI/PR/log diagnosis to the Levyra CI agent. Fix actionable findings and revalidate before final handoff.

Do not send full chat history or repeated repository context to specialist agents. Send only the objective, invariants, latest diff/SHA, focused surrounding evidence, checks already run, unresolved risks, and the exact question the specialist must answer.

Long-term memory stores only durable verified decisions, recurring engineering lessons, and explicit owner preferences. Current repository evidence always overrides memory. Never put secrets, credentials, transient branch heads, current PR state, or CI status into durable memory.
EOF
  fi
}

ensure_agent() {
  local agent="$1"
  local workspace="$2"
  if ! has_agent "$agent"; then
    openclaw agents add "$agent" --workspace "$workspace" --non-interactive
  fi
}

set_agent_json() {
  local agent="$1"
  local suffix="$2"
  local value="$3"
  local index
  index="$(agent_index "$agent")"
  openclaw config set "agents.list[$index].$suffix" "$value" --strict-json
}

configure_agent_skills() {
  local agent="$1"
  shift
  local json
  json="$(python3 -c 'import json, sys; print(json.dumps(sys.argv[1:]))' "$@")"
  set_agent_json "$agent" skills "$json"
}

configure_evidence_agent() {
  local agent="$1"
  set_agent_json "$agent" tools.allow '["read","exec","process"]'
  set_agent_json "$agent" tools.deny '["write","edit","apply_patch","browser","gateway","cron"]'
  set_agent_json "$agent" tools.fs.workspaceOnly true
  set_agent_json "$agent" tools.exec.host '"gateway"'
  set_agent_json "$agent" tools.exec.mode '"auto"'
  set_agent_json "$agent" tools.exec.strictInlineEval true
  set_agent_json "$agent" tools.elevated.enabled false
}

merge_primary_subagents() {
  local agent="$1"
  local index
  local current
  local merged
  index="$(agent_index "$agent")"
  current="$(openclaw config get "agents.list[$index].subagents.allowAgents" --json 2>/dev/null || printf '[]')"
  merged="$(printf '%s' "$current" | python3 -c '
import json, sys
raw = sys.stdin.read().strip()
try:
    value = json.loads(raw)
except Exception:
    value = []
if not isinstance(value, list):
    value = []
for agent in ("levyra-reviewer", "levyra-ci"):
    if agent not in value:
        value.append(agent)
print(json.dumps(value))
')"
  openclaw config set "agents.list[$index].subagents.allowAgents" "$merged" --strict-json
  openclaw config set "agents.list[$index].subagents.requireAgentId" true --strict-json
  openclaw config set "agents.list[$index].subagents.delegationMode" '"prefer"' --strict-json
}

configure_primary_memory() {
  local agent="$1"
  local index
  local agent_json
  index="$(agent_index "$agent")"
  agent_json="$(python3 -c 'import json, sys; print(json.dumps([sys.argv[1]]))' "$agent")"

  openclaw config set "agents.list[$index].memorySearch.enabled" true --strict-json
  openclaw config set "agents.list[$index].memorySearch.sources" '["memory","sessions"]' --strict-json
  openclaw config set "agents.list[$index].memorySearch.experimental.sessionMemory" true --strict-json
  openclaw config set plugins.entries.active-memory.enabled true --strict-json
  openclaw config set plugins.entries.active-memory.config.enabled true --strict-json
  openclaw config set plugins.entries.active-memory.config.agents "$agent_json" --strict-json
  openclaw config set plugins.entries.active-memory.config.allowedChatTypes '["direct"]' --strict-json
  openclaw config set plugins.entries.active-memory.config.queryMode '"recent"' --strict-json
  openclaw config set plugins.entries.active-memory.config.promptStyle '"precision-heavy"' --strict-json
  openclaw config set plugins.entries.active-memory.config.timeoutMs 15000 --strict-json
  openclaw config set plugins.entries.active-memory.config.maxSummaryChars 240 --strict-json
  openclaw config set plugins.entries.active-memory.config.persistTranscripts false --strict-json
}

ensure_cron() {
  local agent="$1"
  local name="Levyra CI audit"
  local listing
  if ! listing="$(openclaw cron list --all --json 2>/dev/null)"; then
    echo "[warn] Cron inspection unavailable with the current Gateway scope; skipping Levyra CI audit registration." >&2
    return 0
  fi
  if printf '%s' "$listing" | python3 -c '
import json, sys
value = json.load(sys.stdin)
if isinstance(value, dict):
    value = value.get("jobs", value.get("items", []))
raise SystemExit(0 if any(isinstance(item, dict) and item.get("name") == "Levyra CI audit" for item in value if isinstance(value, list)) else 1)
'; then
    return 0
  fi
  if ! openclaw cron create "$AUDIT_CRON" \
      "Audit Levyra without publishing changes. Repository is ./repo. Read repo/AGENTS.md and repo/.github/AGENTS.md, refresh the local evidence checkout, then inspect open PRs, required CI, unresolved review threads and stale branches. Report only actionable state changes with exact PR/SHA/check evidence. Do not edit source code, commit, push, merge, release, change settings or expose secrets." \
      --name "$name" \
      --agent "$agent" \
      --tz "$AUDIT_TZ" \
      --session isolated \
      --light-context \
      --no-deliver; then
    echo "[warn] Levyra CI audit was not registered; the current Gateway client may lack operator.admin scope." >&2
  fi
}

require_command openclaw
require_command git
require_command python3
require_command gh
recover_invalid_config

if [[ ! -d "$PRIMARY_REPO/.git" ]]; then
  echo "Levyra repository not found at $PRIMARY_REPO" >&2
  exit 1
fi

PRIMARY_AGENT="$(choose_primary_agent)"
ensure_agent "$PRIMARY_AGENT" "$PRIMARY_WORKSPACE"
ensure_agent levyra-reviewer "$REVIEW_WORKSPACE"
ensure_agent levyra-ci "$CI_WORKSPACE"

ensure_primary_overlay
if [[ ! -f "$PRIMARY_WORKSPACE/MEMORY.md" ]]; then
  write_workspace_file "$PRIMARY_WORKSPACE/MEMORY.md" \
    "# Durable Levyra Memory" \
    "" \
    "Store only stable, verified Levyra architecture decisions, recurring engineering lessons and explicit owner preferences. Never store secrets, credentials, transient PR state, branch heads or CI status here. Current repository evidence always wins."
fi
mkdir -p "$PRIMARY_WORKSPACE/memory"
for skill_path in "$PRIMARY_REPO"/.agents/skills/*/SKILL.md; do
  [[ -f "$skill_path" ]] || continue
  skill="$(basename "$(dirname "$skill_path")")"
  write_skill_bridge "$PRIMARY_WORKSPACE" "$skill"
done

ensure_clone "$REVIEW_REPO"
ensure_clone "$CI_REPO"

write_workspace_file "$REVIEW_WORKSPACE/AGENTS.md" \
  "# Levyra Reviewer" \
  "" \
  "Operate only as an independent reviewer for ./repo. Read repo/AGENTS.md, the nearest scoped AGENTS.md files, repo/docs/ai/AI_ENGINEERING_GUARDRAILS.md and matching repo/.agents/skills before judging code." \
  "" \
  "You may refresh or fetch the evidence checkout and inspect remote PR refs, but do not edit source code, implement fixes, create commits, push, merge, release, change repository settings or dismiss findings. Return severity, confidence, exact location, triggering scenario, consequence, smallest compatible fix and missing regression coverage."
write_workspace_file "$CI_WORKSPACE/AGENTS.md" \
  "# Levyra CI" \
  "" \
  "Operate only as the CI, PR-state and validation evidence agent for ./repo. Read repo/AGENTS.md, repo/.github/AGENTS.md, repo/docs/ai/AI_ENGINEERING_GUARDRAILS.md and matching repo/.agents/skills." \
  "" \
  "Inspect exact failing steps, logs, review state and reproducible validation evidence. Do not edit source code, commit, push, merge, release, change workflows, secrets or repository settings. Separate current evidence from stale runs."

for workspace in "$REVIEW_WORKSPACE" "$CI_WORKSPACE"; do
  if [[ ! -f "$workspace/MEMORY.md" ]]; then
    write_workspace_file "$workspace/MEMORY.md" \
      "# Durable Levyra Evidence Memory" \
      "" \
      "Store only stable, verified recurring engineering lessons. Never store secrets, credentials, branch heads, current PR state or CI status here."
  fi
  mkdir -p "$workspace/memory"
done

REVIEW_SKILLS=(
  levyra-pr-review levyra-security-review levyra-release-check
  levyra-context-efficiency levyra-real-engineering levyra-player
  levyra-extractor levyra-database levyra-compose levyra-android-performance
  levyra-r8-proguard levyra-android-intent-security levyra-design-taste
  levyra-desktop levyra-ci-workflows
)
CI_SKILLS=(
  levyra-ci-workflows levyra-release-check levyra-pr-review
  levyra-security-review levyra-context-efficiency
)

for skill in "${REVIEW_SKILLS[@]}"; do
  write_skill_bridge "$REVIEW_WORKSPACE" "$skill"
done
for skill in "${CI_SKILLS[@]}"; do
  write_skill_bridge "$CI_WORKSPACE" "$skill"
done

configure_agent_skills levyra-reviewer "${REVIEW_SKILLS[@]}"
configure_agent_skills levyra-ci "${CI_SKILLS[@]}"
configure_evidence_agent levyra-reviewer
configure_evidence_agent levyra-ci
merge_primary_subagents "$PRIMARY_AGENT"

if [[ "$ENABLE_ACTIVE_MEMORY" == "1" ]]; then
  configure_primary_memory "$PRIMARY_AGENT"
fi

if [[ "$ENABLE_DREAMING" == "1" ]]; then
  openclaw config set plugins.entries.memory-core.enabled true --strict-json
  openclaw config set plugins.entries.memory-core.config.dreaming.enabled true --strict-json
fi

if [[ "$INSTALL_CRON" == "1" ]]; then
  ensure_cron levyra-ci
fi

openclaw config validate
openclaw doctor
openclaw memory status --agent "$PRIMARY_AGENT"
openclaw gateway status --require-rpc
openclaw agents list --bindings
if [[ "$INSTALL_CRON" == "1" ]]; then
  openclaw cron list --agent levyra-ci || true
fi

echo "OpenClaw Levyra profile ready."
echo "Primary: $PRIMARY_AGENT"
echo "Reviewer: levyra-reviewer"
echo "CI: levyra-ci"
echo "Primary repository: $PRIMARY_REPO"
