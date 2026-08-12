from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ORCHESTRATOR = ROOT / ".agents" / "skills" / "levyra-openclaw-orchestrator" / "SKILL.md"
REVIEW = ROOT / ".agents" / "skills" / "levyra-pr-review" / "SKILL.md"
CI = ROOT / ".agents" / "skills" / "levyra-ci-workflows" / "SKILL.md"


class OpenClawEvidenceContractsTest(unittest.TestCase):
    def test_specialists_use_their_own_checkout_and_exact_sha(self) -> None:
        orchestrator = ORCHESTRATOR.read_text(encoding="utf-8")

        for term in (
            "exact target SHA",
            "own workspace `./repo` checkout",
            "Parent-agent relative paths",
            "including push runs",
        ):
            self.assertIn(term, orchestrator)

    def test_commit_review_does_not_require_a_pull_request(self) -> None:
        review = REVIEW.read_text(encoding="utf-8")

        for term in (
            "A review does not require a pull request",
            "reviewer agent's own `./repo` checkout",
            "git show <sha>",
            "git diff <sha>^ <sha>",
            "missing PR association is never",
        ):
            self.assertIn(term, review)

    def test_ci_queries_actions_by_exact_sha_including_push_runs(self) -> None:
        ci = CI.read_text(encoding="utf-8")

        for term in (
            "exact `head_sha`",
            "push-triggered",
            "empty commit-status surface",
            "gh run list --repo LUC4N3X/Levyra-deepsound --commit <sha>",
            "PR-scoped queries only",
        ):
            self.assertIn(term, ci)


if __name__ == "__main__":
    unittest.main()
