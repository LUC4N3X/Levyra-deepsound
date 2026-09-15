package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.SearchSuggestionBundle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SearchProgressiveMergeTest {
    private fun session(query: String) = SearchSessionSnapshot(
        generation = 1L,
        query = query,
        queryKey = searchQueryKey(query),
        settled = false
    )

    @Test
    fun `strong local match stays on top when remote arrives and no duplicate is shown`() {
        val local = searchTestTrack("h1", "Hello", "Adele")
        val live = searchTestTrack("r1", "Hello Live at the Royal Albert Hall", "Adele", durationMs = 300_000L)
        val afterLocal = session("hello adele").withLocalMatches(listOf(LocalSearchMatch(local, 1_090, strong = true)))

        val afterRemote = afterLocal.withOverview(
            SearchResults(topTrack = live, songs = listOf(local, live)),
            "hello adele"
        )

        assertEquals("h1", afterRemote.results.topTrack?.id)
        assertEquals(listOf("h1", "r1"), afterRemote.results.songs.map { it.id })
        assertTrue(afterRemote.topLocked)
    }

    @Test
    fun `rich suggestion top is replaced once by the authoritative overview`() {
        val rich = searchTestTrack("s1", "Easy On Me", "Adele")
        val authoritative = searchTestTrack("r1", "Hello", "Adele", durationMs = 295_000L)
        val afterRich = session("adele").withSuggestionBundle(SearchSuggestionBundle(songs = listOf(rich)), "adele")
        assertEquals("s1", afterRich.results.topTrack?.id)
        assertFalse(afterRich.topLocked)

        val afterRemote = afterRich.withOverview(SearchResults(topTrack = authoritative, songs = listOf(authoritative)), "adele")
        val afterPage = afterRemote.withSectionPage(
            SearchFilter.Songs,
            SearchSectionPage(songs = listOf(searchTestTrack("p1", "Skyfall", "Adele", durationMs = 286_000L))),
            nextContinuation = ""
        )

        assertEquals("r1", afterRemote.results.topTrack?.id)
        assertEquals("r1", afterPage.results.topTrack?.id)
        assertEquals(listOf("s1", "r1", "p1"), afterPage.results.songs.map { it.id })
    }

    @Test
    fun `carried results survive query only suggestions and are replaced by fresh data`() {
        val stale = searchTestTrack("old", "Old Song", "Old Artist")
        val carried = session("geolier").copy(results = SearchResults(songs = listOf(stale)), carriedOver = true)

        val afterQueries = carried.withSuggestionBundle(SearchSuggestionBundle(queries = listOf("geolier sirio")), "geolier")
        assertTrue(afterQueries.carriedOver)
        assertEquals(listOf("old"), afterQueries.results.songs.map { it.id })
        assertEquals(listOf("geolier sirio"), afterQueries.suggestions)

        val fresh = searchTestTrack("g1", "I P' ME", "Geolier")
        val afterLocal = afterQueries.withLocalMatches(listOf(LocalSearchMatch(fresh, 500, strong = false)))
        assertFalse(afterLocal.carriedOver)
        assertEquals(listOf("g1"), afterLocal.results.songs.map { it.id })
    }

    @Test
    fun `failures during carry over stay pending and move to the fresh results`() {
        val stale = searchTestTrack("old", "Old Song", "Old Artist")
        val carried = session("geolier").copy(
            results = SearchResults(songs = listOf(stale), failedSections = setOf(SearchFilter.Playlists)),
            carriedOver = true
        )

        val failed = carried.withFailedSection(SearchFilter.Albums)
        assertTrue(failed.carriedOver)
        assertEquals(setOf(SearchFilter.Albums), failed.pendingSectionFailures)
        assertEquals(listOf("old"), failed.results.songs.map { it.id })

        val emptyPage = failed.withSectionPage(SearchFilter.Videos, SearchSectionPage(), nextContinuation = "")
        assertTrue(emptyPage.carriedOver)
        assertEquals(listOf("old"), emptyPage.results.songs.map { it.id })
        assertEquals("", emptyPage.sectionContinuations[SearchFilter.Videos])

        val fresh = emptyPage.withSectionPage(
            SearchFilter.Songs,
            SearchSectionPage(songs = listOf(searchTestTrack("g1", "I P' ME", "Geolier"))),
            nextContinuation = ""
        )
        assertFalse(fresh.carriedOver)
        assertEquals(listOf("g1"), fresh.results.songs.map { it.id })
        assertEquals(setOf(SearchFilter.Albums), fresh.results.failedSections)
    }

    @Test
    fun `failures after fresh data are recorded directly`() {
        val fresh = session("geolier").copy(results = SearchResults(songs = listOf(searchTestTrack("g1", "I P' ME", "Geolier"))))

        val failed = fresh.withFailedSection(SearchFilter.Albums)

        assertEquals(setOf(SearchFilter.Albums), failed.results.failedSections)
        assertTrue(failed.pendingSectionFailures.isEmpty())
    }

    @Test
    fun `verified artists fix placeholders without moving songs`() {
        val placeholder = searchTestTrack("p1", "Hello", "YouTube Music")
        val real = searchTestTrack("p2", "Skyfall", "Adele", durationMs = 286_000L)
        val adele = searchTestArtist("Adele", "UCadele", officialArtwork = true)
        val snapshot = session("adele").copy(results = SearchResults(topTrack = placeholder, songs = listOf(placeholder, real)))

        val verified = snapshot.withVerifiedArtists("adele", listOf(adele), artistsFailed = false)

        assertEquals(listOf("p1", "p2"), verified.results.songs.map { it.id })
        assertEquals("Adele", verified.results.songs.first().artist)
        assertEquals(listOf("UCadele"), verified.results.songs.first().artistBrowseIds)
        assertEquals("Adele", verified.results.songs.last().artist)
        assertEquals("Adele", verified.results.topTrack?.artist)
        assertEquals(listOf(adele), verified.results.artists)
        assertFalse(SearchFilter.Artists in verified.results.failedSections)
    }

    @Test
    fun `artist verification failure without candidates marks the section`() {
        val verified = session("adele").withVerifiedArtists("adele", emptyList(), artistsFailed = true)

        assertTrue(SearchFilter.Artists in verified.results.failedSections)
    }

    @Test
    fun `section page merges records continuation and clears the failure`() {
        val first = searchTestTrack("a1", "Hello", "Adele")
        val second = searchTestTrack("a2", "Skyfall", "Adele", durationMs = 286_000L)
        val snapshot = session("adele").copy(
            results = SearchResults(songs = listOf(first), failedSections = setOf(SearchFilter.Songs))
        )

        val paged = snapshot.withSectionPage(SearchFilter.Songs, SearchSectionPage(songs = listOf(first, second)), "tok")

        assertEquals(listOf("a1", "a2"), paged.results.songs.map { it.id })
        assertEquals("tok", paged.sectionContinuations[SearchFilter.Songs])
        assertFalse(SearchFilter.Songs in paged.results.failedSections)
    }

    @Test
    fun `official albums replace homonymous albums for an exact artist query`() = runBlocking {
        val impostor = searchTestAlbum("25", "Other Band", "MPRE_other")
        val official = searchTestAlbum("25", "Adele", "MPRE_official")
        val refinement = refineSearchAlbums("adele", listOf(searchTestArtist("Adele", "UCadele"))) { listOf(official) }
        val snapshot = session("adele").copy(results = SearchResults(albums = listOf(impostor)))

        val refined = snapshot.withAlbumRefinement(refinement)

        assertEquals(listOf("MPRE_official"), refined.results.albums.map { it.browseId })
        assertTrue(refined.verified)
    }

    @Test
    fun `official albums keep already paged albums`() = runBlocking {
        val paged = searchTestAlbum("Live", "Adele", "MPRE_live")
        val official = searchTestAlbum("25", "Adele", "MPRE_official")
        val refinement = refineSearchAlbums("adele", listOf(searchTestArtist("Adele", "UCadele"))) { listOf(official) }
        val snapshot = session("adele").copy(
            results = SearchResults(albums = listOf(paged)),
            sectionContinuations = mapOf(SearchFilter.Albums to "tok")
        )

        assertEquals(
            listOf("MPRE_official", "MPRE_live"),
            snapshot.withAlbumRefinement(refinement).results.albums.map { it.browseId }
        )
    }

    @Test
    fun `non artist query keeps displayed albums`() = runBlocking {
        val refinement = refineSearchAlbums("hello adele", listOf(searchTestArtist("Adele", "UCadele"))) {
            error("profile must not be requested")
        }
        val albums = listOf(searchTestAlbum("25", "Adele", "MPRE_25"))
        val snapshot = session("hello adele").copy(results = SearchResults(albums = albums))

        assertEquals(SearchAlbumRefinement.Keep, refinement)
        assertEquals(albums, snapshot.withAlbumRefinement(refinement).results.albums)
    }

    @Test
    fun `profile failure drops albums that only echo song titles`() = runBlocking {
        val refinement = refineSearchAlbums("adele", listOf(searchTestArtist("Adele", "UCadele"))) {
            throw IOException("profile unavailable")
        }
        val snapshot = session("adele").copy(
            results = SearchResults(
                songs = listOf(searchTestTrack("h1", "Hello", "Adele")),
                albums = listOf(searchTestAlbum("Hello", "Adele", "MPRE_single"), searchTestAlbum("25", "Adele", "MPRE_25"))
            )
        )

        assertEquals(SearchAlbumRefinement.DropSongTitleEchoes, refinement)
        assertEquals(listOf("MPRE_25"), snapshot.withAlbumRefinement(refinement).results.albums.map { it.browseId })
    }

    @Test
    fun `fast and verified copies of the same song collapse into the richer one`() {
        val fast = searchTestTrack("s1", "Hello", "Adele")
        val verified = searchTestTrack("s1", "Hello", "Adele", thumbnailUrl = "https://example.com/s1.jpg")

        val merged = mergeFastSearchResults(SearchResults(songs = listOf(fast)), SearchResults(songs = listOf(verified)), "adele")

        assertEquals(1, merged.songs.size)
        assertEquals("https://example.com/s1.jpg", merged.songs.single().thumbnailUrl)
    }

    @Test
    fun `query suggestions avoid entities already shown as rich results`() {
        val bundle = SearchSuggestionBundle(
            queries = listOf("hello adele", "adele", "adele live"),
            songs = listOf(searchTestTrack("h1", "Hello", "Adele")),
            artists = listOf(searchTestArtist("Adele", "UCadele"))
        )

        val snapshot = session("adel").withSuggestionBundle(bundle, "adel")

        assertEquals(listOf("adele live"), snapshot.suggestions)
    }

    @Test
    fun `verified snapshots ignore late rich items`() {
        val verified = session("adele").copy(
            results = SearchResults(songs = listOf(searchTestTrack("v1", "Hello", "Adele"))),
            verified = true
        )

        val updated = verified.withSuggestionBundle(
            SearchSuggestionBundle(artists = listOf(searchTestArtist("Adele Tribute", "UCfake"))),
            "adele"
        )

        assertTrue(updated.results.artists.isEmpty())
        assertEquals(listOf("v1"), updated.results.songs.map { it.id })
    }
}
