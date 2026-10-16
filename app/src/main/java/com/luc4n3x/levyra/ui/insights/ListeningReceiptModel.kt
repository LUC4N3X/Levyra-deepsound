package com.luc4n3x.levyra.ui.insights

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.ListeningInsightsPeriod
import com.luc4n3x.levyra.domain.ListeningInsightsRange
import com.luc4n3x.levyra.domain.ListeningInsightsRanges
import com.luc4n3x.levyra.domain.ListeningInsightsSnapshot
import java.text.BreakIterator
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal enum class ListeningReceiptStyle { Aurora, Noir, Paper }

@Immutable
internal data class ListeningReceiptLabels(
    val brand: String,
    val title: String,
    val periodShort: String,
    val lifetime: String,
    val minutesListened: String,
    val topTracks: String,
    val topArtist: String,
    val plays: String,
    val tracks: String,
    val artists: String,
    val peakHour: String
)

@Immutable
internal data class ListeningReceiptTrackLine(
    val title: String,
    val artist: String,
    val artworkIndex: Int = -1
)

@Immutable
internal data class ListeningReceiptStat(
    val value: String,
    val label: String
)

@Immutable
internal data class ListeningReceiptContent(
    val brand: String,
    val title: String,
    val periodShort: String,
    val dateRange: String,
    val minutesValue: String,
    val minutesLabel: String,
    val topTracksLabel: String,
    val topTracks: List<ListeningReceiptTrackLine>,
    val topArtistLabel: String,
    val topArtists: List<String>,
    val topArtistArtworkUrl: String,
    val stats: List<ListeningReceiptStat>,
    val peakLine: String,
    val waveform: List<Float>,
    val artworkUrls: List<String>,
    val coverInitials: List<String>,
    val signature: Long
)

internal object ListeningReceiptModel {
    const val MAX_TRACKS = 5
    const val MAX_ARTISTS = 3
    const val MAX_COVERS = 3
    const val MAX_WAVEFORM_BARS = 44
    private const val MIN_ACTIVITY_BARS = 12
    private const val MAX_TEXT_CODE_POINTS = 120

    fun content(
        snapshot: ListeningInsightsSnapshot,
        labels: ListeningReceiptLabels,
        locale: Locale,
        nowMs: Long,
        zone: ZoneId,
        range: ListeningInsightsRange? = null
    ): ListeningReceiptContent? {
        if (!snapshot.hasSignal) return null
        val number = NumberFormat.getIntegerInstance(locale)
        val metrics = snapshot.metrics
        val tracks = snapshot.topTracks.filter { it.title.isNotBlank() }.take(MAX_TRACKS)
        val coverTracks = tracks.filter { it.artworkUrl.isNotBlank() }.distinctBy { it.artworkUrl }
        val artworkUrls = coverTracks.map { it.artworkUrl }.ifEmpty {
            snapshot.topArtists.map { it.artworkUrl }.filter(String::isNotBlank).distinct().take(MAX_COVERS)
        }
        val initialsSource = coverTracks.take(MAX_COVERS).map { it.title }
            .ifEmpty { tracks.map { it.title } }
            .ifEmpty { snapshot.topArtists.map { it.name } }
        val activity = snapshot.activity.map { it.listenedMs }
        return ListeningReceiptContent(
            brand = labels.brand,
            title = labels.title,
            periodShort = labels.periodShort,
            dateRange = if (range != null) {
                formatRange(range.fromMs, minOf(range.toMs - 1L, nowMs), locale, zone)
            } else {
                dateRange(snapshot.period, labels.lifetime, locale, nowMs, zone)
            },
            minutesValue = number.format(metrics.listenedMs.coerceAtLeast(0L) / 60_000L),
            minutesLabel = labels.minutesListened,
            topTracksLabel = labels.topTracks,
            topTracks = tracks.map { track ->
                ListeningReceiptTrackLine(
                    title = bounded(track.title),
                    artist = bounded(track.artist),
                    artworkIndex = if (coverTracks.isEmpty()) -1 else artworkUrls.indexOf(track.artworkUrl)
                )
            },
            topArtistLabel = labels.topArtist,
            topArtists = snapshot.topArtists.map { bounded(it.name) }.filter(String::isNotBlank).take(MAX_ARTISTS),
            topArtistArtworkUrl = snapshot.topArtists.firstOrNull { it.name.isNotBlank() }?.artworkUrl.orEmpty(),
            stats = listOf(
                ListeningReceiptStat(number.format(metrics.countedPlays), labels.plays),
                ListeningReceiptStat(number.format(metrics.distinctTracks), labels.tracks),
                ListeningReceiptStat(number.format(metrics.distinctArtists), labels.artists)
            ),
            peakLine = if (metrics.peakHour in 0..23) {
                "${labels.peakHour} · ${String.format(locale, "%02d", metrics.peakHour)}:00"
            } else {
                ""
            },
            waveform = waveform(
                if (activity.size >= MIN_ACTIVITY_BARS && activity.count { it > 0L } * 4 >= activity.size) activity else snapshot.hourBuckets
            ),
            artworkUrls = artworkUrls,
            coverInitials = initialsSource.take(MAX_COVERS).map(::initial),
            signature = signature(snapshot)
        )
    }

