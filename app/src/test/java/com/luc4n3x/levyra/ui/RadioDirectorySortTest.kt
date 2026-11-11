package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.feature.radio.RadioDirectoryEntry
import java.text.Collator
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioDirectorySortTest {
    private val countries = listOf("US", "DE", "FR", "RU", "CN", "GB", "UA", "AT", "IT").map {
        RadioDirectoryEntry(name = it, code = it, stationCount = 1)
    }

    private fun labels(languageCode: String): List<String> =
        radioDirectoryRows(countries, languageCode) { localizedCountryName(it.code, languageCode) }.map { it.second }

    private fun collated(languageCode: String, values: List<String>): List<String> =
        values.sortedWith(Collator.getInstance(Locale.forLanguageTag(languageCode)).apply { strength = Collator.PRIMARY })

    @Test
    fun countriesFollowLocalizedAlphabeticalOrder() {
        val russian = labels("ru")
        assertEquals(collated("ru", russian), russian)
        assertEquals("Австрия", russian.first())
        assertTrue(russian.indexOf("Россия") < russian.indexOf("Украина"))
        val english = labels("en")
        assertEquals(collated("en", english), english)
        assertEquals(listOf("Austria", "China", "France", "Germany", "Italy", "Russia", "Ukraine"), english.take(7))
    }

    @Test
    fun duplicateDirectoryEntriesCollapseToOneRow() {
        assertEquals(countries.size, radioDirectoryRows(countries + countries.first(), "en") { it.code }.size)
        assertEquals(countries.size, radioDirectoryRows(countries + countries.first(), null) { it.code }.size)
    }

    @Test
    fun unsortedDirectoryKeepsServerOrder() {
        assertEquals(countries.map { it.code }, radioDirectoryRows(countries, null) { it.code }.map { it.second })
    }

    @Test
    fun languagesSortAlphabeticallyAndDropTagJunk() {
        val languages = listOf("russian", "#japan", "english", "", "ukrainian", "arabic").map {
            RadioDirectoryEntry(name = it, stationCount = 1)
        }
        val rows = radioDirectoryRows(languages, "ru", RadioDirectoryEntry::name).map { it.second }
        assertEquals(listOf("arabic", "english", "russian", "ukrainian"), rows)
    }

    @Test
    fun languageNamesFollowTheAppLanguageAndSortThatWay() {
        val languages = listOf("russian" to "ru", "english" to "en", "ukrainian" to "uk", "german" to "de").map { (name, code) ->
            RadioDirectoryEntry(name = name, code = code, stationCount = 1)
        }
        val rows = radioDirectoryRows(languages, "ru") { localizedLanguageName(it, "ru") }
        assertEquals(listOf("Английский", "Немецкий", "Русский", "Украинский"), rows.map { it.second })
        assertEquals("german", rows[1].first.name)
        assertEquals("Inglese", radioLanguageLabel("english", languages, "it"))
        assertEquals("unknown tag", radioLanguageLabel("unknown tag", languages, "it"))
    }

    @Test
    fun italianOrderIsLocaleAware() {
        assertEquals("Austria", labels("it").first())
        assertEquals(collated("it", labels("it")), labels("it"))
    }
}
