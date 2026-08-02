from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")
broken = "private const val HOME_HORIZONTAL_ROW_CONTENT_TYPE = HOME_HORIZONTAL_ROW_CONTENT_TYPE"
fixed = 'private const val HOME_HORIZONTAL_ROW_CONTENT_TYPE = "home-horizontal-row"'
if text.count(broken) != 1:
    raise SystemExit(f"Expected one broken constant, found {text.count(broken)}")
text = text.replace(broken, fixed, 1)
path.write_text(text, encoding="utf-8")
