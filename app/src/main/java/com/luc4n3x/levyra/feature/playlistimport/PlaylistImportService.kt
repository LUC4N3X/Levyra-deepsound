package com.luc4n3x.levyra.feature.playlistimport

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

data class PlaylistImportNotice(
    val title: String,
    val text: String,
    val done: Int,
    val total: Int?
)

class PlaylistImportService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundStarted = false
    private var observing = false
    private var shown: PlaylistImportNotice? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        starting = false
        if (!foregroundStarted && !startImportForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (notices.value == null) {
            shutdown()
            return START_NOT_STICKY
        }
        running = true
        if (!observing) {
            observing = true
            scope.launch { follow() }
        }
        return START_NOT_STICKY
    }

    private suspend fun follow() {
        while (true) {
            val notice = notices.value ?: break
            if (notice != shown) post(notice)
            delay(NOTIFY_INTERVAL_MS)
            notices.first { it != notice }
        }
        shutdown()
    }

    private fun startImportForeground(): Boolean {
        val notice = notices.value ?: lastNotice ?: return false
        createChannel(notice.title)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        foregroundStarted = runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(notice), type)
        }.onFailure { Timber.w(it, "Playlist import foreground start failed") }.isSuccess
        if (foregroundStarted) shown = notice
        return foregroundStarted
    }

    private fun post(notice: PlaylistImportNotice) {
        shown = notice
        runCatching { NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification(notice)) }
            .onFailure { Timber.d(it, "Playlist import notification update skipped") }
    }

    private fun createChannel(name: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun notification(notice: PlaylistImportNotice): Notification {
        val open = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val total = notice.total?.takeIf { it > 0 }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_download)
            .setContentTitle(notice.title)
            .setContentText(notice.text)
            .setProgress(total ?: 0, notice.done.coerceAtMost(total ?: 0), total == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .build()
    }

    private fun shutdown() {
        running = false
        observing = false
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Timber.w("Playlist import foreground time limit reached")
        shutdown()
    }

    override fun onDestroy() {
        scope.cancel()
        running = false
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "levyra_playlist_import"
        private const val NOTIFICATION_ID = 0x1F70
        private const val REQUEST_OPEN = 0x1F71
        private const val NOTIFY_INTERVAL_MS = 1_000L
        private val notices = MutableStateFlow<PlaylistImportNotice?>(null)

        @Volatile
        private var lastNotice: PlaylistImportNotice? = null

        @Volatile
        private var running = false

        @Volatile
        private var starting = false

        fun update(context: Context, notice: PlaylistImportNotice?) {
            if (notice != null) lastNotice = notice
            notices.value = notice
            if (notice == null || running || starting) return
            starting = runCatching {
                ContextCompat.startForegroundService(context, Intent(context, PlaylistImportService::class.java))
            }.onFailure { Timber.w(it, "Playlist import foreground start rejected") }.isSuccess
        }
    }
}
