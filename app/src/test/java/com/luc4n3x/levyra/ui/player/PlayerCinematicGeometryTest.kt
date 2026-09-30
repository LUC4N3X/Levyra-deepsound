package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.graphics.Color
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
    fun stackedCanvasUsesLongReferenceDissolve() {
        assertEquals(0.42f, PlayerCinematicStackedFade, 0f)
    }

    @Test
    fun stackedCanvasDissolveMatchesLinearReferenceMask() {
        val stops = playerCinematicDissolveStops(PlayerCinematicStackedFade)

        assertEquals(3, stops.size)
        assertEquals(0f, stops[0].first, 0f)
        assertEquals(Color.Black, stops[0].second)
        assertEquals(0.58f, stops[1].first, 0.0001f)
        assertEquals(Color.Black, stops[1].second)
        assertEquals(1f, stops[2].first, 0f)
        assertEquals(Color.Transparent, stops[2].second)
    }

    @Test
    fun chromeScrimUsesReferenceLuminanceRange() {
        assertEquals(0.16f, playerCinematicTopScrimAlpha(null), 0.0001f)
        assertEquals(0.16f, playerCinematicTopScrimAlpha(0f), 0.0001f)
        assertEquals(0.65f, playerCinematicTopScrimAlpha(1f), 0.0001f)
        assertEquals(0.405f, playerCinematicTopScrimAlpha(0.5f), 0.0001f)
    }

    @Test
    fun fullscreenCanvasKeepsReferenceControlDim() {
        assertEquals(0.5f, playerCinematicFullscreenDimAlpha(fullscreenCanvas = true), 0f)
        assertEquals(0f, playerCinematicFullscreenDimAlpha(fullscreenCanvas = false), 0f)
    }

    @Test
    fun wideStackedLayoutKeepsReferenceSideFade() {
        assertEquals(
            PlayerCinematicSideDissolveFraction,
            playerCinematicResolvedSideFade(sideDissolve = true),
            0f
        )
        assertEquals(0f, playerCinematicResolvedSideFade(sideDissolve = false), 0f)
    }

    @Test
    fun spotifyCanvasUsesFullscreenPhonePresentationOnly() {
        val spotifyCanvas = "https://canvaz.scdn.co/upload/artist/video/example.cnvs.mp4"

        assertTrue(
            playerCinematicUsesFullscreenCanvas(
                layout = PlayerCinematicLayout.Stacked,
                sideDissolve = false,
                motionUrl = spotifyCanvas
            )
        )
        assertFalse(
            playerCinematicUsesFullscreenCanvas(
                layout = PlayerCinematicLayout.Stacked,
                sideDissolve = true,
                motionUrl = spotifyCanvas
            )
        )
        assertFalse(
            playerCinematicUsesFullscreenCanvas(
                layout = PlayerCinematicLayout.SideBySide,
                sideDissolve = false,
                motionUrl = spotifyCanvas
            )
        )
        assertFalse(
            playerCinematicUsesFullscreenCanvas(
                layout = PlayerCinematicLayout.Stacked,
                sideDissolve = false,
                motionUrl = "https://resources.tidal.com/video-cover/example.mp4"
            )
        )
    }

    @Test
    fun phonePortraitImmersiveCanvasStopsAtHeroBottom() {
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
    fun wideStackedImmersiveCanvasKeepsBoundedHeroAndSideDissolve() {
        val geometry = playerCinematicGeometry(
            pane = LevyraPlayerPane.Stacked,
            containerWidth = 800.dp,
            containerHeight = 1280.dp,
            stackedHeroBottom = 500.dp,
            paneGap = 24.dp
        )

        assertEquals(500.dp * PlayerCinematicMaxStackedAspect, geometry.heroWidth)
        assertEquals(500.dp, geometry.heroHeight)
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
    fun immersiveDynamicAndBlurKeepArtworkForTheColorField() {
        assertEquals("art", playerBackdropArtworkUrl(immersive = true, backgroundMode = PlayerBackgroundMode.Blur, artworkUrl = "art"))
        assertEquals("art", playerBackdropArtworkUrl(immersive = true, backgroundMode = PlayerBackgroundMode.Dynamic, artworkUrl = "art"))
        assertEquals("", playerBackdropArtworkUrl(immersive = true, backgroundMode = PlayerBackgroundMode.Dark, artworkUrl = "art"))
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
