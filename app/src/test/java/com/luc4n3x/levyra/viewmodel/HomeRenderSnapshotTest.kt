package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class HomeRenderSnapshotTest {
    @Test
    fun keepsBaseChartsAndDerivedChunksFromTheSameState() {
        val charts = listOf(track("aaaaaaaaaaa"), track("bbbbbbbbbbb"), track("ccccccccccc"), track("ddddddddddd"), track("eeeeeeeeeee"))
        val state = LevyraUiState(charts = charts)

        val snapshot = buildHomeRenderSnapshot(state)

        assertSame(state, snapshot.state)
        assertEquals(snapshot.state.charts, snapshot.derived.chartChunks.flatten())
    }

    @Test
    fun keepsHomeStructureFrozenWhileIdleAwayFromTop() {
        assertEquals(true, shouldFreezeHomeStructure(scrollInProgress = false, atTop = false))
        assertEquals(true, shouldFreezeHomeStructure(scrollInProgress = true, atTop = true))
        assertEquals(false, shouldFreezeHomeStructure(scrollInProgress = false, atTop = true))
    }

    @Test
    fun freezesStructuralHomeContentWhileScrolling() {
        val initialTrack = track("aaaaaaaaaaa")
        val refreshedTrack = track("bbbbbbbbbbb")
        val initialState = LevyraUiState(
            tracks = listOf(initialTrack),
            homeSections = listOf(HomeSection("Initial", listOf(initialTrack)))
        )
        val previous = buildHomeRenderSnapshot(initialState)
        val refreshedState = initialState.copy(
            tracks = listOf(refreshedTrack),
            homeSections = listOf(HomeSection("Refreshed", listOf(refreshedTrack))),
            isPlaying = true
        )

        val frozen = buildStableHomeRenderSnapshot(refreshedState, previous, freezeContent = true)

        assertSame(previous.state.tracks, frozen.state.tracks)
        assertSame(previous.state.homeSections, frozen.state.homeSections)
        assertEquals(true, frozen.state.isPlaying)
        assertSame(previous.derived, frozen.derived)
    }

    @Test
    fun publishesLatestStructuralHomeContentAfterScrollingSettles() {
        val initialTrack = track("aaaaaaaaaaa")
        val refreshedTrack = track("bbbbbbbbbbb")
        val initialState = LevyraUiState(
            tracks = listOf(initialTrack),
            homeSections = listOf(HomeSection("Initial", listOf(initialTrack)))
        )
        val previous = buildHomeRenderSnapshot(initialState)
        val refreshedState = initialState.copy(
            tracks = listOf(refreshedTrack),
            homeSections = listOf(HomeSection("Refreshed", listOf(refreshedTrack)))
        )

        val published = buildStableHomeRenderSnapshot(refreshedState, previous, freezeContent = false)

        assertSame(refreshedState.tracks, published.state.tracks)
        assertSame(refreshedState.homeSections, published.state.homeSections)
        assertNotSame(previous.derived, published.derived)
        assertEquals(listOf(refreshedTrack), published.derived.otherSections.single().tracks)
    }

    @Test
    fun keepsCachedResonanceStableAcrossOtherHomeChanges() {
        val cachedResonance = track("aaaaaaaaaaa")
        val refreshedTrack = track("bbbbbbbbbbb")
        val initialState = LevyraUiState(
            tracks = listOf(cachedResonance),
            homeResonanceTracks = listOf(cachedResonance),
            homeResonanceUpdatedAt = 100L
        )
        val previous = buildHomeRenderSnapshot(initialState)
        val refreshedState = initialState.copy(
            tracks = listOf(refreshedTrack),
            homeSections = listOf(HomeSection("Refreshed", listOf(refreshedTrack)))
        )

        val refreshed = buildStableHomeRenderSnapshot(refreshedState, previous, freezeContent = false)

        assertEquals(listOf(cachedResonance), refreshed.derived.resonanceTracks)
    }

    @Test
    fun defersResonanceReplacementUntilScrollingSettles() {
        val initialTrack = track("aaaaaaaaaaa")
        val refreshedTrack = track("bbbbbbbbbbb")
        val initialState = LevyraUiState(homeResonanceTracks = listOf(initialTrack))
        val previous = buildHomeRenderSnapshot(initialState)
        val refreshedState = initialState.copy(homeResonanceTracks = listOf(refreshedTrack))

        val frozen = buildStableHomeRenderSnapshot(refreshedState, previous, freezeContent = true)
        val published = buildStableHomeRenderSnapshot(refreshedState, previous, freezeContent = false)

        assertEquals(listOf(initialTrack), frozen.derived.resonanceTracks)
        assertEquals(listOf(refreshedTrack), published.derived.resonanceTracks)
    }

    private fun track(id: String): Track {
        return Track(
            id = id,
            title = "Title $id",
            artist = "Artist $id",
            album = "Album $id",
            durationMs = 180_000L,
            streamUrl = "",
            videoUrl = "https://music.youtube.com/watch?v=$id",
            thumbnailUrl = "https://example.com/$id.jpg",
            largeThumbnailUrl = "https://example.com/$id.jpg",
            source = "YouTube Music",
            moodTags = emptySet(),
            energy = 50,
            vocal = 50,
            replayScore = 50,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0
        )
    }
}
