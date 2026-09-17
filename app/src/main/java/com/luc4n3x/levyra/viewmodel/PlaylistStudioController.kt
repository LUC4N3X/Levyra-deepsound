package com.luc4n3x.levyra.viewmodel

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.PlaylistCoverStyle
import com.luc4n3x.levyra.domain.PlaylistStudioDraft
import com.luc4n3x.levyra.domain.PlaylistStudioEdits
import com.luc4n3x.levyra.domain.PlaylistStudioPhoto
import com.luc4n3x.levyra.domain.StudioSearchIndex
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.buildPlaylistCoverPlan
import com.luc4n3x.levyra.domain.mergeStudioCandidates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

interface PlaylistStudioRollbackToken

interface PlaylistStudioGateway {
    suspend fun create(name: String, tracks: List<Track>): String
    suspend fun captureRollback(playlistId: String): PlaylistStudioRollbackToken? = null
    suspend fun update(playlistId: String, name: String, tracks: List<Track>)
    suspend fun applyCover(playlistId: String, draft: PlaylistStudioDraft): String?
    suspend fun rollbackCreated(playlistId: String): Boolean = false
    suspend fun rollbackUpdated(playlistId: String, token: PlaylistStudioRollbackToken?): Boolean = false
    fun onSaved(playlistId: String)
}

class PlaylistStudioMissingException(playlistId: String) : IllegalStateException("Playlist $playlistId no longer exists")

enum class PlaylistStudioUndoKind { Removed, Moved }

@Immutable
data class PlaylistStudioUndo(
    val kind: PlaylistStudioUndoKind,
    val trackTitle: String,
    val tracks: List<Track>,
    val coverTrackId: String?
)

enum class PlaylistStudioSaveState { Clean, Dirty, Saving, Saved, Failed }

@Immutable
data class PlaylistStudioSession(
    val generation: Long,
    val draft: PlaylistStudioDraft,
    val baseline: PlaylistStudioDraft,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val justSaved: Boolean = false,
    val undo: List<PlaylistStudioUndo> = emptyList(),
    val candidates: StudioSearchIndex? = null
) {
    val dirty: Boolean get() = draft != baseline || draft.isNew

    val saveState: PlaylistStudioSaveState
        get() = when {
            saving -> PlaylistStudioSaveState.Saving
            failed -> PlaylistStudioSaveState.Failed
            draft != baseline || draft.isNew -> PlaylistStudioSaveState.Dirty
            justSaved -> PlaylistStudioSaveState.Saved
            else -> PlaylistStudioSaveState.Clean
        }

    val canSave: Boolean get() = !saving && draft.canSave && (dirty || failed)
    val lastUndo: PlaylistStudioUndo? get() = undo.lastOrNull()
}

