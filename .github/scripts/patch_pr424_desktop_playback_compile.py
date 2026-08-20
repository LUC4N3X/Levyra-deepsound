from pathlib import Path

path = Path("desktop/player/src/main/kotlin/com/luc4n3x/levyra/desktop/player/VlcAudioPlayer.kt")
text = path.read_text(encoding="utf-8")
replacements = {
    "mediaPlayer.media().startPaused(url, *mediaOptions(resumeAtMs))": "mediaPlayer.media().startPaused(url, *mediaOptions(url, resumeAtMs))",
    "mediaPlayer.media().play(url, *mediaOptions(resumeAtMs))": "mediaPlayer.media().play(url, *mediaOptions(url, resumeAtMs))",
}
for old, new in replacements.items():
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one match for {old!r}, found {count}")
    text = text.replace(old, new, 1)
path.write_text(text, encoding="utf-8")
print("desktop playback compile fix applied")
