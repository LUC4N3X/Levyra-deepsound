package com.luc4n3x.levyra.viewmodel

import android.app.Application
import com.luc4n3x.levyra.data.ArtistRepository
import com.luc4n3x.levyra.data.FavoritesStore
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.areAllFavoriteTracks
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.buildAlbumSuggestionShelves
import com.luc4n3x.levyra.domain.primaryArtistSegment
import com.luc4n3x.levyra.ui.albumSuggestionsSupported
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.moreByArtist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class AlbumDetailController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val application: Application,
    private val repository: YoutubeMusicRepository,
    private val artistRepository: ArtistRepository,
    private val favoritesStore: FavoritesStore,
    private val favoriteMutationMutex: Mutex,
    private val hasInternetCapableNetwork: () -> Boolean,
    private val mergeTracks: (List<Track>, List<Track>) -> List<Track>,
    private val refreshAlbumMotionArtwork: (AlbumDetail) -> Unit,
    private val recordSmartAlbumOpen: (AlbumHit) -> Unit,
    private val refreshOfficialMetadataBatch: (List<Track>, Int) -> Unit,
    private val clearTrackDislikes: suspend (List<Track>) -> Unit,
    private val refreshForgottenFavorites: () -> Unit
) {
    private var albumJob: Job? = null
    private var albumFavoriteJob: Job? = null
    private val lastFmSimilarAlbums by lazy { com.luc4n3x.levyra.data.lastfm.LastFmSimilarAlbumsRepository(application) }

    fun cancelLoad() {
        albumJob?.cancel()
    }

    fun load(album: AlbumHit) {
        albumJob = scope.launch {
            val languageCode = uiState.value.languageCode
            val providerResult = runCatchingPreservingCancellation { repository.albumDetail(album, languageCode) }
                .onFailure { Timber.w(it, "Album detail lookup failed") }
            val providerDetail = providerResult.getOrNull()
            if (!isActive) return@launch
            val detail = if (providerDetail?.tracks.isNullOrEmpty()) {
                appleCatalogAlbumDetail(album, languageCode) ?: providerDetail
            } else {
                providerDetail
            }
            if (!isActive) return@launch
            if (detail == null || detail.tracks.isEmpty()) {
                val transientFailure = providerResult.isFailure || !hasInternetCapableNetwork()
                uiState.update {
                    val strings = LevyraStrings.forCode(it.languageCode)
                    it.copy(
                        albumLoading = false,
                        albumError = if (transientFailure) strings.albumTracksLoadFailed else strings.albumTracksUnavailable,
                        albumDetail = detail ?: AlbumDetail(album, "", emptyList())
                    )
                }
                return@launch
            }
            uiState.update {
                it.copy(
                    albumLoading = false,
                    albumError = null,
                    albumDetail = detail,
                    tracks = mergeTracks(detail.tracks, it.tracks),
                    searchResults = detail.tracks.take(12),
                    cacheReport = repository.cacheReport()
                )
            }
            refreshAlbumMotionArtwork(detail)
            if (detail === providerDetail && albumSuggestionsSupported) launch { refreshAlbumSuggestions(detail, languageCode) }
            launch {
                val description = runCatching {
                    repository.resolveAlbumDescription(detail, languageCode)
                }.getOrNull()?.trim().orEmpty()
                if (!isActive || description.isBlank() || description == detail.description) return@launch
                uiState.update { currentState ->
                    val shownDetail = currentState.albumDetail ?: return@update currentState
                    val sameBrowseId = detail.album.browseId.isNotBlank() &&
                        shownDetail.album.browseId.equals(detail.album.browseId, ignoreCase = true)
                    val sameIdentity = shownDetail.album.title.equals(detail.album.title, ignoreCase = true) &&
                        shownDetail.album.artist.equals(detail.album.artist, ignoreCase = true)
                    if (!currentState.showAlbum || (!sameBrowseId && !sameIdentity)) currentState
                    else currentState.copy(albumDetail = shownDetail.copy(description = description))
                }
            }
            recordSmartAlbumOpen(detail.album)
            LevyraArtworkCache.preloadPriority(application.applicationContext, detail.tracks, 8)
            refreshOfficialMetadataBatch(detail.tracks, 12)
        }
    }

    fun toggleFavorite() {
        val tracks = uiState.value.albumDetail?.tracks.orEmpty()
        if (tracks.isEmpty() || albumFavoriteJob?.isActive == true) return
        albumFavoriteJob = scope.launch {
            val updated = favoriteMutationMutex.withLock {
                favoritesStore.toggleFavorites(tracks).also { favorites ->
                    val timestamps = favoritesStore.loadTimestampsSuspending()
                    uiState.update { state ->
                        state.copy(
                            favorites = favorites,
                            favoriteIds = favorites.map { favorite -> favorite.id }.toSet(),
                            favoriteTimestamps = timestamps
                        )
                    }
                }
            }
            clearTrackDislikes(tracks.filter { track -> areAllFavoriteTracks(updated, listOf(track)) })
            refreshForgottenFavorites()
            LevyraArtworkCache.preloadPriority(
                application.applicationContext,
                updated,
                6
            )
        }
    }

    private suspend fun refreshAlbumSuggestions(detail: AlbumDetail, languageCode: String) {
        val album = detail.album
        val strings = LevyraStrings.forCode(languageCode)
        val (profile, similar) = coroutineScope {
            val profile = async {
                album.artistBrowseId.takeIf { it.isNotBlank() }?.let { browseId ->
                    runCatchingPreservingCancellation { artistRepository.profile(browseId, album.artist) }.getOrNull()
                }
            }
            val similar = async {
                runCatchingPreservingCancellation { lastFmSimilarAlbums.similarAlbums(primaryArtistSegment(album.artist).ifBlank { album.artist }) }
                    .getOrNull().orEmpty()
            }
            profile.await() to similar.await()
        }
        val artistName = profile?.name?.ifBlank { null } ?: album.artist
        val releases = profile?.let { it.albums + it.singles }.orEmpty().map { release ->
            AlbumHit(
                title = release.title,
                artist = artistName,
                year = release.year,
                thumbnailUrl = release.thumbnailUrl,
                query = listOf(release.title, artistName).filter { it.isNotBlank() }.joinToString(" "),
                browseId = release.browseId,
                artistBrowseId = profile?.browseId.orEmpty(),
                explicit = release.explicit,
                releaseType = release.releaseType
            )
        }
        val similarAlbums = similar.map { item ->
            AlbumHit(title = item.title, artist = item.artist, year = "", thumbnailUrl = item.imageUrl, query = "${item.title} ${item.artist}")
        }
        val shelves = buildAlbumSuggestionShelves(
            current = album,
            moreByTitle = moreByArtist(languageCode, artistName),
            artistReleases = releases,
            youtubeShelves = detail.suggestionShelves,
            similarTitle = strings.youMightAlsoLike,
            similarAlbums = similarAlbums
        )
        if (!currentCoroutineContext().isActive) return
        uiState.update { current ->
            val shown = current.albumDetail ?: return@update current
            val sameAlbum = if (album.browseId.isNotBlank()) shown.album.browseId.equals(album.browseId, ignoreCase = true)
            else shown.album.title.equals(album.title, ignoreCase = true) && shown.album.artist.equals(album.artist, ignoreCase = true)
            if (!current.showAlbum || !sameAlbum) current
            else current.copy(albumDetail = shown.copy(suggestionShelves = shelves))
        }
    }

    private suspend fun appleCatalogAlbumDetail(album: AlbumHit, languageCode: String): AlbumDetail? {
        val (storefront, albumId) = com.luc4n3x.levyra.data.apple.AppleCatalogIds.albumFromUrl(album.canonicalUrl) ?: return null
        val collection = com.luc4n3x.levyra.data.apple.AppleMusicEditorialRepository.get(application)
            .album(storefront, albumId, languageCode) ?: return null
        val tracks = collection.tracks.map { track -> track.copy(album = track.album.ifBlank { album.title }) }
        return AlbumDetail(
            album = album.copy(thumbnailUrl = album.thumbnailUrl.ifBlank { collection.artworkUrl }),
            description = collection.description,
            tracks = tracks,
            footer = collection.footer,
            relatedShelves = collection.related.map { shelf ->
                com.luc4n3x.levyra.domain.AlbumRelatedShelf(
                    title = shelf.title,
                    items = shelf.items.filter { it.url.isNotBlank() && it.title.isNotBlank() }.map { item ->
                        com.luc4n3x.levyra.domain.AlbumRelatedItem(
                            id = item.id,
                            title = item.title,
                            subtitle = item.subtitle,
                            artworkUrl = item.artworkUrl(APPLE_RELATED_ARTWORK_SIZE),
                            url = item.url,
                            playlist = item.kind == com.luc4n3x.levyra.data.apple.AppleCatalogItemKind.Playlist
                        )
                    }
                )
            }.filter { it.items.isNotEmpty() }
        )
    }

    private companion object {
        const val APPLE_RELATED_ARTWORK_SIZE = 400
    }
}