class PlaylistStudioController(
    private val scope: CoroutineScope,
    private val gateway: PlaylistStudioGateway,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val _session = MutableStateFlow<PlaylistStudioSession?>(null)
    val session: StateFlow<PlaylistStudioSession?> = _session.asStateFlow()

    private var generation = 0L
    private var candidatesJob: Job? = null
    private var saveJob: Job? = null
    private var saveGeneration = -1L

    fun open(draft: PlaylistStudioDraft, candidateSources: () -> List<List<Track>>) {
        generation += 1
        val sessionGeneration = generation
        candidatesJob?.cancel()
        val baseline = draft
        _session.value = PlaylistStudioSession(generation = sessionGeneration, draft = draft, baseline = baseline)
        candidatesJob = scope.launch {
            val index = withContext(computeDispatcher) {
                StudioSearchIndex(mergeStudioCandidates(*candidateSources().toTypedArray()))
            }
            mutate(sessionGeneration) { it.copy(candidates = index) }
        }
    }

    fun close() {
        generation += 1
        candidatesJob?.cancel()
        candidatesJob = null
        _session.value = null
    }

    fun rename(name: String) = edit { PlaylistStudioEdits.rename(it, name) }

    fun toggle(track: Track) = edit { PlaylistStudioEdits.toggle(it, track) }

    fun add(track: Track) = edit { PlaylistStudioEdits.add(it, track) }

    fun remove(trackId: String) {
        val current = _session.value ?: return
        val removed = current.draft.tracks.firstOrNull { it.id == trackId } ?: return
        record(current, PlaylistStudioUndoKind.Removed, removed.title) { PlaylistStudioEdits.remove(it, trackId) }
    }

    fun beginMove(trackId: String) {
        val current = _session.value ?: return
        val moving = current.draft.tracks.firstOrNull { it.id == trackId } ?: return
        mutate(current.generation) { session ->
            val entry = PlaylistStudioUndo(PlaylistStudioUndoKind.Moved, moving.title, session.draft.tracks, session.draft.coverTrackId)
            session.copy(undo = (session.undo + entry).takeLast(MAX_UNDO))
        }
    }

    fun move(fromIndex: Int, toIndex: Int) = edit { PlaylistStudioEdits.move(it, fromIndex, toIndex) }

    fun undo() {
        val current = _session.value ?: return
        val last = current.lastUndo ?: return
        mutate(current.generation) { session ->
            session.copy(
                draft = session.draft.copy(tracks = last.tracks, coverTrackId = last.coverTrackId),
                undo = session.undo.dropLast(1),
                failed = false,
                justSaved = false
            )
        }
    }

    fun dismissUndo() {
        val current = _session.value ?: return
        if (current.undo.isEmpty()) return
        mutate(current.generation) { it.copy(undo = emptyList()) }
    }

    fun setCoverStyle(style: PlaylistCoverStyle) = edit { PlaylistStudioEdits.setCoverStyle(it, style) }

    fun setCoverTrack(trackId: String) = edit { PlaylistStudioEdits.setCoverTrack(it, trackId) }

    fun setPhoto(photo: PlaylistStudioPhoto) = edit { PlaylistStudioEdits.setPhoto(it, photo) }

    fun save() {
        val current = _session.value ?: return
        val sessionGeneration = current.generation
        if (!current.canSave || (saveJob?.isActive == true && saveGeneration == sessionGeneration)) return
        saveGeneration = sessionGeneration
        val snapshot = current.draft
        val baseline = current.baseline
        mutate(sessionGeneration) { it.copy(saving = true, failed = false, justSaved = false) }
        saveJob = scope.launch {
            val existingId = snapshot.playlistId
            var persistedId: String? = existingId
            var rollbackToken: PlaylistStudioRollbackToken? = null
            try {
                if (existingId != null) rollbackToken = gateway.captureRollback(existingId)
                val playlistId = if (existingId == null) {
                    gateway.create(snapshot.name.trim(), snapshot.tracks).also { persistedId = it }
                } else {
                    gateway.update(existingId, snapshot.name.trim(), snapshot.tracks)
                    existingId
                }
                val coverUrl = if (coverNeedsCommit(snapshot, baseline)) {
                    gateway.applyCover(playlistId, snapshot)
                } else {
                    null
                }
                val saved = snapshot.copy(
                    playlistId = playlistId,
                    customCoverUrl = coverUrl ?: snapshot.customCoverUrl
                )
                mutate(sessionGeneration) { session ->
                    session.copy(
                        draft = session.draft.copy(playlistId = playlistId, customCoverUrl = saved.customCoverUrl),
                        baseline = saved,
                        saving = false,
                        failed = false,
                        justSaved = true
                    )
                }
                refreshSafely(playlistId)
            } catch (cancelled: CancellationException) {
                rollbackFailedSave(existingId, persistedId, rollbackToken)
                persistedId?.let(::refreshSafely)
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Playlist Studio save failed")
                val rolledBack = rollbackFailedSave(existingId, persistedId, rollbackToken)
                persistedId?.let(::refreshSafely)
                mutate(sessionGeneration) { session ->
                    val keepPersistedId = existingId == null && persistedId != null && !rolledBack
                    session.copy(
                        draft = if (keepPersistedId) session.draft.copy(playlistId = persistedId) else session.draft,
                        baseline = if (keepPersistedId) session.baseline.copy(playlistId = persistedId) else session.baseline,
                        saving = false,
                        failed = true,
                        justSaved = false
                    )
                }
            }
        }
    }

    fun retry() = save()

    private suspend fun rollbackFailedSave(
        existingId: String?,
        persistedId: String?,
        rollbackToken: PlaylistStudioRollbackToken?
    ): Boolean {
        val playlistId = persistedId ?: return true
        return try {
            withContext(NonCancellable) {
                if (existingId == null) {
                    gateway.rollbackCreated(playlistId)
                } else {
                    gateway.rollbackUpdated(playlistId, rollbackToken)
                }
            }
        } catch (rollbackError: Exception) {
            Timber.w(rollbackError, "Playlist Studio rollback failed")
            false
        }
    }

    private fun refreshSafely(playlistId: String) {
        runCatching { gateway.onSaved(playlistId) }
            .onFailure { Timber.w(it, "Playlist Studio refresh failed") }
    }

    private fun edit(transform: (PlaylistStudioDraft) -> PlaylistStudioDraft) {
        val current = _session.value ?: return
        mutate(current.generation) { session ->
            val next = transform(session.draft)
            if (next === session.draft) session else session.copy(draft = next, justSaved = false, failed = false)
        }
    }

    private fun record(
        current: PlaylistStudioSession,
        kind: PlaylistStudioUndoKind,
        title: String,
        transform: (PlaylistStudioDraft) -> PlaylistStudioDraft
    ) {
        mutate(current.generation) { session ->
            val next = transform(session.draft)
            if (next === session.draft) {
                session
            } else {
                val entry = PlaylistStudioUndo(kind, title, session.draft.tracks, session.draft.coverTrackId)
                session.copy(
                    draft = next,
                    undo = (session.undo + entry).takeLast(MAX_UNDO),
                    justSaved = false,
                    failed = false
                )
            }
        }
    }

    private inline fun mutate(sessionGeneration: Long, transform: (PlaylistStudioSession) -> PlaylistStudioSession) {
        _session.update { session -> if (session == null || session.generation != sessionGeneration) session else transform(session) }
    }

    private companion object {
        const val MAX_UNDO = 20
    }
}

internal fun coverNeedsCommit(snapshot: PlaylistStudioDraft, baseline: PlaylistStudioDraft): Boolean {
    val style = snapshot.coverStyle
    if (style == PlaylistCoverStyle.Current) return false
    if (snapshot.playlistId == null && baseline.playlistId == null && style == PlaylistCoverStyle.Automatic) return false
    if (style != baseline.coverStyle) return true
    return when (style) {
        PlaylistCoverStyle.Automatic -> false
        PlaylistCoverStyle.Photo -> snapshot.photo != baseline.photo
        else -> buildPlaylistCoverPlan(snapshot) != buildPlaylistCoverPlan(baseline)
    }
}
