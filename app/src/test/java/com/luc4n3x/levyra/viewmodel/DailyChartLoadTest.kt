package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.apple.AppleCityChart
import com.luc4n3x.levyra.data.apple.AppleDailyChart
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DailyChartLoadTest {
    private val top100 = listOf(
        AppleDailyChart("pl.global", "Global", true),
        AppleDailyChart("pl.it", "Italia", false),
        AppleDailyChart("pl.fr", "France", false)
    )
    private val cities = listOf(AppleCityChart("pl.aaa", "Top 25", "Milano"), AppleCityChart("pl.bbb", "Top 25", "Roma"))

    private fun track(id: String) = Track(id, id, "Artist", "Album", 1L, "", "", "", "", "apple", emptySet(), 0, 0, 0, 0, 0, 0)

    @Test
    fun cityFailureKeepsTop100() {
        val state = DailyTop100UiState(key = "it:it", charts = top100)
        val merged = mergeCharts(state, state.key, top100 = null, cities = emptyList())
        assertEquals(top100, merged.charts)
        assertEquals(emptyList<AppleCityChart>(), merged.cities)
    }

    @Test
    fun top100FailureKeepsCities() {
        val state = DailyTop100UiState(key = "it:it", cities = cities)
        val merged = mergeCharts(state, state.key, top100 = emptyList(), cities = null)
        assertEquals(cities, merged.cities)
        assertEquals(emptyList<AppleDailyChart>(), merged.charts)
    }

    @Test
    fun staleKeyIgnored() {
        val state = DailyTop100UiState(key = "it:it", charts = top100, cities = cities)
        assertSame(state, mergeCharts(state, "fr:fr", top100 = emptyList(), cities = emptyList()))
    }

    @Test
    fun emptyCitiesStillReload() {
        assertFalse(chartsSettled(DailyTop100UiState(key = "it:it", charts = top100), "it:it"))
        assertTrue(chartsSettled(DailyTop100UiState(key = "it:it", charts = top100, cities = cities), "it:it"))
        assertTrue(chartsSettled(DailyTop100UiState(key = "it:it", loading = true), "it:it"))
        assertFalse(chartsSettled(DailyTop100UiState(key = "fr:fr", charts = top100, cities = cities), "it:it"))
    }

    @Test
    fun staleOpenDoesNotPublish() {
        val state = DailyTop100UiState(key = "it:it", open = DailyTop100ChartUiState(DailyChartRef.City(cities.first())))
        assertSame(state, publishOpen(state, DailyChartRef.Country(top100[1]), listOf(track("a"))))
    }

    @Test
    fun openPublishesTracksAndFailure() {
        val chart = DailyChartRef.City(cities.first())
        val state = DailyTop100UiState(key = "it:it", open = DailyTop100ChartUiState(chart))
        val loaded = publishOpen(state, chart, listOf(track("a")))
        assertEquals(listOf(track("a")), loaded.open?.tracks)
        assertEquals(false, loaded.open?.loading)
        assertEquals(true, publishOpen(state, chart, null).open?.failed)
    }

    @Test
    fun cityFetchFailureDoesNotCancelTop100() = runBlocking {
        val (charts, failedCities) = coroutineScope {
            listOf(
                async { chartsOrNull { top100 } },
                async { chartsOrNull<AppleCityChart> { throw IOException("offline") } }
            ).awaitAll()
        }
        assertEquals(top100, charts)
        assertNull(failedCities)
        assertNull(chartsOrNull { emptyList<AppleCityChart>() })
    }

    @Test(expected = CancellationException::class)
    fun chartFetchCancellationPropagates() {
        runBlocking { chartsOrNull<AppleCityChart> { throw CancellationException("left") } }
    }
}
