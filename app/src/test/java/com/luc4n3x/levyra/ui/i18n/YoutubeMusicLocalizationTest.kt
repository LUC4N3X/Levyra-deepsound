package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicLocalizationTest {
    private val catalog = LevyraLanguageCatalog.languages.map { it.code }.toSet()

    @Test
    fun everyCatalogLanguageHasCompleteLinkCopy() {
        assertEquals(emptySet<String>(), catalog - youtubeMusicLinkCompleteLocalizationCodes())
        assertEquals(youtubeMusicLinkLocalizationCodes(), youtubeMusicLinkCompleteLocalizationCodes())
    }

    @Test
    fun everyCatalogLanguageHasCompleteScreenCopy() {
        assertEquals(emptySet<String>(), catalog - youtubeMusicCopyCompleteLocalizationCodes())
        assertEquals(youtubeMusicCopyLocalizationCodes(), youtubeMusicCopyCompleteLocalizationCodes())
    }

    @Test
    fun placeholdersSurviveEveryTranslation() {
        catalog.forEach { code ->
            val copy = LevyraStrings.forCode(code).youtubeMusicLinkCopy
            listOf(YoutubeMusicLinkText.SignedInAs, YoutubeMusicLinkText.ConfirmTitle, YoutubeMusicLinkText.SentTo).forEach { key ->
                assertTrue("$code $key", copy.format(key, name = "Desk").contains("Desk"))
            }
            assertTrue("$code QrExpires", copy.format(YoutubeMusicLinkText.QrExpires, time = "4:59").contains("4:59"))
        }
    }

    @Test
    fun localizedCopyIsUsedInsteadOfEnglish() {
        val english = LevyraStrings.forCode("en").youtubeMusicLinkCopy
        val german = LevyraStrings.forCode("de").youtubeMusicLinkCopy
        val japanese = LevyraStrings.forCode("ja").youtubeMusicLinkCopy
        assertEquals("Sign in on a computer", english[YoutubeMusicLinkText.SignInComputer])
        assertEquals("Auf einem Computer anmelden", german[YoutubeMusicLinkText.SignInComputer])
        assertEquals("パソコンでログイン", japanese[YoutubeMusicLinkText.SignInComputer])
        assertEquals("Liked songs", youtubeMusicCopy("en").likes)
        assertNotEquals(youtubeMusicCopy("en").likes, youtubeMusicCopy("fr").likes)
        assertEquals("喜歡的歌曲", youtubeMusicCopy("zh-Hant").likes)
    }

    @Test
    fun brazilianPortugueseIsAdapted() {
        val brazil = LevyraStrings.forCode(LevyraLanguageCatalog.BRAZILIAN_PORTUGUESE).youtubeMusicLinkCopy
        assertEquals("A câmera não está disponível", brazil[YoutubeMusicLinkText.CameraUnavailable])
        assertEquals("Aguardando o celular", brazil[YoutubeMusicLinkText.Waiting])
    }
}
