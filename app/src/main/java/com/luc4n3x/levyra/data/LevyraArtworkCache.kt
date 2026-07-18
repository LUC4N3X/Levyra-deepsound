package com.luc4n3x.levyra.data

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
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

internal fun isLikelyArtworkBytes(bytes: ByteArray): Boolean {
    if (bytes.size >= 3 &&
        bytes[0] == 0xFF.toByte() &&
        bytes[1] == 0xD8.toByte() &&
        bytes[2] == 0xFF.toByte()
    ) {
        return true
    }
    if (bytes.size >= 8 &&
        bytes[0] == 0x89.toByte() &&
        bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x4E.toByte() &&
        bytes[3] == 0x47.toByte()
    ) {
        return true
    }
    if (bytes.size >= 12) {
        val prefix = bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII)
        val format = bytes.copyOfRange(8, 12).toString(Charsets.US_ASCII)
        if (prefix == "RIFF" && format == "WEBP") return true
        val box = bytes.copyOfRange(4, 8).toString(Charsets.US_ASCII)
        if (box == "ftyp" && format in setOf("avif", "avis", "mif1", "msf1", "heic", "heix")) return true
    }
    if (bytes.size >= 6) {
        val gif = bytes.copyOfRange(0, 6).toString(Charsets.US_ASCII)
        if (gif == "GIF87a" || gif == "GIF89a") return true
    }
    return false
}

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
                val memoryProfile = memoryDeviceProfile(appContext)
                val memoryCacheBytes = ArtworkMemoryCachePolicy.maxSizeBytes(memoryProfile)
                Timber.i(
                    "Artwork memory cache size=%dMB memoryClass=%dMB largeMemoryClass=%dMB lowRam=%s largeHeap=%s",
                    memoryCacheBytes / (1024L * 1024L),
                    memoryProfile.memoryClassMb,
                    memoryProfile.largeMemoryClassMb,
                    memoryProfile.lowRamDevice,
                    memoryProfile.largeHeapEnabled
                )
                ImageLoader.Builder(appContext)
                    .memoryCache {
                        MemoryCache.Builder()
                            .maxSizeBytes(memoryCacheBytes)
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

    private fun memoryDeviceProfile(context: Context): ArtworkMemoryDeviceProfile {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryClassMb = activityManager?.memoryClass?.coerceAtLeast(64) ?: 256
        val largeMemoryClassMb = activityManager?.largeMemoryClass?.coerceAtLeast(memoryClassMb) ?: memoryClassMb
        val lowRamDevice = activityManager?.isLowRamDevice == true
        val largeHeapEnabled = context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0
        return ArtworkMemoryDeviceProfile(
            memoryClassMb = memoryClassMb,
            largeMemoryClassMb = largeMemoryClassMb,
            lowRamDevice = lowRamDevice,
            largeHeapEnabled = largeHeapEnabled
        )
    }

    fun small(url: String): String = resize(url, SMALL_SIZE)

    fun large(url: String): String = resize(url, LARGE_SIZE)

    fun model(context: Context, track: Track, highRes: Boolean = false): Any? {
        return models(context, track, highRes).firstOrNull()
    }

    fun models(context: Context, track: Track, highRes: Boolean = false): List<Any> {
        val result = ArrayList<Any>(7)
        localFile(context, track, highRes)?.let(result::add)
        artworkUrlCandidates(track, highRes).forEach(result::add)
        return result.distinctBy { model ->
            when (model) {
                is File -> "file:${model.absolutePath}"
                else -> model.toString()
            }
        }
    }

    fun localFile(context: Context, track: Track, highRes: Boolean = false): File? {
        if (artworkUrlCandidates(track, highRes).isEmpty()) return null
        val file = persistentFile(context.applicationContext, track, if (highRes) LARGE_SIZE else SMALL_SIZE)
        if (!file.isFile || file.length() <= 512L) return null
        if (!isLikelyArtworkFile(file)) {
            runCatching { file.delete() }
            return null
        }
        return file
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
            .take(limit.coerceAtLeast(1))
            .mapNotNull { track -> artworkUrlCandidates(track, false).firstOrNull() }
            .distinct()
            .toList()
    }

    private data class ArtworkTarget(val urls: List<String>, val file: File)

    private fun target(context: Context, track: Track, highRes: Boolean): ArtworkTarget? {
        val urls = artworkUrlCandidates(track, highRes)
        if (urls.isEmpty()) return null
        val size = if (highRes) LARGE_SIZE else SMALL_SIZE
        return ArtworkTarget(urls, persistentFile(context, track, size))
    }

    private fun ensurePersistent(target: ArtworkTarget) {
        val file = target.file
        if (file.isFile && file.length() > 512L && isLikelyArtworkFile(file)) {
            file.setLastModified(System.currentTimeMillis())
            return
        }
        if (file.exists()) runCatching { file.delete() }
        target.urls.forEach { url ->
            repeat(2) { attempt ->
                val saved = runCatching {
                    file.parentFile?.mkdirs()
                    val request = Request.Builder()
                        .url(url)
                        .header("Accept", "image/webp,image/jpeg,image/png,image/*;q=0.9,*/*;q=0.5")
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0 Mobile Safari/537.36")
                        .build()
                    LevyraHttpClientFactory.media().newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use false
                        val body = response.body
                        val length = body.contentLength()
                        if (length > MAX_FILE_BYTES) return@use false
                        val bytes = body.bytes()
                        if (bytes.size < 512 || bytes.size.toLong() > MAX_FILE_BYTES || !isLikelyArtworkBytes(bytes)) return@use false
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
                if (saved || file.isFile && file.length() > 512L && isLikelyArtworkFile(file)) return
            }
        }
    }

    internal fun artworkUrlCandidates(track: Track, highRes: Boolean): List<String> {
        val ordered = if (highRes) {
            sequenceOf(track.largeThumbnailUrl, track.thumbnailUrl, LevyraPersonalOrbit.youtubeFallbackArtwork(track).orEmpty())
        } else {
            sequenceOf(track.thumbnailUrl, track.largeThumbnailUrl, LevyraPersonalOrbit.youtubeFallbackArtwork(track).orEmpty())
        }
        return ordered
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .flatMap { raw ->
                val resized = if (highRes) large(raw) else small(raw)
                sequenceOf(resized, raw)
            }
            .distinct()
            .take(6)
            .toList()
    }

    private fun isLikelyArtworkFile(file: File): Boolean {
        return runCatching {
            val header = ByteArray(16)
            val read = file.inputStream().use { input -> input.read(header) }
            read > 0 && isLikelyArtworkBytes(header.copyOf(read))
        }.getOrDefault(false)
    }

    private fun persistentFile(context: Context, track: Track, size: Int): File {
        return File(persistentDirectory(context), "${persistentKey(track, size)}.img")
    }

    private fun persistentDirectory(context: Context): File {
        return File(context.applicationContext.filesDir, "levyra_artwork")
    }

    private fun persistentKey(track: Track, size: Int): String {
        val rawArtwork = artworkUrlCandidates(track, size >= LARGE_SIZE).firstOrNull().orEmpty()
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
