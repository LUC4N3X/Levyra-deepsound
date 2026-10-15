package com.luc4n3x.levyra.domain

import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.i18n.navigationStudioCompleteLocalizationCodes
import com.luc4n3x.levyra.ui.i18n.navigationStudioLocalizationCodes
import com.luc4n3x.levyra.ui.navDockExtraClearance
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
    fun legacyExpressiveFlagMigratesToMotion() {
        val decoded = LevyraNavigationStudioSettings.decode(
            """{"version":1,"style":"floating_dock","tunings":{"floating_dock":{"expressive":false,"height":70},"super_expressive":{"expressive":true,"height":84}}}"""
        )
        assertEquals(NavStyle.FloatingDock, decoded.style)
        assertEquals(NavMotion.Minimal, decoded.tuningFor(NavStyle.FloatingDock).motion)
        assertEquals(70, decoded.tuningFor(NavStyle.FloatingDock).heightDp)
        assertEquals(NavMotion.Expressive, decoded.tuningFor(NavStyle.SuperExpressive).motion)
    }

    @Test
    fun incompatibleChoicesFallBackToStyleDefaults() {
        val minimal = NavTuning(indicator = NavIndicator.Floating, cornerDp = 99, opacity = 5, spacingDp = -3)
            .normalized(NavStyle.Minimal)
        assertEquals(NavIndicator.Dot, minimal.indicator)
        assertEquals(LevyraNavigationStudioSettings.spec(NavStyle.Minimal).corner.last, minimal.cornerDp)
        assertEquals(NavTuning.OPACITY_RANGE.first, minimal.opacity)
        assertEquals(0, minimal.spacingDp)
        val glass = NavTuning(background = NavBackground.Solid).normalized(NavStyle.LiquidGlass)
        assertEquals(NavBackground.Translucent, glass.background)
    }

    @Test
    fun richCustomizationRoundTrips() {
        val style = NavStyle.LiquidGlass
        val settings = LevyraNavigationStudioSettings()
            .withStyle(style)
            .updateTuning(style) {
                it.copy(
                    labels = NavLabelMode.Compact,
                    indicator = NavIndicator.Circle,
                    motion = NavMotion.Off,
                    colorSource = NavColorSource.Custom,
                    accent = 0x00FF8A65,
                    surfaceTone = NavSurfaceTone.Amoled,
                    indicatorTone = NavIndicatorTone.Vivid,
                    opacity = 60,
                    inactiveAlpha = 70,
                    cornerDp = 28,
                    spacingDp = 8,
                    paddingDp = 12,
                    indicatorScale = 115,
                    haptics = false
                )
            }
        assertEquals(0xFFFF8A65.toInt(), settings.activeTuning().accent)
        assertEquals(settings, LevyraNavigationStudioSettings.decode(settings.encode()))
    }

    @Test
    fun onlyStyledBarsReserveExtraContentSpace() {
        assertEquals(0.dp, navDockExtraClearance(NavStyle.Original, NavTuning(heightDp = 120)))
        val tall = LevyraNavigationStudioSettings.spec(NavStyle.SuperExpressive).defaults.copy(heightDp = 88)
        assertEquals(12.dp, navDockExtraClearance(NavStyle.SuperExpressive, tall))
        val floating = LevyraNavigationStudioSettings.spec(NavStyle.FloatingDock).defaults
        assertEquals(0.dp, navDockExtraClearance(NavStyle.FloatingDock, floating))
    }

    @Test
    fun navigationStudioCopyCoversEveryLanguage() {
        val expected = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(expected, navigationStudioLocalizationCodes())
        assertEquals(expected, navigationStudioCompleteLocalizationCodes())
    }

    @Test
    fun expandingLabelsAreTheDefaultOnlyForCustomizableStyles() {
        val expanding = listOf(
            NavStyle.SuperExpressive,
            NavStyle.FloatingDock,
            NavStyle.SplitDock,
            NavStyle.ConnectedDock,
            NavStyle.LiquidGlass,
            NavStyle.Minimal,
            NavStyle.ExpressiveCompact
        )
        expanding.forEach { style ->
            assertEquals(style.name, NavLabelMode.Expanding, LevyraNavigationStudioSettings().tuningFor(style).labels)
        }
        assertEquals(NavLabelMode.Always, LevyraNavigationStudioSettings().tuningFor(NavStyle.YtMusic).labels)
        assertEquals(NavLabelMode.Always, LevyraNavigationStudioSettings().tuningFor(NavStyle.Original).labels)
        assertEquals(NavColorSource.Theme, LevyraNavigationStudioSettings().tuningFor(NavStyle.FloatingDock).colorSource)
    }

    @Test
    fun explicitlyStoredLabelModesSurviveTheNewDefault() {
        val decoded = LevyraNavigationStudioSettings.decode(
            """{"version":2,"style":"floating_dock","tunings":{"floating_dock":{"labels":"active","height":70},"minimal":{"labels":"never","height":64},"liquid_glass":{"height":70}}}"""
        )
        assertEquals(NavLabelMode.ActiveOnly, decoded.tuningFor(NavStyle.FloatingDock).labels)
        assertEquals(NavLabelMode.Never, decoded.tuningFor(NavStyle.Minimal).labels)
        assertEquals(NavLabelMode.Expanding, decoded.tuningFor(NavStyle.LiquidGlass).labels)
        assertEquals(decoded, LevyraNavigationStudioSettings.decode(decoded.encode()))
    }

    @Test
    fun artworkColorSourceRoundTrips() {
        val style = NavStyle.SplitDock
        val settings = LevyraNavigationStudioSettings()
            .withStyle(style)
            .updateTuning(style) { it.copy(colorSource = NavColorSource.Artwork, labels = NavLabelMode.Always) }
        val restored = LevyraNavigationStudioSettings.decode(settings.encode())
        assertEquals(NavColorSource.Artwork, restored.activeTuning().colorSource)
        assertEquals(NavLabelMode.Always, restored.activeTuning().labels)
        assertEquals(settings, restored)
    }
}
