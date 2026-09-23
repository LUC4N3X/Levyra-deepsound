package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.ParametricEqBand
import com.luc4n3x.levyra.domain.ParametricEqProfile
import com.luc4n3x.levyra.domain.ParametricEqualizer
import com.luc4n3x.levyra.domain.ParametricFilterType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParametricEqPersistenceTest {
    private val profile = ParametricEqProfile(
        id = "parametric_custom_reference",
        name = "Reference",
        preampDb = -5.5f,
        bands = listOf(
            ParametricEqBand(105f, 3f, 0.7f, ParametricFilterType.LOW_SHELF),
            ParametricEqBand(1_250f, -2f, 1.4f, ParametricFilterType.PEAK, enabled = false),
            ParametricEqBand(10_000f, 1f, 0.71f, ParametricFilterType.HIGH_SHELF)
        )
    )

    @Test
    fun `profile json round trip preserves every dsp parameter`() {
        assertEquals(profile, parametricProfileFromJson(parametricProfileToJson(profile)))
        assertEquals(listOf(profile), parametricProfilesFromJson(parametricProfilesToJson(listOf(profile))))
    }

    @Test
    fun `invalid persisted coefficients are discarded`() {
        val invalid = parametricProfileToJson(profile).apply {
            getJSONArray("bands").getJSONObject(0).put("q", 0.0)
        }

        assertNull(parametricProfileFromJson(invalid))
    }

    @Test
    fun `backup round trip preserves active and saved profiles`() {
        val settings = LevyraAudioSettings(
            equalizerEnabled = false,
            parametricEqualizerEnabled = true,
            activeParametricProfile = profile,
            customParametricProfiles = listOf(profile)
        )

        val restored = backupAudioSettingsFromJson(backupAudioSettingsToJson(settings))

        assertTrue(restored.parametricEqualizerEnabled)
        assertEquals(profile, restored.activeParametricProfile)
        assertEquals(listOf(profile), restored.customParametricProfiles)
    }

    @Test
    fun `legacy backup remains compatible and leaves parametric eq disabled`() {
        val restored = backupAudioSettingsFromJson(JSONObject().put("equalizerEnabled", true))

        assertFalse(restored.parametricEqualizerEnabled)
        assertNull(restored.activeParametricProfile)
    }

    @Test
    fun `normalization never activates both equalizers`() {
        val normalized = LevyraAudioSettings(
            equalizerEnabled = true,
            parametricEqualizerEnabled = true,
            activeParametricProfile = profile,
            customParametricProfiles = List(ParametricEqualizer.MAX_CUSTOM_PROFILES + 2) { index ->
                profile.copy(id = "${ParametricEqualizer.CUSTOM_PROFILE_PREFIX}$index")
            }
        ).normalized()

        assertFalse(normalized.equalizerEnabled)
        assertTrue(normalized.parametricEqualizerEnabled)
        assertEquals(ParametricEqualizer.MAX_CUSTOM_PROFILES, normalized.customParametricProfiles.size)
    }
}
