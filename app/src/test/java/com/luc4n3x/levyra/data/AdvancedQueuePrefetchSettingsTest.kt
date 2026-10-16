package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ADVANCED_QUEUE_PREFETCH_OFF
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class AdvancedQueuePrefetchSettingsTest {
    @Test
    fun advancedPrefetchIsOffByDefault() {
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, LevyraAudioSettings().advancedQueuePrefetchTracks)
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, LevyraAudioSettings().normalized().advancedQueuePrefetchTracks)
    }

    @Test
    fun supportedWindowsSurviveNormalization() {
        listOf(0, 5, 10, 15).forEach { tracks ->
            assertEquals(tracks, LevyraAudioSettings(advancedQueuePrefetchTracks = tracks).normalized().advancedQueuePrefetchTracks)
        }
    }

    @Test
    fun unsupportedWindowsFallBackToOff() {
        listOf(-1, 1, 3, 7, 20, Int.MAX_VALUE).forEach { tracks ->
            assertEquals(
                ADVANCED_QUEUE_PREFETCH_OFF,
                LevyraAudioSettings(advancedQueuePrefetchTracks = tracks).normalized().advancedQueuePrefetchTracks
            )
        }
    }

    @Test
    fun windowRoundTripsThroughBackup() {
        val restored = backupAudioSettingsFromJson(
            backupAudioSettingsToJson(LevyraAudioSettings(advancedQueuePrefetchTracks = 10))
        )
        assertEquals(10, restored.advancedQueuePrefetchTracks)
    }

    @Test
    fun mobileDataChoiceIsOffByDefaultAndRoundTripsThroughBackup() {
        assertEquals(false, LevyraAudioSettings().advancedQueuePrefetchOnMobileData)
        val restored = backupAudioSettingsFromJson(
            backupAudioSettingsToJson(LevyraAudioSettings(advancedQueuePrefetchTracks = 15, advancedQueuePrefetchOnMobileData = true))
        )
        assertEquals(true, restored.advancedQueuePrefetchOnMobileData)
        assertEquals(false, backupAudioSettingsFromJson(JSONObject()).advancedQueuePrefetchOnMobileData)
    }

    @Test
    fun legacyBackupWithoutTheFieldRestoresOff() {
        val legacy = JSONObject().put("preloadNextTrack", true).put("gaplessEnabled", true)
        val restored = backupAudioSettingsFromJson(legacy)
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, restored.advancedQueuePrefetchTracks)
        assertEquals(true, restored.preloadNextTrack)
    }

    @Test
    fun corruptedBackupValueRestoresOff() {
        val corrupted = JSONObject().put("advancedQueuePrefetchTracks", 12)
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, backupAudioSettingsFromJson(corrupted).advancedQueuePrefetchTracks)
        val wrongType = JSONObject().put("advancedQueuePrefetchTracks", "many")
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, backupAudioSettingsFromJson(wrongType).advancedQueuePrefetchTracks)
    }
}
