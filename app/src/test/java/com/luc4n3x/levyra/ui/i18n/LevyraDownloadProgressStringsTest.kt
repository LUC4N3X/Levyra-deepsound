package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraDownloadProgressStringsTest {
    @Test
    fun coversTheSameLanguagesAsTheRestOfTheApp() {
        assertEquals(dailyTop100LocalizationCodes(), downloadProgressLocalizationCodes())
    }

    @Test
    fun everyLanguageFormatsItsNumbers() {
        downloadProgressLocalizationCodes().forEach { code ->
            val copy = downloadProgressCopy(code)
            val downloaded = copy.downloadedOf(12, 80)
            assertTrue(code, "12" in downloaded && "80" in downloaded)
            assertTrue(code, "7" in copy.queued(7))
            assertTrue(code, "45" in copy.total(45))
            assertTrue(code, copy.openDownloads.isNotBlank())
        }
        assertEquals("Scaricati 12 di 80", downloadProgressCopy("it").downloadedOf(12, 80))
        assertEquals("Total 45%", downloadProgressCopy("en").total(45))
    }
}
