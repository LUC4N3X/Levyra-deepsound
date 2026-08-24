package com.luc4n3x.levyra.domain

enum class ListeningDnaPeriod(val days: Int) {
    Week(7),
    Month(30),
    HalfYear(182),
    AllTime(0)
}

data class DnaArtist(
    val name: String,
    val plays: Int,
    val listenedMs: Long,
    val weight: Float
)

data class ListeningDna(
    val period: ListeningDnaPeriod = ListeningDnaPeriod.Month,
    val totalListenMs: Long = 0L,
    val plays: Int = 0,
    val distinctTracks: Int = 0,
    val distinctArtists: Int = 0,
    val completionRate: Int = 0,
    val discoveryRate: Int = 0,
    val peakHour: Int = -1,
    val artists: List<DnaArtist> = emptyList(),
    val tracks: List<PulseTrack> = emptyList(),
    val hourBuckets: List<Long> = emptyList()
) {
    val hasSignal: Boolean
        get() = plays > 0 || totalListenMs > 0L

    val totalMinutes: Long
        get() = totalListenMs / 60_000L
}

object ListeningDnaEngine {

    private const val ArtistLimit = 8
    private const val TrackLimit = 8
    private const val HoursPerDay = 24
    private const val DayMs = 24L * 60L * 60L * 1000L
    private const val DiscoveryReferenceDays = 30L

    fun build(
        events: List<ListenEvent>,
        period: ListeningDnaPeriod,
        nowMs: Long = System.currentTimeMillis(),
        zoneOffsetMs: Long = 0L
    ): ListeningDna {
        val valid = events.filter {
            it.listenedMs >= ListeningPulseEngine.MIN_LISTEN_MS && it.startedAt in 1..nowMs
        }
        if (valid.isEmpty()) return ListeningDna(period = period, hourBuckets = List(HoursPerDay) { 0L })

        val firstSeen = HashMap<String, Long>(valid.size)
        for (event in valid) {
            val key = trackKey(event)
            val previous = firstSeen[key]
            if (previous == null || event.startedAt < previous) firstSeen[key] = event.startedAt
        }

        val windowStart = if (period.days <= 0) 0L else nowMs - period.days * DayMs
        val discoveryStart = if (windowStart > 0L) windowStart else nowMs - DiscoveryReferenceDays * DayMs
        val scoped = if (windowStart <= 0L) valid else valid.filter { it.startedAt >= windowStart }
        if (scoped.isEmpty()) return ListeningDna(period = period, hourBuckets = List(HoursPerDay) { 0L })

        val hourBuckets = LongArray(HoursPerDay)
        val artistPlays = LinkedHashMap<String, ArtistAccumulator>()
        val trackPlays = LinkedHashMap<String, TrackAccumulator>()
        val distinctTracks = HashSet<String>(scoped.size)
        var totalListenMs = 0L
        var completed = 0
        var discovered = 0

        for (event in scoped) {
            totalListenMs += event.listenedMs
            if (event.completed) completed += 1
            val hour = (((event.startedAt + zoneOffsetMs) / 3_600_000L) % HoursPerDay).toInt()
            hourBuckets[if (hour < 0) hour + HoursPerDay else hour] += event.listenedMs

            val key = trackKey(event)
            distinctTracks.add(key)
            if ((firstSeen[key] ?: event.startedAt) >= discoveryStart) discovered += 1

            val trackAccumulator = trackPlays.getOrPut(key) {
                TrackAccumulator(event.trackId, event.title, event.artist, event.startedAt)
            }
            trackAccumulator.add(event)

            val artistKey = event.artist.trim().lowercase()
            if (artistKey.isNotEmpty()) {
                artistPlays.getOrPut(artistKey) { ArtistAccumulator(event.artist.trim()) }.add(event)
            }
        }

        val artistTotal = artistPlays.values.sumOf { it.listenedMs }.coerceAtLeast(1L)
        val artists = artistPlays.values
            .sortedWith(compareByDescending<ArtistAccumulator> { it.listenedMs }.thenByDescending { it.plays })
            .take(ArtistLimit)
            .map {
                DnaArtist(
                    name = it.name,
                    plays = it.plays,
                    listenedMs = it.listenedMs,
                    weight = (it.listenedMs.toFloat() / artistTotal.toFloat()).coerceIn(0f, 1f)
                )
            }

        val tracks = trackPlays.values
            .sortedWith(compareByDescending<TrackAccumulator> { it.listenedMs }.thenByDescending { it.plays })
            .take(TrackLimit)
            .map { PulseTrack(it.trackId, it.title, it.artist, it.plays, it.listenedMs) }

        var peakHour = -1
        var peakValue = 0L
        for (hour in 0 until HoursPerDay) {
            if (hourBuckets[hour] > peakValue) {
                peakValue = hourBuckets[hour]
                peakHour = hour
            }
        }

        return ListeningDna(
            period = period,
            totalListenMs = totalListenMs,
            plays = scoped.count { isCountedPlay(it) },
            distinctTracks = distinctTracks.size,
            distinctArtists = artistPlays.size,
            completionRate = (completed * 100) / scoped.size,
            discoveryRate = (discovered * 100) / scoped.size,
            peakHour = peakHour,
            artists = artists,
            tracks = tracks,
            hourBuckets = hourBuckets.toList()
        )
    }

    private fun trackKey(event: ListenEvent): String =
        event.trackId.trim().ifEmpty {
            "${event.title.trim().lowercase()}|${event.artist.trim().lowercase()}"
        }

    private class ArtistAccumulator(val name: String) {
        var plays: Int = 0
        var listenedMs: Long = 0L

        fun add(event: ListenEvent) {
            if (isCountedPlay(event)) {
                plays += 1
            }
            listenedMs += event.listenedMs
        }
    }

    private class TrackAccumulator(
        val trackId: String,
        val title: String,
        val artist: String,
        val startedAt: Long
    ) {
        var plays: Int = 0
        var listenedMs: Long = 0L

        fun add(event: ListenEvent) {
            if (isCountedPlay(event)) {
                plays += 1
            }
            listenedMs += event.listenedMs
        }
    }
}
