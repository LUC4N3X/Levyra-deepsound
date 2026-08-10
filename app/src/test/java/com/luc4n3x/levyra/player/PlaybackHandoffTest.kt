package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackHandoffTest {
    @Test
    fun sameTrackUsesLatestActivePosition() {
        assertEquals(
            9_000L,
            replacementStartPosition(
                sameTrack = true,
                requestedPositionMs = 6_000L,
                activePositionMs = 9_000L,
                durationMs = 180_000L
            )
        )
    }

    @Test
    fun recoveryDoesNotMoveBehindRequestedPosition() {
        assertEquals(
            12_000L,
            replacementStartPosition(
                sameTrack = true,
                requestedPositionMs = 12_000L,
                activePositionMs = 8_000L,
                durationMs = 180_000L
            )
        )
    }

    @Test
    fun differentTrackDoesNotInheritOldPosition() {
        assertEquals(
            0L,
            replacementStartPosition(
                sameTrack = false,
                requestedPositionMs = 0L,
                activePositionMs = 90_000L,
                durationMs = 180_000L
            )
        )
    }

    @Test
    fun handoffIsBoundedByResolvedDuration() {
        assertEquals(
            179_750L,
            replacementStartPosition(
                sameTrack = true,
                requestedPositionMs = 190_000L,
                activePositionMs = 195_000L,
                durationMs = 180_000L
            )
        )
    }
}
