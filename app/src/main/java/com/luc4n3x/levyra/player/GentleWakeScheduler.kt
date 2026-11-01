package com.luc4n3x.levyra.player

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.domain.GentleWakeReliabilityStatus
import com.luc4n3x.levyra.domain.GentleWakeScheduleCalculator
import com.luc4n3x.levyra.domain.GentleWakeSettings
import com.luc4n3x.levyra.domain.MorningResumeSnapshot
import com.luc4n3x.levyra.ui.i18n.smartListeningCopyForCode
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

object GentleWakeScheduler {
    const val ACTION_TRIGGER_GENTLE_WAKE = "com.luc4n3x.levyra.action.TRIGGER_GENTLE_WAKE"
    const val ACTION_SNOOZE_GENTLE_WAKE = "com.luc4n3x.levyra.action.SNOOZE_GENTLE_WAKE"
    const val ACTION_STOP_GENTLE_WAKE = "com.luc4n3x.levyra.action.STOP_GENTLE_WAKE"
    const val ACTION_MORNING_RESUME_PROMPT = "com.luc4n3x.levyra.action.MORNING_RESUME_PROMPT"
    const val ACTION_MORNING_RESUME_PLAY = "com.luc4n3x.levyra.action.MORNING_RESUME_PLAY"
    const val ACTION_MORNING_RESUME_DISMISS = "com.luc4n3x.levyra.action.MORNING_RESUME_DISMISS"
    const val EXTRA_OPEN_GENTLE_WAKE = "com.luc4n3x.levyra.extra.OPEN_GENTLE_WAKE"
    const val EXTRA_SNOOZE_DURATION_MS = "com.luc4n3x.levyra.extra.SNOOZE_DURATION_MS"

    private const val GENTLE_WAKE_REQUEST_CODE = 5401
    private const val GENTLE_WAKE_SHOW_REQUEST_CODE = 5402
    private const val GENTLE_WAKE_SNOOZE_REQUEST_CODE = 5403
    private const val GENTLE_WAKE_STOP_REQUEST_CODE = 5404
    private const val MORNING_PROMPT_REQUEST_CODE = 5410
    private const val MORNING_PLAY_REQUEST_CODE = 5411
    private const val MORNING_DISMISS_REQUEST_CODE = 5412

    const val GENTLE_WAKE_CHANNEL_ID = "levyra_gentle_wake_alarm"
    const val MORNING_RESUME_CHANNEL_ID = "levyra_morning_resume_prompt"
    const val GENTLE_WAKE_NOTIFICATION_ID = 5480
    const val MORNING_RESUME_NOTIFICATION_ID = 5481

    @Volatile
    private var snoozeUntilEpochMs: Long = 0L

    fun currentSnoozeUntilEpochMs(): Long = snoozeUntilEpochMs

    fun reliabilityStatus(context: Context): GentleWakeReliabilityStatus {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val exactGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
        val runtimeNotifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val notificationsEnabled = runtimeNotifGranted &&
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        return GentleWakeReliabilityStatus(
            exactAlarmPermitted = exactGranted,
            notificationsPermitted = notificationsEnabled
        )
    }

