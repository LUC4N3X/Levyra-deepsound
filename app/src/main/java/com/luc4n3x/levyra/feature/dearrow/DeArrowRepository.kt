package com.luc4n3x.levyra.feature.dearrow

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel

class DeArrowRepository(
    private val brandingSource: suspend (String) -> DeArrowBranding?,
    @Volatile var enabled: Boolean = true,
    private val clockMs: () -> Long = System::currentTimeMillis,
    private val brandingOutcomeSource: (suspend (String) -> DeArrowBrandingOutcome)? = null
) {
    constructor(api: DeArrowApi, enabled: Boolean = true) : this(
        brandingSource = api::branding,
        enabled = enabled,
        brandingOutcomeSource = api::brandingOutcome
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap<String, Deferred<DeArrowResult?>>()
    private val cache = object : LinkedHashMap<String, DeArrowCacheEntry>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DeArrowCacheEntry>?): Boolean =
            size > MAX_ENTRIES
    }

    suspend fun branding(videoId: String): DeArrowResult? {
        if (!enabled) return null
        val id = videoId.trim()
        if (!DEARROW_VIDEO_ID_PATTERN.matches(id)) return null

        cachedEntry(id, clockMs())?.let { return it.toResult() }

        val created = scope.async(start = CoroutineStart.LAZY) { fetchAndCache(id) }
        created.invokeOnCompletion { inFlight.remove(id, created) }
        val shared = inFlight.putIfAbsent(id, created) ?: created
        if (shared === created) {
            created.start()
        } else {
            created.cancel()
        }
        return try {
            shared.await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            null
        }
    }

    fun close() {
        scope.cancel()
        inFlight.clear()
        synchronized(cache) { cache.clear() }
    }

    private suspend fun fetchAndCache(videoId: String): DeArrowResult? {
        val outcome = try {
            brandingOutcomeSource?.invoke(videoId)
                ?: DeArrowBrandingOutcome.Resolved(brandingSource(videoId))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            DeArrowBrandingOutcome.Inconclusive
        }
        if (outcome is DeArrowBrandingOutcome.Inconclusive) return null

        val now = clockMs()
        val branding = (outcome as DeArrowBrandingOutcome.Resolved).branding
        if (branding == null) {
            putEntry(videoId, DeArrowCacheEntry(title = null, thumbnailUrl = null, expiresAtMs = now + NEGATIVE_TTL_MS))
            return null
        }
        val title = DeArrowSelectionPolicy.selectTitle(branding.titles)
        val timestamp = DeArrowSelectionPolicy.selectThumbnailTimestamp(branding.thumbnails)
        val thumbnailUrl = timestamp?.let { DeArrowApi.thumbnailUrl(videoId, it) }
        if (title == null && thumbnailUrl == null) {
            putEntry(videoId, DeArrowCacheEntry(title = null, thumbnailUrl = null, expiresAtMs = now + NEGATIVE_TTL_MS))
            return null
        }
        putEntry(videoId, DeArrowCacheEntry(title = title, thumbnailUrl = thumbnailUrl, expiresAtMs = now + POSITIVE_TTL_MS))
        return DeArrowResult(title, thumbnailUrl)
    }

    private fun cachedEntry(videoId: String, nowMs: Long): DeArrowCacheEntry? = synchronized(cache) {
        val entry = cache[videoId] ?: return@synchronized null
        if (entry.expiresAtMs <= nowMs) {
            cache.remove(videoId)
            null
        } else {
            entry
        }
    }

    private fun putEntry(videoId: String, entry: DeArrowCacheEntry) {
        synchronized(cache) {
            cache[videoId] = entry
        }
    }

    companion object {
        private const val MAX_ENTRIES = 200
        internal const val POSITIVE_TTL_MS = 6L * 60L * 60L * 1000L
        internal const val NEGATIVE_TTL_MS = 30L * 60L * 1000L
    }
}

private data class DeArrowCacheEntry(
    val title: String?,
    val thumbnailUrl: String?,
    val expiresAtMs: Long
)

private fun DeArrowCacheEntry.toResult(): DeArrowResult? =
    if (title == null && thumbnailUrl == null) null else DeArrowResult(title, thumbnailUrl)
