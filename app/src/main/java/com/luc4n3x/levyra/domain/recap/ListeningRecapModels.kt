package com.luc4n3x.levyra.domain.recap

import androidx.compose.runtime.Immutable
import java.time.LocalDate

enum class ListeningRecapPeriod(val days: Int) {
    Days7(7),
    Days30(30),
    Days365(365),
    AllTime(0)
}

enum class Daypart {
    Morning,
    Afternoon,
    Evening,
    Night
}

@Immutable
data class TopTrackStat(
    val rank: Int,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val thumbnailUrl: String = "",
    val plays: Int = 0,
    val listenedMs: Long = 0L
) {
    val totalMinutes: Long
        get() = listenedMs / 60_000L
}

@Immutable
data class TopArtistStat(
    val rank: Int,
    val name: String,
    val browseId: String = "",
    val lookupName: String = "",
    val plays: Int = 0,
    val listenedMs: Long = 0L,
    val thumbnailUrl: String = "",
    val trackCount: Int = 0
) {
    val totalMinutes: Long
        get() = listenedMs / 60_000L
}

@Immutable
data class TopAlbumStat(
    val rank: Int,
    val title: String,
    val artist: String,
    val thumbnailUrl: String = "",
    val plays: Int = 0,
    val listenedMs: Long = 0L,
    val trackCount: Int = 0
) {
    val totalMinutes: Long
        get() = listenedMs / 60_000L
}

@Immutable
data class RecapHighlightStat(
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val averageMinutesPerDay: Long = 0L,
    val mostActiveDayDate: LocalDate? = null,
    val mostActiveDayMinutes: Long = 0L,
    val favoriteHour: Int = -1,
    val favoriteDaypart: Daypart? = null,
    val mostReplayedTrack: TopTrackStat? = null,
    val discoveryRate: Int = -1,
    val repeatRate: Int = -1,
    val isWindowBounded: Boolean = false
)

@Immutable
data class ListeningPulsePoint(
    val date: LocalDate,
    val listenedMs: Long = 0L,
    val plays: Int = 0
) {
    val minutes: Long
        get() = listenedMs / 60_000L
}

@Immutable
data class ListeningRecapSummary(
    val period: ListeningRecapPeriod = ListeningRecapPeriod.Days30,
    val totalListenMs: Long = 0L,
    val totalPlays: Int = 0,
    val uniqueTracks: Int = 0,
    val uniqueArtists: Int = 0,
    val uniqueAlbums: Int = 0,
    val completionRate: Int = 0,
    val highlights: RecapHighlightStat = RecapHighlightStat(),
    val topTracks: List<TopTrackStat> = emptyList(),
    val topArtists: List<TopArtistStat> = emptyList(),
    val topAlbums: List<TopAlbumStat> = emptyList(),
    val dailyActivity: List<ListeningPulsePoint> = emptyList()
) {
    val hasSignal: Boolean
        get() = totalPlays > 0 || totalListenMs > 0L

    val totalMinutes: Long
        get() = totalListenMs / 60_000L
}
