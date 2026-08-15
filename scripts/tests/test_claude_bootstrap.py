from __future__ import annotations

import unittest

from scripts.validate_claude_bootstrap import main as validate_claude_bootstrap


class ClaudeBootstrapTest(unittest.TestCase):
    def test_claude_bootstrap_contract_is_valid(self) -> None:
        self.assertEqual(0, validate_claude_bootstrap())


if __name__ == "__main__":
    unittest.main()
