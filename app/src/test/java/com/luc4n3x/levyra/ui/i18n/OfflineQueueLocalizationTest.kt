package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQueueLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteCopy() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(supported, offlineQueueLocalizationCodes())
        val english = offlineQueueCopy("en")
        supported.forEach { code ->
            val copy = offlineQueueCopy(code)
            val subtitle = copy.prefetchCustomSubtitle(50)
            assertTrue(code, listOf(copy.prefetchCustomTitle, copy.preloading, copy.offlineMixes, copy.recentlyDownloaded, copy.mostPlayed).all(String::isNotBlank))
            assertTrue(code, subtitle.contains("50"))
            assertFalse(code, subtitle.contains("{max}"))
            if (code != "en") assertNotEquals(code, english.prefetchCustomSubtitle(50), subtitle)
        }
    }

    @Test
    fun russianCopyMatchesTheForumWording() {
        val copy = offlineQueueCopy("ru")
        assertEquals("Предзагрузка", copy.preloading)
        assertEquals("Выберите от 1 до 50 следующих треков.", copy.prefetchCustomSubtitle(50))
    }
}
