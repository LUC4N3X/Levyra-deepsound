package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.ReplayGainMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayGainLocalizationTest {
    @Test
    fun everySupportedLanguageHasUsableReplayGainCopy() {
        LevyraStrings.all().forEach { strings ->
            val copy = strings.replayGainCopy()
            assertTrue(strings.code, copy.track.isNotBlank())
            assertTrue(strings.code, copy.album.isNotBlank())
            assertTrue(strings.code, copy.smart.isNotBlank())
            assertTrue(strings.code, copy.clippingProtection.isNotBlank())
            assertTrue(strings.code, copy.peakAware.contains("ReplayGain", ignoreCase = true))
        }
    }

    @Test
    fun everyNonEnglishLanguageHasDedicatedReplayGainCopy() {
        val english = LevyraStrings.forCode("en").replayGainCopy()
        LevyraStrings.all()
            .filterNot { it.code == "en" }
            .forEach { strings ->
                assertNotEquals(strings.code, english, strings.replayGainCopy())
            }
    }

    @Test
    fun italianModeLabelsAreLocalized() {
        val copy = LevyraStrings.forCode("it").replayGainCopy()
        assertEquals("Traccia", copy.modeLabel(ReplayGainMode.TRACK))
        assertEquals("Album", copy.modeLabel(ReplayGainMode.ALBUM))
        assertEquals("Smart", copy.modeLabel(ReplayGainMode.SMART))
    }
}
