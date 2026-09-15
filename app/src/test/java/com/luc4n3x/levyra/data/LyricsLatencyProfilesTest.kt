package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LyricsLatencyProfilesTest {
    @Test
    fun `bluetooth profile overrides the global offset only for its device`() {
        val profiles = LyricsLatencyProfiles(globalOffsetMs = 90L)
            .withDeviceOffset("buds", 180L)
            .withDeviceOffset("car", 320L)

        assertEquals(180L, profiles.resolve("buds", bluetooth = true))
        assertEquals(320L, profiles.resolve("car", bluetooth = true))
        assertEquals(90L, profiles.resolve("speaker", bluetooth = true))
        assertEquals(90L, profiles.resolve("buds", bluetooth = false))
    }

    @Test
    fun `removing a device profile returns to the global offset`() {
        val profiles = LyricsLatencyProfiles(globalOffsetMs = 75L)
            .withDeviceOffset("buds", 200L)
            .withoutDevice("buds")

        assertEquals(75L, profiles.resolve("buds", bluetooth = true))
    }

    @Test
    fun `storage is bounded clamped and round trips`() {
        var profiles = LyricsLatencyProfiles().withGlobalOffset(99_000L)
        repeat(40) { index -> profiles = profiles.withDeviceOffset("device-$index", -99_000L) }
        val restored = LyricsLatencyProfiles.decode(profiles.encode())

        assertEquals(MAX_LYRICS_OFFSET_MS, restored.globalOffsetMs)
        assertEquals(32, restored.deviceOffsetsMs.size)
        assertFalse(restored.deviceOffsetsMs.containsKey("device-0"))
        assertEquals(-MAX_LYRICS_OFFSET_MS, restored.deviceOffsetsMs["device-39"])
    }
}
