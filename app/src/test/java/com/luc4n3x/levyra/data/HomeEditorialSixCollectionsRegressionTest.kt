package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HomeEditorialSixCollectionsRegressionTest {
    @Test
    fun sparseMixedSignalsStillProduceExactlySixCollections() {
        val tracks = (1..8).map { index ->
            track(
                id = "signal-$index",
                tags = when (index % 4) {
                    0 -> setOf("pop")
                    1 -> setOf("focus")
                    2 -> setOf("chill")
                    else -> setOf("workout")
                },
                energy = 62 + index * 4
            )
        }

        val collections = HomeEditorialEngine.buildCollections(
            homeSections = emptyList(),
            newReleaseTracks = emptyList(),
            personalTracks = tracks.take(2),
            resonanceTracks = tracks.drop(2).take(2),
            quickPickTracks = tracks,
            chartTracks = tracks.takeLast(4),
            favorites = emptyList(),
            libraryTracks = emptyList(),
            includeFresh = false,
            nowMillis = Instant.parse("2026-06-10T08:00:00Z").toEpochMilli()
        )

        assertEquals(6, collections.size)
        assertTrue(collections.all { it.tracks.size >= 4 })
    }

    @Test
    fun localizedEditorialSectionSurvivesSixCollectionSelection() {
        val localized = (1..8).map { index ->
            track(
                id = "localized-$index",
                tags = emptySet(),
                energy = 83,
                vocal = 82,
                replayScore = 70 + index,
                metadataConfidence = 96
            )
        }
        val title = "Rap italiano per te"

        val collections = HomeEditorialEngine.buildCollections(
            homeSections = listOf(HomeSection(title, localized)),
            newReleaseTracks = emptyList(),
            personalTracks = localized.take(2),
            resonanceTracks = emptyList(),
            quickPickTracks = localized,
            chartTracks = localized.takeLast(4),
            favorites = emptyList(),
            libraryTracks = emptyList(),
            includeFresh = false,
            nowMillis = Instant.parse("2026-06-10T08:00:00Z").toEpochMilli()
        )

        assertEquals(6, collections.size)
        assertTrue(collections.any { it.titleOverride == title })
    }

    @Test
    fun sixCollectionContractHoldsAcrossDailyRotation() {
        val tracks = (1..12).map { index ->
            track(
                id = "rotation-$index",
                tags = setOf("workout", "chill", "focus", "party", "rap", "pop"),
                energy = 76 + index % 20
            )
        }
        val base = Instant.parse("2026-06-01T08:00:00Z").toEpochMilli()

        repeat(16) { day ->
            val collections = HomeEditorialEngine.buildCollections(
                homeSections = listOf(
                    HomeSection("local-a", tracks.take(6)),
                    HomeSection("local-b", tracks.drop(3).take(6)),
                    HomeSection("local-c", tracks.drop(6).take(6))
                ),
                newReleaseTracks = emptyList(),
                personalTracks = tracks,
                resonanceTracks = tracks.reversed(),
                quickPickTracks = tracks,
                chartTracks = tracks,
                favorites = tracks.take(6),
                libraryTracks = tracks,
                includeFresh = true,
                nowMillis = base + day * 24L * 60L * 60L * 1000L
            )

            assertEquals(6, collections.size)
            assertTrue(collections.all { it.tracks.size in 4..18 })
        }
    }

    private fun track(
        id: String,
        tags: Set<String>,
        energy: Int = 70,
        vocal: Int = 60,
        replayScore: Int = 85,
        metadataConfidence: Int = 90
    ): Track {
        return Track(
            id = id,
            title = "Title $id",
            artist = "Artist $id",
            album = "Album $id",
            durationMs = 180_000L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = "https://example.com/$id.jpg",
            largeThumbnailUrl = "https://example.com/${id}_large.jpg",
            source = "test",
            moodTags = tags,
            energy = energy,
            vocal = vocal,
            replayScore = replayScore,
            cacheScore = 75,
            accentStart = 0xFF123456.toInt(),
            accentEnd = 0xFF654321.toInt(),
            metadataConfidence = metadataConfidence
        )
    }
}