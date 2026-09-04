package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.ResonanceCommentSnippet
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.resonanceCommentsForTracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class HomeResonanceCommentsPolicyTest {
    @Test
    fun replacingTracksLoadsLatestListAndRejectsOldPublication() {
        val oldTrack = track("aaaaaaaaaaa")
        val newTrack = track("bbbbbbbbbbb")
        val oldComments = mapOf(
            oldTrack.id to ResonanceCommentSnippet(videoId = oldTrack.id, text = "Old", updatedAtMs = 1L)
        )
        val replacedState = LevyraUiState(
            homeResonanceTracks = listOf(newTrack),
            homeResonanceComments = resonanceCommentsForTracks(listOf(newTrack), oldComments)
        )

        val stalePublication = replacedState.withHomeResonanceComment(listOf(oldTrack.id), oldTrack.id) {
            ResonanceCommentSnippet(videoId = oldTrack.id, text = "Late old result")
        }
        val latestCandidates = homeResonanceCommentsToRefresh(
            tracks = listOf(newTrack),
            comments = replacedState.homeResonanceComments,
            nowMs = 10_000L,
            ttlMs = 1_000L
        )
        val latestPublication = replacedState.withHomeResonanceComment(listOf(newTrack.id), newTrack.id) {
            ResonanceCommentSnippet(videoId = newTrack.id, text = "New result", updatedAtMs = 10_000L)
        }

        assertSame(replacedState, stalePublication)
        assertEquals(listOf(newTrack), latestCandidates)
        assertEquals(setOf(newTrack.id), latestPublication.homeResonanceComments.keys)
        assertEquals("New result", latestPublication.homeResonanceComments[newTrack.id]?.text)
    }

    @Test
    fun replacingTracksCancelsActiveRefreshAndLateOldResultCannotPublish() = runBlocking {
        val oldTrack = track("aaaaaaaaaaa")
        val newTrack = track("bbbbbbbbbbb")
        val oldStarted = CompletableDeferred<Unit>()
        val releaseOld = CompletableDeferred<Unit>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var state = LevyraUiState(homeResonanceTracks = listOf(oldTrack))

        val oldJob = scope.replaceHomeResonanceCommentsJob(null) {
            oldStarted.complete(Unit)
            releaseOld.await()
            state = state.withHomeResonanceComment(listOf(oldTrack.id), oldTrack.id) {
                ResonanceCommentSnippet(videoId = oldTrack.id, text = "Late old result")
            }
        }
        withTimeout(1_000L) { oldStarted.await() }
        state = state.copy(homeResonanceTracks = listOf(newTrack), homeResonanceComments = emptyMap())
        val newJob = scope.replaceHomeResonanceCommentsJob(oldJob) {
            state = state.withHomeResonanceComment(listOf(newTrack.id), newTrack.id) {
                ResonanceCommentSnippet(videoId = newTrack.id, text = "New result")
            }
        }

        withTimeout(1_000L) { newJob.join() }
        releaseOld.complete(Unit)
        withTimeout(1_000L) { oldJob.join() }

        assertEquals(setOf(newTrack.id), state.homeResonanceComments.keys)
        assertEquals("New result", state.homeResonanceComments[newTrack.id]?.text)
        scope.cancel()
    }

    @Test
    fun staleCachedCommentRemainsVisibleAndIsSelectedForRefresh() {
        val track = track("aaaaaaaaaaa")
        val cached = ResonanceCommentSnippet(
            videoId = track.id,
            text = "Cached comment",
            updatedAtMs = 1_000L
        )
        val retained = resonanceCommentsForTracks(listOf(track), mapOf(track.id to cached))

        val candidates = homeResonanceCommentsToRefresh(
            tracks = listOf(track),
            comments = retained,
            nowMs = 7_001L,
            ttlMs = 6_000L
        )
        val refreshing = cached.copy(
            isLoading = cached.hasComment.not() && cached.disabled.not(),
            hasError = false
        )

        assertEquals("Cached comment", refreshing.text)
        assertEquals(false, refreshing.isLoading)
        assertEquals(listOf(track), candidates)
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://music.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0
    )
}
