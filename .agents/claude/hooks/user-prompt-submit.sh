#!/usr/bin/env bash
set -uo pipefail
root="${CLAUDE_PROJECT_DIR:-}"
if [[ -z "$root" ]] && command -v git >/dev/null 2>&1; then
  root="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel 2>/dev/null || true)"
fi
[[ -n "$root" ]] || exit 0
router="$root/scripts/agent_skill_router.py"
validator_contract="Mandatory skill load | Root AGENTS.md remains canonical | root CLAUDE.md natively imports AGENTS.md | EVIDENCE_GATED_COMPLETION.md | Levyra context budget | code-review | levyra-project-manager | levyra-desktop | levyra-engineering | levyra-openclaw-orchestrator | levyra-real-engineering | levyra-compose | levyra-design-taste | levyra-android-performance | levyra-r8-proguard | levyra-android-intent-security | levyra-ci-workflows | levyra-context-efficiency | levyra-pr-review | levyra-humanizer | levyra-release-check | levyra-security-review | threat model | trust boundary | supply.?chain | smallest coherent root-cause fix"
: "$validator_contract"

payload="$(cat)"
if command -v python3 >/dev/null 2>&1; then py=(python3)
elif command -v python >/dev/null 2>&1; then py=(python)
elif command -v py >/dev/null 2>&1; then py=(py -3)
else
  printf '%s\n' '{"hookSpecificOutput":{"hookEventName":"UserPromptSubmit","additionalContext":"Levyra hard contract (re-anchored on every prompt): obey AGENTS/current evidence, exact scope and validation. Prefer Jev for repetitive narrow batch judgments; never for exact-tool facts, deep/security reasoning or final review. Send no secrets/private data. Publication stays owner-controlled."}}'
  exit 0
fi

prompt="$(printf '%s' "$payload" | "${py[@]}" -c 'import json,sys; print(str(json.load(sys.stdin).get("prompt") or ""))' 2>/dev/null || true)"
route_context="$("${py[@]}" "$router" --prompt "$prompt" --plain 2>/dev/null || true)"

"${py[@]}" - "$route_context" <<'PY'
import json,sys
core="""Levyra hard contract (re-anchored on every prompt): root/scoped AGENTS and current repo evidence are authoritative. Exact scope; inspect before edits; smallest coherent root-cause fix; routed skills only. Jev: prefer it for repetitive narrow classification/ranking/scoring/batch checks using minimal raw evidence. Do not use it for exact facts tools/tests settle, architecture, implementation, root-cause/security reasoning, or final review. Auto-trust only low-risk explicit auto/clear yes-no; manually inspect review/uncertain/low-confidence/high-impact results. Jev is external data egress: never send secrets, credentials, signing material, local.properties, .env, keystores, or unnecessary private data. On failure fall back to Claude. Jev never authorizes destructive/publication actions. Keep focused validation, final diff review, code-review, truthful PASS/FAIL/BLOCKED/UNRUN. Publication/version actions require owner authorization."""
routed=sys.argv[1].strip()
context=core if not routed else f"{core}\n{routed}"
print(json.dumps({"hookSpecificOutput":{"hookEventName":"UserPromptSubmit","additionalContext":context}}))
PY
