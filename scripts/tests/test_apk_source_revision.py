from __future__ import annotations

import importlib.util
import tempfile
import unittest
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / "scripts" / "verify_apk_source_revision.py"
SPEC = importlib.util.spec_from_file_location("verify_apk_source_revision", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
revision = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(revision)


class ApkSourceRevisionTest(unittest.TestCase):
    EXPECTED = "a" * 40

    def apk(self, directory: str, content: str | None) -> Path:
        path = Path(directory) / "app.apk"
        with zipfile.ZipFile(path, "w") as archive:
            if content is not None:
                archive.writestr(revision.VERSION_CONTROL_ENTRY, content)
        return path

    def test_matching_revision_is_accepted(self) -> None:
        content = f'repositories {{\n  system: GIT\n  revision: "{self.EXPECTED}"\n}}\n'
        with tempfile.TemporaryDirectory() as directory:
            revisions = revision.verify_revision(self.apk(directory, content), self.EXPECTED)
        self.assertEqual((self.EXPECTED,), revisions)

    def test_mismatched_revision_is_rejected(self) -> None:
        content = f'repositories {{\n  system: GIT\n  revision: "{"b" * 40}"\n}}\n'
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(revision.RevisionError):
                revision.verify_revision(self.apk(directory, content), self.EXPECTED)

    def test_missing_version_control_entry_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(revision.RevisionError):
                revision.verify_revision(self.apk(directory, None), self.EXPECTED)

    def test_short_expected_revision_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(revision.RevisionError):
                revision.verify_revision(self.apk(directory, ""), "abc123")


if __name__ == "__main__":
    unittest.main()
