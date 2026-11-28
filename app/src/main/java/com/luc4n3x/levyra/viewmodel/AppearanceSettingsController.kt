package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.LevyraPreferencesSnapshot
import com.luc4n3x.levyra.domain.LevyraCustomTheme
import com.luc4n3x.levyra.domain.LevyraCustomThemes
import com.luc4n3x.levyra.domain.LevyraHomeStudioSettings
import com.luc4n3x.levyra.domain.LevyraLyricsStudioSettings
import com.luc4n3x.levyra.domain.LevyraMiniPlayerStudioSettings
import com.luc4n3x.levyra.domain.LevyraNavigationStudioSettings
import com.luc4n3x.levyra.domain.LevyraYtMusicPairing
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AppearanceSettingsController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val preferences: LevyraPreferences,
    startupSettings: LevyraPreferencesSnapshot
) {
    private var pendingLyricsStudioWrites = 0
    private var lyricsStudioMutationGeneration = 0L
    private var pendingHomeStudioWrites = 0
    private var homeStudioMutationGeneration = 0L
    private var pendingMiniPlayerStudioWrites = 0
    private var miniPlayerStudioMutationGeneration = 0L
    private var pendingNavigationStudioWrites = 0
    private var navigationStudioMutationGeneration = 0L
    private val _miniPlayerStudio = MutableStateFlow(MiniPlayerStudioState(settings = startupSettings.miniPlayerStudioSettings))
    val miniPlayerStudio: StateFlow<MiniPlayerStudioState> = _miniPlayerStudio.asStateFlow()
    private val _navigationStudio = MutableStateFlow(NavigationStudioState(settings = startupSettings.navigationStudioSettings))
    val navigationStudio: StateFlow<NavigationStudioState> = _navigationStudio.asStateFlow()
    private val _customThemes = MutableStateFlow(
        LevyraCustomThemes.normalized(startupSettings.customThemes).also(LevyraThemes::setCustomThemes)
    )
    val customThemes: StateFlow<List<LevyraCustomTheme>> = _customThemes.asStateFlow()

    fun observePreferences() {
        scope.launch {
            preferences.lyricsStudioSettingsFlow.collect { settings ->
                if (pendingLyricsStudioWrites == 0 && settings == preferences.lyricsStudioSettings()) {
                    uiState.update { state -> state.copy(lyricsStudioSettings = settings) }
                }
            }
        }
        scope.launch {
            preferences.customThemesFlow.collect { themes ->
                val normalized = LevyraCustomThemes.normalized(themes)
                if (normalized != _customThemes.value) {
                    LevyraThemes.setCustomThemes(normalized)
                    _customThemes.value = normalized
                }
            }
        }
        scope.launch {
            preferences.homeStudioSettingsFlow.collect { settings ->
                if (pendingHomeStudioWrites == 0 && settings == preferences.homeStudioSettings()) {
                    uiState.update { state -> state.copy(homeStudioSettings = settings) }
                }
            }
        }
        scope.launch {
            preferences.miniPlayerStudioSettingsFlow.collect { settings ->
                if (pendingMiniPlayerStudioWrites == 0 && settings == preferences.miniPlayerStudioSettings()) {
                    _miniPlayerStudio.update { it.copy(settings = settings) }
                }
            }
        }
        scope.launch {
            preferences.navigationStudioSettingsFlow.collect { settings ->
                if (pendingNavigationStudioWrites == 0 && settings == preferences.navigationStudioSettings()) {
                    _navigationStudio.update { it.copy(settings = settings) }
                }
            }
        }
    }

    fun setThemePreset(value: String) {
        val normalized = LevyraThemes.normalize(value)
        preferences.setThemePreset(normalized)
        uiState.update { it.copy(themePreset = normalized) }
    }

    fun setThemeAccent(value: Int) {
        if (uiState.value.themeAccent == value) return
        preferences.setThemeAccent(value)
        uiState.update { it.copy(themeAccent = value) }
    }

    fun saveCustomTheme(theme: LevyraCustomTheme, apply: Boolean) {
        updateCustomThemes(LevyraCustomThemes.upsert(_customThemes.value, theme))
        if (apply) setThemePreset(theme.id)
    }

    fun duplicateCustomTheme(id: String, suffix: String) {
        updateCustomThemes(LevyraCustomThemes.duplicate(_customThemes.value, id, suffix))
    }

    fun deleteCustomTheme(id: String) {
        updateCustomThemes(LevyraCustomThemes.remove(_customThemes.value, id))
        if (uiState.value.themePreset == id) setThemePreset(LevyraThemes.defaultPresetId)
    }

    fun restoreDefaultTheme() {
        setThemeAccent(0)
        setThemePreset(LevyraThemes.defaultPresetId)
    }

    private fun updateCustomThemes(themes: List<LevyraCustomTheme>) {
        val normalized = LevyraCustomThemes.normalized(themes)
        if (normalized == _customThemes.value) return
        LevyraThemes.setCustomThemes(normalized)
        _customThemes.value = normalized
        preferences.setCustomThemes(normalized)
    }

    fun openThemeStudio() {
        uiState.update { it.copy(showThemeStudio = true) }
    }

    fun closeThemeStudio() {
        uiState.update { it.copy(showThemeStudio = false) }
    }

    fun openLyricsStudio() {
        uiState.update { it.copy(showLyricsStudio = true) }
    }

    fun closeLyricsStudio() {
        uiState.update { it.copy(showLyricsStudio = false) }
    }

    fun setLyricsStudioSettings(settings: LevyraLyricsStudioSettings) {
        val normalized = settings.normalized()
        val generation = ++lyricsStudioMutationGeneration
        pendingLyricsStudioWrites += 1
        uiState.update { it.copy(lyricsStudioSettings = normalized) }
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try {
                val saved = preferences.setLyricsStudioSettings(normalized)
                if (generation == lyricsStudioMutationGeneration) {
                    val resolved = if (saved) normalized else preferences.lyricsStudioSettings()
                    uiState.update { it.copy(lyricsStudioSettings = resolved) }
                }
            } finally {
                pendingLyricsStudioWrites -= 1
            }
        }
    }

    fun resetLyricsStudioSettings() {
        setLyricsStudioSettings(LevyraLyricsStudioSettings())
    }

    fun openHomeStudio() {
        uiState.update { it.copy(showHomeStudio = true) }
    }

    fun closeHomeStudio() {
        uiState.update { it.copy(showHomeStudio = false) }
    }

    fun setHomeStudioSettings(settings: LevyraHomeStudioSettings) {
        val normalized = settings.normalized()
        if (normalized == uiState.value.homeStudioSettings) return
        val generation = ++homeStudioMutationGeneration
        pendingHomeStudioWrites += 1
        uiState.update { it.copy(homeStudioSettings = normalized) }
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try {
                val saved = preferences.setHomeStudioSettings(normalized)
                if (generation == homeStudioMutationGeneration) {
                    val resolved = if (saved) normalized else preferences.homeStudioSettings()
                    uiState.update { it.copy(homeStudioSettings = resolved) }
                }
            } finally {
                pendingHomeStudioWrites -= 1
            }
        }
    }

    fun openMiniPlayerStudio() {
        _miniPlayerStudio.update { it.copy(visible = true) }
    }

    fun closeMiniPlayerStudio() {
        _miniPlayerStudio.update { it.copy(visible = false) }
    }

    fun setMiniPlayerStudioSettings(settings: LevyraMiniPlayerStudioSettings) {
        val normalized = settings.normalized()
        val previous = _miniPlayerStudio.value.settings
        if (normalized == previous) return
        val generation = ++miniPlayerStudioMutationGeneration
        pendingMiniPlayerStudioWrites += 1
        _miniPlayerStudio.update { it.copy(settings = normalized) }
        val navigation = _navigationStudio.value.settings
        val pairedNav = LevyraYtMusicPairing.navStyleFor(previous.style, normalized.style, navigation.style)
        if (pairedNav != navigation.style) setNavigationStudioSettings(navigation.withStyle(pairedNav))
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try {
                val saved = preferences.setMiniPlayerStudioSettings(normalized)
                if (generation == miniPlayerStudioMutationGeneration) {
                    val resolved = if (saved) normalized else preferences.miniPlayerStudioSettings()
                    _miniPlayerStudio.update { it.copy(settings = resolved) }
                }
            } finally {
                pendingMiniPlayerStudioWrites -= 1
            }
        }
    }

    fun openNavigationStudio() {
        _navigationStudio.update { it.copy(visible = true) }
    }

    fun closeNavigationStudio() {
        _navigationStudio.update { it.copy(visible = false) }
    }

    fun setNavigationStudioSettings(settings: LevyraNavigationStudioSettings) {
        val normalized = settings.normalized()
        val previous = _navigationStudio.value.settings
        if (normalized == previous) return
        val generation = ++navigationStudioMutationGeneration
        pendingNavigationStudioWrites += 1
        _navigationStudio.update { it.copy(settings = normalized) }
        val miniPlayer = _miniPlayerStudio.value.settings
        val pairedMini = LevyraYtMusicPairing.miniStyleFor(previous.style, normalized.style, miniPlayer.style)
        if (pairedMini != miniPlayer.style) setMiniPlayerStudioSettings(miniPlayer.withStyle(pairedMini))
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try {
                val saved = preferences.setNavigationStudioSettings(normalized)
                if (generation == navigationStudioMutationGeneration) {
                    val resolved = if (saved) normalized else preferences.navigationStudioSettings()
                    _navigationStudio.update { it.copy(settings = resolved) }
                }
            } finally {
                pendingNavigationStudioWrites -= 1
            }
        }
    }
}
