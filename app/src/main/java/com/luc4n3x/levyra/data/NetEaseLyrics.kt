package com.luc4n3x.levyra.data

import kotlin.math.abs
import org.json.JSONObject

internal data class NetEaseSong(
    val id: Long,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val album: String
)

private const val NETEASE_DURATION_TOLERANCE_SEC = 6L
private const val NETEASE_MAX_LOOKUPS = 3
private const val NETEASE_SYNCED_CONFIDENCE = 86
private const val NETEASE_PLAIN_CONFIDENCE = 62
private val NETEASE_TIMESTAMP = Regex("\\[\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?]")
private val NETEASE_INSTRUMENTAL_MARKERS = listOf("纯音乐，请欣赏", "纯音乐, 请欣赏", "此歌曲为没有填词的纯音乐")

internal fun parseNetEaseSongs(body: String): List<NetEaseSong> {
    val songs = JSONObject(body).optJSONObject("result")?.optJSONArray("songs") ?: return emptyList()
    return (0 until songs.length()).mapNotNull { index ->
        val song = songs.optJSONObject(index) ?: return@mapNotNull null
        val id = song.optLong("id")
        val title = song.optString("name").trim()
        if (id <= 0L || title.isBlank()) return@mapNotNull null
        val artists = song.optJSONArray("artists")
        val artist = (0 until (artists?.length() ?: 0))
            .mapNotNull { artists?.optJSONObject(it)?.optString("name")?.trim()?.takeIf(String::isNotBlank) }
            .joinToString(", ")
        NetEaseSong(
            id = id,
            title = title,
            artist = artist,
            durationSec = song.optLong("duration") / 1_000L,
            album = song.optJSONObject("album")?.optString("name").orEmpty()
        )
    }
}

internal fun netEaseShortlist(songs: List<NetEaseSong>, durationSec: Long): List<NetEaseSong> {
    if (durationSec <= 0L) return songs.take(NETEASE_MAX_LOOKUPS - 1)
    return songs
        .filter { it.durationSec <= 0L || abs(it.durationSec - durationSec) <= NETEASE_DURATION_TOLERANCE_SEC }
        .take(NETEASE_MAX_LOOKUPS)
}

internal fun parseNetEaseLyrics(body: String, includeTranslation: Boolean): LyricsRepository.LyricsResult? {
    val json = JSONObject(body)
    if (json.optBoolean("nolyric") || json.optBoolean("uncollected")) return null
    val raw = json.optJSONObject("lrc")?.optString("lyric").orEmpty()
    if (raw.isBlank()) return null
    val synced = NETEASE_TIMESTAMP.containsMatchIn(raw)
    val lines = (if (synced) UnifiedLyricsParser.parse(raw) else UnifiedLyricsParser.parsePlain(raw))
        .filterNot { line -> NETEASE_INSTRUMENTAL_MARKERS.any { line.text.contains(it) } }
    if (lines.none { it.text.isNotBlank() }) return null
    val translations = if (includeTranslation && synced) {
        val translated = json.optJSONObject("tlyric")?.optString("lyric").orEmpty()
        UnifiedLyricsParser.parse(translated)
            .filter { it.text.isNotBlank() }
            .associate { it.startMs to it.text.trim() }
    } else {
        emptyMap()
    }
    val withTranslations = if (translations.isEmpty()) lines else lines.map { line ->
        translations[line.startMs]?.takeIf { line.text.isNotBlank() && it != line.text.trim() }
            ?.let { line.copy(translated = it) } ?: line
    }
    return LyricsRepository.LyricsResult(
        synced = synced,
        lines = withTranslations,
        provider = "NetEase",
        confidence = if (synced) NETEASE_SYNCED_CONFIDENCE else NETEASE_PLAIN_CONFIDENCE,
        cached = false
    )
}
