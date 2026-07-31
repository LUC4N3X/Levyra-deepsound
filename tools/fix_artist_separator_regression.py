from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/domain/LevyraPersonalOrbit.kt")
text = path.read_text(encoding="utf-8")
old = '''    private val artistSeparatorPattern = Regex(
        """(?iU)(?:(?<=\\s)(?:feat\\.?|featuring|ft\\.?|and|with|e|ed|y|et|und)(?=\\s)|(?<=[\\p{L}\\p{M}\\p{N}])[,&;+](?=\\s))"""
    )'''
new = '''    private val artistSeparatorPattern = Regex(
        """(?iU)(?:(?<=\\s)(?:feat\\.?|featuring|ft\\.?|and|with|e|ed|y|et|und|[,&;+])(?=\\s)|(?<=[\\p{L}\\p{M}\\p{N}])[,;&+](?=\\s))"""
    )'''
if new not in text:
    if old not in text:
        raise RuntimeError("Expected final artist separator pattern was not found")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
