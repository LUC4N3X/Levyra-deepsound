from __future__ import annotations

import concurrent.futures
import io
from contextlib import redirect_stdout
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from scripts.levyra_context_optimizer import (
    ContextCache,
    estimate_tokens,
    main,
    optimize_context,
    optimize_git_diff_output,
    optimize_gradle_output,
    optimize_logcat_output,
    optimize_stack_trace_output,
    optimize_unit_test_output,
    run_command,
)


class LevyraContextOptimizerCacheTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.cache_dir = Path(self.temp_dir.name)
        self.cache = ContextCache(cache_dir=self.cache_dir, max_entries=150, max_bytes=25 * 1024 * 1024)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_ccr_lossless_storage_and_retrieval(self) -> None:
        raw_payload = "Line 1: important\nLine 2: data\nLine 3: details\nLine 4: end\n"
        raw_hash, path = self.cache.store(raw_payload, category="test")
        self.assertIsNotNone(path)
        assert path is not None
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

    def test_ccr_empty_cache_and_missing_prefix(self) -> None:
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

    def test_cache_exact_149_items(self) -> None:
        for i in range(149):
            self.cache.store(f"item_{i}_payload_data", category="test")
        stats = self.cache.get_stats()
        self.assertEqual(149, stats["entries"])

    def test_cache_exact_150_items(self) -> None:
        for i in range(150):
            self.cache.store(f"item_{i}_payload_data", category="test")
        stats = self.cache.get_stats()
        self.assertEqual(150, stats["entries"])

    def test_cache_exact_151_items_prunes_single_oldest(self) -> None:
        # Store item 0 first
        h0, _ = self.cache.store("item_000_oldest", category="test")
        for i in range(1, 151):
            self.cache.store(f"item_{i:03d}_data", category="test")

        stats = self.cache.get_stats()
        self.assertEqual(150, stats["entries"])

        # Item 0 was the oldest, should have been pruned
        with self.assertRaises(KeyError):
            self.cache.retrieve(h0)

    def test_cache_200_items_prunes_oldest_50_leaving_exact_150(self) -> None:
        hashes: list[str] = []
        for i in range(200):
            h, _ = self.cache.store(f"payload_sequence_{i:03d}", category="test")
            hashes.append(h)

        stats = self.cache.get_stats()
        self.assertEqual(150, stats["entries"])

        # First 50 items should be evicted
        for h in hashes[:50]:
            with self.assertRaises(KeyError):
                self.cache.retrieve(h)

        # Most recent items should be retrievable
        for h in hashes[150:]:
            self.assertTrue(len(self.cache.retrieve(h)) > 0)

    def test_cache_byte_budget_enforcement(self) -> None:
        # Small cache: 1000 bytes max, 150 items max
        small_cache = ContextCache(cache_dir=self.cache_dir, max_entries=150, max_bytes=1000)
        # Store 10 items of 200 bytes each -> 2000 bytes total, must prune to <= 1000
        hashes = []
        for i in range(10):
            payload = f"block_{i:02d}:" + ("X" * 180)  # ~190 bytes
            h, _ = small_cache.store(payload)
            hashes.append(h)

        stats = small_cache.get_stats()
        self.assertLessEqual(stats["total_bytes"], 1000)
        self.assertLess(stats["entries"], 10)

    def test_single_item_exceeding_total_budget_not_cached(self) -> None:
        small_cache = ContextCache(cache_dir=self.cache_dir, max_entries=10, max_bytes=500)
        # Pre-populate with 2 valid items
        h1, p1 = small_cache.store("small_item_1")
        self.assertIsNotNone(p1)

        # Store a 1000-byte item that exceeds 500-byte budget
        huge_payload = "A" * 1000
        raw_hash, path = small_cache.store(huge_payload)
        self.assertIsNone(path)

        # Confirm the huge item was not stored, and previous items were NOT wiped
        self.assertEqual("small_item_1", small_cache.retrieve(h1))
        self.assertEqual(1, small_cache.get_stats()["entries"])

    def test_corrupted_metadata_handled_gracefully(self) -> None:
        h, raw_path = self.cache.store("valid_content")
        assert raw_path is not None
        meta_path = raw_path.with_suffix(".json")
        # Corrupt the JSON metadata
        meta_path.write_text("CORRUPTED_JSON{invalid", encoding="utf-8")

        # get_stats and retrieve must not crash
        stats = self.cache.get_stats()
        self.assertEqual(1, stats["entries"])
        self.assertEqual("valid_content", self.cache.retrieve(h))

    def test_concurrent_writes_thread_safety(self) -> None:
        def worker(idx: int) -> tuple[str, bool]:
            text = f"worker_{idx}_content_" + ("Z" * 50)
            h, p = self.cache.store(text)
            return h, (p is not None and p.is_file())

        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
            futures = [executor.submit(worker, i) for i in range(30)]
            results = [f.result() for f in futures]

        for h, success in results:
            self.assertTrue(success)
            content = self.cache.retrieve(h)
            self.assertIn("worker_", content)


class LevyraContextOptimizerExecutionTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.cache_dir = Path(self.temp_dir.name)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_cli_run_dispatch_preserves_failure_status(self) -> None:
        with patch("scripts.levyra_context_optimizer.DEFAULT_CACHE_DIR", self.cache_dir):
            self.assertEqual(
                3,
                main(["run", "--", sys.executable, "-c", "import sys; sys.exit(3)"]),
            )

    def test_failure_output_is_never_filtered(self) -> None:
        raw = (
            "BUILD FAILED\n"
            "> Task :app:compileDebugKotlin FAILED\n"
            "Error: exact failure evidence\n"
        )
        code = (
            "import sys; "
            f"sys.stdout.write({raw!r}); "
            "sys.exit(2)"
        )
        output = io.StringIO()
        with redirect_stdout(output):
            status = run_command([sys.executable, "-c", code], cache_dir=self.cache_dir)
        self.assertEqual(2, status)
        self.assertEqual(raw, output.getvalue())

    def test_no_cache_mode_does_not_write_private_output(self) -> None:
        output = io.StringIO()
        with patch.object(ContextCache, "store", side_effect=AssertionError("must not cache")):
            with redirect_stdout(output):
                status = run_command(
                    [sys.executable, "-c", "print('private diagnostic')"],
                    cache_dir=self.cache_dir,
                    no_cache=True,
                )
        self.assertEqual(0, status)
        self.assertEqual("private diagnostic\n", output.getvalue())
        self.assertEqual(0, self.cache.get_stats()["entries"])

    def test_run_command_exit_code_0(self) -> None:
        code = run_command([sys.executable, "-c", "import sys; print('hello'); sys.exit(0)"], cache_dir=self.cache_dir)
        self.assertEqual(0, code)

    def test_run_command_exit_code_1(self) -> None:
        code = run_command([sys.executable, "-c", "import sys; print('fail'); sys.exit(1)"], cache_dir=self.cache_dir)
        self.assertEqual(1, code)

    def test_run_command_exit_code_2(self) -> None:
        code = run_command([sys.executable, "-c", "import sys; print('syntax error'); sys.exit(2)"], cache_dir=self.cache_dir)
        self.assertEqual(2, code)

    def test_run_command_exit_code_127_for_missing_binary(self) -> None:
        code = run_command(["non_existent_binary_xyz_12345"], cache_dir=self.cache_dir)
        self.assertEqual(127, code)

    def test_run_command_empty_input(self) -> None:
        code = run_command([], cache_dir=self.cache_dir)
        self.assertEqual(2, code)

    def test_run_command_signal_exit_code_translation(self) -> None:
        with patch("scripts.levyra_context_optimizer.subprocess.Popen") as mock_popen:
            mock_proc = mock_popen.return_value
            mock_proc.communicate.return_value = ("killed by signal", "")
            mock_proc.returncode = -9  # SIGKILL (code < 0)
            code = run_command(["mock_command"], cache_dir=self.cache_dir, silent=True)
            self.assertEqual(137, code)  # 128 + 9 = 137

    def test_run_command_caches_failure_output_in_ccr(self) -> None:
        failed_script = (
            "import sys\n"
            "print('Compilation error at line 42')\n"
            "sys.exit(1)\n"
        )
        code = run_command(
            [sys.executable, "-c", failed_script],
            category="gradle_build",
            cache_dir=self.cache_dir,
        )
        self.assertEqual(1, code)

        cache = ContextCache(cache_dir=self.cache_dir)
        stats = cache.get_stats()
        self.assertEqual(1, stats["entries"])

    def test_anti_double_compression(self) -> None:
        already_compressed = (
            "Some content\n"
            "[Levyra Context: 100 tokens -> 40 tokens (-60.0%). Raw cached: #levyra-raw:abc123456789]"
        )
        res = optimize_context(already_compressed, cache_dir=self.cache_dir)
        self.assertEqual(0.0, res.reduction_pct)
        self.assertEqual(already_compressed, res.content)


class LevyraContextOptimizerFiltersTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.cache_dir = Path(self.temp_dir.name)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_gradle_failure_preserves_all_compiler_errors(self) -> None:
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
        self.assertIn("framework reflection frames omitted", optimized)
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
        self.assertIn("Large generated file diff folded", optimized)
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

    def test_unavailable_cache_always_preserves_entire_output(self) -> None:
        raw = "BUILD SUCCESSFUL\n" + (
            "> Task :app:preBuild UP-TO-DATE\n" * 90
        )
        with patch.object(ContextCache, "store", return_value=("f" * 64, None)):
            result = optimize_context(raw, cache_dir=self.cache_dir)
        self.assertEqual(raw, result.content)
        self.assertIsNone(result.cached_path)
        self.assertEqual(0.0, result.reduction_pct)

    def test_token_estimate_counts_final_footer(self) -> None:
        raw = "BUILD SUCCESSFUL\n" + (
            "> Task :app:preBuild UP-TO-DATE\n" * 90
        )
        result = optimize_context(raw, cache_dir=self.cache_dir)
        self.assertIn("#levyra-raw:", result.content)
        self.assertEqual(estimate_tokens(result.content), result.optimized_tokens)
        self.assertEqual(len(result.content.encode("utf-8")), result.optimized_bytes)
        self.assertGreater(result.reduction_pct, 12.0)

    def test_generic_unrecognized_text_kept_verbatim(self) -> None:
        text = "Hello world, this is a plain message with no special markers.\n"
        res = optimize_context(text, cache_dir=self.cache_dir)
        self.assertEqual(text, res.content)


if __name__ == "__main__":
    unittest.main()
