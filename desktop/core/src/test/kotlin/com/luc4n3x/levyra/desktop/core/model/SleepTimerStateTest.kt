package com.luc4n3x.levyra.desktop.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerStateTest {

    @Test
    fun durationTimerExpiresOnlyAtTheDeadline() {
        val start = 1_000_000L
        val timer = SleepTimerState.forMinutes(30, start)

        assertEquals(SleepTimerMode.DURATION, timer.mode)
        assertEquals(30, timer.requestedMinutes)
        assertFalse(timer.expired(start))
        assertFalse(timer.expired(start + 29 * SleepTimerState.MINUTE_MS))
        assertTrue(timer.expired(start + 30 * SleepTimerState.MINUTE_MS))
    }

    @Test
    fun remainingTimeIsClampedAndRoundedUpToWholeMinutes() {
        val start = 0L
        val timer = SleepTimerState.forMinutes(15, start)

        assertEquals(15 * SleepTimerState.MINUTE_MS, timer.remainingMs(start))
        assertEquals(15, timer.remainingMinutes(start))
        assertEquals(14, timer.remainingMinutes(start + SleepTimerState.MINUTE_MS))
        assertEquals(1, timer.remainingMinutes(start + 14 * SleepTimerState.MINUTE_MS + 1L))
        assertEquals(0L, timer.remainingMs(start + 20 * SleepTimerState.MINUTE_MS))
        assertEquals(0, timer.remainingMinutes(start + 20 * SleepTimerState.MINUTE_MS))
    }

    @Test
    fun requestedMinutesAreKeptInsideTheSupportedRange() {
        assertEquals(1, SleepTimerState.forMinutes(0, 0L).requestedMinutes)
        assertEquals(1, SleepTimerState.forMinutes(-5, 0L).requestedMinutes)
        assertEquals(600, SleepTimerState.forMinutes(5_000, 0L).requestedMinutes)
    }

    @Test
    fun endOfTrackTimerIsActiveButNeverExpiresOnTime() {
        val timer = SleepTimerState.endOfTrack()

        assertTrue(timer.active)
        assertEquals(SleepTimerMode.END_OF_TRACK, timer.mode)
        assertFalse(timer.expired(Long.MAX_VALUE))
        assertEquals(0L, timer.remainingMs(Long.MAX_VALUE))
    }

    @Test
    fun disabledTimerIsInactive() {
        val timer = SleepTimerState()

        assertFalse(timer.active)
        assertFalse(timer.expired(Long.MAX_VALUE))
    }
}
