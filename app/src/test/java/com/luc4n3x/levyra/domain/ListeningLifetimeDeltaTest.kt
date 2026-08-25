package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local replica of the lifetime-listening store's snapshot delta algorithm:
 * previousMs/newMs = max(previousMs, incomingMs); deltaMs = newMs - previousMs.
 * playDelta/completionDelta only fire on the false->true transition of their flag.
 */
private class SessionDeltaState {
    var previousMs: Long = 0L
    var countedPlayFlagged: Boolean = false
    var completedFlagged: Boolean = false
}

private data class DeltaResult(
    val deltaMs: Long,
    val playDelta: Int,
    val completionDelta: Int
)

private fun applySnapshot(
    state: SessionDeltaState,
    incomingMs: Long,
    trackDurationMs: Long,
    completed: Boolean
): DeltaResult {
    val newMs = maxOf(state.previousMs, incomingMs)
    val deltaMs = newMs - state.previousMs
    state.previousMs = newMs

    val countedNow = ListenPlayPolicy.isCountedPlay(newMs, trackDurationMs, completed)
    val playDelta = if (countedNow && !state.countedPlayFlagged) 1 else 0
    if (countedNow) state.countedPlayFlagged = true

    val completionDelta = if (completed && !state.completedFlagged) 1 else 0
    if (completed) state.completedFlagged = true

    return DeltaResult(deltaMs, playDelta, completionDelta)
}

class ListeningLifetimeDeltaTest {

    @Test
    fun `periodic flush snapshots accumulate to total listened not sum of raw snapshots`() {
        val state = SessionDeltaState()
        val deltas = listOf(5_000L, 20_000L, 35_000L, 60_000L).map { incoming ->
            applySnapshot(state, incoming, trackDurationMs = 0L, completed = false)
        }
        val totalDeltaMs = deltas.sumOf { it.deltaMs }
        assertEquals(60_000L, totalDeltaMs)
        assertTrue(totalDeltaMs != 120_000L)
    }

    @Test
    fun `counted play delta fires exactly once across the same session sequence`() {
        val state = SessionDeltaState()
        val playDeltas = listOf(5_000L, 20_000L, 35_000L, 60_000L).map { incoming ->
            applySnapshot(state, incoming, trackDurationMs = 0L, completed = false).playDelta
        }
        assertEquals(1, playDeltas.sum())
    }

    @Test
    fun `two independent sessions of same track each counted once total two`() {
        val sessionA = SessionDeltaState()
        val sessionB = SessionDeltaState()

        val resultA = applySnapshot(sessionA, 30_000L, trackDurationMs = 0L, completed = false)
        val resultB = applySnapshot(sessionB, 31_000L, trackDurationMs = 0L, completed = false)

        val totalPlays = resultA.playDelta + resultB.playDelta
        assertEquals(2, totalPlays)
    }

    @Test
    fun `completion across two independent sessions can reach two`() {
        val sessionA = SessionDeltaState()
        val sessionB = SessionDeltaState()

        val resultA = applySnapshot(sessionA, 20_000L, trackDurationMs = 20_000L, completed = true)
        val resultB = applySnapshot(sessionB, 20_000L, trackDurationMs = 20_000L, completed = true)

        val totalCompletions = resultA.completionDelta + resultB.completionDelta
        assertEquals(2, totalCompletions)
    }

    @Test
    fun `lower incoming snapshot than persisted produces zero delta and no negative totals`() {
        val state = SessionDeltaState()
        applySnapshot(state, 60_000L, trackDurationMs = 0L, completed = false)

        val result = applySnapshot(state, 40_000L, trackDurationMs = 0L, completed = false)

        assertEquals(0L, result.deltaMs)
        assertTrue(state.previousMs >= 0L)
        assertEquals(60_000L, state.previousMs)
    }
}
