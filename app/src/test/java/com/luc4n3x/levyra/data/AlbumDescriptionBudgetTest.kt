package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class AlbumDescriptionBudgetTest {
    @Test
    fun providerChainSharesOneElapsedTimeBudget() {
        val deadline = TimeUnit.MILLISECONDS.toNanos(10_000L)

        assertEquals(
            7_000L,
            remainingAlbumDescriptionBudgetMillis(
                deadlineNanos = deadline,
                nowNanos = TimeUnit.MILLISECONDS.toNanos(3_000L)
            )
        )
        assertEquals(0L, remainingAlbumDescriptionBudgetMillis(deadline, deadline))
        assertEquals(0L, remainingAlbumDescriptionBudgetMillis(deadline, deadline + 1L))
    }

    @Test
    fun subMillisecondRemainderStillGetsOneMillisecondCallTimeout() {
        assertEquals(1L, remainingAlbumDescriptionBudgetMillis(2L, 1L))
    }
}
