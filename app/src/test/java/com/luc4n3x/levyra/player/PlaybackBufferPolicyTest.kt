package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.PlaybackBufferMode
import com.luc4n3x.levyra.domain.PlaybackBufferSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlaybackBufferPolicyTest {
    private val automatic = PlaybackBufferProfile(6_000, 12_000, 100, 250, 0)

    @Test
    fun automaticReturnsTheUnmodifiedAdaptiveProfile() {
        assertSame(automatic, playbackBufferProfile(automatic, PlaybackBufferSettings()))
    }

    @Test
    fun customMapsAllSupportedDurationsAndKeepsAutomaticBackBuffer() {
        val custom = playbackBufferProfile(
            automatic,
            PlaybackBufferSettings(
                mode = PlaybackBufferMode.CUSTOM,
                minBufferSeconds = 8f,
                maxBufferSeconds = 20f,
                playbackBufferSeconds = 0.8f,
                rebufferSeconds = 1.5f
            )
        )

        assertEquals(8_000, custom.minBufferMs)
        assertEquals(20_000, custom.maxBufferMs)
        assertEquals(800, custom.playbackBufferMs)
        assertEquals(1_500, custom.rebufferMs)
        assertEquals(automatic.backBufferMs, custom.backBufferMs)
    }

    @Test
    fun transitionStartupBufferIsCappedWithoutChangingSmallerValues() {
        assertEquals(100, transitionPlaybackBufferMs(100))
        assertEquals(1_500, transitionPlaybackBufferMs(1_500))
        assertEquals(2_000, transitionPlaybackBufferMs(2_000))
        assertEquals(2_000, transitionPlaybackBufferMs(10_000))
    }
}
