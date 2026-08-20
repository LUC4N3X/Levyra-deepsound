package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfadePlannerTest {

    private fun track(album: String = "", durationMs: Long = 240_000L) = Track(
        id = album + durationMs,
        title = "Title",
        album = album,
        videoUrl = "https://example.org",
        durationMs = durationMs
    )

    @Test
    fun requestedDurationIsClampedToHalfOfTheShorterTrack() {
        assertEquals(6_000L, CrossfadePlanner.effectiveDurationMs(6_000, 240_000L, 200_000L))
        assertEquals(9_000L, CrossfadePlanner.effectiveDurationMs(12_000, 18_000L, 200_000L))
        assertEquals(5_000L, CrossfadePlanner.effectiveDurationMs(12_000, 200_000L, 10_000L))
    }

    @Test
    fun tooShortOrDisabledTransitionsResolveToZero() {
        assertEquals(0L, CrossfadePlanner.effectiveDurationMs(0, 240_000L, 240_000L))
        assertEquals(0L, CrossfadePlanner.effectiveDurationMs(6_000, 0L, 240_000L))
        assertEquals(0L, CrossfadePlanner.effectiveDurationMs(6_000, 800L, 240_000L))
    }

    @Test
    fun smartCrossfadeSkipsConsecutiveTracksOfTheSameAlbum() {
        val current = track(album = "Blurred")
        val next = track(album = "blurred")

        assertTrue(CrossfadePlanner.sharesAlbum(current, next))
        assertEquals(
            0L,
            CrossfadePlanner.transitionDurationMs(6_000, true, current, next, 240_000L)
        )
        assertEquals(
            6_000L,
            CrossfadePlanner.transitionDurationMs(6_000, false, current, next, 240_000L)
        )
    }

    @Test
    fun unknownAlbumsNeverCountAsTheSameAlbum() {
        assertFalse(CrossfadePlanner.sharesAlbum(track(album = ""), track(album = "")))
        assertEquals(
            6_000L,
            CrossfadePlanner.transitionDurationMs(6_000, true, track(), track(), 240_000L)
        )
    }

    @Test
    fun equalPowerGainsPreserveConstantEnergy() {
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
            val outgoing = CrossfadePlanner.outgoingGain(fraction)
            val incoming = CrossfadePlanner.incomingGain(fraction)
            assertEquals(1.0, (outgoing * outgoing + incoming * incoming).toDouble(), 0.0001)
        }
        assertEquals(1f, CrossfadePlanner.outgoingGain(0f), 0.0001f)
        assertEquals(0f, CrossfadePlanner.outgoingGain(1f), 0.0001f)
        assertEquals(0f, CrossfadePlanner.incomingGain(0f), 0.0001f)
        assertEquals(1f, CrossfadePlanner.incomingGain(1f), 0.0001f)
    }

    @Test
    fun prepareThresholdLeavesRoomForTheTransition() {
        assertEquals(
            240_000L - 6_000L - CrossfadePlanner.PREPARE_LEAD_MS,
            CrossfadePlanner.prepareThresholdMs(240_000L, 6_000L)
        )
        assertEquals(0L, CrossfadePlanner.prepareThresholdMs(1_000L, 6_000L))
        assertEquals(Long.MAX_VALUE, CrossfadePlanner.prepareThresholdMs(0L, 6_000L))
    }
}
