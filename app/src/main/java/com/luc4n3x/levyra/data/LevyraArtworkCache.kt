package com.luc4n3x.levyra.data

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.domain.Track
import okio.Path.Companion.toOkioPath

object LevyraArtworkCache {
    private const val SMALL_SIZE = 192
    private const val LARGE_SIZE = 512
    private val youtubeWidthHeight = Regex("=w\\d+-h\\d+[^?&]*")
    private val youtubeSquare = Regex("=s\\d+[^?&]*")
    private val appleArtwork = Regex("\\d+x\\d+bb")

    fun configure(context: Context) {
        SingletonImageLoader.setSafe { appContext ->
            ImageLoader.Builder(appContext)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(appContext, 0.35)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(appContext.cacheDir.resolve("levyra_images").toOkioPath())
                        .maxSizeBytes(384L * 1024 * 1024)
                        .build()
                }
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .build()
        }
    }

    fun small(url: String): String = resize(url, SMALL_SIZE)

    fun large(url: String): String = resize(url, LARGE_SIZE)

    fun preloadHome(context: Context, tracks: List<Track>, limit: Int = 36) {
        if (tracks.isEmpty()) return
        val appContext = context.applicationContext
        configure(appContext)
        val loader = SingletonImageLoader.get(appContext)
        artworkUrls(tracks, limit).forEach { url ->
            loader.enqueue(preloadRequest(appContext, url))
        }
    }

    fun preloadPriority(context: Context, tracks: List<Track>, limit: Int = 10) {
        if (tracks.isEmpty()) return
        val appContext = context.applicationContext
        configure(appContext)
        val loader = SingletonImageLoader.get(appContext)
        artworkUrls(tracks, limit).forEach { url ->
            loader.enqueue(preloadRequest(appContext, url))
            loader.enqueue(preloadRequest(appContext, large(url)))
        }
    }

    private fun preloadRequest(context: Context, url: String): ImageRequest {
        return ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    private fun artworkUrls(tracks: List<Track>, limit: Int): List<String> {
        return tracks
            .asSequence()
            .flatMap { track -> sequenceOf(track.thumbnailUrl, track.largeThumbnailUrl) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map(::small)
            .distinct()
            .take(limit.coerceAtLeast(1))
            .toList()
    }

    private fun resize(url: String, size: Int): String {
        val clean = url.trim()
        if (clean.isBlank()) return clean
        val square = size.coerceIn(96, 1024)
        return when {
            youtubeWidthHeight.containsMatchIn(clean) -> clean.replace(youtubeWidthHeight, "=w${square}-h${square}-l90-rj")
            youtubeSquare.containsMatchIn(clean) -> clean.replace(youtubeSquare, "=s${square}")
            appleArtwork.containsMatchIn(clean) -> clean.replace(appleArtwork, "${square}x${square}bb")
            else -> clean
        }
    }
}
