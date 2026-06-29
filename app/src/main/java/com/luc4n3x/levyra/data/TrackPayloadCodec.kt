package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TrackPayloadCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(track: Track): String = json.encodeToString(track.toPayload())

    fun decode(payload: String): Track? = runCatching { json.decodeFromString<TrackPayload>(payload).toTrack() }.getOrNull()

    private fun Track.toPayload(): TrackPayload = TrackPayload(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = streamUrl,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        largeThumbnailUrl = largeThumbnailUrl,
        source = source,
        moodTags = moodTags.toList(),
        energy = energy,
        vocal = vocal,
        replayScore = replayScore,
        cacheScore = cacheScore,
        accentStart = accentStart,
        accentEnd = accentEnd
    )
}

@Serializable
private data class TrackPayload(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "YouTube Music",
    val durationMs: Long = 0L,
    val streamUrl: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val largeThumbnailUrl: String = "",
    val source: String = "YouTube Music",
    val moodTags: List<String> = listOf("music"),
    val energy: Int = 60,
    val vocal: Int = 50,
    val replayScore: Int = 84,
    val cacheScore: Int = 78,
    val accentStart: Int = 0xFF20E7FF.toInt(),
    val accentEnd: Int = 0xFF8E57FF.toInt()
) {
    fun toTrack(): Track? {
        if (id.isBlank() || title.isBlank()) return null
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            streamUrl = streamUrl,
            videoUrl = videoUrl.ifBlank { "https://www.youtube.com/watch?v=$id" },
            thumbnailUrl = thumbnailUrl,
            largeThumbnailUrl = largeThumbnailUrl,
            source = source,
            moodTags = moodTags.filter { it.isNotBlank() }.toSet().ifEmpty { setOf("music") },
            energy = energy,
            vocal = vocal,
            replayScore = replayScore,
            cacheScore = cacheScore,
            accentStart = accentStart,
            accentEnd = accentEnd
        )
    }
}
