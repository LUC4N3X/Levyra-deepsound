package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraExperienceSettingsTest {
    @Test
    fun interfaceSettingsClampGestureValues() {
        val normalized = LevyraInterfaceSettings(
            doubleTapSeekSeconds = 120,
            longPressSpeed = 8f
        ).normalized()

        assertEquals(30, normalized.doubleTapSeekSeconds)
        assertEquals(3f, normalized.longPressSpeed)
    }

    @Test
    fun downloadSettingsClampConcurrency() {
        assertEquals(1, LevyraDownloadSettings(maxConcurrentDownloads = 0).normalized().maxConcurrentDownloads)
        assertEquals(4, LevyraDownloadSettings(maxConcurrentDownloads = 12).normalized().maxConcurrentDownloads)
    }

    @Test
    fun downloadPresetsSelectIndependentOfflineQuality() {
        assertEquals("High", LevyraDownloadSettings(preset = LevyraDownloadPreset.HighQuality).resolverAudioQuality)
        assertEquals("Low", LevyraDownloadSettings(preset = LevyraDownloadPreset.DataSaver).resolverAudioQuality)
        assertEquals(null, LevyraDownloadSettings(preset = LevyraDownloadPreset.Automatic).resolverAudioQuality)
    }

    @Test
    fun downloadProfilesPersistPresetAndRequestedQuality() {
        val automatic = LevyraDownloadSettings(preset = LevyraDownloadPreset.Automatic)
        val high = LevyraDownloadSettings(preset = LevyraDownloadPreset.HighQuality)
        val saver = LevyraDownloadSettings(preset = LevyraDownloadPreset.DataSaver)

        assertEquals("Automatic", automatic.storedPresetKey)
        assertEquals("Medium", automatic.storedQualityKey("Medium"))
        assertEquals("Auto", automatic.storedQualityKey())
        assertEquals("HighQuality", high.storedPresetKey)
        assertEquals("High", high.storedQualityKey("Medium"))
        assertEquals("DataSaver", saver.storedPresetKey)
        assertEquals("Low", saver.storedQualityKey("Medium"))
    }

    @Test
    fun duplicatePolicyHonorsSkipExistingSetting() {
        val downloaded = setOf("track-1")

        assertTrue(LevyraDownloadSettings(skipExisting = true).shouldSkipExistingDownload("track-1", downloaded))
        assertFalse(LevyraDownloadSettings(skipExisting = false).shouldSkipExistingDownload("track-1", downloaded))
        assertFalse(LevyraDownloadSettings(skipExisting = true).shouldSkipExistingDownload("", downloaded))
        assertFalse(LevyraDownloadSettings(skipExisting = true).shouldSkipExistingDownload("track-2", downloaded))
    }

    @Test
    fun downloadSettingsNormalizeRateAndParallelism() {
        val invalid = LevyraDownloadSettings(maxRateKbps = 777).normalized()
        val dataSaver = LevyraDownloadSettings(preset = LevyraDownloadPreset.DataSaver).normalized()
        val highQuality = LevyraDownloadSettings(preset = LevyraDownloadPreset.HighQuality, maxRateKbps = 4096).normalized()

        assertEquals(0, invalid.maxRateKbps)
        assertEquals(1024, dataSaver.effectiveRateKbps)
        assertEquals(2, dataSaver.maxParallelFragments)
        assertEquals(4096, highQuality.effectiveRateKbps)
        assertEquals(10, highQuality.maxParallelFragments)
    }
}
