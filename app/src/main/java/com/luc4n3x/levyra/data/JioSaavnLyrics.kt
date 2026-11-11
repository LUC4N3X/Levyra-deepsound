package com.luc4n3x.levyra.data

import kotlin.math.abs
import org.json.JSONObject

internal data class JioSaavnLyricsSong(
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val album: String
)

private const val JIOSAAVN_DURATION_TOLERANCE_SEC = 6L
private const val JIOSAAVN_MAX_LOOKUPS = 2
private const val JIOSAAVN_PLAIN_CONFIDENCE = 58
private val JIOSAAVN_BREAK = Regex("(?i)<br\\s*/?>|</?(?:p|div)(?:\\s[^>]*)?>")
private val JIOSAAVN_TAG = Regex("<[^>]+>")
private val JIOSAAVN_SPACES = Regex("\\s+")
private val JIOSAAVN_ENTITIES = mapOf(
    "&quot;" to "\"",
    "&#039;" to "'",
    "&apos;" to "'",
    "&lt;" to "<",
    "&gt;" to ">",
    "&nbsp;" to " ",
    "&amp;" to "&"
)

internal fun parseJioSaavnLyricsSongs(body: String): List<JioSaavnLyricsSong> {
    val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
    return (0 until results.length()).mapNotNull { index ->
        val song = results.optJSONObject(index) ?: return@mapNotNull null
        val info = song.optJSONObject("more_info") ?: return@mapNotNull null
        val id = song.optString("id").trim()
        if (id.isBlank() || song.optString("type") != "song" || info.optString("has_lyrics") != "true") return@mapNotNull null
        val artists = info.optJSONObject("artistMap")?.optJSONArray("primary_artists")
        val artist = (0 until (artists?.length() ?: 0))
            .mapNotNull { artists?.optJSONObject(it)?.optString("name")?.let(::decodeJioSaavnText)?.takeIf(String::isNotBlank) }
            .joinToString(", ")
        JioSaavnLyricsSong(
            id = id,
            title = decodeJioSaavnText(song.optString("title")),
            artist = artist,
            durationSec = info.optString("duration").toLongOrNull() ?: 0L,
            album = decodeJioSaavnText(info.optString("album"))
        )
    }
}

internal fun jioSaavnShortlist(songs: List<JioSaavnLyricsSong>, durationSec: Long): List<JioSaavnLyricsSong> {
    if (durationSec <= 0L) return songs.take(1)
    return songs
        .filter { it.durationSec <= 0L || abs(it.durationSec - durationSec) <= JIOSAAVN_DURATION_TOLERANCE_SEC }
        .take(JIOSAAVN_MAX_LOOKUPS)
}

internal fun parseJioSaavnLyrics(body: String): LyricsRepository.LyricsResult? {
    val raw = JSONObject(body).optString("lyrics")
    if (raw.isBlank()) return null
    val text = decodeJioSaavnEntities(raw.replace(JIOSAAVN_BREAK, "\n").replace(JIOSAAVN_TAG, "")).trim()
    val lines = UnifiedLyricsParser.parsePlain(text)
    if (lines.none { it.text.isNotBlank() }) return null
    return LyricsRepository.LyricsResult(
        synced = false,
        lines = lines,
        provider = "JioSaavn",
        confidence = JIOSAAVN_PLAIN_CONFIDENCE,
        cached = false
    )
}

private fun decodeJioSaavnText(value: String): String =
    decodeJioSaavnEntities(value.replace(JIOSAAVN_TAG, "")).replace(JIOSAAVN_SPACES, " ").trim()

private fun decodeJioSaavnEntities(value: String): String =
    JIOSAAVN_ENTITIES.entries.fold(value) { text, (entity, replacement) -> text.replace(entity, replacement) }
