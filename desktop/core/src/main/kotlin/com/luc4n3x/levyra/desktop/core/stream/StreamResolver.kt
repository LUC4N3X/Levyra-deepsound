package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.PreferredCodec
import com.luc4n3x.levyra.desktop.core.model.Track

data class ResolvedAudio(
    val url: String,
    val label: String,
    val expiresAtMillis: Long,
    val durationMs: Long,
    val artworkUrl: String,
    val title: String,
    val artist: String
) {
    fun isFresh(nowMillis: Long): Boolean = expiresAtMillis <= 0L || nowMillis < expiresAtMillis
}

class StreamResolutionException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface StreamResolver {
    suspend fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio

    fun invalidate(track: Track)
}
