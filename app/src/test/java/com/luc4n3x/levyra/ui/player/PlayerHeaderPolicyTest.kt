package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerHeaderPolicyTest {
    @Test
    fun immersiveAudioHeaderKeepsOnlyCollapseOnTheLeadingSide() {
        val policy = playerHeaderPolicy(
            isVideoMode = false,
            isLiveRadio = false,
            hasTrack = true,
            hasSubtitles = false,
            videoQualityAvailable = false,
            slotWidthDp = 48f,
            spacingDp = 4f,
            sideReserveDp = 108f
        )

        assertEquals(1, policy.leadingCount)
        assertEquals(2, policy.trailingCount)
        assertTrue(policy.centerLocked)
    }

    @Test
    fun audioOutputIsPlacedInOverflowInsteadOfTheHeader() {
        assertEquals(
            PlayerAudioRoutePlacement.Overflow,
            playerAudioRoutePlacement(isVideoMode = false, hasTrack = true)
        )
    }

    @Test
    fun audioOutputIsNotOfferedForVideoMode() {
        assertEquals(
            PlayerAudioRoutePlacement.Hidden,
            playerAudioRoutePlacement(isVideoMode = true, hasTrack = true)
        )
    }
}
