package com.luc4n3x.levyra.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkLayerTest {

    @Test
    fun livingArtworkRunsOnlyWhenEveryGateAllowsIt() {
        assertTrue(
            livingArtworkActive(
                enabled = true,
                lifecycleActive = true,
                localAllowed = true,
                isPlaying = true,
                realCanvasReady = false
            )
        )
    }

    @Test
    fun livingArtworkStopsWhenAnyGateCloses() {
        val blockedStates = listOf(
            booleanArrayOf(false, true, true, true, false),
            booleanArrayOf(true, false, true, true, false),
            booleanArrayOf(true, true, false, true, false),
            booleanArrayOf(true, true, true, false, false),
            booleanArrayOf(true, true, true, true, true)
        )

        blockedStates.forEach { state ->
            assertFalse(
                livingArtworkActive(
                    enabled = state[0],
                    lifecycleActive = state[1],
                    localAllowed = state[2],
                    isPlaying = state[3],
                    realCanvasReady = state[4]
                )
            )
        }
    }
}
