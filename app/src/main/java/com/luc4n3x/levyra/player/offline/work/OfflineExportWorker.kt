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
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.offline.OfflineAudioExporter
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class OfflineExportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val payload = inputData.getString(KEY_TRACK_PAYLOAD).orEmpty()
        val track = TrackPayloadCodec.decode(payload) ?: return Result.failure(errorData("Traccia non valida"))
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
                    if (safeProgress == 100 || safeProgress >= foregroundProgress + FOREGROUND_PROGRESS_STEP) {
                        foregroundProgress = safeProgress
                        setForeground(createForegroundInfo(track, safeProgress))
                    }
                }
            )
            val result = exporter.export(track)
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
            throw error
        } catch (error: Throwable) {
            if (error is IOException && runAttemptCount < 2) {
                Timber.w(error, "Offline export retry scheduled")
                Result.retry()
            } else {
                Timber.e(error, "Offline export failed")
                Result.failure(errorData(error.message ?: "Esportazione non riuscita"))
            }
        }
    }

    private fun errorData(message: String): Data = workDataOf(KEY_ERROR to message)

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
        const val KEY_FILE_NAME = "file_name"
        const val KEY_EMBEDDED_METADATA = "embedded_metadata"
        const val KEY_MIME_TYPE = "mime_type"
        const val KEY_URI = "uri"
        const val KEY_DESTINATION_LABEL = "destination_label"
        const val KEY_ERROR = "error"
        const val KEY_PROGRESS = "progress"
        private const val CHANNEL_ID = "levyra_offline_downloads"
        private const val FOREGROUND_PROGRESS_STEP = 2
        private const val NOTIFICATION_ID_BASE = 4200
        private const val NOTIFICATION_ID_RANGE = 5000

        fun enqueue(context: Context, trackId: String, trackPayload: String): UUID {
            val workManager = WorkManager.getInstance(context.applicationContext)
            val uniqueName = uniqueNameFor(trackId)
            val existing = workManager.getWorkInfosForUniqueWork(uniqueName).get().firstOrNull { !it.state.isFinished }
            if (existing != null) return existing.id
            val request = OneTimeWorkRequestBuilder<OfflineExportWorker>()
                .setInputData(workDataOf(KEY_TRACK_PAYLOAD to trackPayload))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .addTag("levyra_offline_export")
                .addTag(uniqueName)
                .build()
            workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, request)
            return request.id
        }

        private fun uniqueNameFor(trackId: String): String {
            val safe = trackId.trim().ifBlank { "unknown" }.replace(Regex("[^A-Za-z0-9_.-]+"), "_").take(120)
            return "levyra_offline_export_$safe"
        }
    }
}
