package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.FreshDiscoveryWindow
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshDiscoveryLocalizationTest {
    @Test
    fun everySupportedLanguageShipsCompleteCopy() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(supported, freshDiscoveryLocalizationCodes())
        val english = freshDiscoveryCopy("en")
        supported.forEach { code ->
            val copy = freshDiscoveryCopy(code)
            val values = listOf(
                copy.title,
                copy.description,
                copy.windowTitle,
                copy.windowDescription,
                copy.allowFavoritesTitle,
                copy.allowFavoritesDescription
            ) + FreshDiscoveryWindow.entries.map(copy::window)
            assertTrue(code, values.all(String::isNotBlank))
            assertEquals(code, FreshDiscoveryWindow.entries.size, FreshDiscoveryWindow.entries.map(copy::window).toSet().size)
            if (code != "en") {
                assertNotEquals(code, english.description, copy.description)
                assertNotEquals(code, english.windowDescription, copy.windowDescription)
                assertNotEquals(code, english.allowFavoritesDescription, copy.allowFavoritesDescription)
            }
        }
    }

    @Test
    fun italianCopyMatchesTheRequestedWording() {
        val copy = freshDiscoveryCopy("it")
        assertEquals("Fresh Discovery", copy.title)
        assertEquals("Consenti preferiti", copy.allowFavoritesTitle)
        assertEquals("30 giorni", copy.window(FreshDiscoveryWindow.DAYS_30))
        assertEquals("Da sempre", copy.window(FreshDiscoveryWindow.ALL_TIME))
    }
}
