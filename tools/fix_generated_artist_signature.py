from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/domain/LevyraPersonalOrbit.kt")
text = path.read_text(encoding="utf-8")
old = '''    private fun normalizedArtistSignature(value: String): String {
        val normalized = value.lowercase()
            .replace(Regex("""[^\\p{L}\\p{M}\\p{N}\\s]"""), " ")
            .replace(
                Regex("""(?i)\\b(?:feat|featuring|ft|and|with|e|ed|y|et|und|x)\\b"""),
                " "
            )
            .replace(Regex("""\\s+"""), " ")
            .trim()
        return normalized.split(' ')
            .filter(String::isNotBlank)
            .sorted()
            .joinToString(" ")
    }
'''
new = '''    private fun normalizedArtistSignature(value: String): String {
        return value.lowercase()
            .replace(
                Regex("""(?i)\\b(?:feat|featuring|ft|and|with|e|ed|y|et|und|x)\\b|[,&;/+]"""),
                "|"
            )
            .split('|')
            .map { artist ->
                artist
                    .replace(Regex("""[^\\p{L}\\p{M}\\p{N}\\s]"""), " ")
                    .replace(Regex("""\\s+"""), " ")
                    .trim()
            }
            .filter(String::isNotBlank)
            .sorted()
            .joinToString("|")
    }
'''
count = text.count(old)
if count != 1:
    raise RuntimeError(f"artist signature anchor count: {count}")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("Canonical artist signature corrected.")
