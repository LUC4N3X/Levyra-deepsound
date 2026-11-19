package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportPromptScheduleTest {
    private val fresh = SupportPromptState(versionCode = 0, opens = 0, lastOpenAtMs = 0L, finished = false)
    private val hour = 60L * 60L * 1_000L

    @Test
    fun promptsOnFirstOpenThenEveryTenOpens() {
        var state = fresh
        val due = mutableListOf<Int>()
        repeat(70) { index ->
            state = SupportPromptSchedule.recordOpen(state, versionCode = 7, nowMs = (index + 1) * hour)!!
            if (SupportPromptSchedule.isDue(state)) due += state.opens
        }
        assertEquals(listOf(1, 11, 21, 31, 41, 51), due)
    }

    @Test
    fun reopeningWithinTheSessionGapIsNotANewOpen() {
        val first = SupportPromptSchedule.recordOpen(fresh, versionCode = 7, nowMs = hour)!!
        assertNull(SupportPromptSchedule.recordOpen(first, versionCode = 7, nowMs = hour + 60_000L))
        assertEquals(2, SupportPromptSchedule.recordOpen(first, versionCode = 7, nowMs = 2 * hour)?.opens)
    }

    @Test
    fun updateRestartsTheCountButNotAFinishedPrompt() {
        val counted = fresh.copy(versionCode = 7, opens = 34, lastOpenAtMs = hour)
        assertEquals(1, SupportPromptSchedule.recordOpen(counted, versionCode = 8, nowMs = hour + 1L)?.opens)

        val finished = counted.copy(opens = 10, finished = true)
        val next = SupportPromptSchedule.recordOpen(finished, versionCode = 7, nowMs = 2 * hour)!!
        assertEquals(11, next.opens)
        assertTrue(next.finished)
        assertFalse(SupportPromptSchedule.isDue(next))
    }
}
