package com.luc4n3x.levyra.player.offline

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.LevyraDownloadFolderMode
import com.luc4n3x.levyra.domain.LevyraDownloadPreset
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aMetadata
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aTagWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import timber.log.Timber

internal const val DEFAULT_PARALLEL_RANGE_CHUNK_BYTES = 2L * 1024L * 1024L
internal const val MIN_PARALLEL_AUDIO_BYTES = 8L * 1024L * 1024L
internal const val FAST_METADATA_EMBED_MAX_BYTES = 32L * 1024L * 1024L
internal const val FAST_METADATA_EMBED_MAX_DURATION_MS = 20L * 60L * 1000L

internal data class AudioDownloadRange(
    val start: Long,
    val endInclusive: Long
) {
    val length: Long
        get() = endInclusive - start + 1L
}

internal fun planParallelAudioRanges(
    contentLength: Long,
    chunkSize: Long = DEFAULT_PARALLEL_RANGE_CHUNK_BYTES,
    minLength: Long = MIN_PARALLEL_AUDIO_BYTES
): List<AudioDownloadRange> {
    if (contentLength < minLength || chunkSize <= 0L) return emptyList()
    val ranges = mutableListOf<AudioDownloadRange>()
    var start = 0L
    while (start < contentLength) {
        val end = minOf(start + chunkSize - 1L, contentLength - 1L)
        ranges += AudioDownloadRange(start = start, endInclusive = end)
        start = end + 1L
    }
    return ranges
}

internal fun parallelAudioChunkSize(contentLength: Long): Long {
    val oneMb = 1024L * 1024L
    return when {
        contentLength >= 256L * oneMb -> 8L * oneMb
        contentLength >= 96L * oneMb -> 4L * oneMb
        contentLength >= 24L * oneMb -> 3L * oneMb
        else -> 2L * oneMb
    }
}

internal fun parallelAudioConcurrency(contentLength: Long): Int {
    val oneMb = 1024L * 1024L
    return when {
        contentLength >= 256L * oneMb -> 10
        contentLength >= 96L * oneMb -> 8
        contentLength >= 24L * oneMb -> 6
        else -> 4
    }
}

internal fun isUsableAudioRangeResponse(
    code: Int,
    bodyLength: Long,
    contentRange: String,
    range: AudioDownloadRange,
    rangeParamApplied: Boolean
): Boolean {
    if (code == 206) {
        if (bodyLength > 0L && bodyLength != range.length) return false
        if (contentRange.isBlank()) return true
        val bounds = contentRange.substringAfter("bytes", contentRange)
            .substringBefore('/')
            .trim()
            .split('-', limit = 2)
        val start = bounds.getOrNull(0)?.trim()?.toLongOrNull()
        val end = bounds.getOrNull(1)?.trim()?.toLongOrNull()
        return start == range.start && end == range.endInclusive
    }
    if (!rangeParamApplied || code !in 200..299) return false
    if (bodyLength != range.length) return false
    if (contentRange.isBlank()) return true
    return contentRange.contains("${range.start}-${range.endInclusive}") &&
        contentRange.substringAfterLast('/').toLongOrNull() != null
}

internal fun audioContentLengthFromUrl(url: String): Long {
    return url.toHttpUrlOrNull()
        ?.queryParameter("clen")
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
        ?: Regex("(?:[?&])clen=(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()
        ?: -1L
}

internal fun audioContentTypeFromUrl(url: String): String {
    return url.toHttpUrlOrNull()
        ?.queryParameter("mime")
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.US)
        .orEmpty()
}

internal fun stripAudioRangeParameters(url: String): String {
    val fragmentIndex = url.indexOf('#')
    val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""
    val source = if (fragmentIndex >= 0) url.substring(0, fragmentIndex) else url
    val queryIndex = source.indexOf('?')
    if (queryIndex < 0) return url
    val base = source.substring(0, queryIndex)
    val query = source.substring(queryIndex + 1)
    val retained = query
        .split('&')
        .filterNot { parameter -> parameter.substringBefore('=').equals("range", ignoreCase = true) }
    val normalized = if (retained.isEmpty()) base else "$base?${retained.joinToString("&")}"
    return normalized + fragment
}

internal fun shouldEmbedFastMetadata(fileLength: Long, durationMs: Long = 0L): Boolean {
    val durationIsFast = durationMs <= 0L || durationMs <= FAST_METADATA_EMBED_MAX_DURATION_MS
    return durationIsFast && fileLength in 1L..FAST_METADATA_EMBED_MAX_BYTES
}

