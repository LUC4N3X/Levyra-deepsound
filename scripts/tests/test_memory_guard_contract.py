from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class MemoryGuardContractTest(unittest.TestCase):
    def test_android_memory_guard_is_scoped_and_concrete(self) -> None:
        text = (ROOT / "app" / "AGENTS.md").read_text(encoding="utf-8")

        for term in (
            "Memory regression guard",
            "issue #427",
            "Regex",
            ".toRegex()",
            "native heap",
            "PSS/RSS",
            "20–30 minutes",
            "monotonic climb",
            "System.gc()",
            "video decoder",
        ):
            with self.subTest(term=term):
                self.assertIn(term, text)

    def test_player_skill_preserves_memory_stability(self) -> None:
        text = (
            ROOT / ".agents" / "skills" / "levyra-player" / "SKILL.md"
        ).read_text(encoding="utf-8")

        for term in (
            "Playback memory invariant",
            "Issue #427",
            "Regex",
            "ByteBuffer",
            "bounded plateau",
            "levyra-android-performance",
            "native heap",
            "PSS/RSS",
        ):
            with self.subTest(term=term):
                self.assertIn(term, text)


if __name__ == "__main__":
    unittest.main()
