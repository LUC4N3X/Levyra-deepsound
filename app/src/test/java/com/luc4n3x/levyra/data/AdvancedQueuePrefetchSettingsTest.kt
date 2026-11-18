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
        listOf(0, 1, 5, 10, 12, 15, 25, 37, 50).forEach { tracks ->
            assertEquals(tracks, LevyraAudioSettings(advancedQueuePrefetchTracks = tracks).normalized().advancedQueuePrefetchTracks)
        }
    }

    @Test
    fun unsupportedWindowsFallBackToOff() {
        listOf(-1, 51, 100, Int.MIN_VALUE, Int.MAX_VALUE).forEach { tracks ->
            assertEquals(
                ADVANCED_QUEUE_PREFETCH_OFF,
                LevyraAudioSettings(advancedQueuePrefetchTracks = tracks).normalized().advancedQueuePrefetchTracks
            )
        }
    }

    @Test
    fun windowRoundTripsThroughBackup() {
        listOf(10, 37).forEach { tracks ->
            val restored = backupAudioSettingsFromJson(
                backupAudioSettingsToJson(LevyraAudioSettings(advancedQueuePrefetchTracks = tracks))
            )
            assertEquals(tracks, restored.advancedQueuePrefetchTracks)
        }
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
        val corrupted = JSONObject().put("advancedQueuePrefetchTracks", 75)
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, backupAudioSettingsFromJson(corrupted).advancedQueuePrefetchTracks)
        val wrongType = JSONObject().put("advancedQueuePrefetchTracks", "many")
        assertEquals(ADVANCED_QUEUE_PREFETCH_OFF, backupAudioSettingsFromJson(wrongType).advancedQueuePrefetchTracks)
    }
}
