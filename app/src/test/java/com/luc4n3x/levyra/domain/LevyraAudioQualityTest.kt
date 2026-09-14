package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LevyraAudioQualityTest {
    @Test
    fun defaultIsHigh() {
        assertEquals("High", LevyraAudioQuality.DEFAULT)
    }

    @Test
    fun explicitChoicesArePreservedRegardlessOfCaseAndSpacing() {
        assertEquals("High", LevyraAudioQuality.normalize("High"))
        assertEquals("Auto", LevyraAudioQuality.normalize("Auto"))
        assertEquals("Low", LevyraAudioQuality.normalize("Low"))
        assertEquals("Auto", LevyraAudioQuality.normalize(" auto "))
        assertEquals("Low", LevyraAudioQuality.normalize("LOW"))
        assertEquals("High", LevyraAudioQuality.normalize("high"))
    }

    @Test
    fun missingOrBlankValuesUseTheDefault() {
        assertEquals(LevyraAudioQuality.DEFAULT, LevyraAudioQuality.normalize(null))
        assertEquals(LevyraAudioQuality.DEFAULT, LevyraAudioQuality.normalize(""))
        assertEquals(LevyraAudioQuality.DEFAULT, LevyraAudioQuality.normalize("   "))
    }

    @Test
    fun corruptOrUnknownValuesFallBackToTheDefault() {
        assertEquals(LevyraAudioQuality.DEFAULT, LevyraAudioQuality.normalize("Medium"))
        assertEquals(LevyraAudioQuality.DEFAULT, LevyraAudioQuality.normalize("null"))
        assertEquals(LevyraAudioQuality.DEFAULT, LevyraAudioQuality.normalize("320"))
    }
}
