package com.luc4n3x.levyra.player.offline.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.OfflineDownloadTaskEntity
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.offline.OfflineAudioExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit


private object OfflineDownloadConcurrencyGate {
    private val mutex = Mutex()
    private var active = 0

    suspend fun <T> withLimit(limit: Int, block: suspend () -> T): T {
        val normalized = limit.coerceIn(1, 4)
        while (true) {
            val acquired = mutex.withLock {
                if (active < normalized) {
                    active += 1
                    true
                } else {
                    false
                }
            }
            if (acquired) break
            delay(120L)
        }
        return try {
            block()
        } finally {
            mutex.withLock { active = (active - 1).coerceAtLeast(0) }
        }
    }
}

class OfflineExportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val payload = inputData.getString(KEY_TRACK_PAYLOAD).orEmpty()
        val taskKey = inputData.getString(KEY_TASK_KEY).orEmpty().ifBlank { id.toString() }
        val track = TrackPayloadCodec.decode(payload) ?: return Result.failure(errorData("Traccia non valida"))
        val taskDao = LevyraDatabase.get(applicationContext).offlineDownloadTasksDao()
        val preferences = LevyraPreferences(applicationContext)
        val settings = preferences.downloadSettings()
        val downloadQualityKey = settings.storedQualityKey(preferences.audioQuality())
        val workId = id.toString()
        if (settings.skipExisting && track.id.isNotBlank()) {
            val existing = LevyraDatabase.get(applicationContext).downloadedTracksDao().byTrackIdAndProfile(
                trackId = track.id,
                downloadPreset = settings.storedPresetKey,
                downloadQuality = downloadQualityKey
            )
            if (existing != null && isStoredDownloadReadable(existing.uri)) {
                if (taskDao.updateStateForWork(taskKey, workId, "SUCCEEDED", 100, "", System.currentTimeMillis()) == 0) {
                    return Result.failure(errorData(ERROR_SUPERSEDED))
                }
                return Result.success(
                    workDataOf(
                        KEY_FILE_NAME to existing.fileName,
                        KEY_EMBEDDED_METADATA to existing.embeddedMetadata,
                        KEY_MIME_TYPE to existing.mimeType,
                        KEY_URI to existing.uri,
                        KEY_DESTINATION_LABEL to "Già presente nella libreria"
                    )
                )
            }
        }
        if (taskDao.updateStateForWork(taskKey, workId, "RUNNING", 1, "", System.currentTimeMillis()) == 0) {
            return Result.failure(errorData(ERROR_SUPERSEDED))
        }
        return try {
            setProgress(workDataOf(KEY_PROGRESS to 1))
            setForeground(createForegroundInfo(track, 1))
            var foregroundProgress = 1
            val exporter = OfflineAudioExporter(
                context = applicationContext,
                resolver = PlaybackResolver.getInstance(applicationContext),
                progress = { value ->
                    val safeProgress = value.coerceIn(0, 100)
                    setProgress(workDataOf(KEY_PROGRESS to safeProgress))
                    taskDao.updateStateForWork(taskKey, workId, "RUNNING", safeProgress, "", System.currentTimeMillis())
                    if (safeProgress == 100 || safeProgress >= foregroundProgress + FOREGROUND_PROGRESS_STEP) {
                        foregroundProgress = safeProgress
                        setForeground(createForegroundInfo(track, safeProgress))
                    }
                },
                taskKey = taskKey,
                settings = settings,
                downloadQualityKey = downloadQualityKey
            )
            val result = OfflineDownloadConcurrencyGate.withLimit(settings.maxConcurrentDownloads) {
                exporter.export(track)
            }
            if (taskDao.updateStateForWork(taskKey, workId, "SUCCEEDED", 100, "", System.currentTimeMillis()) == 0) {
                return Result.failure(errorData(ERROR_SUPERSEDED))
            }
            Result.success(
                workDataOf(
                    KEY_FILE_NAME to result.fileName,
                    KEY_EMBEDDED_METADATA to result.fileMetadataEmbedded,
                    KEY_MIME_TYPE to result.mimeType,
                    KEY_URI to result.uri.toString(),
                    KEY_DESTINATION_LABEL to result.destinationLabel
                )
            )
        } catch (error: CancellationException) {
            val current = taskDao.byKey(taskKey)
            if (current?.workId == workId && current.state != "CANCELLED") {
                taskDao.updateStateForWork(taskKey, workId, "PAUSED", current.progress, "", System.currentTimeMillis())
            }
            throw error
        } catch (error: Throwable) {
            val current = taskDao.byKey(taskKey)
            if (current == null || current.workId != workId) {
                Result.failure(errorData(ERROR_SUPERSEDED))
            } else if (error is IOException && runAttemptCount < 2) {
                taskDao.updateStateForWork(taskKey, workId, "RETRYING", current.progress, error.message.orEmpty(), System.currentTimeMillis())
                Timber.w(error, "Offline export retry scheduled")
                Result.retry()
            } else {
                taskDao.updateStateForWork(taskKey, workId, "FAILED", current.progress, error.message.orEmpty(), System.currentTimeMillis())
                Timber.e(error, "Offline export failed")
                Result.failure(errorData(error.message ?: "Esportazione non riuscita"))
            }
        }
    }

    private fun errorData(message: String): Data = workDataOf(KEY_ERROR to message)

    private fun isStoredDownloadReadable(rawUri: String): Boolean {
        if (rawUri.isBlank()) return false
        return runCatching {
            applicationContext.contentResolver.openFileDescriptor(android.net.Uri.parse(rawUri), "r")?.use { descriptor ->
                descriptor.statSize != 0L
            } ?: false
        }.getOrDefault(false)
    }

    private fun createForegroundInfo(track: Track, progress: Int): ForegroundInfo {
        ensureNotificationChannel()
        val notification = buildForegroundNotification(track, progress.coerceIn(0, 100))
        val notificationId = NOTIFICATION_ID_BASE + (track.id.hashCode() and Int.MAX_VALUE) % NOTIFICATION_ID_RANGE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun buildForegroundNotification(track: Track, progress: Int): Notification {
        val title = track.title.ifBlank { "Download offline" }
        val artist = track.artist.ifBlank { "LEVYRA" }
        val percent = progress.coerceIn(0, 100)
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_download)
            .setContentTitle("Download offline")
            .setContentText("$percent% - $title")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$artist"))
            .setContentIntent(openAppIntent())
            .setOngoing(percent < 100)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, percent, false)
            .build()
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(applicationContext, 0, intent, flags)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Download offline",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Stato dei download offline Levyra"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEY_TRACK_PAYLOAD = "track_payload"
        const val KEY_TASK_KEY = "task_key"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_EMBEDDED_METADATA = "embedded_metadata"
        const val KEY_MIME_TYPE = "mime_type"
        const val KEY_URI = "uri"
        const val KEY_DESTINATION_LABEL = "destination_label"
        const val KEY_ERROR = "error"
        const val KEY_PROGRESS = "progress"
        const val ERROR_SUPERSEDED = "task_superseded"
        private const val CHANNEL_ID = "levyra_offline_downloads"
        private const val FOREGROUND_PROGRESS_STEP = 2
        private const val NOTIFICATION_ID_BASE = 4200
        private const val NOTIFICATION_ID_RANGE = 5000
        private const val COMPLETED_TASK_RETENTION_MS = 7L * 24L * 60L * 60L * 1000L

        suspend fun enqueue(context: Context, trackId: String, trackPayload: String): UUID {
            val appContext = context.applicationContext
            val workManager = WorkManager.getInstance(appContext)
            val uniqueName = uniqueNameFor(trackId)
            val settings = LevyraPreferences(appContext).downloadSettings()
            val request = OneTimeWorkRequestBuilder<OfflineExportWorker>()
                .setInputData(workDataOf(KEY_TRACK_PAYLOAD to trackPayload, KEY_TASK_KEY to trackId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                        .setRequiresCharging(settings.chargingOnly)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag("levyra_offline_export")
                .addTag(uniqueName)
                .build()
            val dao = LevyraDatabase.get(appContext).offlineDownloadTasksDao()
            val track = TrackPayloadCodec.decode(trackPayload)
            val previous = dao.byKey(trackId)
            val now = System.currentTimeMillis()
            dao.prune(now - COMPLETED_TASK_RETENTION_MS)
            dao.upsert(
                OfflineDownloadTaskEntity(
                    taskKey = trackId,
                    trackId = track?.id.orEmpty(),
                    payload = trackPayload,
                    title = track?.title.orEmpty(),
                    artist = track?.artist.orEmpty(),
                    state = "QUEUED",
                    progress = previous?.progress?.coerceIn(0, 99) ?: 0,
                    workId = request.id.toString(),
                    error = "",
                    createdAt = previous?.createdAt ?: now,
                    updatedAt = now
                )
            )
            workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
            return request.id
        }

        suspend fun pause(context: Context, taskKey: String) {
            val appContext = context.applicationContext
            val dao = LevyraDatabase.get(appContext).offlineDownloadTasksDao()
            val current = dao.byKey(taskKey) ?: return
            dao.updateState(taskKey, "PAUSED", current.progress.coerceIn(0, 99), "", System.currentTimeMillis())
            WorkManager.getInstance(appContext).cancelUniqueWork(uniqueNameFor(taskKey))
        }

        suspend fun resume(context: Context, taskKey: String): UUID? {
            val appContext = context.applicationContext
            val task = LevyraDatabase.get(appContext).offlineDownloadTasksDao().byKey(taskKey) ?: return null
            if (task.state !in setOf("PAUSED", "FAILED", "RETRYING")) return null
            return enqueue(appContext, taskKey, task.payload)
        }

        suspend fun cancel(context: Context, taskKey: String) {
            val appContext = context.applicationContext
            val dao = LevyraDatabase.get(appContext).offlineDownloadTasksDao()
            val current = dao.byKey(taskKey) ?: return
            dao.updateState(taskKey, "CANCELLED", current.progress.coerceIn(0, 100), "", System.currentTimeMillis())
            WorkManager.getInstance(appContext).cancelUniqueWork(uniqueNameFor(taskKey))
        }

        private fun uniqueNameFor(trackId: String): String {
            val safe = trackId.trim().ifBlank { "unknown" }.replace(Regex("[^A-Za-z0-9_.-]+"), "_").take(120)
            return "levyra_offline_export_$safe"
        }
    }
}
