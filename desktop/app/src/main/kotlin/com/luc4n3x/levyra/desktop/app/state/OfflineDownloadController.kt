package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import com.luc4n3x.levyra.desktop.core.storage.DownloadData
import com.luc4n3x.levyra.desktop.core.storage.DownloadRecord
import com.luc4n3x.levyra.desktop.core.storage.DownloadStatus
import com.luc4n3x.levyra.desktop.core.storage.DownloadStore
import com.luc4n3x.levyra.desktop.core.storage.SettingsStore
import com.luc4n3x.levyra.desktop.core.stream.ResolvedAudio
import com.luc4n3x.levyra.desktop.core.stream.StreamResolver
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class OfflineDownloadController(
    private val scope: CoroutineScope,
    private val paths: AppPaths,
    private val resolver: StreamResolver,
    private val catalog: CatalogRepository,
    private val settingsStore: SettingsStore,
    private val store: DownloadStore,
    baseClient: OkHttpClient = ExtractorHttp.client
) {
    private val client = baseClient.newBuilder()
        .callTimeout(0L, TimeUnit.MILLISECONDS)
        .readTimeout(0L, TimeUnit.MILLISECONDS)
        .build()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val permits = Semaphore(MAX_CONCURRENT_DOWNLOADS)

    val downloads: StateFlow<DownloadData> = store.downloads

    init {
        store.reconcile(paths.downloadsDirectory)
        store.current.records
            .filter { it.status == DownloadStatus.QUEUED }
            .forEach { start(it.id) }
    }

    fun recordFor(track: Track): DownloadRecord? = store.record(downloadId(track))

    fun completedTrack(track: Track): Track? {
        val record = recordFor(track) ?: return null
        return record.takeIf { it.isPlayable && Files.isRegularFile(Path.of(it.filePath)) }?.playableTrack()
    }

    fun enqueue(track: Track) {
        val id = downloadId(track)
        val existing = store.record(id)
        if (existing?.isPlayable == true && Files.isRegularFile(Path.of(existing.filePath))) return
        val now = System.currentTimeMillis()
        val record = if (existing == null) {
            DownloadRecord(
                id = id,
                track = track.copy(offlinePath = "", offlineMediaLabel = ""),
                status = DownloadStatus.QUEUED,
                createdAt = now,
                updatedAt = now
            )
        } else {
            existing.copy(
                track = track.copy(offlinePath = "", offlineMediaLabel = ""),
                status = DownloadStatus.QUEUED,
                error = "",
                updatedAt = now
            )
        }
        store.upsert(record)
        start(id)
    }

    fun cancel(id: String) {
        store.update(id) { record ->
            record.copy(status = DownloadStatus.CANCELLED, error = "")
        }
        jobs.remove(id)?.cancel()
    }

    fun retry(id: String) {
        val record = store.record(id) ?: return
        store.upsert(record.copy(status = DownloadStatus.QUEUED, error = ""))
        start(id)
    }

    fun delete(id: String) {
        jobs.remove(id)?.cancel()
        val record = store.record(id) ?: return
        runCatching { record.filePath.takeIf { it.isNotBlank() }?.let { Files.deleteIfExists(Path.of(it)) } }
        runCatching { record.temporaryPath.takeIf { it.isNotBlank() }?.let { Files.deleteIfExists(Path.of(it)) } }
        store.remove(id)
    }

    fun shutdown() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
    }

    private fun start(id: String) {
        if (jobs[id]?.isActive == true) return
        jobs[id] = scope.launch {
            permits.withPermit {
                download(id)
            }
        }.also { job ->
            job.invokeOnCompletion { jobs.remove(id, job) }
        }
    }

    private suspend fun download(id: String) {
        val original = store.record(id) ?: return
        try {
            store.update(id) { it.copy(status = DownloadStatus.RESOLVING, error = "") }
            val playable = resolvePlayable(original.track)
            val settings = settingsStore.current
            val resolved = resolver.resolve(playable, settings.audioQuality, settings.preferredCodec)
            val enriched = playable.copy(
                title = resolved.title.ifBlank { playable.title },
                artist = resolved.artist.ifBlank { playable.artist },
                artworkUrl = resolved.artworkUrl.ifBlank { playable.artworkUrl },
                durationMs = if (resolved.durationMs > 0L) resolved.durationMs else playable.durationMs
            )
            withContext(Dispatchers.IO) {
                writeResolvedDownload(id, enriched, resolved)
            }
        } catch (cancellation: CancellationException) {
            if (store.record(id)?.status != DownloadStatus.CANCELLED) {
                store.update(id) { it.copy(status = DownloadStatus.CANCELLED) }
            }
            throw cancellation
        } catch (error: Throwable) {
            store.update(id) { record ->
                record.copy(
                    status = DownloadStatus.FAILED,
                    error = error.message.orEmpty().ifBlank { error::class.simpleName.orEmpty() }
                )
            }
        }
    }

    private suspend fun resolvePlayable(track: Track): Track {
        if (track.videoUrl.isNotBlank()) return track
        return catalog.findPlayable(track)
            ?: throw IllegalStateException("Nessuna versione riproducibile trovata")
    }

    private suspend fun writeResolvedDownload(
        id: String,
        track: Track,
        resolved: ResolvedAudio
    ) {
        val extension = extensionFor(resolved.label)
        val baseName = OfflineFileName.baseName(track)
        val target = paths.downloadsDirectory.resolve("$baseName.$extension")
        val temporary = paths.downloadsDirectory.resolve("$baseName.$extension.part")
        Files.createDirectories(paths.downloadsDirectory)

        val existingRecord = store.record(id) ?: return
        var resumedBytes = OfflinePartialFileMigration.prepare(
            downloadsDirectory = paths.downloadsDirectory,
            recordedPath = existingRecord.temporaryPath,
            targetPath = temporary
        )
        val requestBuilder = Request.Builder()
            .url(resolved.url)
            .header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
            .header("Accept", "*/*")
        if (resumedBytes > 0L) {
            requestBuilder.header("Range", "bytes=$resumedBytes-")
        }

        store.update(id) { record ->
            record.copy(
                track = track,
                status = DownloadStatus.DOWNLOADING,
                temporaryPath = temporary.toString(),
                filePath = "",
                bytesDownloaded = resumedBytes,
                mediaLabel = resolved.label,
                error = ""
            )
        }

        val call = client.newCall(requestBuilder.build())
        kotlinx.coroutines.currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        call.execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download non disponibile: HTTP ${response.code}")
            }
            val body = response.body
            val append = resumedBytes > 0L && response.code == 206
            if (!append) {
                resumedBytes = 0L
                Files.deleteIfExists(temporary)
            }
            val contentLength = body.contentLength().coerceAtLeast(0L)
            val totalBytes = if (contentLength > 0L) resumedBytes + contentLength else 0L
            val options = if (append) {
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
            } else {
                arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
            }
            BufferedInputStream(body.byteStream(), BUFFER_SIZE).use { input ->
                BufferedOutputStream(Files.newOutputStream(temporary, *options), BUFFER_SIZE).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = resumedBytes
                    var lastPersistedBytes = downloaded
                    var lastPersistedAt = System.nanoTime()
                    while (true) {
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val now = System.nanoTime()
                        if (
                            downloaded - lastPersistedBytes >= PROGRESS_BYTES_INTERVAL ||
                            now - lastPersistedAt >= PROGRESS_TIME_INTERVAL_NANOS
                        ) {
                            store.update(id) { record ->
                                record.copy(
                                    status = DownloadStatus.DOWNLOADING,
                                    bytesDownloaded = downloaded,
                                    totalBytes = totalBytes
                                )
                            }
                            lastPersistedBytes = downloaded
                            lastPersistedAt = now
                        }
                    }
                    output.flush()
                    store.update(id) { record ->
                        record.copy(
                            status = DownloadStatus.DOWNLOADING,
                            bytesDownloaded = downloaded,
                            totalBytes = if (totalBytes > 0L) totalBytes else downloaded
                        )
                    }
                }
            }
        }

        moveAtomically(temporary, target)
        val completedSize = Files.size(target)
        store.update(id) { record ->
            record.copy(
                track = track,
                status = DownloadStatus.COMPLETED,
                filePath = target.toString(),
                temporaryPath = "",
                bytesDownloaded = completedSize,
                totalBytes = completedSize,
                mediaLabel = resolved.label,
                error = ""
            )
        }
    }

    private fun moveAtomically(source: Path, target: Path) {
        runCatching {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.getOrElse {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun extensionFor(label: String): String {
        val normalized = label.lowercase(Locale.ROOT)
        return when {
            "opus" in normalized || "webm" in normalized -> "webm"
            "aac" in normalized || "m4a" in normalized || "mp4" in normalized -> "m4a"
            else -> "m4a"
        }
    }

    private fun downloadId(track: Track): String = track.id.ifBlank {
        "${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}"
    }

    companion object {
        private const val MAX_CONCURRENT_DOWNLOADS = 2
        private const val BUFFER_SIZE = 64 * 1024
        private const val PROGRESS_BYTES_INTERVAL = 512 * 1024L
        private const val PROGRESS_TIME_INTERVAL_NANOS = 250_000_000L
    }
}

internal object OfflinePartialFileMigration {
    fun prepare(downloadsDirectory: Path, recordedPath: String, targetPath: Path): Long {
        val root = downloadsDirectory.toAbsolutePath().normalize()
        val target = targetPath.toAbsolutePath().normalize()
        require(target.startsWith(root)) { "Il file temporaneo deve restare nella cartella download" }
        Files.createDirectories(target.parent)

        val source = recordedPath
            .takeIf { it.isNotBlank() }
            ?.let { raw ->
                runCatching {
                    val parsed = Path.of(raw)
                    (if (parsed.isAbsolute) parsed else root.resolve(parsed)).toAbsolutePath().normalize()
                }.getOrNull()
            }

        if (
            source != null &&
            source != target &&
            source.startsWith(root) &&
            !Files.isSymbolicLink(source) &&
            Files.isRegularFile(source)
        ) {
            if (partialExtension(source) == partialExtension(target) && partialExtension(target).isNotBlank()) {
                migrateCompatiblePartial(source, target)
            } else {
                Files.deleteIfExists(source)
            }
        }

        return runCatching { Files.size(target) }.getOrDefault(0L)
    }

    private fun migrateCompatiblePartial(source: Path, target: Path) {
        if (Files.isRegularFile(target) && !Files.isSymbolicLink(target)) {
            if (Files.size(source) > Files.size(target)) {
                moveAtomically(source, target)
            } else {
                Files.deleteIfExists(source)
            }
        } else {
            Files.deleteIfExists(target)
            moveAtomically(source, target)
        }
    }

    private fun partialExtension(path: Path): String {
        val name = path.fileName.toString()
        if (!name.endsWith(".part", ignoreCase = true)) return ""
        return name.dropLast(PART_SUFFIX_LENGTH)
            .substringAfterLast('.', "")
            .lowercase(Locale.ROOT)
    }

    private fun moveAtomically(source: Path, target: Path) {
        runCatching {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.getOrElse {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private const val PART_SUFFIX_LENGTH = 5
}

internal object OfflineFileName {
    private const val MAX_FILE_NAME_LENGTH = 180
    private const val HASH_BYTES = 8
    private val invalidFileNameChars = Regex("""[\\/:*?"<>|\p{Cntrl}]""")

    fun baseName(track: Track): String {
        val artist = safeComponent(track.artist.ifBlank { "Levyra" })
        val title = safeComponent(track.title.ifBlank { track.id })
        val suffixBlock = " [${stableSuffix(track)}]"
        val availableDescriptionLength =
            (MAX_FILE_NAME_LENGTH - suffixBlock.length).coerceAtLeast(1)
        val description = "$artist - $title"
            .take(availableDescriptionLength)
            .trimEnd(' ', '.', '-')
            .ifBlank { "track" }
        return description + suffixBlock
    }

    private fun stableSuffix(track: Track): String {
        val identity = track.id.ifBlank {
            buildString {
                append(track.title.trim().lowercase(Locale.ROOT))
                append('|')
                append(track.artist.trim().lowercase(Locale.ROOT))
                append('|')
                append(track.videoUrl.trim())
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
        return digest
            .take(HASH_BYTES)
            .joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
    }

    private fun safeComponent(value: String): String = value
        .replace(invalidFileNameChars, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trimEnd('.')
        .ifBlank { "track" }
}
