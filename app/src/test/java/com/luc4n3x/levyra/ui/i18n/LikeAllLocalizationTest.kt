package com.luc4n3x.levyra.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LikeAllLocalizationTest {

    @Test
    fun everyLanguageResolvesLikeAllCopy() {
        LevyraStrings.all().forEach { strings ->
            val copy = strings.likeAllCopy()
            listOf(copy.action, copy.alreadyLiked, copy.failed).forEach { text ->
                assertFalse(strings.code, text.isBlank())
            }
            assertTrue(strings.code, copy.liked(7).contains("7"))
            assertFalse(strings.code, copy.liked(7).contains("{count}"))
        }
    }

    @Test
    fun italianUsesMiPiaceWording() {
        val copy = LevyraStrings.forCode("it").likeAllCopy()

        assertEquals("Metti Mi piace a tutti", copy.action)
        assertEquals("Mi piace aggiunto a 3 brani", copy.liked(3))
    }
}
