package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.SponsorSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SponsorBlockSkipOnceTest {

    private fun segment(
        startMs: Long,
        endMs: Long,
        uuid: String = "",
        category: String = "sponsor"
    ) = SponsorSegment(startMs = startMs, endMs = endMs, category = category, uuid = uuid)

    @Test
    fun firstEncounterSkipsToSegmentEnd() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(segment(5_000L, 15_000L, uuid = "a"))
        tracker.beginPlayback("video")

        assertEquals(15_000L, tracker.planSkip("video", 5_100L, segments))
    }

    @Test
    fun rewindIntoConsumedSegmentDoesNotSkipAgain() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(segment(5_000L, 15_000L, uuid = "a"))
        tracker.beginPlayback("video")
        tracker.planSkip("video", 5_100L, segments)

        assertNull(tracker.planSkip("video", 6_000L, segments))
        assertNull(tracker.planSkip("video", 14_000L, segments))
    }

    @Test
    fun otherSegmentStillSkipsAfterFirstOneIsConsumed() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(
            segment(5_000L, 15_000L, uuid = "a"),
            segment(40_000L, 50_000L, uuid = "b")
        )
        tracker.beginPlayback("video")
        tracker.planSkip("video", 5_100L, segments)

        assertEquals(50_000L, tracker.planSkip("video", 41_000L, segments))
        assertNull(tracker.planSkip("video", 6_000L, segments))
    }

    @Test
    fun mediaChangeResetsConsumedState() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(segment(5_000L, 15_000L, uuid = "a"))
        tracker.beginPlayback("video")
        tracker.planSkip("video", 5_100L, segments)

        tracker.bind("other-video")
        assertEquals(0, tracker.consumedCount)
        tracker.bind("video")

        assertEquals(15_000L, tracker.planSkip("video", 5_100L, segments))
    }

    @Test
    fun replayingSameMediaResetsConsumedState() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(segment(5_000L, 15_000L, uuid = "a"))
        tracker.beginPlayback("video")
        tracker.planSkip("video", 5_100L, segments)

        tracker.beginPlayback("video")

        assertEquals(15_000L, tracker.planSkip("video", 5_100L, segments))
    }

    @Test
    fun bindKeepsConsumedStateWhenMediaIsUnchanged() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(segment(5_000L, 15_000L, uuid = "a"))
        tracker.beginPlayback("video")
        tracker.planSkip("video", 5_100L, segments)

        tracker.bind("video")

        assertNull(tracker.planSkip("video", 5_100L, segments))
    }

    @Test
    fun stateFromOneMediaNeverAppliesToAnother() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(segment(5_000L, 15_000L, uuid = "shared"))
        tracker.beginPlayback("video-a")
        tracker.planSkip("video-a", 5_100L, segments)

        assertNull(tracker.planSkip("video-b", 5_100L, segments))

        tracker.beginPlayback("video-b")
        assertEquals(15_000L, tracker.planSkip("video-b", 5_100L, segments))
    }

    @Test
    fun resetClearsBindingAndConsumedState() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(segment(5_000L, 15_000L, uuid = "a"))
        tracker.beginPlayback("video")
        tracker.planSkip("video", 5_100L, segments)

        tracker.reset()

        assertNull(tracker.activeMediaKey)
        assertEquals(0, tracker.consumedCount)
        assertNull(tracker.planSkip("video", 5_100L, segments))
    }

    @Test
    fun overlappingSegmentsSkipToFurthestEndAndAreAllConsumed() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(
            segment(5_000L, 12_000L, uuid = "a"),
            segment(6_000L, 20_000L, uuid = "b")
        )
        tracker.beginPlayback("video")

        assertEquals(20_000L, tracker.planSkip("video", 7_000L, segments))
        assertEquals(2, tracker.consumedCount)
        assertNull(tracker.planSkip("video", 7_000L, segments))
    }

    @Test
    fun adjacentSegmentsAreSkippedOneAfterTheOther() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(
            segment(0L, 5_000L, uuid = "a"),
            segment(5_000L, 9_000L, uuid = "b")
        )
        tracker.beginPlayback("video")

        assertEquals(5_000L, tracker.planSkip("video", 100L, segments))
        assertEquals(9_000L, tracker.planSkip("video", 5_000L, segments))
        assertNull(tracker.planSkip("video", 100L, segments))
    }

    @Test
    fun consumedOuterSegmentDoesNotBlockUnconsumedInnerSegment() {
        val tracker = SponsorBlockSkipOnceTracker()
        val outer = segment(1_000L, 20_000L, uuid = "outer")
        val inner = segment(5_000L, 8_000L, uuid = "inner")
        val segments = listOf(outer, inner)
        tracker.beginPlayback("video")

        assertEquals(20_000L, tracker.planSkip("video", 1_100L, segments))
        assertEquals(8_000L, tracker.planSkip("video", 5_500L, segments))
        assertNull(tracker.planSkip("video", 5_500L, segments))
    }

    @Test
    fun segmentTailGuardPreventsSkipLoopAtSegmentEnd() {
        val segments = listOf(segment(5_000L, 15_000L, uuid = "a"))

        assertNull(sponsorBlockSkipDecision(segments, 14_800L, emptySet()))
        assertEquals(
            15_000L,
            sponsorBlockSkipDecision(segments, 14_700L, emptySet())?.targetPositionMs
        )
    }

    @Test
    fun positionOutsideEverySegmentYieldsNoDecision() {
        val segments = listOf(segment(5_000L, 15_000L, uuid = "a"))

        assertNull(sponsorBlockSkipDecision(segments, 4_999L, emptySet()))
        assertNull(sponsorBlockSkipDecision(segments, 15_000L, emptySet()))
    }

    @Test
    fun degenerateSegmentIsIgnored() {
        val segments = listOf(segment(5_000L, 5_000L, uuid = "a"))

        assertNull(sponsorBlockSkipDecision(segments, 5_000L, emptySet()))
    }

    @Test
    fun segmentsWithoutUuidFallBackToBoundsAndCategoryIdentity() {
        val tracker = SponsorBlockSkipOnceTracker()
        val segments = listOf(
            segment(5_000L, 15_000L, category = "sponsor"),
            segment(5_000L, 15_000L, category = "intro")
        )
        tracker.beginPlayback("video")

        assertEquals(15_000L, tracker.planSkip("video", 5_100L, segments))
        assertEquals(2, tracker.consumedCount)
        assertNull(tracker.planSkip("video", 5_100L, segments))
    }

    @Test
    fun identityPrefersUpstreamUuidOverBounds() {
        assertEquals("uuid-1", sponsorSegmentIdentity(segment(0L, 1_000L, uuid = "uuid-1")))
        assertEquals("0:1000:sponsor", sponsorSegmentIdentity(segment(0L, 1_000L)))
    }
}
