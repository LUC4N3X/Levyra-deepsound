package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeQuickAccessLayoutTest {
    @Test
    fun keepsPartialSecondPageInsteadOfDroppingIt() {
        val tracks = (0 until 19).map { index -> track(index) }

        val columns = homeQuickAccessColumns(tracks)

        assertEquals(4, columns.size)
        assertEquals(listOf(0, 2, 4, 6, 8), columns[0].map(::indexOf))
        assertEquals(listOf(1, 3, 5, 7, 9), columns[1].map(::indexOf))
        assertEquals(listOf(10, 12, 14, 16, 18), columns[2].map(::indexOf))
        assertEquals(listOf(11, 13, 15, 17), columns[3].map(::indexOf))
    }

    @Test
    fun keepsTheExistingTwoColumnVisualOrder() {
        val tracks = (0 until 10).map { index -> track(index) }

        val columns = homeQuickAccessColumns(tracks)

        assertEquals(listOf(0, 2, 4, 6, 8), columns[0].map(::indexOf))
        assertEquals(listOf(1, 3, 5, 7, 9), columns[1].map(::indexOf))
    }

    private fun indexOf(track: Track): Int = track.title.removePrefix("Title ").toInt()

    private fun track(index: Int): Track {
        val id = index.toString().padStart(11, '0')
        return Track(
            id = id,
            title = "Title $index",
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
