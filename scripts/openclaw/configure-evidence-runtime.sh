#!/usr/bin/env bash
set -euo pipefail

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

effective_model() {
  local index="$1"
  openclaw config get agents --json | python3 -c '
import json, sys
index = int(sys.argv[1])
value = json.load(sys.stdin)
agents = value.get("list", value.get("agents", value.get("items", []))) if isinstance(value, dict) else []
defaults = value.get("defaults", {}) if isinstance(value, dict) else {}

def primary(model):
    if isinstance(model, str):
        return model
    if isinstance(model, dict):
        return model.get("primary")
    return None

if not isinstance(agents, list) or index >= len(agents) or not isinstance(agents[index], dict):
    raise SystemExit(1)
model = primary(agents[index].get("model")) or primary(defaults.get("model"))
if not isinstance(model, str) or not model:
    raise SystemExit(1)
print(model)
' "$index"
}

configure_agent() {
  local agent="$1"
  local index model runtime_json
  index="$(agent_index "$agent")"
  model="$(effective_model "$index")"
  runtime_json="$(python3 -c 'import json, sys; print(json.dumps({sys.argv[1]: {"agentRuntime": {"id": "openclaw"}}}, separators=(",", ":")))' "$model")"

  openclaw config set "agents.list[$index].models" "$runtime_json" --strict-json --merge
  openclaw config set "agents.list[$index].tools.exec.mode" '"allowlist"' --strict-json
}

configure_agent levyra-reviewer
configure_agent levyra-ci
openclaw config validate
