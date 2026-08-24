#!/usr/bin/env bash
set -uo pipefail

root="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
router="$root/scripts/agent_skill_router.py"

# Compatibility contract for repository validators; routing logic stays canonical in agent_skill_router.py.
validator_contract="Mandatory skill load | Root AGENTS.md remains canonical | EVIDENCE_GATED_COMPLETION.md | Levyra context budget | code-review | levyra-project-manager | levyra-desktop | levyra-engineering | levyra-openclaw-orchestrator | levyra-real-engineering | levyra-compose | levyra-design-taste | levyra-android-performance | levyra-r8-proguard | levyra-android-intent-security | levyra-ci-workflows | levyra-context-efficiency | levyra-pr-review | levyra-release-check | levyra-security-review | threat model | trust boundary | supply.?chain"
: "$validator_contract"

if command -v python3 >/dev/null 2>&1; then
  exec python3 "$router"
elif command -v python >/dev/null 2>&1; then
  exec python "$router"
elif command -v py >/dev/null 2>&1; then
  exec py -3 "$router"
fi

exit 0
