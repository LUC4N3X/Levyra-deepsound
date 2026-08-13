#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_DIR="${OPENCLAW_STATE_DIR:-$HOME/.openclaw}"
PRIMARY_WORKSPACE="${LEVYRA_OPENCLAW_WORKSPACE:-$STATE_DIR/workspace-levyra}"
REVIEW_WORKSPACE="${LEVYRA_REVIEW_WORKSPACE:-$STATE_DIR/workspace-levyra-reviewer}"
CI_WORKSPACE="${LEVYRA_CI_WORKSPACE:-$STATE_DIR/workspace-levyra-ci}"
BASE_SETUP="$ROOT/scripts/setup-openclaw-levyra.sh"
WORKER_SOURCE="$ROOT/scripts/openclaw/levyra-worker"
EVIDENCE_SOURCE="$ROOT/scripts/openclaw/levyra-evidence"

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
    echo "No Levyra primary agent found. Run the base OpenClaw bootstrap first." >&2
    exit 1
  fi
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

set_agent_json() {
  local agent="$1"
  local suffix="$2"
  local value="$3"
  local index
  index="$(agent_index "$agent")"
  openclaw config set "agents.list[$index].$suffix" "$value" --strict-json
}

install_tool() {
  local source="$1"
  local workspace="$2"
  local name="$3"
  local target="$workspace/bin/$name"
  mkdir -p "$workspace/bin"
  cp "$source" "$target"
  chmod 700 "$target"
  printf '%s\n' "$target"
}

has_allowlist_pattern() {
  local agent="$1"
  local pattern="$2"
  openclaw approvals get --json | python3 -c '
import json, sys
agent, pattern = sys.argv[1], sys.argv[2]
value = json.load(sys.stdin)
entries = value.get("file", {}).get("agents", {}).get(agent, {}).get("allowlist", [])
raise SystemExit(0 if any(isinstance(item, dict) and item.get("pattern") == pattern for item in entries) else 1)
' "$agent" "$pattern"
}

ensure_allowlist_pattern() {
  local agent="$1"
  local pattern="$2"
  if ! has_allowlist_pattern "$agent" "$pattern"; then
    openclaw approvals allowlist add --agent "$agent" "$pattern"
  fi
}

remove_allowlist_pattern() {
  local agent="$1"
  local pattern="$2"
  if has_allowlist_pattern "$agent" "$pattern"; then
    openclaw approvals allowlist remove --agent "$agent" "$pattern"
  fi
}

configure_primary_exec() {
  local agent="$1"
  set_agent_json "$agent" tools.exec.host '"gateway"'
  set_agent_json "$agent" tools.exec.mode '"allowlist"'
  set_agent_json "$agent" tools.exec.strictInlineEval true
  set_agent_json "$agent" tools.fs.workspaceOnly true
  set_agent_json "$agent" tools.elevated.enabled false
}

configure_evidence_exec() {
  local agent="$1"
  set_agent_json "$agent" tools.allow '["read","exec","process"]'
  set_agent_json "$agent" tools.deny '["write","edit","apply_patch","browser","gateway","cron"]'
  set_agent_json "$agent" tools.exec.host '"gateway"'
  set_agent_json "$agent" tools.exec.mode '"auto"'
  set_agent_json "$agent" tools.exec.strictInlineEval true
  set_agent_json "$agent" tools.fs.workspaceOnly true
  set_agent_json "$agent" tools.elevated.enabled false
}

append_policy() {
  local path="$1"
  local heading="$2"
  shift 2
  touch "$path"
  if ! grep -Fq "$heading" "$path"; then
    printf '\n%s\n\n' "$heading" >> "$path"
    printf '%s\n' "$@" >> "$path"
  fi
}

upsert_policy() {
  local path="$1"
  local heading="$2"
  shift 2
  python3 - "$path" "$heading" "$@" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
heading = sys.argv[2]
body = list(sys.argv[3:])
text = path.read_text(encoding="utf-8") if path.exists() else ""
lines = text.splitlines()
section = [heading, "", *body]
start = next((index for index, line in enumerate(lines) if line == heading), None)
if start is None:
    if lines and lines[-1] != "":
        lines.append("")
    lines.extend(section)
else:
    end = len(lines)
    for index in range(start + 1, len(lines)):
        if lines[index].startswith("## "):
            end = index
            break
    lines[start:end] = section + ([""] if end < len(lines) else [])
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
PY
}

require_command openclaw
require_command python3
require_command git
require_command gh

