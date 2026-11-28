package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.ArtistRepository
import com.luc4n3x.levyra.data.ListeningPulseStore
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.toTrack
import com.luc4n3x.levyra.data.recap.ListeningRecapRepository
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.ListeningDnaEngine
import com.luc4n3x.levyra.domain.ListeningDnaPeriod
import com.luc4n3x.levyra.domain.ListeningInsightsTrack
import com.luc4n3x.levyra.domain.ListeningPulseEngine
import com.luc4n3x.levyra.domain.PulseTrack
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.recap.ListeningRecapPeriod
import com.luc4n3x.levyra.domain.recap.ListeningRecapSummary
import com.luc4n3x.levyra.domain.recap.TopArtistStat
import com.luc4n3x.levyra.domain.recap.TopTrackStat
import com.luc4n3x.levyra.feature.scrobbling.ScrobblingCoordinator
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val LISTEN_SESSION_FLUSH_INTERVAL_MS = 30_000L

internal class ListeningSessionController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val scrobbling: ScrobblingCoordinator,
    private val listeningPulseStore: ListeningPulseStore,
    private val listeningRecapRepository: ListeningRecapRepository,
    private val artistRepository: ArtistRepository,
    private val repository: YoutubeMusicRepository,
    private val database: LevyraDatabase,
    private val refreshListeningPulse: () -> Unit,
    private val playFrom: (List<Track>, Track) -> Unit,
    private val playDownloaded: (DownloadedTrack) -> Unit
) {
    var listenSessionSignificant = false
    var listenSessionTrack: Track? = null
    private var listenSessionStartedAt = 0L
    var listenSessionAccumulatedMs = 0L
    var listenSessionCompleted = false
    var listenTickElapsedMs = 0L
    private var listenSessionPersistedMs = 0L
    private var listenSessionPersistJob: Job? = null
    private var listeningRecapJob: Job? = null
    private var listeningDnaJob: Job? = null

    fun beginListenSession(track: Track) {
        flushListenSession()
        listenSessionTrack = track.copy(streamUrl = "", videoStreamUrl = "")
        listenSessionStartedAt = System.currentTimeMillis()
        listenSessionAccumulatedMs = 0L
        listenSessionCompleted = false
        listenSessionSignificant = false
        listenTickElapsedMs = android.os.SystemClock.elapsedRealtime()
        listenSessionPersistedMs = 0L
        val startedAt = listenSessionStartedAt
        scope.launch(Dispatchers.IO) { scrobbling.nowPlaying(track, startedAt) }
    }

    private data class ListenSession(
        val track: Track,
        val startedAt: Long,
        val listenedMs: Long,
        val completed: Boolean
    )

    private fun takeListenSession(): ListenSession? {
        val track = listenSessionTrack ?: return null
        val session = ListenSession(track, listenSessionStartedAt, listenSessionAccumulatedMs, listenSessionCompleted)
        listenSessionTrack = null
        listenSessionAccumulatedMs = 0L
        listenSessionCompleted = false
        listenSessionSignificant = false
        listenSessionPersistedMs = 0L
        return session.takeIf { it.listenedMs >= ListeningPulseEngine.MIN_LISTEN_MS }
    }

    fun flushListenSession() {
        val session = takeListenSession() ?: return
        scope.launch(Dispatchers.IO) {
            scrobbling.scrobble(session.track, session.startedAt, session.listenedMs)
            listeningPulseStore.record(session.track, session.listenedMs, session.completed, session.startedAt)
            refreshListeningPulse()
        }
    }

    fun flushListenSessionBlocking() {
        val session = takeListenSession() ?: return
        listeningPulseStore.recordSync(session.track, session.listenedMs, session.completed, session.startedAt)
    }

    fun maybePersistListenSession() {
        val track = listenSessionTrack ?: return
        val listenedMs = listenSessionAccumulatedMs
        if (listenedMs < ListeningPulseEngine.MIN_LISTEN_MS) return
        if (listenedMs - listenSessionPersistedMs < LISTEN_SESSION_FLUSH_INTERVAL_MS) return
        if (listenSessionPersistJob?.isActive == true) return
        val startedAt = listenSessionStartedAt
        listenSessionPersistJob = scope.launch(Dispatchers.IO) {
            scrobbling.scrobble(track, startedAt, listenedMs)
            listeningPulseStore.record(track, listenedMs, completed = false, startedAt = startedAt)
            if (listenSessionStartedAt == startedAt && listenSessionTrack?.id == track.id) {
                listenSessionPersistedMs = maxOf(listenSessionPersistedMs, listenedMs)
            }
            refreshListeningPulse()
        }
    }

    fun playListeningDnaTrack(entry: PulseTrack) {
        val snapshot = uiState.value
        val local = (snapshot.recentListens + snapshot.mostPlayedTracks).firstOrNull { candidate ->
            candidate.id.isNotBlank() && candidate.id == entry.trackId
        }
        if (local != null) {
            playFrom(listOf(local), local)
            return
        }
        scope.launch {
            val resolved = try {
                repository.searchSongMatch(entry.title, entry.artist, snapshot.languageCode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Listening DNA track resolve failed")
                null
            } ?: return@launch
            playFrom(listOf(resolved), resolved)
        }
    }
    fun openListeningRecap(period: ListeningRecapPeriod = uiState.value.listeningRecapPeriod) {
        uiState.update { it.copy(showListeningRecap = true, listeningRecapPeriod = period) }
        refreshListeningRecap(period)
    }

    fun openListeningInsights() {
        uiState.update { it.copy(showListeningInsights = true) }
    }

    fun closeListeningInsights() {
        if (!uiState.value.showListeningInsights) return
        uiState.update { it.copy(showListeningInsights = false) }
    }

    fun playListeningInsightsTrack(entry: ListeningInsightsTrack) {
        playListeningRecapTrack(
            TopTrackStat(
                rank = 0,
                trackId = entry.trackId,
                title = entry.title,
                artist = entry.artist,
                album = entry.album,
                thumbnailUrl = entry.artworkUrl,
                plays = entry.plays,
                listenedMs = entry.listenedMs
            )
        )
    }

    fun closeListeningRecap() {
        if (!uiState.value.showListeningRecap) return
        uiState.update { it.copy(showListeningRecap = false) }
    }

    fun selectListeningRecapPeriod(period: ListeningRecapPeriod) {
        if (uiState.value.listeningRecapPeriod == period && uiState.value.listeningRecap.hasSignal) return
        val cached = listeningRecapRepository.peekCached(period)
        uiState.update { current ->
            current.copy(
                listeningRecapPeriod = period,
                listeningRecap = cached ?: ListeningRecapSummary(period = period),
                listeningRecapLoading = cached == null
            )
        }
        refreshListeningRecap(period)
    }

    fun refreshListeningRecap(period: ListeningRecapPeriod = uiState.value.listeningRecapPeriod, force: Boolean = false) {
        listeningRecapJob?.cancel()
        listeningRecapJob = scope.launch {
            uiState.update { it.copy(listeningRecapLoading = true) }
            try {
                val rawRecap = listeningRecapRepository.getRecap(period, force = force)
                val recap = enrichListeningRecapArtists(rawRecap)
                uiState.update { current ->
                    if (current.listeningRecapPeriod != period) {
                        current.copy(listeningRecapLoading = false)
                    } else {
                        current.copy(listeningRecap = recap, listeningRecapLoading = false)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Listening recap load failed")
                uiState.update { it.copy(listeningRecapLoading = false) }
            }
        }
    }

    private suspend fun enrichListeningRecapArtists(recap: ListeningRecapSummary): ListeningRecapSummary {
        if (recap.topArtists.isEmpty()) return recap
        val enriched = coroutineScope {
            recap.topArtists.map { artist ->
                async(Dispatchers.IO) { resolveRecapArtist(artist) }
            }.awaitAll()
        }
        return recap.copy(topArtists = enriched)
    }

    private suspend fun resolveRecapArtist(artist: TopArtistStat): TopArtistStat {
        val lookupName = artist.lookupName.ifBlank { artist.name }
        return try {
            val hit = if (artist.browseId.isNotBlank()) {
                artistRepository.artistHit(artist.browseId, lookupName)
            } else {
                artistRepository.artistHitFor(artist.name)
            }
            if (hit == null || hit.thumbnailUrl.isBlank()) {
                artist
            } else {
                artist.copy(
                    name = hit.name.ifBlank { artist.name },
                    browseId = hit.browseId.ifBlank { artist.browseId },
                    thumbnailUrl = hit.thumbnailUrl
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Listening recap artist artwork resolve failed for ${artist.name}")
            artist
        }
    }

    fun playListeningRecapTrack(entry: TopTrackStat) {
        val snapshot = uiState.value
        val inMemoryCandidate = listOf(
            snapshot.recentListens,
            snapshot.mostPlayedTracks,
            snapshot.favorites,
            snapshot.tracks,
            snapshot.queue,
            snapshot.recentSearches,
            snapshot.forgottenFavorites,
            snapshot.personalOrbitTracks,
            snapshot.quickPickSeeds,
            snapshot.charts,
            snapshot.exploreTracks,
            snapshot.exploreFreshTracks
        ).asSequence().flatten().firstOrNull { candidate ->
            candidate.id.isNotBlank() && candidate.id == entry.trackId
        }
        if (inMemoryCandidate != null) {
            playFrom(listOf(inMemoryCandidate), inMemoryCandidate)
            return
        }
        val downloaded = snapshot.downloads.firstOrNull { it.trackId == entry.trackId }
        if (downloaded != null) {
            playDownloaded(downloaded)
            return
        }
        scope.launch {
            if (entry.trackId.isNotBlank()) {
                val dbTrack = try {
                    withContext(Dispatchers.IO) {
                        database.listenEventsDao()
                            .findLatestByTrackId(entry.trackId)
                            ?.toTrack()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Listening recap DB lookup failed")
                    null
                }
                if (dbTrack != null) {
                    playFrom(listOf(dbTrack), dbTrack)
                    return@launch
                }
            }
            val resolved = try {
                repository.searchSongMatch(entry.title, entry.artist, snapshot.languageCode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Listening recap track resolve failed")
                null
            }
            if (resolved != null) {
                playFrom(listOf(resolved), resolved)
            }
        }
    }

    fun openYourSound() {
        uiState.update { it.copy(showYourSound = true) }
        refreshListeningDna(uiState.value.listeningDnaPeriod)
    }

    fun closeYourSound() {
        if (!uiState.value.showYourSound) return
        uiState.update { it.copy(showYourSound = false) }
    }

    fun selectListeningDnaPeriod(period: ListeningDnaPeriod) {
        if (uiState.value.listeningDnaPeriod == period && uiState.value.listeningDna.hasSignal) return
        uiState.update { it.copy(listeningDnaPeriod = period) }
        refreshListeningDna(period)
    }

    fun refreshListeningDna(period: ListeningDnaPeriod) {
        listeningDnaJob?.cancel()
        listeningDnaJob = scope.launch {
            uiState.update { it.copy(listeningDnaLoading = true) }
            try {
                val events = listeningPulseStore.eventsWindow()
                val zoneOffsetMs = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
                val lifetime = if (period == ListeningDnaPeriod.AllTime) {
                    listeningPulseStore.lifetime()
                } else {
                    null
                }
                val dna = withContext(Dispatchers.Default) {
                    if (lifetime != null) {
                        ListeningDnaEngine.buildAllTime(lifetime, events, zoneOffsetMs = zoneOffsetMs)
                    } else {
                        ListeningDnaEngine.build(events, period, zoneOffsetMs = zoneOffsetMs)
                    }
                }
                uiState.update { current ->
                    if (current.listeningDnaPeriod != period) {
                        current.copy(listeningDnaLoading = false)
                    } else {
                        current.copy(listeningDna = dna, listeningDnaLoading = false)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Listening DNA build failed")
                uiState.update { it.copy(listeningDnaLoading = false) }
            }
        }
    }

    fun cancelListeningDna() {
        listeningDnaJob?.cancel()
    }
}
