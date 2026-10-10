package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.SearchSuggestionBundle
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class LevyraSearchEngineTest {
    private val dispatcher: ExecutorCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val backend = FakeSearchBackend()
    private val effects = RecordingSideEffects()

    @After
    fun tearDown() {
        scope.cancel()
        dispatcher.close()
    }

    private fun engine(timing: SearchTiming = SearchTiming(typingDebounceMs = 0L, suggestionWaitCapMs = 5_000L, prefetchDelayMs = 0L)) =
        LevyraSearchEngine(scope, backend, effects, dispatcher, timing)

    private suspend fun LevyraSearchEngine.awaitState(predicate: (SearchSessionSnapshot) -> Boolean): SearchSessionSnapshot =
        withTimeout(5_000L) { state.first(predicate) }

    private suspend fun awaitCondition(condition: () -> Boolean) {
        withTimeout(5_000L) { while (!condition()) delay(5L) }
    }

    @Test
    fun `late overview of an old query never overwrites the latest query`() = runBlocking {
        val engine = engine()
        engine.submit("adele", "it")
        awaitCondition { "adele" in backend.overviewCalls }
        backend.overview("coldplay").complete(SearchResults(songs = listOf(searchTestTrack("c1", "Yellow", "Coldplay"))))
        engine.submit("coldplay", "it")
        engine.awaitState { it.queryKey == "coldplay" && it.settled }

        backend.overview("adele").complete(SearchResults(songs = listOf(searchTestTrack("a1", "Hello", "Adele"))))
        delay(100L)

        val state = engine.state.value
        assertEquals("coldplay", state.queryKey)
        assertEquals(listOf("c1"), state.results.songs.map { it.id })
    }

    @Test
    fun `late artist verification of an old query is ignored`() = runBlocking {
        val adeleExact = CompletableDeferred<ArtistHit?>()
        backend.exactArtist = { query -> if (query == "adele") adeleExact.await() else null }
        backend.overview("adele").complete(
            SearchResults(
                songs = listOf(searchTestTrack("a1", "Hello", "Adele")),
                artists = listOf(searchTestArtist("Adele", "UCadele"))
            )
        )
        backend.overview("coldplay").complete(
            SearchResults(
                songs = listOf(searchTestTrack("c1", "Yellow", "Coldplay")),
                artists = listOf(searchTestArtist("Coldplay", "UCcoldplay"))
            )
        )
        val engine = engine()
        engine.submit("adele", "it")
        engine.awaitState { it.queryKey == "adele" && it.results.songs.isNotEmpty() && !it.verified }

        engine.submit("coldplay", "it")
        engine.awaitState { it.queryKey == "coldplay" && it.settled }
        adeleExact.complete(searchTestArtist("Adele", "UCadele", officialArtwork = true))
        delay(100L)

        val state = engine.state.value
        assertEquals("coldplay", state.queryKey)
        assertEquals(listOf("UCcoldplay"), state.results.artists.map { it.browseId })
    }

    @Test
    fun `rapid typing only reaches the network for the settled query`() = runBlocking {
        backend.overview("geolier").complete(SearchResults(songs = listOf(searchTestTrack("g1", "I P' ME", "Geolier"))))
        val engine = engine(SearchTiming(typingDebounceMs = 40L, suggestionWaitCapMs = 5_000L, prefetchDelayMs = 0L))

        listOf("g", "ge", "geo", "geol", "geoli", "geolie", "geolier").forEach { engine.onQueryChanged(it, "it") }
        engine.awaitState { it.queryKey == "geolier" && it.settled }

        assertEquals(listOf("geolier"), backend.suggestionCalls.toList())
        assertEquals(listOf("geolier"), backend.overviewCalls.toList())
    }

    @Test
    fun `repeated identical query does not restart the session`() = runBlocking {
        backend.overview("adele").complete(SearchResults(songs = listOf(searchTestTrack("a1", "Hello", "Adele"))))
        val engine = engine()
        engine.submit("adele", "it")
        val settled = engine.awaitState { it.queryKey == "adele" && it.settled }

        engine.submit("Adele ", "it")
        engine.onQueryChanged("ADELE", "it")
        delay(100L)

        assertEquals(1, backend.overviewCalls.size)
        assertEquals(settled.generation, engine.state.value.generation)
    }

    @Test
    fun `returning to a verified query is served from cache without network`() = runBlocking {
        backend.overview("adele").complete(SearchResults(songs = listOf(searchTestTrack("a1", "Hello", "Adele"))))
        backend.overview("coldplay").complete(SearchResults(songs = listOf(searchTestTrack("c1", "Yellow", "Coldplay"))))
        val engine = engine()
        engine.submit("adele", "it")
        engine.awaitState { it.queryKey == "adele" && it.settled }
        engine.submit("coldplay", "it")
        engine.awaitState { it.queryKey == "coldplay" && it.settled }

        engine.submit("adele", "it")
        val cached = engine.awaitState { it.queryKey == "adele" && it.settled }
        awaitCondition { effects.latency.size >= 3 }

        assertEquals(1, backend.overviewCalls.count { it == "adele" })
        assertEquals(listOf("a1"), cached.results.songs.map { it.id })
        assertTrue(cached.verified)
        assertTrue(effects.latency.last().cacheHit)
    }

    @Test
    fun `local match is visible before the remote overview answers`() = runBlocking {
        backend.local = listOf(LocalSearchCandidate(searchTestTrack("h1", "Hello", "Adele"), LocalSearchAffinity.RECENT))
        val engine = engine()
        engine.submit("hello adele", "it")

        val early = engine.awaitState { snapshot -> snapshot.results.songs.any { it.id == "h1" } }

        assertEquals("h1", early.results.topTrack?.id)
        assertFalse(early.settled)
        assertFalse(backend.overview("hello adele").isCompleted)
        assertNotNull(early.results.topTrack)
    }

    @Test
    fun `local and remote results merge without duplicates`() = runBlocking {
        val local = searchTestTrack("h1", "Hello", "Adele")
        val live = searchTestTrack("r1", "Hello Live at the Royal Albert Hall", "Adele", durationMs = 300_000L)
        backend.local = listOf(LocalSearchCandidate(local, LocalSearchAffinity.FAVORITE))
        val engine = engine()
        engine.submit("hello adele", "it")
        engine.awaitState { snapshot -> snapshot.results.songs.any { it.id == "h1" } }

        backend.overview("hello adele").complete(SearchResults(topTrack = live, songs = listOf(live, local)))
        val settled = engine.awaitState { it.queryKey.isNotEmpty() && it.settled }

        assertEquals(listOf("h1", "r1"), settled.results.songs.map { it.id })
        assertEquals("h1", settled.results.topTrack?.id)
    }

    @Test
    fun `failing albums section does not hide songs and artists`() = runBlocking {
        backend.overview("adele").completeExceptionally(IOException("overview down"))
        backend.section = { filter, _, _ ->
            when (filter) {
                SearchFilter.Songs -> SearchSectionPage(songs = listOf(searchTestTrack("a1", "Hello", "Adele")))
                SearchFilter.Artists -> SearchSectionPage(artists = listOf(searchTestArtist("Adele", "UCadele")))
                SearchFilter.Albums -> throw IOException("albums down")
                else -> SearchSectionPage()
            }
        }
        val engine = engine()
        engine.submit("adele", "it")

        val settled = engine.awaitState { it.queryKey.isNotEmpty() && it.settled }

        assertEquals(SearchFailure.None, settled.failure)
        assertEquals(listOf("a1"), settled.results.songs.map { it.id })
        assertEquals(listOf("UCadele"), settled.results.artists.map { it.browseId })
        assertTrue(SearchFilter.Albums in settled.results.failedSections)
        assertFalse(SearchFilter.Songs in settled.results.failedSections)
    }

    @Test
    fun `global failure is reported only when nothing useful exists`() = runBlocking {
        backend.overview("adele").completeExceptionally(IOException("overview down"))
        backend.section = { _, _, _ -> throw IOException("section down") }
        val engine = engine()
        engine.submit("adele", "it")

        val settled = engine.awaitState { it.queryKey.isNotEmpty() && it.settled }

        assertEquals(SearchFailure.Failed, settled.failure)
        assertTrue(settled.results.isEmpty)
    }

    @Test
    fun `empty provider answer is reported as no results`() = runBlocking {
        backend.overview("zzqx").complete(SearchResults())
        val engine = engine()
        engine.submit("zzqx", "it")

        assertEquals(SearchFailure.NoResults, engine.awaitState { it.queryKey.isNotEmpty() && it.settled }.failure)
    }

    @Test
    fun `overview is published before verification completes`() = runBlocking {
        val exact = CompletableDeferred<ArtistHit?>()
        backend.exactArtist = { exact.await() }
        backend.overview("adele").complete(
            SearchResults(
                songs = listOf(searchTestTrack("a1", "Hello", "Adele")),
                artists = listOf(searchTestArtist("Adele", "UCadele"))
            )
        )
        val engine = engine()
        engine.submit("adele", "it")

        val fast = engine.awaitState { it.results.songs.isNotEmpty() }
        assertFalse(fast.verified)
        assertEquals(listOf("UCadele"), fast.results.artists.map { it.browseId })

        exact.complete(searchTestArtist("Adele", "UCadele", officialArtwork = true))
        val verified = engine.awaitState { it.verified && it.settled }
        assertEquals(listOf("UCadele"), verified.results.artists.map { it.browseId })
        assertTrue(verified.results.artists.single().officialArtwork)
    }

    @Test
    fun `homonymous albums are replaced by the verified artist discography`() = runBlocking {
        backend.exactArtist = { searchTestArtist("Adele", "UCadele", officialArtwork = true) }
        backend.officialAlbums = { listOf(searchTestAlbum("25", "Adele", "MPRE_real")) }
        backend.overview("adele").complete(
            SearchResults(
                songs = listOf(searchTestTrack("a1", "Hello", "Adele")),
                albums = listOf(searchTestAlbum("25", "Imitator", "MPRE_fake"))
            )
        )
        val engine = engine()
        engine.submit("adele", "it")

        val verified = engine.awaitState { it.verified && it.settled }

        assertEquals(listOf("MPRE_real"), verified.results.albums.map { it.browseId })
    }

    @Test
    fun `rich suggestions seed the results while the full search runs`() = runBlocking {
        backend.suggestions = {
            SearchSuggestionBundle(
                queries = listOf("adele hello"),
                songs = listOf(searchTestTrack("s1", "Easy On Me", "Adele"))
            )
        }
        val engine = engine()
        engine.onQueryChanged("adele", "it")

        val early = engine.awaitState { snapshot -> snapshot.results.songs.any { it.id == "s1" } }

        assertEquals(listOf("adele hello"), early.suggestions)
        assertFalse(early.settled)
    }

    @Test
    fun `late suggestions of an old query are ignored`() = runBlocking {
        val adeleSuggestions = CompletableDeferred<SearchSuggestionBundle>()
        backend.suggestions = { query ->
            if (query == "adele") adeleSuggestions.await() else SearchSuggestionBundle(queries = listOf("coldplay yellow"))
        }
        backend.overview("coldplay").complete(SearchResults(songs = listOf(searchTestTrack("c1", "Yellow", "Coldplay"))))
        val engine = engine()
        engine.onQueryChanged("adele", "it")
        awaitCondition { "adele" in backend.suggestionCalls }
        engine.onQueryChanged("coldplay", "it")
        engine.awaitState { it.queryKey == "coldplay" && it.settled }

        adeleSuggestions.complete(SearchSuggestionBundle(queries = listOf("adele hello"), songs = listOf(searchTestTrack("a9", "Hello", "Adele"))))
        delay(100L)

        val state = engine.state.value
        assertEquals(listOf("coldplay yellow"), state.suggestions)
        assertTrue(state.results.songs.none { it.id == "a9" })
    }

    @Test
    fun `paging of an old query cannot leak into the new query`() = runBlocking {
        val adelePage = CompletableDeferred<SearchSectionPage>()
        backend.section = { filter, query, _ ->
            if (query == "adele" && filter == SearchFilter.Songs) adelePage.await() else SearchSectionPage()
        }
        backend.overview("adele").complete(SearchResults(songs = listOf(searchTestTrack("a1", "Hello", "Adele"))))
        backend.overview("coldplay").complete(SearchResults(songs = listOf(searchTestTrack("c1", "Yellow", "Coldplay"))))
        val engine = engine()
        engine.submit("adele", "it")
        engine.awaitState { it.queryKey == "adele" && it.settled }
        engine.selectFilter(SearchFilter.Songs)
        awaitCondition { backend.sectionCalls.any { it.filter == SearchFilter.Songs && it.query == "adele" } }

        engine.submit("coldplay", "it")
        engine.awaitState { it.queryKey == "coldplay" && it.settled }
        adelePage.complete(SearchSectionPage(songs = listOf(searchTestTrack("a2", "Skyfall", "Adele")), continuation = "tokA"))
        delay(100L)
        engine.loadMore(SearchFilter.Songs)
        delay(50L)

        val state = engine.state.value
        assertEquals(SearchFilter.All, state.filter)
        assertTrue(state.sectionContinuations.isEmpty())
        assertTrue(state.sectionLoading.isEmpty())
        assertTrue(state.results.songs.none { it.id == "a2" })
        assertTrue(backend.sectionCalls.none { it.continuation == "tokA" })
    }

    @Test
    fun `load more follows the continuation of the current query`() = runBlocking {
        backend.section = { _, _, continuation ->
            when (continuation) {
                "" -> SearchSectionPage(songs = listOf(searchTestTrack("a2", "Skyfall", "Adele")), continuation = "tok1")
                "tok1" -> SearchSectionPage(songs = listOf(searchTestTrack("a3", "Rolling In The Deep", "Adele")))
                else -> SearchSectionPage()
            }
        }
        backend.overview("adele").complete(SearchResults(songs = listOf(searchTestTrack("a1", "Hello", "Adele"))))
        val engine = engine()
        engine.submit("adele", "it")
        engine.awaitState { it.queryKey.isNotEmpty() && it.settled }
        engine.selectFilter(SearchFilter.Songs)
        engine.awaitState { it.sectionContinuations[SearchFilter.Songs] == "tok1" }

        engine.loadMore(SearchFilter.Songs)
        val paged = engine.awaitState { snapshot -> snapshot.results.songs.any { it.id == "a3" } }

        assertEquals(SectionCall(SearchFilter.Songs, "adele", "tok1"), backend.sectionCalls.last())
        assertEquals(listOf("a1", "a2", "a3"), paged.results.songs.map { it.id })
        assertEquals("", paged.sectionContinuations[SearchFilter.Songs])
    }

    @Test
    fun `prefetch runs after first paint and is cancelled by a new query`() = runBlocking {
        backend.overview("adele").complete(SearchResults(songs = listOf(searchTestTrack("a1", "Hello", "Adele"))))
        val engine = engine(SearchTiming(typingDebounceMs = 0L, suggestionWaitCapMs = 5_000L, prefetchDelayMs = 300L))
        engine.submit("adele", "it")
        engine.awaitState { it.results.songs.isNotEmpty() }

        engine.submit("coldplay", "it")
        delay(500L)

        assertTrue(effects.prefetched.isEmpty())
    }

    @Test
    fun `latency report marks the progressive milestones`() = runBlocking {
        backend.overview("adele").complete(
            SearchResults(
                songs = listOf(searchTestTrack("a1", "Hello", "Adele")),
                artists = listOf(searchTestArtist("Adele", "UCadele"))
            )
        )
        val engine = engine()
        engine.submit("adele", "it")
        engine.awaitState { it.queryKey.isNotEmpty() && it.settled }
        awaitCondition { effects.latency.isNotEmpty() }

        val report = effects.latency.last()
        listOf(
            SearchLatencyMark.NETWORK_FIRST,
            SearchLatencyMark.SONG_VISIBLE,
            SearchLatencyMark.ARTIST_VISIBLE,
            SearchLatencyMark.VERIFIED,
            SearchLatencyMark.SETTLED
        ).forEach { mark -> assertNotNull(mark.label, report.elapsedMs[mark]) }
        assertFalse(report.format().contains("adele"))
    }

    @Test
    fun `blocked non cancellable request of an old query does not delay the new query`() = runBlocking {
        val release = CountDownLatch(1)
        backend.blockingOverviews["geo"] = release
        backend.overview("geo").complete(SearchResults(songs = listOf(searchTestTrack("g0", "Geo Old", "Someone"))))
        backend.overview("geolier").complete(SearchResults(songs = listOf(searchTestTrack("g1", "I P' ME", "Geolier"))))
        val engine = engine()
        try {
            engine.submit("geo", "it")
            awaitCondition { "geo" in backend.overviewCalls }

            engine.submit("geolier", "it")
            val fresh = engine.awaitState { it.queryKey == "geolier" && it.settled }

            assertEquals(listOf("g1"), fresh.results.songs.map { it.id })
            assertEquals(1L, release.count)
        } finally {
            release.countDown()
        }
        delay(100L)

        val state = engine.state.value
        assertEquals("geolier", state.queryKey)
        assertTrue(state.results.songs.none { it.id == "g0" })
    }

    @Test
    fun `returning to a query while its request still runs reuses the in flight call`() = runBlocking {
        val release = CountDownLatch(1)
        backend.blockingSuggestions["adele"] = release
        backend.suggestions = { query -> SearchSuggestionBundle(queries = listOf("$query live")) }
        val engine = engine()
        try {
            engine.onQueryChanged("adele", "it")
            awaitCondition { "adele" in backend.suggestionCalls }
            engine.onQueryChanged("coldplay", "it")
            engine.awaitState { it.queryKey == "coldplay" && it.suggestions == listOf("coldplay live") }
            engine.onQueryChanged("adele", "it")
            engine.awaitState { it.queryKey == "adele" }
        } finally {
            release.countDown()
        }

        engine.awaitState { it.queryKey == "adele" && it.suggestions == listOf("adele live") }
        assertEquals(1, backend.suggestionCalls.count { it == "adele" })
    }

    @Test
    fun `section failures during carry over belong to the new query and survive fresh data`() = runBlocking {
        val freshSongs = CompletableDeferred<SearchSectionPage>()
        backend.overview("adele").completeExceptionally(IOException("overview down"))
        backend.overview("adel").completeExceptionally(IOException("overview down"))
        backend.section = { filter, query, _ ->
            when {
                query == "adele" && filter == SearchFilter.Songs ->
                    SearchSectionPage(songs = listOf(searchTestTrack("a1", "Hello", "Adele")))
                query == "adele" -> throw IOException("adele $filter down")
                filter == SearchFilter.Songs -> freshSongs.await()
                filter == SearchFilter.Artists || filter == SearchFilter.Albums -> throw IOException("adel $filter down")
                else -> SearchSectionPage()
            }
        }
        val engine = engine()
        engine.submit("adele", "it")
        val previous = engine.awaitState { it.queryKey == "adele" && it.settled }
        assertTrue(SearchFilter.Playlists in previous.results.failedSections)

        engine.submit("adel", "it")
        val carried = engine.awaitState { snapshot ->
            snapshot.queryKey == "adel" &&
                snapshot.pendingSectionFailures.containsAll(setOf(SearchFilter.Artists, SearchFilter.Albums)) &&
                SearchFilter.Videos in snapshot.sectionContinuations
        }
        assertTrue(carried.carriedOver)
        assertEquals(listOf("a1"), carried.results.songs.map { it.id })
        assertTrue(carried.results.failedSections.isEmpty())

        freshSongs.complete(SearchSectionPage(songs = listOf(searchTestTrack("n1", "Adel Song", "Adel"))))
        val settled = engine.awaitState { it.queryKey == "adel" && it.settled }

        assertEquals(listOf("n1"), settled.results.songs.map { it.id })
        assertEquals(setOf(SearchFilter.Artists, SearchFilter.Albums), settled.results.failedSections)
        assertEquals(SearchFailure.None, settled.failure)
    }
}