    fun waveform(values: List<Long>, maxBars: Int = MAX_WAVEFORM_BARS): List<Float> {
        if (values.isEmpty() || maxBars <= 0) return emptyList()
        val buckets = if (values.size <= maxBars) {
            values.map { it.coerceAtLeast(0L) }
        } else {
            List(maxBars) { bucket ->
                val from = bucket * values.size / maxBars
                val to = ((bucket + 1) * values.size / maxBars).coerceAtLeast(from + 1)
                values.subList(from, to).sumOf { it.coerceAtLeast(0L) }
            }
        }
        val peak = buckets.max()
        if (peak <= 0L) return List(buckets.size) { 0f }
        return buckets.map { (it.toDouble() / peak).toFloat() }
    }

    fun initial(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "♪"
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(trimmed)
        val end = iterator.next().takeIf { it != BreakIterator.DONE } ?: trimmed.length
        return trimmed.substring(0, end).uppercase()
    }

    private fun bounded(text: String): String {
        val trimmed = text.trim()
        val count = trimmed.codePointCount(0, trimmed.length)
        if (count <= MAX_TEXT_CODE_POINTS) return trimmed
        return trimmed.substring(0, trimmed.offsetByCodePoints(0, MAX_TEXT_CODE_POINTS)).trimEnd()
    }

    private fun dateRange(
        period: ListeningInsightsPeriod,
        lifetime: String,
        locale: Locale,
        nowMs: Long,
        zone: ZoneId
    ): String {
        if (period == ListeningInsightsPeriod.AllTime) return lifetime
        return formatRange(ListeningInsightsRanges.current(period, nowMs, zone).fromMs, nowMs, locale, zone)
    }

    private fun formatRange(fromMs: Long, toMs: Long, locale: Locale, zone: ZoneId): String {
        val from = Instant.ofEpochMilli(fromMs).atZone(zone).toLocalDate()
        val to = Instant.ofEpochMilli(toMs).atZone(zone).toLocalDate()
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        return if (from == to) to.format(formatter) else "${from.format(formatter)} – ${to.format(formatter)}"
    }

    private fun signature(snapshot: ListeningInsightsSnapshot): Long {
        var hash = 1125899906842597L
        val parts = snapshot.topTracks.take(MAX_TRACKS).map { it.trackId.ifBlank { it.title } } +
            snapshot.metrics.listenedMs.toString() + snapshot.period.name
        parts.forEach { part -> part.forEach { hash = 31 * hash + it.code } }
        return hash
    }
}
