package com.luc4n3x.levyra.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import java.io.EOFException
import java.io.FileNotFoundException
import java.net.ProtocolException
import java.util.Locale

internal const val PLAYBACK_STREAM_READ_RETRIES = 2

internal fun playbackCacheIsComplete(contentLength: Long, cachedLength: Long): Boolean =
    contentLength > 0L && cachedLength >= contentLength

internal fun isRecoverableStreamEnd(error: Throwable): Boolean {
    var current: Throwable? = error
    var depth = 0
    while (current != null && depth < MAX_CAUSE_DEPTH) {
        val message = current.message.orEmpty().lowercase(Locale.ROOT)
        if (current is ProtocolException && message.contains("unexpected end of stream")) return true
        if (current is EOFException && (message.isBlank() || message.contains("unexpected"))) return true
        current = current.cause
        depth++
    }
    return false
}

internal fun playbackFailureReasonOf(error: Throwable): String {
    val parts = mutableListOf<String>()
    var current: Throwable? = error
    var depth = 0
    while (current != null && depth < MAX_CAUSE_DEPTH) {
        if (current is HttpDataSource.InvalidResponseCodeException) {
            parts += "http ${current.responseCode}"
        } else if (current is FileNotFoundException) {
            parts += "enoent"
        }
        current.message?.takeIf { it.isNotBlank() }?.let(parts::add)
        parts += current::class.java.simpleName
        current = current.cause
        depth++
    }
    return parts.joinToString(separator = " ")
}

@UnstableApi
internal fun isPlaybackResourceFullyCached(cache: Cache, key: String): Boolean = runCatching {
    if (key.isBlank()) return@runCatching false
    val contentLength = ContentMetadata.getContentLength(cache.getContentMetadata(key))
    if (contentLength <= 0L) return@runCatching false
    if (!playbackCacheIsComplete(contentLength, cache.getCachedLength(key, 0L, contentLength))) {
        return@runCatching false
    }
    cache.getCachedSpans(key).all { span -> !span.isCached || span.file?.exists() == true }
}.getOrDefault(false)

@UnstableApi
internal fun removePlaybackCacheResource(cache: Cache, key: String): Boolean = runCatching {
    if (key.isBlank()) return@runCatching false
    cache.removeResource(key)
    true
}.getOrDefault(false)

private const val MAX_CAUSE_DEPTH = 8
