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
    Connected,
    Disconnected
}

enum class JamFailure {
    InvalidCode,
    ConnectionFailed,
    NotAuthorized,
    HostEnded,
    ProtocolError
}

data class JamTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String
)

data class JamParticipant(
    val id: String,
    val name: String,
    val isHost: Boolean
)

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
    val updatedAtElapsedMs: Long
) {
    companion object {
        const val MAX_QUEUE_SIZE = 200
        const val MAX_PARTICIPANTS = 8
        const val MAX_NAME_LENGTH = 32
        const val MAX_TEXT_LENGTH = 200
    }
}

sealed interface JamAction {
    data class AddTrack(val track: JamTrack) : JamAction
    data class RemoveTrack(val trackId: String) : JamAction
    data class SelectIndex(val index: Int) : JamAction
    data class SetPlayWhenReady(val playWhenReady: Boolean) : JamAction
    data class Seek(val positionMs: Long) : JamAction
    data object Next : JamAction
    data object Previous : JamAction
}

internal fun JamAction.isPlaybackControl(): Boolean = when (this) {
    is JamAction.AddTrack -> false
    is JamAction.RemoveTrack,
    is JamAction.SelectIndex,
    is JamAction.SetPlayWhenReady,
    is JamAction.Seek,
    JamAction.Next,
    JamAction.Previous -> true
}

object JamAuthorization {
    fun allows(permission: JamGuestPermission, action: JamAction): Boolean = when {
        action is JamAction.AddTrack -> permission.canAddTracks
        action.isPlaybackControl() -> permission.canControlPlayback
        else -> false
    }
}
