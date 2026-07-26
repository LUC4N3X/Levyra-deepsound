package com.luc4n3x.levyra.desktop.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val videoUrl: String,
    val artworkUrl: String = "",
    val durationMs: Long = 0L,
    val offlinePath: String = "",
    val offlineMediaLabel: String = ""
) {
    val hasArtwork: Boolean get() = artworkUrl.isNotBlank()
    val isOffline: Boolean get() = offlinePath.isNotBlank()

    val displaySubtitle: String
        get() = listOf(artist, album).filter { it.isNotBlank() }.joinToString(" · ")

    companion object {
        fun videoIdOf(url: String): String {
            if (url.isBlank()) return ""
            val trimmed = url.trim()
            val watchIndex = trimmed.indexOf("v=")
            if (watchIndex >= 0) {
                val raw = trimmed.substring(watchIndex + 2)
                return raw.takeWhile { it != '&' && it != '#' }
            }
            val shortHost = trimmed.substringAfter("youtu.be/", "")
            if (shortHost.isNotBlank()) {
                return shortHost.takeWhile { it != '?' && it != '&' && it != '#' }
            }
            val shorts = trimmed.substringAfter("/shorts/", "")
            if (shorts.isNotBlank()) {
                return shorts.takeWhile { it != '?' && it != '&' && it != '#' }
            }
            return ""
        }

        fun watchUrlOf(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"
    }
}

val Track.videoId: String
    get() = Track.videoIdOf(videoUrl).ifBlank { id }
