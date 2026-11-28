package com.luc4n3x.levyra.viewmodel

import android.content.Context
import com.luc4n3x.levyra.data.collections.isPersonalCollectionTrack
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.LevyraCanvasSource
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.feature.motion.MotionArtworkEngine
import com.luc4n3x.levyra.feature.motion.MotionArtworkIdentityKey
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class MotionArtworkController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val context: Context,
    private val motionArtworkEngine: MotionArtworkEngine,
    private val playbackGeneration: PlaybackGenerationGuard,
    private val queueEngine: PersistentQueueEngine,
    private val isLocalPlaybackTrack: (Track) -> Boolean,
    private val samePlayableTrack: (Track, Track) -> Boolean
) {
    private var motionArtworkJob: Job? = null
    @Volatile private var motionArtworkRequestKey: String? = null
    @Volatile private var motionArtworkRequestGeneration: Long = 0L
    private val motionArtworkRequestToken = AtomicLong(0L)
    private var motionArtworkPrefetchJob: Job? = null
    private var motionPosterJob: Job? = null
    @Volatile private var motionArtworkPrefetchKey: String? = null
    @Volatile private var motionArtworkPrefetchToken = 0L
    private var artistMotionJob: Job? = null
    private var artistMotionRequest: ArtistMotionRequest? = null
    private var artistMotionPublished = false
    private var albumMotionJob: Job? = null

    fun cancelRequests() {
        motionArtworkJob?.cancel()
        motionArtworkPrefetchJob?.cancel()
        motionArtworkRequestKey = null
    }

    fun cancelResolution(preserveMotionPrefetchKey: String? = null) {
        motionArtworkJob?.cancel()
        motionArtworkRequestKey = null
        if (preserveMotionPrefetchKey == null || motionArtworkPrefetchKey != preserveMotionPrefetchKey) {
            motionArtworkPrefetchJob?.cancel()
        }
    }

    fun cancelArtist() {
        artistMotionJob?.cancel()
    }

    fun cancelAlbum() {
        albumMotionJob?.cancel()
    }

    fun close() {
        cancelPageMotion()
        motionArtworkJob?.cancel()
        motionArtworkPrefetchJob?.cancel()
        motionArtworkEngine.close()
    }

    private fun canRefreshCurrentMotionArtwork(
        snapshot: LevyraUiState,
        current: Track
    ): Boolean {
        if (!snapshot.animationsEnabled || !snapshot.motionArtworkEnabled || snapshot.isVideoMode) {
            return false
        }
        return !current.isLiveRadio() && !isLocalPlaybackTrack(current)
    }

    private fun canPublishMotionArtworkForRequest(
        state: LevyraUiState,
        ticket: PlaybackTicket,
        expectedKey: String,
        requestToken: Long
    ): Boolean {
        val visualStateAllowsMotion =
            state.animationsEnabled && state.motionArtworkEnabled && !state.isVideoMode
        val requestMatches =
            motionArtworkRequestToken.get() == requestToken && playbackGeneration.isCurrent(ticket)
        val identityMatches = state.currentTrack
            ?.let { activeTrack -> MotionArtworkIdentityKey.create(activeTrack) == expectedKey }
            ?: false
        return visualStateAllowsMotion && requestMatches && identityMatches
    }

    fun refreshCurrent() {
        val snapshot = uiState.value
        val current = snapshot.currentTrack ?: return
        if (!canRefreshCurrentMotionArtwork(snapshot, current)) return

        val expectedKey = MotionArtworkIdentityKey.create(current)
        val ticket = playbackGeneration.current()
        val source = snapshot.interfaceSettings.canvasSource
        val cachedIdentityKey = snapshot.motionArtwork?.identityKey
        motionArtworkJob?.cancel()
        motionArtworkPrefetchJob?.cancel()
        motionArtworkRequestToken.incrementAndGet()
        motionArtworkRequestKey = null
        uiState.update { state ->
            val activeTrack = state.currentTrack
            if (
                activeTrack != null &&
                playbackGeneration.isCurrent(ticket) &&
                MotionArtworkIdentityKey.create(activeTrack) == expectedKey
            ) {
                state.copy(motionArtwork = null, motionArtworkLoading = true)
            } else {
                state
            }
        }

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    motionArtworkEngine.invalidate(
                        track = current,
                        source = source,
                        cachedIdentityKey = cachedIdentityKey
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Timber.d(error, "Motion artwork selective refresh invalidation failed for %s", current.id)
            }

            if (!isActive || !playbackGeneration.isCurrent(ticket)) return@launch
            val activeTrack = uiState.value.currentTrack ?: return@launch
            if (
                uiState.value.isVideoMode ||
                MotionArtworkIdentityKey.create(activeTrack) != expectedKey
            ) {
                return@launch
            }
            refreshAround(activeTrack)
        }
    }

    fun refreshAround(current: Track) {
        if (
            !uiState.value.animationsEnabled ||
            !uiState.value.motionArtworkEnabled ||
            uiState.value.isVideoMode ||
            current.isPersonalCollectionTrack()
        ) {
            motionArtworkJob?.cancel()
            motionArtworkPrefetchJob?.cancel()
            motionArtworkRequestToken.incrementAndGet()
            motionArtworkRequestKey = null
            uiState.update { it.copy(motionArtwork = null, motionArtworkLoading = false) }
            return
        }
        val expectedKey = MotionArtworkIdentityKey.create(current)
        val ticket = playbackGeneration.current()
        val previousGeneration = motionArtworkRequestGeneration
        if (
            uiState.value.motionArtwork != null &&
            previousGeneration != ticket.generation
        ) {
            uiState.update { state ->
                state.copy(motionArtwork = null, motionArtworkLoading = true)
            }
        }
        if (
            motionArtworkJob?.isActive == true &&
            motionArtworkRequestKey == expectedKey &&
            motionArtworkRequestGeneration == ticket.generation
        ) {
            return
        }
        val requestToken = motionArtworkRequestToken.incrementAndGet()
        motionArtworkJob?.cancel()
        motionArtworkRequestKey = expectedKey
        motionArtworkRequestGeneration = ticket.generation
        launchMotionArtworkResolution(current, expectedKey, ticket, requestToken)
    }

    private fun publishMotionArtworkForRequest(
        artwork: MotionArtwork?,
        ticket: PlaybackTicket,
        expectedKey: String,
        requestToken: Long
    ) {
        uiState.update { current ->
            if (canPublishMotionArtworkForRequest(current, ticket, expectedKey, requestToken)) {
                current.copy(
                    motionArtwork = artwork,
                    motionArtworkLoading = false
                )
            } else {
                current
            }
        }
        val published = artwork?.takeIf { it.identityKey == uiState.value.motionArtwork?.identityKey } ?: return
        if (com.luc4n3x.levyra.feature.motion.MotionPosterCache.posterKey(published) == null) return
        motionPosterJob?.cancel()
        motionPosterJob = scope.launch {
            com.luc4n3x.levyra.feature.motion.MotionPosterCache.warm(context, published)
        }
    }

    private fun launchMotionArtworkResolution(
        current: Track,
        expectedKey: String,
        ticket: PlaybackTicket,
        requestToken: Long
    ) {
        motionArtworkJob = scope.launch(Dispatchers.IO) {
            try {
                var stabilized: MotionArtwork? = null
                runCatching {
                    motionArtworkEngine
                        .resolveProgressive(current, uiState.value.interfaceSettings.canvasSource)
                        .collect { artwork ->
                            stabilized = artwork
                            publishMotionArtworkForRequest(artwork, ticket, expectedKey, requestToken)
                        }
                }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.d(error, "Motion artwork resolve failed for %s", current.id)
                    }
                if (!isActive) return@launch
                publishMotionArtworkForRequest(stabilized, ticket, expectedKey, requestToken)
                prefetchNext(current)
            } finally {
                if (
                    motionArtworkRequestToken.get() == requestToken &&
                    motionArtworkRequestKey == expectedKey
                ) {
                    motionArtworkRequestKey = null
                }
            }
        }
    }

    fun prefetchNext(current: Track) {
        motionArtworkPrefetchJob?.cancel()
        if (!uiState.value.animationsEnabled || !uiState.value.motionArtworkEnabled || uiState.value.isVideoMode) return
        val generation = queueEngine.state.value.generation
        val currentKey = MotionArtworkIdentityKey.create(current)
        val next = queueEngine.upcoming(2)
            .firstOrNull { !samePlayableTrack(it, current) && !it.isPersonalCollectionTrack() }
            ?: return
        val nextKey = MotionArtworkIdentityKey.create(next)
        val prefetchToken = ++motionArtworkPrefetchToken
        motionArtworkPrefetchKey = nextKey
        motionArtworkPrefetchJob = scope.launch(Dispatchers.IO) {
            try {
                delay(180L)
                if (!isActive || uiState.value.isVideoMode) return@launch
                val active = uiState.value.currentTrack ?: return@launch
                if (
                    !shouldContinueMotionPrefetch(
                        activeKey = MotionArtworkIdentityKey.create(active),
                        currentKey = currentKey,
                        nextKey = nextKey,
                        queueChanged = queueEngine.state.value.generation != generation
                    )
                ) {
                    return@launch
                }
                runCatching {
                    motionArtworkEngine.prefetchNext(next, uiState.value.interfaceSettings.canvasSource)
                }
                    .onFailure { error ->
                        if (error is CancellationException) throw error
                        Timber.d(error, "Motion artwork prefetch failed for %s", next.id)
                    }
            } finally {
                if (motionArtworkPrefetchToken == prefetchToken) motionArtworkPrefetchKey = null
            }
        }
    }

    fun cancelPageMotion() {
        artistMotionJob?.cancel()
        albumMotionJob?.cancel()
    }

    fun refreshPage() {
        val state = uiState.value
        if (state.showArtist) state.artistProfile?.let(::refreshArtist)
        if (state.showAlbum) state.albumDetail?.let(::refreshAlbum)
    }

    fun refreshArtist(profile: ArtistProfile, keepCurrent: Boolean = false) {
        val seeds = selectArtistMotionSeeds(
            profileName = profile.name,
            tracks = profile.topSongs,
            isLocal = isLocalPlaybackTrack
        )
        val request = ArtistMotionRequest(artistMotionIdentity(profile), seeds.map(::playbackIdentity))
        if (keepCurrent && request.identity == artistMotionRequest?.identity) {
            val published = uiState.value.artistMotionArtwork != null && artistMotionPublished
            val sameWorkInFlight = artistMotionJob?.isActive == true &&
                (request == artistMotionRequest || artistMotionRequest?.seedIdentities?.isNotEmpty() == true)
            if (published || sameWorkInFlight) return
        }
        artistMotionJob?.cancel()
        artistMotionRequest = request
        artistMotionPublished = false
        if (!uiState.value.animationsEnabled || !uiState.value.motionArtworkEnabled) {
            uiState.update { it.copy(artistMotionArtwork = null) }
            return
        }
        if (profile.name.isBlank() && seeds.isEmpty()) {
            uiState.update { it.copy(artistMotionArtwork = null) }
            return
        }
        artistMotionJob = scope.launch(Dispatchers.IO) {
            val canvasSource = uiState.value.interfaceSettings.canvasSource
            val attempts = buildList<suspend () -> MotionArtwork?> {
                if (profile.name.isNotBlank()) {
                    add {
                        runCatchingPreservingCancellation {
                            motionArtworkEngine.awaitArtist(profile.name, profile.browseId, canvasSource)
                        }.onFailure { error ->
                            Timber.d(error, "Artist motion video resolve failed for %s", profile.name)
                        }.getOrNull()
                    }
                }
                seeds.forEach { track ->
                    add {
                        runCatchingPreservingCancellation {
                            motionArtworkEngine.resolve(track, LevyraCanvasSource.Auto)
                        }.onFailure { error ->
                            Timber.d(error, "Artist motion artwork resolve failed for %s", profile.name)
                        }.getOrNull()
                    }
                }
            }
            val resolved = firstAvailableMotionArtwork(attempts)
            Timber.d(
                "Artist hero motion %s artist=%s provider=%s",
                if (resolved == null) "NONE" else "ASSIGNED",
                profile.name,
                resolved?.provider.orEmpty()
            )
            if (!isActive) return@launch
            val visible = uiState.value.artistProfile ?: return@launch
            if (!uiState.value.showArtist || !sameArtistProfile(visible, profile)) return@launch
            withContext(Dispatchers.Main) {
                if (artistMotionRequest != request) return@withContext
                artistMotionPublished = resolved != null
                uiState.update { it.copy(artistMotionArtwork = resolved) }
            }
        }
    }

    private data class ArtistMotionRequest(
        val identity: String,
        val seedIdentities: List<String>
    )

    private fun artistMotionIdentity(profile: ArtistProfile): String =
        profile.browseId.trim().lowercase(Locale.ROOT).ifBlank { profile.name.trim().lowercase(Locale.ROOT) }

    fun refreshAlbum(detail: AlbumDetail) {
        albumMotionJob?.cancel()
        if (!uiState.value.animationsEnabled || !uiState.value.motionArtworkEnabled) {
            uiState.update { it.copy(albumMotionArtwork = null) }
            return
        }
        val seed = selectAlbumMotionSeed(detail, isLocalPlaybackTrack)
        if (seed == null) {
            uiState.update { it.copy(albumMotionArtwork = null) }
            return
        }
        albumMotionJob = scope.launch(Dispatchers.IO) {
            val publishForVisibleAlbum: (MotionArtwork?) -> Unit = { artwork ->
                uiState.update { current ->
                    if (
                        canPublishAlbumMotionArtwork(
                            visible = current.albumDetail,
                            expected = detail,
                            albumVisible = current.showAlbum,
                            animationsEnabled = current.animationsEnabled,
                            motionArtworkEnabled = current.motionArtworkEnabled
                        )
                    ) {
                        current.copy(albumMotionArtwork = artwork)
                    } else {
                        current
                    }
                }
            }
            var published = false
            runCatching {
                motionArtworkEngine
                    .resolveAppleAlbumProgressive(seed)
                    .collect { artwork ->
                        published = true
                        publishForVisibleAlbum(artwork)
                    }
            }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Timber.d(error, "Album motion artwork resolve failed for %s", seed.album)
                }
            if (!isActive) return@launch
            if (!published) publishForVisibleAlbum(null)
        }
    }
}
