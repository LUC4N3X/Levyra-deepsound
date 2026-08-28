package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraBackupSettings
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraPreferencesJamDisplayNameTest {

    @Test
    fun normalizeJamDisplayNameTrimsAndEnforcesMaxLength() {
        assertEquals("Luca", normalizeJamDisplayName("  Luca  "))
        assertEquals(32, normalizeJamDisplayName("a".repeat(50)).length)
        assertEquals("a".repeat(32), normalizeJamDisplayName("a".repeat(50)))
        assertEquals("", normalizeJamDisplayName("   "))
    }

    @Test
    fun preferencesSnapshotIncludesJamDisplayName() {
        val snapshot = LevyraPreferencesSnapshot(
            onboarded = true,
            tastes = emptySet(),
            userName = "TestUser",
            languageCode = "en",
            animationsEnabled = true,
            motionArtworkEnabled = true,
            dynamicColor = true,
            sponsorBlock = true,
            skipSilence = false,
            audioQuality = "Auto",
            dismissedUpdateVersion = "",
            lastTrack = null,
            lastPositionMs = 0L,
            recentSearches = emptyList(),
            personalOrbitTracks = emptyList(),
            audioNormalization = false,
            lyricsTranslationEnabled = false,
            themePreset = "apple_music",
            audioSettings = LevyraAudioSettings(),
            interfaceSettings = LevyraInterfaceSettings(),
            downloadSettings = LevyraDownloadSettings(),
            backupSettings = LevyraBackupSettings(),
            jamDisplayName = "DJ Luca"
        )
        assertEquals("DJ Luca", snapshot.jamDisplayName)
    }
}
