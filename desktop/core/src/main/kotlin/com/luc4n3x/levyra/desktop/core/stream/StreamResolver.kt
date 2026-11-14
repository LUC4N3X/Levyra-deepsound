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
    val artist: String,
    val contentLength: Long = 0L,
    val container: String = "",
    val codec: String = "",
    val offlineAllowed: Boolean = true,
    val sourceIdentity: String = "",
    val deliveryMethod: com.luc4n3x.levyra.domain.PlaybackDeliveryMethod = com.luc4n3x.levyra.domain.PlaybackDeliveryMethod.PROGRESSIVE
) {
    fun isFresh(nowMillis: Long): Boolean = expiresAtMillis <= 0L || nowMillis < expiresAtMillis
}

class StreamResolutionException(message: String, cause: Throwable? = null) : Exception(message, cause)
class StreamSourceExpiredException : java.io.IOException("STREAM_EXPIRED")

interface StreamResolver {
    suspend fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio

    fun invalidate(track: Track)
}
