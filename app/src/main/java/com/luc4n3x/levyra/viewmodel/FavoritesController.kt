package com.luc4n3x.levyra.viewmodel

import android.app.Application
import com.luc4n3x.levyra.data.ExcludedArtistsStore
import com.luc4n3x.levyra.data.FavoriteMembership
import com.luc4n3x.levyra.data.FavoritesStore
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.ListeningPulseStore
import com.luc4n3x.levyra.data.RecommendationFeedbackStore
import com.luc4n3x.levyra.data.areAllFavoriteTracks
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.ArtistExclusions
import com.luc4n3x.levyra.domain.ExcludedArtist
import com.luc4n3x.levyra.domain.ForgottenFavorites
import com.luc4n3x.levyra.domain.RecommendationFeedback
import com.luc4n3x.levyra.domain.RecommendationFeedbackKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.excludedArtistKeyOf
import com.luc4n3x.levyra.domain.isExcludableArtist
import com.luc4n3x.levyra.feature.jam.JamRole
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.likeAllCopy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class FavoritesController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val application: Application,
    private val favoritesStore: FavoritesStore,
    private val favoriteMutationMutex: Mutex,
    private val recommendationFeedbackMutationMutex: Mutex,
    private val recommendationFeedbackStore: RecommendationFeedbackStore,
    private val excludedArtistsStore: ExcludedArtistsStore,
    private val queueEngine: PersistentQueueEngine,
    private val listeningPulseStore: ListeningPulseStore,
    private val offlineDownloads: OfflineDownloadsController,
    private val skipCurrentTrackIfNewlyAvoided: (RecommendationFeedback, RecommendationFeedback) -> Unit,
    private val recordSmartFavorite: (Track, Boolean) -> Unit
) {
    private var favoriteStoreJob: Job? = null
    private var likeAllJob: Job? = null
    private var forgottenFavoritesJob: Job? = null
    private var excludedArtistsGeneration = 0L

    fun loadExcludedArtists() {
        val loadGeneration = excludedArtistsGeneration
        scope.launch {
            val excluded = excludedArtistsStore.load()
            if (loadGeneration != excludedArtistsGeneration) return@launch
            applyExcludedArtists(excluded)
        }
    }

    fun loadRecommendationFeedback() {
        scope.launch {
            recommendationFeedbackMutationMutex.withLock {
                applyRecommendationFeedback(recommendationFeedbackStore.load())
            }
        }
    }

    private fun applyRecommendationFeedback(feedback: RecommendationFeedback) {
        queueEngine.setRecommendationFeedback(feedback)
        if (uiState.value.recommendationFeedback == feedback) return
        uiState.update { it.copy(recommendationFeedback = feedback) }
    }

    fun setTrackFeedback(track: Track, kind: RecommendationFeedbackKind) {
        val entry = RecommendationFeedback.entryFor(track, kind) ?: return
        scope.launch {
            val (before, updated) = recommendationFeedbackMutationMutex.withLock {
                val before = uiState.value.recommendationFeedback
                val updated = if (before.kindFor(track) == kind) {
                    recommendationFeedbackStore.clear(entry.trackKey)
                } else {
                    recommendationFeedbackStore.record(entry)
                }
                applyRecommendationFeedback(updated)
                before to updated
            }
            skipCurrentTrackIfNewlyAvoided(before, updated)
        }
    }

    fun dislikeTracks(tracks: List<Track>) {
        if (uiState.value.jam.role == JamRole.Guest) return
        val entries = tracks.mapNotNull { track ->
            RecommendationFeedback.entryFor(track, RecommendationFeedbackKind.LESS_LIKE_THIS)?.let { track to it }
        }.distinctBy { (_, entry) -> entry.trackKey }
        if (entries.isEmpty()) return
        scope.launch {
            val (before, updated) = recommendationFeedbackMutationMutex.withLock {
                val before = uiState.value.recommendationFeedback
                var latest = before
                entries.forEach { (track, entry) ->
                    if (!before.isExplicitlyAvoided(track)) latest = recommendationFeedbackStore.record(entry)
                }
                applyRecommendationFeedback(latest)
                before to latest
            }
            skipCurrentTrackIfNewlyAvoided(before, updated)
        }
    }

    suspend fun clearTrackDislikes(tracks: List<Track>) {
        if (tracks.none(uiState.value.recommendationFeedback::isExplicitlyAvoided)) return
        recommendationFeedbackMutationMutex.withLock {
            val feedback = uiState.value.recommendationFeedback
            val keys = tracks.filter(feedback::isExplicitlyAvoided).mapNotNull { track ->
                RecommendationFeedback.entryFor(track, RecommendationFeedbackKind.LESS_LIKE_THIS)?.trackKey
            }.distinct()
            if (keys.isEmpty()) return@withLock
            var latest = feedback
            keys.forEach { key -> latest = recommendationFeedbackStore.clear(key) }
            applyRecommendationFeedback(latest)
        }
    }

    fun applyExcludedArtists(excluded: List<ExcludedArtist>) {
        uiState.update {
            it.copy(
                excludedArtists = excluded,
                artistExclusions = ArtistExclusions.from(excluded)
            )
        }
    }

    fun toggleExcludeArtist(browseId: String, name: String) {
        val cleanName = name.trim()
        if (!isExcludableArtist(browseId, cleanName)) return
        val key = excludedArtistKeyOf(browseId, cleanName)
        val current = uiState.value.excludedArtists
        val alreadyExcluded = current.any { it.key == key }
        val updated = if (alreadyExcluded) {
            current.filterNot { it.key == key }
        } else {
            listOf(ExcludedArtist(browseId.trim(), cleanName, System.currentTimeMillis())) +
                current.filterNot { it.key == key }
        }
        applyExcludedArtists(updated)
        excludedArtistsGeneration++
        val mutationGeneration = excludedArtistsGeneration
        scope.launch {
            if (alreadyExcluded) {
                excludedArtistsStore.include(browseId, cleanName)
            } else {
                excludedArtistsStore.exclude(browseId, cleanName)
            }
            val reloaded = excludedArtistsStore.load()
            if (mutationGeneration != excludedArtistsGeneration) return@launch
            applyExcludedArtists(reloaded)
        }
    }

    fun includeArtist(artist: ExcludedArtist) {
        val remaining = uiState.value.excludedArtists.filterNot { it.key == artist.key }
        applyExcludedArtists(remaining)
        excludedArtistsGeneration++
        val mutationGeneration = excludedArtistsGeneration
        scope.launch {
            excludedArtistsStore.include(artist.browseId, artist.name)
            val reloaded = excludedArtistsStore.load()
            if (mutationGeneration != excludedArtistsGeneration) return@launch
            applyExcludedArtists(reloaded)
        }
    }

    fun refreshForgottenFavorites() {
        forgottenFavoritesJob?.cancel()
        val favorites = uiState.value.favorites
        if (favorites.isEmpty()) {
            if (uiState.value.forgottenFavorites.isNotEmpty()) {
                uiState.update { it.copy(forgottenFavorites = emptyList()) }
            }
            return
        }
        forgottenFavoritesJob = scope.launch(Dispatchers.Default) {
            val keys = ForgottenFavorites.listeningKeys(favorites)
            val lastPlayed = listeningPulseStore.lastPlayedByKey(keys)
            val selected = ForgottenFavorites.select(favorites, lastPlayed)
            withContext(Dispatchers.Main) {
                if (uiState.value.favorites !== favorites) return@withContext
                if (uiState.value.forgottenFavorites != selected) {
                    uiState.update { it.copy(forgottenFavorites = selected) }
                }
            }
        }
    }

    suspend fun carryFavorite(original: Track, replacement: Track) {
        favoriteMutationMutex.withLock {
            val updated = runCatchingPreservingCancellation { favoritesStore.replaceFavorite(original, replacement) }
                .onFailure { error -> Timber.w(error, "Favorite replacement failed") }
                .getOrNull() ?: return
            val timestamps = favoritesStore.loadTimestampsSuspending()
            uiState.update { state ->
                state.copy(
                    favorites = updated,
                    favoriteIds = updated.map { favorite -> favorite.id }.toSet(),
                    favoriteTimestamps = timestamps
                )
            }
        }
    }

    fun observeFavoriteStore() {
        favoriteStoreJob?.cancel()
        favoriteStoreJob = scope.launch {
            favoritesStore.observeMembership().collect { membership ->
                val addedCurrent = favoriteMutationMutex.withLock {
                    val previous = uiState.value.favorites
                    val stale = withContext(Dispatchers.Default) { !membership.sameTracksAs(previous) }
                    if (!stale) return@withLock null
                    val favorites = withContext(Dispatchers.IO) { favoritesStore.load() }
                    val timestamps = favoritesStore.loadTimestampsSuspending()
                    uiState.update { state ->
                        state.copy(
                            favorites = favorites,
                            favoriteIds = favorites.map { favorite -> favorite.id }.toSet(),
                            favoriteTimestamps = timestamps
                        )
                    }
                    withContext(Dispatchers.Default) {
                        val current = uiState.value.currentTrack
                            ?.let { track -> FavoriteMembership.of(listOf(track)) }
                        val previousMembership = FavoriteMembership.of(previous)
                        favorites.filter { track ->
                            current?.contains(track) == true && !previousMembership.contains(track)
                        }
                    }
                } ?: return@collect
                refreshForgottenFavorites()
                addedCurrent.forEach { track -> autoDownloadFavorite(track, becameFavorite = true) }
            }
        }
    }

    fun toggleFavorite(track: Track) {
        if (track.isLiveRadio()) return
        scope.launch {
            val (updated, isFavorite) = favoriteMutationMutex.withLock {
                val updated = favoritesStore.toggleFavorite(track)
                val timestamps = favoritesStore.loadTimestampsSuspending()
                val isFavorite = areAllFavoriteTracks(updated, listOf(track))
                uiState.update { state ->
                    state.copy(
                        favorites = updated,
                        favoriteIds = updated.map { favorite -> favorite.id }.toSet(),
                        favoriteTimestamps = timestamps
                    )
                }
                updated to isFavorite
            }
            if (isFavorite) clearTrackDislikes(listOf(track))
            refreshForgottenFavorites()
            LevyraArtworkCache.preloadPriority(
                application.applicationContext,
                updated,
                6
            )
            recordSmartFavorite(track, isFavorite)
            autoDownloadFavorite(track, isFavorite)
        }
    }

    private fun autoDownloadFavorite(track: Track, becameFavorite: Boolean) =
        offlineDownloads.autoDownloadFavorite(track, becameFavorite)

    fun toggleFavorites(tracks: List<Track>) {
        val cleanTracks = tracks
            .asSequence()
            .filterNot { track -> track.isLiveRadio() }
            .distinctBy { track ->
                track.id.ifBlank {
                    track.isrc.ifBlank {
                        track.audioVideoId.ifBlank { track.title + "|" + track.artist }
                    }
                }
            }
            .toList()
        if (cleanTracks.isEmpty()) return
        scope.launch {
            val updated = favoriteMutationMutex.withLock {
                favoritesStore.toggleFavorites(cleanTracks).also { favorites ->
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
            clearTrackDislikes(cleanTracks.filter { track -> areAllFavoriteTracks(updated, listOf(track)) })
            refreshForgottenFavorites()
            LevyraArtworkCache.preloadPriority(
                application.applicationContext,
                updated,
                6
            )
            cleanTracks.forEach { track ->
                val isFavorite = areAllFavoriteTracks(updated, listOf(track))
                recordSmartFavorite(track, isFavorite)
                autoDownloadFavorite(track, isFavorite)
            }
        }
    }

    fun likeAllTracks(tracks: List<Track>) {
        val candidates = tracks.filterNot { track -> track.isLiveRadio() }
        if (candidates.isEmpty() || likeAllJob?.isActive == true) return
        likeAllJob = scope.launch {
            val copy = LevyraStrings.forCode(uiState.value.languageCode).likeAllCopy()
            val addition = try {
                favoriteMutationMutex.withLock {
                    favoritesStore.addMissingFavorites(candidates).also { result ->
                        val timestamps = favoritesStore.loadTimestampsSuspending()
                        uiState.update { state ->
                            state.copy(
                                favorites = result.favorites,
                                favoriteIds = result.favorites.map { favorite -> favorite.id }.toSet(),
                                favoriteTimestamps = timestamps
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Like all tracks failed")
                uiState.update { it.copy(offlineExportMessage = copy.failed) }
                return@launch
            }
            if (addition.added.isEmpty()) {
                uiState.update { it.copy(offlineExportMessage = copy.alreadyLiked) }
                return@launch
            }
            clearTrackDislikes(addition.added)
            refreshForgottenFavorites()
            LevyraArtworkCache.preloadPriority(
                application.applicationContext,
                addition.favorites,
                6
            )
            addition.added.forEach { track ->
                recordSmartFavorite(track, true)
                autoDownloadFavorite(track, true)
            }
            uiState.update { it.copy(offlineExportMessage = copy.liked(addition.added.size)) }
        }
    }

    fun removeFavorites(tracks: List<Track>) {
        val ids = tracks.map { it.id }.filter(String::isNotBlank).toSet()
        if (ids.isEmpty()) return
        val current = uiState.value.favorites
        val updated = current.filterNot { it.id in ids }
        if (updated.size == current.size) return
        uiState.update {
            it.copy(
                favorites = updated,
                favoriteIds = updated.map { favorite -> favorite.id }.toSet(),
                favoriteTimestamps = it.favoriteTimestamps - ids
            )
        }
        scope.launch(Dispatchers.IO) { favoritesStore.saveSuspending(updated) }
        refreshForgottenFavorites()
        uiState.update { it.copy(offlineExportMessage = "Rimossi dai preferiti: ${current.size - updated.size}") }
    }
}
