package com.luc4n3x.levyra.player.offline.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.player.offline.OfflineAudioExporter
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
            val exporter = OfflineAudioExporter(
                context = applicationContext,
                resolver = PlaybackResolver.getInstance(applicationContext),
                progress = { value -> setProgress(workDataOf(KEY_PROGRESS to value.coerceIn(0, 100))) }
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

    companion object {
        const val KEY_TRACK_PAYLOAD = "track_payload"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_EMBEDDED_METADATA = "embedded_metadata"
        const val KEY_MIME_TYPE = "mime_type"
        const val KEY_URI = "uri"
        const val KEY_DESTINATION_LABEL = "destination_label"
        const val KEY_ERROR = "error"
        const val KEY_PROGRESS = "progress"

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
