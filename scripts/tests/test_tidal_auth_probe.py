from __future__ import annotations

import contextlib
import io
import json
import os
from pathlib import Path
import tempfile
import textwrap
import unittest
from unittest.mock import MagicMock, patch
import urllib.error
import urllib.parse
import urllib.request


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/quick-apk.yml"
PROBE = textwrap.dedent(
    WORKFLOW.read_text(encoding="utf-8")
    .split("      - name: Exchange refresh credential without publishing credentials\n", 1)[1]
    .split("        run: |\n", 1)[1]
    .split("\n  quick-apk:", 1)[0]
)


class TidalAuthProbeTest(unittest.TestCase):
    def run_probe(self, payload=None, credential="test-refresh-credential", error=None):
        opener = MagicMock()
        opener.open.return_value.__enter__.return_value.read.return_value = payload
        if error is not None:
            opener.open.side_effect = error
        output = io.StringIO()
        with tempfile.TemporaryDirectory() as directory:
            summary = Path(directory) / "summary.md"
            with patch.dict(os.environ, {
                "LEVYRA_TIDAL_REFRESH_TOKEN": credential,
                "LEVYRA_TIDAL_CLIENT_ID": "test-client",
                "GITHUB_STEP_SUMMARY": str(summary),
            }, clear=True), patch("urllib.request.build_opener", return_value=opener), contextlib.redirect_stdout(output):
                with self.assertRaises(SystemExit) as caught:
                    exec(compile(PROBE, str(WORKFLOW), "exec"), {})
            combined = output.getvalue() + summary.read_text(encoding="utf-8")
        for private_value in ("test-refresh-credential", "test-access-credential", "test-rotated-credential", "test-private-error"):
            self.assertNotIn(private_value, combined)
        return caught.exception.code, combined, opener

    def test_exchange_keeps_credentials_out_of_output(self):
        status, output, opener = self.run_probe(json.dumps({"access_token": "test-access-credential"}).encode())
        self.assertEqual(0, status)
        self.assertIn("PASS:", output)
        request = opener.open.call_args.args[0]
        self.assertEqual("https://auth.tidal.com/v1/oauth2/token", request.full_url)
        self.assertEqual({
            "grant_type": ["refresh_token"],
            "refresh_token": ["test-refresh-credential"],
            "client_id": ["test-client"],
        }, urllib.parse.parse_qs(request.data.decode()))

    def test_missing_or_split_credential_does_not_contact_tidal(self):
        for credential in ("", "test-refresh-credential\n", "test refresh credential"):
            with self.subTest(credential=credential):
                status, output, opener = self.run_probe(credential=credential)
                self.assertEqual(1, status)
                self.assertIn("BLOCKED:", output)
                opener.open.assert_not_called()

    def test_invalid_response_cannot_pass(self):
        for payload in (b"not-json", b"[]", b"{}", b'{"access_token":false}', b"x" * 65_537):
            with self.subTest(payload_length=len(payload)):
                status, output, _ = self.run_probe(payload)
                self.assertEqual(1, status)
                self.assertIn("FAIL:", output)

    def test_http_and_transport_errors_do_not_leak_provider_text(self):
        for error in (
            urllib.error.HTTPError("https://auth.tidal.com", 401, "test-private-error", {}, None),
            urllib.error.URLError("test-private-error"),
            TimeoutError("test-private-error"),
        ):
            with self.subTest(error_type=type(error).__name__):
                status, _, _ = self.run_probe(error=error)
                self.assertEqual(1, status)

    def test_rotation_requires_owner_intervention(self):
        status, output, _ = self.run_probe(json.dumps({
            "access_token": "test-access-credential",
            "refresh_token": "test-rotated-credential",
        }).encode())
        self.assertEqual(1, status)
        self.assertIn("BLOCKED: TIDAL rotated", output)

    def test_redirects_are_rejected(self):
        scope = {}
        opener = MagicMock()
        opener.open.side_effect = urllib.error.URLError("test-error")
        with patch.dict(os.environ, {
            "LEVYRA_TIDAL_REFRESH_TOKEN": "test-refresh-credential",
            "GITHUB_STEP_SUMMARY": os.devnull,
        }), patch("urllib.request.build_opener", return_value=opener):
            with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(SystemExit):
                exec(compile(PROBE, str(WORKFLOW), "exec"), scope)
        self.assertIsNone(scope["RejectRedirects"]().redirect_request(None, None, 302, "", {}, "https://example.com"))


if __name__ == "__main__":
    unittest.main()
