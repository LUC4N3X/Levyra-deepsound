from pathlib import Path
import re

repo_path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt")
vm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
test_path = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeMusicAlbumTitleTest.kt")
repo = repo_path.read_text()
vm = vm_path.read_text()


def once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 anchor, found {count}")
    return text.replace(old, new, 1)


def sub_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, lambda _: replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    return updated


anchor = """internal fun levyraIsAlbumLabel(token: String): Boolean {
    return token.trim().lowercase(Locale.ROOT) in LEVYRA_LOCALIZED_ALBUM_LABELS
}
"""
validator = anchor + """
internal fun isPlausibleYoutubeMusicAlbumTitle(value: String): Boolean {
    val normalized = value
        .replace('\\u00A0', ' ')
        .replace('\\u202F', ' ')
        .replace("\\\\n", " ")
        .replace('\\n', ' ')
        .replace('\\r', ' ')
        .replace(Regex("\\\\s+"), " ")
        .trim()
        .lowercase(Locale.ROOT)
    if (normalized.isBlank() || normalized.looksLikeSerializedJson()) return false
    if (normalized.matches(Regex("^\\d+\\s*(?:brani|tracce|canzoni|songs?|tracks?|titres?|lieder|canciones?|faixas?|곡|曲|שירים)$", RegexOption.IGNORE_CASE))) return false
    val metricWords = listOf(
        "view", "visualizz", "riproduzion", "ascolt", "play", "stream",
        "reproduccion", "reproducción", "escucha", "vue", "écoute",
        "aufruf", "wiedergabe", "reprodução", "visualização",
        "просмотр", "прослушив", "再生", "조회수", "스트리밍", "צפיות", "השמעות"
    )
    if (normalized.any(Char::isDigit) && metricWords.any(normalized::contains)) return false
    return !ALBUM_TRACK_METRIC_PATTERN.containsMatchIn(normalized)
}
"""
repo = once(repo, anchor, validator, "insert validator")
repo = once(
    repo,
    "    if (albumKey.isBlank() || artistKey.isBlank()) return LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE",
    "    if (!isPlausibleYoutubeMusicAlbumTitle(album.title) || albumKey.isBlank() || artistKey.isBlank()) return LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE",
    "guard scoring",
)
repo = once(
    repo,
    "                isAlbumLabel(kind) -> {",
    "                isAlbumLabel(kind) && isPlausibleYoutubeMusicAlbumTitle(title) -> {",
    "guard search card",
)
old_filter = "            .filter { it.title.isNotBlank() && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }"
new_filter = "            .filter { isPlausibleYoutubeMusicAlbumTitle(it.title) && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }"
if old_filter in repo:
    if repo.count(old_filter) != 2:
        raise SystemExit(f"home filters: expected 2, found {repo.count(old_filter)}")
    repo = repo.replace(old_filter, new_filter)
elif repo.count(new_filter) < 2:
    raise SystemExit("home filters missing")

repo = sub_once(
    repo,
    r"private fun parseAlbumFromExploreItem\(item: JSONObject\?\): AlbumHit\? \{.*?\n\s*val subtitle = card\.optJSONObject\(\"subtitle\"\)",
    """private fun parseAlbumFromExploreItem(item: JSONObject?): AlbumHit? {
        item ?: return null
        item.optJSONObject("musicResponsiveListItemRenderer")?.let { parseAlbumHit(it)?.let { album -> return album } }
        val card = item.optJSONObject("musicTwoRowItemRenderer") ?: return null
        val title = card.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null
        val subtitle = card.optJSONObject("subtitle")""",
    "guard explore title",
)
repo = sub_once(
    repo,
    r"private fun parseTwoRowAlbumHit\(two: JSONObject\): AlbumHit\? \{\n\s*val title = .*?\n\s*if \(title\.isBlank\(\)\) return null",
    """private fun parseTwoRowAlbumHit(two: JSONObject): AlbumHit? {
        val title = two.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null""",
    "guard two-row title",
)
repo = sub_once(
    repo,
    r"private fun parseAlbumHit\(renderer: JSONObject\): AlbumHit\? \{\n\s*val lines = extractFlexLines\(renderer\)\n\s*val title = lines\.firstOrNull\(\)\?\.takeIf \{ it\.isNotBlank\(\) \} \?: return null\n",
    """private fun parseAlbumHit(renderer: JSONObject): AlbumHit? {
        val lines = extractFlexLines(renderer)
        val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null
""",
    "guard responsive title",
)

