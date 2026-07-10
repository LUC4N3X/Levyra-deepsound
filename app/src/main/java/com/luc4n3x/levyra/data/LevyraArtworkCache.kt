package com.luc4n3x.levyra.data

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.nio.charset.StandardCharsets

object LevyraArtworkCache {
    private const val SMALL_SIZE = 192
    private const val LARGE_SIZE = 512
    private const val MAX_FILE_BYTES = 6L * 1024L * 1024L
    private const val MAX_PERSISTENT_FILES = 220
    private val youtubeWidthHeight = Regex("=w\\d+-h\\d+[^?&]*")
    private val youtubeSquare = Regex("=s\\d+[^?&]*")
    private val appleArtwork = Regex("\\d+x\\d+bb")
    @Volatile private var configured = false

    fun configure(context: Context) {
        if (configured) return
        synchronized(this) {
            if (configured) return
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
            configured = true
        }
    }

    fun small(url: String): String = resize(url, SMALL_SIZE)

    fun large(url: String): String = resize(url, LARGE_SIZE)

    fun model(context: Context, track: Track, highRes: Boolean = false): Any? {
        val local = localFile(context, track, highRes)
        if (local != null) return local
        val raw = if (highRes) track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } else track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }
        if (raw.isBlank()) return null
        return if (highRes) large(raw) else small(raw)
    }

    fun localFile(context: Context, track: Track, highRes: Boolean = false): File? {
        val raw = if (highRes) track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } else track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }
        if (raw.isBlank()) return null
        val file = persistentFile(context.applicationContext, track, if (highRes) LARGE_SIZE else SMALL_SIZE)
        return file.takeIf { it.isFile && it.length() > 512L }
    }

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

    suspend fun cachePersistent(context: Context, tracks: List<Track>, limit: Int = 12) {
        if (tracks.isEmpty()) return
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val limitedTracks = tracks.take(limit.coerceAtLeast(1))
            val smallTargets = limitedTracks
                .mapNotNull { track -> target(appContext, track, false) }
                .distinctBy { it.file.name }
            val largeTargets = limitedTracks
                .mapNotNull { track -> target(appContext, track, true) }
                .distinctBy { it.file.name }
            if (smallTargets.isEmpty() && largeTargets.isEmpty()) return@withContext
            cacheTargets(smallTargets, 3)
            cacheTargets(largeTargets, 2)
            trimPersistentDirectory(appContext)
        }
    }

    private suspend fun cacheTargets(targets: List<ArtworkTarget>, parallelism: Int) {
        if (targets.isEmpty()) return
        val semaphore = Semaphore(parallelism.coerceAtLeast(1))
        coroutineScope {
            targets.map { target ->
                async {
                    semaphore.withPermit { ensurePersistent(target) }
                }
            }.awaitAll()
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

    private data class ArtworkTarget(val url: String, val file: File)

    private fun target(context: Context, track: Track, highRes: Boolean): ArtworkTarget? {
        val raw = if (highRes) track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } else track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }
        if (raw.isBlank()) return null
        val size = if (highRes) LARGE_SIZE else SMALL_SIZE
        return ArtworkTarget(if (highRes) large(raw) else small(raw), persistentFile(context, track, size))
    }

    private fun ensurePersistent(target: ArtworkTarget) {
        val file = target.file
        if (file.isFile && file.length() > 512L) {
            file.setLastModified(System.currentTimeMillis())
            return
        }
        repeat(2) { attempt ->
            val saved = runCatching {
                file.parentFile?.mkdirs()
                val request = Request.Builder()
                    .url(target.url)
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0 Mobile Safari/537.36")
                    .build()
                LevyraHttpClientFactory.media().newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use false
                    val body = response.body ?: return@use false
                    val length = body.contentLength()
                    if (length > MAX_FILE_BYTES) return@use false
                    val bytes = body.bytes()
                    if (bytes.size < 512 || bytes.size.toLong() > MAX_FILE_BYTES) return@use false
                    val temp = File(file.parentFile, "${file.name}.tmp")
                    temp.writeBytes(bytes)
                    if (file.exists()) file.delete()
                    if (!temp.renameTo(file)) {
                        temp.copyTo(file, overwrite = true)
                        temp.delete()
                    }
                    file.setLastModified(System.currentTimeMillis())
                    true
                }
            }.onFailure { error ->
                if (attempt == 1) Timber.d(error, "Artwork persistent cache miss")
            }.getOrDefault(false)
            if (saved || file.isFile && file.length() > 512L) return
        }
    }

    private fun persistentFile(context: Context, track: Track, size: Int): File {
        return File(persistentDirectory(context), "${persistentKey(track, size)}.img")
    }

    private fun persistentDirectory(context: Context): File {
        return File(context.applicationContext.filesDir, "levyra_artwork")
    }

    private fun persistentKey(track: Track, size: Int): String {
        val rawArtwork = if (size >= LARGE_SIZE) track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } else track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }
        val stable = buildString {
            append(size)
            append('|')
            append(track.id.trim())
            append('|')
            append(track.title.trim().lowercase())
            append('|')
            append(track.artist.trim().lowercase())
            append('|')
            append(resize(rawArtwork, size).trim())
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(stable.toByteArray(StandardCharsets.UTF_8))
            .take(16)
            .joinToString("") { "%02x".format(it) }
    }

    private fun trimPersistentDirectory(context: Context) {
        val files = persistentDirectory(context).listFiles()?.filter { it.isFile }.orEmpty()
        if (files.size <= MAX_PERSISTENT_FILES) return
        files
            .sortedBy { it.lastModified() }
            .take(files.size - MAX_PERSISTENT_FILES)
            .forEach { runCatching { it.delete() } }
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
