package com.luc4n3x.levyra.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.luc4n3x.levyra.domain.FlipGestureDetector
import com.luc4n3x.levyra.domain.MotionControlSettings
import com.luc4n3x.levyra.domain.MotionSensorCapability
import com.luc4n3x.levyra.domain.PlaybackPriorityCoordinator
import com.luc4n3x.levyra.domain.ShakeGestureDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

internal class MotionGestureController(
    context: Context,
    private val onFlipToPause: () -> Unit,
    private val onShakeToSkip: () -> Unit
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val defaultGravitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val defaultAccelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val wakeUpGravitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY, true)
    private val wakeUpAccelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)

    val capability: MotionSensorCapability = MotionSensorCapability(
        hasGravitySensor = defaultGravitySensor != null || wakeUpGravitySensor != null,
        hasAccelerometer = defaultAccelerometer != null || wakeUpAccelerometer != null,
        wakeUpSensorSupported = wakeUpGravitySensor != null || wakeUpAccelerometer != null
    )

    private var settings = MotionControlSettings()
    private var isPlaying = false
    private var gentleWakeRinging = false
    private var androidAutoConnected = false
    private var screenInteractive = powerManager?.isInteractive ?: true
    private var screenReceiverRegistered = false
    private var sensorsRegistered = false

    private var flipDetector = FlipGestureDetector(settings.flipConfirmationDelayMs)
    private val shakeDetector = ShakeGestureDetector()

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    screenInteractive = true
                    reconcileRegistration()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    screenInteractive = false
                    reconcileRegistration()
                }
            }
        }
    }

    init {
        _capabilityFlow.value = capability
    }

    fun updateSettings(newSettings: MotionControlSettings) {
        val normalized = newSettings.normalized()
        val delayChanged = normalized.flipConfirmationDelayMs != settings.flipConfirmationDelayMs
        settings = normalized
        if (delayChanged) {
            flipDetector = FlipGestureDetector(normalized.flipConfirmationDelayMs)
        }
        reconcileScreenReceiver()
        reconcileRegistration()
    }

    fun updatePlaybackContext(
        playing: Boolean,
        alarmRinging: Boolean,
        autoConnected: Boolean
    ) {
        if (isPlaying == playing &&
            gentleWakeRinging == alarmRinging &&
            androidAutoConnected == autoConnected
        ) {
            return
        }
        isPlaying = playing
        gentleWakeRinging = alarmRinging
        androidAutoConnected = autoConnected
        reconcileRegistration()
    }

    fun release() {
        unregisterSensors()
        unregisterScreenReceiver()
    }

    private fun isVoiceCallActive(): Boolean {
        val mode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        return mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION ||
            mode == AudioManager.MODE_RINGTONE
    }

    private fun reconcileScreenReceiver() {
        val shouldListenScreen = settings.hasActiveGesture && capability.isAvailable
        if (shouldListenScreen && !screenReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            runCatching {
                ContextCompat.registerReceiver(
                    appContext,
                    screenStateReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                screenReceiverRegistered = true
                screenInteractive = powerManager?.isInteractive ?: true
            }.onFailure { Timber.w(it, "Motion screen receiver registration failed") }
        } else if (!shouldListenScreen && screenReceiverRegistered) {
            unregisterScreenReceiver()
        }
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return
        screenReceiverRegistered = false
        runCatching { appContext.unregisterReceiver(screenStateReceiver) }
    }

    private fun reconcileRegistration() {
        val canAccept = PlaybackPriorityCoordinator.canAcceptMotionGesture(
            motionEnabled = settings.hasActiveGesture && capability.isAvailable,
            isPlaying = isPlaying,
            gentleWakeRinging = gentleWakeRinging,
            inVoiceCall = isVoiceCallActive(),
            androidAutoConnected = androidAutoConnected
        )
        val screenAllowed = screenInteractive ||
            (settings.allowScreenOffWhenSupported && capability.wakeUpSensorSupported)
        val shouldRegister = canAccept && screenAllowed

        if (!shouldRegister) {
            unregisterSensors()
            return
        }

        if (sensorsRegistered) {
            unregisterSensors()
        }

        val manager = sensorManager ?: return
        flipDetector.reset()
        shakeDetector.reset()

        val useWakeUp = !screenInteractive && settings.allowScreenOffWhenSupported && capability.wakeUpSensorSupported
        var anyRegistered = false

        if (settings.flipToPauseEnabled) {
            val flipSensor = if (useWakeUp) {
                wakeUpGravitySensor ?: wakeUpAccelerometer ?: defaultGravitySensor ?: defaultAccelerometer
            } else {
                defaultGravitySensor ?: defaultAccelerometer
            }
            if (flipSensor != null) {
                val ok = manager.registerListener(
                    this,
                    flipSensor,
                    SensorManager.SENSOR_DELAY_UI,
                    80_000
                )
                anyRegistered = anyRegistered || ok
            }
        }

        if (settings.shakeToSkipEnabled) {
            val shakeSensor = if (useWakeUp) {
                wakeUpAccelerometer ?: defaultAccelerometer
            } else {
                defaultAccelerometer ?: defaultGravitySensor
            }
            if (shakeSensor != null) {
                val ok = manager.registerListener(
                    this,
                    shakeSensor,
                    SensorManager.SENSOR_DELAY_GAME,
                    40_000
                )
                anyRegistered = anyRegistered || ok
            }
        }

        sensorsRegistered = anyRegistered
        _activeMonitoringFlow.value = anyRegistered
    }

    private fun unregisterSensors() {
        if (sensorsRegistered) {
            runCatching { sensorManager?.unregisterListener(this) }
            sensorsRegistered = false
        }
        flipDetector.reset()
        shakeDetector.reset()
        _activeMonitoringFlow.value = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val safeEvent = event ?: return
        if (!sensorsRegistered || !isPlaying || gentleWakeRinging || isVoiceCallActive() || androidAutoConnected) {
            return
        }
        val values = safeEvent.values
        if (values.size < 3) return
        val nowMs = SystemClock.elapsedRealtime()
        val x = values[0]
        val y = values[1]
        val z = values[2]

        when (safeEvent.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                if (settings.flipToPauseEnabled && flipDetector.onSensorSample(x, y, z, nowMs)) {
                    Timber.i("MotionGestureController: flip-to-pause confirmed")
                    onFlipToPause()
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val useAccelForFlip = settings.flipToPauseEnabled && defaultGravitySensor == null
                if (useAccelForFlip && flipDetector.onSensorSample(x, y, z, nowMs)) {
                    Timber.i("MotionGestureController: flip-to-pause confirmed via accelerometer")
                    onFlipToPause()
                    return
                }
                if (settings.shakeToSkipEnabled &&
                    shakeDetector.onAccelerometerSample(x, y, z, nowMs, settings.shakeSensitivity)
                ) {
                    Timber.i("MotionGestureController: shake-to-skip confirmed (%s)", settings.shakeSensitivity.id)
                    onShakeToSkip()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private val _capabilityFlow = MutableStateFlow(MotionSensorCapability())
        val capabilityFlow: StateFlow<MotionSensorCapability> = _capabilityFlow.asStateFlow()

        private val _activeMonitoringFlow = MutableStateFlow(false)
        val activeMonitoringFlow: StateFlow<Boolean> = _activeMonitoringFlow.asStateFlow()

        fun inspectCapability(context: Context): MotionSensorCapability {
            val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val cap = MotionSensorCapability(
                hasGravitySensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) != null ||
                    sm?.getDefaultSensor(Sensor.TYPE_GRAVITY, true) != null,
                hasAccelerometer = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null ||
                    sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true) != null,
                wakeUpSensorSupported = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY, true) != null ||
                    sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true) != null
            )
            _capabilityFlow.value = cap
            return cap
        }
    }
}