if [[ "${LEVYRA_PR_ONLY_SKIP_BASE_SETUP:-0}" != "1" ]]; then
  bash "$BASE_SETUP"
fi

PRIMARY_AGENT="$(choose_primary_agent)"
has_agent levyra-reviewer || { echo "Missing levyra-reviewer agent" >&2; exit 1; }
has_agent levyra-ci || { echo "Missing levyra-ci agent" >&2; exit 1; }

PRIMARY_WORKER="$(install_tool "$WORKER_SOURCE" "$PRIMARY_WORKSPACE" levyra-worker)"
install_tool "$EVIDENCE_SOURCE" "$PRIMARY_WORKSPACE" levyra-evidence >/dev/null
REVIEW_EVIDENCE="$(install_tool "$EVIDENCE_SOURCE" "$REVIEW_WORKSPACE" levyra-evidence)"
CI_EVIDENCE="$(install_tool "$EVIDENCE_SOURCE" "$CI_WORKSPACE" levyra-evidence)"

configure_primary_exec "$PRIMARY_AGENT"
configure_evidence_exec levyra-reviewer
configure_evidence_exec levyra-ci

for agent in "$PRIMARY_AGENT" levyra-reviewer levyra-ci; do
  for pattern in git gh /usr/bin/git /usr/bin/gh /bin/git /bin/gh; do
    remove_allowlist_pattern "$agent" "$pattern"
  done
done
remove_allowlist_pattern "$PRIMARY_AGENT" /usr/local/bin/levyra-worker
remove_allowlist_pattern "$PRIMARY_AGENT" /usr/local/bin/levyra-release-main

ensure_allowlist_pattern "$PRIMARY_AGENT" "$PRIMARY_WORKER"
ensure_allowlist_pattern levyra-reviewer "$REVIEW_EVIDENCE"
ensure_allowlist_pattern levyra-ci "$CI_EVIDENCE"

append_policy "$PRIMARY_WORKSPACE/AGENTS.md" "## Levyra PR-only publication policy" \
  "For Levyra implementation work, start or switch to a non-main work branch with \`./bin/levyra-worker start <branch>\` before editing." \
  "Use normal workspace edit tools for code changes and \`./bin/levyra-worker\` for Git, validation, push, and PR operations." \
  "The only publication path is: work branch -> commit -> push that branch -> open a Pull Request against main." \
  "Never push directly to main/master, merge a PR, create a tag or release, deploy, or bypass the wrapper with raw Git/GitHub shell commands."

upsert_policy "$PRIMARY_WORKSPACE/AGENTS.md" "## Levyra specialist evidence paths" \
  "When delegating to levyra-reviewer, require the absolute wrapper \`$REVIEW_EVIDENCE\`; do not hand the child \`./bin/levyra-evidence\`." \
  "When delegating to levyra-ci, require the absolute wrapper \`$CI_EVIDENCE\`; do not hand the child \`./bin/levyra-evidence\`." \
  "A spawned specialist may inherit the requester working directory, so relative wrapper paths are not valid evidence roots."

upsert_policy "$REVIEW_WORKSPACE/AGENTS.md" "## Levyra evidence command policy" \
  "Use \`$REVIEW_EVIDENCE\` for Git and GitHub evidence, including show/diff/fetch and exact-SHA Actions queries." \
  "Use that absolute wrapper path even when spawnedCwd points at the requester workspace; do not invoke \`./bin/levyra-evidence\`." \
  "Do not use raw Git or gh commands and do not modify the checkout beyond fetch metadata."

upsert_policy "$CI_WORKSPACE/AGENTS.md" "## Levyra evidence command policy" \
  "Use \`$CI_EVIDENCE\` for Git and GitHub evidence, especially \`actions-sha <sha>\` or \`run-list <sha>\` before concluding that CI is absent." \
  "Use that absolute wrapper path even when spawnedCwd points at the requester workspace; do not invoke \`./bin/levyra-evidence\`." \
  "Do not use raw Git or gh commands and do not modify source, workflows, or repository state."

openclaw config validate

echo "OpenClaw Levyra PR-only policy ready."
echo "Primary agent: $PRIMARY_AGENT"
echo "Primary worker: $PRIMARY_WORKER"
echo "Reviewer evidence: $REVIEW_EVIDENCE"
echo "CI evidence: $CI_EVIDENCE"
echo "Publication boundary: branch + Pull Request only; no direct main push, merge, release, or deploy."
