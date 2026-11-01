package com.luc4n3x.levyra.ui.selection

import com.luc4n3x.levyra.domain.Track

internal fun trackSelectionActions(
    state: TrackSelectionState,
    selectedTracks: List<Track>,
    onPlayNext: (List<Track>) -> Unit,
    onAddToQueue: (List<Track>) -> Unit,
    onAddToPlaylist: (List<Track>) -> Unit,
    onFavorite: (List<Track>) -> Unit,
    onDownload: (List<Track>) -> Unit,
    onDislike: ((List<Track>) -> Unit)? = null
): TrackSelectionActions {
    val playlistTracks = selectedTracks.filter { it.id.isNotBlank() }
    return TrackSelectionActions(
        onPlayNext = selectedTracks.selectionAction(state, onPlayNext),
        onAddToQueue = selectedTracks.selectionAction(state, onAddToQueue),
        onAddToPlaylist = playlistTracks.takeIf { it.isNotEmpty() }?.let { tracks ->
            { onAddToPlaylist(tracks) }
        },
        onFavorite = selectedTracks.selectionAction(state, onFavorite),
        onDownload = selectedTracks.selectionAction(state, onDownload),
        onDislike = onDislike?.let { selectedTracks.selectionAction(state, it) }
    )
}

private fun List<Track>.selectionAction(
    state: TrackSelectionState,
    action: (List<Track>) -> Unit
): (() -> Unit)? = takeIf { it.isNotEmpty() }?.let { tracks ->
    {
        action(tracks)
        state.exit()
    }
}
