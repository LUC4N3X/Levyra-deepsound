from __future__ import annotations

import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class UsageObservabilityValidationTest(unittest.TestCase):
    def test_usage_observability_validator(self) -> None:
        result = subprocess.run(
            [sys.executable, str(ROOT / "scripts/validate_usage_observability.py")],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(
            result.returncode,
            0,
            msg=f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}",
        )


if __name__ == "__main__":
    unittest.main()
