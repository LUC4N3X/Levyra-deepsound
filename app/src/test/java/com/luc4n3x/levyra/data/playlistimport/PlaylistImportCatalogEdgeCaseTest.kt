package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.SearchPage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImportCatalogEdgeCaseTest {

    private fun track(
        id: String,
        title: String,
        artist: String,
        durationMs: Long = 210_000L,
        videoType: String = "MUSIC_VIDEO_TYPE_ATV"
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Test Album",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
        largeThumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0,
        videoType = videoType
    )

    @Test
    fun normalizedFallbackKeepsCorrectTrackWhenFirstPassAlreadyFilledLimit() = runBlocking {
        val noisyTitle = "In the End (feat. Jay-Z) [Live At Wembley] - Remastered 2021 (Explicit)"
        val wrong = (1..8).map { index ->
            track("wrong_$index", "Unrelated Song $index", "Different Artist $index")
        }
        val correct = track("correct_fallback", "In the End", "Linkin Park")

        val repository = object : YoutubeMusicRepository(null) {
            override suspend fun searchSongsPage(
                query: String,
                languageCode: String,
                continuation: String
            ): SearchPage<Track> {
                val results = if (query.contains("Live") || query.contains("Remastered")) wrong else listOf(correct)
                return SearchPage(items = results, continuation = "")
            }

            override suspend fun search(query: String, limit: Int, languageCode: String): List<Track> =
                if (query.contains("Live") || query.contains("Remastered")) wrong else listOf(correct)
        }

        val catalog = PlaylistImportCatalog(repository, localTracks = { emptyList() }, languageCode = { "en" })
        val identity = ImportedTrackIdentity(
            position = 0,
            title = noisyTitle,
            artists = listOf("Linkin Park"),
            durationMs = 210_000L
        )

        val candidates = catalog.candidates(identity, broad = false)

        assertTrue(
            "Normalized fallback found the correct track but it was dropped by the result limit",
            candidates.any { it.candidate.id == "correct_fallback" }
        )
    }

    @Test
    fun acceptedSongBeyondEighthPositionIsNotDroppedByFastPath() = runBlocking {
        val wrong = (1..8).map { index ->
            track("wrong_fast_$index", "Unrelated Song $index", "Different Artist $index")
        }
        val correct = track("correct_ninth", "Bohemian Rhapsody", "Queen", durationMs = 210_000L)

        val repository = object : YoutubeMusicRepository(null) {
            override suspend fun searchSongsPage(
                query: String,
                languageCode: String,
                continuation: String
            ): SearchPage<Track> = SearchPage(items = wrong + correct, continuation = "")

            override suspend fun search(query: String, limit: Int, languageCode: String): List<Track> = emptyList()
        }

        val catalog = PlaylistImportCatalog(repository, localTracks = { emptyList() }, languageCode = { "en" })
        val identity = ImportedTrackIdentity(
            position = 0,
            title = "Bohemian Rhapsody",
            artists = listOf("Queen"),
            durationMs = 210_000L
        )

        val candidates = catalog.candidates(identity, broad = false)

        assertTrue(
            "A high-confidence Songs match beyond position eight must not be discarded",
            candidates.any { it.candidate.id == "correct_ninth" }
        )
    }

    @Test
    fun songsRuntimeFailureDoesNotDiscardMixedSearchResults() = runBlocking {
        val mixedTrack = track("mixed_survives", "Alive", "Pearl Jam")
        val repository = object : YoutubeMusicRepository(null) {
            override suspend fun searchSongsPage(
                query: String,
                languageCode: String,
                continuation: String
            ): SearchPage<Track> = throw IllegalStateException("Malformed Songs response")

            override suspend fun search(query: String, limit: Int, languageCode: String): List<Track> = listOf(mixedTrack)
        }
        val catalog = PlaylistImportCatalog(repository, localTracks = { emptyList() }, languageCode = { "en" })

        val results = catalog.search("Alive Pearl Jam", CandidateOrigin.ONLINE)

        assertEquals(listOf("mixed_survives"), results.map { it.candidate.id })
    }

    @Test
    fun mixedRuntimeFailureDoesNotDiscardSongsSearchResults() = runBlocking {
        val songTrack = track("song_survives", "Everlong", "Foo Fighters")
        val repository = object : YoutubeMusicRepository(null) {
            override suspend fun searchSongsPage(
                query: String,
                languageCode: String,
                continuation: String
            ): SearchPage<Track> = SearchPage(items = listOf(songTrack), continuation = "")

            override suspend fun search(query: String, limit: Int, languageCode: String): List<Track> =
                throw IllegalArgumentException("Malformed mixed response")
        }
        val catalog = PlaylistImportCatalog(repository, localTracks = { emptyList() }, languageCode = { "en" })

        val results = catalog.search("Everlong Foo Fighters", CandidateOrigin.ONLINE)

        assertEquals(listOf("song_survives"), results.map { it.candidate.id })
    }

    @Test
    fun onlineSearchStillPropagatesCancellation() = runBlocking {
        val repository = object : YoutubeMusicRepository(null) {
            override suspend fun searchSongsPage(
                query: String,
                languageCode: String,
                continuation: String
            ): SearchPage<Track> = throw CancellationException("cancelled")

            override suspend fun search(query: String, limit: Int, languageCode: String): List<Track> = emptyList()
        }
        val catalog = PlaylistImportCatalog(repository, localTracks = { emptyList() }, languageCode = { "en" })
        var propagated = false

        try {
            catalog.search("Cancelled Search", CandidateOrigin.ONLINE)
        } catch (_: CancellationException) {
            propagated = true
        }

        assertTrue("CancellationException must not be swallowed by branch isolation", propagated)
    }
}
