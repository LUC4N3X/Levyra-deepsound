from pathlib import Path
import re

REPO_PATH = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt")
VM_PATH = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
TEST_PATH = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeMusicAlbumTitleTest.kt")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one anchor, found {count}")
    return text.replace(old, new, 1)


def regex_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, lambda _match: replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"{label}: expected one regex match, found {count}")
    return updated


repo = REPO_PATH.read_text()
vm = VM_PATH.read_text()

validator_anchor = """internal fun levyraIsAlbumLabel(token: String): Boolean {
    return token.trim().lowercase(Locale.ROOT) in LEVYRA_LOCALIZED_ALBUM_LABELS
}
"""
validator_block = validator_anchor + """
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
    if (ALBUM_TITLE_METRIC_PATTERN.matches(normalized)) return false
    if (ALBUM_TRACK_METRIC_PATTERN.containsMatchIn(normalized)) return false
    if (ALBUM_TITLE_TRACK_COUNT_PATTERN.matches(normalized)) return false
    return true
}
"""
repo = replace_once(repo, validator_anchor, validator_block, "insert album title validator")

repo = replace_once(
    repo,
    "    if (albumKey.isBlank() || artistKey.isBlank()) return LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE",
    "    if (!isPlausibleYoutubeMusicAlbumTitle(album.title) || albumKey.isBlank() || artistKey.isBlank()) return LEVYRA_REJECTED_ALBUM_RECOMMENDATION_SCORE",
    "guard album recommendation score",
)

repo = replace_once(
    repo,
    "                isAlbumLabel(kind) -> {",
    "                isAlbumLabel(kind) && isPlausibleYoutubeMusicAlbumTitle(title) -> {",
    "guard search album result",
)

old_filter = "            .filter { it.title.isNotBlank() && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }"
new_filter = "            .filter { isPlausibleYoutubeMusicAlbumTitle(it.title) && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }"
if old_filter in repo:
    if repo.count(old_filter) != 2:
        raise SystemExit(f"home album filters: expected 2, found {repo.count(old_filter)}")
    repo = repo.replace(old_filter, new_filter)
elif repo.count(new_filter) < 2:
    raise SystemExit("home album filters: expected old or already-applied form")

repo = regex_once(
    repo,
    r"(private fun parseAlbumFromExploreItem\(item: JSONObject\?\): AlbumHit\? \{.*?val title = card\.optJSONObject\(\"title\"\)\?\.optJSONArray\(\"runs\"\)\?\.joinText\(\)\.orEmpty\(\)\.trim\(\)\n\s*)if \(title\.isBlank\(\)\) return null",
    """private fun parseAlbumFromExploreItem(item: JSONObject?): AlbumHit? {
        item ?: return null
        item.optJSONObject("musicResponsiveListItemRenderer")?.let { parseAlbumHit(it)?.let { album -> return album } }
        val card = item.optJSONObject("musicTwoRowItemRenderer") ?: return null
        val title = card.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null""",
    "guard explore album title",
)

repo = regex_once(
    repo,
    r"private fun parseTwoRowAlbumHit\(two: JSONObject\): AlbumHit\? \{\n\s*val title = two\.optJSONObject\(\"title\"\)\?\.optJSONArray\(\"runs\"\)\?\.joinText\(\)\.orEmpty\(\)\.trim\(\)\n\s*if \(title\.isBlank\(\)\) return null",
    """private fun parseTwoRowAlbumHit(two: JSONObject): AlbumHit? {
        val title = two.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null""",
    "guard two-row album title",
)

repo = regex_once(
    repo,
    r"private fun parseAlbumHit\(renderer: JSONObject\): AlbumHit\? \{\n\s*val lines = extractFlexLines\(renderer\)\n\s*val title = lines\.firstOrNull\(\)\?\.takeIf \{ it\.isNotBlank\(\) \} \?: return null\n",
    """private fun parseAlbumHit(renderer: JSONObject): AlbumHit? {
        val lines = extractFlexLines(renderer)
        val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        if (!isPlausibleYoutubeMusicAlbumTitle(title)) return null
""",
    "guard responsive album title",
)

