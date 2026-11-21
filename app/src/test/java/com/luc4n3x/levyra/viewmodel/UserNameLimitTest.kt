package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class UserNameLimitTest {
    @Test
    fun keepsShortNamesUntouched() {
        assertEquals("Luca", limitUserName("Luca"))
    }

    @Test
    fun keepsSupplementaryCharacterWholeAtTheLimit() {
        val emoji = String(Character.toChars(0x1F600))
        val name = "A".repeat(USER_NAME_MAX_CODE_POINTS - 1) + emoji + "B"
        assertEquals("A".repeat(USER_NAME_MAX_CODE_POINTS - 1) + emoji, limitUserName(name))
    }

    @Test
    fun dropsWholeSupplementaryCharacterPastTheLimit() {
        val emoji = String(Character.toChars(0x1F600))
        val name = "A".repeat(USER_NAME_MAX_CODE_POINTS) + emoji
        assertEquals("A".repeat(USER_NAME_MAX_CODE_POINTS), limitUserName(name))
    }
}
