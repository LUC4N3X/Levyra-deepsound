package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.desktop.core.model.Track

@Immutable
data class TrackActions(
    val currentTrackId: String,
    val isFavorite: (Track) -> Boolean,
    val onPlay: (List<Track>, Int) -> Unit,
    val onPlayNext: (Track) -> Unit,
    val onEnqueue: (Track) -> Unit,
    val onToggleFavorite: (Track) -> Unit,
    val onAddToPlaylist: (Track) -> Unit
)