resolver = """    private fun albumArtworkIdentityKey(url: String): String = url.trim()
        .lowercase(Locale.ROOT)
        .substringBefore('?')
        .replace(Regex("=w\\d+-h\\d+.*$"), "")
        .replace(Regex("=s\\d+.*$"), "")

    private fun resolveAlbumHit(album: AlbumHit, languageCode: String): AlbumHit {
        val validInputTitle = isPlausibleYoutubeMusicAlbumTitle(album.title)
        if (album.browseId.isNotBlank() && validInputTitle) return album
        val query = when {
            validInputTitle -> album.query.ifBlank { "${album.title} ${album.artist}" }
            album.artist.isNotBlank() -> "${album.artist} album"
            else -> album.query
        }.trim()
        if (query.length < 2) return album.copy(title = album.title.takeIf(::isPlausibleYoutubeMusicAlbumTitle).orEmpty())
        val candidates = searchAlbumHits(query, languageCode, 12)
            .filter { it.browseId.isNotBlank() && isPlausibleYoutubeMusicAlbumTitle(it.title) }
        val titleKey = albumRecommendationTextKey(album.title)
        val artistKey = albumRecommendationTextKey(album.artist)
        val artworkKey = albumArtworkIdentityKey(album.thumbnailUrl)
        val found = candidates.firstOrNull {
            validInputTitle && albumRecommendationTextKey(it.title) == titleKey && albumRecommendationTextKey(it.artist) == artistKey
        } ?: candidates.firstOrNull {
            artworkKey.isNotBlank() && albumArtworkIdentityKey(it.thumbnailUrl) == artworkKey &&
                (artistKey.isBlank() || albumRecommendationTextKey(it.artist) == artistKey)
        } ?: candidates.firstOrNull {
            artistKey.isNotBlank() && albumRecommendationTextKey(it.artist) == artistKey
        } ?: candidates.firstOrNull()
        return found?.let {
            album.copy(
                title = it.title,
                artist = it.artist.ifBlank { album.artist },
                year = it.year.ifBlank { album.year },
                thumbnailUrl = it.thumbnailUrl.ifBlank { album.thumbnailUrl },
                query = it.query.ifBlank { "${it.title} ${it.artist}".trim() },
                browseId = it.browseId,
                artistBrowseId = it.artistBrowseId.ifBlank { album.artistBrowseId }
            )
        } ?: album.copy(title = album.title.takeIf(::isPlausibleYoutubeMusicAlbumTitle).orEmpty())
    }

    private fun parseAlbumHeader"""
repo = sub_once(
    repo,
    r"    private fun resolveAlbumHit\(album: AlbumHit, languageCode: String\): AlbumHit \{.*?\n    \}\n\n    private fun parseAlbumHeader",
    resolver,
    "replace resolver",
)

