package com.luc4n3x.levyra.player.sabr

internal data class SabrMediaWindow(val offset: Int, val length: Int)

internal class SabrSegmentAssembler(
    private val targetItag: Int,
    private val maxTrackedHeaders: Int = MAX_TRACKED_HEADERS
) {
    private val headers = HashMap<Int, HeaderState>()

    fun reset() {
        headers.clear()
    }

    fun onMediaHeader(header: SabrMediaHeader) {
        if (headers.size >= maxTrackedHeaders) headers.clear()
        headers[header.headerId] = HeaderState(header.itag, header.startDataRange)
    }

    fun onMedia(headerId: Int, payloadLength: Int, position: Long): SabrMediaWindow? {
        val state = headers[headerId] ?: return null
        if (payloadLength <= 0) return null
        val chunkStart = state.nextOffset
        state.nextOffset = chunkStart + payloadLength
        if (state.itag != targetItag) return null
        val chunkEnd = chunkStart + payloadLength
        if (chunkEnd <= position) return null
        if (chunkStart > position) return null
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
