package com.luc4n3x.levyra.domain.recap

import com.luc4n3x.levyra.domain.LifetimeArtist
import com.luc4n3x.levyra.domain.LifetimeListening
import com.luc4n3x.levyra.domain.ListenEvent
import com.luc4n3x.levyra.domain.PulseTrack
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningRecapEngineTest {
    private val zone = ZoneOffset.UTC
    private val now = ZonedDateTime.of(2026, 7, 10, 18, 0, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun emptyEventsProduceNoSignal() {
        val recap = ListeningRecapEngine.build(emptyList(), ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertFalse(recap.hasSignal)
        assertEquals(0L, recap.totalListenMs)
        assertEquals(0, recap.totalPlays)
        assertEquals(0, recap.uniqueTracks)
        assertEquals(0, recap.uniqueArtists)
        assertEquals(0, recap.uniqueAlbums)
        assertTrue(recap.topTracks.isEmpty())
        assertTrue(recap.topArtists.isEmpty())
        assertTrue(recap.topAlbums.isEmpty())
        assertEquals(7, recap.dailyActivity.size)
    }

    @Test
    fun shortListensAreIgnored() {
        val events = listOf(
            event(trackId = "short", listenedMs = 3_000L, startedAt = hoursAgo(1))
        )
        val recap = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertFalse(recap.hasSignal)
        assertEquals(0, recap.totalPlays)
    }

    @Test
    fun futureEventsAreIgnored() {
        val events = listOf(
            event(trackId = "future", listenedMs = 60_000L, startedAt = now + 120_000L)
        )
        val recap = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertFalse(recap.hasSignal)
    }

    @Test
    fun periodFilteringRespectsWindow() {
        val eventRecent = event(trackId = "recent", listenedMs = 60_000L, startedAt = daysAgo(2))
        val eventOld = event(trackId = "old", listenedMs = 120_000L, startedAt = daysAgo(15))
        val eventVeryOld = event(trackId = "very_old", listenedMs = 180_000L, startedAt = daysAgo(100))

        val all = listOf(eventRecent, eventOld, eventVeryOld)

        val recap7 = ListeningRecapEngine.build(all, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)
        assertEquals(1, recap7.totalPlays)
        assertEquals("recent", recap7.topTracks.first().trackId)

        val recap30 = ListeningRecapEngine.build(all, ListeningRecapPeriod.Days30, nowMs = now, zone = zone)
        assertEquals(2, recap30.totalPlays)

        val recap365 = ListeningRecapEngine.build(all, ListeningRecapPeriod.Days365, nowMs = now, zone = zone)
        assertEquals(3, recap365.totalPlays)

        val recapAllTime = ListeningRecapEngine.build(all, ListeningRecapPeriod.AllTime, nowMs = now, zone = zone)
        assertEquals(3, recapAllTime.totalPlays)
    }

    @Test
    fun topTracksRanksCorrectly() {
        val events = listOf(
            event(trackId = "t1", title = "Song A", artist = "Artist 1", listenedMs = 180_000L, startedAt = hoursAgo(1)),
            event(trackId = "t1", title = "Song A", artist = "Artist 1", listenedMs = 180_000L, startedAt = hoursAgo(2)),
            event(trackId = "t1", title = "Song A", artist = "Artist 1", listenedMs = 180_000L, startedAt = hoursAgo(3)),
            event(trackId = "t2", title = "Song B", artist = "Artist 2", listenedMs = 120_000L, startedAt = hoursAgo(4)),
            event(trackId = "t2", title = "Song B", artist = "Artist 2", listenedMs = 120_000L, startedAt = hoursAgo(5)),
            event(trackId = "t3", title = "Song C", artist = "Artist 3", listenedMs = 60_000L, startedAt = hoursAgo(6))
        )

        val recap = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertEquals(3, recap.topTracks.size)
        assertEquals("t1", recap.topTracks[0].trackId)
        assertEquals(1, recap.topTracks[0].rank)
        assertEquals(3, recap.topTracks[0].plays)

        assertEquals("t2", recap.topTracks[1].trackId)
        assertEquals(2, recap.topTracks[1].rank)
        assertEquals(2, recap.topTracks[1].plays)

        assertEquals("t3", recap.topTracks[2].trackId)
        assertEquals(3, recap.topTracks[2].rank)
        assertEquals(1, recap.topTracks[2].plays)
    }

    @Test
    fun topArtistsAggregatesPlaysAndTracks() {
        val events = listOf(
            event(trackId = "t1", title = "Song 1", artist = "Artist A", listenedMs = 60_000L, startedAt = hoursAgo(1)),
            event(trackId = "t2", title = "Song 2", artist = "Artist A", listenedMs = 60_000L, startedAt = hoursAgo(2)),
            event(trackId = "t3", title = "Song 3", artist = "Artist B", listenedMs = 120_000L, startedAt = hoursAgo(3))
        )

        val recap = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertEquals(2, recap.topArtists.size)
        assertEquals("Artist A", recap.topArtists[0].name)
        assertEquals(1, recap.topArtists[0].rank)
        assertEquals(2, recap.topArtists[0].plays)
        assertEquals(2, recap.topArtists[0].trackCount)

        assertEquals("Artist B", recap.topArtists[1].name)
        assertEquals(2, recap.topArtists[1].rank)
        assertEquals(1, recap.topArtists[1].plays)
    }

    @Test
    fun topAlbumsIgnoresBlankAndRanksCorrectly() {
        val events = listOf(
            event(trackId = "t1", title = "Track 1", artist = "Artist A", album = "Album One", listenedMs = 60_000L, startedAt = hoursAgo(1)),
            event(trackId = "t2", title = "Track 2", artist = "Artist A", album = "Album One", listenedMs = 60_000L, startedAt = hoursAgo(2)),
            event(trackId = "t3", title = "Track 3", artist = "Artist B", album = "", listenedMs = 120_000L, startedAt = hoursAgo(3))
        )

        val recap = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertEquals(1, recap.topAlbums.size)
        assertEquals("Album One", recap.topAlbums[0].title)
        assertEquals("Artist A", recap.topAlbums[0].artist)
        assertEquals(2, recap.topAlbums[0].plays)
    }

    @Test
    fun daypartAndPeakHourCalculation() {
        val at14 = ZonedDateTime.of(2026, 7, 10, 14, 15, 0, 0, zone).toInstant().toEpochMilli()
        val at15 = ZonedDateTime.of(2026, 7, 10, 15, 10, 0, 0, zone).toInstant().toEpochMilli()
        val events = listOf(
            event(listenedMs = 100_000L, startedAt = at14),
            event(listenedMs = 200_000L, startedAt = at15)
        )

        val recap = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertEquals(Daypart.Afternoon, recap.highlights.favoriteDaypart)
        assertEquals(15, recap.highlights.favoriteHour)
    }

    @Test
    fun streakAndDiscoveryCalculation() {
        val day0 = ZonedDateTime.of(2026, 7, 10, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val day1 = ZonedDateTime.of(2026, 7, 9, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val day2 = ZonedDateTime.of(2026, 7, 8, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

        val events = listOf(
            event(trackId = "unique1", listenedMs = 60_000L, startedAt = daysAgo(20)),
            event(trackId = "unique1", listenedMs = 60_000L, startedAt = day0),
            event(trackId = "unique2", listenedMs = 60_000L, startedAt = day1),
            event(trackId = "unique1", listenedMs = 60_000L, startedAt = day2)
        )

        val recap = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertEquals(3, recap.highlights.currentStreakDays)
        assertEquals(3, recap.highlights.bestStreakDays)
        assertEquals(50, recap.highlights.discoveryRate)
        assertEquals(50, recap.highlights.repeatRate)
        assertNotNull(recap.highlights.mostReplayedTrack)
        assertEquals("unique1", recap.highlights.mostReplayedTrack?.trackId)
        assertEquals(2, recap.highlights.mostReplayedTrack?.plays)
    }

    @Test
    fun allTimePrefersLifetimeAggregateWhenGreater() {
        val lifetime = LifetimeListening(
            totalListenMs = 999_999_999L,
            countedPlays = 500,
            completedCount = 50,
            eventCount = 100,
            distinctTracks = 50,
            distinctArtists = 10,
            tracks = listOf(PulseTrack("t-all", "Lifetime Track", "Lifetime Artist", 200, 500_000_000L)),
            artists = listOf(LifetimeArtist("Lifetime Artist", 200, 500_000_000L))
        )
        val windowEvents = listOf(
            event(trackId = "t-recent", title = "Recent", artist = "Recent Artist", listenedMs = 60_000L, startedAt = hoursAgo(1))
        )

        val recap = ListeningRecapEngine.build(windowEvents, ListeningRecapPeriod.AllTime, lifetime = lifetime, nowMs = now, zone = zone)

        assertEquals(999_999_999L, recap.totalListenMs)
        assertEquals(500, recap.totalPlays)
        assertEquals("t-all", recap.topTracks.first().trackId)
        assertEquals("Lifetime Artist", recap.topArtists.first().name)
    }

    @Test
    fun topAlbumsGroupsFeaturingTracks() {
        val events = listOf(
            event(trackId = "t1", title = "Track 1", artist = "Daft Punk", album = "Random Access Memories", listenedMs = 60_000L, startedAt = hoursAgo(1)),
            event(trackId = "t2", title = "Track 2", artist = "Daft Punk feat. Pharrell Williams", album = "Random Access Memories", listenedMs = 60_000L, startedAt = hoursAgo(2)),
            event(trackId = "t3", title = "Track 3", artist = "Daft Punk with Julian Casablancas", album = "Random Access Memories", listenedMs = 60_000L, startedAt = hoursAgo(3))
        )

        val recap = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertEquals(1, recap.topAlbums.size)
        assertEquals("Random Access Memories", recap.topAlbums[0].title)
        assertEquals("Daft Punk", recap.topAlbums[0].artist)
        assertEquals(3, recap.topAlbums[0].plays)
        assertEquals(3, recap.topAlbums[0].trackCount)
    }

    @Test
    fun topArtistsDoesNotUseTrackArtwork() {
        val events = listOf(
            event(trackId = "t1", title = "Song 1", artist = "Artist A", thumbnailUrl = "https://image.test/album.jpg", listenedMs = 60_000L, startedAt = hoursAgo(1))
        )

        val recap = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertEquals(1, recap.topArtists.size)
        assertEquals("Artist A", recap.topArtists[0].name)
        assertTrue(recap.topArtists[0].thumbnailUrl.isEmpty())
    }

    @Test
    fun discoveryRateReturnsMinusOneWhenNoPriorHistoryOrYearPeriod() {
        val events = listOf(
            event(trackId = "t1", listenedMs = 60_000L, startedAt = daysAgo(2)),
            event(trackId = "t2", listenedMs = 60_000L, startedAt = daysAgo(3))
        )

        val recap7 = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)
        assertEquals(-1, recap7.highlights.discoveryRate)
        assertEquals(-1, recap7.highlights.repeatRate)

        val recap365 = ListeningRecapEngine.build(events, ListeningRecapPeriod.Days365, nowMs = now, zone = zone)
        assertEquals(-1, recap365.highlights.discoveryRate)

        val recapAll = ListeningRecapEngine.build(events, ListeningRecapPeriod.AllTime, nowMs = now, zone = zone)
        assertEquals(-1, recapAll.highlights.discoveryRate)
    }

    @Test
    fun allTimeWithOlderLifetimeHistoryDoesNotExposeWindowAlbumsOrBestStreak() {
        val lifetime = LifetimeListening(
            totalListenMs = 10_000_000L,
            countedPlays = 100,
            completedCount = 50,
            eventCount = 100,
            distinctTracks = 40,
            distinctArtists = 10,
            tracks = listOf(
                PulseTrack("t-all-1", "Lifetime Song 1", "Artist L", 30, 3_000_000L),
                PulseTrack("t-all-2", "Lifetime Song 2", "Artist L", 20, 2_000_000L)
            ),
            artists = listOf(LifetimeArtist("Artist L", 50, 5_000_000L))
        )
        val windowEvents = listOf(
            event(trackId = "t-recent", title = "Recent", artist = "Recent Artist", album = "Recent Album", listenedMs = 60_000L, startedAt = hoursAgo(1))
        )

        val recap = ListeningRecapEngine.build(windowEvents, ListeningRecapPeriod.AllTime, lifetime = lifetime, nowMs = now, zone = zone)

        assertTrue(recap.highlights.isWindowBounded)
        assertEquals(0, recap.uniqueAlbums)
        assertTrue(recap.topAlbums.isEmpty())
        assertEquals(0, recap.highlights.bestStreakDays)
        assertEquals(null, recap.highlights.favoriteDaypart)
        assertEquals(-1, recap.highlights.favoriteHour)
        assertEquals(-1, recap.highlights.discoveryRate)
        assertEquals(null, recap.highlights.mostReplayedTrack)
        assertEquals(50, recap.completionRate)
    }

    @Test
    fun mostReplayedTrackFindsTrackOutsideTopDurationTracks() {
        val longTracks = (1..5).map { i ->
            event(
                trackId = "long-$i",
                title = "Long Track $i",
                artist = "Artist",
                listenedMs = 600_000L,
                startedAt = hoursAgo(i)
            )
        }
        val shortRepeatedTracks = (1..10).map { i ->
            event(
                trackId = "short-repeated",
                title = "Short Track",
                artist = "Artist",
                listenedMs = 30_000L,
                startedAt = hoursAgo(10 + i)
            )
        }

        val recap = ListeningRecapEngine.build(longTracks + shortRepeatedTracks, ListeningRecapPeriod.Days7, nowMs = now, zone = zone)

        assertEquals(5, recap.topTracks.size)
        assertTrue(recap.topTracks.none { it.trackId == "short-repeated" })
        assertEquals("short-repeated", recap.highlights.mostReplayedTrack?.trackId)
        assertEquals(10, recap.highlights.mostReplayedTrack?.plays)
    }

    @Test
    fun lifetimeThumbnailFallbackKeepsNonBlankArtwork() {
        val lifetime = LifetimeListening(
            totalListenMs = 1_000_000L,
            countedPlays = 20,
            completedCount = 10,
            eventCount = 20,
            distinctTracks = 1,
            distinctArtists = 1,
            tracks = listOf(
                PulseTrack("track-1", "Song One", "Artist", 20, 1_000_000L)
            ),
            artists = listOf(LifetimeArtist("Artist", 20, 1_000_000L))
        )
        val windowEvents = listOf(
            event(trackId = "track-1", thumbnailUrl = "https://example.com/art.jpg", listenedMs = 60_000L, startedAt = hoursAgo(2)),
            event(trackId = "track-1", thumbnailUrl = "", listenedMs = 60_000L, startedAt = hoursAgo(1))
        )

        val recap = ListeningRecapEngine.build(windowEvents, ListeningRecapPeriod.AllTime, lifetime = lifetime, nowMs = now, zone = zone)

        assertEquals("https://example.com/art.jpg", recap.topTracks.first().thumbnailUrl)
    }

    @Test
    fun allTimeDerivesLifetimeCompletionRateAndAverageMinutes() {
        val firstPlay = ZonedDateTime.of(2026, 7, 1, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val lastPlay = ZonedDateTime.of(2026, 7, 10, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val lifetime = LifetimeListening(
            totalListenMs = 60_000_000L,
            countedPlays = 50,
            completedCount = 40,
            eventCount = 50,
            distinctTracks = 20,
            distinctArtists = 5,
            firstPlayedAt = firstPlay,
            lastPlayedAt = lastPlay
        )
        val windowEvents = listOf(
            event(trackId = "recent", listenedMs = 60_000L, startedAt = hoursAgo(1))
        )

        val recap = ListeningRecapEngine.build(windowEvents, ListeningRecapPeriod.AllTime, lifetime = lifetime, nowMs = now, zone = zone)

        assertEquals(80, recap.completionRate)
        assertEquals(100L, recap.highlights.averageMinutesPerDay)
    }

    private fun hoursAgo(hours: Int): Long = now - hours * 3_600_000L
    private fun daysAgo(days: Int): Long = now - days * 86_400_000L

    private fun event(
        trackId: String = "track",
        title: String = "Title",
        artist: String = "Artist",
        album: String = "Album",
        thumbnailUrl: String = "",
        listenedMs: Long = 30_000L,
        durationMs: Long = 180_000L,
        completed: Boolean = false,
        startedAt: Long
    ): ListenEvent = ListenEvent(
        trackId = trackId,
        title = title,
        artist = artist,
        album = album,
        thumbnailUrl = thumbnailUrl,
        listenedMs = listenedMs,
        trackDurationMs = durationMs,
        completed = completed,
        startedAt = startedAt
    )
}
