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
        accentEnd = accentEnd,
        youtubeLoudnessDb = youtubeLoudnessDb,
        youtubePerceptualLoudnessDb = youtubePerceptualLoudnessDb,
        isrc = isrc,
        upc = upc,
        releaseDate = releaseDate,
        year = year,
        trackNumber = trackNumber,
        discNumber = discNumber,
        explicit = explicit,
        albumBrowseId = albumBrowseId,
        artistBrowseIds = artistBrowseIds,
        counterpartVideoId = counterpartVideoId,
        videoType = videoType,
        metadataProvider = metadataProvider,
        metadataConfidence = metadataConfidence,
        canonicalAlbumUrl = canonicalAlbumUrl,
        youtubeLikeCount = youtubeLikeCount,
        youtubeViewCount = youtubeViewCount
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
    val accentEnd: Int = 0xFF8E57FF.toInt(),
    val youtubeLoudnessDb: Float? = null,
    val youtubePerceptualLoudnessDb: Float? = null,
    val isrc: String = "",
    val upc: String = "",
    val releaseDate: String = "",
    val year: String = "",
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val explicit: Boolean = false,
    val albumBrowseId: String = "",
    val artistBrowseIds: List<String> = emptyList(),
    val counterpartVideoId: String = "",
    val videoType: String = "",
    val metadataProvider: String = "",
    val metadataConfidence: Int = 0,
    val canonicalAlbumUrl: String = "",
    val youtubeLikeCount: Long = -1L,
    val youtubeViewCount: Long = -1L
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
            accentEnd = accentEnd,
            youtubeLoudnessDb = youtubeLoudnessDb,
            youtubePerceptualLoudnessDb = youtubePerceptualLoudnessDb,
            isrc = isrc,
            upc = upc,
            releaseDate = releaseDate,
            year = year,
            trackNumber = trackNumber.coerceAtLeast(0),
            discNumber = discNumber.coerceAtLeast(0),
            explicit = explicit,
            albumBrowseId = albumBrowseId,
            artistBrowseIds = artistBrowseIds.filter { it.isNotBlank() },
            counterpartVideoId = counterpartVideoId,
            videoType = videoType,
            metadataProvider = metadataProvider,
            metadataConfidence = metadataConfidence.coerceIn(0, 100),
            canonicalAlbumUrl = canonicalAlbumUrl,
            youtubeLikeCount = youtubeLikeCount,
            youtubeViewCount = youtubeViewCount
        )
    }
}
