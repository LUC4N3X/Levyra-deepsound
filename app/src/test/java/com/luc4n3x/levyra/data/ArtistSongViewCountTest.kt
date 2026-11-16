package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistSongViewCountTest {
    @Test
    fun readsPlaysColumnAfterArtist() {
        assertEquals(2_100_000_000L, artistSongViewCount(listOf("Blank Space", "Taylor Swift", "2,1 млрд прослушиваний", "1989"), "1989"))
        assertEquals(845_000_000L, artistSongViewCount(listOf("Shake It Off", "Taylor Swift", "845M plays", "1989"), "1989"))
        assertEquals(12_000_000L, artistSongViewCount(listOf("Talk Talk", "Talk Talk", "12M plays", "The Colour of Spring"), "The Colour of Spring"))
    }

    @Test
    fun ignoresNumericAlbumTitleWithoutPlays() {
        assertEquals(-1L, artistSongViewCount(listOf("Blank Space", "Taylor Swift", "1989"), "1989"))
        assertEquals(-1L, artistSongViewCount(listOf("Song", "Artist"), null))
        assertEquals(-1L, artistSongViewCount(listOf("Blank Space", "Taylor Swift", "1989 (Taylor's Version)"), null))
        assertEquals(-1L, artistSongViewCount(listOf("Minority", "Green Day", "21st Century Breakdown"), null))
        assertEquals(5_123L, artistSongViewCount(listOf("Song", "Artist", "5,123 plays"), null))
    }

    @Test
    fun readsArtistTotalViewsFromDescriptionShelf() {
        val root = JSONObject("""{"contents":{"sectionListRenderer":{"contents":[{"musicDescriptionShelfRenderer":{"subheader":{"runs":[{"text":"46.864.371.906 visualizzazioni"}]},"description":{"runs":[{"text":"Bio"}]}}}]}}}""")
        assertEquals("46.864.371.906 visualizzazioni", ArtistRepository(YoutubeMusicRepository()).extractArtistViews(root))
        assertEquals("", ArtistRepository(YoutubeMusicRepository()).extractArtistViews(JSONObject("{}")))
    }
}
