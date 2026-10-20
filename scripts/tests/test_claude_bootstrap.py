from __future__ import annotations

import unittest
import fnmatch
import json
from pathlib import Path

from scripts.validate_claude_bootstrap import main as validate_claude_bootstrap


class ClaudeBootstrapTest(unittest.TestCase):
    def test_inspection_permissions_do_not_allow_mutating_commands_or_wrappers(self) -> None:
        root = Path(__file__).resolve().parents[2]
        settings = json.loads((root / ".agents/claude/settings.json").read_text(encoding="utf-8"))
        patterns = [entry[5:-1] for entry in settings["permissions"]["allow"] if entry.startswith("Bash(")]
        for command in ("gh pr view 1018", "gh run view 123", "rtk gh pr checks 1018", "rtk git status --short"):
            with self.subTest(command=command):
                self.assertTrue(any(fnmatch.fnmatchcase(command, pattern) for pattern in patterns))
        for command in ("git push", "git commit -m change", "gh pr merge 1018", "gh api repos/x/y -X DELETE", "rtk proxy bash", "rtk run bash", "rtk git push", "rtk gh pr merge 1018", "rtk git log --output=file", "rtk git show --output=file", "rtk git diff --output=file"):
            with self.subTest(command=command):
                self.assertFalse(any(fnmatch.fnmatchcase(command, pattern) for pattern in patterns))
        self.assertIn("Read(./**/.env)", settings["permissions"]["deny"])

    def test_claude_bootstrap_contract_is_valid(self) -> None:
        self.assertEqual(0, validate_claude_bootstrap())


if __name__ == "__main__":
    unittest.main()
