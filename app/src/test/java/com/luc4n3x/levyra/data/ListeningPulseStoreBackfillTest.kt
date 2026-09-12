package com.luc4n3x.levyra.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningPulseStoreBackfillTest {

    @Test
    fun lifetimeArtistIdentityDropsBlankIdsAndUsesFirstValidId() {
        val identity = lifetimeArtistIdentity(
            artist = "Geolier, Sfera Ebbasta",
            rawBrowseIds = listOf("", "  UC_GEOLIER  ", "UC_GEOLIER", "UC_SFERA")
        )

        assertEquals(listOf("UC_GEOLIER", "UC_SFERA"), identity.browseIds)
        assertEquals("id:uc_geolier", identity.key)
        assertEquals("Geolier", identity.name)
    }

    @Test
    fun lifetimeArtistIdentityUpgradesNameKeyWhenIdArrives() {
        val initial = lifetimeArtistIdentity("Geolier", emptyList())
        val upgraded = lifetimeArtistIdentity("Geolier", listOf("", "UC_GEOLIER"))

        assertEquals("geolier", initial.key)
        assertEquals("id:uc_geolier", upgraded.key)
        assertNotEquals(initial.key, upgraded.key)
    }

    @Test
    fun existingPrimaryIdStaysStableAcrossLaterMetadataUpdates() {
        val identity = lifetimeArtistIdentity(
            artist = "Geolier, Sfera Ebbasta",
            rawBrowseIds = listOf("UC_SFERA", "UC_GEOLIER"),
            preferredPrimaryId = "UC_GEOLIER"
        )

        assertEquals("id:uc_geolier", identity.key)
        assertEquals("UC_GEOLIER", identity.browseIds.first())
        assertEquals("Geolier", identity.name)
    }

    @Test
    fun backfillProcessesAllEventsBeyondFiftyThousandWithoutTruncation() = runBlocking {
        val totalRecords = 50_005
        val processedIds = mutableListOf<Long>()

        val totalProcessed = executeLifetimeBackfillPaging(
            pageSize = 400,
            fetchPage = { afterId, pageSize ->
                val start = afterId.toInt() + 1
                val end = minOf(start + pageSize - 1, totalRecords)
                if (start > totalRecords) {
                    emptyList()
                } else {
                    (start..end).map { it.toLong() }
                }
            },
            getId = { it },
            processEvent = { id ->
                if (id == 1L || id == 50_000L || id == 50_001L || id == totalRecords.toLong()) {
                    processedIds.add(id)
                }
            }
        )

        assertEquals(totalRecords, totalProcessed)
        assertTrue(processedIds.contains(1L))
        assertTrue(processedIds.contains(50_000L))
        assertTrue(processedIds.contains(50_001L))
        assertTrue(processedIds.contains(totalRecords.toLong()))
    }
}
