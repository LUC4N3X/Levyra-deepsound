from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/theme/HomeDesign.kt")
text = path.read_text(encoding="utf-8")
old = "val HeroHeight: Dp = 220.dp"
new = "val HeroHeight: Dp = 240.dp"
if old not in text:
    raise RuntimeError("Expected staged Home hero height not found")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