private class DownloadRateLimiter(maxRateKbps: Int) {
    private val maxBytesPerSecond = maxRateKbps.toLong().coerceAtLeast(0L) * 125L
    private val mutex = Mutex()
    private var windowStartedAtNanos = System.nanoTime()
    private var windowBytes = 0L

    suspend fun consume(bytes: Int) {
        if (maxBytesPerSecond <= 0L || bytes <= 0) return
        mutex.withLock {
            windowBytes += bytes.toLong()
            val elapsedNanos = (System.nanoTime() - windowStartedAtNanos).coerceAtLeast(1L)
            val expectedNanos = windowBytes * 1_000_000_000L / maxBytesPerSecond
            if (expectedNanos > elapsedNanos) {
                val waitMs = ((expectedNanos - elapsedNanos) / 1_000_000L).coerceIn(1L, 2_000L)
                delay(waitMs)
            }
            if (System.nanoTime() - windowStartedAtNanos >= 1_000_000_000L) {
                windowStartedAtNanos = System.nanoTime()
                windowBytes = 0L
            }
        }
    }
}

class OfflineAudioExporter(
    private val context: Context,
    private val resolver: PlaybackResolver,
    private val client: OkHttpClient = LevyraHttpClientFactory.download(),
    private val progress: suspend (Int) -> Unit = {},
    private val taskKey: String = "",
    private val settings: LevyraDownloadSettings = LevyraDownloadSettings()
) {
    private val rateLimiter = DownloadRateLimiter(settings.effectiveRateKbps)
    val embeddedMetadataWriterReady: Boolean
        get() = LevyraM4aTagWriter.isAvailable

    suspend fun export(track: Track): OfflineExportResult = withContext(Dispatchers.IO) {
        reportProgress(1)
        val forceQualityResolution = settings.resolverAudioQuality != null
        var playable = if (track.streamUrl.isNotBlank() && !forceQualityResolution) {
            track
        } else {
            reportProgress(4)
            resolver.resolveForOffline(track.copy(streamUrl = ""), settings.resolverAudioQuality)
        }
        if (playable.streamUrl.isBlank()) throw IOException("Stream audio non disponibile")
        reportProgress(10)
        val workspace = File(context.cacheDir, "levyra_offline_export").apply { mkdirs() }
        Timber.i("Offline export started: %s", track.title)
        cleanupWorkspace(workspace)
        val downloaded = runCatching {
            downloadAudio(playable, workspace)
        }.getOrElse { firstError ->
            if (firstError is CancellationException) throw firstError
            val canRefresh = track.id.isNotBlank() || track.videoUrl.isNotBlank()
            if (!canRefresh) throw firstError
            reportProgress(7)
            playable = resolver.resolveForOffline(track.copy(streamUrl = ""), settings.resolverAudioQuality)
            reportProgress(10)
            downloadAudio(playable, workspace)
        }
        var embeddedFile: PreparedAudioFile? = null
        try {
            reportProgress(84)
            val artwork = if (settings.embedMetadata && settings.embedArtwork && downloaded.container.supportsEmbeddedMetadata && shouldEmbedFastMetadata(downloaded.file.length(), playable.durationMs)) {
                downloadArtwork(playable)
            } else {
                null
            }
            reportProgress(88)
            embeddedFile = maybeEmbedMetadata(downloaded.file, playable, artwork, downloaded.container, workspace)
            reportProgress(90)
            if (settings.verifyFile) verifyAudioFile(embeddedFile.file, embeddedFile.container)
            reportProgress(92)
            val exported = saveToMusicCollection(embeddedFile.file, playable, embeddedFile.container)
            reportProgress(98)
            val fileName = buildFileName(playable, embeddedFile.container.extension)
            persistDownload(track, playable, fileName, exported.uri, embeddedFile.container, embeddedFile.fileMetadataEmbedded)
            Timber.i("Offline export completed: %s", fileName)
            reportProgress(100)
            OfflineExportResult(
                uri = exported.uri,
                fileName = fileName,
                fileMetadataEmbedded = embeddedFile.fileMetadataEmbedded,
                mimeType = embeddedFile.container.mimeType,
                destinationLabel = exported.destinationLabel
            )
        } finally {
            runCatching { downloaded.file.delete() }
            embeddedFile?.file?.takeIf { it != downloaded.file }?.let { runCatching { it.delete() } }
        }
    }

    private suspend fun downloadAudio(track: Track, workspace: File): DownloadedAudio {
        var lastError: IOException? = null
        val rangeAttempts = listOf(false, true, false)
        for ((index, useRange) in rangeAttempts.withIndex()) {
            try {
                return downloadAudioAttempt(track, workspace, useRange)
            } catch (error: IOException) {
                lastError = error
                if (index < rangeAttempts.lastIndex) delay(350L * (index + 1))
            }
        }
        throw lastError ?: IOException("Download audio non riuscito")
    }

    private suspend fun downloadAudioAttempt(track: Track, workspace: File, useRange: Boolean): DownloadedAudio {
        reportProgress(12)
        val sourceUrl = stripAudioRangeParameters(track.streamUrl)
        val probe = probeAudio(sourceUrl)
        val expectedLength = probe.contentLength
        val contentType = probe.contentType
        val container = detectContainer(contentType, sourceUrl)
        val partial = resumablePartialFile(workspace, container)
        val existingBytes = partial.takeIf { settings.resumable && it.exists() }?.length()?.coerceAtLeast(0L) ?: 0L
        ensureStorageAvailable(workspace, expectedLength, existingBytes)
        if (expectedLength > 0L && existingBytes == expectedLength) {
            reportProgress(82)
            return DownloadedAudio(partial, container)
        }
        val parallelRanges = if (!useRange && existingBytes == 0L) {
            planParallelAudioRanges(
                contentLength = expectedLength,
                chunkSize = parallelAudioChunkSizeForSettings(expectedLength)
            )
        } else {
            emptyList()
        }
        if (parallelRanges.isNotEmpty()) {
            try {
                return downloadAudioRanges(
                    track = track.copy(streamUrl = sourceUrl),
                    workspace = workspace,
                    targetLength = expectedLength,
                    contentType = contentType,
                    ranges = parallelRanges
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                Timber.w(error, "Parallel offline download failed, falling back to serial")
            }
        }
        val rangeStart = existingBytes.takeIf { settings.resumable && it > 0L && expectedLength > it } ?: 0L
        val downloadUrl = when {
            rangeStart > 0L -> withGoogleVideoRange(sourceUrl, AudioDownloadRange(rangeStart, (expectedLength - 1L).coerceAtLeast(rangeStart)))
            useRange -> withGoogleVideoRange(sourceUrl, expectedLength)
            else -> sourceUrl
        }
        val rangeParamApplied = downloadUrl != sourceUrl
        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "audio/*,*/*;q=0.8")
            .header("Accept-Encoding", "identity")
            .header("Connection", "keep-alive")
            .apply {
                when {
                    rangeStart > 0L && !rangeParamApplied -> header("Range", "bytes=$rangeStart-")
                    useRange && !rangeParamApplied -> header("Range", "bytes=0-")
                }
            }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download audio fallito: HTTP ${response.code}")
            val body = response.body
            val declaredLength = body.contentLength()
            val contentRange = response.header("Content-Range").orEmpty()
            if (rangeStart > 0L) {
                val resumeRange = AudioDownloadRange(rangeStart, expectedLength - 1L)
                if (!isUsableAudioRangeResponse(response.code, declaredLength, contentRange, resumeRange, rangeParamApplied)) {
                    throw IOException("Ripresa audio non supportata: HTTP ${response.code}")
                }
            }
            val append = rangeStart > 0L
            val baseBytes = if (append) rangeStart else 0L
            if (!append && partial.exists()) runCatching { partial.delete() }
            val contentRangeTotal = contentRange.substringAfterLast('/').toLongOrNull() ?: -1L
            val targetLength = when {
                contentRangeTotal > 0L -> contentRangeTotal
                expectedLength > 0L -> expectedLength
                declaredLength > 0L -> baseBytes + declaredLength
                else -> -1L
            }
            if (targetLength > MAX_AUDIO_BYTES) throw IOException("File troppo grande per l'esportazione")
            try {
                body.byteStream().use { input ->
                    FileOutputStream(partial, append).use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                        var total = baseBytes
                        var lastProgress = downloadProgress(total, targetLength)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            rateLimiter.consume(read)
                            total += read.toLong()
                            if (total > MAX_AUDIO_BYTES) throw IOException("File troppo grande per l'esportazione")
                            output.write(buffer, 0, read)
                            val nextProgress = downloadProgress(total, targetLength)
                            if (nextProgress > lastProgress) {
                                lastProgress = nextProgress
                                reportProgress(nextProgress)
                            }
                        }
                        output.flush()
                        if (targetLength > 0L && total != targetLength) throw IOException("Download non valido: $total/$targetLength byte")
                    }
                }
                if (partial.length() <= 0L) throw IOException("File audio esportato vuoto")
                reportProgress(82)
                return DownloadedAudio(partial, container)
            } catch (error: IOException) {
                if (!settings.resumable) runCatching { partial.delete() }
                throw error
            }
        }
    }

    private fun ensureStorageAvailable(workspace: File, expectedLength: Long, existingBytes: Long) {
        val remainingDownloadBytes = if (expectedLength > 0L) {
            (expectedLength - existingBytes).coerceAtLeast(0L)
        } else {
            UNKNOWN_LENGTH_STORAGE_ALLOWANCE_BYTES
        }
        val mediaStoreCopyBytes = expectedLength.takeIf { it > 0L } ?: UNKNOWN_LENGTH_STORAGE_ALLOWANCE_BYTES
        val requiredBytes = remainingDownloadBytes
            .coerceAtMost(MAX_AUDIO_BYTES)
            .plus(mediaStoreCopyBytes.coerceAtMost(MAX_AUDIO_BYTES))
            .plus(MIN_FREE_STORAGE_RESERVE_BYTES)
        if (workspace.usableSpace < requiredBytes) {
            throw IOException("Spazio insufficiente: servono almeno ${formatStorageBytes(requiredBytes)} liberi")
        }
    }

    private fun formatStorageBytes(bytes: Long): String {
        val megabytes = bytes.toDouble() / (1024.0 * 1024.0)
        return if (megabytes >= 1024.0) {
            String.format(Locale.US, "%.1f GB", megabytes / 1024.0)
        } else {
            String.format(Locale.US, "%.0f MB", megabytes)
        }
    }

    private fun resumablePartialFile(workspace: File, container: AudioContainer): File {
        val safeKey = taskKey.ifBlank { "${System.nanoTime()}" }
            .replace(Regex("[^A-Za-z0-9_.-]+"), "_")
            .take(120)
        return File(workspace, "resume-$safeKey.${container.extension}.part")
    }

    private suspend fun downloadAudioRanges(
        track: Track,
        workspace: File,
        targetLength: Long,
        contentType: String,
        ranges: List<AudioDownloadRange>
    ): DownloadedAudio = coroutineScope {
        val container = detectContainer(contentType, track.streamUrl)
        val temp = File(workspace, "raw-${System.nanoTime()}.${container.extension}")
        val downloadedBytes = AtomicLong(0L)
        val lastProgress = AtomicInteger(12)
        val limiter = Semaphore(minOf(parallelAudioConcurrency(targetLength), settings.maxParallelFragments).coerceAtLeast(1))
        try {
            RandomAccessFile(temp, "rw").use { file -> file.setLength(targetLength) }
            ranges.map { range ->
                async(Dispatchers.IO) {
                    limiter.withPermit {
                        downloadAudioRange(track.streamUrl, range, temp, downloadedBytes, lastProgress, targetLength)
                    }
                }
            }.awaitAll()
            if (temp.length() != targetLength) {
                throw IOException("Download parallelo incompleto: ${temp.length()}/$targetLength byte")
            }
            reportProgress(82)
            DownloadedAudio(temp, container)
        } catch (error: CancellationException) {
            runCatching { temp.delete() }
            throw error
        } catch (error: IOException) {
            runCatching { temp.delete() }
            throw error
        }
    }

    private suspend fun downloadAudioRange(
        url: String,
        range: AudioDownloadRange,
        outputFile: File,
        downloadedBytes: AtomicLong,
        lastProgress: AtomicInteger,
        targetLength: Long
    ) {
        var lastError: IOException? = null
        repeat(RANGE_RETRY_COUNT) { attempt ->
            try {
                downloadAudioRangeAttempt(url, range, outputFile, downloadedBytes, lastProgress, targetLength)
                return
            } catch (error: IOException) {
                lastError = error
                if (attempt < RANGE_RETRY_COUNT - 1) delay(RANGE_RETRY_DELAY_MS * (attempt + 1L))
            }
        }
        throw lastError ?: IOException("Range audio non riuscito")
    }

    private suspend fun downloadAudioRangeAttempt(
        url: String,
        range: AudioDownloadRange,
        outputFile: File,
        downloadedBytes: AtomicLong,
        lastProgress: AtomicInteger,
        targetLength: Long
    ) {
        val rangeUrl = withGoogleVideoRange(url, range)
        val rangeParamApplied = rangeUrl != url
        val request = Request.Builder()
            .url(rangeUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "audio/*,*/*;q=0.8")
            .header("Accept-Encoding", "identity")
            .header("Connection", "keep-alive")
            .apply { if (!rangeParamApplied) header("Range", "bytes=${range.start}-${range.endInclusive}") }
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body
            val contentLength = body.contentLength()
            val contentRange = response.header("Content-Range").orEmpty()
            if (!isUsableAudioRangeResponse(response.code, contentLength, contentRange, range, rangeParamApplied)) {
                throw IOException("Range audio non supportato: HTTP ${response.code}")
            }
            var written = 0L
            body.byteStream().use { input ->
                RandomAccessFile(outputFile, "rw").use { output ->
                    output.seek(range.start)
                    val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                    while (written < range.length) {
                        val maxRead = minOf(buffer.size.toLong(), range.length - written).toInt()
                        val read = input.read(buffer, 0, maxRead)
                        if (read < 0) break
                        rateLimiter.consume(read)
                        output.write(buffer, 0, read)
                        written += read.toLong()
                    }
                }
            }
            if (written != range.length) {
                throw IOException("Range troncato: ${range.start}-${range.endInclusive} ($written/${range.length} byte)")
            }
            val total = downloadedBytes.addAndGet(range.length)
            updateParallelProgress(lastProgress, downloadProgress(total, targetLength))
        }
    }

    private suspend fun updateParallelProgress(lastProgress: AtomicInteger, nextProgress: Int) {
        while (true) {
            val current = lastProgress.get()
            if (nextProgress <= current) return
            if (lastProgress.compareAndSet(current, nextProgress)) {
                reportProgress(nextProgress)
                return
            }
        }
    }

    private fun probeAudio(url: String): AudioProbe {
        val sourceUrl = stripAudioRangeParameters(url)
        val urlLength = audioContentLengthFromUrl(sourceUrl)
        val urlContentType = audioContentTypeFromUrl(sourceUrl)
        if (urlLength > 0L && urlContentType.isNotBlank()) {
            return AudioProbe(contentLength = urlLength, contentType = urlContentType)
        }
        val request = Request.Builder()
            .url(sourceUrl)
            .head()
            .header("User-Agent", USER_AGENT)
            .header("Accept-Encoding", "identity")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AudioProbe(contentLength = urlLength, contentType = urlContentType)
                } else {
                    AudioProbe(
                        contentLength = response.header("Content-Length")?.toLongOrNull()?.takeIf { it > 0L } ?: urlLength,
                        contentType = response.header("Content-Type").orEmpty()
                            .substringBefore(';')
                            .trim()
                            .lowercase(Locale.US)
                            .ifBlank { urlContentType }
                    )
                }
            }
        }.getOrDefault(AudioProbe(contentLength = urlLength, contentType = urlContentType))
    }

    private fun withGoogleVideoRange(url: String, contentLength: Long): String {
        if (contentLength <= 0L) return stripAudioRangeParameters(url)
        return withGoogleVideoRange(url, AudioDownloadRange(0L, contentLength - 1L))
    }

    private fun withGoogleVideoRange(url: String, range: AudioDownloadRange): String {
        val sourceUrl = stripAudioRangeParameters(url)
        if (!isGoogleVideoUrl(sourceUrl)) return sourceUrl
        val separator = if (sourceUrl.contains('?')) '&' else '?'
        return "$sourceUrl${separator}range=${range.start}-${range.endInclusive}"
    }

    private fun isGoogleVideoUrl(url: String): Boolean {
        val host = url.substringAfter("://").substringBefore('/').substringBefore(':').lowercase(Locale.US)
        return host.endsWith("googlevideo.com")
    }

    private fun maybeEmbedMetadata(
        input: File,
        track: Track,
        artwork: ByteArray?,
        container: AudioContainer,
        workspace: File
    ): PreparedAudioFile {
        val fileName = buildFileName(track, container.extension)
        if (!settings.embedMetadata || !container.supportsEmbeddedMetadata || !shouldEmbedFastMetadata(input.length(), track.durationMs)) {
            return PreparedAudioFile(input, fileName, container, fileMetadataEmbedded = false)
        }
        val output = File(workspace, "tagged-${System.nanoTime()}.${container.extension}")
        val tagResult = LevyraM4aTagWriter.write(
            input = input,
            output = output,
            metadata = LevyraM4aMetadata(
                title = track.title,
                artist = track.artist,
                album = track.album.ifBlank { "Levyra" },
                albumArtist = track.artist,
                artworkData = artwork
            )
        )
        return if (tagResult.success && output.exists() && output.length() > 0L) {
            PreparedAudioFile(output, fileName, container, fileMetadataEmbedded = true)
        } else {
            runCatching { output.delete() }
            PreparedAudioFile(input, fileName, container, fileMetadataEmbedded = false)
        }
    }

    private suspend fun persistDownload(original: Track, resolved: Track, fileName: String, uri: Uri, container: AudioContainer, embeddedMetadata: Boolean) {
        runCatching {
            LevyraDatabase.get(context).downloadedTracksDao().insert(
                DownloadEntity(
                    trackId = original.id.ifBlank { resolved.id },
                    title = original.title.ifBlank { resolved.title },
                    artist = original.artist.ifBlank { resolved.artist },
                    album = original.album.ifBlank { resolved.album.ifBlank { "Levyra" } },
                    durationMs = original.durationMs.takeIf { it > 0L } ?: resolved.durationMs.coerceAtLeast(0L),
                    fileName = fileName,
                    uri = uri.toString(),
                    mimeType = container.mimeType,
                    embeddedMetadata = embeddedMetadata,
                    savedAt = System.currentTimeMillis()
                )
            )
        }.onFailure { Timber.w(it, "Downloaded track persistence failed") }
    }

    private fun saveToMusicCollection(input: File, track: Track, container: AudioContainer): SavedAudioDestination {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveScoped(input, track, container) else saveLegacy(input, track, container)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveScoped(input: File, track: Track, container: AudioContainer): SavedAudioDestination {
        return runCatching {
            SavedAudioDestination(
                uri = saveScopedAudio(input, track, container),
                destinationLabel = musicDestinationLabel(track)
            )
        }.getOrElse { audioError ->
            Timber.w(audioError, "Audio MediaStore refused %s, falling back to Downloads collection", container.mimeType)
            runCatching {
                SavedAudioDestination(
                    uri = saveScopedDownloadFile(input, track, container),
                    destinationLabel = downloadsDestinationLabel(track)
                )
            }.getOrElse { downloadError ->
                audioError.addSuppressed(downloadError)
                throw audioError
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveScopedAudio(input: File, track: Track, container: AudioContainer): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, buildFileName(track, container.extension))
            put(MediaStore.MediaColumns.MIME_TYPE, container.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, musicDestinationLabel(track))
            put(MediaStore.MediaColumns.SIZE, input.length())
            put(MediaStore.Audio.Media.TITLE, track.title)
            put(MediaStore.Audio.Media.ARTIST, track.artist)
            put(MediaStore.Audio.Media.ALBUM, track.album.ifBlank { "Levyra" })
            put(MediaStore.Audio.Media.DURATION, track.durationMs.coerceAtLeast(0L))
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore non ha creato il file")
        try {
            copyIntoMediaStore(uri, input)
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveScopedDownloadFile(input: File, track: Track, container: AudioContainer): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, buildFileName(track, container.extension))
            put(MediaStore.MediaColumns.MIME_TYPE, container.fileCollectionMimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, downloadsDestinationLabel(track))
            put(MediaStore.MediaColumns.SIZE, input.length())
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore non ha creato il file")
        try {
            copyIntoMediaStore(uri, input)
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun copyIntoMediaStore(uri: Uri, input: File) {
        val channelCopySucceeded = runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "w") ?: return@runCatching false
            ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { output ->
                FileInputStream(input).use { sourceStream ->
                    val source = sourceStream.channel
                    val target = output.channel
                    var position = 0L
                    val length = source.size()
                    while (position < length) {
                        val transferred = source.transferTo(position, minOf(FILE_CHANNEL_CHUNK_BYTES, length - position), target)
                        if (transferred <= 0L) return@runCatching false
                        position += transferred
                    }
                }
            }
            true
        }.getOrDefault(false)
        if (channelCopySucceeded) return
        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
            input.inputStream().use { source -> source.copyTo(output, COPY_BUFFER_BYTES) }
        } ?: throw IOException("Impossibile scrivere il file esportato")
    }

    private fun saveLegacy(input: File, track: Track, container: AudioContainer): SavedAudioDestination {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), legacyRelativeSubdirectory(track)).apply { mkdirs() }
        if (!dir.exists()) throw IOException("Cartella musicale Levyra non disponibile")
        val target = uniqueFile(dir, buildFileName(track, container.extension))
        input.inputStream().use { source -> FileOutputStream(target).use { source.copyTo(it, COPY_BUFFER_BYTES) } }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DATA, target.absolutePath)
            put(MediaStore.MediaColumns.DISPLAY_NAME, target.name)
            put(MediaStore.MediaColumns.MIME_TYPE, container.mimeType)
            put(MediaStore.MediaColumns.SIZE, target.length())
            put(MediaStore.Audio.Media.TITLE, track.title)
            put(MediaStore.Audio.Media.ARTIST, track.artist)
            put(MediaStore.Audio.Media.ALBUM, track.album.ifBlank { "Levyra" })
            put(MediaStore.Audio.Media.DURATION, track.durationMs.coerceAtLeast(0L))
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
        }
        val uri = runCatching {
            context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: Uri.fromFile(target)
        }.getOrElse {
            Uri.fromFile(target)
        }
        return SavedAudioDestination(uri, musicDestinationLabel(track))
    }

    private fun downloadArtwork(track: Track): ByteArray? {
        val url = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.trim()
        if (url.isBlank() || !url.startsWith("http", ignoreCase = true)) return null
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body
                val length = body.contentLength()
                if (length > MAX_ARTWORK_BYTES) return@use null
                val bytes = body.bytes()
                if (bytes.size > MAX_ARTWORK_BYTES) null else bytes
            }
        }.getOrNull()
    }

    private fun parallelAudioChunkSizeForSettings(contentLength: Long): Long {
        val base = parallelAudioChunkSize(contentLength)
        return when (settings.preset) {
            LevyraDownloadPreset.HighQuality -> base
            LevyraDownloadPreset.Automatic -> base
            LevyraDownloadPreset.DataSaver -> (base * 2L).coerceAtMost(8L * 1024L * 1024L)
        }
    }

    private fun verifyAudioFile(file: File, container: AudioContainer) {
        if (!file.isFile || file.length() < MIN_VALID_AUDIO_BYTES) throw IOException("File audio non valido")
        val header = ByteArray(16)
        val read = file.inputStream().use { it.read(header) }
        if (read < 4) throw IOException("Intestazione audio incompleta")
        val valid = when (container.extension) {
            "m4a" -> header.copyOf(read).toString(Charsets.ISO_8859_1).contains("ftyp")
            "webm" -> header[0] == 0x1A.toByte() && header[1] == 0x45.toByte() && header[2] == 0xDF.toByte() && header[3] == 0xA3.toByte()
            "mp3" -> header.copyOf(read).toString(Charsets.ISO_8859_1).startsWith("ID3") || (header[0].toInt() and 0xFF) == 0xFF
            else -> true
        }
        if (!valid) throw IOException("Contenitore audio danneggiato o non riconosciuto")
    }

    private fun musicDestinationLabel(track: Track): String {
        val suffix = relativeFolderSuffix(track)
        return listOf(Environment.DIRECTORY_MUSIC, "Levyra", suffix).filter { it.isNotBlank() }.joinToString("/")
    }

    private fun downloadsDestinationLabel(track: Track): String {
        val suffix = relativeFolderSuffix(track)
        return listOf(Environment.DIRECTORY_DOWNLOADS, "Levyra", suffix).filter { it.isNotBlank() }.joinToString("/")
    }

    private fun legacyRelativeSubdirectory(track: Track): String {
        val suffix = relativeFolderSuffix(track)
        return listOf("Levyra", suffix).filter { it.isNotBlank() }.joinToString(File.separator)
    }

    private fun relativeFolderSuffix(track: Track): String {
        val artist = sanitize(track.artist).ifBlank { "Unknown Artist" }
        val album = sanitize(track.album).ifBlank { "Singles" }
        return when (settings.folderMode) {
            LevyraDownloadFolderMode.Flat -> ""
            LevyraDownloadFolderMode.Artist -> artist
            LevyraDownloadFolderMode.ArtistAlbum -> "$artist/$album"
        }
    }

    private fun detectContainer(contentType: String, url: String): AudioContainer {
        val cleanUrl = url.substringBefore('?').lowercase(Locale.US)
        return when {
            contentType.contains("mp4") || contentType.contains("m4a") || cleanUrl.endsWith(".m4a") || cleanUrl.endsWith(".mp4") -> AudioContainer("m4a", "audio/mp4", "audio/mp4", true)
            contentType.contains("webm") || cleanUrl.endsWith(".webm") -> AudioContainer("webm", "audio/webm", "application/octet-stream", false)
            contentType.contains("mpeg") || contentType.contains("mp3") || cleanUrl.endsWith(".mp3") -> AudioContainer("mp3", "audio/mpeg", "audio/mpeg", false)
            else -> AudioContainer("m4a", "audio/mp4", "audio/mp4", true)
        }
    }

    private suspend fun reportProgress(value: Int) {
        progress(value.coerceIn(0, 100))
    }

    private fun downloadProgress(downloadedBytes: Long, declaredLength: Long): Int {
        return if (declaredLength > 0L) {
            val ratio = downloadedBytes.toDouble() / declaredLength.toDouble()
            (12 + ratio * 70).toInt().coerceIn(12, 82)
        } else {
            val step = (downloadedBytes / (512L * 1024L)).toInt()
            (12 + step).coerceIn(12, 78)
        }
    }

    private fun buildFileName(track: Track, extension: String): String {
        val artist = sanitize(track.artist).ifBlank { "Unknown Artist" }
        val title = sanitize(track.title).ifBlank { track.id.ifBlank { "Levyra Track" } }
        return "$artist - $title.$extension"
    }

    private fun sanitize(value: String): String {
        return value.trim()
            .replace(Regex("[\\/:*?\"<>|\\p{Cntrl}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .take(120)
            .trim('.', ' ')
    }

    private fun uniqueFile(dir: File, name: String): File {
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var candidate = File(dir, name)
        var index = 2
        while (candidate.exists()) {
            candidate = File(dir, if (ext.isBlank()) "$base ($index)" else "$base ($index).$ext")
            index++
        }
        return candidate
    }

    private fun cleanupWorkspace(workspace: File) {
        val now = System.currentTimeMillis()
        workspace.listFiles()?.forEach { file ->
            val retention = if (file.name.startsWith("resume-") && file.name.endsWith(".part")) {
                TimeUnit.DAYS.toMillis(7)
            } else {
                TimeUnit.HOURS.toMillis(2)
            }
            if (now - file.lastModified() > retention) runCatching { file.delete() }
        }
    }

    companion object {
        private const val MAX_AUDIO_BYTES = 2L * 1024L * 1024L * 1024L
        private const val MIN_FREE_STORAGE_RESERVE_BYTES = 128L * 1024L * 1024L
        private const val UNKNOWN_LENGTH_STORAGE_ALLOWANCE_BYTES = 256L * 1024L * 1024L
        private const val MAX_ARTWORK_BYTES = 4 * 1024 * 1024
        private const val MIN_VALID_AUDIO_BYTES = 4L * 1024L
        private const val DOWNLOAD_BUFFER_BYTES = 512 * 1024
        private const val COPY_BUFFER_BYTES = 1024 * 1024
        private const val FILE_CHANNEL_CHUNK_BYTES = 32L * 1024L * 1024L
        private const val RANGE_RETRY_COUNT = 3
        private const val RANGE_RETRY_DELAY_MS = 140L
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    }
}

data class OfflineExportResult(
    val uri: Uri,
    val fileName: String,
    val fileMetadataEmbedded: Boolean,
    val mimeType: String,
    val destinationLabel: String
)

private data class SavedAudioDestination(
    val uri: Uri,
    val destinationLabel: String
)

private data class DownloadedAudio(
    val file: File,
    val container: AudioContainer
)

private data class PreparedAudioFile(
    val file: File,
    val fileName: String,
    val container: AudioContainer,
    val fileMetadataEmbedded: Boolean
)

private data class AudioProbe(
    val contentLength: Long = -1L,
    val contentType: String = ""
)

private data class AudioContainer(
    val extension: String,
    val mimeType: String,
    val fileCollectionMimeType: String,
    val supportsEmbeddedMetadata: Boolean
)
