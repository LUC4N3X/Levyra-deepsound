package com.luc4n3x.levyra.feature.sharedmedia

import com.luc4n3x.levyra.domain.Track
import java.security.MessageDigest

enum class SharedMediaKind {
    Video,
    Playlist,
    Album,
    Artist,
    Channel,
    Search,
    LevyraPlaylist,
    Unsupported
}

data class SharedMediaRequest(
    val rawText: String,
    val url: String,
    val kind: SharedMediaKind,
    val videoId: String = "",
    val playlistId: String = "",
    val browseId: String = "",
    val query: String = "",
    val sharedPlaylistPayload: String = ""
) {
    val key: String
        get() = listOf(
            kind.name,
            videoId,
            playlistId,
            browseId,
            url,
            query,
            stableSharedPayloadDigest(sharedPlaylistPayload)
        ).joinToString("|")
}

data class SharedMediaPreview(
    val request: SharedMediaRequest,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val tracks: List<Track>,
    val loading: Boolean = false,
    val error: String = ""
) {
    val playable: Boolean
        get() = tracks.isNotEmpty()

    val primaryTrack: Track?
        get() = tracks.firstOrNull()
}


private const val SHARED_PAYLOAD_HEX = "0123456789abcdef"

private fun stableSharedPayloadDigest(payload: String): String {
    if (payload.isEmpty()) return ""
    val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
    return buildString(digest.size * 2) {
        digest.forEach { byte ->
            val value = byte.toInt() and 0xFF
            append(SHARED_PAYLOAD_HEX[value ushr 4])
            append(SHARED_PAYLOAD_HEX[value and 0x0F])
        }
    }
}
