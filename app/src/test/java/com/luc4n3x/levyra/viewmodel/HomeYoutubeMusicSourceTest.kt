package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicHome
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicHomeSource
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.HOME_PERSONAL_ORBIT_MAX_TRACKS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeYoutubeMusicSourceTest {
    private val standardOrbit = (1..9).map { track("std$it") }
    private val youtubeOrbit = (1..HOME_PERSONAL_ORBIT_MAX_TRACKS).map { track("ytm$it") }
    private val base = LevyraUiState(
        personalOrbitTracks = standardOrbit,
        tracks = standardOrbit,
        homeSections = listOf(HomeSection("Levyra", standardOrbit))
    )
    private val ready = YoutubeMusicHomeSource.Ready(
        YoutubeMusicHome("account", "it", youtubeOrbit, listOf(HomeSection("Mixed for you", youtubeOrbit)), emptyList())
    )

    @Test
    fun pendingSessionNeverRendersStandardOrbit() {
        val snapshot = buildHomeRenderSnapshot(base, YoutubeMusicHomeSource.Pending)

        assertTrue(snapshot.orbitPending)
        assertTrue(snapshot.state.personalOrbitTracks.isEmpty())
        assertTrue(snapshot.derived.personalOrbitDisplayTracks.none { it.id.startsWith("std") })
    }

    @Test
    fun readySessionRendersOnlyYoutubeMusicOrbit() {
        val snapshot = buildHomeRenderSnapshot(base, ready)

        assertFalse(snapshot.orbitPending)
        assertEquals(youtubeOrbit.map { it.id }, snapshot.state.personalOrbitTracks.map { it.id })
        assertTrue(snapshot.derived.personalOrbitDisplayTracks.none { it.id.startsWith("std") })
    }

    @Test
    fun standardSourceKeepsLevyraOrbit() {
        val snapshot = buildHomeRenderSnapshot(base, YoutubeMusicHomeSource.Standard)

        assertFalse(snapshot.orbitPending)
        assertEquals(standardOrbit, snapshot.state.personalOrbitTracks)
    }

    @Test
    fun restoreSequenceNeverPassesThroughStandardOrbit() {
        var snapshot = buildHomeRenderSnapshot(base, YoutubeMusicHomeSource.Pending)
        val shown = mutableListOf(snapshot.derived.personalOrbitDisplayTracks)
        listOf(YoutubeMusicHomeSource.Pending, ready, ready).forEach { source ->
            snapshot = buildStableHomeRenderSnapshot(base, snapshot, freezeContent = false, source = source)
            shown += snapshot.derived.personalOrbitDisplayTracks
        }

        assertTrue(shown.flatten().none { it.id.startsWith("std") })
        assertEquals(youtubeOrbit.map { it.id }, snapshot.state.personalOrbitTracks.map { it.id })
    }

    @Test
    fun recreatedHomeStartsFromTheCurrentSource() {
        val recreated = buildHomeRenderSnapshot(base, ready)

        assertEquals(youtubeOrbit.map { it.id }, recreated.state.personalOrbitTracks.map { it.id })
    }

    @Test
    fun refreshWhileFrozenKeepsYoutubeMusicOrbit() {
        val previous = buildHomeRenderSnapshot(base, ready)
        val refreshed = base.copy(personalOrbitTracks = (1..9).map { track("new$it") }, isPlaying = true)

        val frozen = buildStableHomeRenderSnapshot(refreshed, previous, freezeContent = true, source = ready)

        assertEquals(youtubeOrbit.map { it.id }, frozen.state.personalOrbitTracks.map { it.id })
        assertSame(previous.state.homeSections, frozen.state.homeSections)
    }

    @Test
    fun logoutReturnsToStandardOrbit() {
        val previous = buildHomeRenderSnapshot(base, ready)

        val loggedOut = buildStableHomeRenderSnapshot(base, previous, freezeContent = false, source = YoutubeMusicHomeSource.Standard)

        assertEquals(standardOrbit, loggedOut.state.personalOrbitTracks)
        assertFalse(loggedOut.orbitPending)
    }

    @Test
    fun pendingFlagIsPartOfSnapshotIdentity() {
        val pending = buildHomeRenderSnapshot(base.copy(personalOrbitTracks = emptyList()), YoutubeMusicHomeSource.Pending)
        val standard = buildHomeRenderSnapshot(base.copy(personalOrbitTracks = emptyList()), YoutubeMusicHomeSource.Standard)

        assertFalse(sameHomeRenderSnapshot(pending, standard))
    }

    @Test
    fun regionChangeWhileScrolledPublishesTheLoadedChart() {
        val italy = (1..50).map { track("it$it") }
        val usa = (1..50).map { track("us$it") }
        var snapshot = buildHomeRenderSnapshot(LevyraUiState(selectedChartId = "it", charts = italy))

        snapshot = buildStableHomeRenderSnapshot(
            LevyraUiState(selectedChartId = "us", charts = emptyList(), isLoadingCharts = true),
            snapshot,
            freezeContent = true
        )
        assertTrue(snapshot.state.isLoadingCharts)

        snapshot = buildStableHomeRenderSnapshot(
            LevyraUiState(selectedChartId = "us", charts = usa, isLoadingCharts = false),
            snapshot,
            freezeContent = true
        )

        assertEquals(usa, snapshot.state.charts)
        assertFalse(snapshot.state.isLoadingCharts)
    }

    @Test
    fun artistsLoadedWhileScrolledReplaceTheLoadingShelf() {
        val artist = com.luc4n3x.levyra.domain.ArtistHit("Artist", "", "https://example.com/a.jpg", 0, 0, browseId = "UC1", officialArtwork = true)
        val previous = buildHomeRenderSnapshot(LevyraUiState(homeArtists = emptyList(), homeArtistsLoading = true))

        val frozen = buildStableHomeRenderSnapshot(
            LevyraUiState(homeArtists = listOf(artist), homeArtistsLoading = false),
            previous,
            freezeContent = true
        )

        assertEquals(listOf(artist), frozen.state.homeArtists)
        assertFalse(frozen.state.homeArtistsLoading)
    }

    @Test
    fun frozenHomeFillsMissingCommentsButKeepsShownOnes() {
        val shown = com.luc4n3x.levyra.domain.ResonanceCommentSnippet(videoId = "one", author = "a", text = "shown")
        val replaced = com.luc4n3x.levyra.domain.ResonanceCommentSnippet(videoId = "one", author = "b", text = "replaced")
        val loading = com.luc4n3x.levyra.domain.ResonanceCommentSnippet(videoId = "two", isLoading = true)
        val arrived = com.luc4n3x.levyra.domain.ResonanceCommentSnippet(videoId = "two", author = "c", text = "arrived")
        val previous = buildHomeRenderSnapshot(LevyraUiState(homeResonanceComments = mapOf("one" to shown, "two" to loading)))

        val frozen = buildStableHomeRenderSnapshot(
            LevyraUiState(homeResonanceComments = mapOf("one" to replaced, "two" to arrived)),
            previous,
            freezeContent = true
        )

        assertEquals(mapOf("one" to shown, "two" to arrived), frozen.state.homeResonanceComments)
    }

    @Test
    fun scrolledHomeKeepsPopulatedChartUntilSettled() {
        val italy = (1..50).map { track("it$it") }
        val refreshed = (1..50).map { track("rf$it") }
        val previous = buildHomeRenderSnapshot(LevyraUiState(selectedChartId = "it", charts = italy))

        val frozen = buildStableHomeRenderSnapshot(
            LevyraUiState(selectedChartId = "it", charts = refreshed),
            previous,
            freezeContent = true
        )

        assertSame(previous.state.charts, frozen.state.charts)
    }

    @Test
    fun accountOrbitStartsWithTracksJustPlayedInLevyra() {
        val played = listOf(track("played1"), track("ytm3"))
        val snapshot = buildHomeRenderSnapshot(base.copy(recentListens = played), ready)

        val ids = snapshot.state.personalOrbitTracks.map { it.id }
        assertEquals(listOf("played1", "ytm3", "ytm1", "ytm2", "ytm4"), ids.take(5))
        assertEquals(ids.distinct(), ids)
        assertFalse(ids.any { it.startsWith("std") })
    }

    @Test
    fun shortAccountOrbitIsFilledFromOtherYoutubeMusicShelves() {
        val shortOrbit = (1..9).map { track("orbit$it") }
        val shelf = (1..12).map { track("shelf$it") }
        val source = YoutubeMusicHomeSource.Ready(
            YoutubeMusicHome("account", "it", shortOrbit, listOf(HomeSection("Mixed for you", shelf)), emptyList())
        )

        val orbit = buildHomeRenderSnapshot(base, source).state.personalOrbitTracks.map { it.id }

        assertEquals(shortOrbit.map { it.id }, orbit.take(9))
        assertTrue(orbit.size >= HOME_PERSONAL_ORBIT_MAX_TRACKS)
        assertTrue(orbit.drop(9).all { it.startsWith("shelf") })
    }

    private fun track(id: String) = Track(
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
