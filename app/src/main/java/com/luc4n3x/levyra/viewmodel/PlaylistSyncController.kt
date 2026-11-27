package com.luc4n3x.levyra.viewmodel

import android.content.Context
import android.os.SystemClock
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.local.PlaylistSyncEntity
import com.luc4n3x.levyra.data.playlistexport.planRemotePlaylistExport
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncErrorKind
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncOutcome
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncSummary
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAccountHttpException
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLibraryEntry
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicPlaylistSyncRepository
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicProtocolException
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicSessionExpired
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.items
import com.luc4n3x.levyra.domain.playableTracks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

internal data class PlaylistSyncSheetState(
    val visible: Boolean = false,
    val playlistId: String = "",
    val playlistName: String = "",
    val trackCount: Int = 0,
    val readyCount: Int = 0,
    val unmatchedCount: Int = 0,
    val linked: PlaylistSyncEntity? = null,
    val accountLabel: String? = null,
    val accountId: String? = null,
    val signedOut: Boolean = false,
    val syncing: Boolean = false,
    val loadingRemotePlaylists: Boolean = false,
    val remotePlaylists: List<YoutubeMusicLibraryEntry> = emptyList(),
    val remotePlaylistsError: PlaylistSyncErrorKind? = null,
    val error: PlaylistSyncErrorKind? = null,
    val lastSummary: PlaylistSyncSummary? = null
)

