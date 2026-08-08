from __future__ import annotations

import importlib.util
import io
import tempfile
import unittest
from contextlib import redirect_stderr
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / "scripts" / "render_fdroid_metadata.py"
SPEC = importlib.util.spec_from_file_location("render_fdroid_metadata", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
metadata = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(metadata)


class FdroidMetadataTest(unittest.TestCase):
    COMMIT = "a" * 40

    def test_current_release_renders_one_canonical_build(self) -> None:
        version, version_code = metadata.release_values(metadata.DEFAULT_PROPERTIES)
        rendered = metadata.render_metadata(
            metadata.DEFAULT_PROPERTIES,
            metadata.DEFAULT_TEMPLATE,
            self.COMMIT,
        )

        self.assertIn("Binaries: \n", rendered)
        self.assertIn(f"  - versionName: {version}\n", rendered)
        self.assertIn(f"    versionCode: {version_code}\n", rendered)
        self.assertIn(f"    commit: {self.COMMIT}\n", rendered)
        self.assertIn(f"CurrentVersion: {version}\n", rendered)
        self.assertIn(f"CurrentVersionCode: {version_code}\n", rendered)
        self.assertEqual(1, rendered.count("  - versionName:"))
        self.assertNotIn("@", rendered)

    def test_check_detects_the_rewritemeta_spacing_regression(self) -> None:
        rendered = metadata.render_metadata(
            metadata.DEFAULT_PROPERTIES,
            metadata.DEFAULT_TEMPLATE,
            self.COMMIT,
        )
        malformed = rendered.replace("Binaries: \n", "Binaries:\n")

        with tempfile.TemporaryDirectory() as directory:
            candidate = Path(directory) / "com.luc4n3x.levyra.yml"
            metadata.write_exact_text(candidate, malformed)
            with redirect_stderr(io.StringIO()):
                self.assertFalse(metadata.check_metadata(rendered, candidate))
            metadata.write_exact_text(candidate, rendered)
            self.assertTrue(metadata.check_metadata(rendered, candidate))

    def test_check_accepts_git_for_windows_line_endings(self) -> None:
        rendered = metadata.render_metadata(
            metadata.DEFAULT_PROPERTIES,
            metadata.DEFAULT_TEMPLATE,
            self.COMMIT,
        )
        with tempfile.TemporaryDirectory() as directory:
            candidate = Path(directory) / "com.luc4n3x.levyra.yml"
            with candidate.open("w", encoding="utf-8", newline="") as destination:
                destination.write(rendered.replace("\n", "\r\n"))
            self.assertTrue(metadata.check_metadata(rendered, candidate))

    def test_mismatched_android_version_code_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            properties = Path(directory) / "gradle.properties"
            properties.write_text(
                "levyraVersionName=2.3.20\nlevyraVersionCode=2031900\n",
                encoding="utf-8",
            )
            with self.assertRaises(metadata.MetadataError):
                metadata.release_values(properties)

    def test_short_commit_is_rejected(self) -> None:
        with self.assertRaises(metadata.MetadataError):
            metadata.resolve_commit("abc123")


if __name__ == "__main__":
    unittest.main()