resolver_replacement = """    private fun albumArtworkIdentityKey(url: String): String {
        return url.trim()
            .lowercase(Locale.ROOT)
            .substringBefore('?')
            .replace(Regex("=w\\d+-h\\d+.*$"), "")
            .replace(Regex("=s\\d+.*$"), "")
    }

    private fun resolveAlbumHit(album: AlbumHit, languageCode: String): AlbumHit {
        val inputTitleIsPlausible = isPlausibleYoutubeMusicAlbumTitle(album.title)
        if (album.browseId.isNotBlank() && inputTitleIsPlausible) return album

        val query = when {
            inputTitleIsPlausible -> album.query.ifBlank { "${album.title} ${album.artist}" }
            album.artist.isNotBlank() -> "${album.artist} album"
            else -> album.query
        }.trim()
        if (query.length < 2) {
            return album.copy(title = album.title.takeIf(::isPlausibleYoutubeMusicAlbumTitle).orEmpty())
        }

        val candidates = searchAlbumHits(query, languageCode, 12)
            .filter { candidate ->
                candidate.browseId.isNotBlank() && isPlausibleYoutubeMusicAlbumTitle(candidate.title)
            }
        val normalizedTitle = albumRecommendationTextKey(album.title)
        val normalizedArtist = albumRecommendationTextKey(album.artist)
        val artworkKey = albumArtworkIdentityKey(album.thumbnailUrl)
        val exact = candidates.firstOrNull { candidate ->
            inputTitleIsPlausible &&
                albumRecommendationTextKey(candidate.title) == normalizedTitle &&
                albumRecommendationTextKey(candidate.artist) == normalizedArtist
        }
        val sameArtwork = artworkKey.takeIf { it.isNotBlank() }?.let { expectedArtwork ->
            candidates.firstOrNull { candidate ->
                albumArtworkIdentityKey(candidate.thumbnailUrl) == expectedArtwork &&
                    (normalizedArtist.isBlank() || albumRecommendationTextKey(candidate.artist) == normalizedArtist)
            }
        }
        val sameTitle = candidates.firstOrNull { candidate ->
            inputTitleIsPlausible && albumRecommendationTextKey(candidate.title) == normalizedTitle
        }
        val sameArtist = candidates.firstOrNull { candidate ->
            normalizedArtist.isNotBlank() && albumRecommendationTextKey(candidate.artist) == normalizedArtist
        }
        val found = exact ?: sameArtwork ?: sameTitle ?: sameArtist ?: candidates.firstOrNull()
        return found?.let { candidate ->
            album.copy(
                title = candidate.title,
                artist = candidate.artist.ifBlank { album.artist },
                year = candidate.year.ifBlank { album.year },
                thumbnailUrl = candidate.thumbnailUrl.ifBlank { album.thumbnailUrl },
                query = candidate.query.ifBlank { "${candidate.title} ${candidate.artist}".trim() },
                browseId = candidate.browseId,
                artistBrowseId = candidate.artistBrowseId.ifBlank { album.artistBrowseId }
            )
        } ?: album.copy(title = album.title.takeIf(::isPlausibleYoutubeMusicAlbumTitle).orEmpty())
    }

    private fun parseAlbumHeader"""
repo = regex_once(
    repo,
    r"    private fun resolveAlbumHit\(album: AlbumHit, languageCode: String\): AlbumHit \{.*?\n    \}\n\n    private fun parseAlbumHeader",
    resolver_replacement,
    "replace album resolver",
)

header_replacement = """    private fun parseAlbumHeader(root: JSONObject, fallback: AlbumHit): AlbumHit {
        val detailHeaders = mutableListOf<JSONObject>()
        val responsiveHeaders = mutableListOf<JSONObject>()
        val editableHeaders = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicDetailHeaderRenderer", detailHeaders)
        collectObjectsByKey(root, "musicResponsiveHeaderRenderer", responsiveHeaders)
        collectObjectsByKey(root, "musicEditablePlaylistDetailHeaderRenderer", editableHeaders)
        val headers = detailHeaders + responsiveHeaders + editableHeaders

        fun titleOf(candidate: JSONObject?): String = candidate
            ?.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.joinText()
            .orEmpty()
            .cleanLabel()

        val safeFallbackTitle = fallback.title.cleanLabel()
            .takeIf(::isPlausibleYoutubeMusicAlbumTitle)
            .orEmpty()
        val header = headers.firstOrNull { candidate ->
            isPlausibleYoutubeMusicAlbumTitle(titleOf(candidate))
        } ?: headers.firstOrNull()
        val title = titleOf(header)
            .takeIf(::isPlausibleYoutubeMusicAlbumTitle)
            .orEmpty()
            .ifBlank { safeFallbackTitle }
            .ifBlank { "Album" }
        val subtitles = listOf(
            header?.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty(),
            header?.optJSONObject("secondSubtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        ).filter { it.isNotBlank() }.joinToString(" • ")
        val tokens = subtitles.split(" • ", " · ", " - ").map { it.trim() }.filter { it.isNotBlank() }
        val fallbackArtist = fallback.artist.cleanAlbumArtistLabel()
        val parsedArtist = tokens.firstNotNullOfOrNull { token ->
            token.cleanAlbumArtistLabel().takeIf { cleaned -> isAlbumArtistToken(cleaned) }
        }.orEmpty().ifBlank { fallbackArtist }
        val artistReference = extractYoutubeMusicArtistReference(header, parsedArtist)
        val artist = artistReference?.name.orEmpty().cleanAlbumArtistLabel().ifBlank { parsedArtist }
        val year = tokens.firstNotNullOfOrNull { Regex("\\b(19|20)\\d{2}\\b").find(it)?.value }.orEmpty().ifBlank { fallback.year }
        val thumbnail = header?.let { findBestThumbnail(it) }.orEmpty().ifBlank { fallback.thumbnailUrl }
        val browseId = fallback.browseId.ifBlank { root.optString("browseId") }
        return fallback.copy(
            title = title,
            artist = artist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(thumbnail),
            query = "$title ${artist.cleanLabel()}".trim(),
            browseId = browseId,
            artistBrowseId = artistReference?.browseId.orEmpty().ifBlank { fallback.artistBrowseId }
        )
    }

    private fun extractAudioPlaylistId"""
