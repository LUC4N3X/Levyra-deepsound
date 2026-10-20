from __future__ import annotations

import json
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
from unittest.mock import patch

from scripts import agent_runtime_doctor


class AgentRuntimeDoctorTest(unittest.TestCase):
    def test_missing_optional_tools_do_not_fail_readiness(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for rel in agent_runtime_doctor.REQUIRED_FILES.values():
                file = root / rel
                file.parent.mkdir(parents=True, exist_ok=True)
                file.touch()
            report = agent_runtime_doctor.inspect_readiness(
                root=root, which=lambda _: None
            )
        self.assertTrue(report["repository_ready"])
        self.assertFalse(any(report["optional_executables"].values()))
        self.assertEqual([], report["missing_contracts"])

    def test_missing_contracts_are_reported_without_starting_agents(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            report = agent_runtime_doctor.inspect_readiness(
                root=Path(tmp), which=lambda _: "/fake/tool"
            )
        self.assertFalse(report["repository_ready"])
        self.assertTrue(report["missing_contracts"])

    def test_json_output_contains_no_paths_or_auth_data(self) -> None:
        report = {
            "repository_ready": True,
            "optional_executables": {"codex": True},
            "notes": "Logins are not checked.",
        }
        output = StringIO()
        with patch.object(agent_runtime_doctor, "inspect_readiness", return_value=report):
            with redirect_stdout(output):
                self.assertEqual(0, agent_runtime_doctor.main(["--json", "--strict"]))
        self.assertEqual(report, json.loads(output.getvalue()))
        self.assertNotIn("/fake/", output.getvalue())

    def test_strict_missing_contracts_returns_nonzero(self) -> None:
        report = {"repository_ready": False, "optional_executables": {}, "notes": ""}
        with patch.object(agent_runtime_doctor, "inspect_readiness", return_value=report):
            with redirect_stdout(StringIO()):
                self.assertEqual(1, agent_runtime_doctor.main(["--strict"]))


if __name__ == "__main__":
    unittest.main()
