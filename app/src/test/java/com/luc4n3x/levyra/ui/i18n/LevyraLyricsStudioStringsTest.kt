package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraLyricsStudioStringsTest {

    @Test
    fun allSupportedLanguagesHaveCompleteLyricsStudioKeys() {
        val allBundles = LevyraStrings.all()
        assertEquals("Expected 41 supported languages", 41, allBundles.size)

        allBundles.forEach { bundle ->
            assertTrue("lyricsStudio in ${bundle.code} should not be blank", bundle.lyricsStudio.isNotBlank())
            assertTrue("lyricsStudioSubtitle in ${bundle.code} should not be blank", bundle.lyricsStudioSubtitle.isNotBlank())
            assertTrue("lyricsStudioPresets in ${bundle.code} should not be blank", bundle.lyricsStudioPresets.isNotBlank())
            assertTrue("lyricsStyleOriginal in ${bundle.code} should not be blank", bundle.lyricsStyleOriginal.isNotBlank())
            assertTrue("lyricsStyleExpressive in ${bundle.code} should not be blank", bundle.lyricsStyleExpressive.isNotBlank())
            assertTrue("lyricsStyleMinimal in ${bundle.code} should not be blank", bundle.lyricsStyleMinimal.isNotBlank())
            assertTrue("lyricsStyleFocus in ${bundle.code} should not be blank", bundle.lyricsStyleFocus.isNotBlank())
            assertTrue("lyricsStyleCinematic in ${bundle.code} should not be blank", bundle.lyricsStyleCinematic.isNotBlank())
            assertTrue("lyricsStudioTypography in ${bundle.code} should not be blank", bundle.lyricsStudioTypography.isNotBlank())
            assertTrue("lyricsStudioColors in ${bundle.code} should not be blank", bundle.lyricsStudioColors.isNotBlank())
            assertTrue("lyricsStudioEffects in ${bundle.code} should not be blank", bundle.lyricsStudioEffects.isNotBlank())
            assertTrue("lyricsStudioBackground in ${bundle.code} should not be blank", bundle.lyricsStudioBackground.isNotBlank())
            assertTrue("lyricsStudioBgOriginal in ${bundle.code} should not be blank", bundle.lyricsStudioBgOriginal.isNotBlank())
            assertTrue("lyricsStudioBgSolid in ${bundle.code} should not be blank", bundle.lyricsStudioBgSolid.isNotBlank())
            assertTrue("lyricsStudioBgAmoled in ${bundle.code} should not be blank", bundle.lyricsStudioBgAmoled.isNotBlank())
            assertTrue("lyricsStudioBgGradient in ${bundle.code} should not be blank", bundle.lyricsStudioBgGradient.isNotBlank())
            assertTrue("lyricsStudioBgBlurredArtwork in ${bundle.code} should not be blank", bundle.lyricsStudioBgBlurredArtwork.isNotBlank())
            assertTrue("lyricsStudioBgCinematic in ${bundle.code} should not be blank", bundle.lyricsStudioBgCinematic.isNotBlank())
            assertTrue("lyricsStudioWordKaraoke in ${bundle.code} should not be blank", bundle.lyricsStudioWordKaraoke.isNotBlank())
            assertTrue("lyricsStudioReset in ${bundle.code} should not be blank", bundle.lyricsStudioReset.isNotBlank())
            assertTrue("lyricsStudioResetConfirm in ${bundle.code} should not be blank", bundle.lyricsStudioResetConfirm.isNotBlank())
            assertTrue("lyricsStudioPreview in ${bundle.code} should not be blank", bundle.lyricsStudioPreview.isNotBlank())
        }
    }

    @Test
    fun everySupportedLanguageHasItsOwnCompleteBundle() {
        val supportedCodes = LevyraStrings.all().map { it.code }.toSet()
        assertEquals(supportedCodes, lyricsStudioLocalizationCodes())
        supportedCodes.forEach { code ->
            val entries = lyricsStudioLocalizationEntries(code)
            assertEquals("Incomplete bundle for $code", lyricsStudioKeys, entries.keys)
            assertTrue(entries.values.all { it.isNotBlank() })
            if (code != "en") {
                assertTrue("English fallback for $code", entries["lyricsStudioSubtitle"] !=
                    lyricsStudioLocalizationEntries("en")["lyricsStudioSubtitle"])
            }
        }
    }

    @Test
    fun englishAndItalianSpecificTranslationsAreAccurate() {
        val en = LevyraStrings.forCode("en")
        assertEquals("Lyrics Studio", en.lyricsStudio)
        assertEquals("Levyra Original", en.lyricsStyleOriginal)
        assertEquals("Expressive Karaoke", en.lyricsStyleExpressive)
        assertEquals("Minimal", en.lyricsStyleMinimal)
        assertEquals("Focus", en.lyricsStyleFocus)
        assertEquals("Cinematic", en.lyricsStyleCinematic)

        val it = LevyraStrings.forCode("it")
        assertEquals("Lyrics Studio", it.lyricsStudio)
        assertEquals("Levyra Originale", it.lyricsStyleOriginal)
        assertEquals("Karaoke Espressivo", it.lyricsStyleExpressive)
        assertEquals("Minimale", it.lyricsStyleMinimal)
        assertEquals("Focus", it.lyricsStyleFocus)
        assertEquals("Cinematico", it.lyricsStyleCinematic)
    }
}
