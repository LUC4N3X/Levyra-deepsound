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
        assertEquals("pW-kkdqr", store.load("track-a", "jiosaavn", "query-fp")?.providerTrackId)
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
        assertNull(store.load("track-a", "jiosaavn", "query-fp"))
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun changedLevyraMetadataInvalidatesMapping() {
        store.save("track-a", mapping())
        assertNull(store.load("track-a", "jiosaavn", "different-fp"))
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
        assertNull(store.load("a", "jiosaavn", "query-fp"))
        assertNotNull(store.load("d", "jiosaavn", "query-fp"))
    }

    @Test
    fun corruptEntryIsDiscarded() {
        store.save("track-a", mapping())
        storage.values.keys.forEach { storage.values[it] = "{not json" }
        assertNull(store.load("track-a", "jiosaavn", "query-fp"))
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun providersKeepIndependentMappingsForTheSameTrack() {
        store.save("track-a", mapping(id = "saavn-1"))
        store.save("track-a", mapping(id = "other-1").copy(providerId = "other"))
        assertEquals("saavn-1", store.load("track-a", "jiosaavn", "query-fp")?.providerTrackId)
        assertEquals("other-1", store.load("track-a", "other", "query-fp")?.providerTrackId)
        store.remove("track-a", "other")
        assertNull(store.load("track-a", "other", "query-fp"))
        assertEquals("saavn-1", store.load("track-a", "jiosaavn", "query-fp")?.providerTrackId)
    }

    @Test
    fun legacySingleProviderMappingIsMigratedOnlyForItsProvider() {
        val legacyKey = "hq-v1:" + java.security.MessageDigest.getInstance("SHA-256")
            .digest("track-a".toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(40)
        storage.values[legacyKey] = org.json.JSONObject()
            .put("schema", 1)
            .put("providerId", "jiosaavn")
            .put("providerTrackId", "legacy-id")
            .put("queryFingerprint", "query-fp")
            .put("candidateFingerprint", "candidate-fp")
            .put("verdict", "EXACT")
            .put("confidence", 100)
            .put("storedAtMs", now)
            .toString()
        assertNull(store.load("track-a", "other", "query-fp"))
        assertTrue(storage.values.containsKey(legacyKey))
        assertEquals("legacy-id", store.load("track-a", "jiosaavn", "query-fp")?.providerTrackId)
        assertFalse(storage.values.containsKey(legacyKey))
        assertEquals("legacy-id", store.load("track-a", "jiosaavn", "query-fp")?.providerTrackId)
    }
}
