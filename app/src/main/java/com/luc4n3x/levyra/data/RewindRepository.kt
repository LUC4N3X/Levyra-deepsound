package com.luc4n3x.levyra.data

import android.content.Context
import androidx.room.withTransaction
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.ListenPlayPolicy
import com.luc4n3x.levyra.domain.ListeningInsightsRanges
import com.luc4n3x.levyra.domain.RewindAlbum
import com.luc4n3x.levyra.domain.RewindSummary
import com.luc4n3x.levyra.domain.rewindMonths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class RewindRepository(context: Context) {
    private val database = LevyraDatabase.get(context.applicationContext)
    private val insights = ListeningInsightsRepository(context.applicationContext)

    suspend fun years(): List<Int> = withContext(Dispatchers.IO) {
        database.listenEventsDao().rewindYears(ListenPlayPolicy.MIN_EVENT_MS)
            .filter { it in 1970..LocalDate.now().year }
    }

    suspend fun summary(year: Int): RewindSummary = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val range = ListeningInsightsRanges.year(year, now, zone)
        database.withTransaction {
            val dao = database.listenEventsDao()
            val snapshot = insights.yearSnapshot(year, now, zone)
            val days = dao.insightsDays(
                range.fromMs, range.toMs, ListenPlayPolicy.MIN_EVENT_MS,
                ListenPlayPolicy.COUNTED_PLAY_MS, ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
                ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
            )
            val albums = dao.rewindAlbums(
                range.fromMs, range.toMs, ListenPlayPolicy.MIN_EVENT_MS,
                ListenPlayPolicy.COUNTED_PLAY_MS, ListenPlayPolicy.SHORT_TRACK_COMPLETION_NUMERATOR,
                ListenPlayPolicy.SHORT_TRACK_COMPLETION_DENOMINATOR
            ).map { RewindAlbum(it.title, it.artist, it.artworkUrl, it.listenedMs, it.plays) }
            RewindSummary(
                year, snapshot, albums,
                rewindMonths(days.map { LocalDate.parse(it.dayKey) to it.listenedMs }, year),
                days.size, days.firstOrNull()?.let { LocalDate.parse(it.dayKey) },
                days.lastOrNull()?.let { LocalDate.parse(it.dayKey) }
            )
        }
    }
}
