from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt")
text = path.read_text(encoding="utf-8")
replacements = {
    r'.replace(Regex("\p{M}+"), "")': r'.replace(Regex("""\p{M}+"""), "")',
    r'.replace(Regex("[^\p{L}\p{N}]+"), " ")': r'.replace(Regex("""[^\p{L}\p{N}]+"""), " ")',
    r'.replace(Regex("\s+"), " ")': r'.replace(Regex("""\s+"""), " ")',
}
for old, new in replacements.items():
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Kotlin regex escaping: expected one match for {old!r}, found {count}")
    text = text.replace(old, new, 1)
path.write_text(text, encoding="utf-8")
print("Converted generated Unicode regexes to Kotlin raw strings")