header = """    private fun parseAlbumHeader(root: JSONObject, fallback: AlbumHit): AlbumHit {
        val detailHeaders = mutableListOf<JSONObject>()
        val responsiveHeaders = mutableListOf<JSONObject>()
        val editableHeaders = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicDetailHeaderRenderer", detailHeaders)
        collectObjectsByKey(root, "musicResponsiveHeaderRenderer", responsiveHeaders)
        collectObjectsByKey(root, "musicEditablePlaylistDetailHeaderRenderer", editableHeaders)
        val headers = detailHeaders + responsiveHeaders + editableHeaders
        fun titleOf(candidate: JSONObject?): String = candidate
            ?.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().cleanLabel()
        val fallbackTitle = fallback.title.cleanLabel().takeIf(::isPlausibleYoutubeMusicAlbumTitle).orEmpty()
        val header = headers.firstOrNull { isPlausibleYoutubeMusicAlbumTitle(titleOf(it)) } ?: headers.firstOrNull()
        val title = titleOf(header).takeIf(::isPlausibleYoutubeMusicAlbumTitle).orEmpty()
            .ifBlank { fallbackTitle }.ifBlank { "Album" }
        val subtitles = listOf(
            header?.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty(),
            header?.optJSONObject("secondSubtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        ).filter(String::isNotBlank).joinToString(" • ")
        val tokens = subtitles.split(" • ", " · ", " - ").map(String::trim).filter(String::isNotBlank)
        val fallbackArtist = fallback.artist.cleanAlbumArtistLabel()
        val parsedArtist = tokens.firstNotNullOfOrNull { token ->
            token.cleanAlbumArtistLabel().takeIf(::isAlbumArtistToken)
        }.orEmpty().ifBlank { fallbackArtist }
        val artistReference = extractYoutubeMusicArtistReference(header, parsedArtist)
        val artist = artistReference?.name.orEmpty().cleanAlbumArtistLabel().ifBlank { parsedArtist }
        val year = tokens.firstNotNullOfOrNull { Regex("\\b(19|20)\\d{2}\\b").find(it)?.value }.orEmpty().ifBlank { fallback.year }
        val thumbnail = header?.let(::findBestThumbnail).orEmpty().ifBlank { fallback.thumbnailUrl }
        return fallback.copy(
            title = title,
            artist = artist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(thumbnail),
            query = "$title ${artist.cleanLabel()}".trim(),
            browseId = fallback.browseId.ifBlank { root.optString("browseId") },
            artistBrowseId = artistReference?.browseId.orEmpty().ifBlank { fallback.artistBrowseId }
        )
    }

    private fun extractAudioPlaylistId"""
repo = sub_once(
    repo,
    r"    private fun parseAlbumHeader\(root: JSONObject, fallback: AlbumHit\): AlbumHit \{.*?\n    \}\n\n    private fun extractAudioPlaylistId",
    header,
    "replace header parser",
)

vm = once(
    vm,
    "import com.luc4n3x.levyra.data.albumRecommendationTextKey",
    "import com.luc4n3x.levyra.data.albumRecommendationTextKey\nimport com.luc4n3x.levyra.data.isPlausibleYoutubeMusicAlbumTitle",
    "import validator",
)
vm = once(
    vm,
    "        val candidates = mergeAlbums(instant, remote)",
    "        val candidates = mergeAlbums(instant, remote).filter { isPlausibleYoutubeMusicAlbumTitle(it.title) }",
    "filter cached albums",
)
vm = once(
    vm,
    """    private fun isUsefulRecommendationAlbum(album: String, trackTitle: String): Boolean {
        val key = albumRecommendationTextKey(album)""",
    """    private fun isUsefulRecommendationAlbum(album: String, trackTitle: String): Boolean {
        if (!isPlausibleYoutubeMusicAlbumTitle(album)) return false
        val key = albumRecommendationTextKey(album)""",
    "filter album seeds",
)

test_path.write_text("""package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicAlbumTitleTest {
    @Test fun rejectsMetrics() {
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("23 Mln riproduzioni"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("232 milioni di ascolti"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("1.2B views"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("16 brani"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("Reproducciones: 24 mil"))
    }

    @Test fun keepsRealTitles() {
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("Alba"))
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("1989"))
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("4:44"))
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("23"))
    }
}
""")
repo_path.write_text(repo)
vm_path.write_text(vm)
