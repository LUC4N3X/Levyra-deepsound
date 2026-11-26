package com.luc4n3x.levyra.desktop.app.ui

import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import com.luc4n3x.levyra.desktop.app.state.*
import com.luc4n3x.levyra.desktop.app.ui.components.TrackActions
import com.luc4n3x.levyra.desktop.app.ui.components.navigation.LevyraSidebar
import com.luc4n3x.levyra.desktop.app.ui.i18n.*
import com.luc4n3x.levyra.desktop.app.ui.screens.NowPlayingScreen
import com.luc4n3x.levyra.desktop.app.ui.player.PlayerBar
import com.luc4n3x.levyra.desktop.app.ui.theme.*
import com.luc4n3x.levyra.desktop.core.model.*
import com.luc4n3x.levyra.desktop.player.PlayerQueue
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
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
                            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                            settings = DesktopSettings(), onUpdateSettings = {})
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

    @Test fun updateNoticePreservesDraftAndMirrorsItsBackButton() {
        val release = DesktopRelease(
            version = "3.2.5",
            name = "Levyra 3.2.5",
            notes = "",
            installer = DesktopReleaseAsset("Levyra.msi", "", 125_000_000L),
            checksum = DesktopReleaseAsset("Levyra.msi.sha256", "", 64L)
        )
        val language = AppLanguage.PERSIAN
        val later = LevyraStrings.forCode(language.tag).later
        var update by mutableStateOf(DesktopUpdateUiState())
        var draftDisposals = 0
        val width = 800
        ImageComposeScene(width, 540) {
            DesktopUpdatePresentation(update, language, ThemeMode.DARK, {}, { update = DesktopUpdateUiState() }) {
                var name by remember { mutableStateOf("") }
                DisposableEffect(Unit) { onDispose { draftDisposals++ } }
                BasicTextField(name, onValueChange = { name = it })
            }
        }.use { scene ->
            scene.render().close()
            val textField = scene.semanticsOwners.map { it.rootSemanticsNode }
                .firstNotNullOfOrNull(::findEditable)
            assertNotNull(textField)
            assertTrue(textField!!.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("Playlist draft")))

            update = DesktopUpdateUiState(phase = DesktopUpdatePhase.AVAILABLE, release = release)
            scene.render().close()
            val back = scene.semanticsOwners.map { it.rootSemanticsNode }
                .firstNotNullOfOrNull { findDescription(it, later) }
            assertNotNull("Missing the update back button", back)
            assertTrue("Back button should be on the right in Persian", back!!.boundsInRoot.left > width / 2)
            assertEquals(0, draftDisposals)

            assertTrue(back.config[SemanticsActions.OnClick].action!!.invoke())
            scene.render().close()
            val restored = scene.semanticsOwners.map { it.rootSemanticsNode }
                .firstNotNullOfOrNull(::findEditable)
            assertNotNull(restored)
            assertEquals("Playlist draft", restored!!.config[SemanticsProperties.EditableText].text)
            assertEquals(0, draftDisposals)
        }
    }

    private fun findEditable(node: SemanticsNode): SemanticsNode? =
        if (node.config.getOrNull(SemanticsActions.SetText) != null) node
        else node.children.firstNotNullOfOrNull(::findEditable)

    @Test fun renderUpdateReleaseAndDownloadProgress() {
        assumeTrue(System.getenv("LEVYRA_RENDER_PREVIEWS") == "1")
        val release = DesktopRelease(
            version = "3.2.5",
            name = "Levyra 3.2.5",
            notes = "<!-- levyra-highlights {\"en\":[\"Explore more curated playlists and updated artist pages on Desktop.\"]} -->",
            installer = DesktopReleaseAsset("Levyra.msi", "", 125_000_000L),
            checksum = DesktopReleaseAsset("Levyra.msi.sha256", "", 64L),
            publishedAtEpochMs = 1_795_651_200_000L
        )
        val available = DesktopUpdateUiState(phase = DesktopUpdatePhase.AVAILABLE, release = release)
        val downloading = available.copy(
            phase = DesktopUpdatePhase.DOWNLOADING,
            totalBytes = release.installer.size,
            bytesDownloaded = 42_000_000L
        )
        val output = Files.createDirectories(Path.of("build", "previews"))
        for ((width, height) in listOf(1280 to 800, 740 to 510)) {
            for (state in listOf(available, downloading)) {
                renderComposeScene(width, height) {
                    LevyraTheme(ThemeMode.DARK) {
                        DesktopUpdateScreen(state, AppLanguage.ENGLISH, {}, {}, animated = false)
                    }
                }.use { image ->
                    Files.write(output.resolve("update-${state.phase.name.lowercase()}-$width.png"), image.encodeToData()!!.bytes)
                }
            }
        }
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
