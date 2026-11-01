package com.luc4n3x.levyra.feature.adaptive

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.domain.AdaptiveListeningCondition
import com.luc4n3x.levyra.domain.AdaptiveListeningEngine
import com.luc4n3x.levyra.domain.AdaptiveListeningSnapshot
import com.luc4n3x.levyra.domain.AmbientFrameMeter
import com.luc4n3x.levyra.domain.AmbientWindow
import com.luc4n3x.levyra.player.AdaptiveListeningBus
import com.luc4n3x.levyra.player.AdaptiveListeningConfig
import com.luc4n3x.levyra.ui.i18n.SmartMusicText
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class AdaptiveListeningService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var configJob: Job? = null
    private var foregroundStarted = false
    private var engine: AdaptiveListeningEngine? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AdaptiveListeningBus.reset(AdaptiveListeningSnapshot(AdaptiveListeningCondition.Waiting))
            shutdown()
            return START_NOT_STICKY
        }
        val permitted = hasMicrophonePermission()
        if (!foregroundStarted && !startMicrophoneForeground()) {
            AdaptiveListeningBus.reset(
                AdaptiveListeningSnapshot(
                    if (permitted) AdaptiveListeningCondition.Unavailable else AdaptiveListeningCondition.PermissionMissing
                )
            )
            shutdown()
            return START_NOT_STICKY
        }
        if (!permitted || !AdaptiveListeningBus.config.value.enabled) {
            if (!permitted) AdaptiveListeningBus.reset(AdaptiveListeningSnapshot(AdaptiveListeningCondition.PermissionMissing))
            shutdown()
            return START_NOT_STICKY
        }
        if (monitorJob?.isActive != true) startMonitoring()
        return START_NOT_STICKY
    }

    private fun startMonitoring() {
        val config = AdaptiveListeningBus.config.value
        val monitor = AdaptiveListeningEngine(config.sensitivity, config.maxBoostDb, storedReference())
        engine = monitor
        AdaptiveListeningBus.monitoring = true
        configJob = scope.launch {
            AdaptiveListeningBus.config.collect { next ->
                if (!next.enabled) {
                    AdaptiveListeningBus.reset(AdaptiveListeningSnapshot(AdaptiveListeningCondition.Off))
                    shutdown()
                } else {
                    monitor.configure(next.sensitivity, next.maxBoostDb)
                }
            }
        }
        monitorJob = scope.launch {
            AdaptiveListeningBus.playbackActive.collectLatest { playing ->
                if (!playing) {
                    monitor.suspend(AdaptiveListeningCondition.Waiting)
                    AdaptiveListeningBus.publish(monitor.snapshot)
                    delay(IDLE_STOP_MS)
                    shutdown()
                    return@collectLatest
                }
                while (true) {
                    if (!headphonesActive()) {
                        monitor.suspend(AdaptiveListeningCondition.SpeakerOutput)
                        AdaptiveListeningBus.publish(monitor.snapshot)
                        delay(ROUTE_POLL_MS)
                        continue
                    }
                    try {
                        capture(monitor)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Throwable) {
                        Timber.w(error, "Adaptive listening capture failed")
                        monitor.suspend(AdaptiveListeningCondition.Unavailable)
                        AdaptiveListeningBus.publish(monitor.snapshot)
                    } finally {
                        saveReference(monitor)
                    }
                    delay(ROUTE_POLL_MS)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun capture(monitor: AdaptiveListeningEngine) = withContext(Dispatchers.IO) {
        val record = openRecord() ?: run {
            monitor.suspend(AdaptiveListeningCondition.Unavailable)
            AdaptiveListeningBus.publish(monitor.snapshot)
            return@withContext
        }
        val meter = AmbientFrameMeter(record.sampleRate)
        val chunk = ShortArray(record.sampleRate / CHUNKS_PER_SECOND)
        val frames = FloatArray(FRAMES_PER_WINDOW)
        var frameCount = 0
        var clippedFrames = 0
        var windowsSinceSave = 0
        val sink = AmbientFrameMeter.FrameSink { level, clipped ->
            if (frameCount < frames.size) {
                frames[frameCount] = level
                frameCount++
                if (clipped) clippedFrames++
            }
        }
        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                monitor.suspend(AdaptiveListeningCondition.Unavailable)
                AdaptiveListeningBus.publish(monitor.snapshot)
                return@withContext
            }
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = record.read(chunk, 0, chunk.size, AudioRecord.READ_BLOCKING)
                if (read < 0) {
                    monitor.suspend(AdaptiveListeningCondition.Unavailable)
                    AdaptiveListeningBus.publish(monitor.snapshot)
                    return@withContext
                }
                meter.process(chunk, read, sink)
                if (frameCount >= frames.size) {
                    val snapshot = monitor.onWindow(
                        AmbientWindow(
                            frameLevelsDb = frames.copyOf(frameCount),
                            clippedFrames = clippedFrames,
                            outputLevelDb = AdaptiveListeningBus.outputLevelDb,
                            headphonesActive = headphonesActive(),
                            atMs = SystemClock.elapsedRealtime()
                        )
                    )
                    AdaptiveListeningBus.publish(snapshot)
                    if (snapshot.condition == AdaptiveListeningCondition.SpeakerOutput) return@withContext
                    frameCount = 0
                    clippedFrames = 0
                    windowsSinceSave++
                    if (windowsSinceSave >= SAVE_EVERY_WINDOWS) {
                        windowsSinceSave = 0
                        saveReference(monitor)
                    }
                }
            }
        } finally {
            runCatching { record.stop() }
            record.release()
            meter.reset()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openRecord(): AudioRecord? {
        val source = if (unprocessedSupported()) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
        for (rate in SAMPLE_RATES) {
            val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minimum <= 0) continue
            val record = runCatching {
                AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, rate / 2) * 2)
            }.getOrNull()
            if (record != null && record.state == AudioRecord.STATE_INITIALIZED) return record
            record?.release()
        }
        return null
    }

    private fun unprocessedSupported(): Boolean = runCatching {
        getSystemService(AudioManager::class.java)?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
    }.getOrDefault(false)

    private fun headphonesActive(): Boolean {
        val manager = getSystemService(AudioManager::class.java) ?: return false
        return runCatching {
            manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in HEADPHONE_TYPES }
        }.getOrDefault(false)
    }

    private fun storedReference(): Float {
        val preferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val stored = preferences.getFloat(KEY_REFERENCE_DB, Float.NaN)
        if (!stored.isFinite()) return Float.NaN
        val savedAt = preferences.getLong(KEY_REFERENCE_AT, 0L)
        val days = ((System.currentTimeMillis() - savedAt).coerceAtLeast(0L) / DAY_MS).toFloat()
        return stored + days * REFERENCE_DRIFT_DB_PER_DAY
    }

    private fun saveReference(monitor: AdaptiveListeningEngine) {
        val reference = monitor.referenceDb
        if (!reference.isFinite() || reference <= AdaptiveListeningEngine.MIN_PLAUSIBLE_REFERENCE_DB) return
        val current = storedReference()
        if (current.isFinite() && reference >= current) return
        getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_REFERENCE_DB, reference)
            .putLong(KEY_REFERENCE_AT, System.currentTimeMillis())
            .apply()
    }

    private fun startMicrophoneForeground(): Boolean {
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        foregroundStarted = runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
        }.onFailure { Timber.w(it, "Adaptive listening foreground start failed") }.isSuccess
        return foregroundStarted
    }

    private fun copy() = smartMusicCopy(runCatching { LevyraPreferences(this).snapshot().languageCode }.getOrDefault("en"))

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, copy()[SmartMusicText.AdaptiveTitle], NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
            setSound(null, null)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun notification(): Notification {
        val text = copy()
        val open = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, AdaptiveListeningService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qs_adaptive_listening)
            .setContentTitle(text[SmartMusicText.AdaptiveTitle])
            .setContentText(text[SmartMusicText.AdaptiveNotificationText])
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .addAction(0, text[SmartMusicText.AdaptiveNotificationStop], stop)
            .build()
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun shutdown() {
        monitorJob?.cancel()
        configJob?.cancel()
        monitorJob = null
        configJob = null
        AdaptiveListeningBus.monitoring = false
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    override fun onDestroy() {
        engine?.let(::saveReference)
        scope.cancel()
        AdaptiveListeningBus.monitoring = false
        val current = AdaptiveListeningBus.snapshot.value.condition
        AdaptiveListeningBus.reset(
            AdaptiveListeningSnapshot(
                if (AdaptiveListeningBus.config.value.enabled && current != AdaptiveListeningCondition.PermissionMissing) {
                    AdaptiveListeningCondition.Waiting
                } else {
                    current.takeIf { it == AdaptiveListeningCondition.PermissionMissing } ?: AdaptiveListeningCondition.Off
                }
            )
        )
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "com.luc4n3x.levyra.action.STOP_ADAPTIVE_LISTENING"
        private const val CHANNEL_ID = "levyra_adaptive_listening"
        private const val NOTIFICATION_ID = 0x1AD7
        private const val REQUEST_OPEN = 0x1AD8
        private const val REQUEST_STOP = 0x1AD9
        private const val PREFERENCES = "levyra_adaptive_listening"
        private const val KEY_REFERENCE_DB = "quiet_reference_db"
        private const val KEY_REFERENCE_AT = "quiet_reference_at"
        private const val REFERENCE_DRIFT_DB_PER_DAY = 1f
        private const val DAY_MS = 24L * 60L * 60L * 1000L
        private const val IDLE_STOP_MS = 30L * 60L * 1000L
        private const val ROUTE_POLL_MS = 5_000L
        private const val CHUNKS_PER_SECOND = 10
        private const val FRAMES_PER_WINDOW = (AdaptiveListeningEngine.WINDOW_MS / AmbientFrameMeter.FRAME_MS).toInt()
        private const val SAVE_EVERY_WINDOWS = 60
        private val SAMPLE_RATES = intArrayOf(16_000, 44_100, 48_000)
        private val HEADPHONE_TYPES = buildSet {
            add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
            add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_USB_HEADSET)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(AudioDeviceInfo.TYPE_BLE_HEADSET)
            }
        }

        fun start(context: Context, config: AdaptiveListeningConfig) {
            AdaptiveListeningBus.configure(config)
            if (!config.enabled) return
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                AdaptiveListeningBus.reset(AdaptiveListeningSnapshot(AdaptiveListeningCondition.PermissionMissing))
                return
            }
            runCatching { ContextCompat.startForegroundService(context, Intent(context, AdaptiveListeningService::class.java)) }
                .onFailure { Timber.w(it, "Adaptive listening start rejected") }
        }

        fun stop(context: Context) {
            runCatching { context.startService(Intent(context, AdaptiveListeningService::class.java).setAction(ACTION_STOP)) }
        }
    }
}
