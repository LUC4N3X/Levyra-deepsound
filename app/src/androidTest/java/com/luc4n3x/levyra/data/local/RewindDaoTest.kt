package com.luc4n3x.levyra.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luc4n3x.levyra.domain.ListenPlayPolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RewindDaoTest {
    @Test fun yearsAndAlbumsUseSavedEventsAndRespectYearBoundaries() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LevyraDatabase::class.java).build()
        try {
            val dao = database.listenEventsDao()
            dao.insert(event("a", "Album", "Artist", "2025-12-31", 90_000L))
            dao.insert(event("a", "Album", "Artist", "2026-01-01", 30_000L))
            dao.insert(event("b", " album ", " artist ", "2026-10-01", 60_000L))
            dao.insert(event("c", "Album", "Other Artist", "2026-10-01", 45_000L))
            dao.insert(event("d", "", "Artist", "2026-10-01", 60_000L))
            dao.insert(event("e", "Ignored", "Artist", "2024-10-01", 1L))
            assertEquals(listOf(2026, 2025), dao.rewindYears(ListenPlayPolicy.MIN_EVENT_MS))
            val albums = dao.rewindAlbums(epoch("2026-01-01"), epoch("2027-01-01"), ListenPlayPolicy.MIN_EVENT_MS,
                ListenPlayPolicy.COUNTED_PLAY_MS, ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR, ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR)
            assertEquals(2, albums.size)
            assertEquals(90_000L, albums[0].listenedMs)
            assertEquals(2, albums[0].plays)
            assertEquals(45_000L, albums[1].listenedMs)
            assertEquals(6, dao.count())
        } finally { database.close() }
    }

    private fun epoch(date: String) = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun event(id: String, album: String, artist: String, date: String, duration: Long) = ListenEventEntity(
        trackId = id, title = id, artist = artist, album = album, durationMs = 180_000L,
        videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "test", listenedMs = duration,
        completed = false, startedAt = epoch(date)
    )
}
