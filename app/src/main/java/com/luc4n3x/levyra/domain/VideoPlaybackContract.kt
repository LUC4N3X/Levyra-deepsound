package com.luc4n3x.levyra.domain

private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val YOUTUBE_VIDEO_URL = Regex("(?:v=|/shorts/|/embed/|/live/|youtu\\.be/)([A-Za-z0-9_-]{11})")

internal fun Track.hasVerifiedYoutubeMusicVideoPairing(): Boolean {
    val audio = audioVideoId.trim()
    val video = counterpartVideoId.trim()
    if (!YOUTUBE_VIDEO_ID.matches(audio) || !YOUTUBE_VIDEO_ID.matches(video) || audio == video) return true
    if (!videoType.contains("OMV", ignoreCase = true)) return false

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
