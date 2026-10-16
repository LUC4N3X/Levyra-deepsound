package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedPrefetchLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteCopy() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(supported, advancedPrefetchLocalizationCodes())
        val english = advancedPrefetchCopy("en")
        supported.forEach { code ->
            val copy = advancedPrefetchCopy(code)
            assertTrue(code, listOf(copy.title, copy.description, copy.off).all(String::isNotBlank))
            assertTrue(code, copy.tracks(15).contains("15"))
            assertTrue(code, !copy.tracks(5).contains("{count}"))
            if (code != "en") assertNotEquals(code, english.description, copy.description)
        }
    }

    @Test
    fun mobileDataOptionShipsInEverySupportedLanguage() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(supported, advancedPrefetchMobileDataLocalizationCodes())
        val english = advancedPrefetchMobileDataCopy("en")
        supported.forEach { code ->
            val copy = advancedPrefetchMobileDataCopy(code)
            assertTrue(code, copy.title.isNotBlank() && copy.description.isNotBlank())
            if (code != "en") assertNotEquals(code, english.description, copy.description)
        }
        val russian = advancedPrefetchMobileDataCopy("ru")
        assertEquals("Использовать мобильный интернет", russian.title)
        assertEquals("Заранее загружать следующие треки и через мобильный интернет. Может расходоваться много трафика.", russian.description)
    }

    @Test
    fun italianCopyMatchesTheRequestedWording() {
        val copy = advancedPrefetchCopy("it")
        assertEquals("Precaricamento avanzato", copy.title)
        assertEquals(
            "Scarica temporaneamente i brani successivi nella cache per continuare l'ascolto anche con una connessione instabile. Può utilizzare più dati e spazio di archiviazione.",
            copy.description
        )
        assertEquals("Disattivato", copy.off)
        assertEquals("10 brani", copy.tracks(10))
    }
}
