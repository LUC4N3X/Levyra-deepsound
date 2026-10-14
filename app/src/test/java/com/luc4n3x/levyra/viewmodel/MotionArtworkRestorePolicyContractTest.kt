package com.luc4n3x.levyra.viewmodel

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkRestorePolicyContractTest {
    @Test
    fun `restore applies restored wifi only policy before motion artwork restarts`() {
        val restore = readSource("viewmodel/LevyraViewModel.kt")
            .substringAfter("private suspend fun refreshAfterRestore()")
            .substringBefore("fun setLanguage(")
            .filterNot(Char::isWhitespace)
        val policyUpdate = restore.indexOf(
            "MotionArtworkNetworkPolicy.updateWifiOnly(snapshot.interfaceSettings.motionArtworkWifiOnly)"
        )
        val stateRestore = restore.indexOf("interfaceSettings=snapshot.interfaceSettings")
        val motionRestart = restore.indexOf("refreshMotionArtworkAround")

        assertTrue(policyUpdate >= 0)
        assertTrue(stateRestore > policyUpdate)
        assertTrue(motionRestart > policyUpdate)
    }

    @Test
    fun `resolved playback start realigns motion artwork with the playing identity`() {
        val start = readSource("viewmodel/LevyraViewModel.kt")
            .substringAfter("private fun startPlayback(playable: Track, request: PlaybackResolveRequest)")
            .substringBefore("private fun handlePlaybackStartSideEffects(")
            .filterNot(Char::isWhitespace)
        val stateUpdate = start.indexOf("currentTrack=playable")
        val motionRefresh = start.indexOf("refreshMotionArtworkAround(playable)")

        assertTrue(stateUpdate >= 0)
        assertTrue(motionRefresh > stateUpdate)
    }

    private fun readSource(relativePath: String): String =
        Files.readString(sourceFile(relativePath)).replace("\r\n", "\n")

    private fun sourceFile(relativePath: String): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
    ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
}
