from __future__ import annotations

import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SETUP = ROOT / "scripts" / "setup-openclaw-levyra.sh"
SKILL = ROOT / ".agents" / "skills" / "levyra-openclaw-orchestrator" / "SKILL.md"


class OpenClawLevyraSetupTest(unittest.TestCase):
    def test_shell_syntax(self) -> None:
        subprocess.run(["bash", "-n", str(SETUP)], check=True, cwd=ROOT)

    def test_specialized_agents_and_boundaries(self) -> None:
        setup = SETUP.read_text(encoding="utf-8")

        for term in (
            "levyra-reviewer",
            "levyra-ci",
            "tools.exec.mode",
            "strictInlineEval",
            "tools.elevated.enabled",
            "merge_primary_subagents",
            "subagents.requireAgentId",
            "subagents.delegationMode",
            "--light-context",
            "--no-deliver",
            "memory-core.config.dreaming.enabled",
        ):
            self.assertIn(term, setup)

        for command in ("git push", "gh pr merge", "gh release create"):
            self.assertNotIn(command, setup)

    def test_openclaw_2026_7_uses_agents_list_schema(self) -> None:
        setup = SETUP.read_text(encoding="utf-8")

        self.assertIn("openclaw config get agents.list --json", setup)
        self.assertIn('agents.list[$index].$suffix', setup)
        self.assertIn('agents.list[$index].subagents.allowAgents', setup)
        self.assertIn('agents.list[$index].memorySearch.rememberAcrossConversations', setup)
        self.assertNotIn("agents.entries.$", setup)
        self.assertNotIn("memory.search.rememberAcrossConversations", setup)

    def test_invalid_config_recovers_only_from_valid_backup(self) -> None:
        setup = SETUP.read_text(encoding="utf-8")

        for term in (
            "recover_invalid_config",
            "OPENCLAW_CONFIG_PATH=\"$CONFIG_BACKUP_PATH\" openclaw config validate",
            "openclaw.json",
            ".invalid-$(date +%Y%m%d-%H%M%S)",
            "Restored the last valid OpenClaw config backup",
            "OpenClaw backup config is also invalid",
        ):
            self.assertIn(term, setup)

        self.assertLess(
            setup.index("recover_invalid_config\n"),
            setup.index('PRIMARY_AGENT="$(choose_primary_agent)"'),
        )

    def test_primary_agent_is_preserved_and_receives_skill_bridges(self) -> None:
        setup = SETUP.read_text(encoding="utf-8")

        self.assertIn("has_agent levyra-worker", setup)
        self.assertIn("has_agent levyra", setup)
        self.assertIn("LEVYRA_OPENCLAW_AGENT", setup)
        self.assertIn('"$PRIMARY_REPO"/.agents/skills/*/SKILL.md', setup)
        self.assertIn('"$PRIMARY_WORKSPACE/MEMORY.md"', setup)
        self.assertIn("## Levyra multi-agent profile", setup)
        self.assertIn("levyra-openclaw-orchestrator", setup)

    def test_active_memory_is_targeted_through_primary_agent_recall(self) -> None:
        setup = SETUP.read_text(encoding="utf-8")

        for term in (
            "memorySearch.rememberAcrossConversations",
            "plugins.entries.active-memory.enabled",
            "plugins.entries.active-memory.config.mode",
            "plugins.entries.active-memory.config.queryMode",
            "plugins.entries.active-memory.config.promptStyle",
            "plugins.entries.active-memory.config.persistTranscripts false",
            "escalate",
            "recent",
            "precision-heavy",
        ):
            self.assertIn(term, setup)

    def test_cron_scope_failure_is_non_fatal(self) -> None:
        setup = SETUP.read_text(encoding="utf-8")

        self.assertIn("Cron inspection unavailable", setup)
        self.assertIn("may lack operator.admin scope", setup)
        self.assertIn("openclaw cron list --agent levyra-ci || true", setup)

    def test_evidence_workspaces_reference_canonical_repo_paths(self) -> None:
        setup = SETUP.read_text(encoding="utf-8")

        self.assertIn("repo/docs/ai/AI_ENGINEERING_GUARDRAILS.md", setup)
        self.assertIn("repo/.github/AGENTS.md", setup)

    def test_orchestrator_uses_compact_independent_handoffs(self) -> None:
        skill = SKILL.read_text(encoding="utf-8")

        for term in (
            "levyra-context-efficiency",
            "levyra-reviewer",
            "levyra-ci",
            "fresh,\n   bounded handoff",
            "code-review",
            "Memory is evidence, not a second source of truth",
        ):
            self.assertIn(term, skill)


if __name__ == "__main__":
    unittest.main()
