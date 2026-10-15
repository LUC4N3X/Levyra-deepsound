package com.luc4n3x.levyra.widget.studio

import android.graphics.Bitmap
import android.util.LruCache
import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import com.luc4n3x.levyra.widget.WidgetArtworkDownloader
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal object WidgetStudioArtwork {
    private const val SOURCE_MIN_SIDE = 640
    private const val SOURCE_CACHE_BYTES = 8 * 1024 * 1024

    private val sources = object : LruCache<String, Bitmap>(SOURCE_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val seeds = LruCache<String, Int>(24)
    private val failed = LruCache<String, Long>(24)
    private val inFlight: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "levyra-widget-artwork").apply { isDaemon = true }
    }

    @Volatile
    private var latestRequested: String = ""

    private val readyVersion = MutableStateFlow(0)
    val version: StateFlow<Int> = readyVersion.asStateFlow()

    fun source(url: String): Bitmap? = if (url.isBlank()) null else sources.get(url)

    fun seed(url: String): Int? = if (url.isBlank()) null else seeds.get(url)

    fun request(url: String, skipWhenStale: Boolean = true, onReady: () -> Unit) {
        if (url.isBlank() || sources.get(url) != null) return
        if (SafeImageUrlPolicy.sanitize(url).isEmpty()) return
        val lastFailure = failed.get(url)
        if (lastFailure != null && System.currentTimeMillis() - lastFailure < RETRY_AFTER_FAILURE_MS) return
        if (skipWhenStale) latestRequested = url
        if (!inFlight.add(url)) return
        executor.execute {
            try {
                if (skipWhenStale && latestRequested != url) return@execute
                val bitmap = WidgetArtworkDownloader.decode(url, SOURCE_MIN_SIDE)?.let(::bounded)
                if (bitmap == null) {
                    failed.put(url, System.currentTimeMillis())
                    return@execute
                }
                WidgetStudioColors.artworkSeed(bitmap)?.let { seeds.put(url, it) }
                sources.put(url, bitmap)
                readyVersion.update { it + 1 }
                onReady()
            } catch (error: Exception) {
                failed.put(url, System.currentTimeMillis())
                Timber.w(error, "Studio widget artwork fetch failed")
            } finally {
                inFlight.remove(url)
            }
        }
    }

    private fun bounded(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= SOURCE_MAX_SIDE) return bitmap
        val ratio = SOURCE_MAX_SIDE.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private const val SOURCE_MAX_SIDE = 720
    private const val RETRY_AFTER_FAILURE_MS = 60_000L
}
