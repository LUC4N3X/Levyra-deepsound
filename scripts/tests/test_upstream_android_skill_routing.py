#!/usr/bin/env python3
"""Focused checks for phase-scoped external Android skill guidance."""

from __future__ import annotations

import unittest

from scripts.agent_skill_router import MAX_ACTIVE_SKILLS, context_for, route_prompt


class UpstreamAndroidRoutingTests(unittest.TestCase):
    def test_material3_android_ui_routes_design_and_later_review(self) -> None:
        prompt = "Rifai la grafica Material 3 Expressive della home Android"
        self.assertLessEqual(len(route_prompt(prompt)), MAX_ACTIVE_SKILLS)
        result = context_for(prompt)
        self.assertIn("material-3 SKILL.md", result)
        self.assertIn("kotlin-footguns", result)
        self.assertIn("Later UI compliance phase", result)

    def test_media3_playback_routes_kotlin_review(self) -> None:
        result = context_for("Fix Media3 playback queue bug")
        self.assertIn("kotlin-footguns", result)
        self.assertNotIn("Later UI compliance phase", result)

    def test_desktop_only_ui_has_no_android_material3_review(self) -> None:
        result = context_for("Improve Windows Desktop Compose Multiplatform UI")
        self.assertNotIn("Later UI compliance phase", result)

    def test_unrelated_tasks_do_not_load_upstream_skills(self) -> None:
        result = context_for("Write an ordinary pull request description")
        self.assertNotIn("Later UI compliance phase", result)
        self.assertNotIn("Later Kotlin review phase", result)


if __name__ == "__main__":
    unittest.main()
