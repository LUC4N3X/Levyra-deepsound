package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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

    @Test
    fun `track correction combines with the active latency profile without double application`() {
        val profiles = LyricsLatencyProfiles(globalOffsetMs = 200L)
            .withDeviceOffset("buds", 350L)
            .withTrackOffset("song-a", 18_000L)

        assertEquals(18_200L, profiles.effectiveOffsetMs("song-a", routeKey = null, bluetooth = false))
        assertEquals(18_350L, profiles.effectiveOffsetMs("song-a", routeKey = "buds", bluetooth = true))
        assertEquals(350L, profiles.effectiveOffsetMs("song-b", routeKey = "buds", bluetooth = true))
        assertEquals(200L, profiles.effectiveOffsetMs(null, routeKey = null, bluetooth = false))
    }

    @Test
    fun `track correction is clamped to thirty seconds while latency stays at five`() {
        val profiles = LyricsLatencyProfiles()
            .withTrackOffset("late", 99_000L)
            .withTrackOffset("early", -99_000L)
            .withGlobalOffset(30_000L)

        assertEquals(MAX_LYRICS_TRACK_OFFSET_MS, profiles.trackOffsetMs("late"))
        assertEquals(-MAX_LYRICS_TRACK_OFFSET_MS, profiles.trackOffsetMs("early"))
        assertEquals(MAX_LYRICS_OFFSET_MS, profiles.globalOffsetMs)
    }

    @Test
    fun `resetting a track removes only that track and keeps latency profiles`() {
        val profiles = LyricsLatencyProfiles(globalOffsetMs = 100L)
            .withDeviceOffset("buds", 300L)
            .withTrackOffset("song-a", 15_000L)
            .withTrackOffset("song-b", -2_500L)
            .withTrackOffset("song-a", 0L)

        assertFalse(profiles.trackOffsetsMs.containsKey("song-a"))
        assertEquals(-2_500L, profiles.trackOffsetMs("song-b"))
        assertEquals(100L, profiles.globalOffsetMs)
        assertEquals(300L, profiles.resolve("buds", bluetooth = true))
    }

    @Test
    fun `unchanged or blank track updates keep the same instance`() {
        val profiles = LyricsLatencyProfiles().withTrackOffset("song", 1_500L)

        assertSame(profiles, profiles.withTrackOffset("song", 1_500L))
        assertSame(profiles, profiles.withTrackOffset("other", 0L))
        assertSame(profiles, profiles.withTrackOffset("   ", 4_000L))
    }

    @Test
    fun `consecutive increments on the latest snapshot are never lost`() {
        var profiles = LyricsLatencyProfiles()
        repeat(25) { profiles = profiles.withTrackOffset("song", profiles.trackOffsetMs("song") + 500L) }
        repeat(3) { profiles = profiles.withTrackOffset("song", profiles.trackOffsetMs("song") - 500L) }

        assertEquals(11_000L, profiles.trackOffsetMs("song"))
        repeat(200) { profiles = profiles.withTrackOffset("song", profiles.trackOffsetMs("song") + 500L) }
        assertEquals(MAX_LYRICS_TRACK_OFFSET_MS, profiles.trackOffsetMs("song"))
    }

    @Test
    fun `track corrections survive the persisted and backup round trip`() {
        val profiles = LyricsLatencyProfiles(globalOffsetMs = -400L)
            .withDeviceOffset("buds", 250L)
            .withTrackOffset("song-a", 22_500L)
            .withTrackOffset("song-b", -30_000L)

        val persisted = LyricsLatencyProfiles.decode(profiles.encode())
        val backup = LyricsLatencyProfiles.decode(JSONObject(profiles.encode()).toString())

        assertEquals(profiles, persisted)
        assertEquals(profiles, backup)
        assertEquals(listOf("song-a", "song-b"), persisted.trackOffsetsMs.keys.toList())
    }

    @Test
    fun `profiles saved before track corrections still decode unchanged`() {
        val legacy = """{"global":1500,"devices":[{"key":"buds","offset":-700}]}"""
        val restored = LyricsLatencyProfiles.decode(legacy)

        assertEquals(1_500L, restored.globalOffsetMs)
        assertEquals(-700L, restored.resolve("buds", bluetooth = true))
        assertTrue(restored.trackOffsetsMs.isEmpty())
        assertEquals(0L, restored.trackOffsetMs("anything"))
    }

    @Test
    fun `malformed track entries are skipped and storage is bounded`() {
        val raw = JSONObject()
            .put("global", 0)
            .put(
                "tracks",
                org.json.JSONArray().apply {
                    put(JSONObject().put("key", "").put("offset", 1_000))
                    put("not-an-object")
                    put(JSONObject().put("key", "zero").put("offset", 0))
                    put(JSONObject().put("key", "huge").put("offset", 120_000))
                }
            )
            .toString()
        val restored = LyricsLatencyProfiles.decode(raw)

        assertEquals(mapOf("huge" to MAX_LYRICS_TRACK_OFFSET_MS), restored.trackOffsetsMs)

        var profiles = LyricsLatencyProfiles()
        repeat(MAX_LYRICS_TRACK_PROFILES + 20) { index -> profiles = profiles.withTrackOffset("t$index", 500L) }
        val bounded = LyricsLatencyProfiles.decode(profiles.encode())

        assertEquals(MAX_LYRICS_TRACK_PROFILES, bounded.trackOffsetsMs.size)
        assertFalse(bounded.trackOffsetsMs.containsKey("t0"))
        assertEquals(500L, bounded.trackOffsetMs("t${MAX_LYRICS_TRACK_PROFILES + 19}"))
    }

    @Test
    fun `resetting a bluetooth delay lands on zero even when the global offset is set`() {
        val profiles = LyricsLatencyProfiles(globalOffsetMs = 2_000L).withDeviceOffset("buds", 400L)
        val current = profiles.resolve("buds", bluetooth = true)

        val reset = profiles.withLatencyOffset("buds", bluetooth = true, offsetMs = current - current)

        assertEquals(0L, reset.resolve("buds", bluetooth = true))
        assertEquals(2_000L, reset.resolve(null, bluetooth = false))
        assertEquals(2_000L, profiles.withoutDevice("buds").resolve("buds", bluetooth = true))
    }
}
