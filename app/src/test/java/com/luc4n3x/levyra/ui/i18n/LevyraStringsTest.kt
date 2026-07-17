package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class LevyraStringsTest {
    @Test
    fun catalogAndStringBundlesStayInSync() {
        val catalogCodes = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(catalogCodes, LevyraStrings.supportedCodes())
        assertEquals(catalogCodes, LevyraStrings.all().map { it.code }.toSet())
    }

    @Test
    fun regionAndScriptVariantsResolveToSupportedBundles() {
        assertEquals("nl", LevyraStrings.forCode("nl-NL").code)
        assertEquals("pt", LevyraStrings.forCode("pt_BR").code)
        assertEquals("uk", LevyraStrings.forCode("uk-UA").code)
        assertEquals("en", LevyraStrings.forCode("xx-YY").code)
    }

    @Test
    fun dutchBundleDoesNotLeakItalianSearchCopy() {
        val strings = LevyraStrings.forCode("nl")
        assertEquals("Recente zoekopdrachten", strings.recentSearches)
        assertEquals("Toevoegen aan wachtrij", strings.addToQueue)
        assertEquals("Spraakgestuurd zoeken wordt niet ondersteund", strings.voiceSearchUnsupported)
        assertEquals("YouTube Music doorzoeken…", strings.searchingYouTubeMusic)
        assertEquals("Ontdek nieuwe muziek, trends en video's", strings.exploreSubtitle)
        assertEquals("De externe link kan niet worden geopend", strings.cannotOpenExternalLink)
        assertFalse(strings.exploreSubtitle.contains("\\"))
        assertFalse(strings.recentSearches.contains("Ricerche", ignoreCase = true))
    }

    @Test
    fun newlyAddedLanguagesContainNativeCoreCopy() {
        val russian = LevyraStrings.forCode("ru")
        val turkish = LevyraStrings.forCode("tr")
        assertEquals("Недавние запросы", russian.recentSearches)
        assertEquals("Son aramalar", turkish.recentSearches)
        assertEquals("Все", russian.all)
        assertEquals("Tümü", turkish.all)
    }

    @Test
    fun lyricsControlsUseLocalizedLabels() {
        val italian = LevyraStrings.forCode("it")
        val ukrainian = LevyraStrings.forCode("uk")
        assertEquals("Duetto", italian.lyricsDuet)
        assertEquals("Pagina", italian.lyricsPage)
        assertEquals("Compatta", italian.lyricsCompact)
        assertEquals("Ritornello", italian.lyricsSectionChorus)
        assertEquals("Романізація", ukrainian.lyricsRomanization)
        assertEquals("Кіно", ukrainian.lyricsCinema)
    }

    @Test
    fun mainUiDoesNotContainKnownItalianLocalizationLeaks() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
        ).firstOrNull(Files::exists) ?: error("LevyraApp.kt not found")
        val content = Files.readString(source)
        val forbidden = listOf(
            "\"Ricerche recenti\"",
            "\"Profilo artista non disponibile\"",
            "\"Brani popolari\"",
            "\"Singoli ed EP\"",
            "\"Cartella download\"",
            "\"Nessun download offline\"",
            "\"INTERFACCIA HOME\"",
            "\"BACKUP E RIPRISTINO\"",
            "\"Sto cercando su YouTube Music…\"",
            "\"Cerco il testo…\""
        )
        forbidden.forEach { leaked -> assertFalse("Hardcoded localization leak: $leaked", content.contains(leaked)) }
    }

    @Test
    fun viewModelDoesNotStoreLocalizedArtistErrorCopy() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
        ).firstOrNull(Files::exists) ?: error("LevyraViewModel.kt not found")

        assertFalse(Files.readString(source).contains("Profilo artista non disponibile"))
    }

    @Test
    fun localizedFormattersUseSelectedLanguage() {
        val dutch = LevyraStrings.forCode("nl")
        val polish = LevyraStrings.forCode("pl")
        val turkish = LevyraStrings.forCode("tr")
        assertEquals("2 resultaten", dutch.formatSearchResults(2))
        assertEquals("3 wyniki", polish.formatSearchResults(3))
        assertEquals("İndiriliyor", turkish.localizeDownloadState("RUNNING"))
        assertTrue(dutch.formatGreeting("Luca", 9).startsWith("Goedemorgen, Luca"))
    }

    @Test
    fun arabicFewPluralUsesModuloOneHundredAcrossFormatters() {
        val strings = LevyraStrings.forCode("ar")
        val cases = listOf(
            3 to listOf("3 مقاطع", "تم تنزيل 3 مقاطع", "تم حفظ 3 مقاطع", "3 نتائج"),
            10 to listOf("10 مقاطع", "تم تنزيل 10 مقاطع", "تم حفظ 10 مقاطع", "10 نتائج"),
            11 to listOf("11 مقطعًا", "تم تنزيل 11 مقطعًا", "تم حفظ 11 مقطعًا", "11 نتيجة"),
            103 to listOf("103 مقاطع", "تم تنزيل 103 مقاطع", "تم حفظ 103 مقاطع", "103 نتائج"),
            111 to listOf("111 مقطعًا", "تم تنزيل 111 مقطعًا", "تم حفظ 111 مقطعًا", "111 نتيجة")
        )

        cases.forEach { (value, expected) ->
            assertEquals(expected[0], strings.formatTrackCount(value))
            assertEquals(expected[1], strings.formatDownloadedTrackCount(value))
            assertEquals(expected[2], strings.formatSavedTrackCount(value))
            assertEquals(expected[3], strings.formatSearchResults(value))
        }
    }

    @Test
    fun onboardingAndArtistHeadingsDoNotUseDecorativeEmoji() {
        LevyraStrings.all().forEach { strings ->
            listOf(strings.welcomeBadge, strings.popularTracks, strings.singlesAndEps).forEach { heading ->
                val hasDecorativeSymbol = heading.codePoints().anyMatch { codePoint ->
                    Character.getType(codePoint) == Character.OTHER_SYMBOL.toInt() ||
                        Character.getType(codePoint) == Character.MODIFIER_SYMBOL.toInt()
                }
                assertFalse("Decorative emoji found in ${strings.code}: $heading", hasDecorativeSymbol)
            }
        }
    }
}
