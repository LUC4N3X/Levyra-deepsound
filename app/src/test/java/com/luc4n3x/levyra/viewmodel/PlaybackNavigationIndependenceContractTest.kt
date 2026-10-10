package com.luc4n3x.levyra.viewmodel

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackNavigationIndependenceContractTest {

    private val viewModel = readSource("viewmodel/LevyraViewModel.kt")
    private val app = readSource("ui/LevyraApp.kt")

    @Test
    fun `starting or changing playback never mutates navigation state`() {
        val playbackEntryPoints = listOf(
            "fun playArtistSong(",
            "fun playAll(",
            "fun playFrom(",
            "fun play(track: Track)",
            "fun playAlbumSong(",
            "fun playCurrentAlbum(",
            "fun next(",
            "fun previous(",
            "fun togglePlay(",
            "private fun startResolve("
        )
        playbackEntryPoints.forEach { signature ->
            val body = functionBody(viewModel, signature)
            NavigationMutations.forEach { mutation ->
                assertFalse("$signature must not call $mutation", body.contains(mutation))
            }
        }
    }

    @Test
    fun `artist page track and video taps play inside the artist page`() {
        val overlay = composableBody(app, "private fun ArtistOverlay(")

        assertTrue(overlay.contains("onClick = { onPlayFrom(shownVideos, track) }"))
        assertFalse(overlay.contains("onClick = { onPlay(track) }"))
    }

    @Test
    fun `artist page shows the shared detail now playing dock while playback is active`() {
        val overlay = composableBody(app, "private fun ArtistOverlay(")
        val dock = overlay.substringAfter("state.currentTrack?.let { current ->", missingDelimiterValue = "")

        assertTrue(dock.trimStart().startsWith("AlbumNowPlayingDock("))
        assertTrue(dock.contains("onOpenPlayer = onOpenPlayer"))
        assertTrue(dock.contains("onToggle = onTogglePlayback"))
    }

    @Test
    fun `opening the artist from now playing reveals it above the collapsed player`() {
        val body = functionBody(viewModel, "fun openArtistFromPlayer(")

        val resolve = body.indexOf("artistReferenceOf(track) ?: return")
        val restore = body.indexOf("restorePlayerReturnDetail()")
        val open = body.indexOf("openArtistReference(")
        val leavePlayer = body.indexOf("moveToTab(previousTab(LevyraTab.Player), rememberCurrent = false)")
        assertTrue(resolve >= 0)
        assertTrue(restore > resolve)
        assertTrue(open > restore)
        assertTrue(leavePlayer > open)
        val playerViewModel = readSource("viewmodel/LevyraScreenViewModels.kt")
        assertTrue(playerViewModel.contains("fun openArtist(track: Track, artistIndex: Int = 0)"))
        assertTrue(playerViewModel.contains("root.openArtistFromPlayer("))
    }

    @Test
    fun `tab behind the expanded player survives activity recreation`() {
        assertTrue(app.contains("var backgroundTab by rememberSaveable {"))
    }

    @Test
    fun `backgrounded Explore destinations cannot consume player back`() {
        val destinations = readSource("ui/ExploreDestinationScreens.kt")
        val samples = readSource("ui/ExploreSamplesScreen.kt")

        assertTrue(app.contains("backEnabled = !rootOverlayOpen && state.selectedTab == LevyraTab.Explore"))
        assertTrue(destinations.contains("BackHandler(enabled = backEnabled, onBack = onBack)"))
        assertTrue(samples.contains("BackHandler(enabled = backEnabled, onBack = onDismiss)"))
        assertFalse(destinations.contains("BackHandler(onBack = onBack)"))
        assertFalse(samples.contains("BackHandler(onBack = onDismiss)"))
    }

    private fun functionBody(source: String, signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Missing $signature", start >= 0)
        val rest = source.substring(start + signature.length)
        val end = NextMember.find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }

    private fun composableBody(source: String, signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("Missing $signature", start >= 0)
        val rest = source.substring(start + signature.length)
        val end = rest.indexOf("\n}\n").takeIf { it >= 0 } ?: rest.length
        return rest.substring(0, end)
    }

    private fun readSource(relativePath: String): String =
        Files.readString(sourceFile(relativePath)).replace("\r\n", "\n")

    private fun sourceFile(relativePath: String): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
    ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")

    private companion object {
        val NextMember = Regex("""\n {4}(?:private |internal |override )?(?:suspend )?fun """)
        val NavigationMutations = listOf(
            "closeArtist(",
            "closeAlbum(",
            "moveToTab(",
            "selectTab(",
            "openPlayerScreen(",
            "navigateBack(",
            "restoreDetailPage(",
            "detailBackStack",
            "showArtist =",
            "showAlbum =",
            "selectedTab ="
        )
    }
}
