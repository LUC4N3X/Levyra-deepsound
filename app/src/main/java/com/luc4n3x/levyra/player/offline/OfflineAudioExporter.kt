package com.luc4n3x.levyra.player.offline

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import timber.log.Timber

internal const val DEFAULT_PARALLEL_RANGE_CHUNK_BYTES = 4L * 1024L * 1024L
internal const val MIN_PARALLEL_AUDIO_BYTES = 16L * 1024L * 1024L

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

internal fun isUsableAudioRangeResponse(
    code: Int,
    bodyLength: Long,
    contentRange: String,
    range: AudioDownloadRange,
    rangeParamApplied: Boolean
): Boolean {
    if (code == 206) return true
    if (!rangeParamApplied || code !in 200..299) return false
    if (bodyLength != range.length) return false
    if (contentRange.isBlank()) return true
    return contentRange.contains("${range.start}-") && contentRange.substringAfterLast('/').toLongOrNull() != null
}

internal fun audioContentLengthFromUrl(url: String): Long {
    return Regex("(?:[?&])clen=(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: -1L
}

class OfflineAudioExporter(
    private val context: Context,
    private val resolver: PlaybackResolver,
    private val client: OkHttpClient = LevyraHttpClientFactory.general(context.applicationContext),
    private val progress: suspend (Int) -> Unit = {}
) {
    val embeddedMetadataWriterReady: Boolean
        get() = LevyraM4aTagWriter.isAvailable

    suspend fun export(track: Track): OfflineExportResult = withContext(Dispatchers.IO) {
        reportProgress(1)
        var playable = runCatching {
            reportProgress(4)
            if (track.id.isNotBlank() || track.videoUrl.isNotBlank()) resolver.resolveForOffline(track.copy(streamUrl = "")) else track
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            if (track.streamUrl.isNotBlank()) track else throw error
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
            playable = resolver.resolveForOffline(track.copy(streamUrl = ""))
            reportProgress(10)
            downloadAudio(playable, workspace)
        }
        var embeddedFile: PreparedAudioFile? = null
        try {
            reportProgress(84)
            val artwork = downloadArtwork(playable)
            reportProgress(88)
            embeddedFile = maybeEmbedMetadata(downloaded.file, playable, artwork, downloaded.container, workspace)
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
        val probe = probeAudio(track.streamUrl)
        val expectedLength = probe.contentLength
        val parallelRanges = if (!useRange && !hasRangeParameter(track.streamUrl)) {
            planParallelAudioRanges(expectedLength)
        } else {
            emptyList()
        }
        if (parallelRanges.isNotEmpty()) {
            runCatching {
                return downloadAudioRanges(
                    track = track,
                    workspace = workspace,
                    targetLength = expectedLength,
                    contentType = probe.contentType,
                    ranges = parallelRanges
                )
            }.onFailure { error ->
                Timber.w(error, "Parallel offline download failed, falling back to serial")
            }
        }
        val downloadUrl = if (useRange) withGoogleVideoRange(track.streamUrl, expectedLength) else track.streamUrl
        val rangeParamApplied = downloadUrl != track.streamUrl
        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "audio/*,*/*;q=0.8")
            .header("Accept-Encoding", "identity")
            .header("Connection", "keep-alive")
            .apply { if (useRange && !rangeParamApplied) header("Range", "bytes=0-") }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download audio fallito: HTTP ${response.code}")
            val body = response.body
            val declaredLength = body.contentLength()
            val contentRangeTotal = response.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull() ?: -1L
            val targetLength = when {
                declaredLength > 0L -> declaredLength
                contentRangeTotal > 0L -> contentRangeTotal
                expectedLength > 0L -> expectedLength
                else -> -1L
            }
            if (targetLength > MAX_AUDIO_BYTES) throw IOException("File troppo grande per l'esportazione")
            val contentType = response.header("Content-Type").orEmpty().substringBefore(';').trim().lowercase(Locale.US)
            val container = detectContainer(contentType, track.streamUrl)
            val temp = File(workspace, "raw-${System.nanoTime()}.${container.extension}")
            try {
                body.byteStream().use { input ->
                    FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                        var total = 0L
                        var lastProgress = 8
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
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
                        if (targetLength > 0L && total < targetLength) {
                            throw IOException("Download troncato: $total/$targetLength byte")
                        }
                    }
                }
                if (temp.length() <= 0L) throw IOException("File audio esportato vuoto")
                reportProgress(82)
                return DownloadedAudio(temp, container)
            } catch (error: IOException) {
                runCatching { temp.delete() }
                throw error
            }
        }
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
        val limiter = Semaphore(PARALLEL_RANGE_CONCURRENCY)
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
            body.byteStream().use { input ->
                RandomAccessFile(outputFile, "rw").use { output ->
                    output.seek(range.start)
                    val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                    var written = 0L
                    while (written < range.length) {
                        val maxRead = minOf(buffer.size.toLong(), range.length - written).toInt()
                        val read = input.read(buffer, 0, maxRead)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read.toLong()
                        val total = downloadedBytes.addAndGet(read.toLong())
                        val nextProgress = downloadProgress(total, targetLength)
                        updateParallelProgress(lastProgress, nextProgress)
                    }
                    if (written != range.length) {
                        throw IOException("Range troncato: ${range.start}-${range.endInclusive} ($written/${range.length} byte)")
                    }
                }
            }
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
        val urlLength = audioContentLengthFromUrl(url)
        val request = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", USER_AGENT)
            .header("Accept-Encoding", "identity")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    AudioProbe()
                } else {
                    AudioProbe(
                        contentLength = response.header("Content-Length")?.toLongOrNull()?.takeIf { it > 0L } ?: urlLength,
                        contentType = response.header("Content-Type").orEmpty().substringBefore(';').trim().lowercase(Locale.US)
                    )
                }
            }
        }.getOrDefault(AudioProbe(contentLength = urlLength))
    }

    private fun withGoogleVideoRange(url: String, contentLength: Long): String {
        if (contentLength <= 0L) return url
        if (!isGoogleVideoUrl(url)) return url
        if (hasRangeParameter(url)) return url
        val separator = if (url.contains('?')) '&' else '?'
        return "$url${separator}range=0-${contentLength - 1}"
    }

    private fun withGoogleVideoRange(url: String, range: AudioDownloadRange): String {
        if (!isGoogleVideoUrl(url)) return url
        if (hasRangeParameter(url)) return url
        val separator = if (url.contains('?')) '&' else '?'
        return "$url${separator}range=${range.start}-${range.endInclusive}"
    }

    private fun isGoogleVideoUrl(url: String): Boolean {
        val host = url.substringAfter("://").substringBefore('/').substringBefore(':').lowercase(Locale.US)
        return host.endsWith("googlevideo.com")
    }

    private fun hasRangeParameter(url: String): Boolean {
        return url.contains("&range=", ignoreCase = true) || url.contains("?range=", ignoreCase = true)
    }

    private fun maybeEmbedMetadata(
        input: File,
        track: Track,
        artwork: ByteArray?,
        container: AudioContainer,
        workspace: File
    ): PreparedAudioFile {
        val fileName = buildFileName(track, container.extension)
        if (!container.supportsEmbeddedMetadata) {
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
                destinationLabel = MUSIC_DESTINATION_LABEL
            )
        }.getOrElse { audioError ->
            Timber.w(audioError, "Audio MediaStore refused %s, falling back to Downloads collection", container.mimeType)
            runCatching {
                SavedAudioDestination(
                    uri = saveScopedDownloadFile(input, track, container),
                    destinationLabel = DOWNLOADS_DESTINATION_LABEL
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
            put(MediaStore.MediaColumns.RELATIVE_PATH, MUSIC_DESTINATION_LABEL)
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
            put(MediaStore.MediaColumns.RELATIVE_PATH, DOWNLOADS_DESTINATION_LABEL)
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
        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
            input.inputStream().use { source -> source.copyTo(output) }
        } ?: throw IOException("Impossibile scrivere il file esportato")
    }

    private fun saveLegacy(input: File, track: Track, container: AudioContainer): SavedAudioDestination {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Levyra").apply { mkdirs() }
        if (!dir.exists()) throw IOException("Cartella Music/Levyra non disponibile")
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
        return SavedAudioDestination(uri, MUSIC_DESTINATION_LABEL)
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
            if (now - file.lastModified() > TimeUnit.HOURS.toMillis(2)) runCatching { file.delete() }
        }
    }

    companion object {
        private const val MAX_AUDIO_BYTES = Long.MAX_VALUE
        private const val MAX_ARTWORK_BYTES = 4 * 1024 * 1024
        private const val DOWNLOAD_BUFFER_BYTES = 256 * 1024
        private const val COPY_BUFFER_BYTES = 512 * 1024
        private const val PARALLEL_RANGE_CONCURRENCY = 4
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        private val MUSIC_DESTINATION_LABEL = "${Environment.DIRECTORY_MUSIC}/Levyra"
        private val DOWNLOADS_DESTINATION_LABEL = "${Environment.DIRECTORY_DOWNLOADS}/Levyra"
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
