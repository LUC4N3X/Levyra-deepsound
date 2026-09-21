package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.ReleaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseRadarPolicyTest {

    @Test
    fun existingBaselineSuppressesOldCatalogItems() {
        val existing = release("MPRE-old", "First Light")
        val known = ReleaseRadarPolicy.identityKeys(existing)

        assertTrue(ReleaseRadarPolicy.freshReleases(listOf(existing), known).isEmpty())
    }

    @Test
    fun duplicateEditionAndReuploadProduceOneFreshRelease() {
        val original = release("MPRE-original", "Afterglow")
        val deluxe = release("MPRE-deluxe", "Afterglow (Deluxe Edition)")

        val fresh = ReleaseRadarPolicy.freshReleases(listOf(original, deluxe), emptySet())

        assertEquals(listOf("MPRE-original"), fresh.map(ArtistRelease::browseId))
    }

    @Test
    fun fingerprintSurvivesCatalogIdChanges() {
        val oldEdition = release("MPRE-old", "Città")
        val reupload = release("MPRE-new", "Citta Remastered")

        val fresh = ReleaseRadarPolicy.freshReleases(
            listOf(reupload),
            ReleaseRadarPolicy.identityKeys(oldEdition)
        )

        assertTrue(fresh.isEmpty())
    }

    @Test
    fun retriesOnlyCompleteFetchFailureAndStopsAfterBoundedAttempts() {
        assertTrue(ReleaseRadarPolicy.shouldRetryFetches(successful = 0, failed = 3, runAttemptCount = 0))
        assertFalse(ReleaseRadarPolicy.shouldRetryFetches(successful = 1, failed = 2, runAttemptCount = 0))
        assertFalse(ReleaseRadarPolicy.shouldRetryFetches(successful = 0, failed = 3, runAttemptCount = 2))
    }

    private fun release(id: String, title: String) = ArtistRelease(
        browseId = id,
        title = title,
        subtitle = "Album",
        thumbnailUrl = "",
        year = "2026",
        releaseType = ReleaseType.Album
    )
}
