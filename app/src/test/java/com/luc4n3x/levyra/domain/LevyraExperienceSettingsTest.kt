package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
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
}
