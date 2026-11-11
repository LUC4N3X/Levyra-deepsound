package com.luc4n3x.levyra.feature.motion

import android.content.Context
import android.graphics.Bitmap
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
    private const val CAPACITY = 4
    private const val SOURCE_SIZE = 256

    private val revision = mutableIntStateOf(0)
    private val sources = object : LinkedHashMap<String, Bitmap>(CAPACITY, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > CAPACITY
    }

    fun displayRequest(context: Context, url: String): ImageRequest =
        ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build()

    fun source(url: String): Bitmap? {
        revision.intValue
        return synchronized(sources) { sources[url] }
    }

    suspend fun warm(context: Context, url: String) {
        if (!url.startsWith("https://") || synchronized(sources) { sources.containsKey(url) }) return
        val appContext = context.applicationContext
        val loader = SingletonImageLoader.get(appContext)
        val source = withContext(Dispatchers.IO) {
            try {
                loader.execute(displayRequest(appContext, url))
                val small = ImageRequest.Builder(appContext).data(url).size(SOURCE_SIZE, SOURCE_SIZE).allowHardware(false).build()
                (loader.execute(small) as? SuccessResult)?.image?.toBitmap()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.d(error, "Motion poster unavailable")
                null
            }
        } ?: return
        synchronized(sources) { sources[url] = source }
        revision.intValue += 1
    }
}
