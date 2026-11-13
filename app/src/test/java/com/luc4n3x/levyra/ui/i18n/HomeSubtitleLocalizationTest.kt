package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSubtitleLocalizationTest {

    private val requiredCodes = listOf(
        "en", "it", "es", "fr", "de", "pt", "pt-BR", "nl", "pl", "ro", "el", "sv", "da", "cs", "uk", "ru", "nb", "ca",
        "kk", "tr", "ar", "fa", "zh", "zh-Hant", "ja", "ko", "hi", "bn", "id", "ms", "vi", "th", "fil", "he", "hu",
        "fi", "bg", "hr", "sr", "sk", "et", "sq"
    )

    @Test
    fun `every required language has its own home subtitle copy`() {
        assertEquals(42, requiredCodes.toSet().size)
        assertEquals(requiredCodes.toSet(), homeSectionSubtitleLocalizationCodes())
        assertTrue(LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet().containsAll(requiredCodes))
    }

    @Test
    fun `every subtitle is present, short and translated`() {
        val english = homeSectionSubtitles("en")
        requiredCodes.forEach { code ->
            val copy = homeSectionSubtitles(code)
            HomeSubtitle.entries.forEach { subtitle ->
                val value = copy[subtitle]
                val label = "$code ${subtitle.name}"
                assertTrue(label, value.isNotBlank())
                assertEquals(label, value.trim(), value)
                assertTrue(label, value.codePointCount(0, value.length) <= if (code == "en") 32 else 40)
                assertFalse(label, value.contains('{') || value.contains('\n'))
                if (code != "en" && (code to subtitle) !in sharedWithEnglish) assertNotEquals(label, english[subtitle], value)
            }
            assertEquals(code, HomeSubtitle.entries.size, HomeSubtitle.entries.map { copy[it] }.toSet().size)
        }
    }

    @Test
    fun `every supported language resolves to a full copy`() {
        LevyraLanguageCatalog.selectableLanguages.forEach { language ->
            val copy = homeSectionSubtitles(language.code)
            HomeSubtitle.entries.forEach { assertTrue(language.code, copy[it].isNotBlank()) }
        }
    }

    @Test
    fun `every header key resolves to a subtitle`() {
        requiredCodes.forEach { code ->
            val strings = LevyraStrings.forCode(code)
            (HOME_SUBTITLE_OFFLINE_DOWNLOADS..HOME_SUBTITLE_YOUR_MUSIC).forEach { key ->
                assertTrue("$code $key", homeSubtitleFor(key, "", strings).orEmpty().isNotBlank())
            }
            val copy = homeSectionSubtitles(code)
            assertEquals(code, copy[HomeSubtitle.AlbumsForYou], homeSubtitleFor(HOME_SUBTITLE_ALBUMS, strings.albumsForYou, strings))
            assertEquals(code, copy[HomeSubtitle.TrendingVideos], homeSubtitleFor(HOME_SUBTITLE_VIDEOS, strings.homeTrendingVideoShelfTitle, strings))
            assertEquals(code, copy[HomeSubtitle.NewVideos], homeSubtitleFor(HOME_SUBTITLE_VIDEOS, strings.homeVideoShelfTitle, strings))
        }
    }

    @Test
    fun `english subtitles stay plain`() {
        val english = homeSectionSubtitles("en")
        assertEquals("Out this week", english[HomeSubtitle.NewAlbums])
        assertEquals("From artists you follow", english[HomeSubtitle.ReleaseRadar])
        assertEquals("Most played right now", english[HomeSubtitle.ChartMarket])
        assertEquals("From your listening", english[HomeSubtitle.Orbit])
        HomeSubtitle.entries.forEach { assertFalse(it.name, english[it].startsWith("Tap")) }
    }

    private val sharedWithEnglish = emptySet<Pair<String, HomeSubtitle>>()
}
