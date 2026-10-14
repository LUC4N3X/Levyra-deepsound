package com.luc4n3x.levyra.domain

import com.luc4n3x.levyra.ui.i18n.navigationStudioCompleteLocalizationCodes
import com.luc4n3x.levyra.ui.i18n.navigationStudioLocalizationCodes
import com.luc4n3x.levyra.ui.navReorderStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraNavigationStudioSettingsTest {

    @Test
    fun defaultsKeepTheOriginalBar() {
        val settings = LevyraNavigationStudioSettings()
        assertEquals(NavStyle.Original, settings.style)
        assertTrue(settings.tunings.isEmpty())
        assertEquals(NavDestinations.defaultOrder, settings.activeTuning().visibleOrder())
    }

    @Test
    fun originalCannotBeCustomized() {
        val settings = LevyraNavigationStudioSettings().updateTuning(NavStyle.Original) { it.copy(heightDp = 60) }
        assertTrue(settings.tunings.isEmpty())
    }

    @Test
    fun corruptOrUnknownStorageFallsBack() {
        assertEquals(LevyraNavigationStudioSettings(), LevyraNavigationStudioSettings.decode("{oops"))
        val decoded = LevyraNavigationStudioSettings.decode(
            """{"style":"rainbow","tunings":{"minimal":{"order":["library","bogus","home"],"hidden":["home","explore"],"labels":"shout","height":999}}}"""
        )
        assertEquals(NavStyle.Original, decoded.style)
        val minimal = decoded.tuningFor(NavStyle.Minimal)
        assertEquals(listOf(LevyraTab.Library, LevyraTab.Home, LevyraTab.Search, LevyraTab.Explore), minimal.order)
        assertEquals(setOf(LevyraTab.Explore), minimal.hidden)
        assertEquals(LevyraNavigationStudioSettings.spec(NavStyle.Minimal).defaults.labels, minimal.labels)
        assertEquals(LevyraNavigationStudioSettings.spec(NavStyle.Minimal).height.last, minimal.heightDp)
    }

    @Test
    fun essentialDestinationsCannotBeHidden() {
        val tuning = NavTuning()
            .withVisibility(LevyraTab.Home, false)
            .withVisibility(LevyraTab.Search, false)
            .withVisibility(LevyraTab.Library, false)
        assertEquals(setOf(LevyraTab.Library), tuning.hidden)
        assertEquals(listOf(LevyraTab.Home, LevyraTab.Search, LevyraTab.Explore), tuning.visibleOrder())
    }

    @Test
    fun customizationRoundTripsAndDefaultsAreDropped() {
        val style = NavStyle.FloatingDock
        val settings = LevyraNavigationStudioSettings()
            .withStyle(style)
            .updateTuning(style) { it.moved(0, 3).copy(indicator = NavIndicator.Line) }
        assertEquals(settings, LevyraNavigationStudioSettings.decode(settings.encode()))
        val restored = settings.updateTuning(style) { LevyraNavigationStudioSettings.spec(style).defaults }
        assertTrue(restored.tunings.isEmpty())
    }

    @Test
    fun everyStyleDefaultIsNormalized() {
        NavStyle.entries.forEach { style ->
            val defaults = LevyraNavigationStudioSettings.spec(style).defaults
            assertEquals(style.name, defaults, defaults.normalized(style))
        }
    }

    @Test
    fun dragReorderMovesPastHalfARow() {
        val order = NavDestinations.defaultOrder
        val heights = order.associateWith { 100 }
        assertEquals(
            listOf(LevyraTab.Search, LevyraTab.Home, LevyraTab.Explore, LevyraTab.Library) to -40f,
            navReorderStep(order, LevyraTab.Home, 60f, heights)
        )
        assertEquals(null, navReorderStep(order, LevyraTab.Home, 40f, heights))
    }

    @Test
    fun navigationStudioCopyCoversEveryLanguage() {
        val expected = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(expected, navigationStudioLocalizationCodes())
        assertEquals(expected, navigationStudioCompleteLocalizationCodes())
    }
}
