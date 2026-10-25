package com.luc4n3x.levyra.domain

enum class MusicVideoPlaybackPreference(val storageValue: String) {
    Automatic("automatic"),
    PreferAudio("audio"),
    PreferVideo("video"),
    RememberLast("remember");

    val pairsArtTrackAudio: Boolean get() = this != Automatic

    fun startsInVideo(track: Track, requestedVideoMode: Boolean, lastVideoMode: Boolean): Boolean {
        if (this == Automatic || !track.isMusicVideoStart(requestedVideoMode)) return requestedVideoMode
        return when (this) {
            Automatic -> requestedVideoMode
            PreferAudio -> false
            PreferVideo -> true
            RememberLast -> lastVideoMode
        }
    }

    companion object {
        fun fromStorage(value: String?): MusicVideoPlaybackPreference =
            entries.firstOrNull { it.storageValue == value?.trim() } ?: Automatic
    }
}

private fun Track.isMusicVideoStart(requestedVideoMode: Boolean): Boolean {
    if (id.startsWith("local:") || source.equals("Offline", ignoreCase = true)) return false
    return requestedVideoMode || YoutubeMusicVideoType.isVideo(videoType)
}
