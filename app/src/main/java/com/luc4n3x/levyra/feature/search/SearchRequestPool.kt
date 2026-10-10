package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.SearchSuggestionBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal class SearchRequestPool<T>(private val scope: CoroutineScope) {
    private val inFlight = ConcurrentHashMap<String, Deferred<T>>()

    suspend fun await(key: String, cached: () -> T? = { null }, block: suspend () -> T): T {
        inFlight[key]?.let { running -> return running.await() }
        cached()?.let { value -> return value }
        return start(key, block).await()
    }

    private fun start(key: String, block: suspend () -> T): Deferred<T> {
        val created = scope.async(start = CoroutineStart.LAZY) { block() }
        val running = inFlight.putIfAbsent(key, created)
        if (running != null) {
            created.cancel()
            return running
        }
        created.invokeOnCompletion { inFlight.remove(key, created) }
        created.start()
        return created
    }
}

internal class DetachedSearchCalls(
    private val backend: SearchBackend,
    scope: CoroutineScope,
    private val suggestionCache: SearchMemoCache<SearchSuggestionBundle>
) {
    private val suggestionCalls = SearchRequestPool<SearchSuggestionBundle>(scope)
    private val overviewCalls = SearchRequestPool<SearchResults>(scope)
    private val sectionCalls = SearchRequestPool<SearchSectionPage>(scope)
    private val exactArtistCalls = SearchRequestPool<ArtistHit?>(scope)
    private val officialArtistCalls = SearchRequestPool<List<ArtistHit>>(scope)
    private val officialAlbumCalls = SearchRequestPool<List<AlbumHit>>(scope)

    suspend fun suggestions(query: String, languageCode: String, cacheKey: String): SearchSuggestionBundle =
        suggestionCalls.await(cacheKey, cached = { suggestionCache.get(cacheKey) }) {
            backend.suggestions(query, languageCode).also { bundle -> suggestionCache.put(cacheKey, bundle) }
        }

    suspend fun overview(query: String, languageCode: String, cacheKey: String): SearchResults =
        overviewCalls.await(cacheKey) { backend.overview(query, languageCode) }

    suspend fun section(
        filter: SearchFilter,
        query: String,
        languageCode: String,
        cacheKey: String,
        continuation: String
    ): SearchSectionPage =
        sectionCalls.await("$cacheKey|${filter.name}|$continuation") {
            backend.section(filter, query, languageCode, continuation)
        }

    suspend fun exactArtist(query: String, queryKey: String): ArtistHit? =
        exactArtistCalls.await(queryKey) { backend.exactArtist(query) }

    suspend fun officialArtists(candidates: List<ArtistHit>): List<ArtistHit> =
        officialArtistCalls.await(candidates.joinToString("|", transform = ::artistRequestKey)) {
            backend.officialArtists(candidates)
        }

    suspend fun officialAlbums(artist: ArtistHit): List<AlbumHit> =
        officialAlbumCalls.await(artistRequestKey(artist)) { backend.officialAlbums(artist) }

    private fun artistRequestKey(artist: ArtistHit): String =
        "${artist.browseId.trim().lowercase(Locale.ROOT)}:${searchQueryKey(artist.name)}"
}
