#!/usr/bin/env python3
"""Reject host-only Java regex features, including inline ``(?U)``, before Android builds."""

from __future__ import annotations

import re
import shutil
import subprocess
import sys
from pathlib import Path

SOURCE_ROOTS = (Path("app/src/main"), Path("third_party/LevyraNexus/src/main"))
SOURCE_SUFFIXES = {".kt", ".java"}
INLINE_FLAGS = re.compile(r"\(\?([idmsuxU-]+)(?::|\))")
UNSUPPORTED_CONSTANTS = (
    "Pattern.CANON_EQ",
    "Pattern.UNICODE_CHARACTER_CLASS",
)
ROOT = Path(__file__).resolve().parents[1]
RESOLVER = ROOT / "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt"


def strip_comments(text: str) -> str:
    output: list[str] = []
    index = 0
    state = "code"
    while index < len(text):
        if state == "code":
            if text.startswith("//", index):
                output.extend((" ", " "))
                index += 2
                state = "line_comment"
            elif text.startswith("/*", index):
                output.extend((" ", " "))
                index += 2
                state = "block_comment"
            elif text.startswith('"""', index):
                output.append('"""')
                index += 3
                state = "triple_string"
            elif text[index] == '"':
                output.append(text[index])
                index += 1
                state = "string"
            elif text[index] == "'":
                output.append(text[index])
                index += 1
                state = "char"
            else:
                output.append(text[index])
                index += 1
        elif state == "line_comment":
            if text[index] == "\n":
                output.append("\n")
                state = "code"
            else:
                output.append(" ")
            index += 1
        elif state == "block_comment":
            if text.startswith("*/", index):
                output.extend((" ", " "))
                index += 2
                state = "code"
            else:
                output.append("\n" if text[index] == "\n" else " ")
                index += 1
        elif state == "triple_string":
            if text.startswith('"""', index):
                output.append('"""')
                index += 3
                state = "code"
            else:
                output.append(text[index])
                index += 1
        else:
            current = text[index]
            output.append(current)
            index += 1
            if current == "\\" and index < len(text):
                output.append(text[index])
                index += 1
            elif state == "string" and current == '"':
                state = "code"
            elif state == "char" and current == "'":
                state = "code"
    return "".join(output)


def apply_stabilization_patch() -> bool:
    result = subprocess.run(
        [sys.executable, "scripts/apply_core_stabilization.py"],
        cwd=ROOT,
        check=False,
    )
    if result.returncode != 0:
        print("Playback stabilization patch failed to apply.", file=sys.stderr)
        return False

    artifact_dir = ROOT / "artifacts/core-stabilization"
    artifact_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(RESOLVER, artifact_dir / RESOLVER.name)
    return True


def main() -> int:
    if not apply_stabilization_patch():
        return 1

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
