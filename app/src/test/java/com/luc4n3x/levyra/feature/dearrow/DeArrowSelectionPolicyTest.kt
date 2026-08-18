package com.luc4n3x.levyra.feature.dearrow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeArrowSelectionPolicyTest {

    @Test
    fun lockedTitleWinsOverHigherVotedUnlockedTitle() {
        val titles = listOf(
            DeArrowTitle("Unlocked Winner By Votes", locked = false, votes = 50, original = false),
            DeArrowTitle("Locked Choice", locked = true, votes = 1, original = false)
        )
        assertEquals("Locked Choice", DeArrowSelectionPolicy.selectTitle(titles))
    }

    @Test
    fun highestVotesWinsAmongUnlockedTitles() {
        val titles = listOf(
            DeArrowTitle("Low Votes", locked = false, votes = 1, original = false),
            DeArrowTitle("High Votes", locked = false, votes = 9, original = false)
        )
        assertEquals("High Votes", DeArrowSelectionPolicy.selectTitle(titles))
    }

    @Test
    fun negativeVoteTitlesAreRejected() {
        val titles = listOf(
            DeArrowTitle("Downvoted", locked = false, votes = -1, original = false)
        )
        assertNull(DeArrowSelectionPolicy.selectTitle(titles))
    }

    @Test
    fun blankTitleIsRejected() {
        val titles = listOf(
            DeArrowTitle("   ", locked = false, votes = 5, original = false)
        )
        assertNull(DeArrowSelectionPolicy.selectTitle(titles))
    }

    @Test
    fun titleLongerThanCapIsRejected() {
        val overLong = "x".repeat(DeArrowSelectionPolicy.MAX_TITLE_LENGTH + 1)
        val titles = listOf(
            DeArrowTitle(overLong, locked = false, votes = 5, original = false)
        )
        assertNull(DeArrowSelectionPolicy.selectTitle(titles))
    }

    @Test
    fun formattingMarkerIsStrippedFromSelectedTitle() {
        val titles = listOf(
            DeArrowTitle(">iPhone 15 Review", locked = false, votes = 5, original = false)
        )
        assertEquals("iPhone 15 Review", DeArrowSelectionPolicy.selectTitle(titles))
    }

    @Test
    fun originalTitlesAreNeverSelected() {
        val titles = listOf(
            DeArrowTitle("Original Video Title", locked = false, votes = 100, original = true)
        )
        assertNull(DeArrowSelectionPolicy.selectTitle(titles))
    }

    @Test
    fun emptyTitleListReturnsNull() {
        assertNull(DeArrowSelectionPolicy.selectTitle(emptyList()))
    }

    @Test
    fun lockedThumbnailWinsOverHigherVotedUnlockedThumbnail() {
        val thumbnails = listOf(
            DeArrowThumbnail(timestamp = 10.0, locked = false, votes = 50, original = false),
            DeArrowThumbnail(timestamp = 3.5, locked = true, votes = 1, original = false)
        )
        assertEquals(3.5, DeArrowSelectionPolicy.selectThumbnailTimestamp(thumbnails))
    }

    @Test
    fun highestVotesWinsAmongUnlockedThumbnails() {
        val thumbnails = listOf(
            DeArrowThumbnail(timestamp = 1.0, locked = false, votes = 1, original = false),
            DeArrowThumbnail(timestamp = 8.0, locked = false, votes = 9, original = false)
        )
        assertEquals(8.0, DeArrowSelectionPolicy.selectThumbnailTimestamp(thumbnails))
    }

    @Test
    fun negativeVoteThumbnailsAreRejected() {
        val thumbnails = listOf(
            DeArrowThumbnail(timestamp = 5.0, locked = false, votes = -3, original = false)
        )
        assertNull(DeArrowSelectionPolicy.selectThumbnailTimestamp(thumbnails))
    }

    @Test
    fun nullTimestampThumbnailIsRejected() {
        val thumbnails = listOf(
            DeArrowThumbnail(timestamp = null, locked = false, votes = 5, original = false)
        )
        assertNull(DeArrowSelectionPolicy.selectThumbnailTimestamp(thumbnails))
    }

    @Test
    fun negativeTimestampThumbnailIsRejected() {
        val thumbnails = listOf(
            DeArrowThumbnail(timestamp = -0.5, locked = false, votes = 5, original = false)
        )
        assertNull(DeArrowSelectionPolicy.selectThumbnailTimestamp(thumbnails))
    }

    @Test
    fun zeroTimestampThumbnailIsAccepted() {
        val thumbnails = listOf(
            DeArrowThumbnail(timestamp = 0.0, locked = false, votes = 5, original = false)
        )
        assertEquals(0.0, DeArrowSelectionPolicy.selectThumbnailTimestamp(thumbnails))
    }

    @Test
    fun originalThumbnailsAreNeverSelected() {
        val thumbnails = listOf(
            DeArrowThumbnail(timestamp = 12.0, locked = false, votes = 100, original = true)
        )
        assertNull(DeArrowSelectionPolicy.selectThumbnailTimestamp(thumbnails))
    }

    @Test
    fun emptyThumbnailListReturnsNull() {
        assertNull(DeArrowSelectionPolicy.selectThumbnailTimestamp(emptyList()))
    }
}
