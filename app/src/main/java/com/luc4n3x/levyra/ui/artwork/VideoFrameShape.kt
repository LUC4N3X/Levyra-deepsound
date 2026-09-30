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
import kotlin.math.abs
import kotlin.math.max

private const val SIDE_UNIFORM_TOLERANCE = 22
private const val SIDE_UNIFORM_SHARE = 0.92f
private const val SIDE_MATCH_TOLERANCE = 28
private const val CENTER_CONTRAST = 28
private const val CENTER_CONTRAST_SHARE = 0.35f
private const val MAX_CACHED_FRAMES = 96

internal object VideoFrameShapeCache {
    private val results = object : LinkedHashMap<String, Boolean>(MAX_CACHED_FRAMES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean =
            size > MAX_CACHED_FRAMES
    }

    fun get(videoId: String): Boolean? = synchronized(results) { results[videoId] }

    fun put(videoId: String, pillarboxed: Boolean) {
        synchronized(results) { results[videoId] = pillarboxed }
    }
}

internal suspend fun detectPillarboxedVideoFrame(context: Context, videoId: String): Boolean? =
    VideoFrameShapeCache.get(videoId) ?: probeVideoFrame(context, videoId)?.let { frame ->
        withContext(Dispatchers.Default) { isPillarboxedVideoFrame(frame.values, frame.width, frame.height) }
            .also { pillarboxed -> VideoFrameShapeCache.put(videoId, pillarboxed) }
    }

private suspend fun probeVideoFrame(context: Context, videoId: String): FramePixels? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context)
        .data("https://i.ytimg.com/vi/$videoId/default.jpg")
        .size(120, 90)
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

private class FramePixels(val values: IntArray, val width: Int, val height: Int)

internal fun isPillarboxedVideoFrame(pixels: IntArray, width: Int, height: Int): Boolean {
    if (width < 16 || height < 16 || pixels.size < width * height) return false
    val top = (height * 0.30f).toInt()
    val bottom = (height * 0.70f).toInt()
    val left = sideStats(pixels, width, FrameRegion((width * 0.03f).toInt(), (width * 0.16f).toInt(), top, bottom))
    val right = sideStats(pixels, width, FrameRegion((width * 0.84f).toInt(), (width * 0.97f).toInt(), top, bottom))
    val sidesMatch = left.uniform && right.uniform &&
        channelDistance(left.mean, right.mean) <= SIDE_MATCH_TOLERANCE
    return sidesMatch && centerContrasts(pixels, width, top, bottom, averageColor(left.mean, right.mean))
}

private fun centerContrasts(pixels: IntArray, width: Int, top: Int, bottom: Int, sideMean: Int): Boolean {
    var contrasting = 0
    var total = 0
    for (y in top until bottom) {
        for (x in (width * 0.40f).toInt() until (width * 0.60f).toInt()) {
            total += 1
            if (channelDistance(pixels[y * width + x], sideMean) > CENTER_CONTRAST) contrasting += 1
        }
    }
    return total > 0 && contrasting >= total * CENTER_CONTRAST_SHARE
}

private class SideStats(val mean: Int, val uniform: Boolean)

private class FrameRegion(val fromX: Int, val toX: Int, val top: Int, val bottom: Int)

private fun sideStats(pixels: IntArray, width: Int, region: FrameRegion): SideStats {
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0
    for (y in region.top until region.bottom) {
        for (x in region.fromX until region.toX) {
            val color = pixels[y * width + x]
            red += (color shr 16) and 0xFF
            green += (color shr 8) and 0xFF
            blue += color and 0xFF
            count += 1
        }
    }
    if (count == 0) return SideStats(0, false)
    val mean = rgb((red / count).toInt(), (green / count).toInt(), (blue / count).toInt())
    var close = 0
    for (y in region.top until region.bottom) {
        for (x in region.fromX until region.toX) {
            if (channelDistance(pixels[y * width + x], mean) <= SIDE_UNIFORM_TOLERANCE) close += 1
        }
    }
    return SideStats(mean, close >= count * SIDE_UNIFORM_SHARE)
}

private fun channelDistance(first: Int, second: Int): Int = max(
    abs(((first shr 16) and 0xFF) - ((second shr 16) and 0xFF)),
    max(
        abs(((first shr 8) and 0xFF) - ((second shr 8) and 0xFF)),
        abs((first and 0xFF) - (second and 0xFF))
    )
)

private fun averageColor(first: Int, second: Int): Int = rgb(
    (((first shr 16) and 0xFF) + ((second shr 16) and 0xFF)) / 2,
    (((first shr 8) and 0xFF) + ((second shr 8) and 0xFF)) / 2,
    ((first and 0xFF) + (second and 0xFF)) / 2
)

private fun rgb(red: Int, green: Int, blue: Int): Int =
    (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
