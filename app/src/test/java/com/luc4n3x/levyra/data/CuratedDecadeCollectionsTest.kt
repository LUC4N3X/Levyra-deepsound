package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ExploreCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedDecadeCollectionsTest {

    @Test
    fun `decade titles from every observed locale resolve to the decade`() {
        val expected = mapOf(
            "2010s" to 2010, "1960s" to 1960, "1980-е" to 1980, "1970-і" to 1970, "Anni 2010" to 2010,
            "Anni '90" to 1990, "Anni '60" to 1960, "שנות ה- 2000" to 2000, "שנות ה-80" to 1980,
            "Δεκαετία '00" to 2000, "Δεκαετία '70" to 1970, "Dekada '50" to 1950, "1950 年代" to 1950,
            "20 世纪 50 年代" to 1950, "1950년대" to 1950, "دهه ۱۹۵۰" to 1950, "ยุค 1950" to 1950
        )
        expected.forEach { (title, decade) -> assertEquals(title, decade, YoutubeMusicDecades.decadeOf(title)) }
        assertNull(YoutubeMusicDecades.decadeOf("Decades"))
        assertNull(YoutubeMusicDecades.decadeOf("موسيقى الخمسينات"))
    }

    @Test
    fun `decade cards keep the first playlist per decade in chronological order`() {
        val sections = listOf(
            section("2010s", playlist("A10"), playlist("B10")),
            section("2000s", playlist("A00")),
            section("Anni '90", playlist("A90")),
            section("1980s", playlist("A80", thumbnail = ""), playlist("B80")),
            section("1970s", playlist("A70")),
            section("1960s", playlist("A60")),
            section("1950s", playlist("A50")),
            section("Genres", playlist("G1"))
        )
        val cards = YoutubeMusicDecades.cards(sections)
        assertEquals(listOf("A60", "A70", "B80", "A90", "A00", "A10"), cards.map { it.playlistId })
        assertEquals("Anni '90", cards[3].subtitle)
        assertEquals("Title A90", cards[3].title)
        assertTrue(YoutubeMusicDecades.cards(emptyList()).isEmpty())
    }

    @Test
    fun `everyday moods skip categories missing from the locale and avoid duplicate playlists`() {
        val workout = YoutubeMusicEverydayMoods.params[0]
        val focus = YoutubeMusicEverydayMoods.params[1]
        val commute = YoutubeMusicEverydayMoods.params[5]
        val feelGood = YoutubeMusicEverydayMoods.params[6]
        val categories = listOf(
            ExploreCategory("Allenamento", workout),
            ExploreCategory("Viaggi", commute),
            ExploreCategory("Buonumore", feelGood),
            ExploreCategory("Natale", "ggMPOg1uX280STVTNVQwRGZ0")
        )
        val titles = YoutubeMusicEverydayMoods.available(categories)
        assertEquals(listOf(workout, commute, feelGood), titles.keys.toList())
        assertFalse(focus in titles)
        val cards = YoutubeMusicEverydayMoods.cards(
            titles,
            mapOf(
                workout to listOf(playlist("W1")),
                commute to listOf(playlist("Shared"), playlist("C2")),
                feelGood to listOf(playlist("Shared"), playlist("F2")),
                focus to listOf(playlist("X"))
            )
        )
        assertEquals(listOf("W1", "Shared", "F2"), cards.map { it.playlistId })
        assertEquals(listOf("Allenamento", "Viaggi", "Buonumore"), cards.map { it.subtitle })
    }

    @Test
    fun `cache round trip keeps valid cards and rejects invalid data`() {
        val snapshot = CuratedCollectionSnapshot(1_000L, listOf(CuratedCollectionCard("The Hits: '80s", "1980s", "RDCLAK5uy_a", "https://yt3.googleusercontent.com/a")))
        assertEquals(snapshot, decodeCuratedCollection(encodeCuratedCollection(snapshot)))
        val unsafe = snapshot.copy(cards = listOf(snapshot.cards[0].copy(thumbnailUrl = "http://example.com/a")))
        assertNull(decodeCuratedCollection(encodeCuratedCollection(unsafe)))
        assertNull(decodeCuratedCollection("not json"))
        assertTrue(snapshot.isFresh(1_000L + CURATED_COLLECTION_TTL_MS - 1))
        assertFalse(snapshot.isFresh(1_000L + CURATED_COLLECTION_TTL_MS))
    }

    private fun section(title: String, vararg playlists: YoutubeMusicMoodPlaylist) = YoutubeMusicMoodSection(title, playlists.toList())

    private fun playlist(id: String, thumbnail: String = "https://yt3.googleusercontent.com/$id") =
        YoutubeMusicMoodPlaylist(title = "Title $id", playlistId = id, browseId = "VL$id", description = "Playlist • YouTube Music", thumbnailUrl = thumbnail)
}
