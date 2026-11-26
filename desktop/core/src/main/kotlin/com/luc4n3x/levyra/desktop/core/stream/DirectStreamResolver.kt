package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.PreferredCodec
import com.luc4n3x.levyra.desktop.core.model.Track
import java.net.URI

class DirectStreamResolver(private val delegate: StreamResolver) : StreamResolver {
    override suspend fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio {
        if (!isDirect(track)) return delegate.resolve(track, quality, codec)
        return ResolvedAudio(
            url = track.videoUrl,
            label = "",
            expiresAtMillis = 0L,
            durationMs = track.durationMs,
            artworkUrl = track.artworkUrl,
            title = track.title,
            artist = track.artist,
            offlineAllowed = !track.live
        )
    }

    override fun invalidate(track: Track) {
        if (!isDirect(track)) delegate.invalidate(track)
    }

    companion object {
        private val youtubeHosts = setOf("youtube.com", "youtu.be", "youtube-nocookie.com")

        fun isDirect(track: Track): Boolean {
            if (track.offlinePath.isNotBlank()) return false
            val uri = runCatching { URI(track.videoUrl.trim()) }.getOrNull() ?: return false
            val scheme = uri.scheme?.lowercase() ?: return false
            if (scheme != "https" && scheme != "http") return false
            val host = uri.host?.lowercase()?.removePrefix("www.")?.removePrefix("m.")?.removePrefix("music.") ?: return false
            return youtubeHosts.none { host == it || host.endsWith(".$it") }
        }
    }
}
