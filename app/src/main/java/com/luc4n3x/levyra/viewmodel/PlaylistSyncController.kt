package com.luc4n3x.levyra.viewmodel

import android.content.Context
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.local.PlaylistSyncEntity
import com.luc4n3x.levyra.data.playlistexport.planRemotePlaylistExport
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncError
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncOutcome
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncSummary
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncTarget
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLibraryEntry
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicPlaylistSyncRepository
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicSessionExpired
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.items
import com.luc4n3x.levyra.domain.playableTracks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

internal data class PlaylistSyncSheetState(
    val visible: Boolean = false,
    val playlistId: String = "",
    val playlistName: String = "",
    val ready: Int = 0,
    val unmatched: Int = 0,
    val accountLoaded: Boolean = false,
    val account: String? = null,
    val remoteLoading: Boolean = false,
    val remote: List<YoutubeMusicLibraryEntry> = emptyList(),
    val remoteError: PlaylistSyncError? = null,
    val busy: Boolean = false,
    val error: PlaylistSyncError? = null,
    val summary: PlaylistSyncSummary? = null
)

internal class PlaylistSyncController(
    private val scope: CoroutineScope,
    context: Context,
    playlistStore: PlaylistStore,
    private val language: () -> String,
    private val onLocalChanged: (String) -> Unit
) {
    private val repository = YoutubeMusicPlaylistSyncRepository(context.applicationContext, playlistStore)
    private val _sheet = MutableStateFlow(PlaylistSyncSheetState())
    private var sheetJob: Job? = null
    private var autoJob: Job? = null
    private var autoPending = false
    private var lastForegroundSync = 0L

    val sheet: StateFlow<PlaylistSyncSheetState> = _sheet.asStateFlow()
    val links: StateFlow<Map<String, PlaylistSyncEntity>> = repository.links.stateIn(scope, SharingStarted.Eagerly, emptyMap())
    val syncing: StateFlow<Set<String>> = repository.syncing

    fun open(playlist: Playlist) {
        sheetJob?.cancel()
        val plan = planRemotePlaylistExport(playlist.items().flatMap { it.playableTracks() })
        _sheet.value = PlaylistSyncSheetState(
            visible = true,
            playlistId = playlist.id,
            playlistName = playlist.name,
            ready = plan.videoIds.size,
            unmatched = plan.unmatched.size
        )
        loadAccount(loadRemote = links.value[playlist.id] == null)
    }

    fun dismiss() {
        sheetJob?.cancel()
        _sheet.value = PlaylistSyncSheetState()
    }

    fun retryRemote() = loadAccount(loadRemote = true)

    fun createAndLink(autoSync: Boolean) = launchSync { id -> repository.link(id, PlaylistSyncTarget.CreateNew, autoSync, language()) }

    fun linkExisting(entry: YoutubeMusicLibraryEntry, autoSync: Boolean) =
        launchSync { id -> repository.link(id, PlaylistSyncTarget.Existing(entry.browseId, entry.title), autoSync, language()) }

    fun syncNow() = launchSync { id -> repository.sync(id, language()) }

    fun setAutoSync(enabled: Boolean) {
        val id = _sheet.value.playlistId.takeIf(String::isNotBlank) ?: return
        scope.launch { repository.setAutoSync(id, enabled) }
    }

    fun unlink() {
        val id = _sheet.value.playlistId.takeIf(String::isNotBlank) ?: return
        scope.launch {
            repository.unlink(id)
            _sheet.update { if (it.playlistId == id) it.copy(error = null, summary = null) else it }
            loadAccount(loadRemote = true)
        }
    }

    fun linkRemote(entry: YoutubeMusicLibraryEntry, onDone: (String?, PlaylistSyncOutcome) -> Unit) {
        scope.launch {
            val (playlistId, outcome) = repository.linkRemote(entry, language())
            playlistId?.let(onLocalChanged)
            onDone(playlistId, outcome)
        }
    }

    fun onPlaylistsChanged() = scheduleAuto(delayMs = LOCAL_CHANGE_DEBOUNCE_MS)

    fun onForeground(now: Long = System.currentTimeMillis()) {
        if (now - lastForegroundSync < FOREGROUND_COOLDOWN_MS) return
        lastForegroundSync = now
        scheduleAuto(delayMs = 0L)
    }

    suspend fun <T> withPlaylistLock(playlistId: String, block: suspend () -> T): T = repository.withPlaylistLock(playlistId, block)

    private fun scheduleAuto(delayMs: Long) {
        if (autoJob?.isActive == true) {
            autoPending = true
            return
        }
        autoJob = scope.launch {
            delay(delayMs)
            do {
                autoPending = false
                val changed = try {
                    repository.syncDue(language(), REMOTE_REFRESH_MS)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Timber.w(error, "automatic playlist sync failed")
                    emptyList()
                }
                changed.forEach(onLocalChanged)
            } while (autoPending)
        }
    }

    private fun launchSync(block: suspend (String) -> PlaylistSyncOutcome) {
        val id = _sheet.value.playlistId.takeIf(String::isNotBlank) ?: return
        if (_sheet.value.busy) return
        _sheet.update { it.copy(busy = true, error = null, summary = null) }
        scope.launch {
            val outcome = block(id)
            onLocalChanged(id)
            _sheet.update { current ->
                if (current.playlistId != id) return@update current
                when (outcome) {
                    is PlaylistSyncOutcome.Synced -> current.copy(busy = false, summary = outcome.summary)
                    is PlaylistSyncOutcome.Failed -> current.copy(busy = false, error = outcome.error)
                }
            }
        }
    }

    private fun loadAccount(loadRemote: Boolean) {
        val id = _sheet.value.playlistId
        sheetJob?.cancel()
        sheetJob = scope.launch {
            val account = repository.activeAccount()
            _sheet.update {
                if (it.playlistId != id) it else it.copy(
                    accountLoaded = true,
                    account = account?.let { active -> active.name.ifBlank { active.handle } },
                    remoteLoading = account != null && loadRemote,
                    remoteError = null
                )
            }
            if (account == null || !loadRemote) return@launch
            try {
                val owned = repository.ownedPlaylists(language())
                _sheet.update { if (it.playlistId != id) it else it.copy(remoteLoading = false, remote = owned) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "owned youtube music playlists unavailable")
                val kind = when (error) {
                    is YoutubeMusicSessionExpired -> PlaylistSyncError.SIGN_IN
                    is IOException -> PlaylistSyncError.NETWORK
                    else -> PlaylistSyncError.SERVICE
                }
                _sheet.update { if (it.playlistId != id) it else it.copy(remoteLoading = false, remoteError = kind) }
            }
        }
    }

    private companion object {
        const val LOCAL_CHANGE_DEBOUNCE_MS = 2_000L
        const val FOREGROUND_COOLDOWN_MS = 60_000L
        const val REMOTE_REFRESH_MS = 15 * 60_000L
    }
}
