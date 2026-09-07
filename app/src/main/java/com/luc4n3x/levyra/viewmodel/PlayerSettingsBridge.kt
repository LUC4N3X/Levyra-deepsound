package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.PlayerVisualMode

fun LevyraViewModel.setPlayerVisualMode(mode: PlayerVisualMode) {
    val current = state.value.interfaceSettings
    if (current.playerVisualMode == mode) return
    setInterfaceSettings(current.copy(playerVisualMode = mode))
}

fun LevyraViewModel.setPlayerBackground(mode: PlayerBackgroundMode) {
    val current = state.value.interfaceSettings
    if (current.playerBackground == mode) return
    setInterfaceSettings(current.copy(playerBackground = mode))
}
