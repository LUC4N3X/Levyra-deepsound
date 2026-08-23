from __future__ import annotations

import io
import json
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

import scripts.agent_checkpoint as checkpoint
import scripts.agent_harness as harness


class AgentHarnessTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.state = self.root / "state"
        self.original_root = harness.ROOT
        self.original_state = harness.STATE_ROOT
        self.original_checkpoint_root = checkpoint.ROOT
        self.original_checkpoint_state = checkpoint.CHECKPOINT_ROOT
        harness.ROOT = self.root
        harness.STATE_ROOT = self.state
        checkpoint.ROOT = self.root
        checkpoint.CHECKPOINT_ROOT = self.state / "checkpoint"

    def tearDown(self) -> None:
        harness.ROOT = self.original_root
        harness.STATE_ROOT = self.original_state
        checkpoint.ROOT = self.original_checkpoint_root
        checkpoint.CHECKPOINT_ROOT = self.original_checkpoint_state
        self.temp.cleanup()

    def capture(self, function, payload: dict) -> str:
        stream = io.StringIO()
        with redirect_stdout(stream):
            function(payload)
        return stream.getvalue().strip()

    def test_existing_whole_file_write_requires_fresh_full_read(self) -> None:
        target = self.root / "sample.kt"
        target.write_text("val answer = 42\n", encoding="utf-8")
        write = {"session_id": "s", "tool_name": "Write", "tool_input": {"file_path": str(target)}}
        blocked = json.loads(self.capture(harness.pre_tool, write))
        self.assertEqual("deny", blocked["hookSpecificOutput"]["permissionDecision"])

        read = {"session_id": "s", "tool_name": "Read", "tool_input": {"file_path": str(target)}}
        harness.post_tool(read)
        allowed = self.capture(harness.pre_tool, write)
        self.assertNotIn('"permissionDecision": "deny"', allowed)

        target.write_text("val answer = 43\n", encoding="utf-8")
        stale = json.loads(self.capture(harness.pre_tool, write))
        self.assertEqual("deny", stale["hookSpecificOutput"]["permissionDecision"])

    def test_edit_injects_scoped_agents_and_current_file(self) -> None:
        app = self.root / "app"
        app.mkdir()
        (app / "AGENTS.md").write_text("Scoped app rule", encoding="utf-8")
        target = app / "Player.kt"
        target.write_text("fun play() = Unit\n", encoding="utf-8")
        payload = {"session_id": "s", "tool_name": "Edit", "tool_input": {"file_path": str(target), "old_string": "fun play"}}
        output = self.capture(harness.pre_tool, payload)
        self.assertIn("Scoped app rule", output)
        self.assertIn("fun play() = Unit", output)

    def test_large_apply_patch_uses_patch_anchor_for_current_context(self) -> None:
        target = self.root / "Large.kt"
        lines = [f"val line{i} = {i}" for i in range(1200)]
        target.write_text("\n".join(lines), encoding="utf-8")
        payload = {
            "session_id": "s",
            "tool_name": "apply_patch",
            "tool_input": {
                "command": "*** Begin Patch\n*** Update File: Large.kt\n@@\n-val line900 = 900\n+val line900 = 901\n*** End Patch"
            },
        }
        output = self.capture(harness.pre_tool, payload)
        self.assertIn("line900", output)
        self.assertNotIn("line10 = 10", output)

    def test_followup_prompt_preserves_open_task_state(self) -> None:
        self.capture(harness.user_prompt, {"session_id": "s", "prompt": "Fix playback"})
        target = self.root / "sample.kt"
        target.write_text("val x = 1\n", encoding="utf-8")
        harness.post_tool({"session_id": "s", "tool_name": "Edit", "tool_input": {"file_path": str(target)}})
        self.capture(harness.user_prompt, {"session_id": "s", "prompt": "Also keep video mode unchanged"})
        state = harness._load({"session_id": "s"})
        self.assertEqual(1, state["edit_generation"])
        self.assertIn("sample.kt", state["edited_paths"])
        self.assertEqual("Also keep video mode unchanged", state["latest_prompt"])

    def test_compaction_reanchors_latest_task_state(self) -> None:
        self.capture(harness.user_prompt, {"session_id": "s", "prompt": "Fix playback without changing UI"})
        output = self.capture(harness.compact, {"session_id": "s"})
        self.assertIn("Fix playback without changing UI", output)
        state = harness._load({"session_id": "s"})
        self.assertTrue(state["reanchor_pending"])

    def test_checkpoint_blocks_third_identical_failed_command(self) -> None:
        payload = {
            "session_id": "s",
            "tool_name": "Bash",
            "tool_input": {"command": "jadx --version"},
        }
        checkpoint._record_failure(payload, "jadx failed")
        checkpoint._record_failure(payload, "jadx failed")

        blocked = json.loads(self.capture(checkpoint.pre_tool, payload))
        self.assertEqual("deny", blocked["hookSpecificOutput"]["permissionDecision"])
        self.assertIn("failed twice", blocked["hookSpecificOutput"]["permissionDecisionReason"])

    def test_checkpoint_clear_preserves_blocked_open_task(self) -> None:
        payload = {
            "session_id": "s",
            "tool_name": "Bash",
            "tool_input": {"command": "jadx --version"},
        }
        checkpoint._record_failure(payload, "jadx: command not found")
        checkpoint._save_hygiene(
            {
                "completed_since_clear": 2,
                "last_completed_signature": "previous",
                "clear_recommended": True,
            }
        )

        output = self.capture(checkpoint.session_start, {"session_id": "fresh", "source": "clear"})
        self.assertEqual("BLOCKED", checkpoint._load()["status"])
        self.assertIn("BLOCKED", output)
        hygiene = checkpoint._load_hygiene()
        self.assertEqual(0, hygiene["completed_since_clear"])
        self.assertFalse(hygiene["clear_recommended"])

    def test_checkpoint_allows_narrow_install_and_denies_broad_upgrade(self) -> None:
        narrow = {
            "session_id": "s",
            "tool_name": "Bash",
            "tool_input": {"command": "winget install JADX.JADX"},
        }
        broad = {
            "session_id": "s",
            "tool_name": "Bash",
            "tool_input": {"command": "winget upgrade --all"},
        }

        self.assertEqual("", self.capture(checkpoint.pre_tool, narrow))
        blocked = json.loads(self.capture(checkpoint.pre_tool, broad))
        self.assertEqual("deny", blocked["hookSpecificOutput"]["permissionDecision"])

    def test_stop_requires_current_diff_review_and_validation(self) -> None:
        target = self.root / "sample.kt"
        target.write_text("val x = 1\n", encoding="utf-8")
        edit = {"session_id": "s", "tool_name": "Edit", "tool_input": {"file_path": str(target)}}
        harness.post_tool(edit)
        with patch.object(harness, "_run", return_value=(0, "")):
            blocked = json.loads(self.capture(harness.stop, {"session_id": "s"}))
        self.assertEqual("block", blocked["decision"])
        self.assertIn("final diff", blocked["reason"])
        self.assertIn("focused validation", blocked["reason"])

        harness.post_tool({"session_id": "s", "tool_name": "Bash", "tool_input": {"command": "git diff -- sample.kt"}})
        harness.post_tool({"session_id": "s", "tool_name": "Bash", "tool_input": {"command": "python -m unittest sample"}})
        with patch.object(harness, "_run", return_value=(0, "")):
            self.assertEqual("", self.capture(harness.stop, {"session_id": "s"}))
        self.assertTrue(harness._load({"session_id": "s"})["task_complete"])


if __name__ == "__main__":
    unittest.main()
