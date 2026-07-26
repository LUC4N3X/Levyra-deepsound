package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorRateLimitException
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import com.luc4n3x.levyra.desktop.core.storage.SessionData
import com.luc4n3x.levyra.desktop.core.storage.SessionStore
import com.luc4n3x.levyra.desktop.core.storage.SettingsStore
import com.luc4n3x.levyra.desktop.core.stream.ResolvedAudio
import com.luc4n3x.levyra.desktop.core.stream.StreamResolver
import com.luc4n3x.levyra.desktop.player.AudioPlayer
import com.luc4n3x.levyra.desktop.player.AudioPlayerUnavailableException
import com.luc4n3x.levyra.desktop.player.PlaybackStatus
import com.luc4n3x.levyra.desktop.player.PlayerEvent
import com.luc4n3x.levyra.desktop.player.PlayerQueue
import com.luc4n3x.levyra.desktop.player.RepeatMode
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    private var consecutiveFailures: Int = 0
    private var pendingResumeMs: Long = 0L
    private var lastAudibleVolume: Int = settingsStore.current.volume.takeIf { it > 0 } ?: DEFAULT_VOLUME

    val state: StateFlow<PlaybackUiState> = internalState.asStateFlow()
    val messages: SharedFlow<String> = messageFlow.asSharedFlow()

    init {
        playerScope.launch {
            settingsStore.settings
                .map { it.equalizer }
                .distinctUntilChanged()
                .collect { equalizer ->
                    player?.applyEqualizer(equalizer.enabled, equalizer.preamp, equalizer.amps)
                }
        }
    }

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
        internalState.update { state ->
            state.copy(
                queue = queue,
                positionMs = pendingResumeMs,
                durationMs = queue.current?.durationMs ?: 0L
            )
        }
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val queue = internalState.value.queue.replace(tracks, startIndex)
        internalState.update { state -> state.copy(queue = queue) }
        startCurrent(0L)
    }

    fun playTrack(track: Track) = playTracks(listOf(track))

    fun playShuffled(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val shuffled = internalState.value.queue.copy(shuffle = true).replace(tracks, 0)
        internalState.update { state -> state.copy(queue = shuffled) }
        startCurrent(0L)
    }

    fun enqueueNext(tracks: List<Track>) {
        val queue = internalState.value.queue
        val wasEmpty = queue.isEmpty
        internalState.update { state -> state.copy(queue = queue.enqueueNext(tracks)) }
        if (wasEmpty) startCurrent(0L) else persistSession()
    }

    fun enqueueLast(tracks: List<Track>) {
        val queue = internalState.value.queue
        val wasEmpty = queue.isEmpty
        internalState.update { state -> state.copy(queue = queue.enqueueLast(tracks)) }
        if (wasEmpty) startCurrent(0L) else persistSession()
    }

    fun jumpTo(position: Int) {
        val queue = internalState.value.queue.jumpTo(position)
        if (queue == internalState.value.queue && internalState.value.status != PlaybackStatus.IDLE) return
        internalState.update { state -> state.copy(queue = queue) }
        startCurrent(0L)
    }

    fun removeFromQueue(position: Int) {
        val previous = internalState.value.queue
        val playingRemoved = position == previous.index
        val queue = previous.removeAt(position)
        internalState.update { state -> state.copy(queue = queue) }
        when {
            queue.isEmpty -> stop()
            playingRemoved -> startCurrent(0L)
            else -> persistSession()
        }
    }

    fun clearQueue() {
        internalState.update { state -> state.copy(queue = state.queue.clear()) }
        stop()
    }

    fun togglePlayPause() {
        val current = internalState.value
        if (current.queue.isEmpty) return
        when (current.status) {
            PlaybackStatus.PLAYING, PlaybackStatus.BUFFERING -> {
                player?.pause()
                internalState.update { state -> state.copy(status = PlaybackStatus.PAUSED) }
                persistSession()
            }

            PlaybackStatus.PAUSED -> {
                player?.resume()
                internalState.update { state -> state.copy(status = PlaybackStatus.PLAYING) }
            }

            PlaybackStatus.OPENING -> Unit

            PlaybackStatus.IDLE, PlaybackStatus.ENDED, PlaybackStatus.FAILED -> startCurrent(pendingResumeMs)
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
        internalState.update { state -> state.copy(queue = advanced) }
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
        internalState.update { state -> state.copy(queue = rewound) }
        startCurrent(0L)
    }

    fun seekTo(positionMs: Long) {
        val safe = positionMs.coerceAtLeast(0L)
        pendingResumeMs = safe
        player?.seekTo(safe)
        internalState.update { state -> state.copy(positionMs = safe) }
    }

    fun setVolume(volume: Int) {
        val safe = volume.coerceIn(0, 100)
        val muted = safe == 0
        if (safe > 0) {
            lastAudibleVolume = safe
        }
        player?.setVolume(safe)
        player?.setMuted(muted)
        internalState.update { state -> state.copy(volume = safe, muted = muted) }
        settingsStore.update { it.copy(volume = safe) }
    }

    fun toggleMuted() {
        val current = internalState.value
        val muted = !current.muted
        if (!muted && current.volume == 0) {
            setVolume(lastAudibleVolume)
            return
        }
        player?.setMuted(muted)
        internalState.update { state -> state.copy(muted = muted) }
    }

    fun toggleShuffle() {
        val queue = internalState.value.queue
        internalState.update { state -> state.copy(queue = queue.withShuffle(!queue.shuffle)) }
        persistSession()
    }

    fun cycleRepeat() {
        val queue = internalState.value.queue
        internalState.update { state -> state.copy(queue = queue.withRepeat(queue.repeat.next())) }
        persistSession()
    }

    fun stop() {
        playbackJob?.cancel()
        player?.stop()
        internalState.update { state ->
            state.copy(
                status = PlaybackStatus.IDLE,
                positionMs = 0L,
                preparingTrackId = ""
            )
        }
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
        internalState.update { state ->
            state.copy(
                preparingTrackId = track.id,
                status = PlaybackStatus.OPENING,
                positionMs = startAtMs,
                durationMs = track.durationMs,
                streamLabel = ""
            )
        }
        playbackJob = playerScope.launch { runPlayback(track, startAtMs, forceRestart) }
    }

    private suspend fun runPlayback(track: Track, startAtMs: Long, forceRestart: Boolean) {
        val activePlayer = ensurePlayer()
        if (activePlayer == null) {
            internalState.update { state ->
                state.copy(preparingTrackId = "", status = PlaybackStatus.FAILED)
            }
            return
        }
        if (forceRestart) {
            activePlayer.stop()
        }
        val playable = resolvePlayable(track) ?: return
        val resolved = resolveStream(playable, track) ?: return
        beginPlayback(activePlayer, track, resolved, startAtMs)
    }

    private suspend fun resolvePlayable(track: Track): Track? {
        if (track.videoUrl.isNotBlank()) return track
        val located = try {
            catalog.findPlayable(track)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            if (isRateLimited(error)) {
                reportRateLimit(error)
                return null
            }
            null
        }
        if (located == null) {
            internalState.update { state -> state.copy(preparingTrackId = "") }
            messageFlow.tryEmit("Nessuna versione riproducibile trovata per ${track.title}")
            skipAfterFailure(track)
            return null
        }
        val merged = track.copy(
            videoUrl = located.videoUrl,
            durationMs = if (located.durationMs > 0L) located.durationMs else track.durationMs
        )
        updateTrackMetadata(merged)
        return merged
    }

    private suspend fun resolveStream(playable: Track, track: Track): ResolvedAudio? {
        val settings = settingsStore.current
        return try {
            resolver.resolve(playable, settings.audioQuality, settings.preferredCodec)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            if (isRateLimited(error)) {
                reportRateLimit(error)
                return null
            }
            internalState.update { state -> state.copy(preparingTrackId = "") }
            messageFlow.tryEmit(error.message ?: "Impossibile risolvere lo stream")
            skipAfterFailure(track)
            null
        }
    }

    private fun beginPlayback(
        activePlayer: AudioPlayer,
        track: Track,
        resolved: ResolvedAudio,
        startAtMs: Long
    ) {
        val enriched = track.copy(
            title = resolved.title.ifBlank { track.title },
            artist = resolved.artist.ifBlank { track.artist },
            artworkUrl = resolved.artworkUrl.ifBlank { track.artworkUrl },
            durationMs = if (resolved.durationMs > 0L) resolved.durationMs else track.durationMs
        )
        updateTrackMetadata(enriched)
        activePlayer.play(resolved.url, startAtMs)
        val current = internalState.value
        activePlayer.setVolume(current.volume)
        activePlayer.setMuted(current.muted)
        internalState.update { state ->
            state.copy(
                preparingTrackId = "",
                status = PlaybackStatus.BUFFERING,
                streamLabel = resolved.label,
                durationMs = if (resolved.durationMs > 0L) resolved.durationMs else state.durationMs
            )
        }
        libraryStore.recordPlayback(enriched)
        persistSession()
    }

    private fun updateTrackMetadata(track: Track) {
        val queue = internalState.value.queue
        val position = queue.items.indexOfFirst { it.id == track.id }
        if (position < 0) return
        val items = queue.items.toMutableList().apply { set(position, track) }
        val original = queue.original.map { if (it.id == track.id) track else it }
        internalState.update { state -> state.copy(queue = queue.copy(items = items, original = original)) }
    }

    private fun isRateLimited(error: Throwable): Boolean {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (current is ExtractorRateLimitException) return true
            val next = current.cause
            if (next === current) return false
            current = next
            depth += 1
        }
        return false
    }

    private fun reportRateLimit(error: Throwable) {
        consecutiveFailures = 0
        internalState.update { state -> state.copy(preparingTrackId = "") }
        messageFlow.tryEmit(error.message ?: "Richieste temporaneamente limitate")
        stop()
    }

    private fun skipAfterFailure(track: Track) {
        resolver.invalidate(track)
        val queue = internalState.value.queue
        consecutiveFailures += 1
        if (queue.items.size <= 1 || consecutiveFailures >= queue.items.size) {
            consecutiveFailures = 0
            stop()
            return
        }
        val advanced = queue.advance(automatic = false)
        if (advanced == null) {
            consecutiveFailures = 0
            stop()
            return
        }
        internalState.update { state -> state.copy(queue = advanced) }
        startCurrent(0L)
    }

    private fun extendWithRadio() {
        val seed = internalState.value.queue.items.lastOrNull() ?: return
        playerScope.launch {
            val tracks = try {
                catalog.radio(seed)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                emptyList()
            }
            if (tracks.isEmpty()) {
                stop()
                return@launch
            }
            val queue = internalState.value.queue
                .trimPlayed(MAX_QUEUE_SIZE)
                .enqueueLast(tracks)
            val advanced = queue.advance(automatic = false)
            if (advanced == null) {
                stop()
                return@launch
            }
            internalState.update { state -> state.copy(queue = advanced) }
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
            val equalizer = settingsStore.current.equalizer
            created.applyEqualizer(equalizer.enabled, equalizer.preamp, equalizer.amps)
            internalState.update { state -> state.copy(unavailableReason = "") }
            created
        } catch (error: AudioPlayerUnavailableException) {
            internalState.update { state -> state.copy(unavailableReason = error.message.orEmpty()) }
            messageFlow.tryEmit(error.message.orEmpty())
            null
        } catch (error: Throwable) {
            val reason = error.message ?: "Motore audio non disponibile"
            internalState.update { state -> state.copy(unavailableReason = reason) }
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
            is PlayerEvent.Opening ->
                internalState.update { state -> state.copy(status = PlaybackStatus.OPENING) }

            is PlayerEvent.Buffering -> if (internalState.value.status != PlaybackStatus.PLAYING) {
                internalState.update { state -> state.copy(status = PlaybackStatus.BUFFERING) }
            }

            is PlayerEvent.Playing -> {
                retriedTrackId = ""
                consecutiveFailures = 0
                internalState.update { state -> state.copy(status = PlaybackStatus.PLAYING) }
            }

            is PlayerEvent.Paused ->
                internalState.update { state -> state.copy(status = PlaybackStatus.PAUSED) }

            is PlayerEvent.Stopped -> Unit

            is PlayerEvent.Finished -> next(automatic = true)

            is PlayerEvent.Failed -> handleFailure(event.reason)

            is PlayerEvent.TimeChanged -> {
                pendingResumeMs = event.positionMs
                internalState.update { state -> state.copy(positionMs = event.positionMs) }
            }

            is PlayerEvent.LengthChanged -> if (event.durationMs > 0L) {
                internalState.update { state -> state.copy(durationMs = event.durationMs) }
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
        const val MAX_QUEUE_SIZE = 200
        const val MAX_CAUSE_DEPTH = 8
        const val DEFAULT_VOLUME = 60
    }
}
