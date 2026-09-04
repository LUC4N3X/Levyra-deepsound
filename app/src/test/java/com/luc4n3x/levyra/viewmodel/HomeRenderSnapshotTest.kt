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
    fun publishesPersonalOrbitRemovalImmediatelyWhileHomeStructureIsFrozen() {
        val keptTrack = track("aaaaaaaaaaa")
        val removedTrack = track("bbbbbbbbbbb")
        val initialState = LevyraUiState(
            tracks = listOf(keptTrack, removedTrack),
            personalOrbitTracks = listOf(keptTrack, removedTrack),
            homeSections = listOf(HomeSection("Initial", listOf(keptTrack, removedTrack)))
        )
        val previous = buildHomeRenderSnapshot(initialState)
        val updatedState = initialState.copy(
            tracks = listOf(removedTrack),
            personalOrbitTracks = listOf(keptTrack),
            homeSections = listOf(HomeSection("Refreshed", listOf(removedTrack)))
        )

        val frozen = buildStableHomeRenderSnapshot(updatedState, previous, freezeContent = true)

        assertEquals(listOf(keptTrack), frozen.state.personalOrbitTracks)
        assertSame(previous.state.tracks, frozen.state.tracks)
        assertSame(previous.state.homeSections, frozen.state.homeSections)
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
    fun exposesQuickPicksWithoutDuplicatingThemInGenericSections() {
        val quickTrack = track("aaaaaaaaaaa")
        val state = LevyraUiState(
            homeSections = listOf(HomeSection("Quick picks", listOf(quickTrack)))
        )

        val snapshot = buildHomeRenderSnapshot(state)

        assertEquals(listOf(quickTrack), snapshot.derived.quickPicks?.tracks)
        assertEquals(emptyList<HomeSection>(), snapshot.derived.otherSections)
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

    @Test
    fun keepsContentFingerprintStableWhenPlaybackStartsAndStops() {
        val chartTrack = track("aaaaaaaaaaa")
        val idle = LevyraUiState(
            tracks = listOf(chartTrack),
            homeSections = listOf(HomeSection("Quick picks", listOf(chartTrack))),
            charts = listOf(chartTrack)
        )
        val playing = idle.copy(currentTrack = chartTrack, isPlaying = true, isResolving = true)

        val idleSnapshot = buildHomeRenderSnapshot(idle)
        val playingSnapshot = buildHomeRenderSnapshot(playing)

        // The fingerprint gates the deferred home shelves. If playback flipped it, tapping a chart row
        // or closing the player would unmount everything below the fold and reset the home scroll.
        assertEquals(idleSnapshot.derived.contentFingerprint, playingSnapshot.derived.contentFingerprint)
        assertEquals(true, playingSnapshot.derived.contentAvailability.hasCurrentTrack)
        assertEquals(false, idleSnapshot.derived.contentAvailability.hasCurrentTrack)
    }

    @Test
    fun prioritizesCommentedSectionTracksInResonanceShelf() {
        val standardTrack = track("std11111111").copy(replayScore = 80)
        val commentedTrack = track("cmt11111111").copy(replayScore = 20)
        val state = LevyraUiState(
            charts = listOf(standardTrack),
            homeSections = listOf(
                HomeSection("Tracce più commentate", listOf(commentedTrack))
            ),
            languageCode = "it"
        )

        val snapshot = buildHomeRenderSnapshot(state)

        assertEquals("cmt11111111", snapshot.derived.resonanceTracks.firstOrNull()?.id)
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
