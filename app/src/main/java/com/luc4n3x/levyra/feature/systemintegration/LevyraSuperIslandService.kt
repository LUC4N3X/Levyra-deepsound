package com.luc4n3x.levyra.feature.systemintegration

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.luc4n3x.levyra.R

class LevyraSuperIslandService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = synchronized(lock) { pending }
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification ?: placeholder(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        }.onFailure { logIslandFailure(LevyraDeviceFeature.SUPER_ISLAND, it) }.isSuccess
        val keep = synchronized(lock) {
            val current = pending
            val keepRunning = started && current != null
            state = if (keepRunning) State.RUNNING else State.IDLE
            if (keepRunning && current !== notification) notifyForeground(current)
            keepRunning
        }
        if (!keep) {
            if (started) ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        synchronized(lock) {
            if (state == State.RUNNING) state = State.IDLE
        }
        super.onDestroy()
    }

    private fun placeholder(): Notification {
        ensureLevyraIslandChannel(this)
        return Notification.Builder(this, LEVYRA_ISLAND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_widget_lyrics)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .build()
    }

    private fun notifyForeground(notification: Notification?) {
        if (notification == null) return
        runCatching {
            getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
        }
    }

    private enum class State {
        IDLE,
        STARTING,
        RUNNING
    }

    internal companion object {
        private const val NOTIFICATION_ID = 0x4c535349
        private val lock = Any()
        private var pending: Notification? = null
        private var state = State.IDLE

        val isShowing: Boolean
            get() = synchronized(lock) { state != State.IDLE }

        fun show(context: Context, notification: Notification): Boolean {
            val appContext = context.applicationContext
            synchronized(lock) {
                pending = notification
                when (state) {
                    State.RUNNING -> {
                        return runCatching {
                            appContext.getSystemService(NotificationManager::class.java)
                                ?.notify(NOTIFICATION_ID, notification)
                        }.isSuccess
                    }
                    State.STARTING -> return true
                    State.IDLE -> Unit
                }
                state = State.STARTING
            }
            val started = runCatching {
                ContextCompat.startForegroundService(appContext, Intent(appContext, LevyraSuperIslandService::class.java))
            }.onFailure { logIslandFailure(LevyraDeviceFeature.SUPER_ISLAND, it) }.isSuccess
            if (!started) {
                synchronized(lock) {
                    state = State.IDLE
                    pending = null
                }
            }
            return started
        }

        fun hide(context: Context) {
            val appContext = context.applicationContext
            val stop = synchronized(lock) {
                pending = null
                if (state == State.RUNNING) {
                    state = State.IDLE
                    true
                } else {
                    false
                }
            }
            if (!stop) return
            runCatching { appContext.stopService(Intent(appContext, LevyraSuperIslandService::class.java)) }
            runCatching { appContext.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID) }
        }
    }
}
