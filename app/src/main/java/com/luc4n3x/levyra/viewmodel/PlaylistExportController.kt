package com.luc4n3x.levyra.viewmodel

import android.content.Context
import android.net.Uri
import com.luc4n3x.levyra.data.playlistexport.PlaylistFileFormat
import com.luc4n3x.levyra.data.playlistexport.RemotePlaylistExportPlan
import com.luc4n3x.levyra.data.playlistexport.RemotePlaylistExportProgress
import com.luc4n3x.levyra.data.playlistexport.RemotePlaylistExporter
import com.luc4n3x.levyra.data.playlistexport.planRemotePlaylistExport
import com.luc4n3x.levyra.data.playlistexport.writePlaylistFile
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAccountHttpException
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicPlaylistExporter
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicProtocolException
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicSession
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicSessionExpired
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicSessionStore
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.items
import com.luc4n3x.levyra.domain.playableTracks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

enum class PlaylistExportPhase { CHOOSE, SENDING, SENT, PARTIAL, CANCELLED, FAILED, FILE_SAVED, FILE_FAILED }

enum class PlaylistExportError { NETWORK, SIGN_IN, SERVICE }

data class PlaylistExportUiState(
    val visible: Boolean = false,
    val playlistId: String = "",
    val title: String = "",
    val tracks: List<Track> = emptyList(),
    val plan: RemotePlaylistExportPlan = RemotePlaylistExportPlan(emptyList(), emptyList(), 0),
    val youtubeAccount: String? = null,
    val phase: PlaylistExportPhase = PlaylistExportPhase.CHOOSE,
    val added: Int = 0,
    val remoteUrl: String = "",
    val error: PlaylistExportError? = null,
    val savedFormat: PlaylistFileFormat? = null
) {
    val total: Int get() = plan.videoIds.size
}

class PlaylistExportController internal constructor(
    private val scope: CoroutineScope,
    private val context: Context
) {
    private val _state = MutableStateFlow(PlaylistExportUiState())
    val state: StateFlow<PlaylistExportUiState> = _state.asStateFlow()
    private var session: YoutubeMusicSession? = null
    private var job: Job? = null

    fun open(playlist: Playlist) {
        job?.cancel()
        val tracks = playlist.items().flatMap { it.playableTracks() }
        _state.value = PlaylistExportUiState(
            visible = true,
            playlistId = playlist.id,
            title = playlist.name,
            tracks = tracks,
            plan = planRemotePlaylistExport(tracks)
        )
        job = scope.launch {
            val active = withContext(Dispatchers.IO) {
                runCatching { YoutubeMusicSessionStore(context).load().active }
                    .onFailure { Timber.w(it, "youtube music session unavailable for export") }
                    .getOrNull()
            }
            session = active
            _state.update { it.copy(youtubeAccount = active?.account?.name?.ifBlank { active.account.handle }) }
        }
    }

    fun dismiss() {
        if (_state.value.phase == PlaylistExportPhase.SENDING) return
        job?.cancel()
        _state.value = PlaylistExportUiState()
    }

    fun back() {
        if (_state.value.phase == PlaylistExportPhase.SENDING) return
        _state.update { it.copy(phase = PlaylistExportPhase.CHOOSE, error = null, added = 0, remoteUrl = "", savedFormat = null) }
    }

    fun exportToYoutubeMusic() {
        val current = _state.value
        val active = session ?: return
        if (current.phase == PlaylistExportPhase.SENDING || current.plan.videoIds.isEmpty()) return
        exportRemote(YoutubeMusicPlaylistExporter(active), current)
    }

    private fun exportRemote(exporter: RemotePlaylistExporter, current: PlaylistExportUiState) {
        _state.update { it.copy(phase = PlaylistExportPhase.SENDING, added = 0, remoteUrl = "", error = null) }
        job = scope.launch {
            val progress = object : RemotePlaylistExportProgress {
                override fun onCreated(url: String) = _state.update { it.copy(remoteUrl = url) }
                override fun onAdded(added: Int) = _state.update { it.copy(added = added) }
            }
            try {
                withContext(Dispatchers.IO) { exporter.export(current.title, current.plan.videoIds, progress) }
                _state.update { it.copy(phase = PlaylistExportPhase.SENT) }
            } catch (error: CancellationException) {
                _state.update { if (it.visible) it.copy(phase = PlaylistExportPhase.CANCELLED) else it }
                throw error
            } catch (error: Exception) {
                Timber.w(error, "playlist export to youtube music failed")
                _state.update {
                    it.copy(
                        phase = if (it.remoteUrl.isBlank()) PlaylistExportPhase.FAILED else PlaylistExportPhase.PARTIAL,
                        error = error.toExportError()
                    )
                }
            }
        }
    }

    fun cancel() {
        if (_state.value.phase == PlaylistExportPhase.SENDING) job?.cancel()
    }

    fun saveFile(uri: Uri, format: PlaylistFileFormat) {
        val current = _state.value
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val text = writePlaylistFile(current.title, current.tracks, format)
                    val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
                    output.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }.onFailure { Timber.w(it, "playlist file export failed") }.isSuccess
            }
            _state.update {
                if (!it.visible) it else it.copy(
                    phase = if (saved) PlaylistExportPhase.FILE_SAVED else PlaylistExportPhase.FILE_FAILED,
                    savedFormat = format
                )
            }
        }
    }

    private fun Exception.toExportError(): PlaylistExportError = when (this) {
        is YoutubeMusicSessionExpired -> PlaylistExportError.SIGN_IN
        is YoutubeMusicProtocolException, is YoutubeMusicAccountHttpException -> PlaylistExportError.SERVICE
        is IOException -> PlaylistExportError.NETWORK
        else -> PlaylistExportError.SERVICE
    }
}
