package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.feature.jam.JamUiState

internal enum class JamSimilarSongAction {
    SelectExisting,
    AddThenSelect,
    AddOnly,
    Reject
}

internal fun jamSimilarSongAction(jam: JamUiState, existingIndex: Int): JamSimilarSongAction = when {
    jam.canControlPlayback && existingIndex >= 0 -> JamSimilarSongAction.SelectExisting
    jam.canControlPlayback && jam.canAddTracks -> JamSimilarSongAction.AddThenSelect
    jam.canAddTracks -> JamSimilarSongAction.AddOnly
    else -> JamSimilarSongAction.Reject
}
