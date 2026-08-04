from pathlib import Path

path = Path("scripts/refine_player_layout.py")
text = path.read_text(encoding="utf-8")
old = '''app = replace_once(
    app,
    "                                        fontSize = if (compactPlayer) 14.sp else 15.sp,",
    "                                        fontSize = if (compactPlayer) 13.sp else 14.sp,",
    "artist metrics",
)
'''
new = '''app = replace_once(
    app,
    """                                    Text(
                                        text = track.artist,
                                        color = LevyraPlayerDesign.TextSecondary,
                                        fontSize = if (compactPlayer) 14.sp else 15.sp,
""",
    """                                    Text(
                                        text = track.artist,
                                        color = LevyraPlayerDesign.TextSecondary,
                                        fontSize = if (compactPlayer) 13.sp else 14.sp,
""",
    "artist metrics",
)
'''
if text.count(old) != 1:
    raise SystemExit(f"selector patch expected 1 match, found {text.count(old)}")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
