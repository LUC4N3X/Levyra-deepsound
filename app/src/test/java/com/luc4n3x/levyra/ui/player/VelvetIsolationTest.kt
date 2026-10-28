package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.ui.LevyraPlayerPane
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VelvetIsolationTest {

    private val velvetSources = listOf(
        "ui/player/AppleMusicPlayer.kt",
        "ui/player/AppleMusicCanvasBackdrop.kt",
        "ui/player/PlayerDeckAura.kt"
    )

    private val defaultOnlyPrimitives = listOf(
        "LocalHomeShelfExpressive",
        "homeShelfArtwork",
        "homeShelfDepth",
        "HomeCollapsedTopBar",
        "LevyraExpressiveScrollbar",
        "FlowTrackShift",
        "FlowAmbient",
        "FlowSideNudge"
    )

    @Test
    fun `velvet keeps its own deck and play pause`() {
        assertEquals(PlayerDeckLayout.Aura, PlayerVisualMode.CanvasImmersive.deckLayout())
        assertTrue(PlayerVisualMode.CanvasImmersive.usesAuraPlayPause())
        assertFalse(PlayerVisualMode.Flow.usesAuraPlayPause())
        assertEquals(
            PlayerDeckLayout.Flow,
            resolvePlayerDeckLayout(PlayerVisualMode.Flow, false, false, LevyraPlayerPane.Stacked, true)
        )
    }

    @Test
    fun `velvet sources never pick up the default skin upgrade`() {
        velvetSources.forEach { path ->
            val text = source(path).readText()
            defaultOnlyPrimitives.forEach { primitive ->
                assertFalse("$path must not use $primitive", text.contains(primitive))
            }
        }
    }

    @Test
    fun `velvet still routes through its dedicated scene`() {
        val nowPlaying = source("ui/player/LevyraNowPlaying.kt").readText()
        assertTrue(
            nowPlaying.contains(
                "val velvet = !LevyraSkinManagesLayout && state.interfaceSettings.playerVisualMode == PlayerVisualMode.CanvasImmersive"
            )
        )
        assertTrue(nowPlaying.contains("AppleMusicPlayerStyleScope(velvet)"))
        assertFalse(nowPlaying.contains("FlowTrackShift"))
    }

    @Test
    fun `flow upgrade lives only in the flow deck`() {
        assertTrue(source("ui/player/PlayerDeckFlow.kt").readText().contains("FlowTrackShift"))
    }

    private fun source(path: String): File {
        val file = listOf(
            File("src/main/java/com/luc4n3x/levyra/$path"),
            File("app/src/main/java/com/luc4n3x/levyra/$path")
        ).firstOrNull { it.isFile }
        assertNotNull("missing $path", file)
        return requireNotNull(file)
    }
}
