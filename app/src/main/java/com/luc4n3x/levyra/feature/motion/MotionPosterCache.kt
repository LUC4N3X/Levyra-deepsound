package com.luc4n3x.levyra.feature.motion

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.compose.runtime.mutableIntStateOf
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object MotionPosterCache {
    private const val SOURCE_CAPACITY = 4
    private const val FRAME_CAPACITY = 3
    private const val SOURCE_SIZE = 256
    private const val FRAME_MAX_WIDTH = 540

    private val revision = mutableIntStateOf(0)
    private val sources = boundedMap(SOURCE_CAPACITY)
    private val frames = boundedMap(FRAME_CAPACITY)

    fun displayRequest(context: Context, url: String): ImageRequest =
        ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build()

    fun posterKey(artwork: MotionArtwork?): String? = when {
        artwork == null -> null
        !artwork.posterUrl.isNullOrBlank() -> artwork.posterUrl
        artwork.mimeType.startsWith(MP4_MIME, ignoreCase = true) && artwork.url.startsWith("https://") -> artwork.url
        else -> null
    }

    fun source(key: String): Bitmap? {
        revision.intValue
        return synchronized(sources) { sources[key] }
    }

    fun frame(key: String): Bitmap? {
        revision.intValue
        return synchronized(frames) { frames[key] }
    }

    suspend fun warm(context: Context, artwork: MotionArtwork) {
        val key = posterKey(artwork) ?: return
        val poster = artwork.posterUrl
        if (!poster.isNullOrBlank()) {
            if (synchronized(sources) { sources.containsKey(key) }) return
            warmImage(context.applicationContext, poster)
        } else {
            if (synchronized(frames) { frames.containsKey(key) }) return
            warmVideoFrame(key)
        }
    }

    private suspend fun warmImage(context: Context, url: String) {
        val loader = SingletonImageLoader.get(context)
        val source = withContext(Dispatchers.IO) {
            guarded {
                loader.execute(displayRequest(context, url))
                val small = ImageRequest.Builder(context).data(url).size(SOURCE_SIZE, SOURCE_SIZE).allowHardware(false).build()
                (loader.execute(small) as? SuccessResult)?.image?.toBitmap()
            }
        } ?: return
        publish(url, source, null)
    }

    private suspend fun warmVideoFrame(url: String) {
        val frame = withContext(Dispatchers.IO) { guarded { firstVideoFrame(url) } } ?: return
        val source = Bitmap.createScaledBitmap(frame, SOURCE_SIZE, (SOURCE_SIZE.toLong() * frame.height / frame.width).toInt().coerceAtLeast(1), true)
        publish(url, source, frame)
    }

    private fun firstVideoFrame(url: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(url, emptyMap())
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            if (width <= 0 || height <= 0) return null
            val targetWidth = minOf(FRAME_MAX_WIDTH, width)
            val targetHeight = (targetWidth.toLong() * height / width).toInt().coerceAtLeast(1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, targetWidth, targetHeight)
            } else {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.let { Bitmap.createScaledBitmap(it, targetWidth, targetHeight, true) }
            }
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun publish(key: String, source: Bitmap, frame: Bitmap?) {
        synchronized(sources) { sources[key] = source }
        if (frame != null) synchronized(frames) { frames[key] = frame }
        revision.intValue += 1
    }

    private inline fun <T> guarded(block: () -> T?): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Timber.d(error, "Motion poster unavailable")
        null
    }

    private fun boundedMap(capacity: Int) = object : LinkedHashMap<String, Bitmap>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > capacity
    }

    private const val MP4_MIME = "video/mp4"
}
