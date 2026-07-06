package com.luc4n3x.levyra.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
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
            val tracks = preferences.loadPersonalOrbitTracks().take(LevyraPersonalOrbit.DISPLAY_LIMIT)
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

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<PersonalOrbitArtworkWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
