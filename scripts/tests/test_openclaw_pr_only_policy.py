from __future__ import annotations

import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORKER = ROOT / "scripts" / "openclaw" / "levyra-worker"
EVIDENCE = ROOT / "scripts" / "openclaw" / "levyra-evidence"
SETUP = ROOT / "scripts" / "setup-openclaw-levyra-pr-only.sh"


class OpenClawPrOnlyPolicyTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        self.repo = self.root / "repo"
        self.bin_dir = self.root / "bin"
        self.repo.mkdir()
        self.bin_dir.mkdir()

        shutil.copy2(WORKER, self.bin_dir / "levyra-worker")
        shutil.copy2(EVIDENCE, self.bin_dir / "levyra-evidence")
        os.chmod(self.bin_dir / "levyra-worker", 0o700)
        os.chmod(self.bin_dir / "levyra-evidence", 0o700)

        self.run_git("init", "-b", "main")
        self.run_git("config", "user.name", "Levyra Test")
        self.run_git("config", "user.email", "levyra@example.invalid")
        (self.repo / "sample.txt").write_text("one\n", encoding="utf-8")
        self.run_git("add", "sample.txt")
        self.run_git("commit", "-m", "initial")

        self.env = os.environ.copy()
        self.env["LEVYRA_REPO"] = str(self.repo)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def run_git(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["git", "-C", str(self.repo), *args],
            check=True,
            text=True,
            capture_output=True,
        )

    def run_worker(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [str(self.bin_dir / "levyra-worker"), *args],
            env=self.env,
            text=True,
            capture_output=True,
        )

    def run_evidence(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [str(self.bin_dir / "levyra-evidence"), *args],
            env=self.env,
            text=True,
            capture_output=True,
        )

    def test_shell_syntax(self) -> None:
        for script in (WORKER, EVIDENCE, SETUP):
            subprocess.run(["bash", "-n", str(script)], check=True, cwd=ROOT)

    def test_worker_blocks_mutating_publication_from_main(self) -> None:
        for command in (("commit", "blocked"), ("push",), ("pr-open", "Title", "Body")):
            result = self.run_worker(*command)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("protected branch", result.stderr)

    def test_evidence_reconstructs_commit_without_pull_request(self) -> None:
        (self.repo / "sample.txt").write_text("one\ntwo\n", encoding="utf-8")
        self.run_git("add", "sample.txt")
        self.run_git("commit", "-m", "second")

        show = self.run_evidence("show", "HEAD")
        diff = self.run_evidence("diff", "HEAD^", "HEAD")

        self.assertEqual(show.returncode, 0, show.stderr)
        self.assertEqual(diff.returncode, 0, diff.stderr)
        self.assertIn("second", show.stdout)
        self.assertIn("+two", diff.stdout)

    def test_worker_blocks_gradle_publication_tasks_on_work_branch(self) -> None:
        self.run_git("switch", "-c", "agent/test")
        result = self.run_worker("gradle", "publish")

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("publication/deployment", result.stderr)

    def test_policy_setup_replaces_legacy_exec_paths_with_role_wrappers(self) -> None:
        setup = SETUP.read_text(encoding="utf-8")

        for term in (
            'tools.exec.mode',
            "allowlist",
            "/usr/local/bin/levyra-worker",
            "/usr/local/bin/levyra-release-main",
            "ensure_allowlist_pattern \"$PRIMARY_AGENT\" \"$PRIMARY_WORKER\"",
            "ensure_allowlist_pattern levyra-reviewer \"$REVIEW_EVIDENCE\"",
            "ensure_allowlist_pattern levyra-ci \"$CI_EVIDENCE\"",
            "Levyra PR-only publication policy",
            "branch + Pull Request only",
        ):
            self.assertIn(term, setup)

    def test_worker_has_no_merge_release_or_direct_main_publication_command(self) -> None:
        worker = WORKER.read_text(encoding="utf-8")

        for forbidden in (
            "gh pr merge",
            "gh release create",
            "git push origin main",
            "git push origin master",
        ):
            self.assertNotIn(forbidden, worker)

        for required in (
            'git -C "$REPO" push --set-upstream origin "HEAD:refs/heads/$branch"',
            "gh pr create",
            "validate_work_branch_name",
        ):
            self.assertIn(required, worker)


if __name__ == "__main__":
    unittest.main()
