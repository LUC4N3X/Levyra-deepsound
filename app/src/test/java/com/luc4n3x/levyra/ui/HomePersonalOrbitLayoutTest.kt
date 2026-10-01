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
