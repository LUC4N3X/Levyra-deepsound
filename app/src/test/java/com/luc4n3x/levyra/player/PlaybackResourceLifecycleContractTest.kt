package com.luc4n3x.levyra.player

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResourceLifecycleContractTest {
    @Test
    fun `transition player excludes video renderers and has one release path`() {
        val service = readSource("player/PlaybackService.kt")
        val transitionStart = service.indexOf("private fun buildTransitionPlayer")
        val transitionEnd = service.indexOf("private suspend fun fadePlayers", transitionStart)
        val transition = service.substring(transitionStart, transitionEnd)

        assertTrue(transition.contains("override fun buildVideoRenderers"))
        assertTrue(service.contains("private fun releaseTransitionPlayer"))
        assertTrue(service.contains("releaseTransitionPlayer(secondary)"))
        assertTrue(service.contains("releaseTransitionPlayer()"))
    }

    @Test
    fun `native heap sampling stays off main and recovery reuses the sample`() {
        val service = readSource("player/PlaybackService.kt")
        val recovery = service
            .substringAfter("private fun recyclePlaybackPipeline")
            .substringBefore("private fun cancelQueueTransition")

        assertTrue(
            service.contains(
                "withContext(Dispatchers.Default) { Debug.getNativeHeapAllocatedSize() }"
            )
        )
        assertTrue(service.contains("recyclePlaybackPipeline(player, threshold, nativeAllocatedBytes)"))
        assertFalse(recovery.contains("Debug.getNativeHeapAllocatedSize()"))
    }

    @Test
    fun `primary audio processors are owned by the service instance`() {
        val service = readSource("player/PlaybackService.kt")
        val viewModel = readSource("viewmodel/LevyraViewModel.kt")

        assertTrue(service.contains("private val normalizationProcessor = NormalizationAudioProcessor()"))
        assertTrue(service.contains("private fun applyPremiumAudioSettingsInternal"))
        assertTrue(service.contains("private var pendingAudioSettings: LevyraAudioSettings? = null"))
        assertTrue(service.contains("activateServiceAndApplyPendingAudioSettings()"))
        assertFalse(viewModel.contains("PlaybackService.normalizationProcessor"))
    }

    private fun readSource(relativePath: String): String =
        Files.readString(sourceFile(relativePath)).replace("\r\n", "\n")

    private fun sourceFile(relativePath: String): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
    ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
}
