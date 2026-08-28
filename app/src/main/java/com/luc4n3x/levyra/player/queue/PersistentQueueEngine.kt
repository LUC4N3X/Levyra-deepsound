package com.luc4n3x.levyra.player.queue

import android.content.Context
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

internal fun queuePersistenceAllowed(transientPlaybackActive: Boolean): Boolean =
    !transientPlaybackActive

internal fun queueRemovalCurrentIndex(removedIndex: Int, currentIndex: Int, newLastIndex: Int): Int = when {
    newLastIndex < 0 -> -1
    removedIndex < currentIndex -> currentIndex - 1
    removedIndex == currentIndex -> removedIndex.coerceAtMost(newLastIndex)
    else -> currentIndex.coerceAtMost(newLastIndex)
}

internal fun queueUndoInsertionIndex(originalIndex: Int, size: Int): Int = originalIndex.coerceIn(0, size)

internal fun queueUndoCurrentIndex(insertionIndex: Int, currentIndex: Int): Int = when {
    currentIndex < 0 -> insertionIndex
    insertionIndex <= currentIndex -> currentIndex + 1
    else -> currentIndex
}

internal fun replacementQueuePositionMs(
    previousIdentity: String?,
    nextIdentity: String?,
    currentPositionMs: Long,
    requestedPositionMs: Long
): Long {
    val requested = requestedPositionMs.coerceAtLeast(0L)
    if (requested > 0L) return requested
    return if (previousIdentity != null && previousIdentity == nextIdentity) {
        currentPositionMs.coerceAtLeast(0L)
    } else {
        0L
    }
}