private data class SectionCall(val filter: SearchFilter, val query: String, val continuation: String)

private class FakeSearchBackend : SearchBackend {
    @Volatile
    var local: List<LocalSearchCandidate> = emptyList()

    @Volatile
    var suggestions: suspend (String) -> SearchSuggestionBundle = { SearchSuggestionBundle() }

    @Volatile
    var section: suspend (SearchFilter, String, String) -> SearchSectionPage = { _, _, _ -> SearchSectionPage() }

    @Volatile
    var exactArtist: suspend (String) -> ArtistHit? = { null }

    @Volatile
    var officialAlbums: suspend (ArtistHit) -> List<AlbumHit> = { emptyList() }

    val overviewCalls = CopyOnWriteArrayList<String>()
    val suggestionCalls = CopyOnWriteArrayList<String>()
    val sectionCalls = CopyOnWriteArrayList<SectionCall>()
    val blockingOverviews = ConcurrentHashMap<String, CountDownLatch>()
    val blockingSuggestions = ConcurrentHashMap<String, CountDownLatch>()
    private val overviews = ConcurrentHashMap<String, CompletableDeferred<SearchResults>>()

    fun overview(query: String): CompletableDeferred<SearchResults> =
        overviews.getOrPut(query) { CompletableDeferred() }

