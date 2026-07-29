from __future__ import annotations

from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
content = path.read_text(encoding="utf-8")
old = """        contentPadding = PaddingValues(top = 8.dp, bottom = if (state.currentTrack != null) 188.dp else 104.dp),
"""
new = """        // Keep the LazyColumn geometry unchanged when the mini player appears or disappears.
        // Changing bottom padding here made Compose re-anchor the visible Top 50 content.
        contentPadding = PaddingValues(top = 8.dp, bottom = 188.dp),
"""
if content.count(old) != 1:
    raise SystemExit(f"Expected exactly one dynamic Home bottom-padding expression, found {content.count(old)}")
path.write_text(content.replace(old, new, 1), encoding="utf-8")
print("Home scroll stability patch applied")
# Trigger after workflow registration.
