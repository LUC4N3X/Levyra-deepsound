package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraAmbientMode
import com.luc4n3x.levyra.domain.LevyraAmbientSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupAmbientSettingsTest {

    @Test
    fun ambientSettingsRoundTripThroughBackupJson() {
        val settings = LevyraAmbientSettings(
            brightness = 0.72f,
            autoDim = false,
            autoDimAfterSeconds = 45,
            pixelShift = false,
            proximityBlackout = true,
            showLyrics = false,
            showCanvas = false,
            mode = LevyraAmbientMode.Spotlight,
            showClock = false,
            showTitle = false,
            showProgress = false,
            amoledBlack = false
        )

        val restored = backupAmbientSettingsFromJson(backupAmbientSettingsToJson(settings))

        assertEquals(settings, restored)
    }

    @Test
    fun missingAmbientSettingsUseCurrentDefaults() {
        assertEquals(LevyraAmbientSettings(), backupAmbientSettingsFromJson(null))
        assertEquals(LevyraAmbientSettings(), backupAmbientSettingsFromJson(JSONObject()))
    }
}
