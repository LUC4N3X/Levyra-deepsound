package com.luc4n3x.levyra.player.waveseek

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WaveSeekStorePolicyTest {
    @Test
    fun `storage identity includes media id and exact duration`() {
        val first = waveSeekStorageKey("video123", 180_000L)
        val same = waveSeekStorageKey("video123", 180_000L)
        val subSecondDifference = waveSeekStorageKey("video123", 180_999L)
        val differentDuration = waveSeekStorageKey("video123", 181_000L)
        val differentTrack = waveSeekStorageKey("video456", 180_000L)

        assertEquals(first, same)
        assertNotEquals(first, subSecondDifference)
        assertNotEquals(first, differentDuration)
        assertNotEquals(first, differentTrack)
        assertNull(waveSeekStorageKey("", 180_000L))
        assertNull(waveSeekStorageKey("video123", 0L))
    }

    @Test
    fun `recent index moves touched key to the end and prunes oldest`() {
        val updated = waveSeekUpdatedIndex(
            existing = listOf("a", "b", "c"),
            touched = "b",
            limit = 3
        )
        assertEquals(listOf("a", "c", "b"), updated.kept)
        assertEquals(emptyList<String>(), updated.evicted)

        val pruned = waveSeekUpdatedIndex(
            existing = updated.kept,
            touched = "d",
            limit = 3
        )
        assertEquals(listOf("c", "b", "d"), pruned.kept)
        assertEquals(listOf("a"), pruned.evicted)
    }
}
