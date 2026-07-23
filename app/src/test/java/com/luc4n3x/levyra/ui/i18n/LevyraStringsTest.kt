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
        assertEquals("ja", LevyraStrings.forCode("ja-JP").code)
        assertEquals("ko", LevyraStrings.forCode("ko_KR").code)
        assertEquals("id", LevyraStrings.forCode("in-ID").code)
        assertEquals("fil", LevyraStrings.forCode("fil-PH").code)
        assertEquals("fil", LevyraStrings.forCode("tl_PH").code)
        assertEquals("he", LevyraStrings.forCode("he-IL").code)
        assertEquals("he", LevyraStrings.forCode("iw_IL").code)
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
    fun editorialCollectionsUseNeutralProfessionalCopyInEveryLanguage() {
        val externalBrands = listOf("Spotify", "Amazon Music", "YouTube Music")
        LevyraStrings.all().forEach { strings ->
            val subtitle = strings.collectionsSubtitle
            assertTrue("Missing collections subtitle for ${strings.code}", subtitle.isNotBlank())
            externalBrands.forEach { brand ->
                assertFalse(
                    "Collections subtitle for ${strings.code} exposes external brand $brand",
                    subtitle.contains(brand, ignoreCase = true)
                )
            }
        }
    }


    @Test
    fun greekAndFilipinoCollectionLabelsStayLocalized() {
        val greek = LevyraStrings.forCode("el")
        val filipino = LevyraStrings.forCode("fil")

        assertEquals("Ροή ραπ", greek.collectionRap)
        assertEquals("Παλμός ποπ", greek.collectionPop)
        assertEquals("Ikot ng rap", filipino.collectionRap)
        assertEquals("Tibok ng pop", filipino.collectionPop)
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
    fun majorAsianBundlesContainNativeCoreCopy() {
        val japanese = LevyraStrings.forCode("ja")
        val korean = LevyraStrings.forCode("ko")
        val hindi = LevyraStrings.forCode("hi")
        val indonesian = LevyraStrings.forCode("id")
        val vietnamese = LevyraStrings.forCode("vi")
        val thai = LevyraStrings.forCode("th")
        val filipino = LevyraStrings.forCode("fil")
        val hebrew = LevyraStrings.forCode("he")

        assertEquals("最近の検索", japanese.recentSearches)
        assertEquals("재생 대기열에 추가", korean.addToQueue)
        assertEquals("वॉइस सर्च समर्थित नहीं है", hindi.voiceSearchUnsupported)
        assertEquals("Jelajahi", indonesian.explore)
        assertEquals("Lời bài hát", vietnamese.lyrics)
        assertEquals("รายการโปรด", thai.favoritesPlain)
        assertEquals("Mga kamakailang paghahanap", filipino.recentSearches)
        assertEquals("Idagdag sa queue", filipino.addToQueue)
        assertEquals("חיפושים אחרונים", hebrew.recentSearches)
        assertEquals("הוספה לתור", hebrew.addToQueue)
    }

    @Test
    fun majorAsianFormattersUseSelectedLanguage() {
        assertEquals("3 曲", LevyraStrings.forCode("ja").formatTrackCount(3))
        assertEquals("결과 4개", LevyraStrings.forCode("ko").formatSearchResults(4))
        assertEquals("5 ट्रैक डाउनलोड किए गए", LevyraStrings.forCode("hi").formatDownloadedTrackCount(5))
        assertEquals("Mengunduh 67%", LevyraStrings.forCode("id").formatDownloadProgress(67))
        assertEquals("Đã lưu 2 bài hát", LevyraStrings.forCode("vi").formatSavedTrackCount(2))
        assertEquals("กำลังดาวน์โหลด", LevyraStrings.forCode("th").localizeDownloadState("RUNNING"))
        assertEquals("3 kanta ang na-download", LevyraStrings.forCode("fil").formatDownloadedTrackCount(3))
        assertEquals("Nagda-download", LevyraStrings.forCode("fil").localizeDownloadState("RUNNING"))
        assertEquals("הורדו 3 שירים", LevyraStrings.forCode("he").formatDownloadedTrackCount(3))
        assertEquals("מוריד", LevyraStrings.forCode("iw-IL").localizeDownloadState("RUNNING"))
    }

    @Test
    fun newAndroidResourceBundlesMatchTheBaseTranslatableKeys() {
        val resourceRoot = sequenceOf(
            Path.of("app/src/main/res"),
            Path.of("src/main/res")
        ).firstOrNull(Files::exists) ?: error("Android resources not found")
        val expected = setOf(
            "widget_description",
            "widget_idle_subtitle",
            "widget_toggle",
            "widget_next",
            "widget_previous",
            "widget_favorites",
            "widget_flow",
            "widget_offline",
            "widget_lyrics",
            "radar_channel_name",
            "radar_channel_description",
            "radar_new_release_title"
        )
        val qualifiers = listOf("values-ja", "values-ko", "values-hi", "values-b+id", "values-vi", "values-th", "values-b+fil", "values-b+he")
        val namePattern = Regex("""<string name="([^"]+)"""")

        qualifiers.forEach { qualifier ->
            val file = resourceRoot.resolve(qualifier).resolve("strings.xml")
            assertTrue("Missing resource bundle: $qualifier", Files.exists(file))
            val names = namePattern.findAll(Files.readString(file)).map { it.groupValues[1] }.toSet()
            assertEquals("Invalid resource keys in $qualifier", expected, names)
        }
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
    fun rtlLanguagesResolveThroughTheCatalog() {
        assertTrue(LevyraLanguageCatalog.isRtl("ar-SA"))
        assertTrue(LevyraLanguageCatalog.isRtl("he-IL"))
        assertTrue(LevyraLanguageCatalog.isRtl("iw_IL"))
        assertFalse(LevyraLanguageCatalog.isRtl("en-US"))
    }

    @Test
    fun rtlDynamicLatinTextUsesBidiIsolation() {
        listOf("he", "ar").forEach { code ->
            val strings = LevyraStrings.forCode(code)
            assertTrue(strings.formatGreeting("Luca 96", 9).contains("\u2068Luca 96\u2069"))
            assertTrue(strings.formatArtists("The Weeknd").contains("\u2068The Weeknd\u2069"))
            assertTrue(strings.formatInstalledVersion("2.3.11").contains("\u20682.3.11\u2069"))
            assertTrue(strings.formatLatestVersionReady("2.3.11").contains("\u2068LEVYRA 2.3.11\u2069"))
        }
    }

    @Test
    fun appAndOnboardingUseTheSharedRtlResolver() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
        ).firstOrNull(Files::exists) ?: error("LevyraApp.kt not found")
        val content = Files.readString(source)
        assertTrue(Regex("LevyraLanguageCatalog\\.isRtl").findAll(content).count() >= 2)
        assertFalse(content.contains("== \"ar\") LayoutDirection.Rtl"))
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
