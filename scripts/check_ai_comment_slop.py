#!/usr/bin/env python3
"""Reject newly added AI-narration comments without policing legacy source."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_SUFFIXES = {
    ".c", ".cc", ".cpp", ".cs", ".go", ".h", ".hpp", ".java", ".js", ".jsx",
    ".kt", ".kts", ".py", ".rs", ".sh", ".swift", ".ts", ".tsx", ".ps1",
}
COMMENT_RE = re.compile(r"^\s*(?://+|#|/\*+|\*+)\s*(.+?)\s*(?:\*/)?$")
SLOP_RE = re.compile(
    r"^(?:step\s*\d+\b|now\s+(?:we|let'?s)\b|here\s+we\b|"
    r"first,?\s+we\b|next,?\s+we\b|finally,?\s+we\b|"
    r"we\s+(?:need to|will|are going to|can now)\b|"
    r"let'?s\s+(?:now\s+)?(?:add|update|create|fix|handle|check|implement)\b)",
    re.I,
)


def scan_diff(diff: str) -> list[str]:
    findings: list[str] = []
    current = ""
    added_line = 0
    for line in diff.splitlines():
        if line.startswith("+++ b/"):
            current = line[6:]
            added_line = 0
            continue
        if line.startswith("@@"):
            match = re.search(r"\+(\d+)", line)
            added_line = int(match.group(1)) - 1 if match else 0
            continue
        if line.startswith("+") and not line.startswith("+++"):
            added_line += 1
            if Path(current).suffix.lower() not in SOURCE_SUFFIXES:
                continue
            body = line[1:]
            match = COMMENT_RE.match(body)
            if match and SLOP_RE.search(match.group(1).strip()):
                findings.append(f"{current}:{added_line}: AI-narration comment: {match.group(1).strip()}")
        elif not line.startswith("-"):
            added_line += 1
    return findings


def _git(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=ROOT,
        check=False,
        text=True,
        encoding="utf-8",
        errors="replace",
        capture_output=True,
        timeout=30,
    )
    return result.stdout if result.returncode == 0 else ""


def repository_diff(working_tree_only: bool) -> str:
    pieces = [_git("diff", "--unified=0"), _git("diff", "--cached", "--unified=0")]
    if not working_tree_only:
        base = ""
        for candidate in ("origin/main", "main", "HEAD~1"):
            probe = _git("rev-parse", "--verify", f"{candidate}^{{commit}}").strip()
            if not probe:
                continue
            merge_base = _git("merge-base", "HEAD", probe).strip()
            if merge_base:
                base = merge_base
                break
        if base:
            pieces.insert(0, _git("diff", "--unified=0", f"{base}...HEAD"))
    return "\n".join(pieces)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--working-tree", action="store_true")
    args = parser.parse_args()
    findings = sorted(set(scan_diff(repository_diff(args.working_tree))))
    if findings:
        print("AI comment slop check failed:", file=sys.stderr)
        for finding in findings:
            print(f"- {finding}", file=sys.stderr)
        return 1
    print("AI comment slop check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
