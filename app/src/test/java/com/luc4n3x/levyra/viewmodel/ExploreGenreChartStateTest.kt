package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.feature.discovery.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreGenreChartStateTest {

    private val hour = 60 * 60 * 1000L

    @Test
    fun freshChartIsNotReloadedButStaleOrFailedOneIs() {
        val loaded = ExploreGenreChartState(key = "it:zone:rock", tracks = listOf(track("A", "B", id = "a")), refreshedAtMs = 10 * hour)

        assertTrue(loaded.coversRequest("it:zone:rock", 11 * hour))
        assertFalse(loaded.coversRequest("it:zone:rock", 17 * hour))
        assertFalse(loaded.copy(refreshedAtMs = 0L).coversRequest("it:zone:rock", 11 * hour))
        assertFalse(loaded.coversRequest("it:zone:pop", 11 * hour))
    }

    @Test
    fun failedRefreshKeepsPreviousTracksAndTimestamp() {
        val loaded = ExploreGenreChartState(key = "k", tracks = listOf(track("A", "B", id = "a")), isLoading = true, refreshedAtMs = 5L)

        val failed = loaded.finishedWith(emptyList(), 99L)

        assertEquals(loaded.tracks, failed.tracks)
        assertEquals(5L, failed.refreshedAtMs)
        assertFalse(failed.isLoading)
    }
}
