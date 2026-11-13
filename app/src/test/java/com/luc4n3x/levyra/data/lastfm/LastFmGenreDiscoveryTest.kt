package com.luc4n3x.levyra.data.lastfm

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.genreDiscoveryCopy
import com.luc4n3x.levyra.ui.i18n.genreDiscoveryLocalizationCodes
import com.luc4n3x.levyra.ui.lastFmAlbumHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LastFmGenreDiscoveryTest {

    @Test
    fun aboutStripsLinksAndMarkup() {
        val body = """{"tag":{"name":"jazz","wiki":{"summary":"Il <b>jazz</b> è nato a New Orleans. Ha influenzato tutto il Novecento. <a href=\"http://www.last.fm/tag/jazz\">Read more on Last.fm</a>.","content":""}}}"""
        val page = LastFmGenreDiscoveryParser.parseAbout(body) as LastFmTagPage.Found
        assertEquals("Il jazz è nato a New Orleans. Ha influenzato tutto il Novecento.", page.value)
    }

    @Test
    fun aboutWithOnlyTheReadMoreLinkIsBlank() {
        val body = """{"tag":{"name":"afrobeats","wiki":{"summary":" <a href=\"http://www.last.fm/tag/afrobeats\">Read more on Last.fm</a>.","content":""}}}"""
        val page = LastFmGenreDiscoveryParser.parseAbout(body) as LastFmTagPage.Found
        assertEquals("", page.value)
    }

    @Test
    fun longAboutIsCutAtTheLastFullSentence() {
        val text = "First sentence here. Second sentence is longer than the first one. Third never fits"
        assertEquals("First sentence here.", LastFmGenreDiscoveryParser.cleanAbout(text, 40))
        assertEquals(
            "First sentence here. Second sentence is longer than the first one.",
            LastFmGenreDiscoveryParser.cleanAbout(text, 80)
        )
        assertEquals("", LastFmGenreDiscoveryParser.cleanAbout("no sentence end at all in this window", 10))
    }

    @Test
    fun topArtistsAreNamedAndDistinct() {
        val body = """{"topartists":{"artist":[
            {"name":"Kanye West","image":[]},
            {"name":"kanye west","image":[]},
            {"name":"","image":[]},
            {"name":"(null)","image":[]},
            {"name":"Kendrick Lamar","image":[]}
        ]}}"""
        val page = LastFmGenreDiscoveryParser.parseTopArtists(body) as LastFmTagPage.Found
        assertEquals(listOf("Kanye West", "Kendrick Lamar"), page.value)
    }

    @Test
    fun topAlbumsKeepOnlyRealArtwork() {
        val body = """{"albums":{"album":[
            {"name":"Scarlet","artist":{"name":"Doja Cat"},"image":[
                {"#text":"https://lastfm.freetls.fastly.net/i/u/174s/aaa.png","size":"large"},
                {"#text":"https://lastfm.freetls.fastly.net/i/u/300x300/aaa.png","size":"extralarge"}]},
            {"name":"Placeholder","artist":{"name":"Someone"},"image":[
                {"#text":"https://lastfm.freetls.fastly.net/i/u/300x300/2a96cbd8b46e442fc41c2b86b821562f.png","size":"extralarge"}]},
            {"name":"No Image","artist":{"name":"Someone"},"image":[{"#text":"","size":"extralarge"}]},
            {"name":"Orphan","image":[{"#text":"https://lastfm.freetls.fastly.net/i/u/300x300/bbb.png","size":"extralarge"}]}
        ]}}"""
        val page = LastFmGenreDiscoveryParser.parseTopAlbums(body) as LastFmTagPage.Found
        assertEquals(
            listOf(LastFmDiscoveryAlbum("Scarlet", "Doja Cat", "https://lastfm.freetls.fastly.net/i/u/300x300/aaa.png")),
            page.value
        )
    }

    @Test
    fun errorsAreDistinctFromEmptyResults() {
        assertEquals(LastFmTagPage.RateLimited, LastFmGenreDiscoveryParser.parseTopArtists("""{"error":29,"message":"Rate limit"}"""))
        assertTrue(LastFmGenreDiscoveryParser.parseTopAlbums("""{"error":6,"message":"Tag not found"}""") is LastFmTagPage.Failed)
        assertTrue(LastFmGenreDiscoveryParser.parseAbout("<html>") is LastFmTagPage.Failed)
        val empty = LastFmGenreDiscoveryParser.parseTopAlbums("""{"albums":{"album":[]}}""") as LastFmTagPage.Found
        assertTrue(empty.value.isEmpty())
    }

    @Test
    fun languageAndInterleavingHelpers() {
        assertEquals("pt", lastFmLanguage("pt-BR"))
        assertEquals("zh", lastFmLanguage("zh-Hant"))
        assertEquals("en", lastFmLanguage(""))
        assertEquals(
            listOf("A", "X", "B", "C"),
            interleaveDistinct(listOf(listOf("A", "B", "C"), listOf("X", "b", "a")), 10)
        )
        assertEquals(listOf("A", "X"), interleaveDistinct(listOf(listOf("A", "B"), listOf("X")), 2))
    }

    @Test
    fun lastFmAlbumsOpenThroughAlbumSearch() {
        val hit = lastFmAlbumHit(LastFmDiscoveryAlbum("Scarlet", "Doja Cat", "https://img/a.png"))
        assertEquals("", hit.browseId)
        assertEquals("Scarlet Doja Cat album", hit.query)
        assertEquals("https://img/a.png", hit.thumbnailUrl)
    }

    @Test
    fun discoveryEmptinessIgnoresBlankAbout() {
        assertTrue(LastFmGenreDiscovery(" ", emptyList(), emptyList()).isEmpty)
        assertFalse(LastFmGenreDiscovery("", listOf("Artist"), emptyList()).isEmpty)
    }

    @Test
    fun failedSectionsKeepTheStoredCopyAndAreNotCachedAsFresh() {
        val stored = LastFmGenreDiscovery("Saved text.", listOf("Saved Artist"), listOf(LastFmDiscoveryAlbum("Saved", "Saved Artist", "https://img/s.png")))

        val failed = mergeLastFmDiscovery(null, listOf(null, null), listOf(null), stored, 20)
        assertEquals(stored, failed.discovery)
        assertFalse(failed.complete)

        val partial = mergeLastFmDiscovery("New text.", listOf(listOf("New Artist")), listOf(null), stored, 20)
        assertEquals(listOf("New Artist"), partial.discovery.artists)
        assertEquals(stored.albums, partial.discovery.albums)
        assertFalse(partial.complete)

        val emptySuccess = mergeLastFmDiscovery("", listOf(emptyList()), listOf(emptyList()), stored, 20)
        assertTrue(emptySuccess.discovery.isEmpty)
        assertTrue(emptySuccess.complete)
    }

    @Test
    fun memoryCacheIsBoundedAndKeepsRecentEntries() {
        val cache = lastFmMemoryCache<Int>(2)
        cache["a"] = 1
        cache["b"] = 2
        cache["a"]
        cache["c"] = 3
        assertEquals(setOf("a", "c"), cache.keys)
    }

    @Test
    fun everySelectableLanguageShipsDiscoveryCopy() {
        val selectable = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertTrue((selectable - genreDiscoveryLocalizationCodes()).toString(), genreDiscoveryLocalizationCodes().containsAll(selectable))
        assertEquals(42, genreDiscoveryLocalizationCodes().size)
        val english = genreDiscoveryCopy("en")
        selectable.forEach { code ->
            val copy = genreDiscoveryCopy(code)
            val values = listOf(copy.aboutTitle, copy.readMore, copy.readLess, copy.aboutCredit, copy.artistsTitle, copy.albumsTitle, copy.appleCredit)
            assertTrue(code, values.all(String::isNotBlank))
            assertTrue(code, copy.appleCredit.endsWith("Apple Music"))
            assertTrue(code, copy.aboutCredit.contains("Last.fm") && copy.aboutCredit.contains("CC BY-SA"))
            if (code != "en") {
                assertTrue(code, copy.aboutTitle != english.aboutTitle && copy.artistsTitle != english.artistsTitle && copy.albumsTitle != english.albumsTitle)
                assertTrue(code, copy.appleCredit != english.appleCredit)
            }
        }
    }
}
