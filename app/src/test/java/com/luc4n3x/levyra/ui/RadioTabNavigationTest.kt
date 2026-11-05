package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavDestinations
import com.luc4n3x.levyra.ui.i18n.radioTabCopy
import com.luc4n3x.levyra.ui.i18n.radioTabLocalizationCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioTabNavigationTest {
    @Test
    fun radioReplacesSearchAndSitsLast() {
        assertEquals(
            listOf(LevyraTab.Home, LevyraTab.Explore, LevyraTab.Library, LevyraTab.Radio),
            levyraBarTabs(NavDestinations.defaultOrder, radioReplacesSearch = true)
        )
    }

    @Test
    fun otherSkinsKeepTheirOrder() {
        assertEquals(NavDestinations.defaultOrder, levyraBarTabs(NavDestinations.defaultOrder, radioReplacesSearch = false))
    }

    @Test
    fun customOrderAndHiddenTabsAreRespected() {
        val custom = listOf(LevyraTab.Library, LevyraTab.Search, LevyraTab.Home)
        assertEquals(listOf(LevyraTab.Library, LevyraTab.Home, LevyraTab.Radio), levyraBarTabs(custom, radioReplacesSearch = true))
    }

    @Test
    fun studioReorderKeepsSearchSlotForOtherSkins() {
        val full = listOf(LevyraTab.Home, LevyraTab.Search, LevyraTab.Explore, LevyraTab.Library)
        val rows = navStudioRows(full, radioReplacesSearch = true)
        assertEquals(listOf(LevyraTab.Home, LevyraTab.Explore, LevyraTab.Library), rows)
        val moved = listOf(LevyraTab.Library, LevyraTab.Home, LevyraTab.Explore)
        assertEquals(
            listOf(LevyraTab.Library, LevyraTab.Search, LevyraTab.Home, LevyraTab.Explore),
            navStudioOrder(full, moved, radioReplacesSearch = true)
        )
        assertEquals(moved, navStudioOrder(full, moved, radioReplacesSearch = false))
        assertEquals(full, navStudioRows(full, radioReplacesSearch = false))
    }

    @Test
    fun radioOrdinalFollowsLibraryForForwardTransitions() {
        assertTrue(LevyraTab.Radio.ordinal > LevyraTab.Library.ordinal)
        assertTrue(LevyraTab.Radio.ordinal < LevyraTab.Player.ordinal)
    }

    @Test
    fun everySelectableLanguageShipsRadioTabCopy() {
        val selectable = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertEquals(selectable, radioTabLocalizationCodes())
        val english = radioTabCopy("en")
        selectable.forEach { code ->
            val copy = radioTabCopy(code)
            assertTrue(code, listOf(copy.tab, copy.forYou, copy.forYouSubtitle, copy.podcastsSubtitle).all(String::isNotBlank))
            if (code != "en") assertNotEquals(code, english.forYouSubtitle, copy.forYouSubtitle)
        }
    }
}
