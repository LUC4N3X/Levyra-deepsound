from __future__ import annotations

import subprocess
import unittest
from unittest.mock import Mock, patch

from scripts import validate_agent_config


class TrackedRuntimePathsTest(unittest.TestCase):
    def test_git_os_error_is_reported(self) -> None:
        errors: list[str] = []
        with patch.object(validate_agent_config.subprocess, "run", side_effect=OSError("missing")):
            self.assertEqual(validate_agent_config.tracked_generated_runtime_paths(errors), [])
        self.assertIn("unable to inspect", errors[0])

    def test_git_timeout_is_reported(self) -> None:
        errors: list[str] = []
        with patch.object(
            validate_agent_config.subprocess,
            "run",
            side_effect=subprocess.TimeoutExpired("git", 30),
        ):
            self.assertEqual(validate_agent_config.tracked_generated_runtime_paths(errors), [])
        self.assertIn("timed out", errors[0])

    def test_git_failure_is_reported(self) -> None:
        errors: list[str] = []
        result = Mock(returncode=128, stderr="fatal probe failure", stdout="")
        with patch.object(validate_agent_config.subprocess, "run", return_value=result):
            self.assertEqual(validate_agent_config.tracked_generated_runtime_paths(errors), [])
        self.assertIn("fatal probe failure", errors[0])


if __name__ == "__main__":
    unittest.main()
