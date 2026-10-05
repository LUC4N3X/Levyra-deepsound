package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ExploreCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreDiscoveryCacheTest {
    @Test
    fun snapshotRoundTripPreservesProviderIdentityAndArtwork() {
        val categories = listOf(
            ExploreCategory("Workout", "opaque-workout", "Moods & moments", 0),
            ExploreCategory("Jazz", "opaque-jazz", "Genres", 1)
        )
        val raw = encodeExploreDiscoverySnapshot(
            languageCode = "it",
            categories = categories,
            artwork = mapOf(
                "opaque-workout" to "https://example.test/workout.jpg",
                "opaque-jazz" to "https://example.test/jazz.jpg",
                "orphan" to "https://example.test/orphan.jpg"
            ),
            savedAtMs = 1234L
        )

        val restored = decodeExploreDiscoverySnapshot(raw, "it")

        requireNotNull(restored)
        assertEquals(1234L, restored.savedAtMs)
        assertEquals(categories, restored.categories)
        assertEquals(
            mapOf(
                "opaque-workout" to "https://example.test/workout.jpg",
                "opaque-jazz" to "https://example.test/jazz.jpg"
            ),
            restored.artwork
        )
    }

    @Test
    fun snapshotRejectsAnotherLanguage() {
        val raw = encodeExploreDiscoverySnapshot(
            languageCode = "it",
            categories = listOf(ExploreCategory("Jazz", "opaque-jazz", "Genres", 1)),
            artwork = emptyMap(),
            savedAtMs = 1234L
        )

        assertNull(decodeExploreDiscoverySnapshot(raw, "en"))
    }

    @Test
    fun freshnessUsesBoundedTtl() {
        val snapshot = ExploreDiscoverySnapshot(
            languageCode = "it",
            savedAtMs = 1_000L,
            categories = listOf(ExploreCategory("Jazz", "opaque-jazz", "Genres", 1)),
            artwork = emptyMap()
        )

        assertTrue(snapshot.isFresh(nowMs = 1_500L, maxAgeMs = 1_000L))
        assertFalse(snapshot.isFresh(nowMs = 2_000L, maxAgeMs = 1_000L))
        assertFalse(snapshot.copy(savedAtMs = 3_000L).isFresh(nowMs = 2_000L, maxAgeMs = 1_000L))
    }
}
