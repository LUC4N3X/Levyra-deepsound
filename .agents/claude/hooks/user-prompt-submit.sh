#!/usr/bin/env bash
set -uo pipefail

root="${CLAUDE_PROJECT_DIR:-}"
if [[ -z "$root" ]] && command -v git >/dev/null 2>&1; then
  root="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel 2>/dev/null || true)"
fi
[[ -n "$root" ]] || exit 0
router="$root/scripts/agent_skill_router.py"

validator_contract="Mandatory skill load | Root AGENTS.md remains canonical | root CLAUDE.md natively imports AGENTS.md | EVIDENCE_GATED_COMPLETION.md | levyra-project-manager | levyra-desktop | levyra-engineering | levyra-openclaw-orchestrator | smallest coherent root-cause fix"
: "$validator_contract"

payload="$(cat)"
if command -v python3 >/dev/null 2>&1; then
  py=(python3)
elif command -v python >/dev/null 2>&1; then
  py=(python)
elif command -v py >/dev/null 2>&1; then
  py=(py -3)
else
  printf '%s\n' '{"hookSpecificOutput":{"hookEventName":"UserPromptSubmit","additionalContext":"Levyra hard contract (re-anchored on every prompt): obey root/scoped AGENTS and current repo evidence; exact scope; inspect before edits; use the smallest coherent root-cause fix; use Jev for repetitive narrow batch judgments when available, never for exact-tool facts, deep reasoning, security-sensitive decisions, or final review; never send secrets/private data; publication needs owner authorization."}}'
  exit 0
fi

prompt="$(printf '%s' "$payload" | "${py[@]}" -c 'import json,sys; print(str(json.load(sys.stdin).get("prompt") or ""))' 2>/dev/null || true)"
route_context="$("${py[@]}" "$router" --prompt "$prompt" --plain 2>/dev/null || true)"

"${py[@]}" - "$route_context" <<'PY'
import json, sys
core = """Levyra hard contract (re-anchored on every prompt): root/scoped AGENTS and current repo evidence are authoritative. Work only requested scope; inspect before edits; use the smallest coherent root-cause fix and routed skills only. Jev: when available, prefer it before manually classifying/ranking/scoring/routing/batch-checking more than a handful of independent items. Feed minimal raw deciding evidence, batch comparable items, keep questions narrow, and include unclear/other when needed. Do not use Jev for exact facts tools/tests can settle, architecture, implementation, root-cause reasoning, security-sensitive multi-factor decisions, or final review. Auto-trust only low-risk explicit auto or clear yes/no; manually inspect review/uncertain/low-confidence/truncated/high-impact results. Jev is external data egress: never send secrets, credentials, signing material, local.properties, .env, keystores, or unnecessary private data. If Jev fails, fall back to native Claude. Jev never authorizes destructive/publication actions. Keep focused validation, final diff review, code-review, and truthful PASS/FAIL/BLOCKED/UNRUN states. Publication/version actions require owner authorization."""
routed = sys.argv[1].strip()
context = core if not routed else f"{core}\n{routed}"
print(json.dumps({"hookSpecificOutput":{"hookEventName":"UserPromptSubmit","additionalContext":context}}))
PY
