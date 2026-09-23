package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SpeedDial
import com.luc4n3x.levyra.domain.SpeedDialKind
import com.luc4n3x.levyra.domain.SpeedDialPin
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class SpeedDialStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    suspend fun load(): List<SpeedDialPin> = withContext(Dispatchers.IO) {
        mutex.withLock { decodeSpeedDialPins(preferences.getString(KEY_PINS, null)) }
    }

    suspend fun save(latest: () -> List<SpeedDialPin>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val encoded = encodeSpeedDialPins(SpeedDial.sanitize(latest()))
            if (!preferences.edit().putString(KEY_PINS, encoded).commit()) {
                Timber.w("Speed dial pins were not persisted")
            }
        }
    }

    private companion object {
        const val PREFERENCES = "levyra_speed_dial"
        const val KEY_PINS = "pins_v1"
        val mutex = Mutex()
    }
}

internal fun encodeSpeedDialPins(pins: List<SpeedDialPin>): String = JSONArray().apply {
    pins.forEach { pin ->
        put(
            JSONObject()
                .put("kind", pin.kind.storageCode)
                .put("targetId", pin.targetId)
                .put("title", pin.title)
                .put("subtitle", pin.subtitle)
                .put("artworkUrl", pin.artworkUrl)
                .put("pinnedAt", pin.pinnedAt)
                .apply {
                    pin.track?.let { put("track", speedDialTrackToJson(it)) }
                    pin.album?.let { put("album", speedDialAlbumToJson(it)) }
                }
        )
    }
}.toString()

internal fun decodeSpeedDialPins(raw: String?): List<SpeedDialPin> {
    if (raw.isNullOrBlank()) return emptyList()
    val array = runCatching { JSONArray(raw) }.getOrElse { error ->
        Timber.w(error, "Speed dial payload is unreadable")
        return emptyList()
    }
    val decoded = ArrayList<SpeedDialPin>(minOf(array.length(), SpeedDial.MAX_PINS))
    for (index in 0 until array.length()) {
        val pin = runCatching { array.optJSONObject(index)?.let(::speedDialPinFromJson) }.getOrNull() ?: continue
        decoded += pin
    }
    return SpeedDial.sanitize(decoded)
}

private fun speedDialPinFromJson(json: JSONObject): SpeedDialPin? {
    val kind = SpeedDialKind.fromStorageCode(json.optString("kind")) ?: return null
    return SpeedDialPin(
        kind = kind,
        targetId = json.optString("targetId"),
        title = json.optString("title"),
        subtitle = json.optString("subtitle"),
        artworkUrl = json.optString("artworkUrl"),
        pinnedAt = json.optLong("pinnedAt", 0L),
        track = json.optJSONObject("track")?.let(::speedDialTrackFromJson),
        album = json.optJSONObject("album")?.let(::speedDialAlbumFromJson)
    )
}

private fun speedDialTrackToJson(track: Track): JSONObject = JSONObject()
    .put("id", track.id)
    .put("title", track.title)
    .put("artist", track.artist)
    .put("album", track.album)
    .put("durationMs", track.durationMs)
    .put("streamUrl", track.streamUrl)
    .put("videoUrl", track.videoUrl)
    .put("thumbnailUrl", track.thumbnailUrl)
    .put("largeThumbnailUrl", track.largeThumbnailUrl)
    .put("source", track.source)
    .put("moodTags", JSONArray(track.moodTags.toList()))
    .put("accentStart", track.accentStart)
    .put("accentEnd", track.accentEnd)
    .put("isrc", track.isrc)
    .put("releaseDate", track.releaseDate)
    .put("year", track.year)
    .put("explicit", track.explicit)
    .put("albumBrowseId", track.albumBrowseId)
    .put("artistBrowseIds", JSONArray(track.artistBrowseIds))
    .put("counterpartVideoId", track.counterpartVideoId)
    .put("videoType", track.videoType)
    .put("audioVideoId", track.audioVideoId)

private fun speedDialTrackFromJson(json: JSONObject): Track? {
    val id = json.optString("id").trim()
    if (id.isEmpty()) return null
    return Track(
        id = id,
        title = json.optString("title"),
        artist = json.optString("artist"),
        album = json.optString("album"),
        durationMs = json.optLong("durationMs", 0L).coerceAtLeast(0L),
        streamUrl = json.optString("streamUrl"),
        videoUrl = json.optString("videoUrl"),
        thumbnailUrl = json.optString("thumbnailUrl"),
        largeThumbnailUrl = json.optString("largeThumbnailUrl"),
        source = json.optString("source"),
        moodTags = json.optJSONArray("moodTags").strings().toSet().ifEmpty { setOf("music") },
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = json.optInt("accentStart"),
        accentEnd = json.optInt("accentEnd"),
        isrc = json.optString("isrc"),
        releaseDate = json.optString("releaseDate"),
        year = json.optString("year"),
        explicit = json.optBoolean("explicit"),
        albumBrowseId = json.optString("albumBrowseId"),
        artistBrowseIds = json.optJSONArray("artistBrowseIds").strings(),
        counterpartVideoId = json.optString("counterpartVideoId"),
        videoType = json.optString("videoType"),
        audioVideoId = json.optString("audioVideoId")
    )
}

private fun speedDialAlbumToJson(album: AlbumHit): JSONObject = JSONObject()
    .put("title", album.title)
    .put("artist", album.artist)
    .put("year", album.year)
    .put("thumbnailUrl", album.thumbnailUrl)
    .put("query", album.query)
    .put("browseId", album.browseId)
    .put("artistBrowseId", album.artistBrowseId)
    .put("audioPlaylistId", album.audioPlaylistId)
    .put("explicit", album.explicit)
    .put("releaseDate", album.releaseDate)
    .put("upc", album.upc)
    .put("canonicalUrl", album.canonicalUrl)

private fun speedDialAlbumFromJson(json: JSONObject): AlbumHit? {
    val title = json.optString("title").trim()
    if (title.isEmpty()) return null
    return AlbumHit(
        title = title,
        artist = json.optString("artist"),
        year = json.optString("year"),
        thumbnailUrl = json.optString("thumbnailUrl"),
        query = json.optString("query"),
        browseId = json.optString("browseId"),
        artistBrowseId = json.optString("artistBrowseId"),
        audioPlaylistId = json.optString("audioPlaylistId"),
        explicit = json.optBoolean("explicit"),
        releaseDate = json.optString("releaseDate"),
        upc = json.optString("upc"),
        canonicalUrl = json.optString("canonicalUrl")
    )
}

private fun JSONArray?.strings(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optString(index).trim().takeIf { it.isNotEmpty() } }
}
