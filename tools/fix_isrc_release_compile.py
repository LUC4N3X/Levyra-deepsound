from pathlib import Path
import re


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one anchor, found {count}")
    path.write_text(text.replace(old, new, 1))


# Keep artwork matching on the same ISRC-first contract as playback matching.
artwork_path = Path("app/src/main/java/com/luc4n3x/levyra/data/OfficialArtworkRepository.kt")
artwork = artwork_path.read_text()
artwork = artwork.replace(
    "        if (!exactIsrc && artistScore < MIN_ARTIST_MATCH_SCORE) return REJECTED_SCORE\n",
    "        if (artistScore < MIN_ARTIST_MATCH_SCORE) return REJECTED_SCORE\n",
)
artwork = artwork.replace(
    "        if (track.isrc.isNotBlank() && isrc.isNotBlank()) score += if (exactIsrc) 220 else -100\n",
    "",
)
if "exactIsrc" in artwork:
    raise SystemExit("Official artwork matcher still references exactIsrc")
artwork_path.write_text(artwork)


# Replace both album renderer parsers as complete units so release type cannot leak
# from one renderer into another and Kotlin regex escaping stays unambiguous.
yt_path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt")
yt = yt_path.read_text()

two_row = r'''    private fun parseTwoRowAlbumHit(two: JSONObject): AlbumHit? {
        val title = two.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null
        val subtitle = two.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        val tokens = subtitle.split(" • ", " · ", " - ").map { it.trim() }.filter { it.isNotBlank() }
        val kind = tokens.firstOrNull().orEmpty()
        val releaseType = levyraReleaseType(kind)
        if (releaseType == ReleaseType.Unknown) return null
        val artist = tokens.drop(1).firstOrNull { isAlbumArtistToken(it) } ?: return null
        val year = tokens.firstNotNullOfOrNull { Regex("""\b(19|20)\d{2}\b""").find(it)?.value }.orEmpty()
        val thumbnail = findBestThumbnail(two)
        if (thumbnail.isBlank()) return null
        val artistReference = extractYoutubeMusicArtistReference(two, artist)
        val resolvedArtist = artistReference?.name?.ifBlank { artist }.orEmpty().ifBlank { artist }
        return AlbumHit(
            title = title.cleanLabel(),
            artist = resolvedArtist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(thumbnail),
            query = "$title $resolvedArtist",
            browseId = extractAlbumBrowseId(two),
            artistBrowseId = artistReference?.browseId.orEmpty(),
            releaseType = releaseType
        )
    }

    private fun parseAlbumHit'''
yt, count = re.subn(
    r"    private fun parseTwoRowAlbumHit\(two: JSONObject\): AlbumHit\? \{.*?\n    \}\n\n    private fun parseAlbumHit",
    lambda _: two_row,
    yt,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit("parseTwoRowAlbumHit replacement failed")

responsive = r'''    private fun parseAlbumHit(renderer: JSONObject): AlbumHit? {
        val lines = extractFlexLines(renderer)
        val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null
        val tokens = lines.drop(1).flatMap { it.split(" • ", " · ", " - ") }.map { it.trim() }.filter { it.isNotBlank() }
        val kind = tokens.firstOrNull().orEmpty()
        val releaseType = levyraReleaseType(kind)
        if (releaseType == ReleaseType.Unknown) return null
        val artist = tokens.drop(1).firstOrNull { isAlbumArtistToken(it) } ?: return null
        val year = tokens.firstNotNullOfOrNull { Regex("""\b(19|20)\d{2}\b""").find(it)?.value }.orEmpty()
        val thumbnail = findBestThumbnail(renderer)
        if (thumbnail.isBlank()) return null
        val artistReference = extractYoutubeMusicArtistReference(renderer, artist)
        val resolvedArtist = artistReference?.name?.ifBlank { artist }.orEmpty().ifBlank { artist }
        return AlbumHit(
            title = title.cleanLabel(),
            artist = resolvedArtist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(thumbnail),
            query = "$title $resolvedArtist",
            browseId = extractAlbumBrowseId(renderer),
            artistBrowseId = artistReference?.browseId.orEmpty(),
            releaseType = releaseType
        )
    }

    private fun isAlbumLabel'''
yt, count = re.subn(
    r"    private fun parseAlbumHit\(renderer: JSONObject\): AlbumHit\? \{.*?\n    \}\n\n    private fun isAlbumLabel",
    lambda _: responsive,
    yt,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit("parseAlbumHit replacement failed")

yt = yt.replace(
    'Regex("\\b(?:19|20)\\d{2}\\b")',
    'Regex("""\\b(?:19|20)\\d{2}\\b""")',
)
yt_path.write_text(yt)


# Read at most one byte beyond the encrypted payload limit without depending on
# a non-existent Kotlin InputStream.readBytes(limit) overload.
credential_path = Path("app/src/main/java/com/luc4n3x/levyra/data/security/YoutubeMusicCredentialStore.kt")
credential = credential_path.read_text()
old_read = '''        val bytes = runCatching { file.openRead().use { input -> input.readBytes(MAX_FILE_BYTES + 1) } }.getOrNull() ?: return null
'''
new_read = '''        val bytes = runCatching {
            file.openRead().use { input ->
                val buffer = ByteArray(MAX_FILE_BYTES + 1)
                var offset = 0
                while (offset < buffer.size) {
                    val count = input.read(buffer, offset, buffer.size - offset)
                    if (count < 0) break
                    offset += count
                }
                buffer.copyOf(offset)
            }
        }.getOrNull() ?: return null
'''
if credential.count(old_read) != 1:
    raise SystemExit("bounded encrypted payload read anchor not found")
credential_path.write_text(credential.replace(old_read, new_read, 1))
