package com.luc4n3x.levyra.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningPulseStoreBackfillTest {

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
