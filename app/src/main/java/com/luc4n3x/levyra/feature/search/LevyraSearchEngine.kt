package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.data.deduplicateSearchSongs
import com.luc4n3x.levyra.data.mergeSearchArtists
import com.luc4n3x.levyra.data.nextSearchContinuation
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.SearchSuggestionBundle
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.viewmodel.mergeReliableArtistSearchResults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal data class SearchSectionPage(
    val songs: List<Track> = emptyList(),
    val videos: List<Track> = emptyList(),
    val albums: List<AlbumHit> = emptyList(),
    val artists: List<ArtistHit> = emptyList(),
    val playlists: List<PlaylistHit> = emptyList(),
    val continuation: String = ""
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && videos.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
}

internal interface SearchBackend {
    fun localCandidates(): List<LocalSearchCandidate>
    fun acceptsLocalTrack(track: Track): Boolean = true
    suspend fun suggestions(query: String, languageCode: String): SearchSuggestionBundle
    suspend fun overview(query: String, languageCode: String): SearchResults
    suspend fun section(filter: SearchFilter, query: String, languageCode: String, continuation: String): SearchSectionPage
    suspend fun exactArtist(query: String): ArtistHit?
    suspend fun officialArtists(candidates: List<ArtistHit>): List<ArtistHit>
    suspend fun officialAlbums(artist: ArtistHit): List<AlbumHit>
    fun overviewHedgeDelayMs(): Long
}

internal interface SearchSideEffects {
    fun onRemoteTracks(tracks: List<Track>) = Unit
    fun onPrefetch(tracks: List<Track>) = Unit
    fun onLatency(report: SearchLatencyReport) = Unit
}

internal enum class SearchFailure { None, NoResults, Failed }

internal data class SearchSessionSnapshot(
    val generation: Long = 0L,
    val query: String = "",
    val queryKey: String = "",
    val suggestions: List<String> = emptyList(),
    val results: SearchResults = SearchResults(),
    val carriedOver: Boolean = false,
    val topLocked: Boolean = false,
    val remoteLoading: Boolean = false,
    val verified: Boolean = false,
    val settled: Boolean = true,
    val failure: SearchFailure = SearchFailure.None,
    val filter: SearchFilter = SearchFilter.All,
    val sectionContinuations: Map<SearchFilter, String> = emptyMap(),
    val sectionLoading: Set<SearchFilter> = emptySet(),
    val pendingSectionFailures: Set<SearchFilter> = emptySet()
) {
    val pending: Boolean
        get() = queryKey.isNotEmpty() && !settled

    val freshResults: SearchResults
        get() = if (carriedOver) SearchResults(failedSections = pendingSectionFailures) else results
}

internal data class SearchTiming(
    val typingDebounceMs: Long = 75L,
    val suggestionWaitCapMs: Long = 450L,
    val prefetchDelayMs: Long = 350L,
    val suggestionTtlMs: Long = 5 * 60_000L,
    val resultTtlMs: Long = 5 * 60_000L,
    val sectionConcurrency: Int = 2
)

private data class SearchRequest(
    val query: String,
    val key: String,
    val languageCode: String,
    val nonce: Long
) {
    val cacheKey: String
        get() = "$languageCode|$key"

    val identity: String
        get() = "$cacheKey|$nonce"
}

private class ActiveSession(
    val generation: Long,
    val request: SearchRequest,
    val scope: CoroutineScope,
    val trace: SearchLatencyTrace,
    val sectionGate: Semaphore
)

