package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QobuzSkinLocalizationTest {
    private val codes = listOf(
        "en", "it", "es", "fr", "de", "pt", "pt-BR", "nl", "pl", "ro", "el", "sv", "da", "cs", "uk", "ru", "nb",
        "ca", "kk", "tr", "ar", "fa", "zh", "zh-Hant", "ja", "ko", "hi", "bn", "id", "ms", "vi", "th", "fil",
        "he", "hu", "fi", "bg", "hr", "sr", "sk", "et", "sq"
    )

    private val brandFields = setOf("qobuzissime", "hiRes", "levyraCategory")

    private val sharedWithEnglish = mapOf(
        "it" to setOf("home", "magazine"),
        "es" to setOf("curatedByQobuz", "magazine"),
        "fr" to setOf("genres", "notifications", "magazine", "releaseType"),
        "de" to setOf("genres", "autoplay"),
        "pt" to setOf("curatedByQobuz", "itemsCountOne"),
        "pt-BR" to setOf("curatedByQobuz", "itemsCountOne"),
        "nl" to setOf("genres", "magazine", "credits", "itemsCount", "itemsCountOne", "releaseType"),
        "ro" to setOf("artist", "explicit", "curatedByQobuz"),
        "sv" to setOf("artist", "explicit"),
        "da" to setOf("releaseType"),
        "hu" to setOf("explicit"),
        "et" to setOf("artist"),
        "nb" to setOf("artist", "releaseType"),
        "ca" to setOf("curatedByQobuz"),
        "id" to setOf("curatedByQobuz", "itemsCountOne"),
        "ms" to setOf("curatedByQobuz", "itemsCountOne"),
        "fil" to setOf("home", "artist", "itemsCountOne")
    )

    private val fields = QobuzSkinCopy::class.java.declaredFields
        .filter { it.type == String::class.java && !it.isSynthetic }
        .onEach { it.isAccessible = true }

    private fun QobuzSkinCopy.values(): Map<String, String> = fields.associate { it.name to it.get(this) as String }

    private fun placeholders(value: String): List<String> =
        Regex("%(\\d+\\$)?[sd]").findAll(value).map { it.value }.sorted().toList()

    @Test
    fun everyAppLanguageIsCovered() {
        assertEquals(42, codes.toSet().size)
        assertEquals(LevyraLanguageCatalog.languages.map { it.code }.toSet() + "pt-BR", codes.toSet())
        assertEquals(76, fields.size)
    }

    @Test
    fun everyLanguageTranslatesEveryNonBrandField() {
        val english = LevyraQobuzSkinStrings.forCode("en").values()
        codes.filter { it != "en" }.forEach { code ->
            val localized = LevyraQobuzSkinStrings.forCode(code).values()
            val allowed = brandFields + sharedWithEnglish[code].orEmpty()
            english.forEach { (field, value) ->
                if (field !in allowed) assertNotEquals("$code.$field", value, localized.getValue(field))
            }
        }
    }

    @Test
    fun valuesAreNonBlankAndKeepEnglishPlaceholders() {
        val english = LevyraQobuzSkinStrings.forCode("en").values()
        codes.forEach { code ->
            LevyraQobuzSkinStrings.forCode(code).values().forEach { (field, value) ->
                assertTrue("$code.$field", value.isNotBlank())
                assertEquals("$code.$field", placeholders(english.getValue(field)), placeholders(value))
            }
        }
    }

    @Test
    fun traditionalChineseNoLongerReusesSimplifiedChinese() {
        val simplified = LevyraQobuzSkinStrings.forCode("zh").values()
        val traditional = LevyraQobuzSkinStrings.forCode("zh-Hant").values()
        val differing = simplified.keys.count { simplified[it] != traditional[it] }
        assertTrue("only $differing fields differ", differing >= 60)
    }

    @Test
    fun brazilianPortugueseUsesBrazilianWording() {
        val brazil = LevyraQobuzSkinStrings.forCode("pt-BR")
        assertEquals("Minha trilha sonora", brazil.welcomeTitle)
        assertEquals("Para você", brazil.forYou)
        assertEquals("Seguindo", brazil.following)
        assertEquals("Configurações", brazil.settings)
        assertEquals("Gêneros", brazil.genres)
        assertEquals("A minha banda sonora", LevyraQobuzSkinStrings.forCode("pt").welcomeTitle)
    }
}