internal class PlaylistSyncController(
    private val scope: CoroutineScope,
    context: Context,
    private val playlistStore: PlaylistStore,
    private val languageProvider: () -> String,
    private val elapsedRealtimeMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val onLocalPlaylistChanged: (String) -> Unit
) {
    private val repository = YoutubeMusicPlaylistSyncRepository(
        context = context.applicationContext,
        playlistStore = playlistStore,
        onLocalPlaylistChanged = onLocalPlaylistChanged
    )

    val syncStates: Flow<Map<String, PlaylistSyncEntity>> get() = repository.syncStates
    val activeSyncIds: StateFlow<Set<String>> get() = repository.activeSyncIds
    val activeRemoteBrowseIds: StateFlow<Set<String>> get() = repository.activeRemoteBrowseIds

    private val _sheet = MutableStateFlow(PlaylistSyncSheetState())
    val sheet: StateFlow<PlaylistSyncSheetState> = _sheet.asStateFlow()

    private val pendingDebounceJobs = ConcurrentHashMap<String, Job>()
    private val activeSyncJobs = ConcurrentHashMap<String, Job>()
    private val pendingSyncManualFlags = ConcurrentHashMap<String, Boolean>()
    private var sheetLoadJob: Job? = null
    private var bulkAutoSyncJob: Job? = null
    @Volatile
    private var lastBulkAutoSyncStartedAtMs: Long = Long.MIN_VALUE

    init {
        scope.launch {
            repository.syncStates.collect { states ->
                _sheet.update { current ->
                    if (!current.visible) current
                    else {
                        val updated = states[current.playlistId]
                        current.copy(
                            linked = updated,
                            unmatchedCount = updated?.lastUnmatchedCount ?: current.unmatchedCount
                        )
                    }
                }
            }
        }
        scope.launch {
            repository.activeSyncIds.collect { activeIds ->
                _sheet.update { current ->
                    if (!current.visible) current
                    else current.copy(syncing = current.playlistId in activeIds)
                }
            }
        }
    }

    fun open(playlist: Playlist, linked: PlaylistSyncEntity?) {
        sheetLoadJob?.cancel()
        val tracks = playlist.items().flatMap { it.playableTracks() }
        val exportPlan = planRemotePlaylistExport(tracks)
        _sheet.value = PlaylistSyncSheetState(
            visible = true,
            playlistId = playlist.id,
            playlistName = playlist.name,
            trackCount = tracks.size,
            readyCount = exportPlan.videoIds.size,
            unmatchedCount = linked?.lastUnmatchedCount ?: exportPlan.unmatched.size,
            linked = linked,
            syncing = playlist.id in activeSyncIds.value,
            error = linked?.lastSyncError?.let(PlaylistSyncErrorKind::fromStorage)
        )
        refreshAccountAndRemotePlaylists(loadRemote = linked == null)
    }

    fun dismiss() {
        sheetLoadJob?.cancel()
        _sheet.value = PlaylistSyncSheetState()
    }

    fun refreshRemotePlaylists() {
        refreshAccountAndRemotePlaylists(loadRemote = true)
    }

    fun createRemoteAndSync(autoSync: Boolean) {
        val current = _sheet.value
        val playlistId = current.playlistId.takeIf(String::isNotBlank) ?: return
        pendingDebounceJobs.remove(playlistId)?.cancel()
        if (activeSyncJobs[playlistId]?.isActive == true) return
        lateinit var job: Job
        job = scope.launch {
            _sheet.update { it.copy(syncing = true, error = null, lastSummary = null) }
            val outcome = repository.createAndLink(playlistId, autoSync, languageProvider())
            applyOutcomeToSheet(playlistId, outcome)
            drainPendingSyncs(playlistId)
        }
        activeSyncJobs[playlistId] = job
        job.invokeOnCompletion { activeSyncJobs.remove(playlistId, job) }
    }

    fun linkExistingAndSync(remotePlaylistId: String, remoteTitle: String, autoSync: Boolean) {
        val current = _sheet.value
        val playlistId = current.playlistId.takeIf(String::isNotBlank) ?: return
        pendingDebounceJobs.remove(playlistId)?.cancel()
        if (activeSyncJobs[playlistId]?.isActive == true) return
        lateinit var job: Job
        job = scope.launch {
            _sheet.update { it.copy(syncing = true, error = null, lastSummary = null) }
            val outcome = repository.linkExisting(
                playlistId = playlistId,
                remotePlaylistId = remotePlaylistId,
                remoteTitle = remoteTitle,
                autoSync = autoSync,
                language = languageProvider()
            )
            applyOutcomeToSheet(playlistId, outcome)
            drainPendingSyncs(playlistId)
        }
        activeSyncJobs[playlistId] = job
        job.invokeOnCompletion { activeSyncJobs.remove(playlistId, job) }
    }

    fun syncNow(playlistId: String) {
        if (playlistId.isBlank()) return
        pendingDebounceJobs.remove(playlistId)?.cancel()
        enqueueSync(playlistId, onlyIfAutoSync = false)
    }

    fun setAutoSync(playlistId: String, enabled: Boolean) {
        if (playlistId.isBlank()) return
        scope.launch {
            repository.setAutoSync(playlistId, enabled)
            _sheet.update { current ->
                if (current.visible && current.playlistId == playlistId && current.linked != null) {
                    current.copy(linked = current.linked.copy(autoSync = enabled))
                } else {
                    current
                }
            }
        }
    }

    fun unlink(playlistId: String) {
        if (playlistId.isBlank()) return
        cancelAutoSync(playlistId)
        scope.launch {
            repository.unlink(playlistId)
            val updated = _sheet.value.let { current ->
                current.visible && current.playlistId == playlistId
            }
            if (updated) {
                _sheet.update {
                    it.copy(
                        linked = null,
                        error = null,
                        lastSummary = null,
                        syncing = false
                    )
                }
                refreshAccountAndRemotePlaylists(loadRemote = true)
            }
        }
    }

    fun cancelAutoSync(playlistId: String) {
        if (playlistId.isBlank()) return
        pendingDebounceJobs.remove(playlistId)?.cancel()
        pendingSyncManualFlags.remove(playlistId)
        activeSyncJobs.remove(playlistId)?.cancel()
    }

    fun scheduleAutoSync(playlistId: String) {
        if (playlistId.isBlank()) return
        pendingDebounceJobs.remove(playlistId)?.cancel()
        lateinit var debounceJob: Job
        debounceJob = scope.launch {
            delay(AUTO_SYNC_DEBOUNCE_MS)
            pendingDebounceJobs.remove(playlistId, debounceJob)
            enqueueSync(playlistId, onlyIfAutoSync = true)
        }
        pendingDebounceJobs[playlistId] = debounceJob
        debounceJob.invokeOnCompletion { pendingDebounceJobs.remove(playlistId, debounceJob) }
    }

    fun syncAutoSyncPlaylists(force: Boolean = false) {
        if (bulkAutoSyncJob?.isActive == true) return
        val now = elapsedRealtimeMs()
        if (!force && lastBulkAutoSyncStartedAtMs != Long.MIN_VALUE &&
            now - lastBulkAutoSyncStartedAtMs < AUTO_SYNC_FOREGROUND_COOLDOWN_MS
        ) {
            return
        }
        lastBulkAutoSyncStartedAtMs = now
        lateinit var job: Job
        job = scope.launch {
            val outcomes = repository.syncAllAutoSync(languageProvider())
            outcomes.forEach { (playlistId, outcome) ->
                applyOutcomeToSheet(playlistId, outcome)
            }
        }
        bulkAutoSyncJob = job
        job.invokeOnCompletion {
            if (bulkAutoSyncJob === job) bulkAutoSyncJob = null
        }
    }

    fun syncRemoteEntry(
        entry: YoutubeMusicLibraryEntry,
        onCompleted: (playlistId: String?, outcome: PlaylistSyncOutcome) -> Unit = { _, _ -> }
    ) {
        scope.launch {
            val (playlistId, outcome) = repository.linkAndSyncRemoteEntry(entry, languageProvider())
            if (playlistId != null) {
                applyOutcomeToSheet(playlistId, outcome)
            }
            onCompleted(playlistId, outcome)
        }
    }

    private fun enqueueSync(playlistId: String, onlyIfAutoSync: Boolean) {
        val forceManual = !onlyIfAutoSync
        val currentJob = activeSyncJobs[playlistId]
        if (currentJob?.isActive == true) {
            pendingSyncManualFlags.merge(playlistId, forceManual) { existing, incoming -> existing || incoming }
            return
        }
        lateinit var job: Job
        job = scope.launch {
            var nextOnlyIfAutoSync = onlyIfAutoSync
            while (true) {
                if (_sheet.value.visible && _sheet.value.playlistId == playlistId) {
                    _sheet.update { it.copy(syncing = true, error = null, lastSummary = null) }
                }
                val outcome = repository.sync(playlistId, languageProvider(), onlyIfAutoSync = nextOnlyIfAutoSync)
                applyOutcomeToSheet(playlistId, outcome)
                val rerunManual = pendingSyncManualFlags.remove(playlistId) ?: break
                nextOnlyIfAutoSync = !rerunManual
            }
        }
        activeSyncJobs[playlistId] = job
        job.invokeOnCompletion { activeSyncJobs.remove(playlistId, job) }
    }

    private suspend fun drainPendingSyncs(playlistId: String) {
        while (true) {
            val rerunManual = pendingSyncManualFlags.remove(playlistId) ?: break
            val outcome = repository.sync(playlistId, languageProvider(), onlyIfAutoSync = !rerunManual)
            applyOutcomeToSheet(playlistId, outcome)
        }
    }

    private fun refreshAccountAndRemotePlaylists(loadRemote: Boolean) {
        val targetPlaylistId = _sheet.value.playlistId
        if (targetPlaylistId.isBlank()) return
        sheetLoadJob?.cancel()
        sheetLoadJob = scope.launch {
            val account = repository.currentAccountInfo()
            if (_sheet.value.playlistId != targetPlaylistId) return@launch
            if (account == null) {
                _sheet.update {
                    it.copy(
                        accountLabel = null,
                        accountId = null,
                        signedOut = true,
                        loadingRemotePlaylists = false,
                        remotePlaylists = emptyList(),
                        remotePlaylistsError = null
                    )
                }
                return@launch
            }
            _sheet.update {
                it.copy(
                    accountLabel = account.name.ifBlank { account.handle },
                    accountId = account.id,
                    signedOut = false,
                    loadingRemotePlaylists = loadRemote,
                    remotePlaylistsError = if (loadRemote) null else it.remotePlaylistsError
                )
            }
            if (!loadRemote) return@launch
            try {
                val owned = repository.ownedRemotePlaylists(languageProvider())
                if (_sheet.value.playlistId != targetPlaylistId) return@launch
                _sheet.update {
                    it.copy(
                        loadingRemotePlaylists = false,
                        remotePlaylists = owned,
                        remotePlaylistsError = null
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Failed to load owned YouTube Music playlists")
                if (_sheet.value.playlistId != targetPlaylistId) return@launch
                _sheet.update {
                    it.copy(
                        loadingRemotePlaylists = false,
                        remotePlaylistsError = error.toRemoteListErrorKind()
                    )
                }
            }
        }
    }

    private suspend fun applyOutcomeToSheet(playlistId: String, outcome: PlaylistSyncOutcome) {
        val refreshedPlaylist = playlistStore.load(playlistId)
        val refreshedTracks = refreshedPlaylist?.items()?.flatMap { it.playableTracks() }.orEmpty()
        val refreshedPlan = planRemotePlaylistExport(refreshedTracks)
        _sheet.update { current ->
            if (!current.visible || current.playlistId != playlistId) return@update current
            when (outcome) {
                is PlaylistSyncOutcome.Synced -> current.copy(
                    playlistName = refreshedPlaylist?.name ?: current.playlistName,
                    trackCount = refreshedTracks.size,
                    readyCount = refreshedPlan.videoIds.size,
                    unmatchedCount = outcome.summary.unmatchedCount,
                    linked = outcome.state,
                    syncing = playlistId in activeSyncIds.value,
                    error = null,
                    lastSummary = outcome.summary
                )
                is PlaylistSyncOutcome.Failed -> current.copy(
                    linked = outcome.state ?: current.linked,
                    syncing = playlistId in activeSyncIds.value,
                    error = outcome.error
                )
            }
        }
    }

    private fun Exception.toRemoteListErrorKind(): PlaylistSyncErrorKind = when (this) {
        is YoutubeMusicSessionExpired -> PlaylistSyncErrorKind.SIGN_IN
        is YoutubeMusicAccountHttpException -> when (status) {
            401, 403 -> PlaylistSyncErrorKind.SIGN_IN
            404 -> PlaylistSyncErrorKind.NOT_FOUND
            else -> PlaylistSyncErrorKind.SERVICE
        }
        is YoutubeMusicProtocolException -> PlaylistSyncErrorKind.SERVICE
        is IOException -> PlaylistSyncErrorKind.NETWORK
        else -> PlaylistSyncErrorKind.SERVICE
    }

    private companion object {
        const val AUTO_SYNC_DEBOUNCE_MS = 1_500L
        const val AUTO_SYNC_FOREGROUND_COOLDOWN_MS = 10_000L
    }
}
