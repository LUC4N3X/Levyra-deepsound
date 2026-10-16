package com.luc4n3x.levyra.ui.insights

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.ListeningInsightsActivityPoint
import com.luc4n3x.levyra.domain.ListeningInsightsRanges
import com.luc4n3x.levyra.ui.i18n.listeningReceiptBundles
import com.luc4n3x.levyra.ui.i18n.listeningReceiptKeys
import com.luc4n3x.levyra.domain.ListeningInsightsArtist
import com.luc4n3x.levyra.domain.ListeningInsightsMetrics
import com.luc4n3x.levyra.domain.ListeningInsightsPeriod
import com.luc4n3x.levyra.domain.ListeningInsightsSnapshot
import com.luc4n3x.levyra.domain.ListeningInsightsTrack
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningReceiptModelTest {
    private val labels = ListeningReceiptLabels(
        brand = "LEVYRA",
        title = "Listening Receipt",
        periodShort = "7D",
        lifetime = "LIFETIME",
        minutesListened = "MINUTES LISTENED",
        topTracks = "TOP TRACKS",
        topArtist = "TOP ARTIST",
        plays = "plays",
        tracks = "tracks",
        artists = "artists",
        peakHour = "Peak hour"
    )
    private val zone = ZoneId.of("UTC")
    private val now = 1_760_000_000_000L

    @Test
    fun emptySnapshotProducesNoReceipt() {
        assertNull(ListeningReceiptModel.content(ListeningInsightsSnapshot(), labels, Locale.US, now, zone))
    }

    @Test
    fun receiptUsesOnlyRealSnapshotData() {
        val tracks = (1..7).map { index ->
            ListeningInsightsTrack(
                trackId = "t$index",
                title = "Song $index",
                artist = "Artist $index",
                artworkUrl = if (index == 2) "" else "https://img/$index"
            )
        }
        val snapshot = ListeningInsightsSnapshot(
            period = ListeningInsightsPeriod.Week,
            metrics = ListeningInsightsMetrics(
                listenedMs = 125L * 60_000L + 59_000L,
                countedPlays = 1234,
                distinctTracks = 56,
                distinctArtists = 7,
                peakHour = 22
            ),
            topTracks = tracks,
            topArtists = listOf("Ana", "Bea", "Cy", "Dan").map { ListeningInsightsArtist(name = it) }
        )

        val content = requireNotNull(ListeningReceiptModel.content(snapshot, labels, Locale.US, now, zone))

        assertEquals("125", content.minutesValue)
        assertEquals(ListeningReceiptModel.MAX_TRACKS, content.topTracks.size)
        assertEquals("Song 1", content.topTracks.first().title)
        assertEquals(listOf("Ana", "Bea", "Cy"), content.topArtists)
        assertEquals(listOf("https://img/1", "https://img/3", "https://img/4", "https://img/5"), content.artworkUrls)
        assertEquals(listOf(0, -1, 1, 2, 3), content.topTracks.map { it.artworkIndex })
        assertEquals(listOf("1,234", "56", "7"), content.stats.map { it.value })
        assertEquals("Peak hour · 22:00", content.peakLine)
        assertTrue(content.dateRange.contains("–"))
    }

    @Test
    fun allTimeUsesLifetimeLabelAndUnknownPeakIsHidden() {
        val snapshot = ListeningInsightsSnapshot(
            period = ListeningInsightsPeriod.AllTime,
            metrics = ListeningInsightsMetrics(listenedMs = 30_000L, countedPlays = 1, peakHour = -1)
        )
        val content = requireNotNull(ListeningReceiptModel.content(snapshot, labels, Locale.US, now, zone))
        assertEquals("LIFETIME", content.dateRange)
        assertEquals("", content.peakLine)
        assertEquals("0", content.minutesValue)
    }

    @Test
    fun waveformFallsBackToHourBucketsWhenActivityIsFlat() {
        val hours = List(24) { if (it == 21) 600L else 0L }
        val snapshot = ListeningInsightsSnapshot(
            metrics = ListeningInsightsMetrics(listenedMs = 600L, countedPlays = 1),
            activity = listOf(ListeningInsightsActivityPoint(0L, 0L, 0)),
            hourBuckets = hours
        )
        val content = requireNotNull(ListeningReceiptModel.content(snapshot, labels, Locale.US, now, zone))
        assertEquals(24, content.waveform.size)
        assertEquals(1f, content.waveform[21])
        assertEquals(0f, content.waveform[0])
    }

    @Test
    fun shortActivitySeriesUsesHourlyRhythm() {
        val hours = List(24) { it.toLong() }
        val snapshot = ListeningInsightsSnapshot(
            metrics = ListeningInsightsMetrics(listenedMs = 600L, countedPlays = 1),
            activity = List(7) { ListeningInsightsActivityPoint(it.toLong(), 100L, 1) },
            hourBuckets = hours
        )
        val content = requireNotNull(ListeningReceiptModel.content(snapshot, labels, Locale.US, now, zone))
        assertEquals(24, content.waveform.size)
    }

    @Test
    fun waveformBucketsLongSeries() {
        val waveform = ListeningReceiptModel.waveform(List(180) { 10L }, maxBars = 44)
        assertEquals(44, waveform.size)
        assertTrue(waveform.all { it in 0f..1f })
        assertEquals(1f, waveform.max())
    }

    @Test
    fun everySupportedLanguageHasItsOwnCompleteTranslation() {
        val english = listeningReceiptBundles.getValue("en")
        LevyraLanguageCatalog.languages.forEach { language ->
            val bundle = requireNotNull(listeningReceiptBundles[language.code]) { "missing ${language.code}" }
            assertEquals(language.code, listeningReceiptKeys, bundle.keys)
            if (language.code != "en") {
                val translated = bundle.count { (key, value) -> value != english.getValue(key) }
                assertTrue("${language.code} looks untranslated", translated >= listeningReceiptKeys.size / 2)
            }
        }
    }

    @Test
    fun yearRangeCoversCalendarYearToDate() {
        val range = ListeningInsightsRanges.year(2025, now, zone)
        assertEquals(1_735_689_600_000L, range.fromMs)
        assertEquals(now + 1L, range.toMs)
        assertEquals(1_767_225_600_000L, ListeningInsightsRanges.year(2025, 1_800_000_000_000L, zone).toMs)
        assertEquals(1_704_067_200_000L, range.previousFromMs)
        val receipt = requireNotNull(
            ListeningReceiptModel.content(
                ListeningInsightsSnapshot(metrics = ListeningInsightsMetrics(listenedMs = 60_000L, countedPlays = 1)),
                labels, Locale.US, now, zone, range
            )
        )
        assertTrue(receipt.dateRange.contains("2025"))
    }

    @Test
    fun initialKeepsWholeGraphemeClusters() {
        assertEquals("É", ListeningReceiptModel.initial(" été"))
        assertEquals("♪", ListeningReceiptModel.initial("   "))
    }
}
