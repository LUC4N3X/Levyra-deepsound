package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.ReactiveAuraPreset

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

fun LevyraViewModel.setReactiveAuraPreset(preset: ReactiveAuraPreset) {
    val current = state.value.interfaceSettings
    if (current.reactiveAuraPreset == preset) return
    setInterfaceSettings(current.copy(reactiveAuraPreset = preset))
}

fun LevyraViewModel.setReactiveAuraIntensity(intensity: Int) {
    val current = state.value.interfaceSettings
    val coerced = intensity.coerceIn(0, 100)
    if (current.reactiveAuraIntensity == coerced) return
    setInterfaceSettings(current.copy(reactiveAuraIntensity = coerced))
}
