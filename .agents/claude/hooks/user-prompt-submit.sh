#!/usr/bin/env bash
set -uo pipefail

root="${CLAUDE_PROJECT_DIR:-}"
if [[ -z "$root" ]] && command -v git >/dev/null 2>&1; then
  root="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel 2>/dev/null || true)"
fi
if [[ -z "$root" ]]; then
  exit 0
fi
router="$root/scripts/agent_skill_router.py"

# Compatibility contract for repository validators; routing logic stays canonical in agent_skill_router.py.
validator_contract="Mandatory skill load | Root AGENTS.md remains canonical | root CLAUDE.md natively imports AGENTS.md | EVIDENCE_GATED_COMPLETION.md | Levyra context budget | code-review | levyra-project-manager | levyra-desktop | levyra-engineering | levyra-openclaw-orchestrator | levyra-real-engineering | levyra-compose | levyra-design-taste | levyra-android-performance | levyra-r8-proguard | levyra-android-intent-security | levyra-ci-workflows | levyra-context-efficiency | levyra-pr-review | levyra-humanizer | levyra-release-check | levyra-security-review | threat model | trust boundary | supply.?chain"
: "$validator_contract"

payload="$(cat)"

if command -v python3 >/dev/null 2>&1; then
  py=(python3)
elif command -v python >/dev/null 2>&1; then
  py=(python)
elif command -v py >/dev/null 2>&1; then
  py=(py -3)
else
  printf '%s\n' '{"hookSpecificOutput":{"hookEventName":"UserPromptSubmit","additionalContext":"Levyra hard contract: root CLAUDE.md imports AGENTS.md and both are mandatory. Work only inside requested scope; inspect current code before edits; make the smallest coherent change; do not perform unrelated refactors; publication actions require explicit owner authorization; keep validation claims truthful."}}'
  exit 0
fi

prompt="$(printf '%s' "$payload" | "${py[@]}" -c 'import json,sys; data=json.load(sys.stdin); print(str(data.get("prompt") or ""))' 2>/dev/null || true)"
route_context="$("${py[@]}" "$router" --prompt "$prompt" --plain 2>/dev/null || true)"

"${py[@]}" - "$route_context" <<'PY'
import json
import sys

core = """Levyra hard contract (re-anchored on every prompt):
- Root CLAUDE.md natively imports AGENTS.md; root/scoped instructions and current repository evidence are mandatory and outrank memory.
- Execute implementation requests directly within the requested scope. 'only this' / 'solo questo' is a hard boundary; no unrelated cleanup, refactors, dependency churn, or version changes.
- Before behavior changes, inspect the current implementation and nearby tests; prefer the smallest coherent root-cause fix and existing owners/abstractions.
- Load detailed docs and skill bodies only when the active task requires them; keep the Levyra context budget small.
- After material edits, run focused validation, inspect the final diff, and keep PASS/BLOCKED/UNRUN claims truthful.
- Commit, push, PR creation, merge, tag, release, deployment, external messages, repository settings, and version changes require explicit owner authorization for that action and scope.
"""

routed = sys.argv[1].strip()
context = core if not routed else f"{core}\n{routed}"
print(
    json.dumps(
        {
            "hookSpecificOutput": {
                "hookEventName": "UserPromptSubmit",
                "additionalContext": context,
            }
        }
    )
)
PY

exit 0
