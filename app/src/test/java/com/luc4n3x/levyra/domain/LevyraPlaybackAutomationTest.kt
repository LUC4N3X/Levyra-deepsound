package com.luc4n3x.levyra.domain

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LevyraPlaybackAutomationTest {

    private val zone: ZoneId = ZoneId.of("Europe/Rome")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone)

    @Test
    fun disabledScheduleHasNoTrigger() {
        val schedule = LevyraBedtimeSchedule(enabled = false)

        assertNull(nextBedtimeTrigger(schedule, at(2026, 9, 10, 20, 0)))
    }

    @Test
    fun scheduleWithoutDaysHasNoTrigger() {
        val schedule = LevyraBedtimeSchedule(enabled = true, days = emptySet())

        assertNull(nextBedtimeTrigger(schedule, at(2026, 9, 10, 20, 0)))
    }

    @Test
    fun sameDayTriggerIsUsedWhenStartIsStillAhead() {
        val schedule = LevyraBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 23 * 60 + 30,
            days = setOf(DayOfWeek.THURSDAY)
        )

        val next = nextBedtimeTrigger(schedule, at(2026, 9, 10, 20, 0))

        assertEquals(at(2026, 9, 10, 23, 30), next)
    }

    @Test
    fun startAlreadyPassedRollsOverToTheNextRepeatDay() {
        val schedule = LevyraBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 23 * 60 + 30,
            days = setOf(DayOfWeek.THURSDAY)
        )

        val next = nextBedtimeTrigger(schedule, at(2026, 9, 10, 23, 45))

        assertEquals(at(2026, 9, 17, 23, 30), next)
    }

    @Test
    fun afterMidnightStartUsesTheSameCalendarDay() {
        val schedule = LevyraBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 30,
            days = setOf(DayOfWeek.FRIDAY)
        )

        val next = nextBedtimeTrigger(schedule, at(2026, 9, 10, 23, 45))

        assertEquals(at(2026, 9, 11, 0, 30), next)
    }

    @Test
    fun weekdayScheduleSkipsTheWeekend() {
        val schedule = LevyraBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 23 * 60 + 30,
            days = LevyraBedtimeSchedule.DEFAULT_DAYS
        )

        val next = nextBedtimeTrigger(schedule, at(2026, 9, 12, 23, 45))

        assertEquals(at(2026, 9, 14, 23, 30), next)
    }

    @Test
    fun scheduleValuesAreClamped() {
        val schedule = LevyraBedtimeSchedule(
            enabled = true,
            startMinuteOfDay = 5_000,
            durationMinutes = 10_000
        ).normalized()

        assertEquals(LevyraBedtimeSchedule.MINUTES_PER_DAY - 1, schedule.startMinuteOfDay)
        assertEquals(LevyraBedtimeSchedule.MAX_DURATION_MINUTES, schedule.durationMinutes)
    }

    @Test
    fun fadeIsDisabledUntilTheSettingIsOn() {
        val settings = LevyraAutomationSettings(sleepFadeOutSeconds = 25)

        assertEquals(0L, settings.fadeMsFor(15 * 60_000L))
    }

    @Test
    fun fadeNeverExceedsHalfTheTimerDuration() {
        val settings = LevyraAutomationSettings(sleepFadeOutEnabled = true, sleepFadeOutSeconds = 30)

        assertEquals(30_000L, settings.fadeMsFor(15 * 60_000L))
        assertEquals(10_000L, settings.fadeMsFor(20_000L))
        assertEquals(0L, settings.fadeMsFor(0L))
    }

    @Test
    fun fadeSecondsAreClamped() {
        assertEquals(
            LevyraAutomationSettings.MAX_FADE_SECONDS,
            LevyraAutomationSettings(sleepFadeOutSeconds = 9_999).normalized().sleepFadeOutSeconds
        )
        assertEquals(
            LevyraAutomationSettings.MIN_FADE_SECONDS,
            LevyraAutomationSettings(sleepFadeOutSeconds = 0).normalized().sleepFadeOutSeconds
        )
    }
}