    fun buildExactAlarmSettingsIntent(context: Context): Intent {
        val pkgUri = Uri.fromParts("package", context.packageName, null)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun buildNotificationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun apply(
        context: Context,
        settings: GentleWakeSettings,
        now: ZonedDateTime = ZonedDateTime.now()
    ): ZonedDateTime? {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return null
        val normalized = settings.normalized()

        if (!normalized.enabled && snoozeUntilEpochMs <= now.toInstant().toEpochMilli()) {
            cancelScheduledWake(appContext, alarmManager)
            return null
        }

        val next = GentleWakeScheduleCalculator.nextTriggerAt(
            settings = normalized,
            now = now,
            snoozeUntilEpochMs = snoozeUntilEpochMs
        )
        if (next == null) {
            cancelScheduledWake(appContext, alarmManager)
            return null
        }

        val triggerAtMillis = next.toInstant().toEpochMilli()
        val operation = wakeTriggerPendingIntent(appContext, PendingIntent.FLAG_UPDATE_CURRENT) ?: return null
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        runCatching {
            if (canExact) {
                val showIntent = Intent(appContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_OPEN_GENTLE_WAKE, true)
                }
                val showPending = PendingIntent.getActivity(
                    appContext,
                    GENTLE_WAKE_SHOW_REQUEST_CODE,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showPending),
                    operation
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    operation
                )
            }
        }.onFailure { error ->
            Timber.w(error, "GentleWakeScheduler: schedule failed")
        }
        return next
    }

    fun scheduleSnooze(
        context: Context,
        settings: GentleWakeSettings,
        customDelayMs: Long? = null
    ): Long {
        val delayMs = (customDelayMs ?: (settings.normalized().snoozeMinutes * 60_000L))
            .coerceIn(5_000L, 30 * 60_000L)
        val targetEpochMs = System.currentTimeMillis() + delayMs
        snoozeUntilEpochMs = targetEpochMs
        apply(context.applicationContext, settings)
        return targetEpochMs
    }

    fun clearSnooze(context: Context, settings: GentleWakeSettings) {
        snoozeUntilEpochMs = 0L
        apply(context.applicationContext, settings)
    }

    fun cancelScheduledWake(context: Context, alarmManager: AlarmManager? = null) {
        snoozeUntilEpochMs = 0L
        val appContext = context.applicationContext
        val manager = alarmManager ?: (appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager) ?: return
        val existing = wakeTriggerPendingIntent(appContext, PendingIntent.FLAG_NO_CREATE) ?: return
        runCatching {
            manager.cancel(existing)
            existing.cancel()
        }
    }

    fun scheduleMorningResumePrompt(
        context: Context,
        snapshot: MorningResumeSnapshot?
    ) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (snapshot == null || snapshot.dismissed || snapshot.promptAtEpochMs <= 0L) {
            val existing = morningPromptPendingIntent(appContext, PendingIntent.FLAG_NO_CREATE)
            if (existing != null) {
                runCatching {
                    alarmManager.cancel(existing)
                    existing.cancel()
                }
            }
            return
        }
        val triggerAt = snapshot.promptAtEpochMs.coerceAtLeast(System.currentTimeMillis() + 5_000L)
        val operation = morningPromptPendingIntent(appContext, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        runCatching {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }.onFailure { Timber.w(it, "GentleWakeScheduler: morning prompt schedule failed") }
    }

    fun postGentleWakeRingingNotification(
        context: Context,
        languageCode: String,
        sourceTitle: String,
        usingEmergencyFallback: Boolean
    ) {
        val appContext = context.applicationContext
        if (!reliabilityStatus(appContext).notificationsPermitted) return
        val notification = buildGentleWakeRingingNotification(appContext, languageCode, sourceTitle, usingEmergencyFallback)
        runCatching {
            NotificationManagerCompat.from(appContext).notify(GENTLE_WAKE_NOTIFICATION_ID, notification)
        }.onFailure { Timber.w(it, "GentleWakeScheduler: notify ringing failed") }
    }

    fun buildGentleWakeRingingNotification(
        context: Context,
        languageCode: String,
        sourceTitle: String,
        usingEmergencyFallback: Boolean
    ): Notification {
        val appContext = context.applicationContext
        ensureNotificationChannels(appContext, languageCode)
        val copy = smartListeningCopyForCode(languageCode)
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_GENTLE_WAKE, true)
        }
        val contentPending = PendingIntent.getActivity(
            appContext,
            GENTLE_WAKE_SHOW_REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(appContext, GentleWakeAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_GENTLE_WAKE
        }
        val snoozePending = PendingIntent.getBroadcast(
            appContext,
            GENTLE_WAKE_SNOOZE_REQUEST_CODE,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(appContext, GentleWakeAlarmReceiver::class.java).apply {
            action = ACTION_STOP_GENTLE_WAKE
        }
        val stopPending = PendingIntent.getBroadcast(
            appContext,
            GENTLE_WAKE_STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitle = if (usingEmergencyFallback) {
            copy.emergencyFallbackNotice
        } else {
            sourceTitle.ifBlank { copy.builtInToneName }
        }

        return NotificationCompat.Builder(appContext, GENTLE_WAKE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_playing)
            .setContentTitle(copy.gentleWakeTitle)
            .setContentText(subtitle)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPending)
            .setFullScreenIntent(contentPending, true)
            .addAction(0, copy.snoozeAction, snoozePending)
            .addAction(0, copy.stopAlarmForToday, stopPending)
            .build()
    }

    fun cancelGentleWakeNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context.applicationContext).cancel(GENTLE_WAKE_NOTIFICATION_ID)
        }
    }

    fun postMorningResumeNotification(
        context: Context,
        languageCode: String,
        snapshot: MorningResumeSnapshot
    ) {
        val appContext = context.applicationContext
        ensureNotificationChannels(appContext, languageCode)
        if (!reliabilityStatus(appContext).notificationsPermitted) return

        val copy = smartListeningCopyForCode(languageCode)
        val resumeIntent = Intent(appContext, MainActivity::class.java).apply {
            action = ACTION_MORNING_RESUME_PLAY
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val resumePending = PendingIntent.getActivity(
            appContext,
            MORNING_PLAY_REQUEST_CODE,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(appContext, GentleWakeAlarmReceiver::class.java).apply {
            action = ACTION_MORNING_RESUME_DISMISS
        }
        val dismissPending = PendingIntent.getBroadcast(
            appContext,
            MORNING_DISMISS_REQUEST_CODE,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val trackSummary = listOfNotNull(
            snapshot.track.title.takeIf { it.isNotBlank() },
            snapshot.track.artist.takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        val notification = NotificationCompat.Builder(appContext, MORNING_RESUME_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_playing)
            .setContentTitle(copy.morningPromptTitle)
            .setContentText(trackSummary)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(resumePending)
            .setDeleteIntent(dismissPending)
            .addAction(0, copy.morningPromptActionResume, resumePending)
            .addAction(0, copy.morningPromptActionLater, dismissPending)
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext).notify(MORNING_RESUME_NOTIFICATION_ID, notification)
        }.onFailure { Timber.w(it, "GentleWakeScheduler: morning resume notification failed") }
    }

    fun cancelMorningResumeNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context.applicationContext).cancel(MORNING_RESUME_NOTIFICATION_ID)
        }
    }

    private fun ensureNotificationChannels(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val copy = smartListeningCopyForCode(languageCode)

        val wakeChannel = NotificationChannel(
            GENTLE_WAKE_CHANNEL_ID,
            copy.gentleWakeTitle,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = copy.gentleWakeSubtitle
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val morningChannel = NotificationChannel(
            MORNING_RESUME_CHANNEL_ID,
            copy.morningResumeTitle,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = copy.morningResumeSubtitle
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannel(wakeChannel)
        manager.createNotificationChannel(morningChannel)
    }

    private fun wakeTriggerPendingIntent(context: Context, flag: Int): PendingIntent? {
        val intent = Intent(context, GentleWakeAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_GENTLE_WAKE
        }
        return PendingIntent.getBroadcast(
            context,
            GENTLE_WAKE_REQUEST_CODE,
            intent,
            flag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun morningPromptPendingIntent(context: Context, flag: Int): PendingIntent? {
        val intent = Intent(context, GentleWakeAlarmReceiver::class.java).apply {
            action = ACTION_MORNING_RESUME_PROMPT
        }
        return PendingIntent.getBroadcast(
            context,
            MORNING_PROMPT_REQUEST_CODE,
            intent,
            flag or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class GentleWakeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val preferences = LevyraPreferences(appContext)
                val wakeSettings = preferences.gentleWakeSettingsFlow.first()
                when (action) {
                    GentleWakeScheduler.ACTION_TRIGGER_GENTLE_WAKE -> {
                        val todayIso = LocalDate.now().toString()
                        val wasSnoozed = GentleWakeScheduler.currentSnoozeUntilEpochMs() > 0L
                        GentleWakeScheduler.clearSnooze(appContext, wakeSettings)
                        if (!wasSnoozed && (!wakeSettings.enabled || wakeSettings.dismissedDateIso == todayIso)) {
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            PlaybackService.triggerGentleWakeAlarm(appContext, wakeSettings)
                        }
                    }
                    GentleWakeScheduler.ACTION_SNOOZE_GENTLE_WAKE -> {
                        val customDelayMs = intent.getLongExtra(GentleWakeScheduler.EXTRA_SNOOZE_DURATION_MS, -1L)
                            .takeIf { it > 0L }
                        withContext(Dispatchers.Main) {
                            PlaybackService.snoozeGentleWakeAlarm(appContext, customDelayMs)
                        }
                    }
                    GentleWakeScheduler.ACTION_STOP_GENTLE_WAKE -> {
                        withContext(Dispatchers.Main) {
                            PlaybackService.stopGentleWakeAlarmForToday(appContext)
                        }
                    }
                    GentleWakeScheduler.ACTION_MORNING_RESUME_PROMPT -> {
                        val snapshot = preferences.morningResumeSnapshotFlow.first()
                        val lang = preferences.snapshot().languageCode
                        if (snapshot != null && !snapshot.dismissed && !PlaybackService.isGentleWakeRinging()) {
                            GentleWakeScheduler.postMorningResumeNotification(appContext, lang, snapshot)
                        }
                    }
                    GentleWakeScheduler.ACTION_MORNING_RESUME_DISMISS -> {
                        val snapshot = preferences.morningResumeSnapshotFlow.first()
                        if (snapshot != null) {
                            preferences.setMorningResumeSnapshot(snapshot.copy(dismissed = true))
                        }
                        GentleWakeScheduler.cancelMorningResumeNotification(appContext)
                    }
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                        GentleWakeScheduler.apply(appContext, wakeSettings)
                        val snapshot = preferences.morningResumeSnapshotFlow.first()
                        GentleWakeScheduler.scheduleMorningResumePrompt(appContext, snapshot)
                    }
                }
            } catch (error: Throwable) {
                Timber.w(error, "GentleWakeAlarmReceiver failed for action=%s", action)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
