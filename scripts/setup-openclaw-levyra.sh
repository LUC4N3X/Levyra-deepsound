#!/usr/bin/env bash
set -euo pipefail

STATE_DIR="${OPENCLAW_STATE_DIR:-$HOME/.openclaw}"
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

configure_agent_skills() {
  local agent="$1"
  shift
  local json
  json="$(python3 -c 'import json, sys; print(json.dumps(sys.argv[1:]))' "$@")"
  openclaw config set "agents.entries.$agent.skills" "$json" --strict-json
}

configure_evidence_agent() {
  local agent="$1"
  openclaw config set "agents.entries.$agent.tools.allow" '["read","exec","process"]' --strict-json
  openclaw config set "agents.entries.$agent.tools.deny" '["write","edit","apply_patch","browser","gateway"]' --strict-json
  openclaw config set "agents.entries.$agent.tools.fs.workspaceOnly" true --strict-json
  openclaw config set "agents.entries.$agent.tools.exec.host" '"gateway"' --strict-json
  openclaw config set "agents.entries.$agent.tools.exec.mode" '"auto"' --strict-json
  openclaw config set "agents.entries.$agent.tools.exec.strictInlineEval" true --strict-json
  openclaw config set "agents.entries.$agent.tools.elevated.enabled" false --strict-json
}

merge_primary_subagents() {
  local agent="$1"
  local current
  local merged
  current="$(openclaw config get "agents.entries.$agent.subagents.allowAgents" --json 2>/dev/null || printf '[]')"
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
  openclaw config set "agents.entries.$agent.subagents.allowAgents" "$merged" --strict-json
}

ensure_agent() {
  local agent="$1"
  local workspace="$2"
  if ! has_agent "$agent"; then
    openclaw agents add "$agent" --workspace "$workspace" --non-interactive
  fi
}

ensure_cron() {
  local agent="$1"
  local name="Levyra CI audit"
  local exists
  exists="$(openclaw cron list --all --json | python3 -c '
import json, sys
value = json.load(sys.stdin)
if isinstance(value, dict):
    value = value.get("jobs", value.get("items", []))
print("yes" if any(isinstance(item, dict) and item.get("name") == "Levyra CI audit" for item in value if isinstance(value, list)) else "no")
')"
  if [[ "$exists" == "no" ]]; then
    openclaw cron create "$AUDIT_CRON" \
      "Audit Levyra without publishing changes. Refresh the local evidence checkout, inspect open PRs, required CI, unresolved review threads and stale branches. Report only actionable state changes with exact PR/SHA/check evidence. Do not edit source code, commit, push, merge, release, change settings or expose secrets." \
      --name "$name" \
      --agent "$agent" \
      --tz "$AUDIT_TZ" \
      --session isolated \
      --light-context \
      --no-deliver
  fi
}

require_command openclaw
require_command git
require_command python3
require_command gh

if [[ ! -d "$PRIMARY_REPO/.git" ]]; then
  echo "Levyra repository not found at $PRIMARY_REPO" >&2
  exit 1
fi

PRIMARY_AGENT="$(choose_primary_agent)"
if ! has_agent "$PRIMARY_AGENT"; then
  openclaw agents add "$PRIMARY_AGENT" --workspace "$PRIMARY_WORKSPACE" --non-interactive
fi

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
  "You may refresh or fetch the private evidence checkout and inspect remote PR refs, but do not edit source code, implement fixes, create commits, push, merge, release, change repository settings or dismiss findings. Return severity, confidence, exact location, triggering scenario, consequence, smallest compatible fix and missing regression coverage. Preserve raw evidence for security, release, R8, Perfetto and exact failures." \
  "" \
  "Use a compact context budget: diff first, then bounded surrounding code, then expand only for a concrete unresolved question. Do not reread unchanged evidence."
write_workspace_file "$REVIEW_WORKSPACE/TOOLS.md" \
  "# Tools" \
  "" \
  "Repository: ./repo" \
  "Use Git/GitHub inspection, focused searches and non-publishing validation. Host exec uses OpenClaw auto-review; elevated tools stay disabled."
if [[ ! -f "$REVIEW_WORKSPACE/MEMORY.md" ]]; then
  write_workspace_file "$REVIEW_WORKSPACE/MEMORY.md" \
    "# Durable Review Memory" \
    "" \
    "Store only stable, verified Levyra review lessons and recurring failure patterns. Never store secrets, credentials, transient PR state, branch heads or CI status here."
