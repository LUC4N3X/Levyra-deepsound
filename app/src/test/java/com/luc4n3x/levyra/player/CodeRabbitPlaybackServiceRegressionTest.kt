package com.luc4n3x.levyra.player

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeRabbitPlaybackServiceRegressionTest {
    @Test
    fun remotePlaybackStateUpdatesEnhancedAudioDuringTheCurrentTrack() {
        val content = playbackServiceSource()
        val collector = content.substringAfter("remotePlaybackBackend.state.collect { state ->")
            .substringBefore("}", missingDelimiterValue = "")

        assertTrue(
            "Cast connect/disconnect must update enhanced-audio bypass without waiting for a media transition",
            collector.contains("enhancedAudioProcessor.isRemotePlayback = state.connected")
        )
    }

    @Test
    fun primaryLimiterStaysEnabledWhenEnhancedAudioIsTheOnlyActiveDsp() {
        val content = playbackServiceSource()
        val limiterBlock = content.substringAfter("limiterProcessor.enabled =")
            .substringBefore("updateQueueTransitionSettings")

        assertTrue(
            "The primary true-peak limiter must cover Levyra Enhanced Audio just like the transition player",
            limiterBlock.contains("normalized.enhancedAudioEnabled")
        )
    }

    private fun playbackServiceSource(): String {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/player/PlaybackService.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/player/PlaybackService.kt")
        ).firstOrNull(Files::exists) ?: error("PlaybackService.kt not found")
        return Files.readString(source)
    }
}
