#!/usr/bin/env python3
"""Levyra RTK Enhanced — Intelligent Context Optimization & Reversible CCR Engine.

Provides context-aware compression for high-volume coding agent outputs (Gradle,
tests, compiler errors, git diffs, Android logcat, stack traces) paired with
Content-Addressed Reversible Context Compression (CCR) so agents can retrieve
100% of the raw, uncompressed output on-demand without re-running commands.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import time
from dataclasses import asdict, dataclass
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
    cached_path: str
    content: str


def estimate_tokens(text: str) -> int:
    """Heuristic token estimation: ~4 chars per token for code/diagnostics."""
    if not text:
        return 0
    return max(len(text) // 4, len(text.split()))


class ContextCache:
    """Bounded, content-addressed LRU cache for raw command outputs."""

    def __init__(self, cache_dir: Path | None = None) -> None:
        self.cache_dir = cache_dir or DEFAULT_CACHE_DIR
        try:
            self.cache_dir.mkdir(parents=True, exist_ok=True)
        except OSError:
            # Fall back to user home if repo directory is not writable
            self.cache_dir = Path.home() / ".levyra" / "context-cache"
            self.cache_dir.mkdir(parents=True, exist_ok=True)

    def store(self, raw_text: str, category: str = "generic") -> tuple[str, Path]:
        """Store raw text in content-addressed cache, returning (sha256, path)."""
        content_bytes = raw_text.encode("utf-8")
        raw_hash = hashlib.sha256(content_bytes).hexdigest()
        raw_file = self.cache_dir / f"{raw_hash}.raw"
        meta_file = self.cache_dir / f"{raw_hash}.json"

        if not raw_file.exists():
            raw_file.write_bytes(content_bytes)
            meta = {
                "hash": raw_hash,
                "category": category,
                "bytes": len(content_bytes),
                "tokens": estimate_tokens(raw_text),
                "timestamp": time.time(),
            }
            meta_file.write_text(json.dumps(meta), encoding="utf-8")
        else:
            # Touch for LRU order
            try:
                raw_file.touch()
            except OSError:
                pass

        self._prune_if_needed()
        return raw_hash, raw_file

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
            raise KeyError(f"No cached raw output found matching prefix: {hash_prefix!r}")
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
        raw_files = list(self.cache_dir.glob("*.raw"))
        meta_files = list(self.cache_dir.glob("*.json"))
        total_bytes = sum(f.stat().st_size for f in raw_files if f.is_file())
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
        for f in self.cache_dir.glob("*"):
            if f.is_file() and (f.suffix in (".raw", ".json")):
                try:
                    f.unlink()
                    count += 1
                except OSError:
                    pass
        return count

    def _prune_if_needed(self) -> None:
        """Enforce maximum cache entries and byte budget."""
        raw_files = sorted(
            self.cache_dir.glob("*.raw"),
            key=lambda p: p.stat().st_mtime if p.exists() else 0,
        )
        if len(raw_files) <= MAX_CACHE_ENTRIES:
            total_bytes = sum(p.stat().st_size for p in raw_files)
            if total_bytes <= MAX_CACHE_BYTES:
                return

        # Prune oldest until within budget
        for raw_path in raw_files:
            if len(raw_files) <= MAX_CACHE_ENTRIES * 0.7:
                break
            meta_path = raw_path.with_suffix(".json")
            try:
                raw_path.unlink(missing_ok=True)
                meta_path.unlink(missing_ok=True)
            except OSError:
                pass


# ---------------------------------------------------------------------------
# Specialized Adaptive Filters
# ---------------------------------------------------------------------------

def optimize_gradle_output(text: str) -> str:
    """Compact Gradle builds. Always preserves compiler errors and failure chains."""
    lines = text.splitlines()
    has_failure = any("FAILED" in line or "What went wrong:" in line for line in lines)

    if not has_failure:
        # Successful build: strip routine up-to-date and daemon output
        kept: list[str] = []
        for line in lines:
            # Strip noise
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

    # Failed build: CRITICAL - Keep all compilation errors, stack traces, and tasks
    kept_failure: list[str] = []
    in_error_block = False
    skipped_tasks = 0

    for line in lines:
        is_task_line = line.startswith("> Task ")
        is_failed_task = is_task_line and "FAILED" in line
        is_compiler_error = bool(re.search(r"\be:\s+.*?:\s*\(\d+,\s*\d+\):", line))
        is_failure_header = (
            "FAILURE: Build failed" in line
            or "* What went wrong:" in line
            or "* Try:" in line
            or "* Exception is:" in line
        )
        is_caused_by = line.strip().startswith("Caused by:")

        if is_compiler_error or is_failure_header or is_caused_by or is_failed_task:
            in_error_block = True

        if is_task_line and not is_failed_task:
            if not in_error_block:
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
        for line in lines
    )
    if not has_failure:
        # Routine passing output
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
            stripped.startswith("at org.gradle.")
            or stripped.startswith("at org.junit.runners.")
            or stripped.startswith("at org.junit.runner.")
            or stripped.startswith("at java.base/jdk.internal.reflect.")
            or stripped.startswith("at java.base/java.lang.reflect.Method.invoke")
        )
        if is_framework_frame:
            omitted_frames += 1
            continue
        elif omitted_frames > 0:
            kept_test.append(f"    ... [{omitted_frames} framework runner frames omitted]")
            omitted_frames = 0

        kept_test.append(line)

    if omitted_frames > 0:
        kept_test.append(f"    ... [{omitted_frames} framework runner frames omitted]")

    return "\n".join(kept_test)


def optimize_git_diff_output(text: str) -> str:
    """Compact git diff. Preserves source code hunks; summarizes bulk/generated files."""
    chunks = re.split(r"(?=^diff --git\s+)", text, flags=re.MULTILINE)
    if len(chunks) <= 1:
        return text

    optimized_chunks: list[str] = []
    for chunk in chunks:
        if not chunk.strip():
            continue
        match = re.search(r"^diff --git\s+a/(\S+)\s+b/\S+", chunk, flags=re.MULTILINE)
        if not match:
            optimized_chunks.append(chunk)
            continue

        filename = match.group(1).lower()
        is_bulk_generated = (
            filename.endswith("baseline-prof.txt")
            or filename.endswith("baselineprofile.txt")
            or filename.endswith(".lockfile")
            or filename.endswith("package-lock.json")
            or filename.endswith(".min.js")
            or filename.endswith(".min.css")
        )

        if is_bulk_generated:
            lines = chunk.splitlines()
            additions = sum(1 for line in lines if line.startswith("+") and not line.startswith("+++"))
            deletions = sum(1 for line in lines if line.startswith("-") and not line.startswith("---"))
            header_lines = [line for line in lines[:4] if line.startswith("diff ") or line.startswith("index ")]
            summary = "\n".join(header_lines) + f"\n    [Generated/bulk asset: +{additions} -{deletions} lines modified — details cached]"
            optimized_chunks.append(summary)
        else:
            # Code change: keep 100% intact
            optimized_chunks.append(chunk)

    return "\n".join(optimized_chunks)


def optimize_logcat_output(text: str) -> str:
    """Compact Android logcat diagnostics. Preserves crashes and Levyra subsystem logs."""
    lines = text.splitlines()
    kept: list[str] = []
    skipped_noise = 0

    critical_tags = (
        "FATAL EXCEPTION",
        "AndroidRuntime",
        "Levyra",
        "AudioEngine",
        "Media3",
        "ExoPlayer",
        "PlaybackService",
        "InnerTube",
        "Database",
        "Room",
        "Download",
    )

    noise_tags = (
        "Choreographer: Skipped",
        "SurfaceFlinger",
        "GraphicBuffer",
        "OpenGLRenderer",
        "ViewRootImpl",
        "BatteryExternalStatsWorker",
        "WifiMulticast",
    )

    for line in lines:
        is_critical = any(tag in line for tag in critical_tags) or "Exception" in line or "Error" in line
        is_noise = any(noise in line for noise in noise_tags)

        if is_noise and not is_critical:
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
            if omitted_frames > 0:
                kept.append(f"    ... [{omitted_frames} framework frames omitted]")
                omitted_frames = 0
            kept.append(line)
            continue

        if is_at_frame:
            if not is_project_frame:
                omitted_frames += 1
                # Keep first 2 framework frames for context
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
    if "BUILD SUCCESSFUL" in text or "BUILD FAILED" in text or "> Task :" in text:
        return "gradle_build"
    if "testDebugUnitTest" in text or "There were failing tests" in text or "junit.framework" in text:
        return "unit_test"
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


def optimize_context(
    raw_text: str,
    category: str | None = None,
    cache_dir: Path | None = None,
    min_savings_pct: float = 12.0,
) -> OptimizationResult:
    """Optimize raw context with content-addressed reversible caching (CCR).

    If savings < min_savings_pct, returns raw text with cache marker to prevent
    unnecessary alterations.
    """
    cache = ContextCache(cache_dir=cache_dir)
    effective_category = category or detect_category(raw_text)

    # 1. Store raw content safely in content-addressed cache (lossless backup)
    raw_hash, cached_path = cache.store(raw_text, category=effective_category)

    # 2. Apply context-aware compression
    try:
        compressed_text = apply_optimization(raw_text, effective_category)
    except Exception:
        # Fail-open: Never fail an agent turn due to compression errors
        compressed_text = raw_text

    orig_bytes = len(raw_text.encode("utf-8"))
    opt_bytes = len(compressed_text.encode("utf-8"))
    orig_tokens = estimate_tokens(raw_text)
    opt_tokens = estimate_tokens(compressed_text)

    reduction = ((orig_tokens - opt_tokens) / orig_tokens * 100.0) if orig_tokens > 0 else 0.0

    # If compression didn't yield meaningful savings, preserve original
    if reduction < min_savings_pct or opt_bytes >= orig_bytes:
        final_content = raw_text
        opt_bytes = orig_bytes
        opt_tokens = orig_tokens
        reduction = 0.0
    else:
        short_hash = raw_hash[:12]
        footer = (
            f"\n[Levyra Context: {orig_tokens:,} tokens -> {opt_tokens:,} tokens "
            f"(-{reduction:.1f}%). Raw preserved: #levyra-raw:{short_hash} | "
            f"Retrieve: python scripts/levyra_context_optimizer.py retrieve {short_hash}]"
        )
        final_content = compressed_text + footer

    return OptimizationResult(
        category=effective_category,
        original_bytes=orig_bytes,
        optimized_bytes=opt_bytes,
        original_tokens=orig_tokens,
        optimized_tokens=opt_tokens,
        reduction_pct=reduction,
        raw_hash=raw_hash,
        cached_path=str(cached_path),
        content=final_content,
    )


# ---------------------------------------------------------------------------
# CLI Interface
# ---------------------------------------------------------------------------

def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Levyra RTK Enhanced Context Optimizer")
    subparsers = parser.add_subparsers(dest="command")

    # Command: process (stdin -> stdout)
    proc_parser = subparsers.add_parser("process", help="Optimize raw command output from stdin")
    proc_parser.add_argument("--category", choices=["gradle_build", "unit_test", "git_diff", "android_logcat", "stack_trace", "generic"], default=None)
    proc_parser.add_argument("--raw", action="store_true", help="Pass through raw output while still storing in CCR cache")

    # Command: retrieve <hash>
    ret_parser = subparsers.add_parser("retrieve", help="Retrieve original raw output from CCR cache")
    ret_parser.add_argument("hash", help="Full sha256 or prefix")
    ret_parser.add_argument("--lines", help="Line range, e.g. 10:50")

    # Command: stats
    subparsers.add_parser("stats", help="Display context optimization statistics")

    # Command: clear
    subparsers.add_parser("clear", help="Clear cached raw outputs")

    args = parser.parse_args(argv)
    cache = ContextCache()

    if args.command == "retrieve":
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
            content = cache.retrieve(args.hash, start_line=start_line, end_line=end_line)
            sys.stdout.write(content)
            return 0
        except KeyError as exc:
            sys.stderr.write(f"Error: {exc}\n")
            return 1

    elif args.command == "stats":
        stats = cache.get_stats()
        print("Levyra Context Optimizer CCR Cache")
        print("==================================")
        print(f"Location:           {cache.cache_dir}")
        print(f"Stored outputs:     {stats['entries']}")
        print(f"Total cached bytes: {stats['total_bytes']:,} B")
        print(f"Total tokens seen:  {int(stats['total_tokens_cached']):,} tokens")
        return 0

    elif args.command == "clear":
        cleared = cache.clear()
        print(f"Cleared {cleared} cached items.")
        return 0

    elif args.command == "process" or args.command is None:
        raw_input = sys.stdin.read()
        if not raw_input:
            return 0

        category = getattr(args, "category", None)
        result = optimize_context(raw_input, category=category)

        if getattr(args, "raw", False):
            sys.stdout.write(raw_input)
        else:
            sys.stdout.write(result.content)
        return 0

    return 0


if __name__ == "__main__":
    sys.exit(main())
