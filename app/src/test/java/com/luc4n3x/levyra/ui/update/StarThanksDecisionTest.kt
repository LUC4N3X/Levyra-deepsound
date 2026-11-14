package com.luc4n3x.levyra.ui.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarThanksDecisionTest {
    @Test
    fun freshInstallHides() {
        assertFalse(shouldShowStarSheet(1000, 1000, false))
    }

    @Test
    fun updateShows() {
        assertTrue(shouldShowStarSheet(1000, 5000, false))
    }

    @Test
    fun alreadyShownHides() {
        assertFalse(shouldShowStarSheet(1000, 5000, true))
    }
}
