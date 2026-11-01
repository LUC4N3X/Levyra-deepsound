package com.luc4n3x.levyra.domain

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.data.TrackJson
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONObject

val DEFAULT_GENTLE_WAKE_DAYS: Set<DayOfWeek> = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY
)

val GENTLE_WAKE_CRESCENDO_MINUTES_OPTIONS: List<Int> = listOf(1, 3, 5, 10)
val SLEEP_WITH_MUSIC_DURATION_PRESETS: List<Int> = listOf(5, 10, 15, 20, 30)

enum class MotionShakeSensitivity(
    val id: String,
    val linearAccelerationThresholdMpS2: Float,
    val requiredReversals: Int
) {
    LOW(id = "low", linearAccelerationThresholdMpS2 = 15.5f, requiredReversals = 3),
    NORMAL(id = "normal", linearAccelerationThresholdMpS2 = 12.2f, requiredReversals = 3),
    HIGH(id = "high", linearAccelerationThresholdMpS2 = 9.5f, requiredReversals = 2);

    companion object {
        fun from(raw: String?): MotionShakeSensitivity =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) } ?: NORMAL
    }
}

@Immutable
data class MotionControlSettings(
    val enabled: Boolean = false,
    val flipToPauseEnabled: Boolean = true,
    val shakeToSkipEnabled: Boolean = true,
    val shakeSensitivity: MotionShakeSensitivity = MotionShakeSensitivity.NORMAL,
    val allowScreenOffWhenSupported: Boolean = false,
    val flipConfirmationDelayMs: Long = DEFAULT_FLIP_CONFIRMATION_DELAY_MS
) {
    fun normalized(): MotionControlSettings = copy(
        flipConfirmationDelayMs = flipConfirmationDelayMs.coerceIn(400L, 1_500L)
    )

    val hasActiveGesture: Boolean
        get() = enabled && (flipToPauseEnabled || shakeToSkipEnabled)

    companion object {
        const val DEFAULT_FLIP_CONFIRMATION_DELAY_MS: Long = 800L
        const val DEFAULT_SHAKE_COOLDOWN_MS: Long = 2_500L
    }
}

@Immutable
data class MotionSensorCapability(
    val hasGravitySensor: Boolean = false,
    val hasAccelerometer: Boolean = false,
    val wakeUpSensorSupported: Boolean = false
) {
    val canDetectFlip: Boolean
        get() = hasGravitySensor || hasAccelerometer

    val canDetectShake: Boolean
        get() = hasAccelerometer || hasGravitySensor

    val isAvailable: Boolean
        get() = canDetectFlip || canDetectShake
}

class FlipGestureDetector(
    private val confirmationDelayMs: Long = MotionControlSettings.DEFAULT_FLIP_CONFIRMATION_DELAY_MS
) {
    private var smoothedX = 0f
    private var smoothedY = 0f
    private var smoothedZ = 0f
    private var initialized = false
    private var armedByNonFaceDown = false
    private var triggeredForCurrentFaceDown = false
    private var faceDownCandidateSinceMs: Long = -1L

    fun reset() {
        smoothedX = 0f
        smoothedY = 0f
        smoothedZ = 0f
        initialized = false
        armedByNonFaceDown = false
        triggeredForCurrentFaceDown = false
        faceDownCandidateSinceMs = -1L
    }

    fun onSensorSample(
        x: Float,
        y: Float,
        z: Float,
        timestampMs: Long
    ): Boolean {
        if (!initialized) {
            smoothedX = x
            smoothedY = y
            smoothedZ = z
            initialized = true
        } else {
            val alpha = 0.28f
            smoothedX += alpha * (x - smoothedX)
            smoothedY += alpha * (y - smoothedY)
            smoothedZ += alpha * (z - smoothedZ)
        }

        val magnitude = sqrt(smoothedX * smoothedX + smoothedY * smoothedY + smoothedZ * smoothedZ)
        if (magnitude !in 7.2f..12.2f) {
            faceDownCandidateSinceMs = -1L
            return false
        }

        val normalizedZ = smoothedZ / magnitude
        val lateralMagnitude = sqrt(smoothedX * smoothedX + smoothedY * smoothedY)
        val instantaneousDelta = sqrt(
            (x - smoothedX) * (x - smoothedX) +
                (y - smoothedY) * (y - smoothedY) +
                (z - smoothedZ) * (z - smoothedZ)
        )
        val isStable = instantaneousDelta <= 1.65f
        val isClearlyNotFaceDown = normalizedZ >= -0.35f || lateralMagnitude >= 5.2f

        if (isClearlyNotFaceDown) {
            armedByNonFaceDown = true
            triggeredForCurrentFaceDown = false
            faceDownCandidateSinceMs = -1L
            return false
        }

        val isFaceDownPose = normalizedZ <= -0.82f && lateralMagnitude <= 4.0f
        if (!isFaceDownPose || !isStable) {
            faceDownCandidateSinceMs = -1L
            return false
        }

        if (!armedByNonFaceDown || triggeredForCurrentFaceDown) {
            return false
        }

        if (faceDownCandidateSinceMs < 0L) {
            faceDownCandidateSinceMs = timestampMs
            return false
        }

        val elapsedMs = timestampMs - faceDownCandidateSinceMs
        if (elapsedMs >= confirmationDelayMs.coerceIn(400L, 1_500L)) {
            triggeredForCurrentFaceDown = true
            faceDownCandidateSinceMs = -1L
            return true
        }
        return false
    }
}

