package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePersonalOrbitLayoutTest {
    @Test
    fun `orbit keeps sequential four row columns including a partial tail`() {
        val columns = homePersonalOrbitColumns(tracks(9))

        assertEquals(
            listOf(
                listOf(0, 1, 2, 3),
                listOf(4, 5, 6, 7),
                listOf(8)
            ),
            columns.map { column -> column.map(::indexOf) }
        )
    }

    @Test
    fun `orbit keeps twenty tracks as five complete columns`() {
        assertEquals(5, homePersonalOrbitColumns(tracks(20), limit = 20).size)
    }

    @Test
    fun `orbit accepts an empty source`() {
        assertTrue(homePersonalOrbitColumns(emptyList()).isEmpty())
    }

    @Test
    fun `orbit deduplicates repeated recordings before layout`() {
        val source = tracks(5).toMutableList().apply { add(1, get(0)) }

        val flattened = homePersonalOrbitColumns(source, limit = 20).flatten()

        assertEquals(listOf(0, 1, 2, 3, 4), flattened.map(::indexOf))
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
