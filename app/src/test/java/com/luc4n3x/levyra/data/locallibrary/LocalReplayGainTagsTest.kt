package com.luc4n3x.levyra.data.locallibrary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalReplayGainTagsTest {
    @Test
    fun readsStandardReplayGainTags() {
        val tags = parseLocalReplayGainTags(
            """
            REPLAYGAIN_TRACK_GAIN=-7.10 dB
            REPLAYGAIN_ALBUM_GAIN=-5,25 dB
            REPLAYGAIN_TRACK_PEAK=0.9231
            REPLAYGAIN_ALBUM_PEAK=0.9812
            """.trimIndent()
        )

        assertEquals(-7.10f, tags.trackGainDb ?: 0f, 0.001f)
        assertEquals(-5.25f, tags.albumGainDb ?: 0f, 0.001f)
        assertEquals(0.9231f, tags.trackPeak ?: 0f, 0.0001f)
        assertEquals(0.9812f, tags.albumPeak ?: 0f, 0.0001f)
    }

    @Test
    fun ignoresMalformedReplayGainValues() {
        val tags = parseLocalReplayGainTags(
            """
            REPLAYGAIN_TRACK_GAIN=not-a-number
            REPLAYGAIN_TRACK_PEAK=-1
            """.trimIndent()
        )

        assertNull(tags.trackGainDb)
        assertNull(tags.trackPeak)
    }
}
