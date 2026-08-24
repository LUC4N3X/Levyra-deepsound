from __future__ import annotations

import unittest
from pathlib import Path

from scripts.evaluate_skill_routing import CASES, evaluate_all, summary

ROOT = Path(__file__).resolve().parents[2]


class SkillIntelligenceTest(unittest.TestCase):
    def test_routing_eval_corpus_passes_without_errors(self) -> None:
        results = evaluate_all()
        failures = [result for result in results if result.status != "PASS"]

        self.assertFalse(
            failures,
            "skill routing eval failures:\n"
            + "\n".join(
                f"{result.name}: status={result.status} selected={result.selected} "
                f"missing={result.missing} forbidden={result.forbidden_selected} "
                f"error={result.error}"
                for result in failures
            ),
        )

        stats = summary(results)
        self.assertEqual(len(CASES), stats["passed"])
        self.assertEqual(0, stats["failed"])
        self.assertEqual(0, stats["errors"])
        self.assertLessEqual(stats["max_context_bytes"], 4096)

    def test_eval_corpus_contains_near_miss_and_collision_coverage(self) -> None:
        names = {case.name for case in CASES}
        for required_name in (
            "design-near-miss-kotlin",
            "design-near-miss-gradle",
            "design-near-miss-subscription",
            "design-near-miss-screenshot",
            "extractor-near-miss-agent-token",
            "normal-release-apk",
            "plain-kotlin-question",
            "semantic-version-near-miss",
            "compose-semantics",
        ):
            with self.subTest(required_name=required_name):
                self.assertIn(required_name, names)

        for case in CASES:
            with self.subTest(case=case.name):
                self.assertGreater(case.max_skills, 0)
                self.assertLessEqual(case.max_context_bytes, 4096)

    def test_real_engineering_has_failure_and_retraction_discipline(self) -> None:
        text = (
            ROOT / ".agents" / "skills" / "levyra-real-engineering" / "SKILL.md"
        ).read_text(encoding="utf-8")

        for term in (
            "reproduce -> isolate -> hypothesis -> smallest experiment -> fix -> prevent",
            "PRODUCT_REGRESSION",
            "TEST_DEFECT",
            "ENVIRONMENT",
            "FLAKY",
            "DISPROVED",
            "retract it explicitly",
            "Do not rerun an unchanged failing command repeatedly to fish for a green result",
            "docs/ai/SKILL_INTELLIGENCE.md",
        ):
            with self.subTest(term=term):
                self.assertIn(term, text)

    def test_security_review_has_finding_states_and_evidence_hygiene(self) -> None:
        text = (
            ROOT / ".agents" / "skills" / "levyra-security-review" / "SKILL.md"
        ).read_text(encoding="utf-8")

        for term in (
            "## Finding lifecycle",
            "SUSPECTED",
            "VALIDATED",
            "DISPROVED",
            "RETRACTED",
            "## Evidence hygiene",
            "<redacted-token>",
            "withheld",
            "Claude-BugHunter",
        ):
            with self.subTest(term=term):
                self.assertIn(term, text)

    def test_pr_review_revalidates_findings_and_ci_failures(self) -> None:
        text = (
            ROOT / ".agents" / "skills" / "levyra-pr-review" / "SKILL.md"
        ).read_text(encoding="utf-8")

        for term in (
            "## Finding lifecycle",
            "CONFIRMED",
            "RETRACTED",
            "current-head",
            "## CI/test failure discipline",
            "PRODUCT_REGRESSION",
            "TEST_DEFECT",
            "FLAKY",
            "Do not repeatedly rerun the same failing check to manufacture a green status",
            "docs/ai/SKILL_INTELLIGENCE.md",
            "Load `levyra-security-review` only when the diff or finding is actually security-sensitive",
        ):
            with self.subTest(term=term):
                self.assertIn(term, text)

    def test_policy_keeps_external_catalogs_out_of_bootstrap(self) -> None:
        text = (ROOT / "docs" / "ai" / "SKILL_INTELLIGENCE.md").read_text(
            encoding="utf-8"
        )

        for term in (
            "does not improve its coding agents by installing every available skill catalog",
            "difficult near-misses",
            "Infrastructure or evaluator errors are `ERROR`",
            "prompt/context tokens",
            "should not be adopted",
            "anthropics/skills",
            "Jeffallan/claude-skills",
            "elementalsouls/Claude-BugHunter",
            "ChrisTitusTech/titus-ai",
        ):
            with self.subTest(term=term):
                self.assertIn(term, text)

    def test_eval_source_keeps_comments_out_of_new_code(self) -> None:
        text = (ROOT / "scripts" / "evaluate_skill_routing.py").read_text(
            encoding="utf-8"
        )
        comment_lines = [
            line
            for line in text.splitlines()[1:]
            if line.lstrip().startswith("#")
        ]
        self.assertEqual([], comment_lines)


if __name__ == "__main__":
    unittest.main()