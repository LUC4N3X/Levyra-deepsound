package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import com.luc4n3x.levyra.desktop.core.storage.SessionData
import com.luc4n3x.levyra.desktop.core.storage.SessionStore
import com.luc4n3x.levyra.desktop.core.storage.SettingsStore
import com.luc4n3x.levyra.desktop.core.stream.StreamResolver
import com.luc4n3x.levyra.desktop.player.AudioPlayer
import com.luc4n3x.levyra.desktop.player.AudioPlayerUnavailableException
import com.luc4n3x.levyra.desktop.player.PlaybackStatus
import com.luc4n3x.levyra.desktop.player.PlayerEvent
import com.luc4n3x.levyra.desktop.player.PlayerQueue
import com.luc4n3x.levyra.desktop.player.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val queue: PlayerQueue = PlayerQueue(),
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Int = 80,
    val muted: Boolean = false,
    val streamLabel: String = "",
    val preparingTrackId: String = "",
    val unavailableReason: String = ""
) {
    val current: Track? get() = queue.current
    val isPlaying: Boolean get() = status == PlaybackStatus.PLAYING
    val isBusy: Boolean get() = preparingTrackId.isNotEmpty() || status == PlaybackStatus.BUFFERING
}

class PlaybackController(
    private val scope: CoroutineScope,
    private val resolver: StreamResolver,
    private val catalog: CatalogRepository,
    private val settingsStore: SettingsStore,
    private val libraryStore: LibraryStore,
    private val sessionStore: SessionStore,
    private val playerFactory: () -> AudioPlayer
) {
    private val internalState = MutableStateFlow(
        PlaybackUiState(
            volume = settingsStore.current.volume,
            queue = PlayerQueue(repeat = RepeatMode.OFF)
        )
    )
    private val messageFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)

    private val playerScope = CoroutineScope(scope.coroutineContext + SupervisorJob())

    private var player: AudioPlayer? = null
    private var playbackJob: Job? = null
    private var eventJob: Job? = null
    private var persistJob: Job? = null
    private var retriedTrackId: String = ""
    private var pendingResumeMs: Long = 0L

    val state: StateFlow<PlaybackUiState> = internalState.asStateFlow()
    val messages: SharedFlow<String> = messageFlow.asSharedFlow()

    fun restoreSession() {
        val session = sessionStore.read()
        if (session.queue.isEmpty()) return
        val queue = PlayerQueue(
            items = session.queue,
            original = session.queue,
            index = session.index.coerceIn(0, session.queue.lastIndex),
            shuffle = session.shuffle,
            repeat = RepeatMode.fromName(session.repeat)
        )
        pendingResumeMs = session.positionMs.coerceAtLeast(0L)
        internalState.value = internalState.value.copy(
            queue = queue,
            positionMs = pendingResumeMs,
            durationMs = queue.current?.durationMs ?: 0L
        )
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val queue = internalState.value.queue.replace(tracks, startIndex)
        internalState.value = internalState.value.copy(queue = queue)
        startCurrent(0L)
    }

    fun playTrack(track: Track) = playTracks(listOf(track))

    fun playShuffled(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val shuffled = internalState.value.queue.copy(shuffle = true).replace(tracks, 0)
        internalState.value = internalState.value.copy(queue = shuffled)
        startCurrent(0L)
    }

    fun enqueueNext(tracks: List<Track>) {
        val queue = internalState.value.queue
        val wasEmpty = queue.isEmpty
        internalState.value = internalState.value.copy(queue = queue.enqueueNext(tracks))
        if (wasEmpty) startCurrent(0L) else persistSession()
    }

    fun enqueueLast(tracks: List<Track>) {
        val queue = internalState.value.queue
        val wasEmpty = queue.isEmpty
        internalState.value = internalState.value.copy(queue = queue.enqueueLast(tracks))
        if (wasEmpty) startCurrent(0L) else persistSession()
    }

    fun jumpTo(position: Int) {
        val queue = internalState.value.queue.jumpTo(position)
        if (queue == internalState.value.queue && internalState.value.status != PlaybackStatus.IDLE) return
        internalState.value = internalState.value.copy(queue = queue)
        startCurrent(0L)
    }

    fun removeFromQueue(position: Int) {
        val previous = internalState.value.queue
        val playingRemoved = position == previous.index
        val queue = previous.removeAt(position)
        internalState.value = internalState.value.copy(queue = queue)
        when {
            queue.isEmpty -> stop()
            playingRemoved -> startCurrent(0L)
            else -> persistSession()
        }
    }

    fun clearQueue() {
        internalState.value = internalState.value.copy(queue = internalState.value.queue.clear())
        stop()
    }

    fun togglePlayPause() {
        val current = internalState.value
        if (current.queue.isEmpty) return
        when (current.status) {
            PlaybackStatus.PLAYING -> {
                player?.pause()
                internalState.value = current.copy(status = PlaybackStatus.PAUSED)
                persistSession()
            }

            PlaybackStatus.PAUSED -> {
                player?.resume()
                internalState.value = current.copy(status = PlaybackStatus.PLAYING)
            }

            else -> startCurrent(pendingResumeMs)
        }
    }

    fun next(automatic: Boolean = false) {
        val current = internalState.value
        if (current.queue.isEmpty) return
        val advanced = current.queue.advance(automatic)
        if (advanced == null) {
            if (automatic && settingsStore.current.autoplayRadio) {
                extendWithRadio()
            } else {
                stop()
            }
            return
        }
        val repeatingSame = automatic && advanced.index == current.queue.index
        internalState.value = current.copy(queue = advanced)
        startCurrent(0L, forceRestart = repeatingSame)
    }

    fun previous() {
        val current = internalState.value
        if (current.queue.isEmpty) return
        if (current.positionMs > RESTART_THRESHOLD_MS) {
            seekTo(0L)
            return
        }
        val rewound = current.queue.rewind() ?: run {
            seekTo(0L)
            return
        }
        internalState.value = current.copy(queue = rewound)
        startCurrent(0L)
    }

    fun seekTo(positionMs: Long) {
        val safe = positionMs.coerceAtLeast(0L)
        pendingResumeMs = safe
        player?.seekTo(safe)
        internalState.value = internalState.value.copy(positionMs = safe)
    }

    fun setVolume(volume: Int) {
        val safe = volume.coerceIn(0, 100)
        player?.setVolume(safe)
        internalState.value = internalState.value.copy(volume = safe, muted = safe == 0)
        settingsStore.update { it.copy(volume = safe) }
    }

    fun toggleMuted() {
        val muted = !internalState.value.muted
        player?.setMuted(muted)
        internalState.value = internalState.value.copy(muted = muted)
    }

    fun toggleShuffle() {
        val queue = internalState.value.queue
        internalState.value = internalState.value.copy(queue = queue.withShuffle(!queue.shuffle))
        persistSession()
    }

    fun cycleRepeat() {
        val queue = internalState.value.queue
        internalState.value = internalState.value.copy(queue = queue.withRepeat(queue.repeat.next()))
        persistSession()
    }

    fun stop() {
        playbackJob?.cancel()
        player?.stop()
        internalState.value = internalState.value.copy(
            status = PlaybackStatus.IDLE,
            positionMs = 0L,
            preparingTrackId = ""
        )
        pendingResumeMs = 0L
        persistSession()
    }

    fun shutdown() {
        persistJob?.cancel()
        playbackJob?.cancel()
        eventJob?.cancel()
        saveSessionNow()
        runCatching { player?.stop() }
        runCatching { player?.close() }
        player = null
        playerScope.cancel()
    }

    private fun startCurrent(startAtMs: Long, forceRestart: Boolean = false) {
        val track = internalState.value.queue.current ?: return
        playbackJob?.cancel()
        pendingResumeMs = startAtMs
        internalState.value = internalState.value.copy(
            preparingTrackId = track.id,
            status = PlaybackStatus.OPENING,
            positionMs = startAtMs,
            durationMs = track.durationMs,
            streamLabel = ""
        )
        playbackJob = playerScope.launch {
            val activePlayer = ensurePlayer()
            if (activePlayer == null) {
                internalState.value = internalState.value.copy(
                    preparingTrackId = "",
                    status = PlaybackStatus.FAILED
                )
                return@launch
            }
            if (forceRestart) {
                activePlayer.stop()
            }
            val settings = settingsStore.current
            val resolved = runCatching {
                resolver.resolve(track, settings.audioQuality, settings.preferredCodec)
            }.getOrElse { error ->
                internalState.value = internalState.value.copy(preparingTrackId = "")
                messageFlow.tryEmit(error.message ?: "Impossibile risolvere lo stream")
                skipAfterFailure(track)
                return@launch
            }
            val enriched = track.copy(
                title = resolved.title.ifBlank { track.title },
                artist = resolved.artist.ifBlank { track.artist },
                artworkUrl = resolved.artworkUrl.ifBlank { track.artworkUrl },
                durationMs = if (resolved.durationMs > 0L) resolved.durationMs else track.durationMs
            )
            updateTrackMetadata(enriched)
            activePlayer.play(resolved.url, startAtMs)
            activePlayer.setVolume(internalState.value.volume)
            activePlayer.setMuted(internalState.value.muted)
            internalState.value = internalState.value.copy(
                preparingTrackId = "",
                status = PlaybackStatus.BUFFERING,
                streamLabel = resolved.label,
                durationMs = if (resolved.durationMs > 0L) resolved.durationMs else internalState.value.durationMs
            )
            libraryStore.recordPlayback(enriched)
            persistSession()
        }
    }

    private fun updateTrackMetadata(track: Track) {
        val queue = internalState.value.queue
        val position = queue.items.indexOfFirst { it.id == track.id }
        if (position < 0) return
        val items = queue.items.toMutableList().apply { set(position, track) }
        val original = queue.original.map { if (it.id == track.id) track else it }
        internalState.value = internalState.value.copy(queue = queue.copy(items = items, original = original))
    }

    private fun skipAfterFailure(track: Track) {
        resolver.invalidate(track)
        val queue = internalState.value.queue
        if (queue.items.size <= 1) {
            stop()
            return
        }
        next(automatic = true)
    }

    private fun extendWithRadio() {
        val seed = internalState.value.queue.items.lastOrNull() ?: return
        playerScope.launch {
            val tracks = runCatching { catalog.radio(seed) }.getOrDefault(emptyList())
            if (tracks.isEmpty()) {
                stop()
                return@launch
            }
            val queue = internalState.value.queue.enqueueLast(tracks)
            val advanced = queue.advance(automatic = false)
            if (advanced == null) {
                stop()
                return@launch
            }
            internalState.value = internalState.value.copy(queue = advanced)
            startCurrent(0L)
        }
    }

    private fun ensurePlayer(): AudioPlayer? {
        player?.let { return it }
        return try {
            val created = playerFactory()
            player = created
            observeEvents(created)
            startPersistLoop()
            internalState.value = internalState.value.copy(unavailableReason = "")
            created
        } catch (error: AudioPlayerUnavailableException) {
            internalState.value = internalState.value.copy(unavailableReason = error.message.orEmpty())
            messageFlow.tryEmit(error.message.orEmpty())
            null
        } catch (error: Throwable) {
            val reason = error.message ?: "Motore audio non disponibile"
            internalState.value = internalState.value.copy(unavailableReason = reason)
            messageFlow.tryEmit(reason)
            null
        }
    }

    private fun observeEvents(target: AudioPlayer) {
        eventJob?.cancel()
        eventJob = playerScope.launch {
            target.events.collect { event -> handleEvent(event) }
        }
    }

    private fun handleEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.Opening -> internalState.value =
                internalState.value.copy(status = PlaybackStatus.OPENING)

            is PlayerEvent.Buffering -> if (internalState.value.status != PlaybackStatus.PLAYING) {
                internalState.value = internalState.value.copy(status = PlaybackStatus.BUFFERING)
            }

            is PlayerEvent.Playing -> {
                retriedTrackId = ""
                internalState.value = internalState.value.copy(status = PlaybackStatus.PLAYING)
            }

            is PlayerEvent.Paused -> internalState.value =
                internalState.value.copy(status = PlaybackStatus.PAUSED)

            is PlayerEvent.Stopped -> Unit

            is PlayerEvent.Finished -> next(automatic = true)

            is PlayerEvent.Failed -> handleFailure(event.reason)

            is PlayerEvent.TimeChanged -> {
                pendingResumeMs = event.positionMs
                internalState.value = internalState.value.copy(positionMs = event.positionMs)
            }

            is PlayerEvent.LengthChanged -> if (event.durationMs > 0L) {
                internalState.value = internalState.value.copy(durationMs = event.durationMs)
            }
        }
    }

    private fun handleFailure(reason: String) {
        val track = internalState.value.queue.current ?: return
        if (retriedTrackId != track.id) {
            retriedTrackId = track.id
            resolver.invalidate(track)
            startCurrent(0L)
            return
        }
        messageFlow.tryEmit(reason)
        skipAfterFailure(track)
    }

    private fun startPersistLoop() {
        if (persistJob?.isActive == true) return
        persistJob = playerScope.launch {
            while (isActive) {
                delay(PERSIST_INTERVAL_MS)
                if (internalState.value.status == PlaybackStatus.PLAYING) {
                    saveSessionNow()
                }
            }
        }
    }

    private fun persistSession() {
        playerScope.launch { saveSessionNow() }
    }

    private fun saveSessionNow() {
        val current = internalState.value
        runCatching {
            sessionStore.write(
                SessionData(
                    queue = current.queue.items,
                    index = current.queue.index.coerceAtLeast(0),
                    positionMs = current.positionMs,
                    shuffle = current.queue.shuffle,
                    repeat = current.queue.repeat.name
                )
            )
        }
    }

    private companion object {
        const val RESTART_THRESHOLD_MS = 4_000L
        const val PERSIST_INTERVAL_MS = 15_000L
    }
}
