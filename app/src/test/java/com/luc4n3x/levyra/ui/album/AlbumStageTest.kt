package com.luc4n3x.levyra.ui.album

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.playerContrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumStageTest {

    private val artworkPalettes = listOf(
        Color(0xFFF7F4EE) to Color(0xFFE9E4DA),
        Color(0xFF050506) to Color(0xFF101012),
        Color(0xFFE0161E) to Color(0xFF7A0A10),
        Color(0xFF1437FF) to Color(0xFF00D5FF),
        Color(0xFFE0161E) to Color(0xFF12C943),
        Color(0xFFFFE600) to Color(0xFFFF7A00)
    )

    @Test
    fun contentStaysReadableOnEveryFieldInDarkTheme() {
        artworkPalettes.forEach { (primary, secondary) ->
            assertReadable(albumStageColors(primary, secondary, lightTheme = false))
        }
    }

    @Test
    fun contentStaysReadableOnEveryFieldInLightTheme() {
        artworkPalettes.forEach { (primary, secondary) ->
            assertReadable(albumStageColors(primary, secondary, lightTheme = true))
        }
    }

    @Test
    fun primaryActionKeepsContrastForSaturatedArtwork() {
        artworkPalettes.forEach { (primary, secondary) ->
            val stage = albumStageColors(primary, secondary, lightTheme = false)
            assertTrue(playerContrastRatio(stage.actionContent, stage.actionStart) >= PlayerMinimumContrast)
            assertTrue(playerContrastRatio(stage.actionContent, stage.actionEnd) >= PlayerMinimumContrast)
        }
    }

    @Test
    fun heroHeightStaysProportionalAndBounded() {
        assertEquals(412.dp * 1.06f, albumStackedHeroHeight(412.dp, 915.dp))
        assertEquals(360.dp * 1.06f, albumStackedHeroHeight(360.dp, 640.dp))
        assertEquals(900.dp * 0.60f, albumStackedHeroHeight(1000.dp, 900.dp))
        assertEquals(280.dp, albumStackedHeroHeight(360.dp, 400.dp))
    }

    @Test
    fun contentGutterCentersWideLayouts() {
        assertEquals(20.dp, albumContentGutter(412.dp))
        assertEquals(60.dp, albumContentGutter(800.dp))
    }

    private fun assertReadable(stage: AlbumStageColors) {
        listOf(stage.fieldTop, stage.fieldMid, stage.base).forEach { field ->
            assertTrue(playerContrastRatio(stage.content, field) >= PlayerMinimumContrast)
            assertTrue(playerContrastRatio(stage.contentMuted, field) >= PlayerMinimumContrast)
            assertTrue(playerContrastRatio(stage.accent, field) >= PlayerMinimumContrast)
        }
    }
}
