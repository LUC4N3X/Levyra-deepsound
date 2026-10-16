package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTranslationStatusStringsTest {

    @Test
    fun everyLanguageExplainsEveryTranslationOutcome() {
        val bundles = LevyraStrings.all()
        assertEquals(41, bundles.size)
        bundles.forEach { bundle ->
            listOf(
                bundle.lyricsTranslationWorking,
                bundle.lyricsTranslationDownloadRequired,
                bundle.lyricsTranslationDownloading,
                bundle.lyricsTranslationUnavailable,
                bundle.lyricsTranslationFailed
            ).forEach { label ->
                assertTrue("blank translation status in ${bundle.code}", label.isNotBlank())
            }
        }
    }

    @Test
    fun turkishAndItalianUseTheirOwnTranslationStatusCopy() {
        val english = LevyraStrings.forCode("en")
        val turkish = LevyraStrings.forCode("tr")
        val italian = LevyraStrings.forCode("it")

        assertEquals("Bu cihazda bu dil için çeviri kullanılamıyor", turkish.lyricsTranslationUnavailable)
        assertNotEquals(english.lyricsTranslationFailed, turkish.lyricsTranslationFailed)
        assertNotEquals(english.lyricsTranslationDownloadRequired, italian.lyricsTranslationDownloadRequired)
    }
}
