package com.luc4n3x.levyra.viewmodel

import android.content.Context
import com.luc4n3x.levyra.data.ARTIST_ALL_SONGS_LIMIT
import com.luc4n3x.levyra.data.ArtistRepository
import com.luc4n3x.levyra.data.FollowedArtistsStore
import com.luc4n3x.levyra.data.ReleaseRadarPolicy
import com.luc4n3x.levyra.data.ReleaseRadarWorker
import com.luc4n3x.levyra.data.artistQueueTail
import com.luc4n3x.levyra.data.mergeArtistSongs
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.ArtistBiography
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val ARTIST_PROFILE_UNAVAILABLE_ERROR = "artist_profile_unavailable"
private const val ARTIST_INITIAL_BIOGRAPHY_WAIT_MS = 250L

internal class ArtistProfileController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val context: Context,
    private val artistRepository: ArtistRepository,
    private val queueEngine: PersistentQueueEngine,
    private val followedArtistsStore: FollowedArtistsStore,
    private val refreshArtistMotion: (ArtistProfile, Boolean) -> Unit,
    private val refreshQueuePrefetch: () -> Unit,
    private val applyFollowedArtists: (List<FollowedArtist>) -> Unit,
    private val loadReleaseRadar: () -> Unit
) {
    private var artistJob: Job? = null
    @Volatile private var artistPlaceholder: ArtistProfile? = null
    private var artistLoreJob: Job? = null
    private var artistQueueExtensionJob: Job? = null
    @Volatile private var artistAllSongsCache: Pair<String, List<Track>>? = null
    private val artistCatalogLoadLock = Any()
    private var artistCatalogLoad: Pair<String, Deferred<List<Track>?>>? = null
    private var followedArtistsJob: Job? = null
    var followedArtistsGeneration = 0L
        private set

    fun cancelRequests() {
        artistJob?.cancel()
        artistLoreJob?.cancel()
    }

    private fun refreshArtistMotionArtwork(profile: ArtistProfile, keepCurrent: Boolean = false) =
        refreshArtistMotion(profile, keepCurrent)

    fun load(clean: String, normalizedBrowseId: String, artworkHint: String, requestedArtistListStateKey: String) {
        val placeholder = artistPlaceholder(normalizedBrowseId, clean, artworkHint)
        artistPlaceholder = placeholder
        uiState.update { current ->
            val sameProfile = current.artistProfile?.let { profile ->
                if (normalizedBrowseId.isNotBlank()) {
                    profile.browseId.equals(normalizedBrowseId, ignoreCase = true)
                } else {
                    profile.name.equals(clean, ignoreCase = true)
                }
            } == true
            current.copy(
                showAlbum = false,
                showArtist = true,
                artistLoading = true,
                artistError = null,
                artistProfile = current.artistProfile?.takeIf { sameProfile && it.hasBio } ?: placeholder,
                artistMotionArtwork = current.artistMotionArtwork.takeIf { sameProfile },
                artistListStateKey = requestedArtistListStateKey,
                artistRequestName = clean,
                openPlaylist = null,
                detailReturnTarget = DetailReturnTarget.None
            )
        }
        placeholder?.let(::refreshArtistMotionArtwork)
        artistJob = scope.launch {
            coroutineScope {
                val biographyDeferred = async {
                    runCatchingPreservingCancellation { artistRepository.biographyFor(clean, normalizedBrowseId) }.getOrNull()
                }
                val appleStorefront = ChartsCatalog.defaultRegionForLanguage(uiState.value.languageCode).country
                val appleBiographyDeferred = async {
                    runCatchingPreservingCancellation { artistRepository.appleBiographyFor(clean, appleStorefront) }.getOrNull()
                }
                val profileDeferred = async {
                    resolveArtistProfileReference(
                        browseId = normalizedBrowseId,
                        name = clean,
                        isActive = { isActive },
                        profileByBrowseId = { id, fallbackName ->
                            artistRepository.profile(id, fallbackName) { preview ->
                                publishArtistPreview(preview, requestedArtistListStateKey, biographyDeferred.completedOrNull())
                            }
                        },
                        profileByName = artistRepository::profileFor
                    )
                }
                val profile = profileDeferred.await()
                if (!isActive) return@coroutineScope
                if (profile == null) {
                    biographyDeferred.cancel()
                    appleBiographyDeferred.cancel()
                    uiState.update {
                        it.copy(
                            artistLoading = false,
                            artistError = ARTIST_PROFILE_UNAVAILABLE_ERROR,
                            artistProfile = null
                        )
                    }
                    return@coroutineScope
                }
                val initialBiography = biographyDeferred.completedOrNull()
                    ?: withTimeoutOrNull(ARTIST_INITIAL_BIOGRAPHY_WAIT_MS) { biographyDeferred.await() }
                val initialProfile = listOfNotNull(initialBiography, appleBiographyDeferred.completedOrNull())
                    .fold(profile, artistRepository::mergeBiography)
                uiState.update {
                    it.copy(
                        artistLoading = false,
                        artistError = null,
                        artistProfile = initialProfile
                    )
                }
                refreshArtistMotionArtwork(initialProfile, keepCurrent = true)
                initialBiography?.let {
                    startArtistLore(initialProfile)
                } ?: run {
                    artistLoreJob?.cancel()
                    artistLoreJob = launchArtistLoreAwait(profile, biographyDeferred)
                }
                appleBiographyDeferred.await()?.let { biography -> mergeVisibleArtistBiography(profile, biography) }
            }
        }
    }

    private fun mergeVisibleArtistBiography(profile: ArtistProfile, biography: ArtistBiography) {
        uiState.update { current ->
            val visible = current.artistProfile ?: return@update current
            if (!current.showArtist || !sameArtistProfile(visible, profile)) return@update current
            current.copy(artistProfile = artistRepository.mergeBiography(visible, biography))
        }
    }

    private fun artistPlaceholder(browseId: String, name: String, artworkUrl: String): ArtistProfile? {
        if (browseId.isBlank()) return null
        val artwork = artworkUrl.trim()
        val accent = artistRepository.accentFor(browseId, name)
        return ArtistProfile(
            browseId = browseId,
            name = name,
            subscribers = "",
            monthlyListeners = "",
            thumbnailUrl = artwork,
            bannerUrl = artwork,
            topSongs = emptyList(),
            albums = emptyList(),
            singles = emptyList(),
            accentStart = accent.first,
            accentEnd = accent.second
        )
    }

    private fun publishArtistPreview(preview: ArtistProfile, requestKey: String, biography: ArtistBiography?) {
        var published = false
        uiState.update { current ->
            published = false
            if (!current.showArtist || !current.artistLoading || current.artistListStateKey != requestKey) return@update current
            val shown = current.artistProfile
            if (shown != null && shown !== artistPlaceholder && !(shown.browseId == preview.browseId && shown.topSongs.size <= preview.topSongs.size)) {
                return@update current
            }
            published = true
            current.copy(artistProfile = biography?.let { artistRepository.mergeBiography(preview, it) } ?: preview)
        }
        if (published && preview.topSongs.isNotEmpty()) {
            scope.launch { refreshArtistMotionArtwork(preview, keepCurrent = true) }
        }
    }

    private fun launchArtistLoreAwait(
        profile: ArtistProfile,
        biographyDeferred: Deferred<ArtistBiography?>
    ): Job {
        return scope.launch {
            runCatchingPreservingCancellation { biographyDeferred.await() }.getOrNull()?.let { biography ->
                mergeVisibleArtistBiography(profile, biography)
            }
            val visible = uiState.value.artistProfile ?: return@launch
            if (!uiState.value.showArtist || !sameArtistProfile(visible, profile)) return@launch
            artistLoreJob = launchArtistLoreCollection(visible)
        }
    }

    fun startArtistLore(profile: ArtistProfile) {
        artistLoreJob?.cancel()
        artistLoreJob = launchArtistLoreCollection(profile)
    }

    private fun launchArtistLoreCollection(profile: ArtistProfile): Job {
        return scope.launch {
            artistRepository.observeBiography(profile).collectLatest { biography ->
                mergeVisibleArtistBiography(profile, biography)
            }
        }
    }

    fun extendArtistQueue(profile: ArtistProfile) {
        if (uiState.value.jam.isActive) return
        val initialQueue = queueEngine.state.value.tracks
        artistQueueExtensionJob?.cancel()
        artistQueueExtensionJob = scope.launch {
            val catalog = allArtistSongs(profile)
            val tail = artistQueueTail(queueEngine.state.value.tracks, initialQueue, catalog)
            if (tail.isEmpty() || uiState.value.jam.isActive) return@launch
            queueEngine.addLast(tail)
            refreshQueuePrefetch()
        }
    }

    suspend fun allArtistSongs(profile: ArtistProfile): List<Track> {
        artistAllSongsCache?.takeIf { it.first == profile.browseId }?.let { return it.second }
        val load = synchronized(artistCatalogLoadLock) {
            artistCatalogLoad?.takeIf { it.first == profile.browseId && it.second.isActive }?.second
                ?: scope.async(Dispatchers.IO) {
                    runCatchingPreservingCancellation { artistRepository.allSongs(profile.songsBrowseId, profile.name) }
                        .onFailure { Timber.w(it, "artist catalog lookup failed") }
                        .getOrNull()
                }.also { artistCatalogLoad = profile.browseId to it }
        }
        val catalog = load.await()
        val songs = mergeArtistSongs(profile.topSongs, catalog.orEmpty(), ARTIST_ALL_SONGS_LIMIT)
        if (catalog != null && catalog.isNotEmpty()) artistAllSongsCache = profile.browseId to songs
        return songs
    }

    fun toggleFollowArtist(artistBrowseId: String, artistName: String, thumbnailUrl: String) {
        val browseId = artistBrowseId.trim()
        if (browseId.isBlank()) return
        val name = artistName.trim()
        if (name.isBlank()) return
        val current = uiState.value.followedArtists
        val exists = current.any { sameArtist(it, browseId, name) }
        val updated = if (exists) {
            current.filterNot { sameArtist(it, browseId, name) }
        } else {
            listOf(FollowedArtist(browseId, name, thumbnailUrl, System.currentTimeMillis())) +
                current.filterNot { it.browseId.isBlank() && it.name.equals(name, ignoreCase = true) }
        }
        followedArtistsGeneration++
        val mutationGeneration = followedArtistsGeneration
        applyFollowedArtists(updated)
        followedArtistsJob?.cancel()
        followedArtistsJob = scope.launch(Dispatchers.IO) {
            persistFollowedArtistMutation(updated, current, exists, browseId, name, mutationGeneration)
        }
    }

    private suspend fun persistFollowedArtistMutation(
        updated: List<FollowedArtist>,
        previous: List<FollowedArtist>,
        wasFollowing: Boolean,
        browseId: String,
        name: String,
        mutationGeneration: Long
    ) {
        followedArtistsStore.save(updated)
        if (mutationGeneration != followedArtistsGeneration) return
        if (wasFollowing) {
            previous.filter { sameArtist(it, browseId, name) }
                .forEach { followedArtistsStore.clearKnownReleases(it.key) }
        } else if (uiState.value.interfaceSettings.releaseNotificationsEnabled) {
            seedFollowedArtistReleaseBaseline(browseId, name, mutationGeneration)
        }
        if (mutationGeneration != followedArtistsGeneration) return
        if (updated.isEmpty() || !uiState.value.interfaceSettings.releaseNotificationsEnabled) {
            ReleaseRadarWorker.cancel(context)
        } else {
            ReleaseRadarWorker.schedule(context)
        }
        withContext(Dispatchers.Main) { loadReleaseRadar() }
    }

    private suspend fun seedFollowedArtistReleaseBaseline(browseId: String, name: String, mutationGeneration: Long) {
        val currentProfile = try {
            artistRepository.profile(browseId, name)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Release radar baseline fetch failed for %s", name)
            null
        }
        if (currentProfile != null && mutationGeneration == followedArtistsGeneration) {
            val baseline = (currentProfile.albums + currentProfile.singles)
                .flatMapTo(linkedSetOf(), ReleaseRadarPolicy::identityKeys)
            followedArtistsStore.saveKnownReleases(browseId, baseline)
        }
    }

    private fun sameArtist(artist: FollowedArtist, browseId: String, name: String): Boolean =
        artist.browseId == browseId || (artist.browseId.isBlank() && artist.name.equals(name, ignoreCase = true))
}

internal fun sameArtistProfile(left: ArtistProfile, right: ArtistProfile): Boolean {
    return if (right.browseId.isNotBlank()) {
        left.browseId.equals(right.browseId, ignoreCase = true)
    } else {
        left.name.equals(right.name, ignoreCase = true)
    }
}
