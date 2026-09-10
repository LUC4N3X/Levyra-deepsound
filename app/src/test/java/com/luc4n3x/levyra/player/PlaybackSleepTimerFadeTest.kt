package com.luc4n3x.levyra.player

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSleepTimerFadeTest {

    private fun monotonicNow(): Long = System.nanoTime() / 1_000_000L

    @Test
    fun fadeVolumeRampsLinearlyAndIsBounded() {
        assertEquals(1f, sleepFadeVolume(remainingMs = 5_000L, fadeMs = 0L), 0f)
        assertEquals(1f, sleepFadeVolume(remainingMs = 30_000L, fadeMs = 20_000L), 0f)
        assertEquals(0.5f, sleepFadeVolume(remainingMs = 10_000L, fadeMs = 20_000L), 0.001f)
        assertEquals(0f, sleepFadeVolume(remainingMs = 0L, fadeMs = 20_000L), 0f)
        assertEquals(0f, sleepFadeVolume(remainingMs = -500L, fadeMs = 20_000L), 0f)
    }

    @Test
    fun cancellingDuringFadeRestoresFullVolume() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val volumes = CopyOnWriteArrayList<Float>()
        val timer = PlaybackSleepTimer(
            scope = scope,
            elapsedRealtime = ::monotonicNow,
            onFadeVolume = { volumes.add(it) }
        )

        timer.startCountdown(totalMs = 2_000L, fadeMs = 1_800L)
        withTimeout(5_000L) {
            while (volumes.isEmpty()) delay(10L)
        }
        timer.cancel()

        assertTrue(volumes.first() < 1f)
        assertEquals(1f, volumes.last(), 0f)
        assertEquals(PlaybackSleepTimerState.Disabled, timer.state.value)
        scope.cancel()
        Unit
    }

    @Test
    fun expiryRestoresVolumeBeforePausing() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val volumes = CopyOnWriteArrayList<Float>()
        val expiredVolume = CopyOnWriteArrayList<Float>()
        val expiries = AtomicInteger(0)
        val timer = PlaybackSleepTimer(
            scope = scope,
            elapsedRealtime = ::monotonicNow,
            onFadeVolume = { volumes.add(it) },
            onExpired = {
                expiries.incrementAndGet()
                expiredVolume.add(volumes.lastOrNull() ?: 1f)
            }
        )

        timer.startCountdown(totalMs = 400L, fadeMs = 300L)
        withTimeout(5_000L) {
            while (expiries.get() == 0) delay(10L)
        }

        assertEquals(1, expiries.get())
        assertEquals(1f, expiredVolume.first(), 0f)
        assertEquals(PlaybackSleepTimerState.Disabled, timer.state.value)
        scope.cancel()
        Unit
    }

    @Test
    fun countdownWithoutFadeNeverTouchesVolume() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val volumes = CopyOnWriteArrayList<Float>()
        val timer = PlaybackSleepTimer(
            scope = scope,
            elapsedRealtime = ::monotonicNow,
            onFadeVolume = { volumes.add(it) }
        )

        timer.startCountdown(totalMs = 200L)
        withTimeout(5_000L) {
            while (timer.state.value !is PlaybackSleepTimerState.Disabled) delay(10L)
        }
        timer.cancel()

        assertTrue(volumes.isEmpty())
        scope.cancel()
        Unit
    }

    @Test
    fun replacingATimerDuringFadeRestoresVolumeFirst() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val volumes = CopyOnWriteArrayList<Float>()
        val timer = PlaybackSleepTimer(
            scope = scope,
            elapsedRealtime = ::monotonicNow,
            onFadeVolume = { volumes.add(it) }
        )

        timer.startCountdown(totalMs = 2_000L, fadeMs = 1_800L)
        withTimeout(5_000L) {
            while (volumes.isEmpty()) delay(10L)
        }
        timer.startCountdown(totalMs = 60_000L)

        assertEquals(1f, volumes.last(), 0f)
        assertTrue(timer.state.value is PlaybackSleepTimerState.Countdown)
        timer.cancel()
        scope.cancel()
        Unit
    }
}
