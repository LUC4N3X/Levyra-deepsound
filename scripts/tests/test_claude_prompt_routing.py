from __future__ import annotations

import json
import subprocess
import unittest
from pathlib import Path

from scripts.ai_quality_gate import find_bash

ROOT = Path(__file__).resolve().parents[2]
HOOK = ROOT / ".claude" / "hooks" / "user-prompt-submit.sh"


from scripts.ai_quality_gate import find_bash


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
    def test_design_activation_phrases(self) -> None:
        prompts = (
            "Make the Now Playing screen more premium",
            "premium",
            "modern",
            "clean",
            "cinematic",
            "less generic",
        )

        for prompt in prompts:
            with self.subTest(prompt=prompt):
                self.assertIn("levyra-design-taste", route(prompt))

    def test_design_words_do_not_trigger_in_unrelated_contexts(self) -> None:
        prompts = (
            "Upgrade to a modern Kotlin version",
            "Clean Gradle build outputs",
            "Integrate a premium subscription API",
        )

        for prompt in prompts:
            with self.subTest(prompt=prompt):
                self.assertNotIn("levyra-design-taste", route(prompt))

    def test_compose_jank_adds_real_engineering(self) -> None:
        context = route("Compose jank issue")

        self.assertIn("levyra-compose", context)
        self.assertIn("levyra-android-performance", context)
        self.assertIn("levyra-real-engineering", context)

    def test_r8_adds_release_validation(self) -> None:
        context = route("R8 missing classes")

        self.assertIn("levyra-r8-proguard", context)
        self.assertIn("levyra-release-check", context)

    def test_intent_security_adds_general_security_review(self) -> None:
        context = route("Audit mutable PendingIntent handling")

        self.assertIn("levyra-android-intent-security", context)
        self.assertIn("levyra-security-review", context)


if __name__ == "__main__":
    unittest.main()
