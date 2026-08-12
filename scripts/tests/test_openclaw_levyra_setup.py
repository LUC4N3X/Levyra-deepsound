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
            "agents.entries.$PRIMARY_AGENT.subagents.allowAgents",
            "tools.exec.mode",
            "strictInlineEval",
            "tools.elevated.enabled",
            "--light-context",
            "--no-deliver",
            "memory-core.config.dreaming.enabled",
        ):
            self.assertIn(term, setup)

        for command in ("git push", "gh pr merge", "gh release create"):
            self.assertNotIn(command, setup)

    def test_primary_agent_is_preserved_and_receives_skill_bridges(self) -> None:
        setup = SETUP.read_text(encoding="utf-8")

        self.assertIn("has_agent levyra-worker", setup)
        self.assertIn("has_agent levyra", setup)
        self.assertIn("LEVYRA_OPENCLAW_AGENT", setup)
        self.assertIn('"$PRIMARY_REPO"/.agents/skills/*/SKILL.md', setup)
        self.assertIn('"$PRIMARY_WORKSPACE/MEMORY.md"', setup)

    def test_evidence_workspaces_reference_canonical_repo_paths(self) -> None:
        setup = SETUP.read_text(encoding="utf-8")

        self.assertIn("repo/docs/ai/AI_ENGINEERING_GUARDRAILS.md", setup)
        self.assertIn("repo/.github/AGENTS.md", setup)
        self.assertNotIn("files, docs/ai/AI_ENGINEERING_GUARDRAILS.md", setup)

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
