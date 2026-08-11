package com.luc4n3x.levyra.domain

import java.util.concurrent.ConcurrentHashMap

private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val verifiedYoutubeMusicVideoPairs = ConcurrentHashMap.newKeySet<String>()

internal fun rememberYoutubeMusicOfficialVideoPairing(audioVideoId: String, videoVideoId: String) {
    val audio = audioVideoId.trim()
    val video = videoVideoId.trim()
    if (!YOUTUBE_VIDEO_ID.matches(audio) || !YOUTUBE_VIDEO_ID.matches(video) || audio == video) return
    verifiedYoutubeMusicVideoPairs += "$audio|$video"
}

internal fun Track.hasVerifiedYoutubeMusicVideoPairing(): Boolean {
    val audio = audioVideoId.trim()
    val video = counterpartVideoId.trim()
    if (!YOUTUBE_VIDEO_ID.matches(audio) || !YOUTUBE_VIDEO_ID.matches(video) || audio == video) return true
    return "$audio|$video" in verifiedYoutubeMusicVideoPairs
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
