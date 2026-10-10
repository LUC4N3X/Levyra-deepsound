package com.luc4n3x.levyra.ui.artwork

import android.content.Context
import android.graphics.Bitmap
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PROBE_WIDTH = 160
private const val PROBE_HEIGHT = 90
private const val MAX_CACHED_FRAMES = 96

internal object VideoFrameFitCache {
    private val results = object : LinkedHashMap<String, VideoFrameFit>(MAX_CACHED_FRAMES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, VideoFrameFit>?): Boolean =
            size > MAX_CACHED_FRAMES
    }

    fun get(videoId: String): VideoFrameFit? = synchronized(results) { results[videoId] }

    fun put(videoId: String, fit: VideoFrameFit) {
        synchronized(results) { results[videoId] = fit }
    }
}

internal suspend fun detectVideoFrameFit(context: Context, videoId: String): VideoFrameFit? =
    VideoFrameFitCache.get(videoId) ?: probeDisplayedFrame(context, videoId)?.let { frame ->
        withContext(Dispatchers.Default) { measureVideoFrameFit(frame.values, frame.width, frame.height) }
            .also { fit -> VideoFrameFitCache.put(videoId, fit) }
    }

private suspend fun probeDisplayedFrame(context: Context, videoId: String): FramePixels? =
    loadFramePixels(context, "https://i.ytimg.com/vi/$videoId/hq720.jpg", PROBE_HEIGHT)
        ?: loadFramePixels(context, "https://i.ytimg.com/vi/$videoId/hqdefault.jpg", PROBE_WIDTH * 3 / 4)
            ?.centerWideCrop()

private suspend fun loadFramePixels(context: Context, url: String, height: Int): FramePixels? =
    withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(PROBE_WIDTH, height)
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        SingletonImageLoader.get(context).execute(request).image?.toBitmap()?.let { bitmap ->
            val values = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(values, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            FramePixels(values, bitmap.width, bitmap.height)
        }
    }

private fun FramePixels.centerWideCrop(): FramePixels {
    val top = height / 8
    val cropHeight = height - top * 2
    return FramePixels(values.copyOfRange(top * width, (top + cropHeight) * width), width, cropHeight)
}
