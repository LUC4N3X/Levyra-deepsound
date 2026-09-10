package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeCollectionKind
import com.luc4n3x.levyra.domain.HomeCollectionSource
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals(
            collections.size,
            collections.map { collection -> collection.tracks.take(6).map { it.id } }.distinct().size
        )
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

    @Test
    fun sixVisibleCollectionCoversNeverReuseArtwork() {
        val tracks = (1..12).map { index ->
            val artworkIndex = (index - 1) % 6
            val size = if (index <= 6) 192 else 512
            track(
                id = "cover-$index",
                tags = setOf("workout", "chill", "focus", "party", "rap", "pop"),
                energy = 88,
                artworkUrl = "https://lh3.googleusercontent.com/levyra-cover-$artworkIndex=s$size-c-k-c0x00ffffff-no-rj"
            )
        }

        val collections = HomeEditorialEngine.buildCollections(
            homeSections = listOf(
                HomeSection("editorial-a", tracks.take(8)),
                HomeSection("editorial-b", tracks.drop(2).take(8))
            ),
            newReleaseTracks = emptyList(),
            personalTracks = tracks,
            resonanceTracks = tracks.reversed(),
            quickPickTracks = tracks,
            chartTracks = tracks.takeLast(8),
            favorites = tracks.take(6),
            libraryTracks = tracks,
            includeFresh = false,
            nowMillis = Instant.parse("2026-06-10T08:00:00Z").toEpochMilli()
        )

        val coverKeys = collections.map { collection ->
            collection.tracks.first().thumbnailUrl.substringBefore('=')
        }
        assertEquals(6, collections.size)
        assertEquals(coverKeys.size, coverKeys.distinct().size)
    }

    @Test
    fun coverUniquenessSurvivesRemovingOneSharedSpotlightTrack() {
        val spotlight = track(
            id = "spotlight-shared",
            tags = setOf("workout", "chill", "focus", "party", "rap", "pop"),
            energy = 91,
            replayScore = 100,
            metadataConfidence = 100,
            artworkUrl = "https://lh3.googleusercontent.com/shared-spotlight=s512-c-k-c0x00ffffff-no-rj"
        )
        val tracks = listOf(spotlight) + (1..14).map { index ->
            track(
                id = "hero-safe-$index",
                tags = setOf("workout", "chill", "focus", "party", "rap", "pop"),
                energy = 86 + index % 8,
                artworkUrl = "https://lh3.googleusercontent.com/hero-safe-$index=s512-c-k-c0x00ffffff-no-rj"
            )
        }

        val collections = HomeEditorialEngine.buildCollections(
            homeSections = listOf(HomeSection("hero-safe", tracks)),
            newReleaseTracks = emptyList(),
            personalTracks = tracks,
            resonanceTracks = tracks.reversed(),
            quickPickTracks = tracks,
            chartTracks = tracks,
            favorites = tracks,
            libraryTracks = tracks,
            includeFresh = false,
            nowMillis = Instant.parse("2026-06-10T08:00:00Z").toEpochMilli()
        )
        val visible = collections.map { collection ->
            collection.copy(tracks = collection.tracks.filterNot { it.id == spotlight.id })
        }
        val coverKeys = visible.map { collection ->
            collection.tracks.first().thumbnailUrl.substringBefore('=')
        }

        assertEquals(6, visible.size)
        assertTrue(visible.all { it.tracks.isNotEmpty() })
        assertEquals(coverKeys.size, coverKeys.distinct().size)
    }

    @Test
    fun undersizedFreshBucketIsNotPaddedWithOldTracks() {
        val freshTracks = (1..3).map { index ->
            track(
                id = "fresh-$index",
                tags = setOf("pop"),
                energy = 80,
                releaseDate = "2026-06-09"
            )
        }
        val catalog = (1..10).map { index ->
            track(
                id = "catalog-$index",
                tags = setOf("workout", "chill", "focus", "party", "rap", "pop"),
                energy = 86,
                releaseDate = "2025-01-01"
            )
        }

        val collections = HomeEditorialEngine.buildCollections(
            homeSections = emptyList(),
            newReleaseTracks = freshTracks,
            personalTracks = catalog,
            resonanceTracks = emptyList(),
            quickPickTracks = catalog,
            chartTracks = catalog.takeLast(6),
            favorites = emptyList(),
            libraryTracks = catalog,
            includeFresh = true,
            nowMillis = Instant.parse("2026-06-10T08:00:00Z").toEpochMilli()
        )

        assertEquals(6, collections.size)
        assertFalse(collections.any { it.kind == HomeCollectionKind.Fresh })
    }

    @Test
    fun discoveryWithoutChartInputIsNotMislabelledAsChartSource() {
        val tracks = (1..12).map { index ->
            track(
                id = "discovery-$index",
                tags = when (index % 5) {
                    0 -> setOf("workout")
                    1 -> setOf("chill")
                    2 -> setOf("focus")
                    3 -> setOf("party")
                    else -> setOf("rap")
                },
                energy = 72 + index
            )
        }

        val collections = HomeEditorialEngine.buildCollections(
            homeSections = emptyList(),
            newReleaseTracks = emptyList(),
            personalTracks = tracks,
            resonanceTracks = tracks.reversed(),
            quickPickTracks = tracks,
            chartTracks = emptyList(),
            favorites = emptyList(),
            libraryTracks = tracks,
            includeFresh = false,
            nowMillis = Instant.parse("2026-06-10T08:00:00Z").toEpochMilli()
        )

        val discovery = collections.firstOrNull { it.kind == HomeCollectionKind.Discovery }
        assertTrue(discovery != null)
        assertEquals(HomeCollectionSource.Levyra, discovery?.source)
    }

    private fun track(
        id: String,
        tags: Set<String>,
        energy: Int = 70,
        vocal: Int = 60,
        replayScore: Int = 85,
        metadataConfidence: Int = 90,
        artworkUrl: String = "https://example.com/$id.jpg",
        releaseDate: String = ""
    ): Track {
        return Track(
            id = id,
            title = "Title $id",
            artist = "Artist $id",
            album = "Album $id",
            durationMs = 180_000L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = artworkUrl,
            largeThumbnailUrl = artworkUrl,
            source = "test",
            moodTags = tags,
            energy = energy,
            vocal = vocal,
            replayScore = replayScore,
            cacheScore = 75,
            accentStart = 0xFF123456.toInt(),
            accentEnd = 0xFF654321.toInt(),
            releaseDate = releaseDate,
            metadataConfidence = metadataConfidence
        )
    }
}