    override fun localCandidates(): List<LocalSearchCandidate> = local

    override suspend fun suggestions(query: String, languageCode: String): SearchSuggestionBundle {
        suggestionCalls += query
        blockingSuggestions[query]?.await()
        return suggestions.invoke(query)
    }

    override suspend fun overview(query: String, languageCode: String): SearchResults {
        overviewCalls += query
        blockingOverviews[query]?.await()
        return overview(query).await()
    }

    override suspend fun section(
        filter: SearchFilter,
        query: String,
        languageCode: String,
        continuation: String
    ): SearchSectionPage {
        sectionCalls += SectionCall(filter, query, continuation)
        return section.invoke(filter, query, continuation)
    }

    override suspend fun exactArtist(query: String): ArtistHit? = exactArtist.invoke(query)

    override suspend fun officialArtists(candidates: List<ArtistHit>): List<ArtistHit> = candidates

    override suspend fun officialAlbums(artist: ArtistHit): List<AlbumHit> = officialAlbums.invoke(artist)

    override fun overviewHedgeDelayMs(): Long = 60_000L
}

private class RecordingSideEffects : SearchSideEffects {
    val remoteTracks = CopyOnWriteArrayList<List<Track>>()
    val prefetched = CopyOnWriteArrayList<List<Track>>()
    val latency = CopyOnWriteArrayList<SearchLatencyReport>()

    override fun onRemoteTracks(tracks: List<Track>) {
        remoteTracks += tracks
    }

    override fun onPrefetch(tracks: List<Track>) {
        prefetched += tracks
    }

    override fun onLatency(report: SearchLatencyReport) {
        latency += report
    }
}
