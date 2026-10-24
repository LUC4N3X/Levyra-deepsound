package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerCustomDurationTest {

    @Test
    fun `slider values snap to five minute steps`() {
        assertEquals(90, snapSleepTimerMinutes(91.4f))
        assertEquals(95, snapSleepTimerMinutes(92.6f))
        assertEquals(20, snapSleepTimerMinutes(20f))
    }

    @Test
    fun `slider values stay inside the supported range`() {
        assertEquals(SLEEP_TIMER_CUSTOM_MIN_MINUTES, snapSleepTimerMinutes(0f))
        assertEquals(SLEEP_TIMER_CUSTOM_MAX_MINUTES, snapSleepTimerMinutes(500f))
    }

    @Test
    fun `preset durations are not custom`() {
        listOf(15, 30, 45, 60).forEach { assertFalse(isCustomSleepTimerMinutes(it)) }
        assertFalse(isCustomSleepTimerMinutes(0))
    }

    @Test
    fun `any other duration is custom`() {
        assertTrue(isCustomSleepTimerMinutes(5))
        assertTrue(isCustomSleepTimerMinutes(90))
        assertTrue(isCustomSleepTimerMinutes(240))
    }

    @Test
    fun `timers extended past the slider range are not shown as slider values`() {
        assertFalse(isCustomSleepTimerMinutes(255))
        assertFalse(isCustomSleepTimerMinutes(3))
    }
}
