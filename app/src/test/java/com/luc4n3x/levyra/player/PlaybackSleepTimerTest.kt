package com.luc4n3x.levyra.player

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSleepTimerTest {

    private fun monotonicNow(): Long = System.nanoTime() / 1_000_000L

    private fun timer(
        scope: CoroutineScope,
        onExpired: () -> Unit = {}
    ): PlaybackSleepTimer = PlaybackSleepTimer(
        scope = scope,
        elapsedRealtime = ::monotonicNow,
        onExpired = onExpired
    )

    @Test
    fun thirtyMinuteCountdownBecomesActive() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val sleepTimer = timer(scope)

        sleepTimer.startCountdown(30 * 60_000L)

        val state = sleepTimer.state.value
        assertTrue(state is PlaybackSleepTimerState.Countdown)
        assertEquals(30 * 60_000L, (state as PlaybackSleepTimerState.Countdown).totalMs)
        scope.cancel()
        Unit
    }

    @Test
    fun replacingCountdownCancelsThePreviousOne() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pauseCount = AtomicInteger(0)
        val sleepTimer = timer(scope) { pauseCount.incrementAndGet() }

        sleepTimer.startCountdown(40L)
        sleepTimer.startCountdown(300L)
        withTimeout(5_000L) {
            delay(120L)
        }

        assertTrue(sleepTimer.state.value is PlaybackSleepTimerState.Countdown)
        assertEquals(300L, (sleepTimer.state.value as PlaybackSleepTimerState.Countdown).totalMs)
        assertEquals(0, pauseCount.get())

        withTimeout(5_000L) {
            while (sleepTimer.state.value !is PlaybackSleepTimerState.Disabled) delay(10L)
        }
        assertEquals(1, pauseCount.get())
        scope.cancel()
        Unit
    }

    @Test
    fun cancelClearsStateAndPreventsFutureCallbacks() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pauseCount = AtomicInteger(0)
        val sleepTimer = timer(scope) { pauseCount.incrementAndGet() }

        sleepTimer.startCountdown(30L)
        sleepTimer.cancel()
        withTimeout(5_000L) { delay(120L) }

        assertEquals(PlaybackSleepTimerState.Disabled, sleepTimer.state.value)
        assertEquals(0, pauseCount.get())
        scope.cancel()
        Unit
    }

    @Test
    fun expiryPausesExactlyOnce() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pauseCount = AtomicInteger(0)
        val sleepTimer = timer(scope) { pauseCount.incrementAndGet() }

        sleepTimer.startCountdown(20L)
        withTimeout(5_000L) {
            while (sleepTimer.state.value !is PlaybackSleepTimerState.Disabled) delay(5L)
        }
        delay(50L)

        assertEquals(1, pauseCount.get())
        assertEquals(PlaybackSleepTimerState.Disabled, sleepTimer.state.value)
        scope.cancel()
        Unit
    }

    @Test
    fun endOfTrackBoundaryConsumesExactlyOnce() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val sleepTimer = timer(scope)

        sleepTimer.startEndOfTrack()
        assertTrue(sleepTimer.isEndOfTrackActive())
        assertTrue(sleepTimer.consumeEndOfTrackBoundary())
        assertFalse(sleepTimer.isEndOfTrackActive())
        assertFalse(sleepTimer.consumeEndOfTrackBoundary())
        assertEquals(PlaybackSleepTimerState.Disabled, sleepTimer.state.value)
        scope.cancel()
    }

    @Test
    fun cancelledEndOfTrackNeverConsumes() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val sleepTimer = timer(scope)

        sleepTimer.startEndOfTrack()
        sleepTimer.cancel()

        assertFalse(sleepTimer.isEndOfTrackActive())
        assertFalse(sleepTimer.consumeEndOfTrackBoundary())
        scope.cancel()
    }

    @Test
    fun repeatOneBoundaryPausesWhenEndOfTrackArmed() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pauseCount = AtomicInteger(0)
        val sleepTimer = timer(scope) { pauseCount.incrementAndGet() }

        sleepTimer.startEndOfTrack()
        val consumed = sleepTimer.consumeEndOfTrackBoundary()
        if (consumed) pauseCount.incrementAndGet()

        assertEquals(1, pauseCount.get())
        assertEquals(PlaybackSleepTimerState.Disabled, sleepTimer.state.value)
        assertFalse(sleepTimer.consumeEndOfTrackBoundary())
        assertEquals(1, pauseCount.get())
        scope.cancel()
        Unit
    }

    @Test
    fun timeBasedTimerIsNotAffectedByTrackBoundary() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val sleepTimer = timer(scope)

        sleepTimer.startCountdown(5 * 60_000L)
        assertFalse(sleepTimer.consumeEndOfTrackBoundary())
        assertTrue(sleepTimer.state.value is PlaybackSleepTimerState.Countdown)
        scope.cancel()
    }

    @Test
    fun cancelLeavesNoActiveJobInOwnerScope() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val sleepTimer = timer(scope)

        sleepTimer.startCountdown(60_000L)
        sleepTimer.cancel()
        withTimeout(5_000L) {
            while (scope.coroutineContext[Job]?.children?.any { it.isActive } == true) delay(5L)
        }

        assertEquals(PlaybackSleepTimerState.Disabled, sleepTimer.state.value)
        assertTrue(scope.coroutineContext[Job]?.children?.none { it.isActive } == true)
        scope.cancel()
        Unit
    }
}
