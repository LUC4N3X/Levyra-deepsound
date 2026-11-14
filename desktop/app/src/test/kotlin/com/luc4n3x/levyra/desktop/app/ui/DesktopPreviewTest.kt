package com.luc4n3x.levyra.desktop.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.*
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.navigation.LevyraSidebar
import com.luc4n3x.levyra.desktop.app.ui.i18n.*
import com.luc4n3x.levyra.desktop.app.ui.screens.NowPlayingScreen
import com.luc4n3x.levyra.desktop.app.ui.player.PlayerBar
import com.luc4n3x.levyra.desktop.app.ui.theme.*
import com.luc4n3x.levyra.desktop.core.model.*
import com.luc4n3x.levyra.desktop.player.PlayerQueue
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalComposeUiApi::class)
class DesktopPreviewTest {
    @Test fun compactPlayerKeepsEveryActionInsideTheWindow() {
        val strings = stringsFor(AppLanguage.ENGLISH)
        val track = Track("preview", "A long recording title", "Artist", videoUrl = "", durationMs = 200_000)
        val state = PlaybackUiState(queue = PlayerQueue().replace(listOf(track), 0), streamLabel = "JioSaavn · 320 kbps")
        for (width in listOf(800, 1200)) {
            ImageComposeScene(width, 240) {
                LevyraTheme(ThemeMode.DARK) {
                    CompositionLocalProvider(LocalStrings provides strings) {
                        PlayerBar(state, MutableStateFlow(state), false, false,
                            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
                    }
                }
            }.use { scene ->
                scene.render().close()
                val roots = scene.semanticsOwners.map { it.rootSemanticsNode }
                for (label in listOf(strings.playbackClose, strings.queueTitle, strings.playbackMute,
                        strings.settingsPlayback, strings.playbackPlay, strings.playbackShuffle, strings.playbackRepeat)) {
                    val node = roots.firstNotNullOfOrNull { findDescription(it, label) }
                    assertNotNull("Missing $label at width=$width", node)
                    val bounds = node!!.boundsInRoot
                    assertTrue("Clipped $label at width=$width: $bounds", bounds.width > 0 && bounds.height > 0 &&
                        bounds.left >= 0 && bounds.right <= width && bounds.bottom <= 240)
                }
                val output = Files.createDirectories(Path.of("build", "previews"))
                scene.render().use { image ->
                    Files.write(output.resolve("player-$width.png"), image.encodeToData()!!.bytes)
                }
            }
        }
    }

    private fun findDescription(node: SemanticsNode, label: String): SemanticsNode? {
        if (node.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label) == true) return node
        return node.children.firstNotNullOfOrNull { findDescription(it, label) }
    }

    @Test fun renderWideCompactAndRtlNowPlaying() {
        assumeTrue(System.getenv("LEVYRA_RENDER_PREVIEWS") == "1")
        val track = Track("preview", "Tum Hi Ho", "Arijit Singh", "Aashiqui 2", "", durationMs = 262_000)
        val state = PlaybackUiState(queue = PlayerQueue().replace(listOf(track), 0), streamLabel = "JioSaavn · 320 kbps")
        val flow = MutableStateFlow(state)
        val actions = TrackActions(track.id, { false }, { _, _ -> }, {}, {}, {}, {})
        val output = Files.createDirectories(Path.of("build", "previews"))
        for ((width, language, theme) in listOf(
            Triple(1440, AppLanguage.ENGLISH, ThemeMode.DARK),
            Triple(960, AppLanguage.GERMAN, ThemeMode.LIGHT),
            Triple(960, AppLanguage.PERSIAN, ThemeMode.DARK)
        )) {
            val image = renderComposeScene(width, 800) {
                LevyraTheme(theme) {
                    CompositionLocalProvider(LocalStrings provides stringsFor(language),
                        LocalAccentColor provides MaterialTheme.colorScheme.primary,
                        LocalLayoutDirection provides if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                LevyraSidebar(Destination.NOW_PLAYING, true, true, {}, compact = width < 1150)
                                NowPlayingScreen(state, flow, LyricsUiState(), actions, {}, {}, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            image.use { Files.write(output.resolve("now-playing-$width-${language.tag}.png"), it.encodeToData()!!.bytes) }
        }
    }
}
