package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.feature.radio.MemoryRadioPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackLoudnessMemoryTest {
    @Test
    fun rememberedLoudnessSurvivesReload() {
        val preferences = MemoryRadioPreferences()
        TrackLoudnessMemory(preferences).remember("id|FLAC", -9.5f, measuredBlocks = 1_500)

        val reloaded = TrackLoudnessMemory(preferences).apply { load() }

        assertEquals(-9.5f, reloaded.loudnessLufs("id|FLAC")!!, 0f)
        assertNull(reloaded.loudnessLufs("id|YouTube"))
    }

    @Test
    fun partialListenDoesNotReplaceFullerMeasurement() {
        val memory = TrackLoudnessMemory(MemoryRadioPreferences())
        memory.remember("id|FLAC", -9f, measuredBlocks = 2_000)

        memory.remember("id|FLAC", -20f, measuredBlocks = 350)
        assertEquals(-9f, memory.loudnessLufs("id|FLAC")!!, 0f)

        memory.remember("id|FLAC", -8f, measuredBlocks = 2_100)
        assertEquals(-8f, memory.loudnessLufs("id|FLAC")!!, 0f)
    }

    @Test
    fun implausibleMeasurementsAreIgnored() {
        val memory = TrackLoudnessMemory(MemoryRadioPreferences())

        memory.remember("silent|FLAC", -90f, measuredBlocks = 500)
        memory.remember("broken|FLAC", Float.NaN, measuredBlocks = 500)

        assertNull(memory.loudnessLufs("silent|FLAC"))
        assertNull(memory.loudnessLufs("broken|FLAC"))
    }

    @Test
    fun oldestEntriesAreTrimmedPastCapacity() {
        val preferences = MemoryRadioPreferences()
        val memory = TrackLoudnessMemory(preferences)

        repeat(TrackLoudnessMemory.CAPACITY + 1) { index ->
            memory.remember("track$index|FLAC", -10f, measuredBlocks = 500, nowMs = index.toLong())
        }

        assertNull(memory.loudnessLufs("track0|FLAC"))
        assertEquals(-10f, memory.loudnessLufs("track${TrackLoudnessMemory.CAPACITY}|FLAC")!!, 0f)
        assertEquals(TrackLoudnessMemory.TRIMMED_SIZE, preferences.all.size)
    }

    @Test
    fun keyNeedsMediaIdAndSeparatesSources() {
        assertNull(TrackLoudnessMemory.key("", "FLAC"))
        assertEquals("id|FLAC", TrackLoudnessMemory.key("id", "FLAC"))
        assertEquals("id|", TrackLoudnessMemory.key("id", null))
    }
}
