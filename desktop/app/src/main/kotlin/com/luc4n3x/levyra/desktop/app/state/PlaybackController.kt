package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.app.DesktopDiagnostics
import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorRateLimitException
import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import com.luc4n3x.levyra.desktop.core.model.SleepTimerMode
import com.luc4n3x.levyra.desktop.core.model.SleepTimerState
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import com.luc4n3x.levyra.desktop.core.storage.SessionData
import com.luc4n3x.levyra.desktop.core.storage.SessionStore
import com.luc4n3x.levyra.desktop.core.storage.SettingsStore
import com.luc4n3x.levyra.desktop.core.stream.ResolvedAudio
import com.luc4n3x.levyra.desktop.core.stream.StreamResolver
import com.luc4n3x.levyra.desktop.player.AudioOutputDevice
import com.luc4n3x.levyra.desktop.player.AudioPlayer
import com.luc4n3x.levyra.desktop.player.AudioPlayerUnavailableException
import com.luc4n3x.levyra.desktop.player.CrossfadePlanner
import com.luc4n3x.levyra.desktop.player.PlaybackStatus
import com.luc4n3x.levyra.desktop.player.PlayerEvent
import com.luc4n3x.levyra.desktop.player.PlayerQueue
import com.luc4n3x.levyra.desktop.player.PrefetchPlanner
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
    val speed: Float = DesktopSettings.DEFAULT_SPEED,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val sleepRemainingMs: Long = 0L,
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
            speed = DesktopSettings.normalizeSpeed(settingsStore.current.playbackSpeed),
            queue = PlayerQueue(repeat = RepeatMode.OFF)
        )
    )
    private val messageFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)
    private val outputDevicesState = MutableStateFlow<List<AudioOutputDevice>>(emptyList())
    private val outputDeviceMissingState = MutableStateFlow(false)

    private val playerScope = CoroutineScope(scope.coroutineContext + SupervisorJob())
    private val transitionLock = Any()

    private var player: AudioPlayer? = null
    private var companionPlayer: AudioPlayer? = null
    private var transitionJob: Job? = null
    private var prepareJob: Job? = null
    private var preparedTrackId: String = ""
    private var preparedTransitionMs: Long = 0L
    private var preparedStreamLabel: String = ""
    private var handoffAttemptedTrackId: String = ""
    private var transitionActive: Boolean = false
    private var playbackJob: Job? = null
    private var eventJob: Job? = null
    private var persistJob: Job? = null
    private var prefetchJob: Job? = null
    private var sleepJob: Job? = null
    private var outputDeviceJob: Job? = null
    private var prefetchedTrackId: String = ""
    private var retriedTrackId: String = ""
    private var consecutiveFailures: Int = 0
    private var pendingResumeMs: Long = 0L
    private var lastAudibleVolume: Int = settingsStore.current.volume.takeIf { it > 0 } ?: DEFAULT_VOLUME

    val state: StateFlow<PlaybackUiState> = internalState.asStateFlow()
    val messages: SharedFlow<String> = messageFlow.asSharedFlow()
    val audioOutputDevices: StateFlow<List<AudioOutputDevice>> = outputDevicesState.asStateFlow()
    val audioOutputDeviceMissing: StateFlow<Boolean> = outputDeviceMissingState.asStateFlow()

    init {
        playerScope.launch {
            settingsStore.settings
                .map { it.equalizer }
                .distinctUntilChanged()
                .collect { equalizer ->
                    player?.applyEqualizer(equalizer.enabled, equalizer.preamp, equalizer.amps)
                    companionPlayer?.applyEqualizer(equalizer.enabled, equalizer.preamp, equalizer.amps)
                }
        }
        playerScope.launch {
            settingsStore.settings
                .map { DesktopSettings.normalizeSpeed(it.playbackSpeed) }
                .distinctUntilChanged()
                .collect { speed -> applySpeed(speed) }
        }
        playerScope.launch {
            settingsStore.settings
                .map { it.audioOutputDeviceId }
                .distinctUntilChanged()
                .collect { deviceId ->
                    outputDeviceMissingState.value = false
                    player?.applyOutputDevice(deviceId)
                    companionPlayer?.applyOutputDevice(deviceId)
                }
        }
    }

    fun refreshAudioOutputDevices(createEngine: Boolean = false) {
        playerScope.launch {
            val active = if (createEngine) ensurePlayer() else player
            if (active == null) return@launch
            val devices = runCatching { active.outputDevices() }.getOrDefault(emptyList())
            if (devices.isNotEmpty()) {
                outputDevicesState.value = devices
                outputDeviceMissingState.value = isSelectedOutputDeviceMissing(devices)
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
        cancelTransition()
        val queue = internalState.value.queue
        val wasEmpty = queue.isEmpty
        internalState.update { state -> state.copy(queue = queue.enqueueNext(tracks)) }
        if (wasEmpty) startCurrent(0L) else persistSession()
    }

    fun enqueueLast(tracks: List<Track>) {
        cancelTransition()
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
        cancelTransition()
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
        cancelTransition()
        internalState.update { state -> state.copy(queue = state.queue.clear()) }
        stop()
    }

    fun togglePlayPause() {
        val current = internalState.value
        if (current.queue.isEmpty) return
        when (current.status) {
            PlaybackStatus.PLAYING, PlaybackStatus.BUFFERING -> {
                cancelTransition()
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
        cancelTransition()
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
        if (!transitionActive) {
            player?.setVolume(safe)
        }
        player?.setMuted(muted)
        companionPlayer?.setMuted(muted)
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
        companionPlayer?.setMuted(muted)
        internalState.update { state -> state.copy(muted = muted) }
    }

    fun setSpeed(speed: Float) {
        val safe = DesktopSettings.normalizeSpeed(speed)
        if (safe == internalState.value.speed) return
        settingsStore.update { it.copy(playbackSpeed = safe) }
    }

    fun startSleepTimer(minutes: Int) {
        val timer = SleepTimerState.forMinutes(minutes, System.currentTimeMillis())
        internalState.update { state ->
            state.copy(sleepTimer = timer, sleepRemainingMs = timer.remainingMs(System.currentTimeMillis()))
        }
        restartSleepLoop()
    }

    fun sleepAtEndOfTrack() {
        cancelTransition()
        sleepJob?.cancel()
        internalState.update { state ->
            state.copy(sleepTimer = SleepTimerState.endOfTrack(), sleepRemainingMs = 0L)
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        internalState.update { state ->
            state.copy(sleepTimer = SleepTimerState(), sleepRemainingMs = 0L)
        }
    }

    fun toggleShuffle() {
        cancelTransition()
        val queue = internalState.value.queue
        internalState.update { state -> state.copy(queue = queue.withShuffle(!queue.shuffle)) }
        persistSession()
    }

    fun cycleRepeat() {
        cancelTransition()
        val queue = internalState.value.queue
        internalState.update { state -> state.copy(queue = queue.withRepeat(queue.repeat.next())) }
        persistSession()
    }

    fun stop() {
        cancelTransition()
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
        transitionJob?.cancel()
        prepareJob?.cancel()
        persistJob?.cancel()
        playbackJob?.cancel()
        prefetchJob?.cancel()
        sleepJob?.cancel()
        outputDeviceJob?.cancel()
        eventJob?.cancel()
        saveSessionNow()
        runCatching { companionPlayer?.stop() }
        runCatching { companionPlayer?.close() }
        companionPlayer = null
        runCatching { player?.stop() }
        runCatching { player?.close() }
        player = null
        playerScope.cancel()
    }

    private fun startCurrent(startAtMs: Long, forceRestart: Boolean = false) {
        val track = internalState.value.queue.current ?: return
        cancelTransition()
        playbackJob?.cancel()
        if (track.id != prefetchedTrackId) {
            prefetchJob?.cancel()
        }
        prefetchedTrackId = ""
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
        if (track.offlinePath.isNotBlank() || track.videoUrl.isNotBlank()) return track
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
        applySpeed(settingsStore.current.playbackSpeed, activePlayer)
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

    private fun applySpeed(speed: Float, target: AudioPlayer? = player) {
        val safe = DesktopSettings.normalizeSpeed(speed)
        if (target == null || target.setSpeed(safe)) {
            internalState.update { state -> state.copy(speed = safe) }
        }
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

    private fun maybePrepareHandoff() {
        if (transitionActive) return
        if (!settingsStore.current.preloadNextTrack) return
        val current = internalState.value
        if (current.status != PlaybackStatus.PLAYING) return
        if (current.sleepTimer.mode == SleepTimerMode.END_OF_TRACK) return
        val playing = current.queue.current ?: return
        val next = PrefetchPlanner.handoffTrack(current.queue) ?: return
        if (next.id == preparedTrackId) {
            maybeStartCrossfade(current.positionMs, current.durationMs)
            return
        }
        if (next.id == handoffAttemptedTrackId) return
        if (prepareJob?.isActive == true) return
        val transitionMs = CrossfadePlanner.transitionDurationMs(
            requestedMs = settingsStore.current.crossfadeMs,
            smartCrossfade = settingsStore.current.smartCrossfade,
            current = playing,
            next = next,
            currentDurationMs = current.durationMs
        )
        if (current.positionMs < CrossfadePlanner.prepareThresholdMs(current.durationMs, transitionMs)) {
            return
        }
        handoffAttemptedTrackId = next.id
        prepareJob = playerScope.launch { prepareCompanion(next, transitionMs) }
    }

    private suspend fun prepareCompanion(next: Track, transitionMs: Long) {
        val companion = ensureCompanion() ?: return
        val settings = settingsStore.current
        val playable = resolveHandoffTrack(next) ?: return
        val resolved = try {
            resolver.resolve(playable, settings.audioQuality, settings.preferredCodec)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            DesktopDiagnostics.background("handoff resolve of ${next.title}", error)
            return
        }
        if (!companion.prepare(resolved.url, 0L)) return
        companion.setVolume(0)
        companion.setMuted(internalState.value.muted)
        val equalizer = settings.equalizer
        companion.applyEqualizer(equalizer.enabled, equalizer.preamp, equalizer.amps)
        companion.applyOutputDevice(effectiveOutputDeviceId(settings.audioOutputDeviceId))
        companion.setSpeed(DesktopSettings.normalizeSpeed(settings.playbackSpeed))
        val enriched = playable.copy(
            title = resolved.title.ifBlank { playable.title },
            artist = resolved.artist.ifBlank { playable.artist },
            artworkUrl = resolved.artworkUrl.ifBlank { playable.artworkUrl },
            durationMs = if (resolved.durationMs > 0L) resolved.durationMs else playable.durationMs
        )
        val published = synchronized(transitionLock) {
            if (
                transitionActive ||
                PrefetchPlanner.handoffTrack(internalState.value.queue)?.id != next.id
            ) {
                false
            } else {
                preparedTrackId = next.id
                preparedTransitionMs = transitionMs
                preparedStreamLabel = resolved.label
                true
            }
        }
        if (!published) {
            runCatching { companion.stop() }
            return
        }
        updateTrackMetadata(enriched)
    }

    private suspend fun resolveHandoffTrack(next: Track): Track? {
        if (next.videoUrl.isNotBlank() || next.offlinePath.isNotBlank()) return next
        return try {
            catalog.findPlayable(next)?.let { located ->
                next.copy(
                    videoUrl = located.videoUrl,
                    durationMs = if (located.durationMs > 0L) located.durationMs else next.durationMs
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            DesktopDiagnostics.background("handoff lookup of ${next.title}", error)
            null
        }
    }

    private fun ensureCompanion(): AudioPlayer? {
        companionPlayer?.let { return it }
        val created = runCatching { player?.createCompanion() }.getOrNull() ?: return null
        companionPlayer = created
        return created
    }

    private fun maybeStartCrossfade(positionMs: Long, durationMs: Long) {
        val transitionMs = preparedTransitionMs
        if (transitionMs <= 0L || durationMs <= 0L) return
        if (positionMs < durationMs - transitionMs) return
        startTransition(transitionMs)
    }

    private fun startPreparedHandoff(): Boolean = startTransition(0L)

    private fun startTransition(transitionMs: Long): Boolean {
        val started = synchronized(transitionLock) {
            if (transitionActive || preparedTrackId.isEmpty() || companionPlayer == null) {
                false
            } else {
                transitionActive = true
                true
            }
        }
        if (!started) return false
        transitionJob = playerScope.launch { runTransition(transitionMs) }
        return true
    }

    private suspend fun runTransition(transitionMs: Long) {
        val companion = companionPlayer
        val outgoing = player
        if (companion == null) {
            synchronized(transitionLock) { transitionActive = false }
            return
        }
        companion.setMuted(internalState.value.muted)
        companion.setVolume(if (transitionMs > 0L) 0 else internalState.value.volume)
        if (!companion.startPrepared()) {
            abortTransition()
            if (transitionMs <= 0L) {
                next(automatic = true)
            }
            return
        }
        if (transitionMs > 0L) {
            val steps = (transitionMs / CrossfadePlanner.STEP_MS).toInt().coerceAtLeast(1)
            for (step in 1..steps) {
                val fraction = step.toFloat() / steps.toFloat()
                val base = internalState.value.volume
                outgoing?.setVolume(
                    CrossfadePlanner.volumeFor(base, CrossfadePlanner.outgoingGain(fraction))
                )
                companion.setVolume(
                    CrossfadePlanner.volumeFor(base, CrossfadePlanner.incomingGain(fraction))
                )
                delay(CrossfadePlanner.STEP_MS)
            }
        }
        completeTransition()
    }

    private fun abortTransition() {
        val companion: AudioPlayer?
        synchronized(transitionLock) {
            transitionActive = false
            preparedTrackId = ""
            preparedTransitionMs = 0L
            preparedStreamLabel = ""
            handoffAttemptedTrackId = ""
            companion = companionPlayer
        }
        runCatching { companion?.stop() }
        player?.setVolume(internalState.value.volume)
    }

    private fun completeTransition() {
        val incoming: AudioPlayer
        val outgoing: AudioPlayer?
        val advanced: PlayerQueue
        val streamLabel: String
        synchronized(transitionLock) {
            if (!transitionActive) return
            val candidateCompanion = companionPlayer
            if (candidateCompanion == null) {
                transitionActive = false
                return
            }
            val expected = preparedTrackId
            val candidate = internalState.value.queue.advance(automatic = true)
            if (candidate == null || candidate.current?.id != expected) {
                transitionActive = false
                preparedTrackId = ""
                preparedTransitionMs = 0L
                preparedStreamLabel = ""
                handoffAttemptedTrackId = ""
                runCatching { candidateCompanion.stop() }
                player?.setVolume(internalState.value.volume)
                return
            }
            incoming = candidateCompanion
            advanced = candidate
            outgoing = player
            streamLabel = preparedStreamLabel
            player = incoming
            companionPlayer = outgoing
            preparedTrackId = ""
            preparedTransitionMs = 0L
            preparedStreamLabel = ""
            handoffAttemptedTrackId = ""
            transitionActive = false
        }
        runCatching { outgoing?.stop() }
        outgoing?.setVolume(internalState.value.volume)
        observeEvents(incoming)
        incoming.setVolume(internalState.value.volume)
        incoming.setMuted(internalState.value.muted)
        applySpeed(settingsStore.current.playbackSpeed, incoming)
        retriedTrackId = ""
        consecutiveFailures = 0
        pendingResumeMs = incoming.positionMs()
        val duration = incoming.durationMs()
        internalState.update { state ->
            state.copy(
                queue = advanced,
                status = PlaybackStatus.PLAYING,
                preparingTrackId = "",
                positionMs = pendingResumeMs,
                durationMs = if (duration > 0L) duration else advanced.current?.durationMs ?: 0L,
                streamLabel = streamLabel
            )
        }
        advanced.current?.let(libraryStore::recordPlayback)
        persistSession()
    }

    private fun cancelTransition() {
        val companion: AudioPlayer?
        val wasActive: Boolean
        synchronized(transitionLock) {
            wasActive = transitionActive
            transitionActive = false
            preparedTrackId = ""
            preparedTransitionMs = 0L
            preparedStreamLabel = ""
            handoffAttemptedTrackId = ""
            companion = companionPlayer
        }
        transitionJob?.cancel()
        prepareJob?.cancel()
        runCatching { companion?.stop() }
        if (wasActive) {
            player?.setVolume(internalState.value.volume)
        }
    }

    private fun maybePrefetchNext() {
        if (!settingsStore.current.preloadNextTrack) return
        val current = internalState.value
        if (current.status != PlaybackStatus.PLAYING) return
        if (!PrefetchPlanner.withinLeadWindow(current.positionMs, current.durationMs)) return
        val next = PrefetchPlanner.nextTrack(current.queue) ?: return
        if (next.id == prefetchedTrackId) return
        if (prefetchJob?.isActive == true) return
        prefetchedTrackId = next.id
        prefetchJob = playerScope.launch { prewarm(next) }
    }

    private suspend fun prewarm(track: Track) {
        val settings = settingsStore.current
        try {
            val playable = if (track.videoUrl.isNotBlank()) {
                track
            } else {
                catalog.findPlayable(track)?.let { located ->
                    track.copy(
                        videoUrl = located.videoUrl,
                        durationMs = if (located.durationMs > 0L) located.durationMs else track.durationMs
                    )
                } ?: return
            }
            resolver.resolve(playable, settings.audioQuality, settings.preferredCodec)
            if (playable.videoUrl != track.videoUrl) {
                updateTrackMetadata(playable)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            DesktopDiagnostics.background("prefetch of ${track.title}", error)
        }
    }

    private fun restartSleepLoop() {
        sleepJob?.cancel()
        sleepJob = playerScope.launch {
            while (isActive) {
                val timer = internalState.value.sleepTimer
                if (timer.mode != SleepTimerMode.DURATION) return@launch
                val now = System.currentTimeMillis()
                if (timer.expired(now)) {
                    pauseForSleepTimer()
                    return@launch
                }
                internalState.update { state -> state.copy(sleepRemainingMs = timer.remainingMs(now)) }
                delay(SLEEP_TICK_MS)
            }
        }
    }

    private fun pauseForSleepTimer() {
        cancelTransition()
        val settled = internalState.value.status in SETTLED_STATUSES
        if (settled) {
            player?.pause()
        } else {
            playbackJob?.cancel()
            prefetchJob?.cancel()
            runCatching { player?.stop() }
        }
        internalState.update { state ->
            state.copy(
                status = if (settled) PlaybackStatus.PAUSED else PlaybackStatus.IDLE,
                preparingTrackId = if (settled) state.preparingTrackId else "",
                sleepTimer = SleepTimerState(),
                sleepRemainingMs = 0L
            )
        }
        saveSessionNow()
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
            startOutputDeviceWatch()
            val equalizer = settingsStore.current.equalizer
            created.applyEqualizer(equalizer.enabled, equalizer.preamp, equalizer.amps)
            created.applyOutputDevice(settingsStore.current.audioOutputDeviceId)
            applySpeed(settingsStore.current.playbackSpeed, created)
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
                applySpeed(settingsStore.current.playbackSpeed)
                internalState.update { state -> state.copy(status = PlaybackStatus.PLAYING) }
            }

            is PlayerEvent.Paused ->
                internalState.update { state -> state.copy(status = PlaybackStatus.PAUSED) }

            is PlayerEvent.Stopped -> Unit

            is PlayerEvent.Finished -> when {
                transitionActive -> Unit

                internalState.value.sleepTimer.mode == SleepTimerMode.END_OF_TRACK -> {
                    cancelSleepTimer()
                    stop()
                }

                startPreparedHandoff() -> Unit

                else -> next(automatic = true)
            }

            is PlayerEvent.Failed -> handleFailure(event.reason)

            is PlayerEvent.TimeChanged -> {
                pendingResumeMs = event.positionMs
                internalState.update { state -> state.copy(positionMs = event.positionMs) }
                maybePrefetchNext()
                maybePrepareHandoff()
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

    private fun startOutputDeviceWatch() {
        if (outputDeviceJob?.isActive == true) return
        outputDeviceJob = playerScope.launch {
            while (isActive) {
                val active = player
                if (active != null) {
                    val devices = runCatching { active.outputDevices() }.getOrDefault(emptyList())
                    if (devices.isNotEmpty()) {
                        outputDevicesState.value = devices
                        val wasMissing = outputDeviceMissingState.value
                        val missing = isSelectedOutputDeviceMissing(devices)
                        when {
                            missing && !wasMissing -> {
                                active.applyOutputDevice(AudioOutputDevice.SYSTEM_DEFAULT_ID)
                                companionPlayer?.applyOutputDevice(AudioOutputDevice.SYSTEM_DEFAULT_ID)
                            }

                            !missing && wasMissing -> {
                                val selected = settingsStore.current.audioOutputDeviceId
                                active.applyOutputDevice(selected)
                                companionPlayer?.applyOutputDevice(selected)
                            }
                        }
                        outputDeviceMissingState.value = missing
                    }
                }
                delay(OUTPUT_DEVICE_POLL_MS)
            }
        }
    }

    private fun effectiveOutputDeviceId(selected: String): String =
        if (outputDeviceMissingState.value) AudioOutputDevice.SYSTEM_DEFAULT_ID else selected

    private fun isSelectedOutputDeviceMissing(devices: List<AudioOutputDevice>): Boolean {
        val selected = settingsStore.current.audioOutputDeviceId
        if (selected.isEmpty()) return false
        return devices.none { it.id == selected }
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
        const val OUTPUT_DEVICE_POLL_MS = 8_000L
        const val SLEEP_TICK_MS = 1_000L
        val SETTLED_STATUSES = setOf(PlaybackStatus.PLAYING, PlaybackStatus.PAUSED)
        const val MAX_QUEUE_SIZE = 200
        const val MAX_CAUSE_DEPTH = 8
        const val DEFAULT_VOLUME = 60
    }
}
