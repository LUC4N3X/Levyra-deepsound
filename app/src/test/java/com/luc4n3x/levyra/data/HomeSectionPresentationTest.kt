package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSectionPresentationTest {
    @Test
    fun `rich sections never repeat the same presentation twice in a row`() {
        val presentations = (0 until 9).map { position ->
            HomeSectionLayoutPolicy.presentationFor(position = position, trackCount = 10)
        }
        presentations.zipWithNext().forEach { (previous, next) ->
            assertTrue("$previous repeated at consecutive positions", previous != next)
        }
        assertTrue(presentations.contains(HomeSectionPresentation.ArtworkRow))
        assertTrue(presentations.contains(HomeSectionPresentation.TrackGrid))
        assertTrue(presentations.contains(HomeSectionPresentation.ArtworkGrid))
    }

    @Test
    fun `short sections stay on the artwork row`() {
        assertEquals(
            HomeSectionPresentation.ArtworkRow,
            HomeSectionLayoutPolicy.presentationFor(position = 1, trackCount = 7)
        )
        assertEquals(
            HomeSectionPresentation.ArtworkRow,
            HomeSectionLayoutPolicy.presentationFor(position = 2, trackCount = 5)
        )
        assertEquals(
            HomeSectionPresentation.ArtworkRow,
            HomeSectionLayoutPolicy.presentationFor(position = 4, trackCount = 1)
        )
    }

    @Test
    fun `dense presentations require enough tracks to fill them`() {
        assertEquals(
            HomeSectionPresentation.TrackGrid,
            HomeSectionLayoutPolicy.presentationFor(position = 1, trackCount = 8)
        )
        assertEquals(
            HomeSectionPresentation.ArtworkGrid,
            HomeSectionLayoutPolicy.presentationFor(position = 2, trackCount = 6)
        )
    }

    @Test
    fun `long sections keep the dense track presentation within its bound`() {
        assertEquals(
            HomeSectionPresentation.TrackGrid,
            HomeSectionLayoutPolicy.presentationFor(position = 1, trackCount = 20)
        )
        assertEquals(
            HomeSectionPresentation.TrackGrid,
            HomeSectionLayoutPolicy.presentationFor(position = 3, trackCount = 40)
        )
        assertEquals(
            HomeSectionPresentation.TrackGrid,
            HomeSectionLayoutPolicy.presentationFor(
                position = 1,
                trackCount = HomeSectionLayoutPolicy.TRACK_GRID_CAPACITY
            )
        )
        assertEquals(
            HomeSectionPresentation.ArtworkRow,
            HomeSectionLayoutPolicy.presentationFor(
                position = 1,
                trackCount = HomeSectionLayoutPolicy.TRACK_GRID_CAPACITY + 1
            )
        )
        assertEquals(
            HomeSectionPresentation.ArtworkRow,
            HomeSectionLayoutPolicy.presentationFor(
                position = 2,
                trackCount = HomeSectionLayoutPolicy.ARTWORK_GRID_CAPACITY + 1
            )
        )
    }

    @Test
    fun `presentation is stable for the same position and size`() {
        repeat(4) {
            assertEquals(
                HomeSectionLayoutPolicy.presentationFor(position = 7, trackCount = 12),
                HomeSectionLayoutPolicy.presentationFor(position = 7, trackCount = 12)
            )
        }
    }

    @Test
    fun `invalid positions fall back to the artwork row`() {
        assertEquals(
            HomeSectionPresentation.ArtworkRow,
            HomeSectionLayoutPolicy.presentationFor(position = -1, trackCount = 30)
        )
    }
}
