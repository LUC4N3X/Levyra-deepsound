package com.luc4n3x.levyra.desktop.core.sponsorblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SponsorSkipPlannerTest {

    private val intro = SponsorSegment(0L, 12_000L, "intro", uuid = "intro-1")
    private val sponsor = SponsorSegment(30_000L, 45_000L, "sponsor", uuid = "sponsor-1")
    private val segments = listOf(intro, sponsor)

    @Test
    fun positionOutsideEverySegmentIsNotSkipped() {
        assertNull(SponsorSkipPlanner.decide(segments, positionMs = 20_000L, alreadySkipped = emptySet()))
    }

    @Test
    fun positionInsideASegmentSkipsToItsEnd() {
        val decision = SponsorSkipPlanner.decide(segments, positionMs = 31_000L, alreadySkipped = emptySet())
        assertEquals(45_000L, decision?.targetPositionMs)
        assertEquals(listOf("sponsor-1"), decision?.skippedIdentities)
    }

    @Test
    fun overlappingSegmentsSkipToTheFarthestEnd() {
        val overlapping = listOf(
            SponsorSegment(10_000L, 20_000L, "sponsor", uuid = "a"),
            SponsorSegment(12_000L, 26_000L, "selfpromo", uuid = "b")
        )
        val decision = SponsorSkipPlanner.decide(overlapping, positionMs = 13_000L, alreadySkipped = emptySet())
        assertEquals(26_000L, decision?.targetPositionMs)
        assertEquals(listOf("a", "b"), decision?.skippedIdentities)
    }

    @Test
    fun anAlreadySkippedSegmentIsNotSkippedAgain() {
        assertNull(
            SponsorSkipPlanner.decide(segments, positionMs = 31_000L, alreadySkipped = setOf("sponsor-1"))
        )
    }

    @Test
    fun theGuardWindowBeforeTheEndIsNotSkipped() {
        assertNull(
            SponsorSkipPlanner.decide(
                segments,
                positionMs = 45_000L - SponsorSkipPlanner.SKIP_GUARD_MS,
                alreadySkipped = emptySet()
            )
        )
    }

    @Test
    fun nonSkipActionTypesAreIgnored() {
        val muted = listOf(SponsorSegment(0L, 9_000L, "sponsor", uuid = "m", actionType = "mute"))
        assertNull(SponsorSkipPlanner.decide(muted, positionMs = 1_000L, alreadySkipped = emptySet()))
    }

    @Test
    fun segmentsWithoutUuidFallBackToAPositionalIdentity() {
        val anonymous = listOf(SponsorSegment(0L, 5_000L, "intro"))
        val decision = SponsorSkipPlanner.decide(anonymous, positionMs = 100L, alreadySkipped = emptySet())
        assertEquals(listOf("0:5000:intro"), decision?.skippedIdentities)
    }

    @Test
    fun trackerSkipsEachSegmentOnlyOncePerBoundTrack() {
        val tracker = SponsorSkipTracker()
        tracker.bind("video-1")
        assertEquals(45_000L, tracker.planSkip("video-1", 31_000L, segments))
        assertNull(tracker.planSkip("video-1", 31_000L, segments))
    }

    @Test
    fun trackerIgnoresSegmentsBoundToAnotherTrack() {
        val tracker = SponsorSkipTracker()
        tracker.bind("video-1")
        assertNull(tracker.planSkip("video-2", 31_000L, segments))
    }

    @Test
    fun rebindingClearsPreviouslySkippedSegments() {
        val tracker = SponsorSkipTracker()
        tracker.bind("video-1")
        tracker.planSkip("video-1", 31_000L, segments)
        tracker.bind("video-1")
        tracker.bind("video-2")
        tracker.bind("video-1")
        assertEquals(45_000L, tracker.planSkip("video-1", 31_000L, segments))
    }

    @Test
    fun resetStopsEverySkip() {
        val tracker = SponsorSkipTracker()
        tracker.bind("video-1")
        tracker.reset()
        assertNull(tracker.planSkip("video-1", 31_000L, segments))
    }
}
