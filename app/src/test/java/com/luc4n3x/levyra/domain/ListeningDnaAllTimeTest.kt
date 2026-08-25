package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningDnaAllTimeTest {

    private val nowMs = 1_700_000_000_000L

    private val windowEvents = listOf(
        ListenEvent(
            trackId = "t1",
            title = "Song",
            artist = "Artist",
            listenedMs = 45_000L,
            trackDurationMs = 0L,
            completed = false,
            startedAt = nowMs - 1_000L
        )
    )

    @Test
    fun `buildAllTime prefers lifetime aggregate totals over the events window`() {
        val lifetime = LifetimeListening(
            totalListenMs = 999_999_999L,
            countedPlays = 500,
            completedCount = 10,
            eventCount = 20,
            distinctTracks = 50,
            distinctArtists = 10,
            firstPlayedAt = nowMs - 1_000_000L,
            lastPlayedAt = nowMs,
            tracks = listOf(PulseTrack("t1", "Song", "Artist", 500, 999_999_999L)),
            artists = listOf(LifetimeArtist("Artist", 500, 999_999_999L))
        )

        val dna = ListeningDnaEngine.buildAllTime(lifetime, windowEvents, nowMs)

        assertEquals(999_999_999L, dna.totalListenMs)
        assertEquals(500, dna.plays)
        assertEquals(50, dna.distinctTracks)
        assertEquals(10, dna.distinctArtists)
    }

    @Test
    fun `buildAllTime falls back to window result when lifetime has no signal`() {
        val emptyLifetime = LifetimeListening()
        val expectedWindow = ListeningDnaEngine.build(windowEvents, ListeningDnaPeriod.AllTime, nowMs)

        val dna = ListeningDnaEngine.buildAllTime(emptyLifetime, windowEvents, nowMs)

        assertEquals(expectedWindow.totalListenMs, dna.totalListenMs)
        assertEquals(expectedWindow.plays, dna.plays)
        assertEquals(expectedWindow.distinctTracks, dna.distinctTracks)
        assertEquals(expectedWindow.distinctArtists, dna.distinctArtists)
    }
}
