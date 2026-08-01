from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")
needle = "import androidx.compose.foundation.layout.size\n"
replacement = needle + "import androidx.compose.foundation.layout.sizeIn\n"

if "import androidx.compose.foundation.layout.sizeIn\n" in text:
    raise SystemExit("sizeIn import already present")
if text.count(needle) != 1:
    raise SystemExit(f"expected one size import, found {text.count(needle)}")

path.write_text(text.replace(needle, replacement, 1), encoding="utf-8")
