package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPredictiveBackTest {

    @Test
    fun zeroProgressKeepsTheStartingExpansion() {
        assertEquals(1f, playerPredictiveBackExpansion(1f, 0f), 0.0001f)
        assertEquals(0.5f, playerPredictiveBackExpansion(0.5f, 0f), 0.0001f)
    }

    @Test
    fun fullProgressRecedesButNeverCollapsesOnItsOwn() {
        val value = playerPredictiveBackExpansion(1f, 1f)
        assertTrue(value > 0f)
        assertTrue(value < 1f)
    }

    @Test
    fun progressIsMonotonicAndClamped() {
        var previous = playerPredictiveBackExpansion(1f, 0f)
        for (step in 1..10) {
            val current = playerPredictiveBackExpansion(1f, step / 10f)
            assertTrue(current <= previous)
            assertTrue(current in 0f..1f)
            previous = current
        }
    }

    @Test
    fun nonFiniteInputsFallBackSafely() {
        assertEquals(1f, playerPredictiveBackExpansion(Float.NaN, 0f), 0.0001f)
        assertEquals(1f, playerPredictiveBackExpansion(1f, Float.NaN), 0.0001f)
    }
}
