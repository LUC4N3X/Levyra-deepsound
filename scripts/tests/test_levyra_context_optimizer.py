from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from scripts.levyra_context_optimizer import (
    ContextCache,
    estimate_tokens,
    optimize_context,
    optimize_git_diff_output,
    optimize_gradle_output,
    optimize_logcat_output,
    optimize_stack_trace_output,
    optimize_unit_test_output,
)


class LevyraContextOptimizerTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.cache_dir = Path(self.temp_dir.name)
        self.cache = ContextCache(cache_dir=self.cache_dir)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_ccr_lossless_storage_and_retrieval(self) -> None:
        raw_payload = "Line 1: important\nLine 2: data\nLine 3: details\nLine 4: end\n"
        raw_hash, path = self.cache.store(raw_payload, category="test")
        self.assertTrue(path.is_file())

        # Exact retrieval by full hash
        retrieved_full = self.cache.retrieve(raw_hash)
        self.assertEqual(raw_payload, retrieved_full)

        # Exact retrieval by prefix
        retrieved_prefix = self.cache.retrieve(raw_hash[:8])
        self.assertEqual(raw_payload, retrieved_prefix)

        # Slice retrieval by line range
        slice_retrieved = self.cache.retrieve(raw_hash[:8], start_line=2, end_line=3)
        self.assertEqual("Line 2: data\nLine 3: details\n", slice_retrieved)

    def test_ccr_missing_and_ambiguous_prefix(self) -> None:
        with self.assertRaises(KeyError):
            self.cache.retrieve("nonexistenthash123")

    def test_cache_stats_and_clear(self) -> None:
        self.cache.store("test output 1")
        self.cache.store("test output 2")
        stats = self.cache.get_stats()
        self.assertEqual(2, stats["entries"])
        self.assertGreater(stats["total_bytes"], 0)

        cleared = self.cache.clear()
        self.assertEqual(4, cleared)  # 2 .raw + 2 .json
        self.assertEqual(0, self.cache.get_stats()["entries"])

    def test_gradle_failure_preserves_compiler_errors_and_tasks(self) -> None:
        build_output = (
            "> Task :app:preBuild UP-TO-DATE\n"
            "> Task :app:compileDebugKotlin\n"
            "e: /path/PlayerDeckSheet.kt: (112, 28): Unresolved reference: prismState\n"
            "> Task :app:compileDebugKotlin FAILED\n\n"
            "FAILURE: Build failed with an exception.\n\n"
            "* What went wrong:\n"
            "Execution failed for task ':app:compileDebugKotlin'.\n"
        )
        optimized = optimize_gradle_output(build_output)
        self.assertIn("PlayerDeckSheet.kt: (112, 28): Unresolved reference: prismState", optimized)
        self.assertIn("> Task :app:compileDebugKotlin FAILED", optimized)
        self.assertIn("FAILURE: Build failed with an exception.", optimized)
        self.assertIn("* What went wrong:", optimized)

    def test_gradle_success_strips_noise_and_keeps_summary(self) -> None:
        build_output = (
            "Configuration on demand is an incubating feature.\n"
            "> Task :app:preBuild UP-TO-DATE\n"
            "> Task :app:preDebugBuild UP-TO-DATE\n"
            "> Task :app:compileDebugKotlin\n"
            "BUILD SUCCESSFUL in 12s\n"
            "34 actionable tasks: 1 executed, 33 up-to-date\n"
        )
        optimized = optimize_gradle_output(build_output)
        self.assertNotIn("UP-TO-DATE", optimized)
        self.assertNotIn("incubating feature", optimized)
        self.assertIn("> Task :app:compileDebugKotlin", optimized)
        self.assertIn("BUILD SUCCESSFUL in 12s", optimized)

    def test_unit_test_failure_preserves_assertions_and_condenses_framework(self) -> None:
        test_output = (
            "com.luc4n3x.levyra.domain.PlaybackSpeedTest > speedClamping FAILED\n"
            "    java.lang.AssertionError: expected:<2.0> but was:<3.5>\n"
            "        at org.junit.Assert.fail(Assert.java:89)\n"
            "        at com.luc4n3x.levyra.domain.PlaybackSpeedTest.speedClamping(PlaybackSpeedTest.kt:34)\n"
            "        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
            "        at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)\n"
            "        at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)\n"
        )
        optimized = optimize_unit_test_output(test_output)
        self.assertIn("speedClamping FAILED", optimized)
        self.assertIn("java.lang.AssertionError: expected:<2.0> but was:<3.5>", optimized)
        self.assertIn("com.luc4n3x.levyra.domain.PlaybackSpeedTest.speedClamping", optimized)
        self.assertIn("framework runner frames omitted", optimized)
        self.assertNotIn("NativeMethodAccessorImpl", optimized)

    def test_git_diff_preserves_code_hunks_and_condenses_generated(self) -> None:
        diff_output = (
            "diff --git a/app/Player.kt b/app/Player.kt\n"
            "--- a/app/Player.kt\n"
            "+++ b/app/Player.kt\n"
            "@@ -10,3 +10,4 @@\n"
            "+val newFeature = true\n"
            "diff --git a/app/baseline-prof.txt b/app/baseline-prof.txt\n"
            "--- a/app/baseline-prof.txt\n"
            "+++ b/app/baseline-prof.txt\n"
            "@@ -1,3 +1,3 @@\n"
            "+Lcom/method1;\n"
            "+Lcom/method2;\n"
        )
        optimized = optimize_git_diff_output(diff_output)
        self.assertIn("diff --git a/app/Player.kt b/app/Player.kt", optimized)
        self.assertIn("+val newFeature = true", optimized)
        self.assertIn("Generated/bulk asset", optimized)
        self.assertNotIn("+Lcom/method1;", optimized)

    def test_logcat_preserves_crashes_and_subsystem_tags(self) -> None:
        logcat_output = (
            "10-20 14:10:02.112  1245  1245 D LevyraPlayer: Prepared track\n"
            "10-20 14:10:02.400  1245  1245 I Choreographer: Skipped 48 frames!\n"
            "10-20 14:10:02.430  1245  1245 E AndroidRuntime: FATAL EXCEPTION: main\n"
            "10-20 14:10:02.430  1245  1245 E AndroidRuntime: java.lang.NullPointerException: test crash\n"
        )
        optimized = optimize_logcat_output(logcat_output)
        self.assertIn("LevyraPlayer: Prepared track", optimized)
        self.assertIn("FATAL EXCEPTION: main", optimized)
        self.assertIn("java.lang.NullPointerException: test crash", optimized)
        self.assertNotIn("Choreographer: Skipped", optimized)
        self.assertIn("omitted 1 repetitive system telemetry lines", optimized)

    def test_stack_trace_preserves_root_causes(self) -> None:
        trace = (
            "AppException: top failure\n"
            "	at com.luc4n3x.levyra.MainActivity.onCreate(MainActivity.kt:15)\n"
            "	at android.app.Activity.performCreate(Activity.java:8000)\n"
            "	at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1300)\n"
            "	at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3400)\n"
            "Caused by: RootException: actual cause\n"
            "	at com.luc4n3x.levyra.Engine.init(Engine.kt:40)\n"
        )
        optimized = optimize_stack_trace_output(trace)
        self.assertIn("AppException: top failure", optimized)
        self.assertIn("com.luc4n3x.levyra.MainActivity.onCreate", optimized)
        self.assertIn("Caused by: RootException: actual cause", optimized)
        self.assertIn("com.luc4n3x.levyra.Engine.init", optimized)

    def test_fail_open_on_exception(self) -> None:
        raw_text = "Some raw text"
        with patch("scripts.levyra_context_optimizer.apply_optimization", side_effect=ValueError("boom")):
            res = optimize_context(raw_text, cache_dir=self.cache_dir)
            self.assertEqual(raw_text, res.content)

    def test_empty_output_safety(self) -> None:
        res = optimize_context("", cache_dir=self.cache_dir)
        self.assertEqual("", res.content)

    def test_non_repeatable_command_cached_immediately(self) -> None:
        stateful_output = "MIGRATION_RUN_ID=98124_APPLIED_TRANSACTION"
        res = optimize_context(stateful_output, cache_dir=self.cache_dir)
        retrieved = self.cache.retrieve(res.raw_hash[:8])
        self.assertEqual(stateful_output, retrieved)


if __name__ == "__main__":
    unittest.main()
