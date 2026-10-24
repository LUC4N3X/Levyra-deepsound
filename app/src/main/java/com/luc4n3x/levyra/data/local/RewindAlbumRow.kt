package com.luc4n3x.levyra.data.local

data class RewindAlbumRow(
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val listenedMs: Long,
    val plays: Int
)
