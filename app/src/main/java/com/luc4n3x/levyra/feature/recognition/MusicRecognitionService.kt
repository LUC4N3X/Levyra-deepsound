package com.luc4n3x.levyra.feature.recognition

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.TileService
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class MusicRecognitionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val notifications by lazy { RecognitionNotifications(this) }
    private var observerJob: Job? = null
    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var strings: LevyraStrings? = null
    private var foregroundStarted = false
    private var ownsActiveRecognition = false

    override fun onCreate() {
        super.onCreate()
        notifications.createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RECOGNIZE_MICROPHONE -> startMicrophoneRecognition()
            ACTION_RECOGNIZE_DEVICE_PLAYBACK -> startDevicePlaybackRecognition(intent)
            ACTION_CANCEL -> cancelRecognition()
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        observerJob?.cancel()
        observerJob = null
        if (ownsActiveRecognition) LevyraRecognitionCenter.cancel(this)
        ownsActiveRecognition = false
        releaseProjection()
        scope.cancel()
        super.onDestroy()
    }

    private fun startMicrophoneRecognition() {
        if (!hasRecordAudioPermission()) {
            finishWith(RecognitionState.Error(RecognitionErrorKind.PermissionDenied))
            return
        }
        if (!startRecognitionForeground(microphoneServiceType())) return
        ownsActiveRecognition = true
        LevyraRecognitionCenter.startMicrophone(this)
        observeRecognition()
    }

    private fun startDevicePlaybackRecognition(intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            finishWith(RecognitionState.Error(RecognitionErrorKind.Unavailable))
            return
        }
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = intent.projectionResultData()
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            finishWith(RecognitionState.Error(RecognitionErrorKind.Cancelled))
            return
        }
        if (!startRecognitionForeground(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)) return

        val manager = getSystemService(MediaProjectionManager::class.java)
        val projection = runCatching { manager?.getMediaProjection(resultCode, resultData) }
            .onFailure { Timber.w(it, "Media projection could not be created") }
            .getOrNull()
        if (projection == null) {
            finishWith(RecognitionState.Error(RecognitionErrorKind.Unavailable))
            return
        }
        mediaProjection = projection
        registerProjectionCallback(projection)
        ownsActiveRecognition = true
        LevyraRecognitionCenter.startDevicePlayback(this, projection)
        observeRecognition()
    }

    private fun observeRecognition() {
        observerJob?.cancel()
        observerJob = scope.launch {
            LevyraRecognitionCenter.get(this@MusicRecognitionService).state.collect { state ->
                requestTileRefresh()
                when (state) {
                    RecognitionState.Listening -> notifications.notify(notifications.listening(strings()))
                    RecognitionState.Identifying -> notifications.notify(notifications.processing(strings()))
                    RecognitionState.Idle -> Unit
                    is RecognitionState.Result,
                    RecognitionState.NoMatch,
                    is RecognitionState.Error -> finishWith(state)
                }
            }
        }
    }

    private fun startRecognitionForeground(serviceType: Int): Boolean {
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                RecognitionNotifications.NOTIFICATION_ID,
                notifications.listening(strings()),
                serviceType
            )
        }.onFailure { Timber.w(it, "Recognition foreground start failed") }.isSuccess
        foregroundStarted = started
        if (!started) stopSelf()
        return started
    }

    private fun cancelRecognition() {
        LevyraRecognitionCenter.cancel(this)
        ownsActiveRecognition = false
        finishWith(RecognitionState.Error(RecognitionErrorKind.Cancelled))
    }

    private fun finishWith(state: RecognitionState) {
        ownsActiveRecognition = false
        observerJob?.cancel()
        observerJob = null
        releaseProjection()
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        when (state) {
            is RecognitionState.Result -> notifications.notify(notifications.result(strings(), state.result))
            RecognitionState.NoMatch -> notifications.notify(notifications.failure(strings(), state))
            is RecognitionState.Error -> if (state.kind == RecognitionErrorKind.Cancelled) {
                notifications.cancel()
            } else {
                notifications.notify(notifications.failure(strings(), state))
            }
            else -> notifications.cancel()
        }
        requestTileRefresh()
        stopSelf()
    }

    private fun registerProjectionCallback(projection: MediaProjection) {
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                LevyraRecognitionCenter.cancel(this@MusicRecognitionService)
            }
        }
        projectionCallback = callback
        runCatching { projection.registerCallback(callback, Handler(Looper.getMainLooper())) }
            .onFailure { Timber.w(it, "Media projection callback registration failed") }
    }

    private fun releaseProjection() {
        val projection = mediaProjection ?: return
        mediaProjection = null
        projectionCallback?.let { callback -> runCatching { projection.unregisterCallback(callback) } }
        projectionCallback = null
        runCatching { projection.stop() }
    }

    private fun strings(): LevyraStrings = strings ?: LevyraStrings
        .forCode(LevyraPreferences(this).snapshot().languageCode)
        .also { strings = it }

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun microphoneServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

    private fun requestTileRefresh() {
        runCatching {
            TileService.requestListeningState(
                this,
                ComponentName(this, MusicRecognitionTileService::class.java)
            )
        }
    }

    private fun Intent.projectionResultData(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(EXTRA_RESULT_DATA)
        }

    companion object {
        private const val ACTION_RECOGNIZE_MICROPHONE =
            "com.luc4n3x.levyra.action.RECOGNIZE_MICROPHONE"
        private const val ACTION_RECOGNIZE_DEVICE_PLAYBACK =
            "com.luc4n3x.levyra.action.RECOGNIZE_DEVICE_PLAYBACK"
        private const val ACTION_CANCEL =
            "com.luc4n3x.levyra.action.CANCEL_RECOGNITION"
        private const val EXTRA_RESULT_CODE = "levyra.projection_result_code"
        private const val EXTRA_RESULT_DATA = "levyra.projection_result_data"

        fun microphoneIntent(context: Context): Intent =
            Intent(context, MusicRecognitionService::class.java).setAction(ACTION_RECOGNIZE_MICROPHONE)

        fun devicePlaybackIntent(context: Context, resultCode: Int, resultData: Intent): Intent =
            Intent(context, MusicRecognitionService::class.java)
                .setAction(ACTION_RECOGNIZE_DEVICE_PLAYBACK)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, resultData)

        fun cancelIntent(context: Context): Intent =
            Intent(context, MusicRecognitionService::class.java).setAction(ACTION_CANCEL)
    }
}
