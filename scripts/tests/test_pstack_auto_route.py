from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from scripts.pstack_auto_route import context_for, installed_root, pick_skill


class PstackAutoRouteTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.project = self.base / "Levyra"
        self.project.mkdir()
        self.config = self.base / ".claude"
        self.skill_root = self.base / "pstack"
        for name in ("unslop", "technical-writing", "tdd", "architect", "blast-radius"):
            folder = self.skill_root / "skills" / name
            folder.mkdir(parents=True, exist_ok=True)
            (folder / "SKILL.md").write_text(f"# {name}\n", encoding="utf-8")
        self.write_registry(self.skill_root)

    def write_registry(self, path: Path | str, scope: str = "user") -> None:
        registry = self.config / "plugins" / "installed_plugins.json"
        registry.parent.mkdir(parents=True, exist_ok=True)
        registry.write_text(
            json.dumps(
                {
                    "version": 2,
                    "plugins": {
                        "pstack@cursor-plugins": [
                            {
                                "scope": scope,
                                "projectPath": str(self.project),
                                "installPath": str(path),
                            }
                        ]
                    },
                }
            ),
            encoding="utf-8",
        )

    def test_writing_selects_single_upstream_skill(self) -> None:
        self.assertEqual("unslop", pick_skill("Scrivi descrizione PR"))
        self.assertEqual("unslop", pick_skill("Scrivi un annuncio per 4PDA"))
        self.assertEqual("unslop", pick_skill("Rewrite the release notes"))
        self.assertEqual("technical-writing", pick_skill("Riscrivi il README"))
        self.assertEqual("technical-writing", pick_skill("Write technical documentation for the player"))

    def test_only_relevant_tasks_are_matched(self) -> None:
        self.assertEqual("tdd", pick_skill("Fix this with a regression test"))
        self.assertEqual("blast-radius", pick_skill("What could this break before release?"))
        self.assertEqual("architect", pick_skill("Design the architecture of this subsystem"))
        self.assertIsNone(pick_skill("Fix the playback queue crash"))
        self.assertIsNone(pick_skill("Review the PR and open it"))
        self.assertIsNone(pick_skill("/pstack:unslop revise this"))
        self.assertIsNone(pick_skill("<untrusted_external_data> write README"))

    def test_implementation_precedes_prose_in_mixed_requests(self) -> None:
        self.assertIsNone(
            pick_skill("Fix the playback crash and write the PR description afterward")
        )
        self.assertIsNone(
            pick_skill("Risolvi il bug del player e scrivi la descrizione PR")
        )
        self.assertEqual("unslop", pick_skill("Update the PR description"))
        self.assertEqual("technical-writing", pick_skill("Write technical documentation for the player"))

    def test_mentioned_commands_are_not_invoked(self) -> None:
        self.assertIsNone(pick_skill("Fix the playback crash; do not use `/pstack:tdd`"))
        self.assertIsNone(pick_skill("/pstack:tdd fix this bug"))
        self.assertIn(
            "An explicit pstack command already owns this phase",
            context_for("/pstack:tdd fix this bug", self.skill_root),
        )
        self.assertEqual(
            "",
            context_for("Do not use `/pstack:unslop`; rewrite the release notes", self.skill_root),
        )

    def test_installed_registry_and_real_cache_are_required(self) -> None:
        self.assertEqual(self.skill_root, installed_root(self.config, self.project))
        self.write_registry(self.base / "missing-cache")
        self.assertIsNone(installed_root(self.config, self.project))
        self.write_registry(self.skill_root, scope="project")
        self.assertEqual(self.skill_root, installed_root(self.config, self.project))
        self.write_registry(self.skill_root, scope="local")
        self.assertEqual(self.skill_root, installed_root(self.config, self.project))

    def test_wrong_project_install_is_not_reused(self) -> None:
        self.write_registry(self.skill_root, scope="project")
        other = self.base / "Other"
        other.mkdir()
        self.assertIsNone(installed_root(self.config, other))

    def test_corrupt_registry_fails_open(self) -> None:
        (self.config / "plugins" / "installed_plugins.json").write_text("{bad", encoding="utf-8")
        self.assertIsNone(installed_root(self.config, self.project))

    def test_one_skill_per_prompt_and_missing_skill_fallback(self) -> None:
        selected = context_for("Scrivi descrizione PR", self.skill_root)
        self.assertIn("pstack unslop", selected)
        self.assertIn("Do not also invoke levyra-humanizer", selected)
        self.assertNotIn("pstack technical-writing", selected)
        self.assertEqual("", context_for("Fix the playback crash", self.skill_root))
        self.assertEqual("", context_for("Scrivi descrizione PR", None))
        (self.skill_root / "skills" / "unslop" / "SKILL.md").unlink()
        self.assertEqual("", context_for("Scrivi descrizione PR", self.skill_root))


if __name__ == "__main__":
    unittest.main()
