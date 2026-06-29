package com.luc4n3x.levyra.player.offline.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
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

class OfflineExportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val payload = inputData.getString(KEY_TRACK_PAYLOAD).orEmpty()
        val track = TrackPayloadCodec.decode(payload) ?: return Result.failure(errorData("Traccia non valida"))
        return try {
            val exporter = OfflineAudioExporter(applicationContext, PlaybackResolver.getInstance(applicationContext))
            val result = exporter.export(track)
            Result.success(
                workDataOf(
                    KEY_FILE_NAME to result.fileName,
                    KEY_EMBEDDED_METADATA to result.fileMetadataEmbedded,
                    KEY_MIME_TYPE to result.mimeType,
                    KEY_URI to result.uri.toString()
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
        const val KEY_ERROR = "error"

        fun enqueue(context: Context, trackPayload: String): UUID {
            val request = OneTimeWorkRequestBuilder<OfflineExportWorker>()
                .setInputData(workDataOf(KEY_TRACK_PAYLOAD to trackPayload))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("levyra_offline_export")
                .build()
            WorkManager.getInstance(context.applicationContext).enqueue(request)
            return request.id
        }
    }
}
