package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.jam.JamAction
import com.luc4n3x.levyra.feature.jam.JamController
import com.luc4n3x.levyra.feature.jam.JamGuestPermission
import com.luc4n3x.levyra.feature.jam.JamPlayerBridge
import com.luc4n3x.levyra.feature.jam.JamTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val JAM_SIMILAR_SONG_SELECT_TIMEOUT_MS = 5_000L
private const val JAM_TRACK_SOURCE = "jam"
private const val JAM_DISPLAY_NAME_MAX_LENGTH = 32

internal class JamSessionController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val preferences: LevyraPreferences,
    jamBridge: JamPlayerBridge
) {
    private val jamControllerDelegate = lazy { JamController(scope, jamBridge) }
    val jamController by jamControllerDelegate
    private var jamStateJob: Job? = null
    private var jamSimilarSongJob: Job? = null

    fun openJam() {
        uiState.update { it.copy(showJam = true) }
    }

    fun closeJam() {
        uiState.update { it.copy(showJam = false) }
    }

    fun setJamDisplayName(value: String) {
        val trimmed = value.take(JAM_DISPLAY_NAME_MAX_LENGTH)
        uiState.update { it.copy(jamDisplayName = trimmed) }
        scope.launch(Dispatchers.IO) { preferences.setJamDisplayName(trimmed) }
    }

    fun createJam(permission: JamGuestPermission, approvalRequired: Boolean = true) {
        scope.launch {
            jamController.createJam(jamDisplayNameOrDefault(), permission, approvalRequired)
        }
    }

    fun joinJam(code: String) {
        scope.launch {
            val identity = withContext(Dispatchers.IO) { preferences.jamGuestId() }
            jamController.joinJam(code, jamDisplayNameOrDefault(), identity)
        }
    }

    fun leaveJam() {
        scope.launch { jamController.leave() }
    }

    fun endJam() {
        scope.launch { jamController.endJam() }
    }

    fun setJamPermission(permission: JamGuestPermission) {
        scope.launch { jamController.setGuestPermission(permission) }
    }

    fun removeJamParticipant(participantId: String, ban: Boolean = false) {
        scope.launch { jamController.removeParticipant(participantId, ban) }
    }

    fun approveJamParticipant(participantId: String) {
        scope.launch { jamController.approveParticipant(participantId) }
    }

    fun rejectJamParticipant(participantId: String) {
        scope.launch { jamController.rejectParticipant(participantId) }
    }

    fun setJamSessionLocked(locked: Boolean) {
        scope.launch { jamController.setSessionLocked(locked) }
    }

    fun setJamApprovalRequired(required: Boolean) {
        scope.launch { jamController.setApprovalRequired(required) }
    }

    fun clearJamBans() {
        scope.launch { jamController.clearBans() }
    }

    fun clearJamFailure() = jamController.clearFailure()

    private fun jamDisplayNameOrDefault(): String {
        val name = uiState.value.jamDisplayName.trim()
        return name.ifBlank { uiState.value.userName.trim().ifBlank { "Levyra" } }
    }

    fun observeJamState() {
        jamStateJob?.cancel()
        jamStateJob = scope.launch {
            jamController.state.collect { jam -> uiState.update { it.copy(jam = jam) } }
        }
    }

    fun routeJamAction(action: JamAction): Boolean {
        if (!uiState.value.jam.isActive) return false
        scope.launch { jamController.requestAction(action) }
        return true
    }

    fun addAndSelectJamTrack(track: Track) {
        jamSimilarSongJob?.cancel()
        jamSimilarSongJob = scope.launch {
            if (!routeJamAction(JamAction.AddTrack(toJamTrack(track)))) return@launch
            val index = withTimeoutOrNull(JAM_SIMILAR_SONG_SELECT_TIMEOUT_MS) {
                jamController.state
                    .map { jam -> jam.session?.queue?.indexOfFirst { it.id == track.id } ?: -1 }
                    .first { it >= 0 }
            } ?: return@launch
            routeJamAction(JamAction.SelectIndex(index))
        }
    }

    fun close() {
        if (jamControllerDelegate.isInitialized()) jamController.close()
    }

    fun release() {
        jamStateJob?.cancel()
        if (jamControllerDelegate.isInitialized()) jamController.close()
    }
}

internal fun toJamTrack(track: Track): JamTrack = JamTrack(
    id = track.id,
    title = track.title,
    artist = track.artist,
    durationMs = track.durationMs,
    thumbnailUrl = track.thumbnailUrl
)

internal fun fromJamTrack(track: JamTrack): Track = Track(
    id = track.id,
    title = track.title,
    artist = track.artist,
    album = "",
    durationMs = track.durationMs,
    streamUrl = "",
    videoUrl = "",
    thumbnailUrl = track.thumbnailUrl,
    largeThumbnailUrl = track.thumbnailUrl,
    source = JAM_TRACK_SOURCE,
    moodTags = emptySet(),
    energy = 0,
    vocal = 0,
    replayScore = 0,
    cacheScore = 0,
    accentStart = 0,
    accentEnd = 0
)
