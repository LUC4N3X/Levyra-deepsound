from pathlib import Path

path = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeShortsRepositoryTest.kt")
text = path.read_text(encoding="utf-8")
replacements = {
    'assertEquals("Artista seguito shorts", queries.first())': 'assertEquals("Artista seguito #shorts", queries.first())',
    'assertTrue(queries.contains("Artista ascoltato shorts"))': 'assertTrue(queries.contains("Artista ascoltato #shorts"))',
    'assertTrue(queries.contains("musica virale shorts"))': 'assertTrue(queries.contains("musica virale #shorts"))',
}
for old, new in replacements.items():
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one match for {old!r}, found {count}")
    text = text.replace(old, new, 1)
path.write_text(text, encoding="utf-8")
print("Aligned Shorts query tests with hashtag discovery")
