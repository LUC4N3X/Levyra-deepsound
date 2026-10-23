package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.SearchPage
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeMusicPlaybackSearchTest {
    private val track = Track(
        id = "7sEXyTvcZOE", title = "tiny window", artist = "Ólafur Apollo", album = "tiny window",
        durationMs = 142_000L, streamUrl = "", videoUrl = "https://www.youtube.com/watch?v=7sEXyTvcZOE",
        thumbnailUrl = "", largeThumbnailUrl = "", source = "YouTube Music", moodTags = emptySet(),
        energy = 50, vocal = 50, replayScore = 70, cacheScore = 70, accentStart = 0, accentEnd = 0
    )

    @Test
    fun playbackFindsSongWithoutUsingTheGenericSearchThatMissesIt() = runBlocking {
        val repository = object : YoutubeMusicRepository() {
            override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> {
                assertEquals("tiny window Ólafur Apollo", query)
                assertEquals("en", languageCode)
                return SearchPage(listOf(track))
            }

            override suspend fun search(query: String, limit: Int, languageCode: String): List<Track> =
                error("Generic search must only run after an empty song search")
        }

        assertEquals(listOf(track), repository.searchForPlayback("tiny window Ólafur Apollo", 8, "en"))
    }

    @Test
    fun rejectedSongResultsStillAllowAnOfficialVideoAlternative() = runBlocking {
        val alternative = track.copy(id = "newvideo001", videoUrl = "https://www.youtube.com/watch?v=newvideo001")
        val repository = object : YoutubeMusicRepository() {
            override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> =
                SearchPage(listOf(track))

            override suspend fun search(query: String, limit: Int, languageCode: String): List<Track> = listOf(alternative)
        }

        val result = repository.searchForPlayback("tiny window Ólafur Apollo", 8, "en") { candidates ->
            candidates.filterNot { it.id == track.id }
        }
        assertEquals(listOf(alternative), result)
    }

    @Test
    fun emptySongSearchFallsBackToGenericSearch() = runBlocking {
        val repository = object : YoutubeMusicRepository() {
            override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> =
                SearchPage()

            override suspend fun search(query: String, limit: Int, languageCode: String): List<Track> = listOf(track)
        }

        assertEquals(listOf(track), repository.searchForPlayback("tiny window Ólafur Apollo", 8, "en"))
    }

    @Test(expected = CancellationException::class)
    fun cancelledSongSearchDoesNotStartAnotherRequest(): Unit = runBlocking {
        val repository = object : YoutubeMusicRepository() {
            override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> =
                throw CancellationException("selection changed")

            override suspend fun search(query: String, limit: Int, languageCode: String): List<Track> =
                error("Cancelled playback must not start another request")
        }

        repository.searchForPlayback("tiny window Ólafur Apollo", 8, "en")
        Unit
    }
}
