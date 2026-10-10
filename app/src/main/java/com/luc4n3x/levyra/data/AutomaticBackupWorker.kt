package com.luc4n3x.levyra.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.luc4n3x.levyra.domain.LevyraBackupSettings
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

class AutomaticBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = LevyraPreferences(applicationContext).backupSettings().normalized()
        if (!settings.enabled) return Result.success()
        return try {
            LevyraBackupManager(applicationContext).exportAutomatic(settings.retentionCount)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            Timber.w(error, "Automatic backup failed")
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } catch (error: Throwable) {
            Timber.e(error, "Automatic backup failed permanently")
            Result.failure()
        }
    }

    private companion object {
        const val MAX_RETRIES = 2
    }
}

object AutomaticBackupScheduler {
    private const val WORK_NAME = "levyra_automatic_backup"

    fun schedule(context: Context, settings: LevyraBackupSettings) {
        val manager = WorkManager.getInstance(context.applicationContext)
        val normalized = settings.normalized()
        if (!normalized.enabled) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .setRequiresCharging(normalized.chargingOnly)
            .build()
        val request = PeriodicWorkRequestBuilder<AutomaticBackupWorker>(
            normalized.frequency.intervalDays,
            TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(15L, TimeUnit.MINUTES)
            .build()
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
