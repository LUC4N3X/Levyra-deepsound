package com.luc4n3x.levyra.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueuePrefetchPolicyTest {

    @Test
    fun `next track preload stays enabled by default`() {
        assertTrue(LevyraAudioSettings().preloadNextTrack)
        assertTrue(queuePrefetchAllowed(LevyraAudioSettings()))
    }

    @Test
    fun `disabling preload blocks the optional queue prefetch`() {
        assertFalse(queuePrefetchAllowed(LevyraAudioSettings(preloadNextTrack = false)))
    }

    @Test
    fun `preload stays independent from gapless`() {
        assertTrue(queuePrefetchAllowed(LevyraAudioSettings(gaplessEnabled = false)))
        assertFalse(queuePrefetchAllowed(LevyraAudioSettings(gaplessEnabled = true, preloadNextTrack = false)))
    }
}
