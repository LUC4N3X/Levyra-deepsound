package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraYtMusicPairingTest {

    @Test
    fun choosingYtMusicBarAlsoPicksItsMiniPlayer() {
        assertEquals(
            MiniPlayerStyle.YtMusic,
            LevyraYtMusicPairing.miniStyleFor(NavStyle.Original, NavStyle.YtMusic, MiniPlayerStyle.Floating)
        )
        assertEquals(
            NavStyle.YtMusic,
            LevyraYtMusicPairing.navStyleFor(MiniPlayerStyle.Original, MiniPlayerStyle.YtMusic, NavStyle.Minimal)
        )
    }

    @Test
    fun leavingYtMusicRestoresTheOriginalPartner() {
        assertEquals(
            MiniPlayerStyle.Original,
            LevyraYtMusicPairing.miniStyleFor(NavStyle.YtMusic, NavStyle.Minimal, MiniPlayerStyle.YtMusic)
        )
        assertEquals(
            NavStyle.Original,
            LevyraYtMusicPairing.navStyleFor(MiniPlayerStyle.YtMusic, MiniPlayerStyle.Floating, NavStyle.YtMusic)
        )
    }

    @Test
    fun otherStyleChangesLeaveThePartnerAlone() {
        assertEquals(
            MiniPlayerStyle.Floating,
            LevyraYtMusicPairing.miniStyleFor(NavStyle.Original, NavStyle.Minimal, MiniPlayerStyle.Floating)
        )
        assertEquals(
            NavStyle.LiquidGlass,
            LevyraYtMusicPairing.navStyleFor(MiniPlayerStyle.Original, MiniPlayerStyle.Floating, NavStyle.LiquidGlass)
        )
    }

    @Test
    fun connectedDockPairsWithItsMiniPlayerBothWays() {
        assertEquals(
            MiniPlayerStyle.Connected,
            LevyraYtMusicPairing.miniStyleFor(NavStyle.FloatingDock, NavStyle.ConnectedDock, MiniPlayerStyle.Floating)
        )
        assertEquals(
            NavStyle.ConnectedDock,
            LevyraYtMusicPairing.navStyleFor(MiniPlayerStyle.Floating, MiniPlayerStyle.Connected, NavStyle.SplitDock)
        )
        assertEquals(
            MiniPlayerStyle.Original,
            LevyraYtMusicPairing.miniStyleFor(NavStyle.ConnectedDock, NavStyle.SplitDock, MiniPlayerStyle.Connected)
        )
        assertEquals(
            NavStyle.Original,
            LevyraYtMusicPairing.navStyleFor(MiniPlayerStyle.Connected, MiniPlayerStyle.Floating, NavStyle.ConnectedDock)
        )
    }

    @Test
    fun switchingBetweenPairedStylesSwapsThePartnerDirectly() {
        assertEquals(
            MiniPlayerStyle.Connected,
            LevyraYtMusicPairing.miniStyleFor(NavStyle.YtMusic, NavStyle.ConnectedDock, MiniPlayerStyle.YtMusic)
        )
        assertEquals(
            NavStyle.YtMusic,
            LevyraYtMusicPairing.navStyleFor(MiniPlayerStyle.Connected, MiniPlayerStyle.YtMusic, NavStyle.ConnectedDock)
        )
    }

    @Test
    fun splitDockLeavesTheMiniPlayerAlone() {
        assertEquals(
            MiniPlayerStyle.LiquidGlass,
            LevyraYtMusicPairing.miniStyleFor(NavStyle.Original, NavStyle.SplitDock, MiniPlayerStyle.LiquidGlass)
        )
    }

    @Test
    fun newDockStylesRoundTripAndFloat() {
        listOf(NavStyle.SplitDock, NavStyle.ConnectedDock).forEach { style ->
            assertEquals(true, style.floating)
            val settings = LevyraNavigationStudioSettings()
                .withStyle(style)
                .updateTuning(style) { it.copy(cornerDp = 24, heightDp = 60) }
            assertEquals(settings, LevyraNavigationStudioSettings.decode(settings.encode()))
            assertEquals(24, settings.activeTuning().cornerDp)
        }
        val mini = LevyraMiniPlayerStudioSettings().withStyle(MiniPlayerStyle.Connected)
        assertEquals(mini, LevyraMiniPlayerStudioSettings.decode(mini.encode()))
        assertEquals(16, mini.activeTuning().insetDp)
        assertEquals(MiniProgressStyle.Line, mini.activeTuning().progress)
    }

    @Test
    fun ytMusicDefaultsAreFlatAndIndicatorFree() {
        val nav = LevyraNavigationStudioSettings.spec(NavStyle.YtMusic).defaults
        assertEquals(NavIndicator.None, nav.indicator)
        assertEquals(0, nav.cornerDp)
        assertEquals(NavLabelMode.Always, nav.labels)
        val mini = LevyraMiniPlayerStudioSettings.spec(MiniPlayerStyle.YtMusic).defaults
        assertEquals(0, mini.cornerDp)
        assertEquals(0, mini.insetDp)
        assertEquals(MiniControlSet.Full, mini.controls)
        assertEquals(MiniButtonStyle.Plain, mini.buttons)
        val settings = LevyraNavigationStudioSettings().withStyle(NavStyle.YtMusic)
        assertEquals(settings, LevyraNavigationStudioSettings.decode(settings.encode()))
        val miniSettings = LevyraMiniPlayerStudioSettings().withStyle(MiniPlayerStyle.YtMusic)
        assertEquals(miniSettings, LevyraMiniPlayerStudioSettings.decode(miniSettings.encode()))
    }
}
