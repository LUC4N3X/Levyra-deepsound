package com.luc4n3x.levyra.feature.providers

import com.luc4n3x.levyra.data.YoutubeMusicPlaylistDetail
import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SearchResults
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class LevyraProviderRouterTest {
    @Test
    fun catalogFallsBackAfterProviderFailure() = runBlocking {
        val failures = AtomicInteger(0)
        val primary = FakeCatalogProvider(
            id = "primary",
            priority = 0,
            search = {
                failures.incrementAndGet()
                throw IOException("offline")
            }
        )
        val fallbackAlbum = AlbumHit(
            title = "Fallback",
            artist = "Levyra",
            year = "2026",
            thumbnailUrl = "https://example.com/art.jpg",
            query = "Fallback Levyra"
        )
        val fallback = FakeCatalogProvider(
            id = "fallback",
            priority = 10,
            search = { SearchResults(albums = listOf(fallbackAlbum)) }
        )
        val router = LevyraProviderRouter(
            catalogProviders = listOf(primary, fallback),
            playbackProviders = emptyList(),
            catalogTimeoutMs = 1_000L
        )

        val result = router.searchEverything("fallback", "it")

        assertEquals("Fallback", result.albums.single().title)
        assertEquals(1, failures.get())
        assertTrue(router.health().first { it.providerId == "primary" }.totalFailures == 1L)
    }

    @Test
    fun circuitBreakerStopsCallingRepeatedlyFailingProvider() = runBlocking {
        val calls = AtomicInteger(0)
        val failing = FakeCatalogProvider(
            id = "unstable",
            priority = 0,
            search = {
                calls.incrementAndGet()
                throw IOException("unavailable")
            }
        )
        val fallback = FakeCatalogProvider(
            id = "stable",
            priority = 10,
            search = { SearchResults(albums = listOf(album("Stable"))) }
        )
        val router = LevyraProviderRouter(
            catalogProviders = listOf(failing, fallback),
            playbackProviders = emptyList(),
            healthTracker = LevyraProviderHealthTracker(failureThreshold = 3, cooldownMs = 60_000L),
            catalogTimeoutMs = 1_000L
        )

        repeat(5) { router.searchEverything("test", "it") }

        assertEquals(3, calls.get())
        val health = router.health().first { it.providerId == "unstable" }
        assertEquals(3, health.consecutiveFailures)
        assertTrue(health.circuitOpenUntilMs > System.currentTimeMillis())
    }

    private fun album(title: String): AlbumHit = AlbumHit(
        title = title,
        artist = "Levyra",
        year = "2026",
        thumbnailUrl = "https://example.com/art.jpg",
        query = title
    )

    private class FakeCatalogProvider(
        override val id: String,
        override val priority: Int,
        private val search: suspend () -> SearchResults
    ) : LevyraCatalogProvider {
        override suspend fun searchEverything(query: String, languageCode: String): SearchResults = search()

        override suspend fun albumDetail(album: AlbumHit, languageCode: String): AlbumDetail? = null

        override suspend fun playlist(playlistId: String, languageCode: String, limit: Int): YoutubeMusicPlaylistDetail? = null
    }
}
