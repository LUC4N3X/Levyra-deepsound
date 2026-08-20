from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")
old = '''                                alpha = if (morphActive) {
                                    0f
                                } else {
                                    playerSwipeContentAlpha(settledSwipeOffset, size.width) *
                                        (1f - immersiveMotionAlpha)
                                }
'''
new = '''                                alpha = if (morphActive || immersiveArtworkEnabled) {
                                    0f
                                } else {
                                    playerSwipeContentAlpha(settledSwipeOffset, size.width)
                                }
'''
count = text.count(old)
if count != 1:
    raise SystemExit(f"expected one immersiveMotionAlpha use, found {count}")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("canvas compile fix applied")
