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
import kotlin.math.abs

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
    private var registeredFlipSensor: Sensor? = null
    private var registeredShakeSensor: Sensor? = null
    private var requestedFlipSensor: Sensor? = null
    private var requestedShakeSensor: Sensor? = null

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

    private fun canAcceptGestures(): Boolean = PlaybackPriorityCoordinator.canAcceptMotionGesture(
        motionEnabled = settings.hasActiveGesture && capability.isAvailable,
        isPlaying = isPlaying,
        gentleWakeRinging = gentleWakeRinging,
        inVoiceCall = isVoiceCallActive(),
        androidAutoConnected = androidAutoConnected
    )

    private fun reconcileRegistration() {
        val manager = sensorManager
        if (manager == null || !canAcceptGestures()) {
            unregisterSensors()
            return
        }

        val useWakeUp = !screenInteractive && capability.wakeUpSensorSupported
        val flipTarget = if (settings.flipToPauseEnabled) flipSensorFor(useWakeUp) else null
        val shakeAllowed = settings.shakeToSkipEnabled &&
            MotionGestureRegistrationPolicy.shakeAllowed(screenInteractive, settings.allowScreenOffWhenSupported)
        val shakeTarget = if (shakeAllowed) shakeSensorFor(useWakeUp) else null

        if (sensorsRegistered && flipTarget == requestedFlipSensor && shakeTarget == requestedShakeSensor) {
            return
        }

        val resumingMonitoring = sensorsRegistered
        if (sensorsRegistered) {
            runCatching { manager.unregisterListener(this) }
        }
        if (!resumingMonitoring) {
            flipDetector.reset()
            shakeDetector.reset()
        }

        val (flipSensor, shakeSensor) = registerTargets(manager, flipTarget, shakeTarget, shakeSensorFor(useWakeUp))
        requestedFlipSensor = flipTarget
        requestedShakeSensor = shakeTarget
        if (flipTarget != null && flipSensor != flipTarget) {
            Timber.w("MotionGestureController: flip sensor %s unavailable, using %s", flipTarget.name, flipSensor?.name)
        }
        if (shakeTarget != null && shakeSensor == null) Timber.w("MotionGestureController: shake sensor unavailable (%s)", shakeTarget.name)
        registeredFlipSensor = flipSensor
        registeredShakeSensor = shakeSensor
        val anyRegistered = flipSensor != null || shakeSensor != null

        sensorsRegistered = anyRegistered
        if (!anyRegistered) {
            flipDetector.reset()
            shakeDetector.reset()
        }
        _activeMonitoringFlow.value = anyRegistered
    }

    private fun registerTargets(
        manager: SensorManager,
        flipTarget: Sensor?,
        shakeTarget: Sensor?,
        accelerometer: Sensor?
    ): Pair<Sensor?, Sensor?> {
        if (flipTarget != null && flipTarget == shakeTarget) {
            val shared = flipTarget.takeIf { registerSensor(manager, it, SensorManager.SENSOR_DELAY_GAME, 40_000) }
            return shared to shared
        }
        val shakeSensor = shakeTarget?.takeIf { registerSensor(manager, it, SensorManager.SENSOR_DELAY_GAME, 40_000) }
        if (flipTarget == null) return null to shakeSensor
        if (registerSensor(manager, flipTarget, SensorManager.SENSOR_DELAY_UI, 80_000)) return flipTarget to shakeSensor
        val fallback = when {
            accelerometer == null || accelerometer == flipTarget -> null
            accelerometer == shakeSensor -> accelerometer
            registerSensor(manager, accelerometer, SensorManager.SENSOR_DELAY_UI, 80_000) -> accelerometer
            else -> null
        }
        return fallback to shakeSensor
    }

    private fun flipSensorFor(useWakeUp: Boolean): Sensor? =
        if (useWakeUp) {
            wakeUpGravitySensor ?: wakeUpAccelerometer ?: defaultGravitySensor ?: defaultAccelerometer
        } else {
            defaultGravitySensor ?: defaultAccelerometer
        }

    private fun shakeSensorFor(useWakeUp: Boolean): Sensor? =
        if (useWakeUp) {
            wakeUpAccelerometer ?: defaultAccelerometer
        } else {
            defaultAccelerometer
        }

    private fun registerSensor(
        manager: SensorManager,
        sensor: Sensor,
        samplingPeriod: Int,
        maxReportLatencyUs: Int
    ): Boolean = runCatching {
        val latencyUs = MotionGestureRegistrationPolicy.reportLatencyUs(sensor.fifoMaxEventCount, maxReportLatencyUs)
        manager.registerListener(this, sensor, samplingPeriod, latencyUs) ||
            (latencyUs > 0 && manager.registerListener(this, sensor, samplingPeriod))
    }.getOrDefault(false)

    private fun unregisterSensors() {
        if (sensorsRegistered) {
            runCatching { sensorManager?.unregisterListener(this) }
            sensorsRegistered = false
        }
        registeredFlipSensor = null
        registeredShakeSensor = null
        requestedFlipSensor = null
        requestedShakeSensor = null
        flipDetector.reset()
        shakeDetector.reset()
        _activeMonitoringFlow.value = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val safeEvent = event ?: return
        if (!sensorsRegistered || !isPlaying || gentleWakeRinging || androidAutoConnected) {
            return
        }
        val values = safeEvent.values
        if (values.size < 3) return
        val sensor = safeEvent.sensor
        val sampleMs = MotionGestureRegistrationPolicy.sampleTimeMs(safeEvent.timestamp, SystemClock.elapsedRealtime())
        val x = values[0]
        val y = values[1]
        val z = values[2]

        if (sensor == registeredFlipSensor && flipDetector.onSensorSample(x, y, z, sampleMs)) {
            if (!isVoiceCallActive()) {
                Timber.i("MotionGestureController: flip-to-pause confirmed (%s)", sensor.name)
                onFlipToPause()
            }
            return
        }
        if (sensor == registeredShakeSensor &&
            shakeDetector.onAccelerometerSample(x, y, z, sampleMs, settings.shakeSensitivity) &&
            !isVoiceCallActive()
        ) {
            Timber.i("MotionGestureController: shake-to-skip confirmed (%s)", settings.shakeSensitivity.id)
            onShakeToSkip()
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

internal object MotionGestureRegistrationPolicy {
    private const val MAX_CLOCK_SKEW_MS = 10_000L

    fun reportLatencyUs(fifoMaxEventCount: Int, requestedLatencyUs: Int): Int =
        if (fifoMaxEventCount > 0) requestedLatencyUs.coerceAtLeast(0) else 0

    fun shakeAllowed(screenInteractive: Boolean, allowScreenOff: Boolean): Boolean =
        screenInteractive || allowScreenOff

    fun sampleTimeMs(eventTimestampNs: Long, elapsedRealtimeMs: Long): Long {
        val eventMs = eventTimestampNs / 1_000_000L
        return if (eventMs > 0L && abs(elapsedRealtimeMs - eventMs) <= MAX_CLOCK_SKEW_MS) eventMs else elapsedRealtimeMs
    }
}
