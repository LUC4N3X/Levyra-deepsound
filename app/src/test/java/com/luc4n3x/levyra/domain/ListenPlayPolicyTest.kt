package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenPlayPolicyTest {

    @Test
    fun `listened 3s is not recordable`() {
        assertFalse(ListenPlayPolicy.isRecordableEvent(3_000L))
    }

    @Test
    fun `listened 15s not completed under long track is not counted`() {
        assertFalse(ListenPlayPolicy.isCountedPlay(15_000L, 200_000L, false))
    }

    @Test
    fun `29999ms is not counted, 30000ms is counted`() {
        assertFalse(ListenPlayPolicy.isCountedPlay(29_999L, 0L, false))
        assertTrue(ListenPlayPolicy.isCountedPlay(30_000L, 0L, false))
    }

    @Test
    fun `does not trust completed flag alone`() {
        assertFalse(ListenPlayPolicy.isCountedPlay(20_000L, 180_000L, true))
    }

    @Test
    fun `short track at or above 80 percent completion counts`() {
        assertTrue(ListenPlayPolicy.isCountedPlay(16_000L, 20_000L, true))
    }

    @Test
    fun `short track below completion threshold does not count`() {
        assertFalse(ListenPlayPolicy.isCountedPlay(5_000L, 20_000L, true))
    }

    @Test
    fun `short track not completed does not count even at 95 percent`() {
        assertFalse(ListenPlayPolicy.isCountedPlay(19_000L, 20_000L, false))
    }

    @Test
    fun `unknown duration counts once past 30s threshold`() {
        assertTrue(ListenPlayPolicy.isCountedPlay(40_000L, 0L, false))
    }

    @Test
    fun `trackKey falls back to title and artist when trackId blank`() {
        assertEquals("song title|the artist", ListenIdentity.trackKey("  ", " Song Title ", " The Artist "))
        assertEquals("abc123", ListenIdentity.trackKey(" abc123 ", "Song Title", "The Artist"))
    }

    @Test
    fun `artistKey trims and lowercases`() {
        assertEquals("the artist", ListenIdentity.artistKey("  The Artist  "))
    }
}
