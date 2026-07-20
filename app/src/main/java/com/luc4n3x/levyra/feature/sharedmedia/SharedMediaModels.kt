package com.luc4n3x.levyra.feature.sharedmedia

import com.luc4n3x.levyra.domain.Track

enum class SharedMediaKind {
    Video,
    Playlist,
    Album,
    Artist,
    Channel,
    Search,
    Unsupported
}

data class SharedMediaRequest(
    val rawText: String,
    val url: String,
    val kind: SharedMediaKind,
    val videoId: String = "",
    val playlistId: String = "",
    val browseId: String = "",
    val query: String = ""
) {
    val key: String
        get() = listOf(kind.name, videoId, playlistId, browseId, url, query).joinToString("|")
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
