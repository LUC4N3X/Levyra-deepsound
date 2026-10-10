package com.luc4n3x.levyra.feature.search

import com.luc4n3x.levyra.data.ArtistRepository
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.SearchSuggestionBundle
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.providers.LevyraProviderRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class YoutubeMusicSearchBackend(
    private val repository: YoutubeMusicRepository,
    private val artistRepository: ArtistRepository,
    private val providerRouter: LevyraProviderRouter,
    private val localCandidateSource: () -> List<LocalSearchCandidate>,
    private val playableTrack: (Track) -> Boolean
) : SearchBackend {
    override fun localCandidates(): List<LocalSearchCandidate> = localCandidateSource()

    override fun acceptsLocalTrack(track: Track): Boolean = playableTrack(track)

    override suspend fun suggestions(query: String, languageCode: String): SearchSuggestionBundle =
        withContext(Dispatchers.IO) { repository.searchSuggestionBundle(query, languageCode) }

    override suspend fun overview(query: String, languageCode: String): SearchResults =
        providerRouter.searchEverything(query, languageCode)

    override suspend fun section(
        filter: SearchFilter,
        query: String,
        languageCode: String,
        continuation: String
    ): SearchSectionPage = when (filter) {
        SearchFilter.Songs -> repository.searchSongsPage(query, languageCode, continuation)
            .let { page -> SearchSectionPage(songs = page.items, continuation = page.continuation) }
        SearchFilter.Videos -> repository.searchVideosPage(query, languageCode, continuation)
            .let { page -> SearchSectionPage(videos = page.items, continuation = page.continuation) }
        SearchFilter.Albums -> repository.searchAlbumsPage(query, languageCode, continuation)
            .let { page -> SearchSectionPage(albums = page.items, continuation = page.continuation) }
        SearchFilter.Artists -> repository.searchArtistsPage(query, languageCode, continuation)
            .let { page -> SearchSectionPage(artists = page.items, continuation = page.continuation) }
        SearchFilter.Playlists -> repository.searchPlaylistsPage(query, languageCode, continuation)
            .let { page -> SearchSectionPage(playlists = page.items, continuation = page.continuation) }
        SearchFilter.All -> SearchSectionPage()
    }

    override suspend fun exactArtist(query: String): ArtistHit? = artistRepository.artistHitFor(query)

    override suspend fun officialArtists(candidates: List<ArtistHit>): List<ArtistHit> =
        artistRepository.officialArtistHits(candidates)

    override suspend fun officialAlbums(artist: ArtistHit): List<AlbumHit> =
        officialSearchAlbums(artistRepository.profile(artist.browseId, artist.name), artist)

    override fun overviewHedgeDelayMs(): Long {
        val averageLatencyMs = providerRouter.health()
            .firstOrNull { it.providerId == YOUTUBE_MUSIC_PROVIDER_ID }
            ?.averageLatencyMs
            ?: 0L
        if (averageLatencyMs <= 0L) return DEFAULT_HEDGE_DELAY_MS
        return (averageLatencyMs * 2L).coerceIn(MIN_HEDGE_DELAY_MS, MAX_HEDGE_DELAY_MS)
    }

    private companion object {
        const val YOUTUBE_MUSIC_PROVIDER_ID = "youtube_music"
        const val DEFAULT_HEDGE_DELAY_MS = 1_800L
        const val MIN_HEDGE_DELAY_MS = 1_200L
        const val MAX_HEDGE_DELAY_MS = 3_500L
    }
}
