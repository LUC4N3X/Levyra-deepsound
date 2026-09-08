from __future__ import annotations

import unittest
from pathlib import Path

from scripts.agent_skill_router import context_for, route_prompt

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
        )
        for prompt in prompts:
            with self.subTest(prompt=prompt):
                self.assertLessEqual(len(context_for(prompt).encode("utf-8")), 1024)

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

    def test_explicit_owner_mode_keeps_engineering_routes(self) -> None:
        selected = self.selected("VAI sistema il playback crash")
        self.assertEqual(
            ("levyra-mode", "levyra-real-engineering", "levyra-player"),
            selected,
        )
        self.assertNotIn("levyra-context-efficiency", selected)

    def test_high_volume_work_still_gets_context_efficiency(self) -> None:
        for prompt in (
            "Build and validate the release APK",
            "Inspect repository logs for a crash",
            "Review CodeRabbit findings on this PR",
        ):
            with self.subTest(prompt=prompt):
                self.assertIn("levyra-context-efficiency", self.selected(prompt))


if __name__ == "__main__":
    unittest.main()
