package com.luc4n3x.levyra.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoveryBudgetContractTest {
    @Test
    fun mediaSourcesUseTheServiceRecoveryBudgetWithoutHiddenRetries() {
        val service = readServiceSource()

        assertTrue(service.contains("private object LevyraPlaybackLoadErrorHandlingPolicy"))
        assertTrue(service.contains("getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long =\n        C.TIME_UNSET"))
        assertTrue(service.contains("getMinimumLoadableRetryCount(dataType: Int): Int = 0"))
        assertTrue(service.contains("HlsMediaSource.Factory(dataSourceFactory)\n                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)"))
        assertTrue(service.contains("DashMediaSource.Factory(dataSourceFactory)\n                    .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)"))
        assertTrue(service.contains("ProgressiveMediaSource.Factory(dataSourceFactory)\n                .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)"))
    }

    @Test
    fun healthyPlaybackDoesNotRearmTheSameRecoveryBudget() {
        val service = readPlayerSource("PlaybackService.kt")
        val player = readPlayerSource("LevyraPlayer.kt")

        assertFalse(service.contains("scheduleRecoveryReset"))
        assertFalse(service.contains("recoveryResetJob"))
        assertFalse(player.contains("recoveryResetJob"))
        assertFalse(player.contains("delay(5_000L)\n                            if (connected.playbackState == Player.STATE_READY) recoveryAttempts = 0"))
        assertTrue(
            Regex("""if\s*\(\s*isTerminalPlaybackFailure\([^)]*\)\s*\)""").containsMatchIn(service)
        )
        assertTrue(
            Regex("""isTerminalPlaybackFailure\(\s*classifyPlaybackFailureReason\(message\)\s*\)""")
                .containsMatchIn(player)
        )
    }

    private fun readServiceSource(): String = readPlayerSource("PlaybackService.kt")

    private fun readPlayerSource(name: String): String =
        Files.readString(sourceFile(name)).replace("\r\n", "\n")

    private fun sourceFile(name: String): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/player/$name"),
        Path.of("src/main/java/com/luc4n3x/levyra/player/$name")
    ).firstOrNull(Files::exists) ?: error("$name not found")
}
