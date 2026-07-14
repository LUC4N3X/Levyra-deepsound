package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkStartupMetricsCollectorTest {
    @Test
    fun recordsFirstArtworkLatencyAndPersistentHitRate() {
        var now = 0L
        val collector = ArtworkStartupMetricsCollector { now }
        collector.reset()
        collector.recordArtworkRequest("track-1:small", "file:/art/1", ArtworkRequestSource.PersistentFile)
        collector.recordArtworkRequest("track-2:small", "remote:https://cdn/2", ArtworkRequestSource.Remote)
        now = 90_000_000L
        collector.recordArtworkDisplayed("track-1:small")

        val snapshot = collector.snapshot()

        assertEquals(90L, snapshot.firstRealArtworkMs ?: -1L)
        assertEquals(2, snapshot.uniqueArtworkRequests)
        assertEquals(1, snapshot.persistentArtworkRequests)
        assertEquals(0.5, snapshot.persistentRequestRate, 0.0001)
    }

    @Test
    fun repeatedIdenticalModelDoesNotCountAsArtworkChange() {
        val collector = ArtworkStartupMetricsCollector { 0L }
        collector.reset()
        collector.recordArtworkRequest("track-1:small", "file:/art/1", ArtworkRequestSource.PersistentFile)
        collector.recordArtworkRequest("track-1:small", "file:/art/1", ArtworkRequestSource.PersistentFile)

        assertEquals(0, collector.snapshot().artworkModelChanges)
    }

    @Test
    fun changedModelForSameCardIsReportedAsRegression() {
        val collector = ArtworkStartupMetricsCollector { 0L }
        collector.reset()
        collector.recordArtworkRequest("track-1:small", "remote:https://cdn/old", ArtworkRequestSource.Remote)
        collector.recordArtworkRequest("track-1:small", "remote:https://cdn/new", ArtworkRequestSource.Remote)

        val snapshot = collector.snapshot()

        assertEquals(1, snapshot.artworkModelChanges)
        assertTrue(snapshot.regressionViolations().contains("artwork_model_changes=1"))
    }

    @Test
    fun shimmerWithUsableContentIsReportedAsRegression() {
        val collector = ArtworkStartupMetricsCollector { 0L }
        collector.reset()
        collector.recordShimmer(hasUsableContent = true)

        val snapshot = collector.snapshot()

        assertEquals(1, snapshot.shimmerWithUsableContent)
        assertTrue(snapshot.regressionViolations().contains("shimmer_with_content=1"))
    }

    @Test
    fun latePlaceholderIsReportedAfterFirstArtworkWasDisplayed() {
        var now = 0L
        val collector = ArtworkStartupMetricsCollector { now }
        collector.reset()
        now = 20_000_000L
        collector.recordArtworkDisplayed("track-1:small")
        now = 40_000_000L
        collector.recordArtworkLoading("track-1:small")

        val snapshot = collector.snapshot()

        assertEquals(1, snapshot.placeholdersAfterFirstArtwork)
        assertTrue(snapshot.regressionViolations().contains("late_placeholders=1"))
    }

    @Test
    fun duplicateHomeFingerprintIsNotCountedTwice() {
        val collector = ArtworkStartupMetricsCollector { 0L }
        collector.reset()
        collector.recordHomeEmission("home-a", hasUsableContent = true)
        collector.recordHomeEmission("home-a", hasUsableContent = true)
        collector.recordHomeEmission("home-b", hasUsableContent = true)

        assertEquals(2, collector.snapshot().visibleHomeEmissions)
        assertTrue(collector.snapshot().regressionViolations().isEmpty())
    }
}
