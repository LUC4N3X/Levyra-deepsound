#!/usr/bin/env python3
"""Reject host-only Java regex features, including inline ``(?U)``, before Android builds."""

from __future__ import annotations

import re
import sys
from pathlib import Path

SOURCE_ROOTS = (Path("app/src/main"), Path("third_party/LevyraNexus/src/main"))
SOURCE_SUFFIXES = {".kt", ".java"}
INLINE_FLAGS = re.compile(r"\(\?([idmsuxU-]+)(?::|\))")
UNSUPPORTED_CONSTANTS = (
    "Pattern.CANON_EQ",
    "Pattern.UNICODE_CHARACTER_CLASS",
)


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    return re.sub(r"//.*", "", text)


def main() -> int:
    findings: list[str] = []
    for root in SOURCE_ROOTS:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if path.suffix not in SOURCE_SUFFIXES:
                continue
            source = strip_comments(path.read_text(encoding="utf-8"))
            for line_number, line in enumerate(source.splitlines(), start=1):
                for match in INLINE_FLAGS.finditer(line):
                    if "U" in match.group(1):
                        findings.append(
                            f"{path}:{line_number}: Android does not support the inline Unicode "
                            f"character-class flag: {match.group(0)}"
                        )
                for constant in UNSUPPORTED_CONSTANTS:
                    if constant in line:
                        findings.append(
                            f"{path}:{line_number}: Android does not support {constant}."
                        )

    if findings:
        print("Android regex compatibility check failed:", file=sys.stderr)
        print("\n".join(f"- {finding}" for finding in findings), file=sys.stderr)
        return 1

    print("Android regex compatibility check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())