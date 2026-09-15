package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.ui.LevyraPlayerPane
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerCinematicGeometryTest {

    @Test
    fun stackedHeroEndsJustInsideTheTitleBlock() {
        val bottom = playerCinematicStackedHeroBottom(
            statusBarTop = 40.dp,
            chromeTopPadding = 6.dp,
            headerHeight = 48.dp,
            itemSpacing = 6.dp,
            heroVerticalPadding = 2.dp,
            artworkSize = 331.dp
        )

        assertEquals((40 + 6 + 48 + 6 + 4 + 331 + 6).dp + PlayerCinematicTitleOverlap, bottom)
    }

    @Test
    fun phonePortraitHeroIsEdgeToEdge() {
        val geometry = playerCinematicGeometry(
            pane = LevyraPlayerPane.Stacked,
            containerWidth = 412.dp,
            containerHeight = 915.dp,
            stackedHeroBottom = 461.dp,
            paneGap = 24.dp
        )

        assertEquals(PlayerCinematicLayout.Stacked, geometry.layout)
        assertEquals(412.dp, geometry.heroWidth)
        assertEquals(461.dp, geometry.heroHeight)
        assertFalse(geometry.sideDissolve)
    }

    @Test
    fun wideStackedHeroIsCappedAndDissolvesSideways() {
        val geometry = playerCinematicGeometry(
            pane = LevyraPlayerPane.Stacked,
            containerWidth = 800.dp,
            containerHeight = 1280.dp,
            stackedHeroBottom = 500.dp,
            paneGap = 24.dp
        )

        assertEquals(500.dp * PlayerCinematicMaxStackedAspect, geometry.heroWidth)
        assertTrue(geometry.sideDissolve)
    }

    @Test
    fun sideBySideHeroCoversStartPaneUpToTheGap() {
        val geometry = playerCinematicGeometry(
            pane = LevyraPlayerPane.SideBySide,
            containerWidth = 915.dp,
            containerHeight = 412.dp,
            stackedHeroBottom = 0.dp,
            paneGap = 24.dp
        )

        assertEquals(PlayerCinematicLayout.SideBySide, geometry.layout)
        assertEquals(915.dp / 2 + 12.dp, geometry.heroWidth)
        assertEquals(412.dp, geometry.heroHeight)
    }

    @Test
    fun immersiveBlurKeepsBlurredArtworkBackdrop() {
        assertEquals("art", playerBackdropArtworkUrl(immersive = true, backgroundMode = PlayerBackgroundMode.Blur, artworkUrl = "art"))
        assertEquals("", playerBackdropArtworkUrl(immersive = true, backgroundMode = PlayerBackgroundMode.Dynamic, artworkUrl = "art"))
        assertEquals("art", playerBackdropArtworkUrl(immersive = false, backgroundMode = PlayerBackgroundMode.Dynamic, artworkUrl = "art"))
    }

    @Test
    fun stackedHeroNeverExceedsContainer() {
        val geometry = playerCinematicGeometry(
            pane = LevyraPlayerPane.Stacked,
            containerWidth = 360.dp,
            containerHeight = 300.dp,
            stackedHeroBottom = 420.dp,
            paneGap = 24.dp
        )

        assertEquals(300.dp, geometry.heroHeight)
    }
}
