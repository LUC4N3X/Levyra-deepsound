package com.luc4n3x.levyra.viewmodel

import android.app.Application
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.LIVE_RADIO_RETRY_DELAYS_MS
import com.luc4n3x.levyra.feature.radio.LiveRadioArtworkResolver
import com.luc4n3x.levyra.feature.radio.RadioRepository
import com.luc4n3x.levyra.feature.radio.RadioStation
import com.luc4n3x.levyra.feature.radio.liveRadioNowPlaying
import com.luc4n3x.levyra.feature.radio.liveRadioRetryPlan
import com.luc4n3x.levyra.player.LevyraPlayer
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import com.luc4n3x.levyra.player.queue.PlaybackQueueSnapshot
import com.luc4n3x.levyra.player.queue.queueAfterPlayNextIntent
import com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val LIVE_RADIO_RECOVERY_STABLE_MS = 10_000L
private const val LIVE_RADIO_MAX_SESSION_RECOVERIES = 8
private const val LIVE_RADIO_SIBLING_LOOKUP_TIMEOUT_MS = 6_000L

internal interface LiveRadioHost {
    var streamRecoveryJob: Job?

    fun hasInternetCapableNetwork(): Boolean

    fun updateWidget()
}

internal class LiveRadioController(
    private val scope: CoroutineScope,
    private val application: Application,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val player: LevyraPlayer,
    private val queueEngine: PersistentQueueEngine,
    private val host: LiveRadioHost
) {
    var liveRadioQueueSnapshot: PlaybackQueueSnapshot? = null
    private var liveRadioPlayNextPendingIdentities: List<String> = emptyList()
    private var liveRadioRecoveryAttempt = 0
    private var liveRadioSessionRecoveries = 0
    private var liveRadioTriedStreams: Set<String> = emptySet()
    private var liveRadioSiblingStreams: List<String>? = null
    val liveRadioRepository by lazy { RadioRepository(application) }
    val favoriteRadioStations by lazy { liveRadioRepository.observeFavorites() }
    private var liveRadioArtworkJob: Job? = null
    private var liveRadioPlaybackSaveJob: Job? = null
    private var liveRadioArtwork = ""

    fun observeNowPlaying() {
        scope.launch {
            PlaybackService.liveRadioMetadataFlow.collect { metadata ->
                val snapshot = uiState.value
                val station = snapshot.liveRadioStation ?: return@collect
                val nowPlaying = liveRadioNowPlaying(
                    metadata = metadata,
                    stationName = station.name,
                    advertisementLabel = LevyraLiveRadioCatalog.advertisement(snapshot.languageCode)
                )
                if (nowPlaying != snapshot.liveRadioNowPlaying) {
                    uiState.update { current ->
                        if (current.liveRadioStation?.uuid == station.uuid) current.copy(liveRadioNowPlaying = nowPlaying) else current
                    }
                }
            }
        }
    }

    fun beginStationSession(station: RadioStation, track: Track) {
        liveRadioRecoveryAttempt = 0
        liveRadioSessionRecoveries = 0
        liveRadioTriedStreams = setOf(station.preferredStreamUrl, station.alternateStreamUrl)
        liveRadioSiblingStreams = null
        liveRadioArtwork = ""
        if (liveRadioQueueSnapshot == null) {
            liveRadioQueueSnapshot = queueEngine.state.value
            liveRadioPlayNextPendingIdentities = emptyList()
            queueEngine.beginTransientPlayback(liveRadioQueueSnapshot!!, listOf(track), 0)
        } else {
            queueEngine.replaceTransient(listOf(track), 0)
        }
    }

    suspend fun awaitPlaybackSave() {
        liveRadioPlaybackSaveJob?.join()
    }

    fun saveLiveRadioPlayback(station: RadioStation?) {
        liveRadioPlaybackSaveJob?.cancel()
        liveRadioPlaybackSaveJob = scope.launch {
            liveRadioRepository.savePlaybackStation(station)
        }
    }

    fun refreshLiveRadioArtwork(station: RadioStation, trackId: String) {
        liveRadioArtworkJob?.cancel()
        liveRadioArtworkJob = scope.launch {
            val artwork = LiveRadioArtworkResolver.playerArtwork(application, station) ?: return@launch
            if (uiState.value.liveRadioStation?.uuid != station.uuid) return@launch
            liveRadioArtwork = artwork
            var published = false
            uiState.update { current ->
                val activeTrack = current.currentTrack?.takeIf {
                    it.id == trackId && current.liveRadioStation?.uuid == station.uuid
                }
                if (activeTrack == null) {
                    current
                } else {
                    published = true
                    current.copy(currentTrack = activeTrack.copy(thumbnailUrl = artwork, largeThumbnailUrl = artwork))
                }
            }
            if (!published) return@launch
            PlaybackService.publishLiveRadioArtwork(trackId, artwork)
            host.updateWidget()
        }
    }

    fun recoverLiveRadioStream(failedTrack: Track, playWhenReady: Boolean, errorMessage: String) {
        val station = uiState.value.liveRadioStation ?: return
        if (failedTrack.id != station.toTrack().id) return
        val attempt = liveRadioRecoveryAttempt + 1
        liveRadioSessionRecoveries++
        val playingSibling = failedTrack.streamUrl !in setOf(station.preferredStreamUrl, station.alternateStreamUrl)
        val recovery = liveRadioRetryPlan(station, failedTrack.streamUrl, attempt)
            ?.takeIf { liveRadioSessionRecoveries <= LIVE_RADIO_MAX_SESSION_RECOVERIES }
            ?.let { plan -> if (playingSibling) plan.copy(streamUrl = failedTrack.streamUrl) else plan }
        if (recovery == null) {
            tryLiveRadioSiblingStream(station, playWhenReady, errorMessage)
            return
        }
        liveRadioRecoveryAttempt = attempt
        uiState.update {
            it.copy(
                isPlaying = false,
                isResolving = true,
                liveRadioReconnectAttempt = attempt,
                playerError = null
            )
        }
        host.streamRecoveryJob?.cancel()
        host.streamRecoveryJob = scope.launch {
            delay(recovery.delayMs)
            if (!isActive || uiState.value.liveRadioStation?.uuid != station.uuid) return@launch
            if (!player.hasMediaItem || !player.playWhenReady) {
                stopLiveRadioRecovery()
                return@launch
            }
            val replacement = station.toTrack(recovery.streamUrl, liveRadioArtwork)
            queueEngine.replaceTransient(listOf(replacement), 0)
            player.replaceSource(
                track = replacement,
                positionMs = 0L,
                videoMode = false,
                playWhenReady = playWhenReady
            )
            uiState.update {
                it.copy(
                    currentTrack = replacement,
                    isPlaying = playWhenReady,
                    isResolving = false,
                    liveRadioReconnectAttempt = attempt,
                    playerError = null
                )
            }
            host.updateWidget()
            delay(LIVE_RADIO_RECOVERY_STABLE_MS)
            if (uiState.value.liveRadioStation?.uuid == station.uuid && player.isRendering) {
                liveRadioRecoveryAttempt = 0
                player.markRecoverySucceeded()
                uiState.update { it.copy(liveRadioReconnectAttempt = 0) }
            }
        }
    }

    fun tryLiveRadioSiblingStream(station: RadioStation, playWhenReady: Boolean, errorMessage: String) {
        host.streamRecoveryJob?.cancel()
        if (!playWhenReady || liveRadioSessionRecoveries > LIVE_RADIO_MAX_SESSION_RECOVERIES) {
            failLiveRadioStation(errorMessage)
            return
        }
        uiState.update {
            it.copy(
                isPlaying = false,
                isResolving = true,
                liveRadioReconnectAttempt = LIVE_RADIO_RETRY_DELAYS_MS.size + 1,
                playerError = null
            )
        }
        host.streamRecoveryJob = scope.launch {
            val siblings = liveRadioSiblingStreams ?: withTimeoutOrNull(LIVE_RADIO_SIBLING_LOOKUP_TIMEOUT_MS) {
                try {
                    liveRadioRepository.siblingStreams(station)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    emptyList()
                }
            }.orEmpty().also { liveRadioSiblingStreams = it }
            if (uiState.value.liveRadioStation?.uuid != station.uuid) return@launch
            val next = siblings.firstOrNull { it !in liveRadioTriedStreams }
            if (next == null || !host.hasInternetCapableNetwork()) {
                failLiveRadioStation(errorMessage)
                return@launch
            }
            liveRadioTriedStreams = liveRadioTriedStreams + next
            liveRadioRecoveryAttempt = 0
            val replacement = station.toTrack(next, liveRadioArtwork)
            queueEngine.replaceTransient(listOf(replacement), 0)
            player.play(replacement, videoMode = false, startPositionMs = 0L)
            uiState.update {
                it.copy(
                    currentTrack = replacement,
                    isPlaying = true,
                    isResolving = false,
                    playerError = null
                )
            }
            host.updateWidget()
            delay(LIVE_RADIO_RECOVERY_STABLE_MS)
            if (uiState.value.liveRadioStation?.uuid == station.uuid && player.isRendering) {
                player.markRecoverySucceeded()
                uiState.update { it.copy(liveRadioReconnectAttempt = 0) }
            }
        }
    }

    private fun failLiveRadioStation(errorMessage: String) {
        if (BuildConfig.DEBUG) Timber.w("live radio unavailable after recovery: %s", errorMessage)
        liveRadioRecoveryAttempt = 0
        player.stop()
        uiState.update {
            it.copy(
                isPlaying = false,
                isResolving = false,
                liveRadioReconnectAttempt = 0,
                playerError = liveRadioUnavailableMessage()
            )
        }
        host.updateWidget()
    }

    fun stopLiveRadioRecovery() {
        liveRadioRecoveryAttempt = 0
        player.stop()
        uiState.update {
            it.copy(
                isPlaying = false,
                isResolving = false,
                liveRadioReconnectAttempt = 0
            )
        }
        host.updateWidget()
    }

    fun liveRadioUnavailableMessage(): String {
        val languageCode = uiState.value.languageCode
        return if (host.hasInternetCapableNetwork()) {
            LevyraLiveRadioCatalog.streamUnavailable(languageCode)
        } else {
            LevyraLiveRadioCatalog.forCode(languageCode).internetRequired
        }
    }

    fun leaveLiveRadioQueue() {
        val preserved = liveRadioQueueSnapshot ?: return
        saveLiveRadioPlayback(null)
        liveRadioQueueSnapshot = null
        liveRadioPlayNextPendingIdentities = emptyList()
        liveRadioRecoveryAttempt = 0
        liveRadioSessionRecoveries = 0
        liveRadioTriedStreams = emptySet()
        liveRadioSiblingStreams = null
        host.streamRecoveryJob?.cancel()
        liveRadioArtworkJob?.cancel()
        liveRadioArtwork = ""
        queueEngine.endTransientPlayback(preserved)
        uiState.update {
            it.copy(
                liveRadioStation = null,
                liveRadioNowPlaying = "",
                liveRadioReconnectAttempt = 0
            )
        }
    }

    fun playNextInPreservedLiveRadioQueue(additions: List<Track>): Boolean {
        val preserved = liveRadioQueueSnapshot ?: return false
        val mutation = queueAfterPlayNextIntent(
            current = preserved.tracks,
            currentIndex = preserved.currentIndex,
            pendingIdentities = liveRadioPlayNextPendingIdentities,
            additions = additions
        )
        liveRadioPlayNextPendingIdentities = mutation.pendingIdentities
        liveRadioQueueSnapshot = preserved.copy(
            tracks = mutation.tracks,
            currentIndex = mutation.currentIndex,
            generation = preserved.generation + 1L
        )
        return true
    }
}
