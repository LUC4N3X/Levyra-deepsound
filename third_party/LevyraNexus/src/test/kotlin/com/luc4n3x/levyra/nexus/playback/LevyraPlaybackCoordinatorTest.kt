package com.luc4n3x.levyra.nexus.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPlaybackCoordinatorTest {
    private val coordinator = LevyraPlaybackCoordinator()

    @Test
    fun naturalEndIsAlwaysCompletionEvenWithStalePosition() {
        val signal = coordinator.classify(
            LevyraPlaybackSnapshot("track", positionMs = 70_000L, durationMs = 100_000L, ended = true),
            LevyraTransitionReason.AUTO_ADVANCE
        )
        assertTrue(signal is LevyraPlaybackSignal.Completed)
        assertEquals(1.0, (signal as LevyraPlaybackSignal.Completed).completionRatio, 0.0)
    }

    @Test
    fun manualEarlyTransitionIsSkip() {
        val signal = coordinator.classify(
            LevyraPlaybackSnapshot("track", positionMs = 10_000L, durationMs = 100_000L),
            LevyraTransitionReason.USER_NEXT
        )
        assertTrue(signal is LevyraPlaybackSignal.Skipped)
    }

    @Test
    fun incompleteAutomaticTransitionIsNeutral() {
        val signal = coordinator.classify(
            LevyraPlaybackSnapshot("track", positionMs = 20_000L, durationMs = 100_000L),
            LevyraTransitionReason.AUTO_ADVANCE
        )
        assertTrue(signal is LevyraPlaybackSignal.Transitioned)
    }

    @Test
    fun repeatIsNotLearnedAsSkip() {
        val signal = coordinator.classify(
            LevyraPlaybackSnapshot("track", positionMs = 100_000L, durationMs = 100_000L),
            LevyraTransitionReason.REPEAT
        )
        assertTrue(signal is LevyraPlaybackSignal.Replayed)
    }
}
