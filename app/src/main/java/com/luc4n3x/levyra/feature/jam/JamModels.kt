package com.luc4n3x.levyra.feature.jam

enum class JamRole {
    Host,
    Guest
}

enum class JamGuestPermission(val id: String) {
    HostOnly("host_only"),
    AddSongs("add_songs"),
    Collaborative("collaborative");

    val canAddTracks: Boolean get() = this != HostOnly
    val canControlPlayback: Boolean get() = this == Collaborative

    companion object {
        fun fromId(value: String?): JamGuestPermission =
            entries.firstOrNull { it.id.equals(value?.trim(), ignoreCase = true) } ?: HostOnly
    }
}

enum class JamConnectionState {
    Idle,
    Connecting,
    AwaitingApproval,
    Connected,
    Disconnected
}

enum class JamFailure {
    InvalidCode,
    ConnectionFailed,
    NotAuthorized,
    HostEnded,
    ProtocolError,
    Rejected,
    Banned,
    SessionLocked,
    SessionFull,
    Removed
}

data class JamTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String,
    val addedBy: String = ""
)

data class JamParticipant(
    val id: String,
    val name: String,
    val isHost: Boolean
)

data class JamPendingParticipant(
    val participantId: String,
    val guestId: String,
    val name: String,
    val requestedAtElapsedMs: Long
)

object JamCapabilities {
    const val BATCH_ADD_TRACKS = "batch_add_tracks_v1"
    const val MODERATION = "moderation_v1"
    val current: Set<String> = setOf(BATCH_ADD_TRACKS, MODERATION)
}

data class JamSessionState(
    val sessionId: String,
    val hostId: String,
    val revision: Long,
    val createdAt: Long,
    val participants: List<JamParticipant>,
    val queue: List<JamTrack>,
    val currentIndex: Int,
    val currentMediaId: String,
    val positionMs: Long,
    val playWhenReady: Boolean,
    val shuffle: Boolean,
    val repeatMode: Int,
    val permission: JamGuestPermission,
    val updatedAtElapsedMs: Long,
    val capabilities: Set<String> = emptySet(),
    val locked: Boolean = false,
    val requireApproval: Boolean = false
) {
    companion object {
        const val MAX_QUEUE_SIZE = 200
        const val MAX_PARTICIPANTS = 8
        const val MAX_PENDING = 8
        const val MAX_BANNED = 32
        const val MAX_NAME_LENGTH = 32
        const val MAX_TEXT_LENGTH = 200
    }
}

sealed interface JamAction {
    data class AddTrack(val track: JamTrack) : JamAction
    data class AddTracks(val tracks: List<JamTrack>) : JamAction
    data class PlayNextTracks(val tracks: List<JamTrack>) : JamAction
    data class RemoveTrack(val trackId: String) : JamAction
    data class SelectIndex(val index: Int) : JamAction
    data class SetPlayWhenReady(val playWhenReady: Boolean) : JamAction
    data class Seek(val positionMs: Long) : JamAction
    data object Next : JamAction
    data object Previous : JamAction
}

internal fun JamAction.isPlaybackControl(): Boolean = when (this) {
    is JamAction.AddTrack,
    is JamAction.AddTracks,
    is JamAction.PlayNextTracks -> false
    is JamAction.RemoveTrack,
    is JamAction.SelectIndex,
    is JamAction.SetPlayWhenReady,
    is JamAction.Seek,
    JamAction.Next,
    JamAction.Previous -> true
}

object JamAuthorization {
    fun allows(permission: JamGuestPermission, action: JamAction): Boolean = when {
        action is JamAction.AddTrack || action is JamAction.AddTracks -> permission.canAddTracks
        action is JamAction.PlayNextTracks -> false
        action.isPlaybackControl() -> permission.canControlPlayback
        else -> false
    }
}

object JamIdentity {
    const val LENGTH = 32

    private val identityPattern = Regex("[0-9a-f]{$LENGTH}")

    fun isValid(value: String): Boolean = identityPattern.matches(value)

    fun sanitize(value: String): String {
        val normalized = value.trim().lowercase()
        return if (isValid(normalized)) normalized else ""
    }
}