repo = regex_once(
    repo,
    r"    private fun parseAlbumHeader\(root: JSONObject, fallback: AlbumHit\): AlbumHit \{.*?\n    \}\n\n    private fun extractAudioPlaylistId",
    header_replacement,
    "replace album header parser",
)

metric_anchor = """private val ALBUM_TRACK_METRIC_PATTERN = Regex(
    "(?:^|\\s)[\\d.,]+\\s*(?:k|m|mln|mil|mio|mrd|bn|b|milioni?|miliardi?|millions?|billions?)?\\s*(?:views?|visualizzazioni?|riproduzioni?|ascolti?|plays?|streams?)\\b",
    RegexOption.IGNORE_CASE
)"""
metric_block = """private val ALBUM_TITLE_METRIC_PATTERN = Regex(
    """ + '"""' + """^(?:[\\d.,]+\\s*(?:k|m|mln|mil|mio|mrd|bn|b|milioni?|miliardi?|millions?|billions?)?\\s*(?:(?:di|of)\\s+)?(?:views?|visualizzazioni?|riproduzioni?|ascolti?|plays?|streams?|reproducciones?|escuchas?|vues?|écoutes?|aufrufe?|wiedergaben?|reproduções?|visualizações?|просмотров?|прослушиваний?)|(?:views?|visualizzazioni?|riproduzioni?|ascolti?|plays?|streams?|reproducciones?|escuchas?|vues?|écoutes?|aufrufe?|wiedergaben?|reproduções?|visualizações?|просмотров?|прослушиваний?)\\s*[:\\-]?\\s*[\\d.,]+\\s*(?:k|m|mln|mil|mio|mrd|bn|b|milioni?|miliardi?|millions?|billions?)?|[\\d.,]+\\s*(?:万|億|만)?\\s*(?:再生(?:回数)?|回再生|조회수|스트리밍|צפיות|השמעות))$""" + '"""' + """,
    RegexOption.IGNORE_CASE
)

private val ALBUM_TITLE_TRACK_COUNT_PATTERN = Regex(
    """ + '"""' + """^\\d+\\s*(?:brani|tracce|canzoni|songs?|tracks?|titres?|lieder|canciones?|faixas?|곡|曲|שירים)$""" + '"""' + """,
    RegexOption.IGNORE_CASE
)

private val ALBUM_TRACK_METRIC_PATTERN = Regex(
    "(?:^|\\s)[\\d.,]+\\s*(?:k|m|mln|mil|mio|mrd|bn|b|milioni?|miliardi?|millions?|billions?)?\\s*(?:views?|visualizzazioni?|riproduzioni?|ascolti?|plays?|streams?)\\b",
    RegexOption.IGNORE_CASE
)"""
repo = replace_once(repo, metric_anchor, metric_block, "insert album metric patterns")

vm = replace_once(
    vm,
    "import com.luc4n3x.levyra.data.albumRecommendationTextKey",
    "import com.luc4n3x.levyra.data.albumRecommendationTextKey\nimport com.luc4n3x.levyra.data.isPlausibleYoutubeMusicAlbumTitle",
    "import album title validator",
)
vm = replace_once(
    vm,
    "        val candidates = mergeAlbums(instant, remote)",
    "        val candidates = mergeAlbums(instant, remote).filter { album -> isPlausibleYoutubeMusicAlbumTitle(album.title) }",
    "filter cached album recommendations",
)
vm = replace_once(
    vm,
    """    private fun isUsefulRecommendationAlbum(album: String, trackTitle: String): Boolean {
        val key = albumRecommendationTextKey(album)""",
    """    private fun isUsefulRecommendationAlbum(album: String, trackTitle: String): Boolean {
        if (!isPlausibleYoutubeMusicAlbumTitle(album)) return false
        val key = albumRecommendationTextKey(album)""",
    "filter invalid album seed",
)

TEST_PATH.write_text(
    """package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicAlbumTitleTest {
    @Test
    fun rejectsPlaybackMetricsAndTrackCountsAsAlbumTitles() {
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("23 Mln riproduzioni"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("232 milioni di ascolti"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("1.2B views"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("16 brani"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("Reproducciones: 24 mil"))
    }

    @Test
    fun keepsRealAlbumTitlesIncludingNumericOnes() {
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("Alba"))
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("1989"))
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("4:44"))
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("23"))
    }
}
"""
)

REPO_PATH.write_text(repo)
VM_PATH.write_text(vm)
