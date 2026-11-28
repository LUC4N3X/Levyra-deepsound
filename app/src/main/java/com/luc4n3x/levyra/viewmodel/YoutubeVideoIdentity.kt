package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.PlaybackSourceIdentity
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeMusicVideoType

internal val YOUTUBE_PLAYABLE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

internal fun catalogArtTrackId(ownId: String, playlist: com.luc4n3x.levyra.data.YoutubeMusicWatchPlaylist?): String? =
    playlist?.tracks
        ?.firstOrNull { it.videoId == ownId }
        ?.counterpart
        ?.takeIf { YoutubeMusicVideoType.isArtTrack(it.videoType) }
        ?.videoId
        ?.takeIf { it.isNotBlank() && it != ownId }

internal fun youtubePlayableTrack(track: Track, preferVideo: Boolean = false): Track? {
    val counterpart = track.counterpartVideoId.trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val fromUrl = youtubeVideoId(track.videoUrl).trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val fromIdUrl = youtubeVideoId(track.id).trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val rawId = track.id.trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val storedAudio = track.audioVideoId.trim().takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    val cachedVideoId = if (track.id.startsWith("spotify:")) {
        com.luc4n3x.levyra.data.spotify.SpotifyYouTubeMatchCache.get().get(track.id)?.videoId?.trim()?.takeIf(YOUTUBE_PLAYABLE_VIDEO_ID::matches).orEmpty()
    } else ""
    val regular = sequenceOf(storedAudio, fromIdUrl, rawId, fromUrl, cachedVideoId)
        .firstOrNull(String::isNotBlank)
        .orEmpty()
    val type = track.videoType.uppercase()
    val videoTyped = type.contains("OMV") || type.contains("UGC")
    val videoId = when {
        !preferVideo -> regular.ifBlank { counterpart }
        type.contains("ATV") -> counterpart
        videoTyped -> fromUrl.ifBlank { regular }
        counterpart.isNotBlank() -> counterpart
        else -> ""
    }
    if (videoId.isBlank()) return null
    if (preferVideo && videoId == regular && !videoTyped) return null
    return track.copy(
        videoUrl = "https://www.youtube.com/watch?v=$videoId",
        audioVideoId = regular
    )
}


private val YOUTUBE_ENGAGEMENT_VIDEO_ID = YOUTUBE_PLAYABLE_VIDEO_ID

internal fun youtubeEngagementVideoId(track: Track): String {
    val selectedVideoId = PlaybackSourceIdentity.sourceVideoId(track)
    val youtubeBacked = isYoutubeBackedTrack(track)
    if (
        youtubeBacked &&
        YoutubeMusicVideoType.isVideo(track.videoType) &&
        YOUTUBE_ENGAGEMENT_VIDEO_ID.matches(selectedVideoId)
    ) {
        return selectedVideoId
    }
    if (youtubeBacked) {
        val audioVideoId = track.audioVideoId.trim()
        val counterpart = track.counterpartVideoId.trim()
        val confirmedAudioPair =
            YOUTUBE_ENGAGEMENT_VIDEO_ID.matches(audioVideoId) &&
                selectedVideoId == audioVideoId
        if (
            confirmedAudioPair &&
            YOUTUBE_ENGAGEMENT_VIDEO_ID.matches(counterpart) &&
            counterpart != audioVideoId
        ) {
            return counterpart
        }
    }
    if (youtubeBacked && YOUTUBE_ENGAGEMENT_VIDEO_ID.matches(selectedVideoId)) {
        return selectedVideoId
    }
    val urlVideoId = youtubeVideoId(track.videoUrl).trim()
    if (YOUTUBE_ENGAGEMENT_VIDEO_ID.matches(urlVideoId)) return urlVideoId

    val idUrlVideoId = youtubeVideoId(track.id).trim()
    if (YOUTUBE_ENGAGEMENT_VIDEO_ID.matches(idUrlVideoId)) return idUrlVideoId

    if (!youtubeBacked) return ""
    return sequenceOf(track.counterpartVideoId, track.id)
        .map(String::trim)
        .firstOrNull(YOUTUBE_ENGAGEMENT_VIDEO_ID::matches)
        .orEmpty()
}

private fun isYoutubeBackedTrack(track: Track): Boolean {
    val sourceMarker = "${track.source} ${track.metadataProvider}".lowercase()
    return sourceMarker.contains("youtube") ||
        track.videoType.isNotBlank() ||
        track.videoUrl.contains("youtube.com", ignoreCase = true) ||
        track.videoUrl.contains("youtu.be", ignoreCase = true)
}

private val YOUTUBE_VIDEO_ID_PATTERNS = listOf(
    Regex("[?&]v=([^&?/]+)"),
    Regex("youtu\\.be/([^?&/]+)"),
    Regex("/shorts/([^?&/]+)"),
    Regex("/embed/([^?&/]+)")
)

internal fun youtubeVideoId(url: String): String {
    if (url.isBlank()) return ""
    return YOUTUBE_VIDEO_ID_PATTERNS.firstNotNullOfOrNull { pattern ->
        pattern.find(url)?.groupValues?.getOrNull(1)
    }.orEmpty()
}
