#!/usr/bin/env python3
"""Verify the source revision recorded in an Android APK."""

from __future__ import annotations

import argparse
import re
import sys
import zipfile
from pathlib import Path


VERSION_CONTROL_ENTRY = "META-INF/version-control-info.textproto"
COMMIT_PATTERN = re.compile(r"^[0-9a-f]{40}$")
REVISION_PATTERN = re.compile(r'^\s*revision:\s*"([0-9a-f]{40})"\s*$', re.MULTILINE)


class RevisionError(ValueError):
    """Raised when an APK does not record the expected source revision."""


def read_revisions(apk_path: Path) -> tuple[str, ...]:
    try:
        with zipfile.ZipFile(apk_path) as apk:
            payload = apk.read(VERSION_CONTROL_ENTRY)
    except KeyError as error:
        raise RevisionError(f"APK is missing {VERSION_CONTROL_ENTRY}") from error
    except zipfile.BadZipFile as error:
        raise RevisionError(f"Invalid APK archive: {apk_path}") from error

    try:
        text = payload.decode("utf-8")
    except UnicodeDecodeError as error:
        raise RevisionError(f"{VERSION_CONTROL_ENTRY} is not valid UTF-8") from error

    revisions = tuple(REVISION_PATTERN.findall(text))
    if not revisions:
        raise RevisionError(f"{VERSION_CONTROL_ENTRY} contains no full Git revision")
    return revisions


def verify_revision(apk_path: Path, expected_revision: str) -> tuple[str, ...]:
    expected = expected_revision.lower()
    if not COMMIT_PATTERN.fullmatch(expected):
        raise RevisionError("Expected revision must be a full 40-character SHA-1")

    revisions = read_revisions(apk_path)
    mismatches = tuple(revision for revision in revisions if revision != expected)
    if mismatches:
        raise RevisionError(
            f"APK source revision mismatch: expected {expected}, recorded {', '.join(revisions)}"
        )
    return revisions


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--expected-revision", required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        revisions = verify_revision(args.apk, args.expected_revision)
        print(f"APK source revision verified: {', '.join(revisions)}")
        return 0
    except (OSError, RevisionError) as error:
        print(f"APK source revision verification failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
