package com.luc4n3x.levyra.player.enhanced

import com.luc4n3x.levyra.data.backupAudioSettingsFromJson
import com.luc4n3x.levyra.data.backupAudioSettingsToJson
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancedAudioSettingsPersistenceTest {

    @Test
    fun defaultEnhancedAudio_isEnabledByDefault() {
        val settings = LevyraAudioSettings()
        assertTrue("Levyra Enhanced Audio must be enabled by default", settings.enhancedAudioEnabled)
    }

    @Test
    fun toggleEnhancedAudio_updatesSettingsCorrectly() {
        val original = LevyraAudioSettings(enhancedAudioEnabled = true)
        assertTrue(original.enhancedAudioEnabled)

        val disabled = original.copy(enhancedAudioEnabled = false)
        assertFalse(disabled.enhancedAudioEnabled)

        val reEnabled = disabled.copy(enhancedAudioEnabled = true)
        assertTrue(reEnabled.enhancedAudioEnabled)
    }

    @Test
    fun backupRoundTrip_preservesEnhancedAudioState() {
        val enabledSettings = LevyraAudioSettings(enhancedAudioEnabled = true)
        val restoredEnabled = backupAudioSettingsFromJson(backupAudioSettingsToJson(enabledSettings))
        assertTrue(restoredEnabled.enhancedAudioEnabled)

        val disabledSettings = LevyraAudioSettings(enhancedAudioEnabled = false)
        val restoredDisabled = backupAudioSettingsFromJson(backupAudioSettingsToJson(disabledSettings))
        assertFalse(restoredDisabled.enhancedAudioEnabled)
    }

    @Test
    fun legacyBackup_defaultsEnhancedAudioToTrue() {
        val legacyJson = JSONObject()
        val restored = backupAudioSettingsFromJson(legacyJson)
        assertTrue("Legacy backups without enhancedAudioEnabled must default to true", restored.enhancedAudioEnabled)
    }
}
