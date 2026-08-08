from pathlib import Path


def replace_line(path: Path, predicate, replacement: str, label: str) -> None:
    lines = path.read_text(encoding="utf-8").splitlines()
    matches = [index for index, line in enumerate(lines) if predicate(line)]
    if len(matches) != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {len(matches)}")
    lines[matches[0]] = replacement
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


sponsor = Path("app/src/main/java/com/luc4n3x/levyra/data/SponsorBlockRepository.kt")
replace_line(
    sponsor,
    lambda line: "val catsJson = categories.joinToString" in line,
    '        val catsJson = categories.joinToString(",", prefix = "[", postfix = "]") { "\\\"$it\\\"" }',
    "SponsorBlock categories JSON",
)

importer = Path("app/src/main/java/com/luc4n3x/levyra/data/UniversalPlaylistImporter.kt")
replace_line(
    importer,
    lambda line: line.startswith("private val META_TAG_PATTERN = Regex("),
    'private val META_TAG_PATTERN = Regex("""<meta\\b[^>]*>""", RegexOption.IGNORE_CASE)',
    "Spotify meta tag regex",
)
replace_line(
    importer,
    lambda line: line.startswith("private val DECIMAL_HTML_ENTITY = Regex("),
    'private val DECIMAL_HTML_ENTITY = Regex("""&#(\\d+);""")',
    "decimal HTML entity regex",
)
replace_line(
    importer,
    lambda line: '.replace(Regex("\\p{M}+"), "")' in line,
    '    .replace(Regex("""\\p{M}+"""), "")',
    "combining-mark regex",
)
replace_line(
    importer,
    lambda line: '.replace(Regex("[^\\p{L}\\p{N}]+"), " ")' in line,
    '    .replace(Regex("""[^\\p{L}\\p{N}]+"""), " ")',
    "letter-number regex",
)
replace_line(
    importer,
    lambda line: '.replace(Regex("\\s+"), " ")' in line,
    '    .replace(Regex("""\\s+"""), " ")',
    "whitespace regex",
)

source = importer.read_text(encoding="utf-8")
start = source.index("    private suspend fun executeSpotifyRequest(request: Request): Response =")
end = source.index("\n\n    private fun isYoutubeUrl", start)
replacement = '''    private suspend fun executeSpotifyRequest(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = spotifyHttpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!continuation.isActive) {
                    response.close()
                    return
                }
                continuation.resume(response)
            }
        })
    }'''
source = source[:start] + replacement + source[end:]
if "import kotlin.coroutines.resume\n" not in source:
    anchor = "import kotlin.math.abs\n"
    if anchor not in source:
        raise SystemExit("kotlin.math.abs import anchor missing")
    source = source.replace(anchor, anchor + "import kotlin.coroutines.resume\nimport kotlin.coroutines.resumeWithException\n", 1)
importer.write_text(source, encoding="utf-8")

status = Path("app/src/main/java/com/luc4n3x/levyra/ui/i18n/PlaylistImportStatusStrings.kt")
replace_line(
    status,
    lambda line: '"ko" ->' in line and "requestedCount" in line and "importedCount" in line,
    '        "ko" -> "${requestedCount}곡 중 ${importedCount}곡을 ${playlistName}에 가져왔습니다"',
    "Korean partial import message",
)
replace_line(
    status,
    lambda line: '"ko" ->' in line and "최대" in line and "$value" in line,
    '            "ko" -> ": 최대 ${value}곡"',
    "Korean import limit message",
)

for path in (sponsor, importer, status):
    text = path.read_text(encoding="utf-8")
    bad_controls = sorted({ord(ch) for ch in text if ord(ch) < 32 and ch not in "\n\r\t"})
    if bad_controls:
        raise SystemExit(f"{path}: unexpected control characters {bad_controls}")

print("PR 319 Kotlin compile fixes applied")
