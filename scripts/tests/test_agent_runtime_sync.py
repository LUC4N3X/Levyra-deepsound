from __future__ import annotations

import importlib.util
import json
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
    spec.loader.exec_module(module)
    return module


class AgentRuntimeSyncTest(unittest.TestCase):
    def test_sync_projects_canonical_skills_and_preserves_unknown_local_files(self) -> None:
        module = load_runtime_module()

        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            canonical = root / ".agents"
            runtime = root / ".claude"
            skill = canonical / "skills" / "levyra-example" / "SKILL.md"
            settings = canonical / "claude" / "settings.json"
            skill.parent.mkdir(parents=True)
            settings.parent.mkdir(parents=True)
            skill.write_text("---\nname: levyra-example\n---\n", encoding="utf-8")
            settings.write_text('{"example": true}\n', encoding="utf-8")

            runtime.mkdir(parents=True)
            unknown = runtime / "settings.local.json"
            unknown.write_text('{"local": true}\n', encoding="utf-8")
            stale = runtime / "skills" / "removed" / "SKILL.md"
            stale.parent.mkdir(parents=True)
            stale.write_text("stale\n", encoding="utf-8")
            (runtime / module.MANIFEST_NAME).write_text(
                json.dumps({"managed_files": ["skills/removed/SKILL.md"]}),
                encoding="utf-8",
            )

            module.ROOT = root
            module.RUNTIMES = {
                "claude": module.RuntimeSpec(
                    target=runtime,
                    mappings=(
                        module.Mapping(settings, Path("settings.json")),
                        module.Mapping(canonical / "skills", Path("skills")),
                    ),
                )
            }

            module.sync_runtime("claude", quiet=True)

            self.assertEqual(
                (runtime / "skills" / "levyra-example" / "SKILL.md").read_text(encoding="utf-8"),
                skill.read_text(encoding="utf-8"),
            )
            self.assertEqual(
                (runtime / "settings.json").read_text(encoding="utf-8"),
                settings.read_text(encoding="utf-8"),
            )
            self.assertFalse(stale.exists())
            self.assertTrue(unknown.is_file())
            self.assertEqual(unknown.read_text(encoding="utf-8"), '{"local": true}\n')
            self.assertEqual(module.check_runtime("claude"), [])


if __name__ == "__main__":
    unittest.main()
