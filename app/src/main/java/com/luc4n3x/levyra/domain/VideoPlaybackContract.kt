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

internal const val VIDEO_VIEW_COUNT_STEP = 500

internal fun videoViewCountBonus(views: Long): Int {
    if (views <= 0L) return 0
    var digits = 0
    var remaining = views
    while (remaining > 0L) {
        digits += 1
        remaining /= 10L
    }
    return digits.coerceAtMost(10) * VIDEO_VIEW_COUNT_STEP
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

internal fun Track.withPairedArtTrackAudio(): Track {
    if (!YoutubeMusicVideoType.isVideo(videoType) || audioVideoId.isNotBlank()) return this
    val ownId = id.trim()
    val counterpart = counterpartVideoId.trim()
    if (!YOUTUBE_VIDEO_ID.matches(ownId) || !YOUTUBE_VIDEO_ID.matches(counterpart) || ownId == counterpart) return this
    return copy(audioVideoId = counterpart, counterpartVideoId = ownId)
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

internal fun Track.hasReusableVideoPlaybackPayload(): Boolean {
    if (!hasVideoPlaybackPayload()) return false
    val manifest = playbackManifest ?: return true
    if (videoStreamUrl.isNotBlank() || !manifest.isMuxed) return true
    return manifest.streams.any { it.kind == PlaybackStreamKind.VIDEO }
}
