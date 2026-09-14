package com.luc4n3x.levyra.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luc4n3x.levyra.domain.ListenPlayPolicy
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListeningInsightsDaoTest {
    private lateinit var database: LevyraDatabase
    private lateinit var dao: ListenEventsDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LevyraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.listenEventsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aggregateUsesListenPlayPolicyThresholds() = runBlocking {
        dao.insert(event("a", "Artist A", 16_000L, 20_000L, completed = true, startedAt = 1_000L))
        dao.insert(event("a", "Artist A", 10_000L, 180_000L, completed = false, startedAt = 2_000L))
        dao.insert(event("b", "Artist B", 35_000L, 180_000L, completed = false, startedAt = 3_000L))

        val aggregate = dao.insightsAggregate(
            fromMs = 0L,
            toMs = 4_000L,
            minimumEventMs = ListenPlayPolicy.MIN_EVENT_MS,
            countedPlayMs = ListenPlayPolicy.COUNTED_PLAY_MS,
            completionNumerator = ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            completionDenominator = ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        )

        assertEquals(61_000L, aggregate.listenedMs)
        assertEquals(2L, aggregate.countedPlays)
        assertEquals(3L, aggregate.eventCount)
        assertEquals(2L, aggregate.distinctTracks)
        assertEquals(2L, aggregate.distinctArtists)
    }

    @Test
    fun historyUsesStableKeysetCursor() = runBlocking {
        dao.insert(event("a", "Artist", 30_000L, startedAt = 2_000L))
        dao.insert(event("b", "Artist", 30_000L, startedAt = 2_000L))
        dao.insert(event("c", "Artist", 30_000L, startedAt = 1_000L))

        val first = historyPage(beforeStartedAt = Long.MAX_VALUE, beforeId = Long.MAX_VALUE, limit = 2)
        val cursor = first.last()
        val second = historyPage(beforeStartedAt = cursor.startedAt, beforeId = cursor.id, limit = 2)

        assertEquals(2, first.size)
        assertEquals(1, second.size)
        assertEquals("c", second.single().trackId)
    }

    @Test
    fun timelineEventsOrderedChronologically() = runBlocking {
        dao.insert(event("b", "Artist B", 35_000L, startedAt = 2_000L))
        dao.insert(event("a", "Artist A", 10_000L, startedAt = 1_000L))

        val timeline = dao.insightsTimelineEvents(
            fromMs = 0L,
            toMs = 3_000L,
            minimumEventMs = ListenPlayPolicy.MIN_EVENT_MS,
            countedPlayMs = ListenPlayPolicy.COUNTED_PLAY_MS,
            completionNumerator = ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
            completionDenominator = ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
        )

        assertEquals(2, timeline.size)
        assertEquals(1_000L, timeline[0].startedAt)
        assertEquals(2_000L, timeline[1].startedAt)
        assertEquals(0L, timeline[0].countedPlays)
        assertEquals(1L, timeline[1].countedPlays)
    }

    @Test
    fun historySearchTreatsPercentAndUnderscoreLiterally() = runBlocking {
        dao.insert(event("1", "Artist", 30_000L, startedAt = 3_000L, title = "100% Pure"))
        dao.insert(event("2", "Artist_One", 30_000L, startedAt = 2_000L, title = "Normal Track"))
        dao.insert(event("3", "Artist Two", 30_000L, startedAt = 1_000L, title = "Other Track"))

        val percentResults = dao.insightsHistoryPage(
            fromMs = 0L,
            toMs = Long.MAX_VALUE,
            query = "%",
            beforeStartedAt = Long.MAX_VALUE,
            beforeId = Long.MAX_VALUE,
            limit = 10,
            minimumEventMs = ListenPlayPolicy.MIN_EVENT_MS
        )
        assertEquals(1, percentResults.size)
        assertEquals("1", percentResults.single().trackId)

        val underscoreResults = dao.insightsHistoryPage(
            fromMs = 0L,
            toMs = Long.MAX_VALUE,
            query = "_",
            beforeStartedAt = Long.MAX_VALUE,
            beforeId = Long.MAX_VALUE,
            limit = 10,
            minimumEventMs = ListenPlayPolicy.MIN_EVENT_MS
        )
        assertEquals(1, underscoreResults.size)
        assertEquals("2", underscoreResults.single().trackId)
    }

    private suspend fun historyPage(beforeStartedAt: Long, beforeId: Long, limit: Int) =
        dao.insightsHistoryPage(
            fromMs = 0L,
            toMs = Long.MAX_VALUE,
            query = "",
            beforeStartedAt = beforeStartedAt,
            beforeId = beforeId,
            limit = limit,
            minimumEventMs = ListenPlayPolicy.MIN_EVENT_MS
        )

    private fun event(
        trackId: String,
        artist: String,
        listenedMs: Long,
        durationMs: Long = 180_000L,
        completed: Boolean = false,
        startedAt: Long,
        title: String = "Track $trackId"
    ) = ListenEventEntity(
        trackId = trackId,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = durationMs,
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        listenedMs = listenedMs,
        completed = completed,
        startedAt = startedAt
    )
}