class ShakeGestureDetector(
    private val cooldownMs: Long = MotionControlSettings.DEFAULT_SHAKE_COOLDOWN_MS
) {
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    private var gravityInitialized = false
    private var reversalCount = 0
    private var lastReversalSign = 0
    private var firstReversalAtMs = -1L
    private var lastReversalAtMs = -1L
    private var lastTriggeredAtMs = -1L

    fun reset() {
        gravityX = 0f
        gravityY = 0f
        gravityZ = 0f
        gravityInitialized = false
        clearWindow()
    }

    private fun clearWindow() {
        reversalCount = 0
        lastReversalSign = 0
        firstReversalAtMs = -1L
        lastReversalAtMs = -1L
    }

    fun onAccelerometerSample(
        x: Float,
        y: Float,
        z: Float,
        timestampMs: Long,
        sensitivity: MotionShakeSensitivity
    ): Boolean {
        if (!gravityInitialized) {
            gravityX = x
            gravityY = y
            gravityZ = z
            gravityInitialized = true
            return false
        }

        val alpha = 0.82f
        gravityX = alpha * gravityX + (1f - alpha) * x
        gravityY = alpha * gravityY + (1f - alpha) * y
        gravityZ = alpha * gravityZ + (1f - alpha) * z

        val linearX = x - gravityX
        val linearY = y - gravityY
        val linearZ = z - gravityZ

        if (lastTriggeredAtMs >= 0L && timestampMs - lastTriggeredAtMs < cooldownMs) {
            return false
        }

        val horizontalMagnitude = sqrt(linearX * linearX + linearY * linearY)
        val totalMagnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)

        if (lastReversalAtMs >= 0L && timestampMs - lastReversalAtMs > 280L) {
            clearWindow()
        }

        if (totalMagnitude < sensitivity.linearAccelerationThresholdMpS2 ||
            horizontalMagnitude < sensitivity.linearAccelerationThresholdMpS2 * 0.68f
        ) {
            return false
        }

        val dominantAxis = if (abs(linearX) >= abs(linearY)) linearX else linearY
        val sign = if (dominantAxis >= 0f) 1 else -1

        if (lastReversalSign == 0) {
            lastReversalSign = sign
            reversalCount = 1
            firstReversalAtMs = timestampMs
            lastReversalAtMs = timestampMs
            return false
        }

        val gapMs = timestampMs - lastReversalAtMs
        if (sign != lastReversalSign && gapMs in 45L..270L) {
            lastReversalSign = sign
            reversalCount += 1
            lastReversalAtMs = timestampMs

            val windowSpanMs = timestampMs - firstReversalAtMs
            if (reversalCount >= sensitivity.requiredReversals && windowSpanMs in 110L..720L) {
                lastTriggeredAtMs = timestampMs
                clearWindow()
                return true
            }
        } else if (gapMs > 270L) {
            lastReversalSign = sign
            reversalCount = 1
            firstReversalAtMs = timestampMs
            lastReversalAtMs = timestampMs
        }
        return false
    }
}

enum class GentleWakeSourceType(val id: String) {
    PLAYLIST("playlist"),
    ALBUM("album"),
    RADIO("radio"),
    LOCAL_AUDIO("local_audio"),
    BUILT_IN_TONE("built_in_tone");

