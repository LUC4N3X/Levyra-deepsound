package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.data.PlaybackSourceIdentity
import com.luc4n3x.levyra.domain.Track

object LevyraPlaybackCacheKey {
    private val itagPattern = Regex("(?:[?&]|%26)itag(?:=|%3D)(\\d+)", RegexOption.IGNORE_CASE)

    fun stream(track: Track): String {
        // Keyed on the resolved source, not the catalog id: in native-video mode the audio stream
        // belongs to the official video, so a catalog-id key collided with the art track whenever
        // both used the same itag and Media3 replayed one recording's bytes for the other.
        val id = PlaybackSourceIdentity.sourceVideoId(track)
            .ifBlank { stableId(track) }
            .replace(':', '_')
        return "levyra:$id:stream-v2:${variant(track.streamUrl)}"
    }

    fun offlineStream(track: Track): String {
        val id = PlaybackSourceIdentity.sourceVideoId(track)
            .ifBlank { stableId(track) }
            .replace(':', '_')
        return "levyra:$id:offline-v1:${variant(track.streamUrl)}"
    }

    fun video(track: Track): String {
        val id = PlaybackSourceIdentity.sourceVideoId(track)
            .ifBlank { stableId(track) }
            .replace(':', '_')
        val url = track.videoStreamUrl.ifBlank { track.streamUrl }
        return "levyra:$id:video-v5:${variant(url)}"
    }

    private fun stableId(track: Track): String = track.id.trim()
        .ifBlank { track.videoUrl.trim() }
        .ifBlank { "${track.artist.trim()}-${track.title.trim()}" }
        .replace(':', '_')

    private fun variant(url: String): String {
        val clean = url.lowercase()
        val itag = itagPattern.find(url)?.groupValues?.getOrNull(1)
        if (!itag.isNullOrBlank()) return "itag-$itag"
        return when {
            clean.contains(".m3u8") || clean.contains("/hls_playlist") || clean.contains("/manifest/hls") -> "hls"
            clean.contains("mime=audio%2fwebm") || clean.contains("mime=audio/webm") -> "audio-webm"
            clean.contains("mime=audio%2fmp4") || clean.contains("mime=audio/mp4") -> "audio-mp4"
            clean.contains("mime=video%2fwebm") || clean.contains("mime=video/webm") -> "video-webm"
            clean.contains("mime=video%2fmp4") || clean.contains("mime=video/mp4") -> "video-mp4"
            else -> "direct"
        }
    }
}
