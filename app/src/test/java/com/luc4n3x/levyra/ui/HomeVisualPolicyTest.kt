package com.luc4n3x.levyra.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeVisualPolicyTest {
    @Test
    fun backdropMotionRunsOnlyWhenAnimationsEnabledAndHomeIsIdle() {
        assertTrue(shouldAnimateHomeBackdrop(animationsEnabled = true, isScrolling = false))
        assertFalse(shouldAnimateHomeBackdrop(animationsEnabled = false, isScrolling = false))
        assertFalse(shouldAnimateHomeBackdrop(animationsEnabled = true, isScrolling = true))
    }
}
