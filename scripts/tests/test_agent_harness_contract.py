from __future__ import annotations

import unittest

from scripts.validate_agent_harness import main as validate_agent_harness


class AgentHarnessContractTest(unittest.TestCase):
    def test_always_on_agent_harness_contract_is_valid(self) -> None:
        self.assertEqual(0, validate_agent_harness())


if __name__ == "__main__":
    unittest.main()
