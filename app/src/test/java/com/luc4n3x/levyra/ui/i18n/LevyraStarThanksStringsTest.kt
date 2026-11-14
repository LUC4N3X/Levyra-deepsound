package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraStarThanksStringsTest {
    private val dailyTop100Codes = listOf(
        "en", "it", "es", "fr", "de", "pt", "pt-BR", "nl", "pl", "ro", "el", "sv", "da", "cs", "sk",
        "hr", "sr", "sq", "bg", "hu", "fi", "et", "nb", "ca", "uk", "ru", "kk", "tr", "ar", "fa",
        "zh", "zh-Hant", "ja", "ko", "hi", "bn", "id", "ms", "vi", "th", "fil", "he"
    )

    @Test
    fun codeListMatchesDailyTop100() {
        assertEquals(42, dailyTop100Codes.size)
        assertEquals(dailyTop100LocalizationCodes(), starThanksLocalizationCodes())
    }

    @Test
    fun englishAndItalianExactValues() {
        assertEquals(
            StarThanksCopy(
                title = "Levyra is free",
                body = "Every update takes a lot of time and work. I'm not asking for money: " +
                    "if you'd like to say thanks, leave a star on GitHub. " +
                    "If you already have, thank you so much. Happy listening.",
                star = "Leave a star",
                done = "I already did, thanks",
                logoDescription = "Levyra logo"
            ),
            starThanksCopy("en")
        )
        assertEquals(
            StarThanksCopy(
                title = "Levyra è gratis",
                body = "Ogni aggiornamento richiede tanto tempo e lavoro. Non ti chiedo soldi: " +
                    "se vuoi dirmi grazie, lasciami una stella su GitHub. " +
                    "Se l'hai già fatto, grazie di cuore. Buon ascolto.",
                star = "Lascia una stella",
                done = "L'ho già fatto, grazie",
                logoDescription = "Logo di Levyra"
            ),
            starThanksCopy("it")
        )
    }

    @Test
    fun unknownCodeFallsBackToEnglish() {
        assertEquals(starThanksCopy("en"), starThanksCopy("xx"))
    }

    @Test
    fun everyDailyTop100LanguageShipsOwnCopy() {
        val english = starThanksCopy("en")
        dailyTop100Codes.forEach { code ->
            val copy = starThanksCopy(code)
            listOf(copy.title, copy.body, copy.star, copy.done, copy.logoDescription).forEach {
                assertTrue(code, it.isNotBlank() && '"' !in it && '—' !in it && '’' !in it)
            }
            if (code != "en") {
                assertNotEquals(code, english.title, copy.title)
                assertNotEquals(code, english.body, copy.body)
                assertNotEquals(code, english.star, copy.star)
                assertNotEquals(code, english.done, copy.done)
            }
        }
    }
}
