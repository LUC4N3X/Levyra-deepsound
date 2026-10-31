package com.luc4n3x.levyra.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionLoopTest {

    @Test
    fun tapsMarkTheStartThenTheEndThenClear() {
        val marking = SectionLoopState.Off.advance("song", 62_400L)
        assertEquals(SectionLoopState.Marking("song", 62_400L), marking)
        val active = marking.advance("song", 90_100L)
        assertEquals(SectionLoopState.Active("song", 62_400L, 90_100L), active)
        assertEquals(SectionLoopState.Off, active.advance("song", 95_000L))
    }

    @Test
    fun anEndTooCloseToTheStartKeepsMarking() {
        val marking = SectionLoopState.Marking("song", 10_000L)
        assertEquals(marking, marking.advance("song", 10_500L))
    }

    @Test
    fun anEndBeforeTheStartSwapsThePoints() {
        val active = SectionLoopState.Marking("song", 40_000L).advance("song", 20_000L)
        assertEquals(SectionLoopState.Active("song", 20_000L, 40_000L), active)
    }

    @Test
    fun markingRestartsOnAnotherTrack() {
        val marking = SectionLoopState.Marking("old", 5_000L).advance("new", 8_000L)
        assertEquals(SectionLoopState.Marking("new", 8_000L), marking)
    }

    @Test
    fun playbackJumpsBackOnlyWhenItLeavesTheSection() {
        val loop = SectionLoopState.Active("song", 62_000L, 90_000L)
        assertNull(sectionLoopSeekTarget(loop, "song", 75_000L))
        assertEquals(62_000L, sectionLoopSeekTarget(loop, "song", 90_000L))
        assertEquals(62_000L, sectionLoopSeekTarget(loop, "song", 30_000L))
        assertNull(sectionLoopSeekTarget(loop, "other", 95_000L))
        assertNull(sectionLoopSeekTarget(SectionLoopState.Off, "song", 95_000L))
    }

    @Test
    fun completionRestartsOnlyTheLoopedTrack() {
        val loop = SectionLoopState.Active("song", 62_000L, 90_000L)
        assertEquals(62_000L, sectionLoopRestartTarget(loop, "song"))
        assertNull(sectionLoopRestartTarget(loop, "other"))
        assertNull(sectionLoopRestartTarget(SectionLoopState.Marking("song", 62_000L), "song"))
        assertNull(sectionLoopRestartTarget(SectionLoopState.Off, "song"))
    }

    @Test
    fun pollTightensNearTheEndAndFollowsPlaybackSpeed() {
        val loop = SectionLoopState.Active("song", 1_000L, 5_000L)
        assertEquals(SECTION_LOOP_POLL_MS, sectionLoopPollDelayMs(loop, 2_000L, 1f))
        assertEquals(60L, sectionLoopPollDelayMs(loop, 4_940L, 1f))
        assertEquals(30L, sectionLoopPollDelayMs(loop, 4_940L, 2f))
        assertEquals(SECTION_LOOP_MIN_POLL_MS, sectionLoopPollDelayMs(loop, 5_100L, 1f))
        assertEquals(SECTION_LOOP_POLL_MS, sectionLoopPollDelayMs(SectionLoopState.Off, 0L, 1f))
    }

    @Test
    fun loopIsDroppedWhenTheTrackChanges() {
        val loop = SectionLoopState.Active("song", 1_000L, 5_000L)
        assertTrue(loop.belongsTo("song"))
        assertFalse(loop.belongsTo("next"))
        assertTrue(SectionLoopState.Off.belongsTo(null))
    }
}
