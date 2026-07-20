package com.luc4n3x.levyra.feature.providers

import com.luc4n3x.levyra.data.YoutubeMusicPlaylistDetail
import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class LevyraProviderRouterTest {
    @Test
    fun defaultTimeoutsPreserveNativeResolverBudgets() {
        assertTrue(DEFAULT_PLAYBACK_PROVIDER_TIMEOUT_MS > 30_000L)
        assertTrue(DEFAULT_OFFLINE_PROVIDER_TIMEOUT_MS > 60_000L)
    }

    @Test
    fun offlineResolutionUsesItsIndependentTimeoutBudget() = runBlocking {
        val provider = object : LevyraPlaybackProvider {
            override val id: String = "native"
            override val priority: Int = 0

            override suspend fun resolve(track: Track, videoMode: Boolean): Track {
                delay(80L)
                return track.copy(streamUrl = "https://example.com/playback.m4a")
            }

            override suspend fun resolveForOffline(track: Track): Track {
                delay(80L)
                return track.copy(streamUrl = "https://example.com/offline.m4a")
            }
        }
        val router = LevyraProviderRouter(
            catalogProviders = emptyList(),
            playbackProviders = listOf(provider),
            playbackTimeoutMs = 20L,
            offlinePlaybackTimeoutMs = 200L
        )

        val result = router.resolveForOffline(track())

        assertEquals("https://example.com/offline.m4a", result.streamUrl)
    }

    @Test
    fun cacheMissDoesNotPolluteHealthOrOpenCircuit() = runBlocking {
        val cacheCalls = AtomicInteger(0)
        val cache = object : LevyraPlaybackProvider {
            override val id: String = "playback_cache"
            override val priority: Int = 0

            override suspend fun resolve(track: Track, videoMode: Boolean): Track {
                cacheCalls.incrementAndGet()
                throw LevyraProviderMissException("miss")
            }

            override suspend fun resolveForOffline(track: Track): Track {
                cacheCalls.incrementAndGet()
                throw LevyraProviderMissException("miss")
            }
        }
        val native = object : LevyraPlaybackProvider {
            override val id: String = "native"
            override val priority: Int = 10

            override suspend fun resolve(track: Track, videoMode: Boolean): Track {
                return track.copy(streamUrl = "https://example.com/native.m4a")
            }

            override suspend fun resolveForOffline(track: Track): Track {
                return track.copy(streamUrl = "https://example.com/native-offline.m4a")
            }
        }
        val router = LevyraProviderRouter(
            catalogProviders = emptyList(),
            playbackProviders = listOf(cache, native),
            healthTracker = LevyraProviderHealthTracker(failureThreshold = 3, cooldownMs = 60_000L),
            playbackTimeoutMs = 1_000L,
            offlinePlaybackTimeoutMs = 1_000L
        )

        repeat(5) { router.resolve(track()) }

        assertEquals(5, cacheCalls.get())
        val health = router.health().first { it.providerId == "playback_cache" }
        assertEquals(0, health.consecutiveFailures)
        assertEquals(0L, health.totalFailures)
        assertEquals(0L, health.circuitOpenUntilMs)
    }

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

    private fun track(): Track = Track(
        id = "track-1",
        title = "Track",
        artist = "Levyra",
        album = "Tests",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://youtube.com/watch?v=track-1",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )

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
