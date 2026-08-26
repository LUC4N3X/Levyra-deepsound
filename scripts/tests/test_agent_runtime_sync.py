from __future__ import annotations

import hashlib
import importlib.util
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / "scripts" / "sync_agent_runtime.py"


def load_runtime_module():
    spec = importlib.util.spec_from_file_location("levyra_sync_agent_runtime", MODULE_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError("unable to load sync_agent_runtime.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


class AgentRuntimeSyncTest(unittest.TestCase):
    def setUp(self) -> None:
        self.module = load_runtime_module()
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.canonical = self.root / ".agents"
        self.runtime = self.root / ".claude"
        self.skill = self.canonical / "skills" / "levyra-example" / "SKILL.md"
        self.settings = self.canonical / "claude" / "settings.json"
        self.skill.parent.mkdir(parents=True)
        self.settings.parent.mkdir(parents=True)
        self.skill.write_text("---\nname: levyra-example\n---\n", encoding="utf-8")
        self.settings.write_text('{"example": true}\n', encoding="utf-8")

        self.module.ROOT = self.root
        self.module.RUNTIMES = {
            "claude": self.module.RuntimeSpec(
                target=self.runtime,
                mappings=(
                    self.module.Mapping(self.settings, Path("settings.json")),
                    self.module.Mapping(self.canonical / "skills", Path("skills")),
                ),
                owned_files=(Path("settings.json"),),
                owned_directories=(Path("skills"),),
            )
        }

    def write_manifest(self, managed_files: dict[str, str]) -> None:
        self.runtime.mkdir(parents=True, exist_ok=True)
        (self.runtime / self.module.MANIFEST_NAME).write_text(
            json.dumps(
                {
                    "schema_version": self.module.MANIFEST_SCHEMA_VERSION,
                    "runtime": "claude",
                    "source": ".agents",
                    "managed_files": managed_files,
                }
            ),
            encoding="utf-8",
        )

    def test_sync_projects_canonical_skills_and_preserves_unknown_local_files(self) -> None:
        self.runtime.mkdir(parents=True)
        unknown = self.runtime / "settings.local.json"
        unknown.write_text('{"local": true}\n', encoding="utf-8")
        stale = self.runtime / "skills" / "removed" / "SKILL.md"
        stale.parent.mkdir(parents=True)
        stale_content = b"stale\n"
        stale.write_bytes(stale_content)
        self.write_manifest({"skills/removed/SKILL.md": sha256_bytes(stale_content)})

        self.module.sync_runtime("claude", quiet=True)

        self.assertEqual(
            (self.runtime / "skills" / "levyra-example" / "SKILL.md").read_text(encoding="utf-8"),
            self.skill.read_text(encoding="utf-8"),
        )
        self.assertEqual(
            (self.runtime / "settings.json").read_text(encoding="utf-8"),
            self.settings.read_text(encoding="utf-8"),
        )
        self.assertFalse(stale.exists())
        self.assertTrue(unknown.is_file())
        self.assertEqual(unknown.read_text(encoding="utf-8"), '{"local": true}\n')
        self.assertEqual(self.module.check_runtime("claude"), [])

    def test_manifest_path_traversal_is_rejected_without_touching_outside_file(self) -> None:
        outside = self.root / "outside.txt"
        outside.write_text("keep\n", encoding="utf-8")
        self.write_manifest({"../outside.txt": sha256_bytes(b"keep\n")})

        with self.assertRaises(self.module.ProjectionError):
            self.module.sync_runtime("claude", quiet=True)

        self.assertEqual(outside.read_text(encoding="utf-8"), "keep\n")

    def test_manifest_outside_owned_namespace_is_rejected(self) -> None:
        local = self.runtime / "settings.local.json"
        self.runtime.mkdir(parents=True, exist_ok=True)
        local.write_text("keep\n", encoding="utf-8")
        self.write_manifest({"settings.local.json": sha256_bytes(b"keep\n")})

        with self.assertRaises(self.module.ProjectionError):
            self.module.sync_runtime("claude", quiet=True)

        self.assertEqual(local.read_text(encoding="utf-8"), "keep\n")

    def test_locally_modified_stale_generated_file_is_preserved_and_blocks_cleanup(self) -> None:
        stale = self.runtime / "skills" / "removed" / "SKILL.md"
        stale.parent.mkdir(parents=True)
        stale.write_text("locally changed\n", encoding="utf-8")
        self.write_manifest({"skills/removed/SKILL.md": sha256_bytes(b"original generated\n")})

        with self.assertRaises(self.module.ProjectionError):
            self.module.sync_runtime("claude", quiet=True)

        self.assertEqual(stale.read_text(encoding="utf-8"), "locally changed\n")

    def test_unknown_directory_conflict_is_preserved(self) -> None:
        conflict = self.runtime / "settings.json"
        conflict.mkdir(parents=True)
        marker = conflict / "local.txt"
        marker.write_text("keep\n", encoding="utf-8")

        with self.assertRaises(self.module.ProjectionError):
            self.module.sync_runtime("claude", quiet=True)

        self.assertEqual(marker.read_text(encoding="utf-8"), "keep\n")

    def test_corrupt_manifest_blocks_sync_instead_of_forgetting_managed_state(self) -> None:
        self.runtime.mkdir(parents=True)
        (self.runtime / self.module.MANIFEST_NAME).write_text("{broken", encoding="utf-8")

        with self.assertRaises(self.module.ProjectionError):
            self.module.sync_runtime("claude", quiet=True)

    @unittest.skipUnless(hasattr(os, "symlink"), "symlinks unavailable")
    def test_symlinked_projection_parent_does_not_write_outside_runtime(self) -> None:
        outside = self.root / "outside"
        outside.mkdir()
        self.runtime.mkdir(parents=True)
        try:
            (self.runtime / "skills").symlink_to(outside, target_is_directory=True)
        except OSError as exc:
            self.skipTest(f"symlink creation unavailable: {exc}")

        with self.assertRaises(self.module.ProjectionError):
            self.module.sync_runtime("claude", quiet=True)

        self.assertFalse((outside / "levyra-example" / "SKILL.md").exists())

    @unittest.skipUnless(hasattr(os, "symlink"), "symlinks unavailable")
    def test_symlinked_runtime_root_is_rejected(self) -> None:
        outside = self.root / "outside-runtime"
        outside.mkdir()
        try:
            self.runtime.symlink_to(outside, target_is_directory=True)
        except OSError as exc:
            self.skipTest(f"symlink creation unavailable: {exc}")

        with self.assertRaises(self.module.ProjectionError):
            self.module.sync_runtime("claude", quiet=True)

        self.assertEqual(list(outside.iterdir()), [])


if __name__ == "__main__":
    unittest.main()
