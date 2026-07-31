from pathlib import Path

path = Path("tools/levyra-editorial/tests/test_youtube_music.py")
text = path.read_text(encoding="utf-8")
old = (
    "def test_resolve_skips_web_video_query_when_audio_identity_is_missing("
    "monkeypatch: pytest.MonkeyPatch) -> None:\n"
)
new = (
    "def test_resolve_skips_web_video_query_when_audio_identity_is_missing(\n"
    "    monkeypatch: pytest.MonkeyPatch,\n"
    ") -> None:\n"
)
if new not in text:
    if old not in text:
        raise RuntimeError("Expected final YouTube Music test signature was not found")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
