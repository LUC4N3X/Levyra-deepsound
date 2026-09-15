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

    @Test
    fun `rapid consecutive updates to different devices preserve all entries`() {
        val initial = LyricsLatencyProfiles(globalOffsetMs = 50L)
        val update1 = initial.withDeviceOffset("buds", 150L)
        val update2 = update1.withDeviceOffset("car", 300L)

        assertEquals(150L, update2.resolve("buds", bluetooth = true))
        assertEquals(300L, update2.resolve("car", bluetooth = true))
        assertEquals(50L, update2.resolve("other", bluetooth = true))
        assertEquals(2, update2.deviceOffsetsMs.size)
    }

    @Test
    fun `atomic update sequence preserves both device offset and global offset changes`() {
        var state = LyricsLatencyProfiles(globalOffsetMs = 0L)
        fun save(routeKey: String?, bluetooth: Boolean, offsetMs: Long) {
            state = if (bluetooth && !routeKey.isNullOrBlank()) {
                state.withDeviceOffset(routeKey, offsetMs)
            } else {
                state.withGlobalOffset(offsetMs)
            }
        }

        save("buds", true, 120L)
        save("car", true, 250L)
        save(null, false, 80L)

        assertEquals(80L, state.globalOffsetMs)
        assertEquals(120L, state.resolve("buds", bluetooth = true))
        assertEquals(250L, state.resolve("car", bluetooth = true))
        assertEquals(80L, state.resolve("unknown", bluetooth = true))
    }
}
