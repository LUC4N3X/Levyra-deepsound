package com.luc4n3x.levyra.ui.artwork

import android.content.Context
import android.graphics.Bitmap
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
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

internal suspend fun detectPillarboxedVideoFrame(context: Context, videoId: String): Boolean? {
    VideoFrameShapeCache.get(videoId)?.let { return it }
    val pixels = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data("https://i.ytimg.com/vi/$videoId/default.jpg")
            .size(120, 90)
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        val bitmap = try {
            SingletonImageLoader.get(context).execute(request).image?.toBitmap()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.d(error, "Video frame shape probe failed")
            null
        } ?: return@withContext null
        FramePixels(
            IntArray(bitmap.width * bitmap.height).also { buffer ->
                bitmap.getPixels(buffer, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            },
            bitmap.width,
            bitmap.height
        )
    } ?: return null
    val pillarboxed = withContext(Dispatchers.Default) {
        isPillarboxedVideoFrame(pixels.values, pixels.width, pixels.height)
    }
    VideoFrameShapeCache.put(videoId, pillarboxed)
    return pillarboxed
}

private class FramePixels(val values: IntArray, val width: Int, val height: Int)

internal fun isPillarboxedVideoFrame(pixels: IntArray, width: Int, height: Int): Boolean {
    if (width < 16 || height < 16 || pixels.size < width * height) return false
    val top = (height * 0.30f).toInt()
    val bottom = (height * 0.70f).toInt()
    val left = sideStats(pixels, width, (width * 0.03f).toInt(), (width * 0.16f).toInt(), top, bottom)
    val right = sideStats(pixels, width, (width * 0.84f).toInt(), (width * 0.97f).toInt(), top, bottom)
    if (!left.uniform || !right.uniform) return false
    if (channelDistance(left.mean, right.mean) > SIDE_MATCH_TOLERANCE) return false
    val sideMean = averageColor(left.mean, right.mean)
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

private fun sideStats(pixels: IntArray, width: Int, fromX: Int, toX: Int, top: Int, bottom: Int): SideStats {
    var red = 0L
    var green = 0L
    var blue = 0L
    var count = 0
    for (y in top until bottom) {
        for (x in fromX until toX) {
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
    for (y in top until bottom) {
        for (x in fromX until toX) {
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
