package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.PlaybackBufferMode
import com.luc4n3x.levyra.domain.PlaybackBufferSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackBufferBackupTest {
    @Test
    fun customBufferRoundTripsThroughBackup() {
        val configured = PlaybackBufferSettings(
            mode = PlaybackBufferMode.CUSTOM,
            minBufferSeconds = 8f,
            maxBufferSeconds = 32f,
            playbackBufferSeconds = 0.7f,
            rebufferSeconds = 1.6f
        )

        val restored = backupAudioSettingsFromJson(
            backupAudioSettingsToJson(LevyraAudioSettings(playbackBuffer = configured))
        )

        assertEquals(configured, restored.playbackBuffer)
    }

    @Test
    fun legacyBackupDefaultsToAutomatic() {
        assertEquals(
            PlaybackBufferMode.AUTOMATIC,
            backupAudioSettingsFromJson(JSONObject()).playbackBuffer.mode
        )
    }
}
