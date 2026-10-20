from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from scripts import headroom_adapter


class HeadroomAdapterTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.mock_config = Path(self.temp_dir.name) / "headroom.json"
        self.patcher = patch.object(headroom_adapter, "HEADROOM_CONFIG_PATH", self.mock_config)
        self.patcher.start()

    def tearDown(self) -> None:
        self.patcher.stop()
        self.temp_dir.cleanup()

    def test_probe_headroom_status_shape(self) -> None:
        status = headroom_adapter.probe_headroom_status()
        self.assertIsInstance(status.available, bool)
        self.assertIsInstance(status.enabled, bool)
        self.assertIsInstance(status.bundled_tools, dict)
        self.assertIsInstance(status.proxy_running, bool)

    def test_enable_and_disable_lifecycle(self) -> None:
        self.assertFalse(headroom_adapter.is_headroom_enabled())
        headroom_adapter.set_headroom_enabled(True)
        self.assertTrue(headroom_adapter.is_headroom_enabled())
        headroom_adapter.set_headroom_enabled(False)
        self.assertFalse(headroom_adapter.is_headroom_enabled())

    def test_proxy_listening_socket_probe(self) -> None:
        # Port 59999 should not be listening
        self.assertFalse(headroom_adapter.is_proxy_listening("127.0.0.1", 59999, timeout=0.1))

    def test_mcp_config_structure(self) -> None:
        config = headroom_adapter.get_mcp_config()
        self.assertIn("mcpServers", config)
        self.assertIn("headroom", config["mcpServers"])
        server = config["mcpServers"]["headroom"]
        self.assertIn("command", server)
        self.assertEqual(["mcp", "serve"], server["args"])

    def test_fallback_when_executable_missing(self) -> None:
        with patch.object(headroom_adapter, "get_headroom_executable", return_value=None):
            status = headroom_adapter.probe_headroom_status()
            self.assertFalse(status.available)
            self.assertIsNone(status.version)
            diff = headroom_adapter.run_structural_diff("fileA", "fileB")
            self.assertIsNone(diff)

    def test_cli_status_and_doctor(self) -> None:
        self.assertEqual(0, headroom_adapter.main(["status"]))
        self.assertEqual(0, headroom_adapter.main(["doctor"]))
        self.assertEqual(0, headroom_adapter.main(["mcp-config"]))


if __name__ == "__main__":
    unittest.main()
