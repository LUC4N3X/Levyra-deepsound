package com.luc4n3x.levyra.domain

data class PulseArtistShare(
    val name: String,
    val listenedMs: Long,
    val fraction: Float
)

object ListeningChartProjection {

    const val HOURS_PER_DAY = 24
    const val WEEK_DAYS = 7
    const val MIN_BAR_FRACTION = 0.06f
    const val MAX_RING_ARTISTS = 5

    fun weekFractions(week: List<PulseDay>, minPeakMs: Long): List<Float> {
        if (week.isEmpty()) return List(WEEK_DAYS) { 0f }
        val peak = week.maxOf { it.listenedMs }
        val scale = maxOf(peak, minPeakMs)
        if (scale <= 0L) return week.map { 0f }
        return week.map { day ->
            if (day.listenedMs <= 0L) {
                0f
            } else {
                (day.listenedMs.toFloat() / scale.toFloat()).coerceIn(MIN_BAR_FRACTION, 1f)
            }
        }
    }

    fun peakDayIndex(week: List<PulseDay>): Int {
        var index = -1
        var best = 0L
        week.forEachIndexed { position, day ->
            if (day.listenedMs > best) {
                best = day.listenedMs
                index = position
            }
        }
        return index
    }

    fun hourFractions(hourBuckets: List<Long>): List<Float> {
        val normalized = List(HOURS_PER_DAY) { hour -> hourBuckets.getOrElse(hour) { 0L }.coerceAtLeast(0L) }
        val peak = normalized.maxOrNull() ?: 0L
        if (peak <= 0L) return List(HOURS_PER_DAY) { 0f }
        return normalized.map { value ->
            if (value <= 0L) 0f else (value.toFloat() / peak.toFloat()).coerceIn(MIN_BAR_FRACTION, 1f)
        }
    }

    fun peakHourIndex(hourBuckets: List<Long>): Int {
        var index = -1
        var best = 0L
        for (hour in 0 until HOURS_PER_DAY) {
            val value = hourBuckets.getOrElse(hour) { 0L }
            if (value > best) {
                best = value
                index = hour
            }
        }
        return index
    }

    fun artistShares(artists: List<PulseArtist>, limit: Int = MAX_RING_ARTISTS): List<PulseArtistShare> {
        val bounded = artists
            .filter { it.name.isNotBlank() && it.listenedMs > 0L }
            .sortedByDescending { it.listenedMs }
            .take(limit.coerceIn(1, MAX_RING_ARTISTS))
        val total = bounded.sumOf { it.listenedMs }
        if (total <= 0L) return emptyList()
        return bounded.map { artist ->
            PulseArtistShare(
                name = artist.name,
                listenedMs = artist.listenedMs,
                fraction = (artist.listenedMs.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
            )
        }
    }
}
