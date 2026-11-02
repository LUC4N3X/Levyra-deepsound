package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteCopy() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(supported, podcastLocalizationCodes())
        supported.forEach { code ->
            val copy = podcastCopy(code)
            assertTrue(code, copy.all().all(String::isNotBlank))
            assertTrue(code, copy.minutesLeft.contains("%d"))
        }
    }

    @Test
    fun italianCopyFormatsRemainingMinutes() {
        assertEquals("Podcast", podcastCopy("it").title)
        assertEquals("12 min rimanenti", podcastCopy("it").minutesLeft(12))
    }
}
