from pathlib import Path

APP = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
WORKFLOW = Path(".github/workflows/home-reference-polish.yml")
SELF = Path("scripts/home_reference_polish.py")

text = APP.read_text(encoding="utf-8")

replacements = {
    "private val HOME_COLLECTION_CARD_WIDTH = 154.dp": "private val HOME_COLLECTION_CARD_WIDTH = 164.dp",
    "private val HOME_COLLECTION_CARD_HEIGHT = 140.dp": "private val HOME_COLLECTION_CARD_HEIGHT = 150.dp",
    "private val HOME_COLLECTION_CARD_CORNER = 8.dp": "private val HOME_COLLECTION_CARD_CORNER = 12.dp",
    "private val HOME_COLLECTION_COMPACT_WIDTH = 168.dp": "private val HOME_COLLECTION_COMPACT_WIDTH = 160.dp",
    "private val HOME_COLLECTION_ART_SIZE = 58.dp": "private val HOME_COLLECTION_ART_SIZE = 64.dp",
}

for old, new in replacements.items():
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one match for {old!r}, found {count}")
    text = text.replace(old, new, 1)

marker = "private fun PersonalOrbitSpeedDialCard("
start = text.find(marker)
if start < 0:
    raise RuntimeError("PersonalOrbitSpeedDialCard not found")
end = text.find("\n@Composable", start + len(marker))
if end < 0:
    end = len(text)
block = text[start:end]
old_shape = "val shape = RoundedCornerShape(2.dp)"
if block.count(old_shape) != 1:
    raise RuntimeError("Expected the Personal Orbit 2.dp shape exactly once")
block = block.replace(old_shape, "val shape = RoundedCornerShape(6.dp)", 1)
text = text[:start] + block + text[end:]

APP.write_text(text, encoding="utf-8")

SELF.unlink(missing_ok=True)
WORKFLOW.unlink(missing_ok=True)
