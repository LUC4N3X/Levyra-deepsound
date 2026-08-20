from pathlib import Path

path = Path("desktop/player/src/main/kotlin/com/luc4n3x/levyra/desktop/player/VlcAudioPlayer.kt")
text = path.read_text(encoding="utf-8")
old = '''            if (youtube != null) {
                add(":http-reconnect")
                add(":http-continuous")
            }
'''
new = '''            if (youtube != null) {
                add(":http-reconnect")
            }
'''
if text.count(old) != 1:
    raise SystemExit(f"expected one http option block, found {text.count(old)}")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("removed unsafe http-continuous option")