    companion object {
        fun from(raw: String?): GentleWakeSourceType =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) } ?: BUILT_IN_TONE
    }
}

@Immutable
data class GentleWakeSettings(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 30,
    val days: Set<DayOfWeek> = DEFAULT_GENTLE_WAKE_DAYS,
    val sourceType: GentleWakeSourceType = GentleWakeSourceType.BUILT_IN_TONE,
    val sourceId: String = "",
    val sourceTitle: String = "",
    val sourceSubtitle: String = "",
    val sourceArtworkUrl: String = "",
    val sourcePayloadJson: String = "",
    val crescendoMinutes: Int = 3,
    val maxVolumeFraction: Float = 0.80f,
    val snoozeMinutes: Int = 5,
    val dismissedDateIso: String = ""
) {
    val minuteOfDay: Int
        get() = hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59)

    fun normalized(): GentleWakeSettings = copy(
        hour = hour.coerceIn(0, 23),
        minute = minute.coerceIn(0, 59),
        days = if (days.isEmpty()) DEFAULT_GENTLE_WAKE_DAYS else days,
        crescendoMinutes = GENTLE_WAKE_CRESCENDO_MINUTES_OPTIONS.minByOrNull { abs(it - crescendoMinutes) } ?: 3,
        maxVolumeFraction = maxVolumeFraction.coerceIn(0.20f, 1.0f),
        snoozeMinutes = snoozeMinutes.coerceIn(1, 15)
    )
}

@Immutable
data class GentleWakeReliabilityStatus(
    val exactAlarmPermitted: Boolean = true,
    val notificationsPermitted: Boolean = true
) {
    val isReliable: Boolean
        get() = exactAlarmPermitted && notificationsPermitted
}

@Immutable
data class GentleWakeRuntimeState(
    val active: Boolean = false,
    val snoozedUntilEpochMs: Long = 0L,
    val currentVolumeFraction: Float = 0f,
    val targetVolumeFraction: Float = 0.80f,
    val crescendoDurationMs: Long = 180_000L,
    val elapsedMs: Long = 0L,
    val sourceTitle: String = "",
    val usingEmergencyFallback: Boolean = false,
    val previewActive: Boolean = false
)

object GentleWakeCurve {
    const val INITIAL_VOLUME_FRACTION: Float = 0.025f

    fun volumeAt(
        elapsedMs: Long,
        totalDurationMs: Long,
        maxVolumeFraction: Float
    ): Float {
        val cappedMax = maxVolumeFraction.coerceIn(0.15f, 1.0f)
        val start = INITIAL_VOLUME_FRACTION.coerceAtMost(cappedMax * 0.25f)
        if (totalDurationMs <= 0L) return cappedMax
        val progress = (elapsedMs.coerceAtLeast(0L).toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
        val smoothStep = progress * progress * (3f - 2f * progress)
        val perceptual = smoothStep.pow(1.35f)
        return (start + (cappedMax - start) * perceptual).coerceIn(start, cappedMax)
    }

    fun snoozeFadeOutVolume(
        startVolume: Float,
        elapsedMs: Long,
        fadeDurationMs: Long
    ): Float {
        val safeStart = startVolume.coerceIn(0f, 1f)
        if (fadeDurationMs <= 0L) return 0f
        val progress = (elapsedMs.coerceAtLeast(0L).toFloat() / fadeDurationMs.toFloat()).coerceIn(0f, 1f)
        val cosine = 0.5f * (1f + cos(PI.toFloat() * progress))
        return (safeStart * cosine).coerceIn(0f, safeStart)
    }
}

object GentleWakeScheduleCalculator {
    fun nextTriggerAt(
        settings: GentleWakeSettings,
        now: ZonedDateTime,
        snoozeUntilEpochMs: Long = 0L
    ): ZonedDateTime? {
        val normalized = settings.normalized()
        if (snoozeUntilEpochMs > now.toInstant().toEpochMilli()) {
            return ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(snoozeUntilEpochMs),
                now.zone
            )
        }
        if (!normalized.enabled || normalized.days.isEmpty()) return null
        val targetTime = LocalTime.of(normalized.hour, normalized.minute)
        val dismissedDate = normalized.dismissedDateIso
            .takeIf { it.isNotBlank() }
            ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }

        for (dayOffset in 0..7) {
            val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())
            if (candidateDate.dayOfWeek !in normalized.days) continue
            if (dismissedDate != null && candidateDate == dismissedDate) continue
            val candidate = ZonedDateTime.of(candidateDate, targetTime, now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }
}

enum class PlaybackSleepTimerMode(val id: String) {
    CLASSIC("classic"),
    SLEEP_WITH_MUSIC("sleep_with_music");

    companion object {
        fun from(raw: String?): PlaybackSleepTimerMode =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) } ?: CLASSIC
    }
}

@Immutable
data class SleepWithMusicSettings(
    val preferSmartSleepMode: Boolean = false,
    val defaultDurationMinutes: Int = 15,
    val timbreSofteningEnabled: Boolean = true,
    val morningResumeEnabled: Boolean = true,
    val morningPromptHour: Int = 8,
    val morningPromptMinute: Int = 0,
    val linkMorningPromptToGentleWake: Boolean = true
) {
    fun normalized(): SleepWithMusicSettings = copy(
        defaultDurationMinutes = defaultDurationMinutes.coerceIn(5, 180),
        morningPromptHour = morningPromptHour.coerceIn(0, 23),
        morningPromptMinute = morningPromptMinute.coerceIn(0, 59)
    )
}

object SleepWithMusicCurve {
    fun fadeWindowMsForTotal(totalMs: Long): Long {
        if (totalMs <= 0L) return 0L
        val proportional = (totalMs * 2L) / 5L
        return proportional.coerceIn(15_000L.coerceAtMost(totalMs), 360_000L.coerceAtMost(totalMs))
    }

    fun volumeAt(
        remainingMs: Long,
        fadeWindowMs: Long
    ): Float {
        if (remainingMs <= 0L) return 0f
        if (fadeWindowMs <= 0L || remainingMs >= fadeWindowMs) return 1f
        val progressIntoFade = 1f - (remainingMs.toFloat() / fadeWindowMs.toFloat()).coerceIn(0f, 1f)
        val cosine = 0.5f * (1f + cos(PI.toFloat() * progressIntoFade))
        return cosine.pow(1.55f).coerceIn(0f, 1f)
    }

    fun timbreSoftnessAt(
        remainingMs: Long,
        fadeWindowMs: Long,
        enabled: Boolean
    ): Float {
        if (!enabled || fadeWindowMs <= 0L) return 0f
        if (remainingMs <= 0L) return 1f
        val timbreWindowMs = (fadeWindowMs * 3L / 2L).coerceAtLeast(fadeWindowMs)
        if (remainingMs >= timbreWindowMs) return 0f
        val progress = 1f - (remainingMs.toFloat() / timbreWindowMs.toFloat()).coerceIn(0f, 1f)
        return (progress * progress * (3f - 2f * progress)).coerceIn(0f, 1f)
    }
}