fi
mkdir -p "$REVIEW_WORKSPACE/memory"

write_workspace_file "$CI_WORKSPACE/AGENTS.md" \
  "# Levyra CI" \
  "" \
  "Operate only as the CI, PR-state and validation evidence agent for ./repo. Read repo/AGENTS.md, repo/.github/AGENTS.md, repo/docs/ai/AI_ENGINEERING_GUARDRAILS.md and matching repo/.agents/skills." \
  "" \
  "You may refresh or fetch the private evidence checkout and inspect GitHub state. Inspect exact failing steps, logs, review state and reproducible validation evidence. Do not edit source code, commit, push, merge, release, change workflows, secrets or repository settings. Separate current evidence from stale runs." \
  "" \
  "Use a compact context budget: SHA/PR/check first, failing step next, bounded raw logs only where they determine the conclusion."
write_workspace_file "$CI_WORKSPACE/TOOLS.md" \
  "# Tools" \
  "" \
  "Repository: ./repo" \
  "Use gh, git and focused validation without publishing changes. Host exec uses OpenClaw auto-review; elevated tools stay disabled. Keep exact failure output raw."
if [[ ! -f "$CI_WORKSPACE/MEMORY.md" ]]; then
  write_workspace_file "$CI_WORKSPACE/MEMORY.md" \
    "# Durable CI Memory" \
    "" \
    "Store only stable, verified CI/release diagnostics and recurring infrastructure lessons. Never store secrets, credentials, transient run state, branch heads or current PR status here."
fi
mkdir -p "$CI_WORKSPACE/memory"

REVIEW_SKILLS=(
  levyra-pr-review
  levyra-security-review
  levyra-release-check
  levyra-context-efficiency
  levyra-real-engineering
  levyra-player
  levyra-extractor
  levyra-database
  levyra-compose
  levyra-android-performance
  levyra-r8-proguard
  levyra-android-intent-security
  levyra-design-taste
  levyra-desktop
  levyra-ci-workflows
)
CI_SKILLS=(
  levyra-ci-workflows
  levyra-release-check
  levyra-pr-review
  levyra-security-review
  levyra-context-efficiency
)

for skill in "${REVIEW_SKILLS[@]}"; do
  write_skill_bridge "$REVIEW_WORKSPACE" "$skill"
done
for skill in "${CI_SKILLS[@]}"; do
  write_skill_bridge "$CI_WORKSPACE" "$skill"
done

ensure_agent levyra-reviewer "$REVIEW_WORKSPACE"
ensure_agent levyra-ci "$CI_WORKSPACE"

configure_agent_skills levyra-reviewer "${REVIEW_SKILLS[@]}"
configure_agent_skills levyra-ci "${CI_SKILLS[@]}"
configure_evidence_agent levyra-reviewer
configure_evidence_agent levyra-ci
merge_primary_subagents "$PRIMARY_AGENT"

if [[ "$ENABLE_ACTIVE_MEMORY" == "1" ]]; then
  openclaw config set "agents.entries.$PRIMARY_AGENT.memory.search.rememberAcrossConversations" true --strict-json
  openclaw config set plugins.entries.active-memory.enabled true --strict-json
  openclaw config set plugins.entries.active-memory.config.enabled true --strict-json
  openclaw config set plugins.entries.active-memory.config.mode '"escalate"' --strict-json
  openclaw config set plugins.entries.active-memory.config.queryMode '"recent"' --strict-json
  openclaw config set plugins.entries.active-memory.config.promptStyle '"precision-heavy"' --strict-json
  openclaw config set plugins.entries.active-memory.config.maxSummaryChars 240 --strict-json
  openclaw config set plugins.entries.active-memory.config.persistTranscripts false --strict-json
fi

if [[ "$ENABLE_DREAMING" == "1" ]]; then
  openclaw config set plugins.entries.memory-core.config.dreaming.enabled true --strict-json
fi

if [[ "$INSTALL_CRON" == "1" ]]; then
  ensure_cron levyra-ci
fi

openclaw config validate
openclaw doctor
openclaw memory status --agent "$PRIMARY_AGENT" --index
openclaw gateway status --require-rpc
openclaw agents list --bindings
openclaw cron list --agent levyra-ci

echo "OpenClaw Levyra profile ready."
echo "Primary: $PRIMARY_AGENT"
echo "Reviewer: levyra-reviewer"
echo "CI: levyra-ci"
echo "Primary repository: $PRIMARY_REPO"
