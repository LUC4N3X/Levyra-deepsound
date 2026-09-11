package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.AlternativeMatchVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighQualityMappingStoreTest {
    private var now = 1_000_000_000L
    private val storage = InMemoryMappingStorage()
    private val store = HighQualityMappingStore(storage, clock = { now }, maxEntries = 3, ttlMs = 10_000L)

    private fun mapping(
        id: String = "pW-kkdqr",
        fingerprint: String = "query-fp",
        verdict: AlternativeMatchVerdict = AlternativeMatchVerdict.EXACT,
        confidence: Int = 100
    ) = StoredAlternativeMapping("jiosaavn", id, fingerprint, "candidate-fp", verdict, confidence, now)

    @Test
    fun verifiedMappingRoundTrips() {
        assertTrue(store.save("track-a", mapping()))
        assertEquals("pW-kkdqr", store.load("track-a", "query-fp")?.providerTrackId)
    }

    @Test
    fun uncertainMatchesAreNeverPersisted() {
        assertFalse(store.save("track-a", mapping(verdict = AlternativeMatchVerdict.HIGH, confidence = 80)))
        assertFalse(store.save("track-b", mapping(verdict = AlternativeMatchVerdict.REJECTED, confidence = 0)))
        assertTrue(storage.values.isEmpty())
        assertTrue(store.save("track-c", mapping(verdict = AlternativeMatchVerdict.HIGH, confidence = 90)))
    }

    @Test
    fun staleMappingExpires() {
        store.save("track-a", mapping())
        now += 10_001L
        assertNull(store.load("track-a", "query-fp"))
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun changedLevyraMetadataInvalidatesMapping() {
        store.save("track-a", mapping())
        assertNull(store.load("track-a", "different-fp"))
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun storageStaysBounded() {
        listOf("a", "b", "c").forEach { key ->
            store.save(key, mapping(id = key))
            now += 1L
        }
        store.save("d", mapping(id = "d"))
        assertEquals(3, storage.values.size)
        assertNull(store.load("a", "query-fp"))
        assertNotNull(store.load("d", "query-fp"))
    }

    @Test
    fun corruptEntryIsDiscarded() {
        store.save("track-a", mapping())
        storage.values.keys.forEach { storage.values[it] = "{not json" }
        assertNull(store.load("track-a", "query-fp"))
        assertTrue(storage.values.isEmpty())
    }
}
