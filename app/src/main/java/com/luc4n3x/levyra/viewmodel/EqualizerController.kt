package com.luc4n3x.levyra.viewmodel

import android.content.Context
import com.luc4n3x.levyra.data.AutoEqCatalogRepository
import com.luc4n3x.levyra.domain.AutoEqCatalogEntry
import com.luc4n3x.levyra.domain.AutoEqImporter
import com.luc4n3x.levyra.domain.LevyraAudioPreset
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.OrbitLayout
import com.luc4n3x.levyra.domain.ParametricEqProfile
import com.luc4n3x.levyra.domain.ParametricEqualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class EqualizerController(
    scope: CoroutineScope,
    context: Context,
    private val uiState: StateFlow<LevyraUiState>,
    private val updateAudioSettings: (LevyraAudioSettings) -> Unit
) {
    private val autoEqCatalogDelegate = lazy {
        AutoEqCatalogController(scope, AutoEqCatalogRepository(context))
    }
    private val autoEqCatalogController by autoEqCatalogDelegate
    val autoEqCatalog: StateFlow<AutoEqCatalogUiState> get() = autoEqCatalogController.state

    fun setEqualizerEnabled(value: Boolean) {
        updateAudioSettings(
            uiState.value.audioSettings.copy(
                equalizerEnabled = value,
                parametricEqualizerEnabled = if (value) false else uiState.value.audioSettings.parametricEqualizerEnabled
            )
        )
    }

    fun setEqualizerPreset(presetId: String) {
        val custom = uiState.value.audioSettings.customPresets.firstOrNull { it.id == presetId }
        if (custom != null) {
            updateAudioSettings(
                uiState.value.audioSettings.copy(
                    equalizerEnabled = true,
                    presetId = custom.id,
                    bandLevels = custom.levels,
                    bassBoost = custom.bassBoost,
                    virtualizer = custom.virtualizer,
                    preampDb = custom.preampDb,
                    parametricEqualizerEnabled = false
                )
            )
            return
        }
        val preset = LevyraAudioPresets.preset(presetId)
        updateAudioSettings(
            uiState.value.audioSettings.copy(
                equalizerEnabled = true,
                presetId = preset.id,
                bandLevels = preset.levels,
                bassBoost = preset.bassBoost,
                virtualizer = preset.virtualizer,
                parametricEqualizerEnabled = false
            )
        )
    }

    fun applyAutoEqImport(profile: AutoEqImporter.ImportedProfile) {
        updateAudioSettings(
            uiState.value.audioSettings.copy(
                equalizerEnabled = true,
                presetId = LevyraAudioPresets.FLAT,
                bandLevels = profile.bandLevels,
                bassBoost = 0,
                virtualizer = 0,
                preampDb = profile.preampDb,
                parametricEqualizerEnabled = false
            )
        )
    }

    fun openAutoEqCatalog() {
        autoEqCatalogController.open()
    }

    fun updateAutoEqCatalogQuery(query: String) {
        autoEqCatalogController.updateQuery(query)
    }

    fun selectAutoEqCatalogEntry(entry: AutoEqCatalogEntry) {
        autoEqCatalogController.select(entry)
    }

    fun dismissAutoEqCatalogProfile() {
        autoEqCatalogController.dismissSelection()
    }

    fun closeAutoEqCatalog() {
        if (autoEqCatalogDelegate.isInitialized()) autoEqCatalogController.close()
    }

    fun saveAutoEqCustomPreset(name: String, profile: AutoEqImporter.ImportedProfile): Boolean {
        val cleanName = name.trim().take(ParametricEqualizer.MAX_NAME_CHARS)
        if (cleanName.isBlank()) return false
        val preset = LevyraAudioPreset(
            id = AutoEqImporter.customPresetId(cleanName, profile),
            fallbackLabel = cleanName,
            levels = profile.bandLevels,
            bassBoost = 0,
            virtualizer = 0,
            preampDb = profile.preampDb
        )
        val existing = uiState.value.audioSettings.customPresets
        if (existing.none { it.id == preset.id } && existing.size >= LevyraAudioPresets.MAX_CUSTOM_PRESETS) return false
        val customPresets = existing.filterNot { it.id == preset.id } + preset
        updateAudioSettings(
            uiState.value.audioSettings.copy(
                equalizerEnabled = true,
                presetId = preset.id,
                bandLevels = preset.levels,
                bassBoost = preset.bassBoost,
                virtualizer = preset.virtualizer,
                preampDb = preset.preampDb,
                customPresets = customPresets,
                parametricEqualizerEnabled = false
            )
        )
        return true
    }

    fun setParametricEqualizerEnabled(value: Boolean) {
        val settings = uiState.value.audioSettings
        updateAudioSettings(
            settings.copy(
                equalizerEnabled = if (value) false else settings.equalizerEnabled,
                parametricEqualizerEnabled = value,
                activeParametricProfile = settings.activeParametricProfile ?: ParametricEqualizer.defaultProfile
            )
        )
    }

    fun applyParametricAutoEq(profile: ParametricEqProfile) {
        val normalized = profile.normalized() ?: return
        updateAudioSettings(
            uiState.value.audioSettings.copy(
                equalizerEnabled = false,
                parametricEqualizerEnabled = true,
                activeParametricProfile = normalized
            )
        )
    }

    fun applyOrbitEqualizer(profile: ParametricEqProfile, layout: OrbitLayout) {
        val normalized = profile.normalized() ?: return
        updateAudioSettings(
            uiState.value.audioSettings.copy(
                equalizerEnabled = false,
                parametricEqualizerEnabled = true,
                activeParametricProfile = normalized,
                orbitLayout = layout
            )
        )
    }

    fun resetParametricEqualizer() {
        updateAudioSettings(uiState.value.audioSettings.withNeutralParametricEqualizer())
    }

    fun setEqualizerBand(index: Int, value: Int) {
        if (index !in 0 until LevyraAudioPresets.bandCount) return
        val levels = uiState.value.audioSettings.bandLevels.toMutableList()
        if (levels.size != LevyraAudioPresets.bandCount) return
        levels[index] = value.coerceIn(-100, 100)
        updateAudioSettings(
            uiState.value.audioSettings.copy(
                equalizerEnabled = true,
                parametricEqualizerEnabled = false,
                presetId = LevyraAudioPresets.FLAT,
                bandLevels = levels
            )
        )
    }

    fun resetEqualizer() {
        updateAudioSettings(uiState.value.audioSettings.withNeutralEqualizer())
    }
}
