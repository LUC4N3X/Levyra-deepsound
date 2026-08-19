package com.luc4n3x.levyra.desktop.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioOutputDeviceTest {
    @Test
    fun sanitizeDropsBlankIdentifiersAndDuplicates() {
        val sanitized = AudioOutputDevice.sanitize(
            listOf(
                AudioOutputDevice(id = "  ", label = "Ghost"),
                AudioOutputDevice(id = "{0.0.0}", label = " Speakers "),
                AudioOutputDevice(id = "{0.0.0}", label = "Speakers duplicate"),
                AudioOutputDevice(id = " {0.0.1} ", label = "")
            )
        )

        assertEquals(
            listOf(
                AudioOutputDevice(id = "{0.0.0}", label = "Speakers"),
                AudioOutputDevice(id = "{0.0.1}", label = "{0.0.1}")
            ),
            sanitized
        )
    }

    @Test
    fun systemDefaultIdentifierIsEmpty() {
        assertEquals("", AudioOutputDevice.SYSTEM_DEFAULT_ID)
    }
}
