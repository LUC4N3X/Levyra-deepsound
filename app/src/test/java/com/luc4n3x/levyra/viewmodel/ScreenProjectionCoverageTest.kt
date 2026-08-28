package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.BatchDownload
import com.luc4n3x.levyra.domain.BatchDownloadKind
import com.luc4n3x.levyra.domain.BatchDownloadState
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.feature.recognition.RecognitionState
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScreenProjectionCoverageTest {
    private val base = LevyraUiState()

    @Test
    fun `search projection reacts to section loading state`() {
        assertNotEquals(
            searchProjection(base),
            searchProjection(base.copy(searchSectionLoading = setOf(SearchFilter.Albums)))
        )
    }

    @Test
    fun `search projection reacts to section continuations`() {
        assertNotEquals(
            searchProjection(base),
            searchProjection(base.copy(searchSectionContinuations = mapOf(SearchFilter.Albums to "TOKEN")))
        )
    }

    @Test
    fun `search projection reacts to the new result categories`() {
        val withPlaylists = base.copy(
            searchData = SearchResults(
                playlists = listOf(PlaylistHit(title = "Chill", author = "YT", thumbnailUrl = "", playlistId = "PL1"))
            )
        )

        assertNotEquals(searchProjection(base), searchProjection(withPlaylists))
    }

    @Test
    fun `search projection reacts to failed sections`() {
        assertNotEquals(
            searchProjection(base),
            searchProjection(base.copy(searchData = SearchResults(failedSections = setOf(SearchFilter.Artists))))
        )
    }

    @Test
    fun `search projection reacts to recognition availability`() {
        assertNotEquals(
            searchProjection(base),
            searchProjection(base.copy(recognitionAvailable = false))
        )
    }

    @Test
    fun `search projection reacts to recognition state`() {
        assertNotEquals(
            searchProjection(base),
            searchProjection(base.copy(recognitionState = RecognitionState.Listening))
        )
    }

    @Test
    fun `library projection reacts to batch downloads`() {
        val withBatch = base.copy(downloadBatches = listOf(batch(completed = 1, progress = 25)))

        assertNotEquals(libraryProjection(base), libraryProjection(withBatch))
    }

    @Test
    fun `library projection reacts to batch progress advancing`() {
        val early = base.copy(downloadBatches = listOf(batch(completed = 1, progress = 25)))
        val later = base.copy(downloadBatches = listOf(batch(completed = 2, progress = 50)))

        assertNotEquals(libraryProjection(early), libraryProjection(later))
    }

    private fun batch(completed: Int, progress: Int) = BatchDownload(
        key = "album:test",
        kind = BatchDownloadKind.Album,
        title = "Test",
        artworkUrl = "",
        total = 4,
        completed = completed,
        failed = 0,
        active = 4 - completed,
        progress = progress,
        state = BatchDownloadState.Downloading
    )
}
