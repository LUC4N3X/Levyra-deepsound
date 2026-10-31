package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerShareMomentTest {

    @Test
    fun momentIsOfferedOnlyAfterTheOpeningSecondsAndBeforeTheEnd() {
        assertNull(shareMomentSeconds(positionMs = 4_999L, durationMs = 200_000L))
        assertEquals(83L, shareMomentSeconds(positionMs = 83_900L, durationMs = 200_000L))
        assertNull(shareMomentSeconds(positionMs = 200_000L, durationMs = 200_000L))
        assertEquals(12L, shareMomentSeconds(positionMs = 12_000L, durationMs = 0L))
    }

    @Test
    fun linkStartsPlaybackAtTheSharedSecond() {
        assertEquals("https://www.youtube.com/watch?v=abc123&t=83s", shareMomentUrl("abc123", 83L))
    }

    @Test
    fun timestampUsesHoursOnlyForLongTracks() {
        assertEquals("0:07", formatShareMomentTimestamp(7L))
        assertEquals("1:23", formatShareMomentTimestamp(83L))
        assertEquals("1:02:03", formatShareMomentTimestamp(3_723L))
    }

    @Test
    fun sharedTextNamesTheMomentAndCarriesTheLink() {
        assertEquals(
            "Song — Artist (from 1:23)\nhttps://www.youtube.com/watch?v=abc123&t=83s",
            shareMomentText("Song", "Artist", "abc123", 83L, "from")
        )
        assertEquals(
            "Song (from 0:07)\nhttps://www.youtube.com/watch?v=abc123&t=7s",
            shareMomentText("Song", "", "abc123", 7L, "from")
        )
    }
}
