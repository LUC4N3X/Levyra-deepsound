package com.luc4n3x.levyra.player.sabr

/** The readable slice of one media payload, relative to the start of the UMP part. */
internal data class SabrMediaWindow(val offset: Int, val length: Int)

/**
 * Rebuilds one format's contiguous byte stream out of the interleaved media parts of a SABR
 * response.
 *
 * Media headers carry the absolute byte offset of the chunk that follows, so a response that starts
 * before the byte the player asked for is simply trimmed instead of restarting the request.
 */
internal class SabrSegmentAssembler(
    private val targetItag: Int,
    private val maxTrackedHeaders: Int = MAX_TRACKED_HEADERS
) {
    private val headers = HashMap<Int, HeaderState>()

    var deliveredBytes: Long = 0L
        private set

    fun reset() {
        headers.clear()
        deliveredBytes = 0L
    }

    fun onMediaHeader(header: SabrMediaHeader) {
        if (headers.size >= maxTrackedHeaders) headers.clear()
        headers[header.headerId] = HeaderState(header.itag, header.startDataRange)
    }

    /**
     * Advances the byte cursor for [headerId] and returns the part of the payload that still lies at
     * or after [position], or null when the chunk belongs to another format or is already consumed.
     */
    fun onMedia(headerId: Int, payloadLength: Int, position: Long): SabrMediaWindow? {
        val state = headers[headerId] ?: return null
        if (payloadLength <= 0) return null
        val chunkStart = state.nextOffset
        state.nextOffset = chunkStart + payloadLength
        if (state.itag != targetItag) return null
        deliveredBytes += payloadLength
        val chunkEnd = chunkStart + payloadLength
        if (chunkEnd <= position) return null
        val skip = (position - chunkStart).coerceAtLeast(0L).toInt()
        val length = payloadLength - skip
        return if (length > 0) SabrMediaWindow(MEDIA_PAYLOAD_OFFSET + skip, length) else null
    }

    private class HeaderState(val itag: Int, var nextOffset: Long)

    companion object {
        const val MEDIA_PAYLOAD_OFFSET = 1
        private const val MAX_TRACKED_HEADERS = 32
    }
}

/**
 * Maps a byte position to the media time a SABR request should start from. The estimate is linear in
 * the format's own byte stream; [unproductiveAttempts] walks it backwards so a request that landed
 * past the wanted byte converges instead of repeating.
 */
internal fun sabrPlayerTimeMsFor(
    contentLength: Long,
    durationMs: Long,
    position: Long,
    unproductiveAttempts: Int = 0,
    backoffMs: Long = SABR_UNPRODUCTIVE_BACKOFF_MS
): Long {
    if (position <= 0L || contentLength <= 0L || durationMs <= 0L) return 0L
    val estimate = durationMs * position.coerceAtMost(contentLength) / contentLength
    val backoff = unproductiveAttempts.coerceAtLeast(0).toLong() * backoffMs
    return (estimate - backoff).coerceIn(0L, (durationMs - 1L).coerceAtLeast(0L))
}

internal const val SABR_UNPRODUCTIVE_BACKOFF_MS = 10_000L
