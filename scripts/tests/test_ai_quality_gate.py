from __future__ import annotations

import unittest

from scripts.ai_quality_gate import (
    build_commands,
    classify_changes,
    forbidden_path_findings,
    gradle_wrapper,
    scan_added_lines,
)
from scripts.validate_agent_config import DOCUMENTED_AGENT_IDS


class AiQualityGateTest(unittest.TestCase):
    def test_classifies_platform_changes(self) -> None:
        android = classify_changes({"app/src/main/java/example.kt"})
        desktop = classify_changes({"desktop/src/main/kotlin/example.kt"})
        extractor = classify_changes({"third_party/LevyraExtractor/src/Test.kt"})

        self.assertTrue(android.android)
        self.assertFalse(android.desktop)
        self.assertTrue(desktop.desktop)
        self.assertTrue(extractor.extractor)

    def test_root_gradle_change_does_not_select_independent_desktop(self) -> None:
        kinds = classify_changes({"settings.gradle.kts"})

        self.assertTrue(kinds.android)
        self.assertFalse(kinds.desktop)
        self.assertTrue(kinds.extractor)

    def test_rejects_sensitive_and_generated_paths(self) -> None:
        findings = forbidden_path_findings(
            {"local.properties", "app/release.jks", "app/build/output.apk"},
            allowlist=set(),
        )

        self.assertEqual(3, len(findings))

    def test_allowlist_is_exact(self) -> None:
        findings = forbidden_path_findings(
            {"app/release.jks", "other/release.jks"},
            allowlist={"app/release.jks"},
        )

        self.assertEqual(["signing/private-key file changed: other/release.jks"], findings)

    def test_detects_conflicts_and_credentials(self) -> None:
        credential = "real" + "-production-key-123"
        findings = scan_added_lines(
            ["<<<<<<< HEAD", "api_" + f'key = "{credential}"'],
            "fixture",
        )

        self.assertEqual(2, len(findings))

    def test_allows_synthetic_ci_credentials(self) -> None:
        findings = scan_added_lines(
            ['password = "levyra_ci_test_password"', 'token = "${TOKEN}"'],
            "fixture",
        )

        self.assertEqual([], findings)

    def test_does_not_treat_word_fragment_as_synthetic(self) -> None:
        candidate = "la" + "test-production-secret"
        findings = scan_added_lines(
            ["pass" + f'word = "{candidate}"'],
            "fixture",
        )

        self.assertEqual(1, len(findings))

    def test_allows_token_identifiers_and_kotlin_type_annotations(self) -> None:
        findings = scan_added_lines(
            [
                "val normalizedToken = normalizedReleaseArtist(token)",
                "return referenceNames.all(normalizedToken::contains)",
                "private fun label(token: String): Boolean = token.isNotBlank()",
            ],
            "fixture",
        )

        self.assertEqual([], findings)

    def test_still_detects_standalone_token_assignments(self) -> None:
        credential = "real" + "-production-token-123"
        findings = scan_added_lines(
            [
                f'{"to" + "ken"} = "{credential}"',
                f'{"api" + "_" + "key"}: "{credential}"',
            ],
            "fixture",
        )

        self.assertEqual(2, len(findings))

    def test_openclaw_agent_ids_are_not_native_skill_requirements(self) -> None:
        self.assertEqual(
            {"levyra-ci", "levyra-reviewer", "levyra-worker"},
            DOCUMENTED_AGENT_IDS,
        )

    def test_full_android_profile_adds_tests_lint_and_compile(self) -> None:
        commands, blocked = build_commands(
            {"app/src/main/java/example.kt"},
            "full",
            python="python",
        )
        labels = {command.label for command in commands}

        self.assertEqual([], blocked)
        self.assertIn("Check Android runtime compatibility", labels)
        self.assertIn("Run all Android unit tests", labels)
        self.assertIn("Run Android release lint", labels)
        self.assertIn("Compile unsigned F-Droid release", labels)

    def test_fast_android_profile_skips_heavy_gradle_checks(self) -> None:
        commands, blocked = build_commands(
            {"app/src/main/java/example.kt"},
            "fast",
            python="python",
        )
        labels = {command.label for command in commands}

        self.assertEqual([], blocked)
        self.assertNotIn("Run all Android unit tests", labels)

    def test_full_desktop_profile_uses_desktop_project_directory(self) -> None:
        """The full gate must invoke the Desktop wrapper in the Desktop project."""
        commands, blocked = build_commands(
            {"desktop/app/src/main/kotlin/example.kt"},
            "full",
            python="python",
        )
        desktop_command = next(
            command for command in commands if command.label == "Run Desktop checks and assembly"
        )

        self.assertEqual([], blocked)
        self.assertEqual(gradle_wrapper("desktop"), desktop_command.argv[0])
        self.assertEqual(("-p", "desktop", "check", "assemble"), desktop_command.argv[-4:])


if __name__ == "__main__":
    unittest.main()
