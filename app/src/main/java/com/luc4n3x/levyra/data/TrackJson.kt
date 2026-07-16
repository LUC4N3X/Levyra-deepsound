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
        .put("youtubeLoudnessDb", track.youtubeLoudnessDb)
        .put("youtubePerceptualLoudnessDb", track.youtubePerceptualLoudnessDb)
        .put("isrc", track.isrc)
        .put("upc", track.upc)
        .put("releaseDate", track.releaseDate)
        .put("year", track.year)
        .put("trackNumber", track.trackNumber)
        .put("discNumber", track.discNumber)
        .put("explicit", track.explicit)
        .put("albumBrowseId", track.albumBrowseId)
        .put("artistBrowseIds", org.json.JSONArray(track.artistBrowseIds))
        .put("counterpartVideoId", track.counterpartVideoId)
        .put("videoType", track.videoType)
        .put("metadataProvider", track.metadataProvider)
        .put("metadataConfidence", track.metadataConfidence)
        .put("canonicalAlbumUrl", track.canonicalAlbumUrl)

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
            accentEnd = json.optInt("accentEnd", 0xFF8E57FF.toInt()),
            youtubeLoudnessDb = json.optNullableFloat("youtubeLoudnessDb"),
            youtubePerceptualLoudnessDb = json.optNullableFloat("youtubePerceptualLoudnessDb"),
            isrc = json.optString("isrc"),
            upc = json.optString("upc"),
            releaseDate = json.optString("releaseDate"),
            year = json.optString("year"),
            trackNumber = json.optInt("trackNumber"),
            discNumber = json.optInt("discNumber"),
            explicit = json.optBoolean("explicit"),
            albumBrowseId = json.optString("albumBrowseId"),
            artistBrowseIds = json.optJSONArray("artistBrowseIds").toStringList(),
            counterpartVideoId = json.optString("counterpartVideoId"),
            videoType = json.optString("videoType"),
            metadataProvider = json.optString("metadataProvider"),
            metadataConfidence = json.optInt("metadataConfidence").coerceIn(0, 100),
            canonicalAlbumUrl = json.optString("canonicalAlbumUrl")
        )
    }
}

private fun JSONObject.optNullableFloat(key: String): Float? {
    if (!has(key) || isNull(key)) return null
    val value = optDouble(key, Double.NaN)
    return value.takeIf { it.isFinite() }?.toFloat()
}

private fun org.json.JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}
