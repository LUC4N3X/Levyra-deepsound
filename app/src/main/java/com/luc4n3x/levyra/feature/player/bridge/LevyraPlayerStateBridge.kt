package com.luc4n3x.levyra.feature.player.bridge

import com.luc4n3x.levyra.feature.player.domain.PlayerModel
import com.luc4n3x.levyra.viewmodel.LevyraUiState

object LevyraPlayerStateBridge {
    fun fromUiState(state: LevyraUiState): PlayerModel = PlayerModel(
        currentTrack = state.currentTrack,
        queue = state.queue,
        isPlaying = state.isPlaying,
        isResolving = state.isResolving,
        isVideoMode = state.isVideoMode,
        repeatMode = state.repeatMode,
        shuffleEnabled = state.shuffleEnabled,
        positionMs = state.positionMs,
        durationMs = state.durationMs,
        errorMessage = state.playerError
    ).normalized()
}
