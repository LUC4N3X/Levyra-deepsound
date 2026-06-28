package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.json.JSONObject

object TrackJson {
    fun toJson(track: Track): JSONObject = JSONObject()
        .put("id", track.id)
        .put("title", track.title)
        .put("artist", track.artist)
        .put("album", track.album)
        .put("durationMs", track.durationMs)
        .put("videoUrl", track.videoUrl)
        .put("thumbnailUrl", track.thumbnailUrl)
        .put("largeThumbnailUrl", track.largeThumbnailUrl)
        .put("source", track.source)
        .put("accentStart", track.accentStart)
        .put("accentEnd", track.accentEnd)

    fun fromJson(json: JSONObject): Track? {
        val id = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        val title = json.optString("title").takeIf { it.isNotBlank() } ?: return null
        return Track(
            id = id,
            title = title,
            artist = json.optString("artist"),
            album = json.optString("album", "YouTube Music"),
            durationMs = json.optLong("durationMs", 0L),
            streamUrl = "",
            videoUrl = json.optString("videoUrl", "https://www.youtube.com/watch?v=$id"),
            thumbnailUrl = json.optString("thumbnailUrl"),
            largeThumbnailUrl = json.optString("largeThumbnailUrl"),
            source = json.optString("source", "YouTube Music"),
            moodTags = setOf("music"),
            energy = 60,
            vocal = 50,
            replayScore = 84,
            cacheScore = 78,
            accentStart = json.optInt("accentStart", 0xFF20E7FF.toInt()),
            accentEnd = json.optInt("accentEnd", 0xFF8E57FF.toInt())
        )
    }
}
