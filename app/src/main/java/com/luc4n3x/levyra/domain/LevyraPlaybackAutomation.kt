package com.luc4n3x.levyra.domain

import java.time.DayOfWeek
import java.time.ZonedDateTime

data class LevyraBedtimeSchedule(
    val enabled: Boolean = false,
    val startMinuteOfDay: Int = DEFAULT_START_MINUTE,
    val durationMinutes: Int = DEFAULT_DURATION_MINUTES,
    val days: Set<DayOfWeek> = DEFAULT_DAYS
) {
    fun normalized(): LevyraBedtimeSchedule = copy(
        startMinuteOfDay = startMinuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1),
        durationMinutes = durationMinutes.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
    )

    val isArmed: Boolean
        get() = enabled && days.isNotEmpty()

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val MIN_DURATION_MINUTES = 5
        const val MAX_DURATION_MINUTES = 480
        const val DEFAULT_START_MINUTE = 23 * 60 + 30
        const val DEFAULT_DURATION_MINUTES = 45
        val DEFAULT_DAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    }
}

data class LevyraAutomationSettings(
    val resumeOnBluetoothReconnect: Boolean = false,
    val pauseOnMute: Boolean = false,
    val autoDownloadFavorites: Boolean = false,
    val skipUnrecoverableErrors: Boolean = false,
    val sleepFadeOutEnabled: Boolean = false,
    val sleepFadeOutSeconds: Int = DEFAULT_FADE_SECONDS,
    val bedtime: LevyraBedtimeSchedule = LevyraBedtimeSchedule()
) {
    fun normalized(): LevyraAutomationSettings = copy(
        sleepFadeOutSeconds = sleepFadeOutSeconds.coerceIn(MIN_FADE_SECONDS, MAX_FADE_SECONDS),
        bedtime = bedtime.normalized()
    )

    fun fadeMsFor(totalMs: Long): Long {
        if (!sleepFadeOutEnabled || totalMs <= 0L) return 0L
        val requested = sleepFadeOutSeconds.coerceIn(MIN_FADE_SECONDS, MAX_FADE_SECONDS) * 1_000L
        return requested.coerceAtMost(totalMs / 2L)
    }

    companion object {
        const val MIN_FADE_SECONDS = 5
        const val MAX_FADE_SECONDS = 120
        const val DEFAULT_FADE_SECONDS = 25
    }
}

fun nextBedtimeTrigger(
    schedule: LevyraBedtimeSchedule,
    now: ZonedDateTime
): ZonedDateTime? {
    val normalized = schedule.normalized()
    if (!normalized.isArmed) return null
    val startDate = now.toLocalDate()
    val startTime = java.time.LocalTime.of(
        normalized.startMinuteOfDay / 60,
        normalized.startMinuteOfDay % 60
    )
    for (dayOffset in 0..7) {
        val date = startDate.plusDays(dayOffset.toLong())
        if (date.dayOfWeek !in normalized.days) continue
        val candidate = date.atTime(startTime).atZone(now.zone)
        if (!candidate.isAfter(now)) continue
        return candidate
    }
    return null
}