internal class LevyraSearchEngine(
    private val scope: CoroutineScope,
    private val backend: SearchBackend,
    private val sideEffects: SearchSideEffects = object : SearchSideEffects {},
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timing: SearchTiming = SearchTiming(),
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L },
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val requests = MutableStateFlow(SearchRequest("", "", "", 0L))
    private val submittedIdentity = MutableStateFlow("")
    private val _state = MutableStateFlow(SearchSessionSnapshot())
    val state: StateFlow<SearchSessionSnapshot> = _state.asStateFlow()

    private val generations = AtomicLong(0L)
    private val nonces = AtomicLong(0L)
    private val suggestionCache = SearchMemoCache<SearchSuggestionBundle>(SUGGESTION_CACHE_ENTRIES, timing.suggestionTtlMs, clock)
    private val resultCache = SearchMemoCache<SearchResults>(RESULT_CACHE_ENTRIES, timing.resultTtlMs, clock)
    private val sectionCache = SearchMemoCache<SearchSectionPage>(SECTION_CACHE_ENTRIES, timing.resultTtlMs, clock)
    private val remote = DetachedSearchCalls(
        backend = backend,
        scope = CoroutineScope(SupervisorJob(scope.coroutineContext[Job]) + ioDispatcher),
        suggestionCache = suggestionCache
    )

    @Volatile
    private var activeSession: ActiveSession? = null

    init {
        scope.launch {
            requests
                .distinctUntilChanged { previous, next -> previous.identity == next.identity }
                .collectLatest { request -> runSession(request) }
        }
    }

    fun onQueryChanged(query: String, languageCode: String) {
        requests.value = requestFor(query, languageCode, nonces.get())
    }

    fun submit(query: String, languageCode: String) {
        val key = requestFor(query, languageCode, 0L).key
        if (key.isEmpty()) return
        val current = _state.value
        val retry = current.queryKey == key && current.failure == SearchFailure.Failed
        val nonce = if (retry) nonces.incrementAndGet() else nonces.get()
        val request = requestFor(query, languageCode, nonce)
        submittedIdentity.value = request.identity
        requests.value = request
    }

    fun reset() {
        requests.value = SearchRequest("", "", "", nonces.get())
    }

    fun selectFilter(filter: SearchFilter) {
        _state.update { it.copy(filter = filter) }
        if (filter != SearchFilter.All) loadSection(filter, initial = true)
    }

    fun loadMore(filter: SearchFilter) {
        loadSection(filter, initial = false)
    }

    fun patchResults(transform: (SearchResults) -> SearchResults) {
        _state.update { it.copy(results = transform(it.results)) }
    }

    private fun requestFor(query: String, languageCode: String, nonce: Long): SearchRequest {
        val trimmed = query.trim()
        val key = if (trimmed.length >= MIN_SEARCH_QUERY_CHARS) searchQueryKey(trimmed) else ""
        return if (key.isEmpty()) {
            SearchRequest("", "", languageCode, nonce)
        } else {
            SearchRequest(trimmed, key, languageCode, nonce)
        }
    }

    private suspend fun runSession(request: SearchRequest) {
        val generation = generations.incrementAndGet()
        if (request.key.isEmpty()) {
            _state.value = SearchSessionSnapshot(generation = generation)
            return
        }
        val cachedResults = resultCache.get(request.cacheKey)
        val cachedSuggestions = suggestionCache.get(request.cacheKey)
        _state.value = openingSnapshot(generation, request, cachedResults, cachedSuggestions)
        coroutineScope {
            val session = ActiveSession(
                generation = generation,
                request = request,
                scope = this,
                trace = SearchLatencyTrace(clock),
                sectionGate = Semaphore(timing.sectionConcurrency)
            )
            activeSession = session
            try {
                runStages(session, cachedResults, cachedSuggestions)
                awaitCancellation()
            } finally {
                if (activeSession === session) activeSession = null
            }
        }
    }

    private fun openingSnapshot(
        generation: Long,
        request: SearchRequest,
        cachedResults: SearchResults?,
        cachedSuggestions: SearchSuggestionBundle?
    ): SearchSessionSnapshot {
        val previous = _state.value
        val prefixSuggestions = if (cachedSuggestions == null) {
            suggestionCache.longestPrefix(request.cacheKey)?.let { narrowSuggestionBundle(it, request.query) }
        } else {
            null
        }
        val carryOver = cachedResults == null &&
            !previous.results.isEmpty &&
            areRelatedSearchKeys(previous.queryKey, request.key)
        val initialResults = cachedResults
            ?: if (carryOver) previous.results.copy(failedSections = emptySet()) else SearchResults()
        return SearchSessionSnapshot(
            generation = generation,
            query = request.query,
            queryKey = request.key,
            suggestions = mergeSearchQuerySuggestions(
                request.query,
                prefixSuggestions?.queries.orEmpty(),
                if (carryOver) emptySet() else searchEntityTextKeys(initialResults)
            ),
            results = initialResults,
            carriedOver = carryOver,
            topLocked = cachedResults != null,
            verified = cachedResults != null,
            settled = false
        )
    }

    private suspend fun runStages(
        session: ActiveSession,
        cachedResults: SearchResults?,
        cachedSuggestions: SearchSuggestionBundle?
    ) {
        session.scope.launch { publishLocalMatches(session) }
        if (cachedSuggestions != null) publishSuggestionBundle(session, cachedSuggestions)
        val loadsSuggestions = cachedSuggestions == null && !isSubmitted(session.request)
        if (cachedResults != null) {
            session.trace.markVisible(cachedResults)
            if (loadsSuggestions) session.scope.launch { loadSuggestions(session) }
            settle(session, remoteFailed = false, cacheHit = true)
            schedulePrefetch(session)
            return
        }
        if (loadsSuggestions) awaitFullSearchTrigger(session, session.scope.launch { loadSuggestions(session) })
        runRemoteSearch(session)
    }

    private fun isSubmitted(request: SearchRequest): Boolean = submittedIdentity.value == request.identity

    private suspend fun awaitFullSearchTrigger(session: ActiveSession, suggestions: Job) {
        if (isSubmitted(session.request)) return
        coroutineScope {
            val submitted = launch { submittedIdentity.first { it == session.request.identity } }
            val cap = launch { delay(timing.typingDebounceMs + timing.suggestionWaitCapMs) }
            select<Unit> {
                suggestions.onJoin {}
                submitted.onJoin {}
                cap.onJoin {}
            }
            submitted.cancel()
            cap.cancel()
        }
    }

    private suspend fun loadSuggestions(session: ActiveSession) {
        val request = session.request
        if (!isSubmitted(request)) delay(timing.typingDebounceMs)
        val bundle = searchCatching {
            remote.suggestions(request.query, request.languageCode, request.cacheKey)
        }.getOrNull() ?: return
        publishSuggestionBundle(session, bundle)
    }

    private fun publishSuggestionBundle(session: ActiveSession, bundle: SearchSuggestionBundle) {
        val query = session.request.query
        if (publish(session) { it.withSuggestionBundle(bundle, query) } != null) {
            session.trace.mark(SearchLatencyMark.SUGGESTION)
        }
    }

    private suspend fun publishLocalMatches(session: ActiveSession) {
        val request = session.request
        val matches = withContext(computeDispatcher) {
            matchLocalSearchTracks(
                query = request.query,
                candidates = backend.localCandidates(),
                accept = backend::acceptsLocalTrack
            )
        }
        if (matches.isEmpty()) return
        if (publish(session) { it.withLocalMatches(matches) } != null) {
            session.trace.mark(SearchLatencyMark.LOCAL)
        }
    }

    private suspend fun runRemoteSearch(session: ActiveSession) {
        val request = session.request
        publish(session) { it.copy(remoteLoading = true) }
        var remoteFailed = false
        coroutineScope {
            val exactArtist = async { searchCatching { remote.exactArtist(request.query, request.key) }.getOrNull() }
            val overview = async {
                searchCatching { remote.overview(request.query, request.languageCode, request.cacheKey) }
            }
            val hedge = launch { hedgeSongs(session, overview) }
            val raw = overview.await().getOrNull()
            hedge.cancel()
            if (raw != null) {
                applyOverview(session, raw)
                schedulePrefetch(session)
                runVerification(session, raw, exactArtist)
            } else {
                exactArtist.cancel()
                remoteFailed = !runSectionFallback(session)
            }
        }
        settle(session, remoteFailed, cacheHit = false)
    }

    private suspend fun fetchSection(request: SearchRequest, filter: SearchFilter, continuation: String): SearchSectionPage =
        remote.section(filter, request.query, request.languageCode, request.cacheKey, continuation)

    private suspend fun hedgeSongs(session: ActiveSession, overview: Deferred<*>) {
        delay(backend.overviewHedgeDelayMs())
        if (overview.isCompleted) return
        val page = searchCatching { fetchSection(session.request, SearchFilter.Songs, "") }.getOrNull() ?: return
        if (page.songs.isEmpty()) return
        applySectionPage(session, SearchFilter.Songs, page, requestedContinuation = "")
        session.trace.mark(SearchLatencyMark.NETWORK_FIRST)
    }

    private fun applyOverview(session: ActiveSession, raw: SearchResults) {
        val query = session.request.query
        if (publish(session) { it.withOverview(raw, query) } != null) {
            session.trace.mark(SearchLatencyMark.NETWORK_FIRST)
            if (raw.songs.isNotEmpty()) sideEffects.onRemoteTracks(raw.songs)
        }
    }

    private suspend fun runVerification(
        session: ActiveSession,
        raw: SearchResults,
        exactArtistLookup: Deferred<ArtistHit?>
    ) {
        val query = session.request.query
        val exactArtist = exactArtistLookup.await()
        val displayed = _state.value.takeIf { it.generation == session.generation }?.results?.artists.orEmpty()
        val candidates = mergeSearchArtists(raw.artists, displayed).filterNot { candidate ->
            exactArtist != null &&
                candidate.browseId.isNotBlank() &&
                candidate.browseId.equals(exactArtist.browseId, ignoreCase = true)
        }
        val official = searchCatching {
            mergeReliableArtistSearchResults(
                query = query,
                exactArtist = exactArtist,
                verifiedArtists = remote.officialArtists(candidates)
            )
        }.getOrNull()
        val resolvedArtists = official ?: raw.artists
        val artistsFailed = official == null && raw.artists.isEmpty()
        publish(session) { it.withVerifiedArtists(query, resolvedArtists, artistsFailed) }
        val refinement = refineSearchAlbums(query, resolvedArtists, remote::officialAlbums)
        val verified = publish(session) { it.withAlbumRefinement(refinement) } ?: return
        session.trace.mark(SearchLatencyMark.VERIFIED)
        if (!verified.results.isEmpty) resultCache.put(session.request.cacheKey, verified.results)
    }

    private suspend fun runSectionFallback(session: ActiveSession): Boolean {
        val request = session.request
        val songsDelivered = _state.value.let { snapshot ->
            snapshot.generation == session.generation && SearchFilter.Songs in snapshot.sectionContinuations
        }
        val successes = AtomicInteger(if (songsDelivered) 1 else 0)
        coroutineScope {
            FALLBACK_SECTION_ORDER
                .filterNot { songsDelivered && it == SearchFilter.Songs }
                .forEach { filter ->
                    launch {
                        session.sectionGate.withPermit {
                            searchCatching { fetchSection(request, filter, "") }
                                .onSuccess { page ->
                                    successes.incrementAndGet()
                                    sectionCache.put(sectionCacheKey(request, filter), page)
                                    applySectionPage(session, filter, page, requestedContinuation = "")
                                    session.trace.mark(SearchLatencyMark.NETWORK_FIRST)
                                }
                                .onFailure { publish(session) { snapshot -> snapshot.withFailedSection(filter) } }
                        }
                    }
                }
        }
        return successes.get() > 0
    }

    private fun applySectionPage(
        session: ActiveSession,
        filter: SearchFilter,
        page: SearchSectionPage,
        requestedContinuation: String,
        finishesLoading: Boolean = false
    ) {
        val next = nextSearchContinuation(requestedContinuation, page.continuation)
        val published = publish(session) { current ->
            val paged = current.withSectionPage(filter, page, next)
            if (finishesLoading) paged.copy(sectionLoading = paged.sectionLoading - filter) else paged
        } != null
        if (published && (filter == SearchFilter.Songs || filter == SearchFilter.Videos)) {
            val tracks = page.songs + page.videos
            if (tracks.isNotEmpty()) sideEffects.onRemoteTracks(tracks)
        }
    }

    private fun loadSection(filter: SearchFilter, initial: Boolean) {
        if (filter == SearchFilter.All) return
        val session = activeSession ?: return
        val snapshot = _state.value
        if (snapshot.generation != session.generation) return
        if (filter in snapshot.sectionLoading) return
        val paged = snapshot.sectionContinuations.containsKey(filter)
        val continuation = snapshot.sectionContinuations[filter].orEmpty()
        if (initial && paged) return
        if (!initial && (!paged || continuation.isBlank())) return
        if (publish(session) { it.copy(sectionLoading = it.sectionLoading + filter) } == null) return
        val request = session.request
        session.scope.launch {
            try {
                val cacheKey = sectionCacheKey(request, filter)
                val cached = if (initial) sectionCache.get(cacheKey) else null
                val outcome = if (cached != null) {
                    Result.success(cached)
                } else {
                    searchCatching { session.sectionGate.withPermit { fetchSection(request, filter, continuation) } }
                }
                outcome
                    .onSuccess { page ->
                        if (initial && cached == null) sectionCache.put(cacheKey, page)
                        applySectionPage(session, filter, page, continuation, finishesLoading = true)
                    }
                    .onFailure {
                        publish(session) { current ->
                            current.withFailedSection(filter).copy(sectionLoading = current.sectionLoading - filter)
                        }
                    }
            } finally {
                publish(session) { it.copy(sectionLoading = it.sectionLoading - filter) }
            }
        }
    }

    private fun schedulePrefetch(session: ActiveSession) {
        session.scope.launch {
            delay(timing.prefetchDelayMs)
            val snapshot = _state.value
            if (snapshot.generation != session.generation) return@launch
            val tracks = listOfNotNull(snapshot.results.topTrack) + snapshot.results.songs
            if (tracks.isNotEmpty()) sideEffects.onPrefetch(deduplicateSearchSongs(tracks))
        }
    }

    private fun settle(session: ActiveSession, remoteFailed: Boolean, cacheHit: Boolean) {
        publish(session) { current ->
            val results = current.freshResults
            current.copy(
                results = results,
                carriedOver = false,
                remoteLoading = false,
                settled = true,
                failure = when {
                    !results.isEmpty -> SearchFailure.None
                    remoteFailed -> SearchFailure.Failed
                    else -> SearchFailure.NoResults
                }
            )
        } ?: return
        session.trace.mark(SearchLatencyMark.SETTLED)
        sideEffects.onLatency(session.trace.report(session.request.query.length, cacheHit))
    }

    private fun publish(
        session: ActiveSession,
        transform: (SearchSessionSnapshot) -> SearchSessionSnapshot
    ): SearchSessionSnapshot? {
        var published: SearchSessionSnapshot? = null
        _state.update { current ->
            if (current.generation != session.generation) {
                published = null
                current
            } else {
                transform(current).also { published = it }
            }
        }
        val snapshot = published ?: return null
        if (!snapshot.carriedOver) session.trace.markVisible(snapshot.results)
        return snapshot
    }

    private fun sectionCacheKey(request: SearchRequest, filter: SearchFilter): String =
        "${request.cacheKey}|${filter.name}"

    private companion object {
        const val SUGGESTION_CACHE_ENTRIES = 48
        const val RESULT_CACHE_ENTRIES = 24
        const val SECTION_CACHE_ENTRIES = 32
        val FALLBACK_SECTION_ORDER = listOf(
            SearchFilter.Songs,
            SearchFilter.Artists,
            SearchFilter.Albums,
            SearchFilter.Playlists,
            SearchFilter.Videos
        )
    }
}
