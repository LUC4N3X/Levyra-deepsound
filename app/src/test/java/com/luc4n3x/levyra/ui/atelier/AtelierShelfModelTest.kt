package com.luc4n3x.levyra.ui.atelier

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AtelierShelfModelTest {
    private fun track(id: String, album: String, artist: String = "Artist", cover: String = "https://x/$album.jpg") = Track(
        id = id, title = id, artist = artist, album = album, durationMs = 1000L, streamUrl = "", videoUrl = "",
        thumbnailUrl = cover, largeThumbnailUrl = "", source = "", moodTags = emptySet(), energy = 0, vocal = 0,
        replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0
    )

    @Test
    fun groupsTracksByAlbumAndSortsByTitle() {
        val shelf = buildAtelierShelf(
            listOf(track("1", "Zeta"), track("2", "Alpha"), track("3", "alpha"), track("2", "Alpha")),
            emptyList(), emptyList()
        )
        assertEquals(listOf("Alpha", "Zeta"), shelf.map { it.title })
        assertEquals(2, shelf.first().tracks.size)
    }

    @Test
    fun skipsItemsWithoutCoverOrAlbumAndAvoidsDuplicateSuggestions() {
        val shelf = buildAtelierShelf(
            listOf(track("1", ""), track("2", "NoCover", cover = ""), track("3", "Known")),
            emptyList(),
            listOf(AlbumHit("Known", "Artist", "", "https://x/k.jpg", "q"), AlbumHit("Fresh", "Artist", "", "https://x/f.jpg", "q"))
        )
        assertEquals(listOf("Fresh", "Known"), shelf.map { it.title })
    }

    @Test
    fun slotIsContinuousAcrossTheFrontPosition() {
        val before = FloatArray(AtelierSlot.SIZE)
        val after = FloatArray(AtelierSlot.SIZE)
        var depth = -3f
        while (depth < 3f) {
            AtelierSlot.fill(depth, 1f, before)
            AtelierSlot.fill(depth + 0.01f, 1f, after)
            assertTrue("x jumps at $depth", abs(before[AtelierSlot.X] - after[AtelierSlot.X]) < 0.05f)
            assertTrue("z jumps at $depth", abs(before[AtelierSlot.Z] - after[AtelierSlot.Z]) < 0.05f)
            assertTrue("yaw jumps at $depth", abs(before[AtelierSlot.YAW] - after[AtelierSlot.YAW]) < 2f)
            depth += 0.01f
        }
    }

    @Test
    fun frontSleeveIsTheNearestAndOpaqueOnceEntered() {
        val front = FloatArray(AtelierSlot.SIZE)
        val side = FloatArray(AtelierSlot.SIZE)
        AtelierSlot.fill(0f, 1f, front)
        AtelierSlot.fill(1f, 1f, side)
        assertTrue(front[AtelierSlot.Z] < side[AtelierSlot.Z])
        assertEquals(1f, front[AtelierSlot.ALPHA], 0.001f)
    }

    @Test
    fun quadContainsDetectsInsideAndOutside() {
        val quad = floatArrayOf(0f, 0f, 10f, 0f, 10f, 10f, 0f, 10f)
        assertTrue(atelierQuadContains(quad, 5f, 5f))
        assertFalse(atelierQuadContains(quad, 15f, 5f))
    }

    @Test
    fun armTracksGrooveInwardAsTheSongProgresses() {
        val outer = AtelierDeckGeometry.armAngleForRadius(AtelierDeckGeometry.grooveRadius(0f))
        val inner = AtelierDeckGeometry.armAngleForRadius(AtelierDeckGeometry.grooveRadius(1f))
        assertTrue(outer > AtelierDeckGeometry.RestAngle)
        assertTrue(inner > outer)
    }

    @Test
    fun stageClockIsOrderedAndComplete() {
        assertEquals(0f, AtelierStageClock.deckRise(0f), 0f)
        assertEquals(1f, AtelierStageClock.deckRise(1f), 0f)
        assertEquals(0f, AtelierStageClock.recordFlight(0.4f), 0f)
        assertEquals(1f, AtelierStageClock.recordFlight(1f), 0f)
        assertEquals(0f, AtelierStageClock.armEngage(0.85f), 0f)
        assertEquals(1f, AtelierStageClock.controls(1f), 0f)
    }

    @Test
    fun ambientToneStaysDarkEnoughForLightText() {
        val (primary, secondary) = atelierAmbientFrom(intArrayOf(0xFFFFFFFF.toInt(), 0xFFFF0000.toInt()))
        listOf(primary, secondary).forEach { color ->
            val high = maxOf(color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF)
            assertTrue(high <= 110)
        }
    }
}
