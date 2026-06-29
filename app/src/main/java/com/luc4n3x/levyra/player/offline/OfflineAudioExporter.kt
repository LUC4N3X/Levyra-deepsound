package com.luc4n3x.levyra.player.offline

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aMetadata
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aTagWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import timber.log.Timber

class OfflineAudioExporter(
    private val context: Context,
    private val resolver: PlaybackResolver,
    private val client: OkHttpClient = LevyraHttpClientFactory.general(context.applicationContext)
) {
    val embeddedMetadataWriterReady: Boolean
        get() = LevyraM4aTagWriter.isAvailable

    suspend fun export(track: Track): OfflineExportResult = withContext(Dispatchers.IO) {
        val playable = if (track.streamUrl.isBlank()) resolver.resolve(track.copy(streamUrl = "")) else track
        if (playable.streamUrl.isBlank()) throw IOException("Stream audio non disponibile")
        val workspace = File(context.cacheDir, "levyra_offline_export").apply { mkdirs() }
        Timber.i("Offline export started: %s", track.title)
        cleanupWorkspace(workspace)
        val downloaded = downloadAudio(playable, workspace)
        var embeddedFile: PreparedAudioFile? = null
        try {
            val artwork = downloadArtwork(playable)
            embeddedFile = maybeEmbedMetadata(downloaded.file, playable, artwork, downloaded.container, workspace)
            val exported = saveToMusicCollection(embeddedFile.file, playable, embeddedFile.container)
            val fileName = buildFileName(playable, embeddedFile.container.extension)
            persistDownload(playable, fileName, exported, embeddedFile.container, embeddedFile.fileMetadataEmbedded)
            Timber.i("Offline export completed: %s", fileName)
            OfflineExportResult(
                uri = exported,
                fileName = fileName,
                fileMetadataEmbedded = embeddedFile.fileMetadataEmbedded,
                mimeType = embeddedFile.container.mimeType
            )
        } finally {
            runCatching { downloaded.file.delete() }
            embeddedFile?.file?.takeIf { it != downloaded.file }?.let { runCatching { it.delete() } }
        }
    }

    private fun downloadAudio(track: Track, workspace: File): DownloadedAudio {
        val request = Request.Builder()
            .url(track.streamUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "audio/*,*/*;q=0.8")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download audio fallito: HTTP ${response.code}")
            val body = response.body ?: throw IOException("Risposta audio vuota")
            val declaredLength = body.contentLength()
            if (declaredLength > MAX_AUDIO_BYTES) throw IOException("File troppo grande per l'esportazione")
            val contentType = response.header("Content-Type").orEmpty().substringBefore(';').trim().lowercase(Locale.US)
            val container = detectContainer(contentType, track.streamUrl)
            val temp = File(workspace, "raw-${System.nanoTime()}.${container.extension}")
            body.byteStream().use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read.toLong()
                        if (total > MAX_AUDIO_BYTES) throw IOException("File troppo grande per l'esportazione")
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            if (temp.length() <= 0L) throw IOException("File audio esportato vuoto")
            return DownloadedAudio(temp, container)
        }
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

    private suspend fun persistDownload(track: Track, fileName: String, uri: Uri, container: AudioContainer, embeddedMetadata: Boolean) {
        runCatching {
            LevyraDatabase.get(context).downloadedTracksDao().insert(
                DownloadEntity(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    album = track.album.ifBlank { "Levyra" },
                    durationMs = track.durationMs.coerceAtLeast(0L),
                    fileName = fileName,
                    uri = uri.toString(),
                    mimeType = container.mimeType,
                    embeddedMetadata = embeddedMetadata,
                    savedAt = System.currentTimeMillis()
                )
            )
        }.onFailure { Timber.w(it, "Downloaded track persistence failed") }
    }

    private fun saveToMusicCollection(input: File, track: Track, container: AudioContainer): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveScoped(input, track, container) else saveLegacy(input, track, container)
    }

    private fun saveScoped(input: File, track: Track, container: AudioContainer): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, buildFileName(track, container.extension))
            put(MediaStore.MediaColumns.MIME_TYPE, container.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Levyra")
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
            resolver.openOutputStream(uri, "w")?.use { output -> input.inputStream().use { it.copyTo(output) } }
                ?: throw IOException("Impossibile scrivere il file esportato")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun saveLegacy(input: File, track: Track, container: AudioContainer): Uri {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Levyra").apply { mkdirs() }
        if (!dir.exists()) throw IOException("Cartella Music/Levyra non disponibile")
        val target = uniqueFile(dir, buildFileName(track, container.extension))
        input.inputStream().use { source -> FileOutputStream(target).use { source.copyTo(it) } }
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
        return context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: Uri.fromFile(target)
    }

    private fun downloadArtwork(track: Track): ByteArray? {
        val url = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }.trim()
        if (url.isBlank() || !url.startsWith("http", ignoreCase = true)) return null
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
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
            contentType.contains("mp4") || contentType.contains("m4a") || cleanUrl.endsWith(".m4a") || cleanUrl.endsWith(".mp4") -> AudioContainer("m4a", "audio/mp4", true)
            contentType.contains("webm") || cleanUrl.endsWith(".webm") -> AudioContainer("webm", "audio/webm", false)
            contentType.contains("mpeg") || contentType.contains("mp3") || cleanUrl.endsWith(".mp3") -> AudioContainer("mp3", "audio/mpeg", false)
            else -> AudioContainer("m4a", "audio/mp4", true)
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
        private const val MAX_AUDIO_BYTES = 96L * 1024L * 1024L
        private const val MAX_ARTWORK_BYTES = 4 * 1024 * 1024
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    }
}

data class OfflineExportResult(
    val uri: Uri,
    val fileName: String,
    val fileMetadataEmbedded: Boolean,
    val mimeType: String
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

private data class AudioContainer(
    val extension: String,
    val mimeType: String,
    val supportsEmbeddedMetadata: Boolean
)
