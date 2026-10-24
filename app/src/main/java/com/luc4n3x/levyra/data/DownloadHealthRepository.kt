package com.luc4n3x.levyra.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.DownloadHealth
import com.luc4n3x.levyra.domain.DownloadOwnership
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.canRepairDownload
import com.luc4n3x.levyra.domain.downloadAudioHealth
import com.luc4n3x.levyra.domain.downloadRepairTaskKey
import com.luc4n3x.levyra.player.offline.work.OfflineExportWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException

data class DownloadHealthResult(val download: DownloadEntity, val health: DownloadHealth, val bytes: Long = 0L) {
    val repairable: Boolean get() = canRepairDownload(download.trackId, health)
}

class DownloadHealthRepository(
    context: Context,
    private val database: LevyraDatabase = LevyraDatabase.get(context.applicationContext)
) {
    private val context = context.applicationContext
    val tasks = database.offlineDownloadTasksDao().observeActive()

    suspend fun scan(onProgress: (Int, Int) -> Unit): List<DownloadHealthResult> = withContext(Dispatchers.IO) {
        val downloads = database.downloadedTracksDao().all()
        onProgress(0, downloads.size)
        downloads.mapIndexed { index, download ->
            currentCoroutineContext().ensureActive()
            inspect(download).also { onProgress(index + 1, downloads.size) }
        }
    }

    private fun inspect(download: DownloadEntity): DownloadHealthResult {
        val uri = Uri.parse(download.uri)
        if (uri.scheme !in setOf("content", "file")) return DownloadHealthResult(download, DownloadHealth.Unavailable)
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                val bytes = descriptor.length.takeIf { it >= 0L } ?: descriptor.parcelFileDescriptor.statSize
                if (bytes == 0L) return DownloadHealthResult(download, DownloadHealth.Empty)
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(context, uri, null)
                    var hasAudio = false
                    var duration = 0L
                    for (index in 0 until extractor.trackCount) {
                        val format = extractor.getTrackFormat(index)
                        if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                            hasAudio = true
                            if (format.containsKey(MediaFormat.KEY_DURATION)) duration = maxOf(duration, format.getLong(MediaFormat.KEY_DURATION) / 1_000L)
                        }
                    }
                    DownloadHealthResult(download, downloadAudioHealth(bytes, hasAudio, duration, download.durationMs), bytes.coerceAtLeast(0L))
                } finally {
                    extractor.release()
                }
            } ?: DownloadHealthResult(download, DownloadHealth.Unavailable)
        } catch (_: SecurityException) {
            DownloadHealthResult(download, DownloadHealth.AccessDenied)
        } catch (_: FileNotFoundException) {
            DownloadHealthResult(download, DownloadHealth.Missing)
        } catch (_: IOException) {
            DownloadHealthResult(download, DownloadHealth.Unreadable)
        } catch (_: IllegalArgumentException) {
            DownloadHealthResult(download, DownloadHealth.Unreadable)
        } catch (_: IllegalStateException) {
            DownloadHealthResult(download, DownloadHealth.Unreadable)
        }
    }

    suspend fun repair(result: DownloadHealthResult): Boolean = withContext(Dispatchers.IO) {
        val current = database.downloadedTracksDao().byId(result.download.id) ?: return@withContext false
        val verified = inspect(current)
        if (!verified.repairable) return@withContext false
        val tasks = database.offlineDownloadTasksDao()
        val taskKey = downloadRepairTaskKey(current.id)
        if (tasks.active().any { it.taskKey == taskKey }) return@withContext false
        val previous = tasks.byKey(current.trackId)
        val track = previous?.payload?.let(TrackPayloadCodec::decode) ?: Track(
            id = current.trackId, title = current.title, artist = current.artist, album = current.album,
            durationMs = current.durationMs, streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "",
            source = "", moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0,
            accentStart = 0, accentEnd = 0
        )
        OfflineExportWorker.enqueue(
            context, taskKey, TrackPayloadCodec.encode(track),
            ownership = DownloadOwnership.fromStorage(current.ownership), repairDownloadId = current.id
        )
        true
    }

    suspend fun cancel(taskKey: String) = withContext(Dispatchers.IO) { OfflineExportWorker.cancel(context, taskKey) }

    suspend fun retry(taskKey: String) = withContext(Dispatchers.IO) { OfflineExportWorker.resume(context, taskKey) }
}
