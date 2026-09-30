from pathlib import Path
import re

APP = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
WORKFLOW = Path(".github/workflows/home-ytmusic-style-patch.yml")
SELF = Path("scripts/home_ytmusic_style_patch.py")


def function_bounds(text: str, marker: str) -> tuple[int, int]:
    start = text.find(marker)
    if start < 0:
        raise RuntimeError(f"Missing function marker: {marker}")
    opening = text.find("{", start)
    if opening < 0:
        raise RuntimeError(f"Missing opening brace after: {marker}")

    depth = 0
    i = opening
    state = "code"
    while i < len(text):
        if state == "code":
            if text.startswith('"""', i):
                state = "triple"
                i += 3
                continue
            if text.startswith("//", i):
                state = "line_comment"
                i += 2
                continue
            if text.startswith("/*", i):
                state = "block_comment"
                i += 2
                continue
            if text[i] == '"':
                state = "string"
            elif text[i] == "'":
                state = "char"
            elif text[i] == "{":
                depth += 1
            elif text[i] == "}":
                depth -= 1
                if depth == 0:
                    return start, i + 1
        elif state == "string":
            if text[i] == "\\":
                i += 2
                continue
            if text[i] == '"':
                state = "code"
        elif state == "char":
            if text[i] == "\\":
                i += 2
                continue
            if text[i] == "'":
                state = "code"
        elif state == "triple":
            if text.startswith('"""', i):
                state = "code"
                i += 3
                continue
        elif state == "line_comment":
            if text[i] == "\n":
                state = "code"
        elif state == "block_comment":
            if text.startswith("*/", i):
                state = "code"
                i += 2
                continue
        i += 1
    raise RuntimeError(f"Unclosed function: {marker}")


def rewrite_function(text: str, marker: str, transform) -> str:
    start, end = function_bounds(text, marker)
    original = text[start:end]
    updated = transform(original)
    if updated == original:
        raise RuntimeError(f"No changes made in {marker}")
    return text[:start] + updated + text[end:]


def sharpen_orbit(block: str) -> str:
    pattern = r"val shape = RoundedCornerShape\(\d+(?:\.\d+)?\.dp\)"
    matches = re.findall(pattern, block)
    if len(matches) != 1:
        raise RuntimeError(f"Personal Orbit: expected one card shape, found {len(matches)}")
    return re.sub(pattern, "val shape = RoundedCornerShape(2.dp)", block, count=1)


def inspect_collection(block: str) -> str:
    print("--- HOME_COLLECTION_FUNCTION ---")
    print(block)
    raise RuntimeError("Collection diagnostic complete")


text = APP.read_text(encoding="utf-8")
text = rewrite_function(text, "private fun PersonalOrbitSpeedDialCard(", sharpen_orbit)
text = rewrite_function(text, "private fun HomeEditorialCollectionCard(", inspect_collection)
APP.write_text(text, encoding="utf-8")

SELF.unlink(missing_ok=True)
WORKFLOW.unlink(missing_ok=True)
