package com.luc4n3x.levyra.data.lastfm

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.feature.discovery.track
import com.luc4n3x.levyra.ui.i18n.genreChartCopy
import com.luc4n3x.levyra.ui.i18n.genreChartGenreNameCodes
import com.luc4n3x.levyra.ui.i18n.genreChartGenreNames
import com.luc4n3x.levyra.ui.i18n.genreChartLocalizationCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LastFmGenreChartsTest {

    @Test
    fun parsesTopTracksInRankOrderWithDurations() {
        val page = LastFmGenreChartParser.parseTopTracks(topTracksBody) as LastFmChartPage.Found

        assertEquals(listOf("Thinkin Bout You", "Pink + White"), page.entries.map { it.title })
        assertEquals("Frank Ocean", page.entries.first().artist)
        assertEquals(200_000L, page.entries.first().durationMs)
        assertEquals(0L, page.entries.last().durationMs)
    }

    @Test
    fun skipsEntriesWithoutTitleOrArtist() {
        val body = """{"tracks":{"track":[{"name":"","artist":{"name":"A"}},{"name":"Song","artist":{}}]}}"""

        val page = LastFmGenreChartParser.parseTopTracks(body) as LastFmChartPage.Found

        assertTrue(page.entries.isEmpty())
    }

    @Test
    fun distinguishesRateLimitFromOtherErrors() {
        assertEquals(LastFmChartPage.RateLimited, LastFmGenreChartParser.parseTopTracks("""{"error":29,"message":"Rate"}"""))
        assertTrue(LastFmGenreChartParser.parseTopTracks("""{"error":6,"message":"Missing"}""") is LastFmChartPage.Failed)
        assertTrue(LastFmGenreChartParser.parseTopTracks("<html>") is LastFmChartPage.Failed)
    }

    @Test
    fun mapsZonesAndLocalizedCategoryTitlesToTags() {
        assertEquals(listOf("rock", "alternative"), LastFmGenreTags.forZone("rock-alt"))
        assertTrue(LastFmGenreTags.forZone("nuove-uscite").isEmpty())
        assertEquals(listOf("rnb", "soul"), LastFmGenreTags.forCategoryTitle("R&B e soul"))
        assertEquals(listOf("electronic", "dance"), LastFmGenreTags.forCategoryTitle("Dance ed elettronica"))
        assertEquals(listOf("k-pop"), LastFmGenreTags.forCategoryTitle("K-Pop"))
        assertEquals(listOf("hip-hop"), LastFmGenreTags.forCategoryTitle("Хип-хоп"))
        assertEquals(listOf("classical"), LastFmGenreTags.forCategoryTitle("Классическая музыка"))
        assertEquals(listOf("rock"), LastFmGenreTags.forCategoryTitle("Classic Rock"))
        assertEquals(listOf("rock"), LastFmGenreTags.forCategoryTitle("Rock classique"))
        assertEquals(listOf("rock"), LastFmGenreTags.forCategoryTitle("Rock clásico"))
        assertEquals(listOf("rock"), LastFmGenreTags.forCategoryTitle("Классический рок"))
        assertEquals(listOf("classical"), LastFmGenreTags.forCategoryTitle("Classical"))
        assertEquals(listOf("classical"), LastFmGenreTags.forCategoryTitle("Musica classica"))
        assertEquals(listOf("classical"), LastFmGenreTags.forCategoryTitle("Musique classique"))
        assertEquals(listOf("classical"), LastFmGenreTags.forCategoryTitle("Klassik"))
        assertEquals(listOf("reggaeton"), LastFmGenreTags.forCategoryTitle("Reggaeton"))
        assertTrue(LastFmGenreTags.forCategoryTitle("Allenamento").isEmpty())
        assertTrue(LastFmGenreTags.forCategoryTitle("Trap").isEmpty())
    }

    @Test
    fun interleavesTagChartsWithoutDuplicates() {
        val rock = listOf(entry(1, "A", "X"), entry(2, "B", "Y"))
        val alternative = listOf(entry(1, "a", "x"), entry(2, "C", "Z"))

        val merged = interleaveLastFmCharts(listOf(rock, alternative), limit = 10)

        assertEquals(listOf("A", "B", "C"), merged.map { it.title })
        assertEquals(2, interleaveLastFmCharts(listOf(rock, alternative), limit = 2).size)
    }

    @Test
    fun acceptsOnlyConfidentPlayableMatches() {
        val reference = entry(1, "Thinkin Bout You", "Frank Ocean", durationMs = 200_000L)

        assertNotNull(acceptLastFmMatch(reference, track("Thinkin Bout You", "Frank Ocean", id = "vid1", durationMs = 201_000L)))
        assertNull(acceptLastFmMatch(reference, track("Thinkin Bout You", "Some Cover Band", id = "vid2", durationMs = 200_000L)))
        assertNull(acceptLastFmMatch(reference, track("Thinkin Bout You", "Frank Ocean", id = "vid3", durationMs = 320_000L)))
        assertNull(acceptLastFmMatch(reference, track("Thinkin Bout You (Live)", "Frank Ocean", id = "vid4", durationMs = 200_000L)))
        assertNull(acceptLastFmMatch(reference, track("Thinkin Bout You", "Frank Ocean", id = "", durationMs = 200_000L)))
        assertNull(acceptLastFmMatch(reference, null))
    }

    @Test
    fun everySelectableLanguageShipsGenreChartCopy() {
        val selectable = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertTrue((selectable - genreChartLocalizationCodes()).toString(), genreChartLocalizationCodes().containsAll(selectable))
        selectable.forEach { code ->
            val copy = genreChartCopy(code)
            assertTrue(code, listOf(copy.title, copy.subtitle, copy.credit, copy.loading, copy.showAll, copy.showLess).all(String::isNotBlank))
            assertTrue(code, copy.credit.contains("Last.fm"))
            assertTrue(code, copy.shelfTitle.isNotBlank() && copy.shelfSubtitle.isNotBlank())
            val names = genreChartGenreNames(code)
            assertTrue(code, listOf(names.kpop, names.metal, names.indie, names.jazz).all(String::isNotBlank))
        }
        assertTrue(genreChartGenreNameCodes().containsAll(selectable))
    }

    @Test
    fun shelfOnlyGenresMapToSingleLastFmTags() {
        assertEquals(listOf("k-pop"), LastFmGenreTags.forZone("chart-kpop"))
        assertEquals(listOf("metal"), LastFmGenreTags.forZone("chart-metal"))
        assertEquals(listOf("indie"), LastFmGenreTags.forZone("chart-indie"))
        assertEquals(listOf("jazz"), LastFmGenreTags.forZone("chart-jazz"))
    }

    private fun entry(rank: Int, title: String, artist: String, durationMs: Long = 0L) =
        LastFmChartEntry(rank = rank, title = title, artist = artist, durationMs = durationMs)

    private val topTracksBody = """
        {"tracks":{"track":[
          {"name":"Pink + White","duration":"0","artist":{"name":"Frank Ocean"},"@attr":{"rank":"2"}},
          {"name":"Thinkin Bout You","duration":"200","artist":{"name":"Frank Ocean"},"@attr":{"rank":"1"}}
        ],"@attr":{"tag":"rnb","page":"1"}}}
    """.trimIndent()
}
