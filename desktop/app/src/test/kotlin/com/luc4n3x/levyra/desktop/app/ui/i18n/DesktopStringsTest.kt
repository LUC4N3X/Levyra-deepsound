package com.luc4n3x.levyra.desktop.app.ui.i18n

import com.luc4n3x.levyra.desktop.core.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopStringsTest {
    @Test
    fun everyAndroidLanguageBuildsACompleteDesktopCatalog() {
        AppLanguage.entries.forEach { language ->
            val strings = stringsFor(language, "Luca", 9)
            val required = listOf(
                strings.appName,
                strings.navHome,
                strings.navDiscover,
                strings.navSearch,
                strings.navLibrary,
                strings.navSettings,
                strings.navNowPlaying,
                strings.searchPlaceholder,
                strings.searchExploreTitle,
                strings.homeGreeting,
                strings.homeSubtitle,
                strings.homeOrbitTitle,
                strings.homeOrbitSubtitle,
                strings.homeTop50,
                strings.libraryTitle,
                strings.librarySubtitle,
                strings.libraryPlaylists,
                strings.libraryFavorites,
                strings.libraryDownloads,
                strings.libraryHistory,
                strings.downloadAction,
                strings.downloadCancel,
                strings.downloadCompleted,
                strings.downloadRemove,
                strings.playbackPlay,
                strings.playbackClose,
                strings.settingsLanguage,
                strings.settingsCountry,
                strings.onboardingWelcomeBadge,
                strings.onboardingWelcomeTitle,
                strings.onboardingLanguageQuestion,
                strings.onboardingNameQuestion,
                strings.onboardingTasteQuestion,
                strings.onboardingContinue,
                strings.onboardingSkip,
                strings.onboardingStart
            )
            assertTrue("Missing desktop translation for ${language.tag}", required.all { it.isNotBlank() })
            assertEquals(language.tag, strings.languageCode)
        }
    }

    @Test
    fun rtlLanguagesRemainMarkedForBidirectionalLayout() {
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertTrue(AppLanguage.HEBREW.isRtl)
    }
}
