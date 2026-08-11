package com.luc4n3x.levyra.domain

import java.util.Locale

private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val YOUTUBE_VIDEO_URL = Regex("(?:v=|/shorts/|/embed/|/live/|youtu\\.be/)([A-Za-z0-9_-]{11})")

/**
 * Shared reading of the YouTube Music `musicVideoType` metadata so playback identity and
 * video-candidate selection classify the same string in the same way.
 */
internal object YoutubeMusicVideoType {
    fun isArtTrack(videoType: String): Boolean = videoType.uppercase(Locale.ROOT).contains("ATV")

    fun isOfficialVideo(videoType: String): Boolean {
        if (isArtTrack(videoType)) return false
        val type = videoType.uppercase(Locale.ROOT)
        return type.contains("OMV") ||
            type.contains("OFFICIAL_MUSIC_VIDEO") ||
            type.contains("OFFICIAL_SOURCE_MUSIC")
    }

    fun isVideo(videoType: String): Boolean {
        if (isOfficialVideo(videoType)) return true
        if (isArtTrack(videoType)) return false
        return videoType.uppercase(Locale.ROOT).contains("UGC")
    }
}

internal fun Track.hasVerifiedYoutubeMusicVideoPairing(): Boolean {
    val audio = audioVideoId.trim()
    val video = counterpartVideoId.trim()
    if (!YOUTUBE_VIDEO_ID.matches(audio) || !YOUTUBE_VIDEO_ID.matches(video) || audio == video) return true

    val selectedVideo = YOUTUBE_VIDEO_URL
        .find(videoUrl.trim())
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    return selectedVideo == video
}

internal fun Track.hasVideoPlaybackPayload(): Boolean {
    if (streamUrl.isBlank() || !hasVerifiedYoutubeMusicVideoPairing()) return false
    if (videoStreamUrl.isNotBlank()) return true

    return playbackManifest
        ?.streams
        ?.asSequence()
        ?.filter { it.selected }
        ?.any { stream ->
            stream.kind == PlaybackStreamKind.MUXED ||
                stream.kind == PlaybackStreamKind.VIDEO ||
                stream.kind == PlaybackStreamKind.HLS
        } == true
}
