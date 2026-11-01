package com.luc4n3x.levyra.domain

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.luc4n3x.levyra.player.PlaybackSleepTimer
import com.luc4n3x.levyra.player.SleepTimbreAudioProcessor
import com.luc4n3x.levyra.ui.i18n.smartListeningCopyForCode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartListeningTest {

    @Test
    fun flipGestureDetector_confirmsStableFaceDownAfterDelay_andNeverAutoResumesOnFlipUp() {
        val detector = FlipGestureDetector(confirmationDelayMs = 800L)
        var t = 1_000L

        assertFalse(detector.onSensorSample(0f, 0f, -9.8f, t))

        repeat(15) {
            t += 60L
            assertFalse(detector.onSensorSample(0f, 0f, 9.8f, t))
        }

        val flippedAt = t
        var triggeredAt = -1L
        while (t - flippedAt < 2_500L) {
            t += 50L
            val x = if ((t / 50L) % 2L == 0L) 0.1f else -0.1f
            if (detector.onSensorSample(x, 0.1f, -9.8f, t)) {
                assertEquals(-1L, triggeredAt)
                triggeredAt = t
            }
        }
        assertTrue(triggeredAt - flippedAt >= 800L)
        assertTrue(triggeredAt - flippedAt <= 1_600L)

        repeat(15) {
            t += 60L
            assertFalse(detector.onSensorSample(0f, 0f, 9.8f, t))
        }
    }

    @Test
    fun flipGestureDetector_rejectsBriefOrUnstableMovement() {
        val detector = FlipGestureDetector(confirmationDelayMs = 800L)
        var t = 1_000L

        repeat(6) {
            t += 50L
            detector.onSensorSample(0f, 0f, 9.8f, t)
        }
        repeat(6) {
            t += 50L
            detector.onSensorSample(0f, 0f, -9.8f, t)
        }

        t += 400L
        assertFalse(detector.onSensorSample(0f, 0f, -9.8f, t))

        t += 100L
        assertFalse(detector.onSensorSample(6.5f, 5.2f, -3.0f, t))

        repeat(6) {
            t += 50L
            assertFalse(detector.onSensorSample(0f, 0f, -9.8f, t))
        }
    }

    @Test
    fun shakeGestureDetector_recognizesIntentionalShake_filtersWalking_andEnforcesCooldown() {
        val detector = ShakeGestureDetector(cooldownMs = 2_500L)
        var t = 2_000L

        repeat(20) { step ->
            t += 220L
            val walkX = if (step % 2 == 0) 4.2f else -3.8f
            assertFalse(
                detector.onAccelerometerSample(
                    x = walkX,
                    y = 1.5f,
                    z = 9.8f,
                    timestampMs = t,
                    sensitivity = MotionShakeSensitivity.NORMAL
                )
            )
        }

        detector.reset()
        t = 10_000L
        detector.onAccelerometerSample(0f, 0f, 9.8f, t, MotionShakeSensitivity.NORMAL)
        t += 90L
        assertFalse(detector.onAccelerometerSample(22.5f, 2f, 9.8f, t, MotionShakeSensitivity.NORMAL))
        t += 95L
        assertFalse(detector.onAccelerometerSample(-23.0f, -2f, 9.8f, t, MotionShakeSensitivity.NORMAL))
        t += 95L
        assertTrue(detector.onAccelerometerSample(23.2f, 1.5f, 9.8f, t, MotionShakeSensitivity.NORMAL))

        t += 90L
        assertFalse(detector.onAccelerometerSample(-24f, 0f, 9.8f, t, MotionShakeSensitivity.NORMAL))
        t += 90L
        assertFalse(detector.onAccelerometerSample(24f, 0f, 9.8f, t, MotionShakeSensitivity.NORMAL))
        t += 90L
        assertFalse(detector.onAccelerometerSample(-24f, 0f, 9.8f, t, MotionShakeSensitivity.NORMAL))

        t += 2_600L
        assertFalse(detector.onAccelerometerSample(22.8f, 1f, 9.8f, t, MotionShakeSensitivity.NORMAL))
        t += 95L
        assertFalse(detector.onAccelerometerSample(-23.4f, -1f, 9.8f, t, MotionShakeSensitivity.NORMAL))
        t += 95L
        assertTrue(detector.onAccelerometerSample(23.1f, 1f, 9.8f, t, MotionShakeSensitivity.NORMAL))
    }

    @Test
    fun gentleWakeCurve_isSmoothMonotonicAndRespectsMaxCap() {
        val maxCap = 0.78f
        val totalMs = 180_000L
        val start = GentleWakeCurve.volumeAt(0L, totalMs, maxCap)
        val quarter = GentleWakeCurve.volumeAt(45_000L, totalMs, maxCap)
        val half = GentleWakeCurve.volumeAt(90_000L, totalMs, maxCap)
        val threeQuarters = GentleWakeCurve.volumeAt(135_000L, totalMs, maxCap)
        val end = GentleWakeCurve.volumeAt(180_000L, totalMs, maxCap)

        assertEquals(GentleWakeCurve.INITIAL_VOLUME_FRACTION, start, 0.001f)
        assertTrue(quarter > start)
        assertTrue(half > quarter)
        assertTrue(threeQuarters > half)
        assertTrue(end > threeQuarters)
        assertEquals(maxCap, end, 0.001f)
    }

    @Test
    fun gentleWakeScheduleCalculator_handlesWeekdaysSnoozeAndDismissedToday() {
        val zone = ZoneId.of("Europe/Rome")
        val mondayMorning = ZonedDateTime.of(2026, 5, 4, 6, 30, 0, 0, zone)

        val settings = GentleWakeSettings(
            enabled = true,
            hour = 7,
            minute = 15,
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )

        val triggerToday = GentleWakeScheduleCalculator.nextTriggerAt(settings, mondayMorning)
        val expectedToday = ZonedDateTime.of(2026, 5, 4, 7, 15, 0, 0, zone)
        assertEquals(expectedToday, triggerToday)

        val dismissedTodaySettings = settings.copy(dismissedDateIso = "2026-05-04")
        val triggerWednesday = GentleWakeScheduleCalculator.nextTriggerAt(dismissedTodaySettings, mondayMorning)
        val expectedWednesday = ZonedDateTime.of(2026, 5, 6, 7, 15, 0, 0, zone)
        assertEquals(expectedWednesday, triggerWednesday)

        val snoozeTargetMs = mondayMorning.toInstant().toEpochMilli() + 5 * 60_000L
        val snoozedTrigger = GentleWakeScheduleCalculator.nextTriggerAt(settings, mondayMorning, snoozeTargetMs)
        assertEquals(snoozeTargetMs, snoozedTrigger?.toInstant()?.toEpochMilli())
    }

    @Test
    fun gentleWakeReliabilityStatus_neverMarksAlarmReliableWhenPermissionsAreMissing() {
        val missingExactAlarm = GentleWakeReliabilityStatus(
            exactAlarmPermitted = false,
            notificationsPermitted = true
        )
        assertFalse(missingExactAlarm.isReliable)

        val missingNotifications = GentleWakeReliabilityStatus(
            exactAlarmPermitted = true,
            notificationsPermitted = false
        )
        assertFalse(missingNotifications.isReliable)

        val allGranted = GentleWakeReliabilityStatus(
            exactAlarmPermitted = true,
            notificationsPermitted = true
        )
        assertTrue(allGranted.isReliable)
    }

    @Test
    fun sleepWithMusicCurve_isNonLinearAndSoftensTimbreBeforeVolumeFade() {
        val fadeMs = 240_000L
        val midFade = SleepWithMusicCurve.volumeAt(fadeMs / 2L, fadeMs)
        assertTrue(abs(midFade - 0.5f) > 0.05f)
        assertEquals(1f, SleepWithMusicCurve.volumeAt(fadeMs, fadeMs), 0.001f)
        assertEquals(0f, SleepWithMusicCurve.volumeAt(0L, fadeMs), 0.001f)
        assertTrue(SleepWithMusicCurve.timbreSoftnessAt(fadeMs, fadeMs, enabled = true) > 0f)
        assertEquals(0f, SleepWithMusicCurve.timbreSoftnessAt(fadeMs / 2L, fadeMs, enabled = false), 0f)
        assertEquals(240_000L, SleepWithMusicCurve.fadeWindowMsForTotal(600_000L))
    }

    @Test
    fun sleepWithMusicTimer_softensTimbre_andRestoresOnCancel() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val volumes = CopyOnWriteArrayList<Float>()
        val softnesses = CopyOnWriteArrayList<Float>()
        val expiries = AtomicInteger(0)
        val timer = PlaybackSleepTimer(
            scope = scope,
            elapsedRealtime = { System.nanoTime() / 1_000_000L },
            onFadeVolume = { volumes.add(it) },
            onTimbreSoftness = { softnesses.add(it) },
            onExpiredWithMode = { expiries.incrementAndGet() }
        )

        timer.startCountdown(
            totalMs = 3_000L,
            fadeMs = 1_800L,
            mode = PlaybackSleepTimerMode.SLEEP_WITH_MUSIC,
            timbreSoftening = true
        )
        withTimeout(5_000L) {
            while (volumes.none { it < 1f } || softnesses.none { it > 0.1f }) delay(10L)
        }
        assertTrue(timer.currentFadeVolume() < 1f)

        timer.cancel()
        assertEquals(1f, timer.currentFadeVolume(), 0.001f)
        assertEquals(1f, volumes.last(), 0.001f)
        assertEquals(0f, softnesses.last(), 0.001f)
        assertEquals(0, expiries.get())
        scope.cancel()
        Unit
    }

    @Test
    fun sleepTimbreAudioProcessor_softensHighFrequencySignalWithoutArtifacts_andBypassesWhenZero() {
        val processor = SleepTimbreAudioProcessor()
        val format = AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        processor.setTargetSoftness(0f)
        val bypassInput = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder())
        repeat(8) { index ->
            bypassInput.putShort(if (index % 2 == 0) 12000 else -12000)
        }
        bypassInput.flip()
        processor.queueInput(bypassInput)
        val bypassOut = processor.output
        assertEquals(16, bypassOut.remaining())
        assertEquals(12000.toShort(), bypassOut.short)

        processor.flush()
        processor.setTargetSoftness(1f)
        val frameCount = 8_192
        val hfInput = ByteBuffer.allocateDirect(frameCount * 4).order(ByteOrder.nativeOrder())
        for (frame in 0 until frameCount) {
            val sample: Short = if (frame % 2 == 0) 16000 else -16000
            hfInput.putShort(sample)
            hfInput.putShort(sample)
        }
        hfInput.flip()
        processor.queueInput(hfInput)
        val filteredOut = processor.output
        var peakAfterWarmup = 0
        for (frame in 0 until frameCount) {
            val left = filteredOut.short.toInt()
            val right = filteredOut.short.toInt()
            if (frame > frameCount - 1_024) {
                peakAfterWarmup = maxOf(peakAfterWarmup, abs(left), abs(right))
            }
        }
        assertTrue(peakAfterWarmup in 100..12000)
    }

    @Test
    fun morningResumeSnapshot_roundTripsAndLinksToGentleWake() {
        val track = Track(
            id = "track-1",
            title = "Midnight Horizon",
            artist = "Levyra Ensemble",
            album = "Nocturnes",
            durationMs = 252_000L,
            streamUrl = "https://example.com/audio.mp3",
            videoUrl = "",
            thumbnailUrl = "https://example.com/cover.jpg",
            largeThumbnailUrl = "https://example.com/cover.jpg",
            source = "YouTube",
            moodTags = emptySet(),
            energy = 40,
            vocal = 30,
            replayScore = 80,
            cacheScore = 80,
            accentStart = 0xFF00E5FF.toInt(),
            accentEnd = 0xFF7B61FF.toInt()
        )
        val snapshot = MorningResumeSnapshot(
            track = track,
            positionMs = 94_500L,
            queue = listOf(track),
            queueIndex = 0,
            shuffleEnabled = true,
            repeatMode = RepeatMode.All,
            isLiveRadio = false,
            radioStationUuid = "",
            capturedAtEpochMs = 1_700_000_000_000L,
            promptAtEpochMs = 1_700_028_800_000L,
            dismissed = false
        )
        val encoded = snapshot.toJson().toString()
        val decoded = MorningResumeSnapshot.fromJson(JSONObject(encoded))
        assertNotNull(decoded)
        assertEquals(snapshot.track.id, decoded?.track?.id)
        assertEquals(snapshot.positionMs, decoded?.positionMs)
        assertEquals(snapshot.shuffleEnabled, decoded?.shuffleEnabled)
        assertEquals(snapshot.repeatMode, decoded?.repeatMode)

        val longQueue = (0 until 100).map { index -> track.copy(id = "track-$index", title = "Track $index") }
        val deepSnapshot = snapshot.copy(track = longQueue[80], queue = longQueue, queueIndex = 80)
        val deepDecoded = MorningResumeSnapshot.fromJson(JSONObject(deepSnapshot.toJson().toString()))
        assertNotNull(deepDecoded)
        assertEquals(60, deepDecoded?.queue?.size)
        assertEquals("track-80", deepDecoded?.queue?.get(deepDecoded.queueIndex)?.id)

        assertFalse(
            PlaybackPriorityCoordinator.canAcceptMotionGesture(
                motionEnabled = true,
                isPlaying = true,
                gentleWakeRinging = true,
                inVoiceCall = false,
                androidAutoConnected = false
            )
        )
        assertFalse(PlaybackPriorityCoordinator.canStartSleepFade(gentleWakeRinging = true))
    }

    @Test
    fun smartListeningTranslations_coverAll41CatalogLanguagesPlusPtBr() {
        val codes = LevyraLanguageCatalog.languages.map { it.code } + "pt-BR"
        assertTrue(LevyraLanguageCatalog.languages.size >= 41)
        for (code in codes) {
            val copy = smartListeningCopyForCode(code)
            assertTrue("hubTitle blank for $code", copy.hubTitle.isNotBlank())
            assertTrue("motionSectionTitle blank for $code", copy.motionSectionTitle.isNotBlank())
            assertTrue("flipToPauseTitle blank for $code", copy.flipToPauseTitle.isNotBlank())
            assertTrue("shakeToSkipTitle blank for $code", copy.shakeToSkipTitle.isNotBlank())
            assertTrue("gentleWakeTitle blank for $code", copy.gentleWakeTitle.isNotBlank())
            assertTrue("snoozeAction blank for $code", copy.snoozeAction.isNotBlank())
            assertTrue("sleepWithMusicTitle blank for $code", copy.sleepWithMusicTitle.isNotBlank())
            assertTrue("morningPromptTitle blank for $code", copy.morningPromptTitle.isNotBlank())
            assertTrue("morningPromptActionResume blank for $code", copy.morningPromptActionResume.isNotBlank())
            assertTrue("morningPromptActionLater blank for $code", copy.morningPromptActionLater.isNotBlank())
        }
    }
}
