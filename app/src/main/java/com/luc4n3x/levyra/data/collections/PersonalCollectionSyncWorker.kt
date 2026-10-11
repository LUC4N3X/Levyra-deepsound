package com.luc4n3x.levyra.data.collections

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class PersonalCollectionSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        PersonalCollectionRepository.get(applicationContext).refreshAutomatic()
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        Timber.w("Personal collection automatic refresh failed: %s", error.javaClass.simpleName)
        Result.success()
    }
}

object PersonalCollectionSyncScheduler {
    private const val WORK_NAME = "levyra_personal_collection_refresh"
    private const val INTERVAL_HOURS = 12L

    fun update(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<PersonalCollectionSyncWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(INTERVAL_HOURS, TimeUnit.HOURS)
            .build()
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