class PersistentQueueEngine private constructor(context: Context) {
    private val store = PlaybackQueueStore(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val _state = MutableStateFlow(PlaybackQueueSnapshot())
    private var persistJob: Job? = null
    private var positionPersistJob: Job? = null
    private var undoRemoval: QueueRemoval? = null
    @Volatile private var transientPlaybackActive: Boolean = false

    val state: StateFlow<PlaybackQueueSnapshot> = _state.asStateFlow()

    suspend fun restore(
        fallbackTracks: List<Track>,
        fallbackIndex: Int,
        fallbackPositionMs: Long,
        fallbackRepeatMode: RepeatMode = RepeatMode.Off,
        fallbackShuffleEnabled: Boolean = false,
        fallbackRadioEnabled: Boolean = false
    ): PlaybackQueueSnapshot {
        transientPlaybackActive = false
        val restored = runCatching { store.load() }
            .onFailure { Timber.w(it, "Persistent queue restore failed") }
            .getOrNull()
        val snapshot = if (restored != null && restored.tracks.isNotEmpty()) {
            restored.toRuntimeSnapshot()
        } else {
            buildSnapshot(
                tracks = fallbackTracks,
                currentIndex = fallbackIndex,
                positionMs = fallbackPositionMs,
                repeatMode = fallbackRepeatMode,
                shuffleEnabled = fallbackShuffleEnabled,
                radioEnabled = fallbackRadioEnabled,
                generation = 1L
            )
        }
        synchronized(lock) {
            _state.value = snapshot
            undoRemoval = null
        }
        schedulePersist(immediate = true)
        return snapshot
    }

    fun clear(): PlaybackQueueSnapshot = mutate(structural = true, immediatePersist = true) { current ->
        undoRemoval = null
        PlaybackQueueSnapshot(
            tracks = emptyList(),
            currentIndex = -1,
            positionMs = 0L,
            shuffleEnabled = false,
            shuffleOrder = emptyList(),
            shuffleCursor = -1,
            history = emptyList(),
            repeatMode = RepeatMode.Off,
            radioEnabled = false,
            generation = current.generation + 1L,
            updatedAt = System.currentTimeMillis(),
            undoAvailable = false
        )
    }

    fun replace(
        tracks: List<Track>,
        currentIndex: Int,
        positionMs: Long = 0L,
        keepPlaybackModes: Boolean = true,
        radioEnabled: Boolean? = null
    ): PlaybackQueueSnapshot = mutate(structural = true, immediatePersist = true) { current ->
        val normalized = tracks.filter { it.title.isNotBlank() }.distinctBy(::playbackQueueIdentity)
        val safeIndex = if (normalized.isEmpty()) -1 else currentIndex.coerceIn(0, normalized.lastIndex)
        val previousIdentity = current.currentTrack?.let(::playbackQueueIdentity)
        val nextIdentity = normalized.getOrNull(safeIndex)?.let(::playbackQueueIdentity)
        val nextPositionMs = replacementQueuePositionMs(
            previousIdentity = previousIdentity,
            nextIdentity = nextIdentity,
            currentPositionMs = current.positionMs,
            requestedPositionMs = positionMs
        )
        buildSnapshot(
            tracks = normalized,
            currentIndex = safeIndex,
            positionMs = nextPositionMs,
            repeatMode = if (keepPlaybackModes) current.repeatMode else RepeatMode.Off,
            shuffleEnabled = keepPlaybackModes && current.shuffleEnabled,
            radioEnabled = radioEnabled ?: if (keepPlaybackModes) current.radioEnabled else false,
            generation = current.generation + 1L,
            history = emptyList()
        )
    }

    fun beginTransientPlayback(
        preservedSnapshot: PlaybackQueueSnapshot,
        tracks: List<Track>,
        currentIndex: Int,
        positionMs: Long = 0L
    ): PlaybackQueueSnapshot {
        persistJob?.cancel()
        positionPersistJob?.cancel()
        val durableSnapshot = preservedSnapshot.toPersistent()
        transientPlaybackActive = true
        scope.launch {
            runCatching { store.save(durableSnapshot) }
                .onFailure { Timber.w(it, "Unable to preserve durable queue before transient playback") }
        }
        return replaceTransient(tracks, currentIndex, positionMs)
    }

    fun replaceTransient(
        tracks: List<Track>,
        currentIndex: Int,
        positionMs: Long = 0L
    ): PlaybackQueueSnapshot {
        check(transientPlaybackActive) { "Transient playback must be started before replacement" }
        return mutate(structural = true, immediatePersist = false) { current ->
            val normalized = tracks.filter { it.title.isNotBlank() }.distinctBy(::playbackQueueIdentity)
            val safeIndex = if (normalized.isEmpty()) -1 else currentIndex.coerceIn(0, normalized.lastIndex)
            buildSnapshot(
                tracks = normalized,
                currentIndex = safeIndex,
                positionMs = positionMs,
                repeatMode = RepeatMode.Off,
                shuffleEnabled = false,
                radioEnabled = false,
                generation = current.generation + 1L
            )
        }
    }

    fun endTransientPlayback(snapshot: PlaybackQueueSnapshot): PlaybackQueueSnapshot {
        transientPlaybackActive = false
        return restoreSnapshot(snapshot)
    }

    fun restoreSnapshot(snapshot: PlaybackQueueSnapshot): PlaybackQueueSnapshot =
        mutate(structural = true, immediatePersist = true) { current ->
            undoRemoval = null
            val tracks = snapshot.tracks.map { it.queueStoredCopy() }
            val currentIndex = if (tracks.isEmpty()) -1 else snapshot.currentIndex.coerceIn(0, tracks.lastIndex)
            val validShuffleOrder = snapshot.shuffleOrder
                .filter { it in tracks.indices }
                .distinct()
            val shuffleOrder = when {
                !snapshot.shuffleEnabled -> emptyList()
                validShuffleOrder.size == tracks.size -> validShuffleOrder
                else -> stableShuffleOrder(tracks, currentIndex, current.generation + 1L)
            }
            PlaybackQueueSnapshot(
                tracks = tracks,
                currentIndex = currentIndex,
                positionMs = snapshot.positionMs.coerceAtLeast(0L),
                shuffleEnabled = snapshot.shuffleEnabled,
                shuffleOrder = shuffleOrder,
                shuffleCursor = if (snapshot.shuffleEnabled) {
                    shuffleOrder.indexOf(currentIndex).coerceAtLeast(0)
                } else {
                    -1
                },
                history = snapshot.history.filter { it in tracks.indices }.takeLast(200),
                repeatMode = snapshot.repeatMode,
                radioEnabled = snapshot.radioEnabled,
                generation = current.generation + 1L,
                updatedAt = System.currentTimeMillis(),
                undoAvailable = false
            )
        }

    fun select(index: Int, positionMs: Long = 0L, rememberCurrent: Boolean = true): Track? {
        var selected: Track? = null
        mutate(immediatePersist = true) { current ->
            if (index !in current.tracks.indices) return@mutate current
            selected = current.tracks[index]
            selectSnapshot(current, index, positionMs, rememberCurrent)
        }
        return selected
    }

    fun addLast(track: Track): PlaybackQueueSnapshot = mutate(structural = true, immediatePersist = true) { current ->
        val identity = playbackQueueIdentity(track)
        if (current.tracks.any { playbackQueueIdentity(it) == identity }) return@mutate current
        val nextTracks = current.tracks + track.queueStoredCopy()
        rebuildAfterStructureChange(current, nextTracks, current.currentIndex)
    }

    fun playNext(track: Track): PlaybackQueueSnapshot = mutate(structural = true, immediatePersist = true) { current ->
        val identity = playbackQueueIdentity(track)
        val withoutDuplicate = current.tracks.filterNot { playbackQueueIdentity(it) == identity }
        val currentTrackIdentity = current.currentTrack?.let(::playbackQueueIdentity)
        val baseIndex = currentTrackIdentity?.let { key -> withoutDuplicate.indexOfFirst { playbackQueueIdentity(it) == key } }
            ?.takeIf { it >= 0 }
            ?: current.currentIndex.coerceIn(-1, withoutDuplicate.lastIndex)
        val insertionIndex = (baseIndex + 1).coerceIn(0, withoutDuplicate.size)
        val nextTracks = withoutDuplicate.toMutableList().apply {
            add(insertionIndex, track.queueStoredCopy())
        }
        rebuildAfterStructureChange(current, nextTracks, baseIndex)
    }

    fun remove(index: Int): PlaybackQueueSnapshot = mutate(structural = true, immediatePersist = true) { current ->
        if (index !in current.tracks.indices) return@mutate current
        val removed = current.tracks[index]
        undoRemoval = QueueRemoval(removed, index)
        val nextTracks = current.tracks.toMutableList().apply { removeAt(index) }
        val nextCurrentIndex = queueRemovalCurrentIndex(index, current.currentIndex, nextTracks.lastIndex)
        rebuildAfterStructureChange(current, nextTracks, nextCurrentIndex).copy(undoAvailable = true)
    }

    fun undoRemove(): PlaybackQueueSnapshot = mutate(structural = true, immediatePersist = true) { current ->
        val removal = undoRemoval ?: return@mutate current
        val identity = playbackQueueIdentity(removal.track)
        if (current.tracks.any { playbackQueueIdentity(it) == identity }) {
            undoRemoval = null
            return@mutate current.copy(undoAvailable = false)
        }
        val insertionIndex = queueUndoInsertionIndex(removal.index, current.tracks.size)
        val nextTracks = current.tracks.toMutableList().apply { add(insertionIndex, removal.track) }
        val nextCurrentIndex = queueUndoCurrentIndex(insertionIndex, current.currentIndex)
        undoRemoval = null
        rebuildAfterStructureChange(current, nextTracks, nextCurrentIndex).copy(undoAvailable = false)
    }

    fun move(from: Int, to: Int): PlaybackQueueSnapshot = mutate(structural = true, immediatePersist = true) { current ->
        if (from !in current.tracks.indices || to !in current.tracks.indices || from == to) return@mutate current
        val currentIdentity = current.currentTrack?.let(::playbackQueueIdentity)
        val nextTracks = current.tracks.toMutableList()
        val moved = nextTracks.removeAt(from)
        nextTracks.add(to, moved)
        val nextCurrentIndex = currentIdentity?.let { key -> nextTracks.indexOfFirst { playbackQueueIdentity(it) == key } } ?: -1
        rebuildAfterStructureChange(current, nextTracks, nextCurrentIndex)
    }

    fun updateTrackAt(index: Int, track: Track): PlaybackQueueSnapshot = mutate(structural = true, immediatePersist = true) { current ->
        if (index !in current.tracks.indices) return@mutate current
        val nextTracks = current.tracks.toMutableList().apply { set(index, track.queueStoredCopy()) }
        current.copy(tracks = nextTracks, generation = current.generation + 1L)
    }

    fun updateTrackMetadata(track: Track): PlaybackQueueSnapshot = mutate(immediatePersist = true) { current ->
        val identity = playbackQueueIdentity(track)
        var changed = false
        val updatedTracks = current.tracks.map { existing ->
            if (playbackQueueIdentity(existing) != identity) {
                existing
            } else {
                changed = true
                existing.copy(
                    title = track.title.ifBlank { existing.title },
                    artist = track.artist.ifBlank { existing.artist },
                    album = track.album.ifBlank { existing.album },
                    durationMs = track.durationMs.takeIf { it > 0L } ?: existing.durationMs,
                    videoUrl = track.videoUrl.ifBlank { existing.videoUrl },
                    thumbnailUrl = track.thumbnailUrl.ifBlank { existing.thumbnailUrl },
                    largeThumbnailUrl = track.largeThumbnailUrl.ifBlank { existing.largeThumbnailUrl },
                    source = track.source.ifBlank { existing.source },
                    streamUrl = track.queueStoredCopy().streamUrl,
                    videoStreamUrl = ""
                )
            }
        }
        if (!changed) current else current.copy(tracks = updatedTracks)
    }

    fun setShuffle(enabled: Boolean): PlaybackQueueSnapshot = mutate(immediatePersist = true) { current ->
        if (current.shuffleEnabled == enabled) return@mutate current
        if (!enabled) {
            current.copy(shuffleEnabled = false, shuffleOrder = emptyList(), shuffleCursor = -1, generation = current.generation + 1L)
        } else {
            val order = stableShuffleOrder(current.tracks, current.currentIndex, current.generation + 1L)
            current.copy(
                shuffleEnabled = true,
                shuffleOrder = order,
                shuffleCursor = order.indexOf(current.currentIndex).coerceAtLeast(0),
                generation = current.generation + 1L
            )
        }
    }

    fun setRepeatMode(mode: RepeatMode): PlaybackQueueSnapshot = mutate(immediatePersist = true) { current ->
        current.copy(repeatMode = mode)
    }

    fun setRadioEnabled(enabled: Boolean): PlaybackQueueSnapshot = mutate(immediatePersist = true) { current ->
        current.copy(radioEnabled = enabled)
    }

    fun next(respectRepeatOne: Boolean = true): Track? = advanceNext(respectRepeatOne, expectedIdentity = null)

    fun nextMatching(expectedIdentity: String, respectRepeatOne: Boolean = true): Track? =
        advanceNext(respectRepeatOne, expectedIdentity)

    fun handoffNext(
        expectedGeneration: Long,
        expectedCurrentIdentity: String,
        expectedNextIdentity: String,
        resolved: Track
    ): Track? {
        var result: Track? = null
        mutate(structural = true, immediatePersist = true) { current ->
            if (current.generation != expectedGeneration) return@mutate current
            if (current.repeatMode == RepeatMode.One) return@mutate current
            if (current.currentTrack?.let(::playbackQueueIdentity) != expectedCurrentIdentity) return@mutate current

            val target = nextTargetIndex(current) ?: return@mutate current
            val selected = current.tracks[target]
            val replacement = resolvedQueueTrackForHandoff(selected, expectedNextIdentity, resolved)
                ?: return@mutate current

            val advanced = selectSnapshot(current, target, 0L, rememberCurrent = true)
            val updatedTracks = advanced.tracks.toMutableList().apply {
                set(target, replacement.queueStoredCopy())
            }
            result = resolved
            advanced.copy(tracks = updatedTracks, generation = advanced.generation + 1L)
        }
        return result
    }

    private fun advanceNext(respectRepeatOne: Boolean, expectedIdentity: String?): Track? {
        var result: Track? = null
        mutate(immediatePersist = true) { current ->
            if (current.tracks.isEmpty() || current.currentIndex !in current.tracks.indices) return@mutate current
            if (respectRepeatOne && current.repeatMode == RepeatMode.One) {
                val repeated = current.currentTrack
                if (expectedIdentity != null && repeated?.let(::playbackQueueIdentity) != expectedIdentity) return@mutate current
                result = repeated
                return@mutate current.copy(positionMs = 0L)
            }
            val target = nextTargetIndex(current) ?: return@mutate current
            val selected = current.tracks[target]
            if (expectedIdentity != null && playbackQueueIdentity(selected) != expectedIdentity) return@mutate current
            result = selected
            selectSnapshot(current, target, 0L, rememberCurrent = true)
        }
        return result
    }

    private fun nextTargetIndex(current: PlaybackQueueSnapshot): Int? {
        if (current.tracks.isEmpty() || current.currentIndex !in current.tracks.indices) return null
        return if (current.shuffleEnabled) {
            val order = normalizedShuffleOrder(current)
            val cursor = order.indexOf(current.currentIndex).takeIf { it >= 0 } ?: current.shuffleCursor.coerceAtLeast(0)
            val nextCursor = cursor + 1
            when {
                nextCursor < order.size -> order[nextCursor]
                current.repeatMode == RepeatMode.All -> order.firstOrNull()
                else -> null
            }
        } else {
            when {
                current.currentIndex < current.tracks.lastIndex -> current.currentIndex + 1
                current.repeatMode == RepeatMode.All -> 0
                else -> null
            }
        }
    }

    fun previous(): Track? {
        var result: Track? = null
        mutate(immediatePersist = true) { current ->
            if (current.tracks.isEmpty()) return@mutate current
            val history = current.history.toMutableList()
            var target: Int? = null
            while (history.isNotEmpty() && target == null) {
                val candidate = history.removeAt(history.lastIndex)
                if (candidate in current.tracks.indices && candidate != current.currentIndex) target = candidate
            }
            if (target == null && !current.shuffleEnabled) {
                target = when {
                    current.currentIndex > 0 -> current.currentIndex - 1
                    current.repeatMode == RepeatMode.All -> current.tracks.lastIndex
                    else -> current.currentIndex.takeIf { it in current.tracks.indices }
                }
            }
            val index = target ?: return@mutate current.copy(history = history)
            result = current.tracks[index]
            selectSnapshot(current.copy(history = history), index, 0L, rememberCurrent = false)
        }
        return result
    }

    fun updatePosition(positionMs: Long) {
        val safe = positionMs.coerceAtLeast(0L)
        val updated = synchronized(lock) {
            val current = _state.value
            if (kotlin.math.abs(current.positionMs - safe) < 1_000L) return
            current.copy(positionMs = safe, updatedAt = System.currentTimeMillis()).also { _state.value = it }
        }
        schedulePositionPersist(updated)
    }

    fun upcoming(limit: Int): List<Track> {
        val current = _state.value
        if (limit <= 0 || current.tracks.isEmpty() || current.currentIndex !in current.tracks.indices) return emptyList()
        val indices = if (current.shuffleEnabled) {
            val order = normalizedShuffleOrder(current)
            val cursor = order.indexOf(current.currentIndex).coerceAtLeast(0)
            buildList {
                for (offset in 1..limit) {
                    val candidateCursor = cursor + offset
                    val index = when {
                        candidateCursor < order.size -> order[candidateCursor]
                        current.repeatMode == RepeatMode.All && order.isNotEmpty() -> order[candidateCursor % order.size]
                        else -> -1
                    }
                    if (index >= 0 && index !in this) add(index)
                }
            }
        } else {
            buildList {
                for (offset in 1..limit) {
                    val raw = current.currentIndex + offset
                    val index = when {
                        raw < current.tracks.size -> raw
                        current.repeatMode == RepeatMode.All && current.tracks.isNotEmpty() -> raw % current.tracks.size
                        else -> -1
                    }
                    if (index >= 0 && index !in this) add(index)
                }
            }
        }
        return indices.mapNotNull(current.tracks::getOrNull)
    }

    fun appendRadioTracks(tracks: List<Track>): PlaybackQueueSnapshot = mutate(structural = true, immediatePersist = true) { current ->
        val existing = current.tracks.mapTo(LinkedHashSet(), ::playbackQueueIdentity)
        val existingTitles = current.tracks.mapTo(LinkedHashSet()) {
            "${it.artist.trim().lowercase()}|${it.title.trim().lowercase()}"
        }
        val available = (MAX_RADIO_QUEUE_SIZE - current.tracks.size).coerceAtLeast(0)
        val additions = tracks
            .filter { it.title.isNotBlank() }
            .filter { existing.add(playbackQueueIdentity(it)) }
            .filter { existingTitles.add("${it.artist.trim().lowercase()}|${it.title.trim().lowercase()}") }
            .map { it.queueStoredCopy() }
            .take(minOf(RADIO_BATCH_SIZE, available))
        if (additions.isEmpty()) return@mutate current
        rebuildAfterStructureChange(current, current.tracks + additions, current.currentIndex)
    }

    suspend fun flush() {
        if (!queuePersistenceAllowed(transientPlaybackActive)) return
        persistJob?.cancel()
        positionPersistJob?.cancel()
        val snapshot = _state.value.toPersistent()
        runCatching { store.save(snapshot) }
            .onFailure { Timber.w(it, "Persistent queue flush failed") }
    }

    fun flushBlocking() {
        runCatching { runBlocking(Dispatchers.IO) { flush() } }
            .onFailure { Timber.w(it, "Persistent queue blocking flush failed") }
    }

    private fun mutate(
        structural: Boolean = false,
        immediatePersist: Boolean = false,
        persistDelayMs: Long = 350L,
        transform: (PlaybackQueueSnapshot) -> PlaybackQueueSnapshot
    ): PlaybackQueueSnapshot {
        val updated = synchronized(lock) {
            val before = _state.value
            val transformed = transform(before)
            val normalized = transformed.copy(
                currentIndex = if (transformed.tracks.isEmpty()) -1 else transformed.currentIndex.coerceIn(0, transformed.tracks.lastIndex),
                positionMs = transformed.positionMs.coerceAtLeast(0L),
                history = transformed.history.filter { it in transformed.tracks.indices }.takeLast(200),
                updatedAt = System.currentTimeMillis(),
                generation = if (structural && transformed.generation <= before.generation) before.generation + 1L else transformed.generation.coerceAtLeast(1L),
                undoAvailable = undoRemoval != null
            )
            _state.value = normalized
            normalized
        }
        schedulePersist(immediatePersist, persistDelayMs)
        return updated
    }

    private fun schedulePersist(immediate: Boolean, delayMs: Long = 350L) {
        if (!queuePersistenceAllowed(transientPlaybackActive)) return
        positionPersistJob?.cancel()
        persistJob?.cancel()
        persistJob = scope.launch {
            if (!immediate) delay(delayMs.coerceAtLeast(100L))
            val snapshot = _state.value.toPersistent()
            runCatching { store.save(snapshot) }
                .onFailure { Timber.w(it, "Persistent queue save failed") }
        }
    }

    private fun schedulePositionPersist(snapshot: PlaybackQueueSnapshot) {
        if (!queuePersistenceAllowed(transientPlaybackActive)) return
        positionPersistJob?.cancel()
        positionPersistJob = scope.launch {
            delay(1_500L)
            val current = _state.value
            if (current.generation != snapshot.generation || current.currentIndex != snapshot.currentIndex) return@launch
            runCatching { store.updatePosition(current.positionMs, current.updatedAt) }
                .onFailure { Timber.w(it, "Persistent queue position save failed") }
        }
    }

    private fun buildSnapshot(
        tracks: List<Track>,
        currentIndex: Int,
        positionMs: Long,
        repeatMode: RepeatMode,
        shuffleEnabled: Boolean,
        radioEnabled: Boolean,
        generation: Long,
        history: List<Int> = emptyList()
    ): PlaybackQueueSnapshot {
        val safeIndex = if (tracks.isEmpty()) -1 else currentIndex.coerceIn(0, tracks.lastIndex)
        val order = if (shuffleEnabled) stableShuffleOrder(tracks, safeIndex, generation) else emptyList()
        return PlaybackQueueSnapshot(
            tracks = tracks.map { it.queueStoredCopy() },
            currentIndex = safeIndex,
            positionMs = positionMs.coerceAtLeast(0L),
            shuffleEnabled = shuffleEnabled,
            shuffleOrder = order,
            shuffleCursor = order.indexOf(safeIndex).coerceAtLeast(if (order.isEmpty()) -1 else 0),
            history = history,
            repeatMode = repeatMode,
            radioEnabled = radioEnabled,
            generation = generation.coerceAtLeast(1L),
            updatedAt = System.currentTimeMillis(),
            undoAvailable = undoRemoval != null
        )
    }

    private fun rebuildAfterStructureChange(
        current: PlaybackQueueSnapshot,
        tracks: List<Track>,
        currentIndex: Int
    ): PlaybackQueueSnapshot {
        val previousIdentity = current.currentTrack?.let(::playbackQueueIdentity)
        val nextIdentity = tracks.getOrNull(currentIndex)?.let(::playbackQueueIdentity)
        return buildSnapshot(
            tracks = tracks,
            currentIndex = currentIndex,
            positionMs = replacementQueuePositionMs(
                previousIdentity = previousIdentity,
                nextIdentity = nextIdentity,
                currentPositionMs = current.positionMs,
                requestedPositionMs = 0L
            ),
            repeatMode = current.repeatMode,
            shuffleEnabled = current.shuffleEnabled,
            radioEnabled = current.radioEnabled,
            generation = current.generation + 1L,
            history = remapHistory(current, tracks)
        )
    }

    private fun remapHistory(current: PlaybackQueueSnapshot, tracks: List<Track>): List<Int> {
        val identities = tracks.map(::playbackQueueIdentity)
        return current.history.mapNotNull { oldIndex ->
            current.tracks.getOrNull(oldIndex)?.let(::playbackQueueIdentity)?.let(identities::indexOf)?.takeIf { it >= 0 }
        }.takeLast(200)
    }

    private fun selectSnapshot(
        current: PlaybackQueueSnapshot,
        index: Int,
        positionMs: Long,
        rememberCurrent: Boolean
    ): PlaybackQueueSnapshot {
        val history = if (rememberCurrent && current.currentIndex in current.tracks.indices && current.currentIndex != index) {
            (current.history + current.currentIndex).takeLast(200)
        } else {
            current.history
        }
        val order = if (current.shuffleEnabled) normalizedShuffleOrder(current) else emptyList()
        return current.copy(
            currentIndex = index,
            positionMs = positionMs.coerceAtLeast(0L),
            history = history,
            shuffleOrder = order,
            shuffleCursor = if (current.shuffleEnabled) order.indexOf(index).coerceAtLeast(0) else -1,
            generation = current.generation + 1L
        )
    }

    private fun normalizedShuffleOrder(current: PlaybackQueueSnapshot): List<Int> {
        val valid = current.shuffleOrder.filter { it in current.tracks.indices }.distinct()
        return if (valid.size == current.tracks.size) valid else stableShuffleOrder(current.tracks, current.currentIndex, current.generation)
    }

    private fun PersistentQueueSnapshot.toRuntimeSnapshot(): PlaybackQueueSnapshot {
        val safeOrder = shuffleOrder.filter { it in tracks.indices }.distinct()
        val order = if (shuffleEnabled && safeOrder.size != tracks.size) stableShuffleOrder(tracks, currentIndex, generation) else safeOrder
        return PlaybackQueueSnapshot(
            tracks = tracks.map { it.queueStoredCopy() },
            currentIndex = currentIndex,
            positionMs = positionMs,
            shuffleEnabled = shuffleEnabled,
            shuffleOrder = order,
            shuffleCursor = if (shuffleEnabled) order.indexOf(currentIndex).coerceAtLeast(0) else -1,
            history = history,
            repeatMode = repeatMode,
            radioEnabled = radioEnabled,
            generation = generation,
            updatedAt = updatedAt,
            undoAvailable = false
        )
    }

    private fun PlaybackQueueSnapshot.toPersistent(): PersistentQueueSnapshot = PersistentQueueSnapshot(
        tracks = tracks,
        currentIndex = currentIndex,
        positionMs = positionMs,
        shuffleEnabled = shuffleEnabled,
        shuffleOrder = shuffleOrder,
        shuffleCursor = shuffleCursor,
        history = history,
        repeatMode = repeatMode,
        radioEnabled = radioEnabled,
        generation = generation,
        updatedAt = updatedAt
    )

    private data class QueueRemoval(val track: Track, val index: Int)

    companion object {
        private const val RADIO_BATCH_SIZE = 5
        private const val MAX_RADIO_QUEUE_SIZE = 100
        @Volatile
        private var instance: PersistentQueueEngine? = null

        fun get(context: Context): PersistentQueueEngine = instance ?: synchronized(this) {
            instance ?: PersistentQueueEngine(context.applicationContext).also { instance = it }
        }
    }
}

data class PlaybackQueueSnapshot(
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val positionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val shuffleOrder: List<Int> = emptyList(),
    val shuffleCursor: Int = -1,
    val history: List<Int> = emptyList(),
    val repeatMode: RepeatMode = RepeatMode.Off,
    val radioEnabled: Boolean = false,
    val generation: Long = 1L,
    val updatedAt: Long = 0L,
    val undoAvailable: Boolean = false
) {
    val currentTrack: Track?
        get() = tracks.getOrNull(currentIndex)
}

internal fun resolvedQueueTrackForHandoff(
    selected: Track,
    expectedNextIdentity: String,
    resolved: Track
): Track? {
    if (playbackQueueIdentity(selected) != expectedNextIdentity) return null
    return resolved.copy(
        id = selected.id,
        title = selected.title,
        artist = selected.artist,
        videoUrl = selected.videoUrl
    )
}

internal fun Track.queueStoredCopy(): Track {
    val localStream = streamUrl.takeIf { value ->
        value.startsWith("content://", ignoreCase = true) || value.startsWith("file://", ignoreCase = true)
    }.orEmpty()
    return copy(streamUrl = localStream, videoStreamUrl = "")
}

internal fun stableShuffleOrder(tracks: List<Track>, currentIndex: Int, generation: Long): List<Int> {
    if (tracks.isEmpty()) return emptyList()
    val current = currentIndex.coerceIn(0, tracks.lastIndex)
    val seed = tracks.fold(generation) { acc, track -> acc * 31L + playbackQueueIdentity(track).hashCode().toLong() }
    val rest = tracks.indices.filter { it != current }.shuffled(Random(seed))
    return listOf(current) + rest
}

internal fun playbackQueueIdentity(track: Track): String {
    val videoId = Regex("(?:v=|youtu\\.be/|shorts/|embed/)([A-Za-z0-9_-]{6,})")
        .find(track.videoUrl)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    return when {
        videoId.isNotBlank() -> "yt:$videoId"
        track.id.isNotBlank() -> "id:${track.id.trim().lowercase(Locale.ROOT)}"
        else -> "meta:${track.artist.trim().lowercase(Locale.ROOT)}|${track.title.trim().lowercase(Locale.ROOT)}"
    }
}
