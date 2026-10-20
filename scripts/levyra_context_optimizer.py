#!/usr/bin/env python3
"""Levyra RTK Enhanced — Intelligent Context Optimization & Reversible CCR Engine.

Provides context-aware compression for high-volume coding agent outputs (Gradle,
tests, compiler errors, git diffs, Android logcat, stack traces) paired with
Content-Addressed Reversible Context Compression (CCR) so agents can inspect
the uncompressed original output on-demand without re-running stateful commands.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import threading
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CACHE_DIR = ROOT / ".levyra-tools" / "context-cache"
MAX_CACHE_ENTRIES = 150
MAX_CACHE_BYTES = 25 * 1024 * 1024  # 25 MB


@dataclass
class OptimizationResult:
    category: str
    original_bytes: int
    optimized_bytes: int
    original_tokens: int
    optimized_tokens: int
    reduction_pct: float
    raw_hash: str
    cached_path: str | None
    content: str


def estimate_tokens(text: str) -> int:
    """Deterministic lexical token proxy; not a model-specific tokenizer.

    Counts word clusters and punctuation separately. Benchmarks must label
    these figures as estimates, never billed or model-token counts.
    """
    if not text:
        return 0
    tokens = len(re.findall(r"\w+|[^\w\s]", text))
    return max(tokens, 1)


class ContextCache:
    """Bounded, content-addressed LRU cache for raw command outputs."""

    def __init__(
        self,
        cache_dir: Path | None = None,
        max_entries: int = MAX_CACHE_ENTRIES,
        max_bytes: int = MAX_CACHE_BYTES,
    ) -> None:
        self.cache_dir = cache_dir or DEFAULT_CACHE_DIR
        self.max_entries = max_entries
        self.max_bytes = max_bytes
        self._lock = threading.Lock()
        self._last_ts_ns = 0
        try:
            self.cache_dir.mkdir(parents=True, exist_ok=True)
        except OSError:
            # Fall back to user home if repo directory is not writable
            self.cache_dir = Path.home() / ".levyra" / "context-cache"
            self.cache_dir.mkdir(parents=True, exist_ok=True)

    def _next_timestamp_ns(self) -> int:
        """Return a strictly monotonic nanosecond timestamp for LRU mtime precision."""
        now = time.time_ns()
        if now <= self._last_ts_ns:
            now = self._last_ts_ns + 1
        self._last_ts_ns = now
        return now

    def store(self, raw_text: str, category: str = "generic") -> tuple[str, Path | None]:
        """Store raw text in content-addressed cache, returning (sha256, path_or_None).

        If the content exceeds the total cache budget, or if the cache directory is
        not writable, the content is not cached to prevent cache exhaustion.
        """
        content_bytes = raw_text.encode("utf-8")
        raw_hash = hashlib.sha256(content_bytes).hexdigest()

        # Reject single payload larger than the entire cache budget
        if len(content_bytes) > self.max_bytes:
            return raw_hash, None

        raw_file = self.cache_dir / f"{raw_hash}.raw"
        meta_file = self.cache_dir / f"{raw_hash}.json"

        with self._lock:
            try:
                ts_ns = self._next_timestamp_ns()
                if not raw_file.exists():
                    tmp_raw = self.cache_dir / f"{raw_hash}.raw.tmp.{os.getpid()}_{ts_ns}"
                    tmp_raw.write_bytes(content_bytes)
                    os.replace(tmp_raw, raw_file)
                    try:
                        os.utime(raw_file, ns=(ts_ns, ts_ns))
                    except OSError:
                        pass

                    meta = {
                        "hash": raw_hash,
                        "category": category,
                        "bytes": len(content_bytes),
                        "tokens": estimate_tokens(raw_text),
                        "timestamp": ts_ns / 1_000_000_000.0,
                    }
                    tmp_meta = self.cache_dir / f"{raw_hash}.json.tmp.{os.getpid()}_{ts_ns}"
                    tmp_meta.write_text(json.dumps(meta), encoding="utf-8")
                    os.replace(tmp_meta, meta_file)
                else:
                    try:
                        os.utime(raw_file, ns=(ts_ns, ts_ns))
                    except OSError:
                        pass

                self._prune_if_needed()

                if raw_file.is_file():
                    return raw_hash, raw_file
                return raw_hash, None
            except OSError:
                return raw_hash, None

    def retrieve(
        self,
        hash_prefix: str,
        start_line: int | None = None,
        end_line: int | None = None,
    ) -> str:
        """Retrieve cached raw text by full hash or prefix, with optional slicing."""
        clean_prefix = hash_prefix.strip().lower()
        candidates = list(self.cache_dir.glob(f"{clean_prefix}*.raw"))
        if not candidates:
            raise KeyError(
                f"No cached raw output found matching prefix: {hash_prefix!r}. "
                "The output may have been evicted by LRU budget or exceeded cache limit."
            )
        if len(candidates) > 1:
            raise KeyError(
                f"Ambiguous prefix {hash_prefix!r}: matches {len(candidates)} cached files"
            )

        target = candidates[0]
        text = target.read_text(encoding="utf-8", errors="replace")

        if start_line is not None or end_line is not None:
            lines = text.splitlines(keepends=True)
            s = (start_line - 1) if (start_line and start_line > 0) else 0
            e = end_line if (end_line and end_line > 0) else len(lines)
            return "".join(lines[s:e])

        return text

    def get_stats(self) -> dict[str, int | float]:
        """Return cache occupancy and aggregated token savings."""
        with self._lock:
            raw_files = [f for f in self.cache_dir.glob("*.raw") if f.is_file()]
            meta_files = [f for f in self.cache_dir.glob("*.json") if f.is_file()]
            total_bytes = sum(f.stat().st_size for f in raw_files)
            total_tokens = 0

            for mf in meta_files:
                try:
                    data = json.loads(mf.read_text(encoding="utf-8"))
                    total_tokens += data.get("tokens", 0)
                except (OSError, json.JSONDecodeError):
                    continue

            return {
                "entries": len(raw_files),
                "total_bytes": total_bytes,
                "total_tokens_cached": total_tokens,
            }

    def clear(self) -> int:
        """Clear all cached entries."""
        count = 0
        with self._lock:
            for f in self.cache_dir.glob("*"):
                if f.is_file() and (f.suffix in (".raw", ".json", ".tmp") or ".tmp." in f.name):
                    try:
                        f.unlink()
                        count += 1
                    except OSError:
                        pass
        return count

    def _prune_if_needed(self) -> int:
        """Enforce maximum cache entries and byte budget progressively.

        Deletes ONLY the oldest items necessary to bring the cache within both
        max_entries and max_bytes limits. Synchronizes .raw and .json pairs.
        Returns the number of evicted items.
        """
        raw_files = [f for f in self.cache_dir.glob("*.raw") if f.is_file()]

        # Sweep orphaned .json or stale temp files
        for json_file in self.cache_dir.glob("*.json"):
            if not json_file.with_suffix(".raw").exists():
                try:
                    json_file.unlink()
                except OSError:
                    pass

        now = time.time()
        for tmp_file in self.cache_dir.glob("*.tmp*"):
            try:
                if now - tmp_file.stat().st_mtime > 300:
                    tmp_file.unlink()
            except OSError:
                pass

        entries: list[tuple[Path, int, int]] = []
        for p in raw_files:
            try:
                st = p.stat()
                entries.append((p, st.st_size, st.st_mtime_ns))
            except OSError:
                continue

        current_count = len(entries)
        current_bytes = sum(size for _, size, _ in entries)

        if current_count <= self.max_entries and current_bytes <= self.max_bytes:
            return 0

        # Sort oldest first (smallest mtime_ns)
        entries.sort(key=lambda item: item[2])

        evicted = 0
        for raw_path, file_size, _ in entries:
            if current_count <= self.max_entries and current_bytes <= self.max_bytes:
                break

            meta_path = raw_path.with_suffix(".json")
            try:
                raw_path.unlink(missing_ok=True)
                meta_path.unlink(missing_ok=True)
                current_count -= 1
                current_bytes -= file_size
                evicted += 1
            except OSError:
                pass

        return evicted


# ---------------------------------------------------------------------------
# Specialized Context Filters
# ---------------------------------------------------------------------------

def is_gradle_failure(lines: list[str]) -> bool:
    """Detect whether a Gradle run encountered compilation, execution, or test failures."""
    failure_keywords = (
        "BUILD FAILED",
        "FAILURE: Build failed",
        "* What went wrong:",
        "* Exception is:",
        "Compilation error",
        "TaskExecutionException",
        "Execution failed for task",
        "There were failing tests",
    )
    for line in lines:
        if any(kw in line for kw in failure_keywords):
            return True
        if re.search(r"\be:\s+.*?:\s*\(\d+,\s*\d+\):", line):
            return True
        if re.search(r">\s+Task\s+:\S+\s+FAILED", line):
            return True
    return False


def optimize_gradle_output(text: str) -> str:
    """Compact Gradle builds. Always preserves compiler errors and failure chains."""
    lines = text.splitlines()
    has_failure = is_gradle_failure(lines)

    if not has_failure:
        kept: list[str] = []
        for line in lines:
            if re.match(r"^>\s+Task\s+:\S+\s+(?:UP-TO-DATE|FROM-CACHE|SKIPPED)$", line):
                continue
            if re.match(r"^Daemon will be stopped at the end of the build", line):
                continue
            if re.match(r"^Configuration on demand is an incubating feature", line):
                continue
            if re.match(r"^\s*$", line) and kept and kept[-1] == "":
                continue
            kept.append(line)
        return "\n".join(kept)

    # Failed build: CRITICAL - Keep all compilation errors, stack traces, and failure tasks
    kept_failure: list[str] = []
    in_error_block = False
    skipped_tasks = 0

    for line in lines:
        is_task_line = line.startswith("> Task ")
        is_failed_task = is_task_line and "FAILED" in line
        is_compiler_error = bool(
            re.search(r"\be:\s+", line)
            or re.search(r"\b\[ERROR\]", line)
            or re.search(r":\s*\(\d+,\s*\d+\):", line)
            or "Unresolved reference" in line
            or "Type mismatch" in line
        )
        is_failure_header = (
            "FAILURE: Build failed" in line
            or "* What went wrong:" in line
            or "* Try:" in line
            or "* Exception is:" in line
            or "Execution failed for task" in line
        )
        is_caused_by = line.strip().startswith("Caused by:") or "Exception" in line

        if is_compiler_error or is_failure_header or is_caused_by or is_failed_task:
            in_error_block = True

        if is_task_line and not is_failed_task and not in_error_block:
            skipped_tasks += 1
            continue

        kept_failure.append(line)

    if skipped_tasks > 0:
        kept_failure.insert(0, f"[Levyra Context: omitted {skipped_tasks} intermediate successful tasks]")

    return "\n".join(kept_failure)


def optimize_unit_test_output(text: str) -> str:
    """Compact unit test output. Preserves failing tests and project stack frames."""
    lines = text.splitlines()
    has_failure = any(
        "FAIL" in line or "AssertionError" in line or "ComparisonFailure" in line
        or "There were failing tests" in line
        for line in lines
    )
    if not has_failure:
        kept = [
            line for line in lines
            if not re.match(r"^\s*at\s+org\.gradle\.", line)
            and not re.match(r"^\s*at\s+java\.base\/jdk\.internal\.", line)
        ]
        return "\n".join(kept)

    # Failed tests: filter out external reflection frames, keep project frames
    kept_test: list[str] = []
    omitted_frames = 0

    for line in lines:
        stripped = line.strip()
        is_framework_frame = (
            stripped.startswith("at java.base/jdk.internal.reflect.")
            or stripped.startswith("at java.base/java.lang.reflect.")
            or stripped.startswith("at org.gradle.internal.dispatch.")
            or stripped.startswith("at org.junit.runners.")
        )
        is_project_frame = "com.luc4n3x.levyra" in stripped or "scripts." in stripped
        is_failure_signal = (
            "FAIL" in line
            or "AssertionError" in line
            or "ComparisonFailure" in line
            or stripped.startswith("Expected:")
            or stripped.startswith("Actual:")
            or stripped.startswith("Caused by:")
        )

        if is_failure_signal:
            if omitted_frames > 0:
                kept_test.append(f"    ... [{omitted_frames} framework reflection frames omitted]")
                omitted_frames = 0
            kept_test.append(line)
            continue

        if is_framework_frame and not is_project_frame:
            omitted_frames += 1
            continue

        if omitted_frames > 0:
            kept_test.append(f"    ... [{omitted_frames} framework reflection frames omitted]")
            omitted_frames = 0

        kept_test.append(line)

    if omitted_frames > 0:
        kept_test.append(f"    ... [{omitted_frames} framework reflection frames omitted]")

    return "\n".join(kept_test)


def optimize_git_diff_output(text: str) -> str:
    """Compact git diff. Preserves code hunks verbatim; folds massive generated assets."""
    lines = text.splitlines()
    kept: list[str] = []
    in_generated_file = False
    omitted_lines = 0

    for line in lines:
        if line.startswith("diff --git"):
            if in_generated_file and omitted_lines > 0:
                kept.append(f"[Levyra Context: omitted {omitted_lines} lines of generated asset diff]")
                omitted_lines = 0

            is_baseline_profile = "baseline-prof.txt" in line or "baselineProfiles/" in line
            is_large_lockfile = "gradle.lockfile" in line or "package-lock.json" in line
            in_generated_file = is_baseline_profile or is_large_lockfile

            kept.append(line)
            if in_generated_file:
                kept.append("    [Large generated file diff folded for context efficiency]")
            continue

        if in_generated_file:
            if line.startswith("index ") or line.startswith("--- ") or line.startswith("+++ "):
                kept.append(line)
            else:
                omitted_lines += 1
            continue

        kept.append(line)

    if in_generated_file and omitted_lines > 0:
        kept.append(f"[Levyra Context: omitted {omitted_lines} lines of generated asset diff]")

    return "\n".join(kept)


def optimize_logcat_output(text: str) -> str:
    """Compact Android logcat while preserving crashes, exceptions, and Levyra tags."""
    lines = text.splitlines()
    kept: list[str] = []
    skipped_noise = 0

    crucial_tags = (
        "FATAL EXCEPTION",
        "AndroidRuntime",
        "DEBUG",
        "Levyra",
        "LevyraAudio",
        "Playback",
        "Player",
        "InnerTube",
        "Extractor",
        "Media3",
        "ExoPlayer",
        "Database",
        "Room",
        "Security",
    )

    for line in lines:
        is_crucial = any(tag in line for tag in crucial_tags) or "Caused by:" in line or "Exception" in line
        if is_crucial:
            kept.append(line)
            continue

        if (
            "Choreographer: Skipped" in line
            or "ViewRootImpl" in line
            or "InputMethodManager" in line
            or "BatteryStats" in line
            or "surfaceflinger" in line
        ):
            skipped_noise += 1
            continue

        kept.append(line)

    if skipped_noise > 0:
        kept.append(f"[Levyra Context: omitted {skipped_noise} repetitive system telemetry lines]")

    return "\n".join(kept)


def optimize_stack_trace_output(text: str) -> str:
    """Compact stack traces while preserving top exception, causes, and project frames."""
    lines = text.splitlines()
    kept: list[str] = []
    omitted_frames = 0

    for line in lines:
        stripped = line.strip()
        is_at_frame = stripped.startswith("at ")
        is_project_frame = "com.luc4n3x.levyra" in stripped or "scripts." in stripped
        is_cause = stripped.startswith("Caused by:") or "Exception" in stripped or "Error" in stripped

        if is_cause:
            if omitted_frames > 2:
                kept.append(f"    ... [{omitted_frames - 2} framework frames omitted]")
            omitted_frames = 0
            kept.append(line)
            continue

        if is_at_frame:
            if not is_project_frame:
                omitted_frames += 1
                if omitted_frames <= 2:
                    kept.append(line)
                continue
            else:
                if omitted_frames > 2:
                    kept.append(f"    ... [{omitted_frames - 2} framework frames omitted]")
                omitted_frames = 0
                kept.append(line)
                continue

        kept.append(line)

    if omitted_frames > 2:
        kept.append(f"    ... [{omitted_frames - 2} framework frames omitted]")

    return "\n".join(kept)


def detect_category(text: str) -> str:
    """Classify output to pick the optimal context-aware filter."""
    if "testDebugUnitTest" in text or "There were failing tests" in text or "junit.framework" in text:
        return "unit_test"
    if "BUILD SUCCESSFUL" in text or "BUILD FAILED" in text or "> Task :" in text:
        return "gradle_build"
    if text.startswith("diff --git") or "\ndiff --git" in text:
        return "git_diff"
    if "--------- beginning of" in text or "AndroidRuntime:" in text:
        return "android_logcat"
    if "Caused by:" in text or "Traceback (most recent call last):" in text:
        return "stack_trace"
    return "generic"


def apply_optimization(text: str, category: str) -> str:
    """Route text to the appropriate context-aware filter."""
    if category == "gradle_build":
        return optimize_gradle_output(text)
    if category == "unit_test":
        return optimize_unit_test_output(text)
    if category == "git_diff":
        return optimize_git_diff_output(text)
    if category == "android_logcat":
        return optimize_logcat_output(text)
    if category == "stack_trace":
        return optimize_stack_trace_output(text)
    return text


def _select_context_content(
    raw_text: str,
    category: str,
    raw_hash: str,
    min_savings_pct: float,
) -> str:
    """Return compacted text only when the final agent-visible form saves space."""
    try:
        compacted = apply_optimization(raw_text, category)
    except Exception:
        return raw_text

    if compacted == raw_text:
        return raw_text

    short_hash = raw_hash[:12]
    retrieval_hint = (
        f"\n[Levyra Context: #levyra-raw:{short_hash}; "
        f"retrieve: python scripts/levyra_context_optimizer.py retrieve {short_hash}]"
    )
    candidate = compacted + retrieval_hint
    original_tokens = estimate_tokens(raw_text)
    if (
        original_tokens > 0
        and len(candidate.encode("utf-8")) < len(raw_text.encode("utf-8"))
        and estimate_tokens(candidate) <= original_tokens * (1 - min_savings_pct / 100.0)
    ):
        return candidate
    return raw_text


def _context_result(
    raw_text: str,
    content: str,
    category: str,
    raw_hash: str,
    cached_path: Path | None,
) -> OptimizationResult:
    """Calculate honest, final-form estimates (including retrieval markers)."""
    original_bytes = len(raw_text.encode("utf-8"))
    optimized_bytes = len(content.encode("utf-8"))
    original_tokens = estimate_tokens(raw_text)
    optimized_tokens = estimate_tokens(content)
    reduction = (
        (original_tokens - optimized_tokens) * 100.0 / original_tokens
        if original_tokens else 0.0
    )
    return OptimizationResult(
        category=category,
        original_bytes=original_bytes,
        optimized_bytes=optimized_bytes,
        original_tokens=original_tokens,
        optimized_tokens=optimized_tokens,
        reduction_pct=reduction,
        raw_hash=raw_hash,
        cached_path=str(cached_path) if cached_path else None,
        content=content,
    )


def optimize_context(
    raw_text: str,
    category: str | None = None,
    cache_dir: Path | None = None,
    min_savings_pct: float = 12.0,
) -> OptimizationResult:
    """Compact text only with a retrievable raw copy and meaningful net savings."""
    effective_category = category or detect_category(raw_text)
    raw_hash, cached_path = ContextCache(cache_dir=cache_dir).store(
        raw_text, category=effective_category
    )

    # Never drop diagnostic evidence when storage is unavailable or the text
    # already includes our own compression marker.
    if cached_path is None or "#levyra-raw:" in raw_text or "[Levyra Context:" in raw_text:
        content = raw_text
    else:
        content = _select_context_content(
            raw_text, effective_category, raw_hash, min_savings_pct
        )

    return _context_result(raw_text, content, effective_category, raw_hash, cached_path)


# ---------------------------------------------------------------------------
# Command Execution Wrapper
# ---------------------------------------------------------------------------

def _clean_command_tokens(cmd: list[str]) -> list[str]:
    """Strip optional '--' delimiter from command tokens."""
    if cmd and cmd[0] == "--":
        return cmd[1:]
    return cmd


def _execute_subprocess(cmd: list[str], silent: bool = False) -> tuple[int, str]:
    """Execute subprocess, capturing combined stdout/stderr and handling interrupts."""
    try:
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
    except FileNotFoundError:
        if not silent:
            sys.stderr.write(f"Error: command not found: {cmd[0]}\n")
        return 127, ""
    except OSError as exc:
        if not silent:
            sys.stderr.write(f"Error executing {cmd[0]}: {exc}\n")
        return 1, ""

    try:
        raw_output, _ = proc.communicate()
    except KeyboardInterrupt:
        proc.terminate()
        try:
            proc.wait(timeout=2.0)
        except subprocess.TimeoutExpired:
            proc.kill()
        if not silent:
            sys.stderr.write("\n[Command interrupted by user]\n")
        return 130, ""

    code = proc.returncode
    if code < 0:
        code = 128 + (-code)
    return code, raw_output


def _emit_original_output(
    text: str,
    *,
    cache_dir: Path | None,
    category: str,
    silent: bool,
    cache_enabled: bool,
) -> None:
    """Display unmodified diagnostics, optionally retaining a bounded local copy."""
    if not text:
        return
    if cache_enabled:
        ContextCache(cache_dir=cache_dir).store(text, category=category)
    if not silent:
        sys.stdout.write(text)


def run_command(
    cmd: list[str],
    category: str | None = None,
    raw_mode: bool = False,
    cache_dir: Path | None = None,
    silent: bool = False,
    no_cache: bool = False,
) -> int:
    """Run a finite text command, preserving its exit status and error details."""
    cleaned_cmd = _clean_command_tokens(cmd)
    if not cleaned_cmd:
        if not silent:
            sys.stderr.write("Error: no command provided to run.\n")
        return 2

    exit_code, raw_output = _execute_subprocess(cleaned_cmd, silent=silent)
    if no_cache or exit_code != 0 or raw_mode or not raw_output:
        _emit_original_output(
            raw_output,
            cache_dir=cache_dir,
            category=category or ("failure" if exit_code else "raw"),
            silent=silent,
            cache_enabled=not no_cache,
        )
        return exit_code

    result = optimize_context(raw_output, category=category, cache_dir=cache_dir)
    if not silent:
        sys.stdout.write(
            result.content if result.content.endswith("\n") else f"{result.content}\n"
        )
    return exit_code


# ---------------------------------------------------------------------------
# CLI Interface
# ---------------------------------------------------------------------------

def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Levyra RTK Enhanced Context Optimizer")
    subparsers = parser.add_subparsers(dest="command")

    # Command: run [--category CAT] [--raw] -- <cmd...>
    run_parser = subparsers.add_parser(
        "run",
        help="Run a command, store raw output in CCR, optimize context, and preserve exit code",
    )
    run_parser.add_argument(
        "--category",
        choices=["gradle_build", "unit_test", "git_diff", "android_logcat", "stack_trace", "generic"],
        default=None,
    )
    run_parser.add_argument(
        "--raw",
        action="store_true",
        help="Pass through raw output without compression while still storing in CCR cache",
    )
    run_parser.add_argument(
        "--no-cache",
        action="store_true",
        help="Print raw output without caching (use for sensitive command output)",
    )
    run_parser.add_argument(
        "cmd",
        nargs=argparse.REMAINDER,
        help="Command to execute (e.g. run -- ./gradlew assembleDebug)",
    )

    # Command: process (stdin -> stdout)
    proc_parser = subparsers.add_parser("process", help="Optimize raw command output from stdin")
    proc_parser.add_argument(
        "--category",
        choices=["gradle_build", "unit_test", "git_diff", "android_logcat", "stack_trace", "generic"],
        default=None,
    )
    proc_parser.add_argument(
        "--raw",
        action="store_true",
        help="Pass through raw output while still storing in CCR cache",
    )
    proc_parser.add_argument(
        "--exit-code",
        type=int,
        default=0,
        help="Explicit original process exit code to propagate",
    )
    proc_parser.add_argument(
        "--no-cache",
        action="store_true",
        help="Pass through input without caching or filtering",
    )

    # Command: retrieve <hash>
    ret_parser = subparsers.add_parser("retrieve", help="Retrieve original raw output from CCR cache")
    ret_parser.add_argument("hash", help="Full sha256 or prefix")
    ret_parser.add_argument("--lines", help="Line range, e.g. 10:50")

    # Command: stats
    subparsers.add_parser("stats", help="Display context optimization statistics")

    # Command: clear
    subparsers.add_parser("clear", help="Clear cached raw outputs")

    return parser


def _retrieve_cached_text(args: argparse.Namespace) -> int:
    cache = ContextCache()
    start_line = None
    end_line = None
    if args.lines:
        parts = args.lines.split(":")
        if len(parts) == 2:
            try:
                start_line = int(parts[0]) if parts[0] else None
                end_line = int(parts[1]) if parts[1] else None
            except ValueError:
                sys.stderr.write(f"Invalid lines range: {args.lines}\n")
                return 1
    try:
        sys.stdout.write(cache.retrieve(args.hash, start_line=start_line, end_line=end_line))
        return 0
    except KeyError as exc:
        sys.stderr.write(f"Error: {exc}\n")
        return 1


def _print_cache_stats() -> int:
    cache = ContextCache()
    stats = cache.get_stats()
    print("Levyra Context Optimizer CCR Cache")
    print("==================================")
    print(f"Location:           {cache.cache_dir}")
    print(f"Stored outputs:     {stats['entries']} / {cache.max_entries} max")
    print(f"Total cached bytes: {stats['total_bytes']:,} B / {cache.max_bytes:,} B max")
    print(f"Total tokens seen:  {int(stats['total_tokens_cached']):,} tokens")
    return 0


def _process_stdin(args: argparse.Namespace) -> int:
    raw_input = sys.stdin.read()
    exit_code = getattr(args, "exit_code", 0)
    if not raw_input:
        return exit_code
    if getattr(args, "no_cache", False):
        sys.stdout.write(raw_input)
        return exit_code
    result = optimize_context(raw_input, category=getattr(args, "category", None))
    sys.stdout.write(raw_input if getattr(args, "raw", False) else result.content)
    return exit_code


def main(argv: Sequence[str] | None = None) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")

    args = _build_parser().parse_args(argv)
    if args.command == "run":
        return run_command(
            cmd=args.cmd,
            category=args.category,
            raw_mode=args.raw,
            no_cache=args.no_cache,
        )
    if args.command == "retrieve":
        return _retrieve_cached_text(args)
    if args.command == "stats":
        return _print_cache_stats()
    if args.command == "clear":
        print(f"Cleared {ContextCache().clear()} cached items.")
        return 0
    return _process_stdin(args)


if __name__ == "__main__":
    sys.exit(main())
