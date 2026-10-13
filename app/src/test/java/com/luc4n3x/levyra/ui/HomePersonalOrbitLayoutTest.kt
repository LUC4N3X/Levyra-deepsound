package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePersonalOrbitLayoutTest {
    @Test
    fun `orbit lays tracks out as three by three pages in order`() {
        val pages = homePersonalOrbitPages(tracks(9))

        assertEquals(
            listOf(
                listOf(
                    listOf(0, 1, 2),
                    listOf(3, 4, 5),
                    listOf(6, 7, 8)
                )
            ),
            pages.map { page -> page.map { row -> row.map(::indexOf) } }
        )
    }

    @Test
    fun `orbit keeps only full pages once there is more than one page`() {
        val pages = homePersonalOrbitPages(tracks(20), limit = 20)

        assertEquals(2, pages.size)
        assertTrue(pages.all { page -> page.size == 3 && page.all { row -> row.size == 3 } })
        assertEquals((0 until 18).toList(), pages.flatten().flatten().map(::indexOf))
    }

    @Test
    fun `a single short page keeps every track`() {
        val pages = homePersonalOrbitPages(tracks(5))

        assertEquals(listOf(listOf(listOf(0, 1, 2), listOf(3, 4))), pages.map { page -> page.map { row -> row.map(::indexOf) } })
    }

    @Test
    fun `orbit keeps a partial second swipe page`() {
        val pages = homePersonalOrbitPages(tracks(12))

        assertEquals(2, pages.size)
        assertEquals(
            listOf(
                listOf(listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8)),
                listOf(listOf(9, 10, 11))
            ),
            pages.map { page -> page.map { row -> row.map(::indexOf) } }
        )
    }

    @Test
    fun `orbit exposes the tenth track on a second page`() {
        val pages = homePersonalOrbitPages(tracks(10))

        assertEquals(2, pages.size)
        assertEquals(listOf(9), pages.last().flatten().map(::indexOf))
    }

    @Test
    fun `orbit fills both pages without borrowing from quick picks`() {
        val catalog = tracks(35)
        val quickPicks = listOf(catalog[1], catalog[12], catalog[13], catalog[17], catalog[23])
        val filled = homePersonalOrbitFilledTracks(
            tracks = catalog.take(13),
            recommendations = catalog.drop(10),
            excludedTracks = quickPicks
        )

        assertEquals(18, filled.size)
        assertEquals(2, homePersonalOrbitPages(filled).size)
        assertTrue(homePersonalOrbitPages(filled).all { it.flatten().size == 9 })
        assertEquals((0..12).filterNot { it == 1 || it == 12 }, filled.take(11).map(::indexOf))
        assertTrue(filled.none { it in quickPicks })
        assertEquals(filled.size, filled.map(::indexOf).distinct().size)
    }

    @Test
    fun `orbit avoids other versions of quick picks songs`() {
        val catalog = tracks(28)
        val liveVersion = catalog[2].copy(title = "Orbit 2 (Live)")
        val filled = homePersonalOrbitFilledTracks(
            tracks = catalog.take(11),
            recommendations = listOf(liveVersion) + catalog.drop(11),
            excludedTracks = listOf(catalog[2])
        )

        assertEquals(18, filled.size)
        assertTrue(filled.none { it.title.startsWith("Orbit 2") })
    }

    @Test
    fun `orbit does not invent backfill without distinct recommendations`() {
        val catalog = tracks(10)
        val filled = homePersonalOrbitFilledTracks(
            tracks = catalog,
            recommendations = catalog.take(3),
            excludedTracks = catalog.take(2)
        )

        assertEquals((2..9).toList(), filled.map(::indexOf))
        assertTrue(
            homePersonalOrbitFilledTracks(emptyList(), catalog, emptyList()).isEmpty()
        )
    }

    @Test
    fun `orbit accepts an empty source`() {
        assertTrue(homePersonalOrbitPages(emptyList()).isEmpty())
        assertTrue(homePersonalOrbitPages(tracks(9), limit = 0).isEmpty())
    }

    @Test
    fun `orbit deduplicates repeated recordings before layout`() {
        val source = tracks(5).toMutableList().apply { add(1, get(0)) }

        val flattened = homePersonalOrbitPages(source, limit = 20).flatten().flatten()

        assertEquals(listOf(0, 1, 2, 3, 4), flattened.map(::indexOf))
    }

    @Test
    fun `orbit initial comes from the first letter of the name`() {
        assertEquals("L", homePersonalOrbitInitial("  luca "))
        assertEquals("É", homePersonalOrbitInitial("élodie"))
        assertNull(homePersonalOrbitInitial(""))
        assertNull(homePersonalOrbitInitial("   "))
    }

    private fun tracks(count: Int): List<Track> = (0 until count).map(::track)

    private fun indexOf(track: Track): Int = track.title.removePrefix("Orbit ").toInt()

    private fun track(index: Int): Track {
        val id = index.toString().padStart(11, '0')
        return Track(
            id = id,
            title = "Orbit $index",
            artist = "Artist $index",
            album = "Album $index",
            durationMs = 180_000L,
            streamUrl = "",
            videoUrl = "https://music.youtube.com/watch?v=$id",
            thumbnailUrl = "https://example.com/$id.jpg",
            largeThumbnailUrl = "https://example.com/$id.jpg",
            source = "YouTube Music",
            moodTags = emptySet(),
            energy = 50,
            vocal = 50,
            replayScore = 50,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0
        )
    }
}
