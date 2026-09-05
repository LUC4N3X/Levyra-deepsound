package com.luc4n3x.levyra.ui.player

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVideoSurfaceContractTest {
    @Test
    fun videoLayoutUsesGesturePlayerViewAndTextureSurface() {
        val layout = sequenceOf(
            Path.of("app/src/main/res/layout/levyra_video_player_view.xml"),
            Path.of("src/main/res/layout/levyra_video_player_view.xml")
        ).firstOrNull(Files::exists) ?: error("Video player layout not found")
        val xml = Files.readString(layout)

        assertTrue(xml.contains("com.luc4n3x.levyra.ui.player.LevyraGesturePlayerView"))
        assertTrue(xml.contains("app:surface_type=\"texture_view\""))
    }

    @Test
    fun mainPlayerInflatesTheGestureVideoLayout() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
        ).firstOrNull(Files::exists) ?: error("LevyraApp source not found")
        val kotlin = Files.readString(source)

        assertTrue(kotlin.contains("R.layout.levyra_video_player_view"))
    }
}
