package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExpiringCachePolicyTest {
    @Test
    fun `persisted cache parser rejects malformed and non string entries`() {
        assertNull(parsePersistedCacheEntry(42L))
        assertNull(parsePersistedCacheEntry("{not-json"))
        assertNotNull(parsePersistedCacheEntry("""{"expiresAt": 100}"""))
    }

    @Test
    fun `expired entries provide capacity before live entries are evicted`() {
        val entries = mapOf(
            "expired" to 10L,
            "older-live" to 30L,
            "newer-live" to 40L
        )

        val removed = expiringCacheKeysToRemove(
            entries = entries,
            nowMs = 20L,
            maxEntries = 3,
            incomingKey = "incoming"
        )

        assertEquals(setOf("expired"), removed)
    }

    @Test
    fun `earliest expiry is evicted when a new entry reaches capacity`() {
        val entries = mapOf(
            "first" to 30L,
            "second" to 40L,
            "third" to 50L
        )

        val removed = expiringCacheKeysToRemove(
            entries = entries,
            nowMs = 20L,
            maxEntries = 3,
            incomingKey = "incoming"
        )

        assertEquals(setOf("first"), removed)
    }

    @Test
    fun `updating a live entry does not evict another entry`() {
        val entries = mapOf(
            "first" to 30L,
            "second" to 40L,
            "third" to 50L
        )

        val removed = expiringCacheKeysToRemove(
            entries = entries,
            nowMs = 20L,
            maxEntries = 3,
            incomingKey = "second"
        )

        assertEquals(emptySet<String>(), removed)
    }

    @Test
    fun `restored entries are reduced to the configured bound`() {
        val entries = mapOf(
            "first" to 30L,
            "second" to 40L,
            "third" to 50L,
            "fourth" to 60L
        )

        val removed = expiringCacheKeysToRemove(
            entries = entries,
            nowMs = 20L,
            maxEntries = 2
        )

        assertEquals(setOf("first", "second"), removed)
    }
}
