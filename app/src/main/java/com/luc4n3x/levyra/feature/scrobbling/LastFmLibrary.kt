package com.luc4n3x.levyra.feature.scrobbling

import org.json.JSONArray
import org.json.JSONObject

enum class LastFmPlaylistKind { Site, Loved, TopWeek, TopMonth, TopAllTime }

data class LastFmPlaylist(
    val id: String,
    val kind: LastFmPlaylistKind,
    val title: String = "",
    val tracks: List<ListenBrainzTrack> = emptyList(),
    val trackCount: Int = tracks.size,
    val coverUrl: String? = null
)

internal data class LastFmChartRequest(val kind: LastFmPlaylistKind, val method: String, val container: String, val period: String?, val limit: Int) {
    fun parameters(user: String): Map<String, String> = buildMap {
        put("user", user)
        put("limit", limit.toString())
        period?.let { put("period", it) }
    }
}

internal val lastFmChartRequests = listOf(
    LastFmChartRequest(LastFmPlaylistKind.Loved, "user.getLovedTracks", "lovedtracks", null, 500),
    LastFmChartRequest(LastFmPlaylistKind.TopWeek, "user.getTopTracks", "toptracks", "7day", 50),
    LastFmChartRequest(LastFmPlaylistKind.TopMonth, "user.getTopTracks", "toptracks", "1month", 50),
    LastFmChartRequest(LastFmPlaylistKind.TopAllTime, "user.getTopTracks", "toptracks", "overall", 100)
)

internal object LastFmJson {
    fun tracks(root: JSONObject, container: String): List<ListenBrainzTrack> {
        val node = root.optJSONObject(container) ?: return emptyList()
        val items = node.optJSONArray("track")
            ?: node.optJSONObject("track")?.let { JSONArray().put(it) }
            ?: return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("name").trim()
            val artist = when (val raw = item.opt("artist")) {
                is JSONObject -> raw.optString("name").ifBlank { raw.optString("#text") }
                else -> raw?.toString().orEmpty()
            }.trim()
            if (title.isBlank() || artist.isBlank()) null else ListenBrainzTrack(title, artist)
        }.distinctBy { it.artist.lowercase() to it.title.lowercase() }
    }

    fun userName(root: JSONObject): String = root.optJSONObject("user")?.optString("name").orEmpty().trim()
}

internal object LastFmSitePages {
    private val playlistLink = Regex("""href="/user/[^/"]+/playlists/(\d+)"\s+title="([^"]*)"""")
    private val entryCount = Regex("""playlisting-playlists-item-entry-count">\s*([\d,.\s]+)""")
    private val cover = Regex("""<img src="(https://[^"]+)"""")
    private val trackName = Regex("""data-track-name="([^"]*)"""")
    private val artistName = Regex("""data-artist-name="([^"]*)"""")
    private val numericEntity = Regex("""&#(x?)([0-9a-fA-F]+);""")
    private const val ITEM_WINDOW = 2500

    fun playlists(html: String): List<LastFmPlaylist> = playlistLink.findAll(html).mapNotNull { match ->
        val id = match.groupValues[1]
        val title = unescape(match.groupValues[2]).trim()
        if (title.isBlank()) return@mapNotNull null
        val window = html.substring(match.range.last, minOf(html.length, match.range.last + ITEM_WINDOW))
        val count = entryCount.find(window)?.groupValues?.get(1)?.filter(Char::isDigit)?.toIntOrNull() ?: 0
        LastFmPlaylist(
            id = id,
            kind = LastFmPlaylistKind.Site,
            title = title,
            trackCount = count,
            coverUrl = cover.find(window)?.groupValues?.get(1)
        )
    }.distinctBy { it.id }.toList()

    fun tracks(html: String): List<ListenBrainzTrack> = html.split("data-playlist-row").drop(1).mapNotNull { row ->
        val title = trackName.find(row)?.groupValues?.get(1)?.let(::unescape)?.trim().orEmpty()
        val artist = artistName.find(row)?.groupValues?.get(1)?.let(::unescape)?.trim().orEmpty()
        if (title.isBlank() || artist.isBlank()) null else ListenBrainzTrack(title, artist)
    }

    fun isChallenge(html: String): Boolean = html.contains("<title>Client Challenge", ignoreCase = true)

    private fun unescape(value: String): String = numericEntity.replace(value) { match ->
        val code = match.groupValues[2].toIntOrNull(if (match.groupValues[1].isEmpty()) 10 else 16)
        code?.let { String(Character.toChars(it)) } ?: match.value
    }.replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">").replace("&apos;", "'").replace("&amp;", "&")
}
