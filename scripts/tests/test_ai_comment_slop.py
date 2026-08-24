from __future__ import annotations

import unittest

from scripts.check_ai_comment_slop import scan_diff


class AiCommentSlopTest(unittest.TestCase):
    def test_rejects_process_narration_in_added_source_comments(self) -> None:
        diff = """diff --git a/app/Test.kt b/app/Test.kt
+++ b/app/Test.kt
@@ -1,0 +1,2 @@
+// Step 1: update the state
+val state = 1
"""
        findings = scan_diff(diff)
        self.assertEqual(1, len(findings))
        self.assertIn("Step 1", findings[0])

    def test_allows_non_obvious_contract_comment_and_docs(self) -> None:
        diff = """diff --git a/app/Test.kt b/app/Test.kt
+++ b/app/Test.kt
@@ -1,0 +1,1 @@
+// Media3 requires this callback on the application looper.
diff --git a/docs/note.md b/docs/note.md
+++ b/docs/note.md
@@ -1,0 +1,1 @@
+# Step 1: user documentation
"""
        self.assertEqual([], scan_diff(diff))


if __name__ == "__main__":
    unittest.main()
