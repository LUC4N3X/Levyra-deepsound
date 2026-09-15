package com.luc4n3x.levyra.feature.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMemoCacheTest {
    private var now = 1_000L
    private val clock: () -> Long = { now }

    @Test
    fun `fresh entries are returned and expire after ttl`() {
        val cache = SearchMemoCache<String>(maxEntries = 4, ttlMs = 100L, clock = clock)
        cache.put("it|adele", "results")

        assertEquals("results", cache.get("it|adele"))
        now += 100L
        assertNull(cache.get("it|adele"))
        assertEquals(0, cache.size)
    }

    @Test
    fun `least recently used entry is evicted first`() {
        val cache = SearchMemoCache<String>(maxEntries = 2, ttlMs = 10_000L, clock = clock)
        cache.put("a", "A")
        cache.put("b", "B")
        cache.get("a")
        cache.put("c", "C")

        assertNull(cache.get("b"))
        assertEquals("A", cache.get("a"))
        assertEquals("C", cache.get("c"))
    }

    @Test
    fun `size never grows beyond the bound`() {
        val cache = SearchMemoCache<Int>(maxEntries = 8, ttlMs = 10_000L, clock = clock)
        repeat(100) { index -> cache.put("key-$index", index) }

        assertTrue(cache.size <= 8)
    }

    @Test
    fun `longest fresh proper prefix wins`() {
        val cache = SearchMemoCache<String>(maxEntries = 8, ttlMs = 100L, clock = clock)
        cache.put("it|g", "g")
        cache.put("it|geo", "geo")
        cache.put("it|geolx", "geolx")
        cache.put("it|geoli", "exact")

        assertEquals("geo", cache.longestPrefix("it|geoli"))
    }

    @Test
    fun `expired prefixes are ignored`() {
        val cache = SearchMemoCache<String>(maxEntries = 8, ttlMs = 100L, clock = clock)
        cache.put("it|ge", "old")
        now += 60L
        cache.put("it|g", "fresh")
        now += 50L

        assertEquals("fresh", cache.longestPrefix("it|geolier"))
    }
}
