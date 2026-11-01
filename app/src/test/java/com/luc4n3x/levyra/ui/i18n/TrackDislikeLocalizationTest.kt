package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackDislikeLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteCopy() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(supported, trackDislikeLocalizationCodes())
        supported.forEach { code ->
            val copy = trackDislikeCopy(code)
            assertTrue(code, listOf(copy.dislike, copy.removeDislike, copy.skipped, copy.undo).all(String::isNotBlank))
        }
    }

    @Test
    fun italianCopyUsesTheRequestedWording() {
        assertEquals("Non mi piace", trackDislikeCopy("it").dislike)
    }
}
