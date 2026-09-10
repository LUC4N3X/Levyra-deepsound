from __future__ import annotations

import unittest
from pathlib import Path

from scripts.agent_skill_router import MAX_ACTIVE_SKILLS, context_for, route_prompt

ROOT = Path(__file__).resolve().parents[2]


class AgentTokenBudgetTest(unittest.TestCase):
    @staticmethod
    def selected(prompt: str) -> tuple[str, ...]:
        return tuple(skill for skill, _ in route_prompt(prompt))

    def test_service_skill_bodies_stay_compact(self) -> None:
        limits = {
            ".agents/skills/levyra-mode/SKILL.md": 4096,
            ".agents/skills/levyra-humanizer/SKILL.md": 4096,
            ".agents/skills/levyra-context-efficiency/SKILL.md": 6144,
            ".agents/claude/hooks/user-prompt-submit.sh": 3072,
        }
        for relative_path, max_bytes in limits.items():
            with self.subTest(path=relative_path):
                self.assertLessEqual((ROOT / relative_path).stat().st_size, max_bytes)

    def test_routed_prompt_context_stays_small(self) -> None:
        prompts = (
            "VAI sistema il playback crash",
            "Build and validate the release APK",
            "Investigate a cross-domain architecture issue across subsystems",
            "Open a pull request for this branch",
            "VAI fix playback crash, test release APK, review and open PR",
        )
        for prompt in prompts:
            with self.subTest(prompt=prompt):
                self.assertLessEqual(len(context_for(prompt).encode("utf-8")), 1024)
                self.assertLessEqual(len(self.selected(prompt)), MAX_ACTIVE_SKILLS)

    def test_unmatched_prompt_adds_no_router_context(self) -> None:
        self.assertEqual("", context_for("Modifica il README"))

    def test_small_tasks_do_not_load_efficiency_layers(self) -> None:
        cases = {
            "Modifica il README": (),
            "Analyze this function": (),
            "Implementa una piccola modifica Compose": ("levyra-compose",),
        }
        for prompt, expected in cases.items():
            with self.subTest(prompt=prompt):
                selected = self.selected(prompt)
                self.assertEqual(expected, selected)
                self.assertNotIn("levyra-context-efficiency", selected)
                self.assertNotIn("levyra-mode", selected)

    def test_owner_execution_cues_do_not_consume_skill_budget(self) -> None:
        self.assertEqual(("levyra-player",), self.selected("VAI sistema il playback crash"))
        cases = {
            "Ok intervieni sul player": ("levyra-player",),
            "Dai vai col fix": (),
            "Ora procedi con la PR": ("levyra-pr-review",),
            "Adesso intervieni": (),
        }
        for prompt, expected in cases.items():
            with self.subTest(prompt=prompt):
                selected = self.selected(prompt)
                self.assertEqual(expected, selected)
                self.assertNotIn("levyra-mode", selected)

    def test_owner_execution_cues_are_core_contract_behavior(self) -> None:
        contract = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
        self.assertIn("Explicit owner execution cues", contract)
        self.assertIn("not a reason to load `levyra-mode`", contract)
        self.assertIn("maximum of two skill bodies active for the current phase", contract)

    def test_context_efficiency_is_core_not_an_automatic_skill(self) -> None:
        expected = {
            "Build and validate the release APK": ("levyra-release-check",),
            "Inspect repository logs for a crash": (),
            "Review CodeRabbit findings on this PR": ("levyra-pr-review",),
        }
        for prompt, routed in expected.items():
            with self.subTest(prompt=prompt):
                selected = self.selected(prompt)
                self.assertEqual(routed, selected)
                self.assertNotIn("levyra-context-efficiency", selected)

        contract = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
        self.assertIn("replace automatic loading of `levyra-context-efficiency`", contract)

    def test_implementation_phase_defers_terminal_skills(self) -> None:
        selected = self.selected("VAI fix playback crash, test release APK, review and open PR")
        self.assertEqual(("levyra-player",), selected)
        for deferred in (
            "levyra-pr-review",
            "levyra-release-check",
            "levyra-humanizer",
            "levyra-mode",
            "levyra-context-efficiency",
        ):
            with self.subTest(skill=deferred):
                self.assertNotIn(deferred, selected)


if __name__ == "__main__":
    unittest.main()
