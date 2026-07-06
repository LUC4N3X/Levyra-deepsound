package com.luc4n3x.levyra.data

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
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import timber.log.Timber
import java.util.concurrent.TimeUnit

class PersonalOrbitArtworkWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val preferences = LevyraPreferences(applicationContext)
            val languageCode = inputData.getString(KEY_LANGUAGE_CODE).orEmpty().ifBlank { preferences.languageCode() }
            val tracks = preferences.loadPersonalOrbitTracks(languageCode).take(LevyraPersonalOrbit.DISPLAY_LIMIT)
            if (tracks.isNotEmpty()) {
                LevyraArtworkCache.configure(applicationContext)
                LevyraArtworkCache.cachePersistent(applicationContext, tracks, LevyraPersonalOrbit.DISPLAY_LIMIT)
                LevyraArtworkCache.preloadPriority(applicationContext, tracks, LevyraPersonalOrbit.DISPLAY_LIMIT)
            }
            Result.success()
        }.getOrElse { error ->
            Timber.w(error, "Personal orbit artwork cache failed")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "levyra_personal_orbit_artwork"
        private const val KEY_LANGUAGE_CODE = "language_code"

        fun enqueue(context: Context, languageCode: String = "") {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<PersonalOrbitArtworkWorker>()
                .setConstraints(constraints)
                .setInputData(Data.Builder().putString(KEY_LANGUAGE_CODE, languageCode).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            val workName = if (languageCode.isBlank()) WORK_NAME else "${WORK_NAME}_${languageCode}"
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
