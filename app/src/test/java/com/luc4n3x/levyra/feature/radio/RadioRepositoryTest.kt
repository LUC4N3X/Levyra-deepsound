package com.luc4n3x.levyra.feature.radio

import android.content.Context
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class RadioRepositoryTest {
    @Test
    fun personalStationsRemainSearchableWhenCatalogIsOffline() = runBlocking {
        val store = RadioStore(MemoryRadioPreferences())
        val station = customRadioStation("Радио Маяк", "https://radio.example/live", "")
        store.saveCustom(station)
        val result = repository(store).search("МАЯК")
        assertEquals(listOf(station), result.stations)
        assertTrue(result.partialFailure)
        assertTrue(result.next.isNotEmpty())
    }

    @Test
    fun personalRecoveryDoesNotQueryRadioBrowser() = runBlocking {
        val station = customRadioStation("Personal", "https://radio.example/live", "")
        val requests = AtomicInteger()
        val repository = repository(RadioStore(MemoryRadioPreferences()), requests)
        assertTrue(repository.siblingStreams(station).isEmpty())
        assertEquals(0, requests.get())
    }

    private fun repository(store: RadioStore, requests: AtomicInteger = AtomicInteger()) = RadioRepository(
        context = mock(Context::class.java),
        store = store,
        api = RadioBrowserApi(
            client = OkHttpClient.Builder().addInterceptor { requests.incrementAndGet(); throw IOException("Catalog offline fixture") }.build(),
            serverPool = RadioBrowserServerPool(lookup = { emptyArray() })
        )
    )
}