@Immutable
data class MorningResumeSnapshot(
    val track: Track,
    val positionMs: Long,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val isLiveRadio: Boolean = false,
    val radioStationUuid: String = "",
    val capturedAtEpochMs: Long = 0L,
    val promptAtEpochMs: Long = 0L,
    val dismissed: Boolean = false
) {
    val canRestoreExactPosition: Boolean
        get() = !isLiveRadio && positionMs > 0L && (track.durationMs <= 0L || positionMs < track.durationMs)

    fun isReadyToPrompt(nowEpochMs: Long): Boolean =
        !dismissed && track.title.isNotBlank() && (promptAtEpochMs <= 0L || nowEpochMs >= promptAtEpochMs)

    private val queueWindowStart: Int
        get() = (queueIndex - QUEUE_WINDOW_SIZE / 2)
            .coerceAtMost(queue.size - QUEUE_WINDOW_SIZE)
            .coerceAtLeast(0)

    fun toJson(): JSONObject = JSONObject()
        .put("track", TrackJson.toJson(track))
        .put("positionMs", positionMs.coerceAtLeast(0L))
        .put(
            "queue",
            JSONArray().apply {
                queue.drop(queueWindowStart).take(QUEUE_WINDOW_SIZE).forEach { item -> put(TrackJson.toJson(item)) }
            }
        )
        .put("queueIndex", (queueIndex - queueWindowStart).coerceAtLeast(0))
        .put("shuffleEnabled", shuffleEnabled)
        .put("repeatMode", repeatMode.name)
        .put("isLiveRadio", isLiveRadio)
        .put("radioStationUuid", radioStationUuid)
        .put("capturedAtEpochMs", capturedAtEpochMs)
        .put("promptAtEpochMs", promptAtEpochMs)
        .put("dismissed", dismissed)

    companion object {
        private const val QUEUE_WINDOW_SIZE = 60

        fun fromJson(json: JSONObject?): MorningResumeSnapshot? {
            if (json == null) return null
            val trackObj = json.optJSONObject("track") ?: return null
            val track = runCatching { TrackJson.fromJson(trackObj) }.getOrNull() ?: return null
            if (track.id.isBlank() && track.title.isBlank()) return null
            val queueArray = json.optJSONArray("queue")
            val decodedQueue = buildList {
                if (queueArray != null) {
                    for (index in 0 until queueArray.length()) {
                        val itemObj = queueArray.optJSONObject(index) ?: continue
                        runCatching { TrackJson.fromJson(itemObj) }.getOrNull()?.let(::add)
                    }
                }
            }.ifEmpty { listOf(track) }
            val safeQueueIndex = json.optInt("queueIndex", 0).coerceIn(0, (decodedQueue.size - 1).coerceAtLeast(0))
            val repeatMode = runCatching {
                RepeatMode.valueOf(json.optString("repeatMode", RepeatMode.Off.name))
            }.getOrDefault(RepeatMode.Off)
            return MorningResumeSnapshot(
                track = track,
                positionMs = json.optLong("positionMs", 0L).coerceAtLeast(0L),
                queue = decodedQueue,
                queueIndex = safeQueueIndex,
                shuffleEnabled = json.optBoolean("shuffleEnabled", false),
                repeatMode = repeatMode,
                isLiveRadio = json.optBoolean("isLiveRadio", track.isLiveRadio()),
                radioStationUuid = json.optString("radioStationUuid", ""),
                capturedAtEpochMs = json.optLong("capturedAtEpochMs", 0L),
                promptAtEpochMs = json.optLong("promptAtEpochMs", 0L),
                dismissed = json.optBoolean("dismissed", false)
            )
        }

        fun computeMorningPromptTime(
            now: ZonedDateTime,
            sleepSettings: SleepWithMusicSettings,
            gentleWakeSettings: GentleWakeSettings
        ): ZonedDateTime {
            val normalizedSleep = sleepSettings.normalized()
            val normalizedWake = gentleWakeSettings.normalized()
            if (normalizedSleep.linkMorningPromptToGentleWake && normalizedWake.enabled) {
                val nextWake = GentleWakeScheduleCalculator.nextTriggerAt(normalizedWake, now)
                if (nextWake != null) return nextWake
            }
            val promptTime = LocalTime.of(normalizedSleep.morningPromptHour, normalizedSleep.morningPromptMinute)
            val todayCandidate = ZonedDateTime.of(now.toLocalDate(), promptTime, now.zone)
            return if (todayCandidate.isAfter(now.plusMinutes(15))) {
                todayCandidate
            } else {
                ZonedDateTime.of(now.toLocalDate().plusDays(1), promptTime, now.zone)
            }
        }
    }
}

enum class PlaybackControlOwner(val priority: Int) {
    GENTLE_WAKE_ALARM(priority = 4),
    MANUAL_USER_ACTION(priority = 3),
    SLEEP_FADE(priority = 2),
    MOTION_GESTURE(priority = 1)
}

object PlaybackPriorityCoordinator {
    fun canAcceptMotionGesture(
        motionEnabled: Boolean,
        isPlaying: Boolean,
        gentleWakeRinging: Boolean,
        inVoiceCall: Boolean,
        androidAutoConnected: Boolean
    ): Boolean = motionEnabled &&
        isPlaying &&
        !gentleWakeRinging &&
        !inVoiceCall &&
        !androidAutoConnected

    fun canStartSleepFade(gentleWakeRinging: Boolean): Boolean = !gentleWakeRinging

    fun effectivePlayerVolume(
        gentleWakeActive: Boolean,
        gentleWakeVolume: Float,
        sleepFadeVolume: Float
    ): Float = if (gentleWakeActive) {
        gentleWakeVolume.coerceIn(0f, 1f)
    } else {
        sleepFadeVolume.coerceIn(0f, 1f)
    }
}
