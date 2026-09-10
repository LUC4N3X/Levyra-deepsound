from __future__ import annotations

import json
import subprocess
import unittest
from pathlib import Path

from scripts.ai_quality_gate import find_bash

ROOT = Path(__file__).resolve().parents[2]
CLAUDE_ROOT = ROOT / ".agents" / "claude"
HOOK = CLAUDE_ROOT / "hooks" / "user-prompt-submit.sh"


def route(prompt: str) -> str:
    bash = find_bash()
    if not bash:
        raise unittest.SkipTest("Bash is required for Claude prompt routing test")
    result = subprocess.run(
        [bash, str(HOOK)],
        input=json.dumps({"prompt": prompt}),
        text=True,
        capture_output=True,
        check=True,
        cwd=ROOT,
    )
    payload = json.loads(result.stdout)
    return payload["hookSpecificOutput"]["additionalContext"]


class ClaudePromptRoutingTest(unittest.TestCase):
    def test_claude_context_budget_stays_compact(self) -> None:
        settings = json.loads((CLAUDE_ROOT / "settings.json").read_text(encoding="utf-8"))
        self.assertEqual(0.01, settings["skillListingBudgetFraction"])
        self.assertEqual(768, settings["maxSkillDescriptionChars"])
        self.assertFalse(settings["includeGitInstructions"])

        instructions = (CLAUDE_ROOT / "CLAUDE.md").read_text(encoding="utf-8")
        self.assertLessEqual(len(instructions.encode("utf-8")), 7000)
        self.assertNotIn("@../AGENTS.md", instructions)
        self.assertNotIn("@../docs/ai/", instructions)
        self.assertIn("Subagent token discipline", instructions)

        developer = (CLAUDE_ROOT / "agents" / "levyra-android-developer.md").read_text(encoding="utf-8")
        self.assertIn("tools: Read, Grep, Glob, Edit, Write, Bash, Skill", developer)
        self.assertIn("effort: high", developer)
        self.assertIn("model: inherit", developer)
        self.assertNotIn("Read `.claude/CLAUDE.md`", developer)

    def test_design_activation_phrases(self) -> None:
        for prompt in (
            "Make the Now Playing screen more premium",
            "premium",
            "modern",
            "clean",
            "cinematic",
            "less generic",
        ):
            with self.subTest(prompt=prompt):
                self.assertIn("levyra-design-taste", route(prompt))

    def test_design_words_do_not_trigger_in_unrelated_contexts(self) -> None:
        for prompt in (
            "Upgrade to a modern Kotlin version",
            "Clean Gradle build outputs",
            "Integrate a premium subscription API",
        ):
            with self.subTest(prompt=prompt):
                self.assertNotIn("levyra-design-taste", route(prompt))

    def test_compose_jank_uses_two_domain_skills_only(self) -> None:
        context = route("Compose jank issue")
        self.assertIn("levyra-compose", context)
        self.assertIn("levyra-android-performance", context)
        self.assertNotIn("levyra-real-engineering", context)
        self.assertIn("max 2", context)

    def test_memory_terms_use_performance_without_generic_workflow(self) -> None:
        for prompt in (
            "RAM keeps climbing during playback",
            "Investigate native memory growth",
            "OOM after 20 minutes of music",
            "Check PSS RSS and dumpsys meminfo",
        ):
            with self.subTest(prompt=prompt):
                context = route(prompt)
                self.assertIn("levyra-android-performance", context)
                self.assertNotIn("levyra-real-engineering", context)

    def test_playback_allocation_growth_uses_player_and_performance(self) -> None:
        context = route("Player allocations grow on every track change")
        self.assertIn("levyra-player", context)
        self.assertIn("levyra-android-performance", context)
        self.assertNotIn("levyra-real-engineering", context)

    def test_root_cause_memory_prompt_stays_within_phase_budget(self) -> None:
        context = route("Root cause of native memory growth during playback")
        self.assertIn("levyra-player", context)
        self.assertIn("levyra-android-performance", context)
        self.assertNotIn("levyra-real-engineering", context)
        self.assertIn("max 2", context)

    def test_r8_defers_release_validation(self) -> None:
        context = route("R8 missing classes")
        self.assertIn("levyra-r8-proguard", context)
        self.assertNotIn("levyra-release-check", context)

    def test_intent_security_adds_general_security_review(self) -> None:
        context = route("Audit mutable PendingIntent handling")
        self.assertIn("levyra-android-intent-security", context)
        self.assertIn("levyra-security-review", context)

    def test_reverse_engineering_r8_stays_within_two_skills(self) -> None:
        context = route("Decompile this APK and recover Kotlin R8 metadata")
        self.assertIn("levyra-android-reverse-engineering", context)
        self.assertIn("levyra-r8-proguard", context)
        self.assertNotIn("levyra-security-review", context)
        self.assertNotIn("levyra-release-check", context)

    def test_reverse_engineering_without_r8_adds_security_review(self) -> None:
        context = route("Decompile this APK and inspect exposed components")
        self.assertIn("levyra-android-reverse-engineering", context)
        self.assertIn("levyra-security-review", context)

    def test_normal_apk_release_does_not_trigger_reverse_engineering(self) -> None:
        context = route("Build and validate the release APK")
        self.assertIn("levyra-release-check", context)
        self.assertNotIn("levyra-android-reverse-engineering", context)
        self.assertNotIn("levyra-context-efficiency", context)

    def test_project_manager_route(self) -> None:
        context = route("Update roadmap acceptance criteria for the active phase")
        self.assertIn("Mandatory skill load", context)
        self.assertIn("levyra-project-manager", context)
        self.assertNotIn("levyra-real-engineering", context)

    def test_desktop_route(self) -> None:
        self.assertIn("levyra-desktop", route("Work on the Windows Desktop mini player"))

    def test_openclaw_route_uses_only_orchestrator_and_project_manager(self) -> None:
        context = route("Delegate this Levyra fix through OpenClaw")
        self.assertIn("levyra-openclaw-orchestrator", context)
        self.assertIn("levyra-project-manager", context)
        self.assertNotIn("levyra-context-efficiency", context)

    def test_cross_domain_route_adds_engineering_coordinator(self) -> None:
        context = route("Investigate a cross-domain architecture issue across subsystems")
        self.assertIn("levyra-engineering", context)
        self.assertIn("levyra-real-engineering", context)
        self.assertNotIn("levyra-context-efficiency", context)

    def test_pr_authoring_routes_only_terminal_phase_skills(self) -> None:
        for prompt in (
            "Open a pull request for this branch",
            "Update the PR description",
            "Apri una PR con la descrizione Levyra",
        ):
            with self.subTest(prompt=prompt):
                context = route(prompt)
                self.assertIn("levyra-humanizer", context)
                self.assertIn("levyra-pr-review", context)
                self.assertNotIn("levyra-mode", context)

    def test_implementation_prompt_defers_pr_and_release_phase(self) -> None:
        context = route("VAI fix playback crash, test release APK, review and open PR")
        self.assertIn("levyra-player", context)
        for deferred in (
            "levyra-pr-review",
            "levyra-humanizer",
            "levyra-release-check",
            "levyra-mode",
            "levyra-context-efficiency",
        ):
            with self.subTest(skill=deferred):
                self.assertNotIn(deferred, context)


if __name__ == "__main__":
    unittest.main()
