package com.luc4n3x.levyra.desktop.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioOutputDeviceTest {
    @Test
    fun sanitizeDropsBlankIdentifiersAndDuplicates() {
        val first = AudioOutputDevice.create("mmdevice", "{0.0.0}", " Speakers ")
        val second = AudioOutputDevice.create("mmdevice", "{0.0.1}", "")
        val sanitized = AudioOutputDevice.sanitize(
            listOf(
                AudioOutputDevice(id = "  ", label = "Ghost", deviceId = ""),
                first,
                first.copy(label = "Speakers duplicate"),
                second
            )
        )

        assertEquals(
            listOf(
                first.copy(label = "Speakers"),
                second.copy(label = "{0.0.1}")
            ),
            sanitized
        )
    }

    @Test
    fun persistedIdRoundTripsOutputModuleAndDevice() {
        val device = AudioOutputDevice.create("mmdevice", "{0.0.0}", "Speakers")
        val restored = AudioOutputDevice.fromPersistedId(device.id)

        assertEquals("mmdevice", restored.outputName)
        assertEquals("{0.0.0}", restored.deviceId)
        assertEquals(device.id, restored.id)
    }

    @Test
    fun legacyRawDeviceIdRemainsUsable() {
        val restored = AudioOutputDevice.fromPersistedId("{0.0.0}")

        assertEquals("", restored.outputName)
        assertEquals("{0.0.0}", restored.deviceId)
    }

    @Test
    fun systemDefaultIdentifierIsEmpty() {
        assertEquals("", AudioOutputDevice.SYSTEM_DEFAULT_ID)
    }
}
