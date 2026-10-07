package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.data.isYoutubeShortTrack

internal const val ExploreSampleLimit = 10
internal const val ExploreImmersiveSampleLimit = 24

internal enum class ExploreCategoryPresentation {
    Atmospheric,
    Structured,
    Mixed
}

internal data class ExploreCategorySection(
    val key: String,
    val providerTitle: String,
    val presentation: ExploreCategoryPresentation,
    val categories: List<ExploreCategory>
)

internal enum class ExploreAnchor {
    Fresh,
    Samples,
    Moods
}

internal enum class ExploreShortcut(val anchor: ExploreAnchor, val zoneId: String?) {
    NewReleases(ExploreAnchor.Fresh, ExploreCatalog.NEW_RELEASES_ZONE_ID),
    Samples(ExploreAnchor.Samples, null),
    Moods(ExploreAnchor.Moods, null)
}

internal sealed interface ExploreRow {
    val key: String

    data object Shortcuts : ExploreRow {
        override val key: String = "explore-shortcuts"
    }

    data class Header(val anchor: ExploreAnchor) : ExploreRow {
        override val key: String = "explore-header-${anchor.name}"
    }

    data object FreshLoading : ExploreRow {
        override val key: String = "explore-fresh-loading"
    }

    data object FreshEmpty : ExploreRow {
        override val key: String = "explore-fresh-empty"
    }

    data object FreshCarousel : ExploreRow {
        override val key: String = "explore-fresh-carousel"
    }

    data object Samples : ExploreRow {
        override val key: String = "explore-samples-carousel"
    }

    data object MixTools : ExploreRow {
        override val key: String = "explore-mix-tools"
    }

    data class MoodPair(val leading: ExploreZone, val trailing: ExploreZone?) : ExploreRow {
        override val key: String = "explore-mood-${leading.id}"
    }
}

internal fun buildExploreRows(
    zones: List<ExploreZone>,
    isFreshLoading: Boolean,
    hasFreshTracks: Boolean,
    hasSamples: Boolean
): List<ExploreRow> {
    val rows = mutableListOf<ExploreRow>()
    rows += ExploreRow.Shortcuts
    rows += ExploreRow.Header(ExploreAnchor.Fresh)
    rows += when {
        hasFreshTracks -> ExploreRow.FreshCarousel
        isFreshLoading -> ExploreRow.FreshLoading
        else -> ExploreRow.FreshEmpty
    }

    val distinctZones = zones.distinctBy { it.id }
    if (distinctZones.isNotEmpty()) {
        rows += ExploreRow.Header(ExploreAnchor.Moods)
        distinctZones.chunked(2).forEach { pair ->
            rows += ExploreRow.MoodPair(pair.first(), pair.getOrNull(1))
        }
    }

    if (hasSamples) {
        rows += ExploreRow.Header(ExploreAnchor.Samples)
        rows += ExploreRow.Samples
    }
    rows += ExploreRow.MixTools
    return rows
}

internal fun exploreAnchorIndex(rows: List<ExploreRow>, anchor: ExploreAnchor): Int =
    rows.indexOfFirst { row -> row is ExploreRow.Header && row.anchor == anchor }

internal fun exploreAvailableAnchors(rows: List<ExploreRow>): Set<ExploreAnchor> =
    rows.asSequence()
        .filterIsInstance<ExploreRow.Header>()
        .map { row -> row.anchor }
        .toSet()

internal fun exploreSampleTracks(videos: List<Track>, limit: Int = ExploreSampleLimit): List<Track> {
    if (limit <= 0) return emptyList()
    return videos.asSequence()
        .filter(::isYoutubeShortTrack)
        .filter { track -> track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank() }
        .distinctBy { track -> track.id }
        .take(limit)
        .toList()
}

internal fun buildExploreCategorySections(categories: List<ExploreCategory>): List<ExploreCategorySection> {
    val validCategories = LinkedHashMap<String, ExploreCategory>()
    categories.forEach { category ->
        val title = category.title.trim()
        if (title.isNotBlank() && category.params.isNotBlank()) {
            validCategories.putIfAbsent(
                category.params,
                category.copy(title = title, section = category.section.trim())
            )
        }
    }

    return validCategories.values
        .groupBy { category -> category.sectionIndex to category.section }
        .map { (sectionIdentity, sectionCategories) ->
            val (sectionIndex, providerTitle) = sectionIdentity
            ExploreCategorySection(
                key = "provider-section-$sectionIndex-${sectionCategories.first().params}",
                providerTitle = providerTitle,
                presentation = when {
                    sectionIndex < 0 -> ExploreCategoryPresentation.Mixed
                    sectionIndex == 0 -> ExploreCategoryPresentation.Atmospheric
                    else -> ExploreCategoryPresentation.Structured
                },
                categories = sectionCategories
            )
        }
}

internal fun exploreFallbackGenres(zones: List<ExploreZone>): List<ExploreZone> = zones
    .asSequence()
    .filterNot { zone ->
        zone.id == ExploreCatalog.NEW_RELEASES_ZONE_ID || zone.id == ExploreCatalog.LOCAL_WAVE_ZONE_ID
    }
    .distinctBy { zone -> zone.id }
    .toList()
