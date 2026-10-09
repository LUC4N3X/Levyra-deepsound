package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.data.isYoutubeShortTrack

internal const val ExploreSampleLimit = 10
internal const val ExploreImmersiveSampleLimit = 24
internal const val ExploreSpotlightLimit = 5
internal const val ExploreMomentRowsPerPage = 4
internal const val ExploreMomentPageLimit = 4

internal enum class ExploreFreshScope {
    Local,
    World
}

internal enum class ExploreReleaseKind {
    Album,
    Single,
    Release
}

internal data class ExploreFreshFeed(
    val scope: ExploreFreshScope,
    val spotlight: List<Track>,
    val moment: List<Track>
)

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

    data object FreshScopes : ExploreRow {
        override val key: String = "explore-fresh-scopes"
    }

    data object FreshSpotlight : ExploreRow {
        override val key: String = "explore-fresh-spotlight"
    }

    data object FreshMoment : ExploreRow {
        override val key: String = "explore-fresh-moment"
    }

    data object Samples : ExploreRow {
        override val key: String = "explore-samples-carousel"
    }

    data object MixTools : ExploreRow {
        override val key: String = "explore-mix-tools"
    }

    data class MoodRail(val zones: List<ExploreZone>) : ExploreRow {
        override val key: String = "explore-mood-rail"
    }
}

internal fun buildExploreRows(
    zones: List<ExploreZone>,
    isFreshLoading: Boolean,
    hasFreshTracks: Boolean,
    hasSamples: Boolean,
    hasFreshScopes: Boolean = false,
    hasFreshMoment: Boolean = false
): List<ExploreRow> {
    val rows = mutableListOf<ExploreRow>()
    rows += ExploreRow.Shortcuts
    rows += ExploreRow.Header(ExploreAnchor.Fresh)
    when {
        hasFreshTracks -> {
            if (hasFreshScopes) rows += ExploreRow.FreshScopes
            rows += ExploreRow.FreshSpotlight
            if (hasFreshMoment) rows += ExploreRow.FreshMoment
        }
        isFreshLoading -> rows += ExploreRow.FreshLoading
        else -> rows += ExploreRow.FreshEmpty
    }

    val distinctZones = zones.distinctBy { it.id }
    if (distinctZones.isNotEmpty()) {
        rows += ExploreRow.Header(ExploreAnchor.Moods)
        rows += ExploreRow.MoodRail(distinctZones)
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

internal fun exploreFreshScopes(
    localTracks: List<Track>,
    worldTracks: List<Track>
): List<ExploreFreshScope> = buildList {
    if (localTracks.isNotEmpty()) add(ExploreFreshScope.Local)
    if (worldTracks.isNotEmpty()) add(ExploreFreshScope.World)
}

internal fun exploreFreshFeed(
    scope: ExploreFreshScope,
    localTracks: List<Track>,
    worldTracks: List<Track>
): ExploreFreshFeed {
    val resolvedScope = if (scope == ExploreFreshScope.World && worldTracks.isEmpty()) {
        ExploreFreshScope.Local
    } else {
        scope
    }
    val source = when (resolvedScope) {
        ExploreFreshScope.Local -> localTracks
        ExploreFreshScope.World -> worldTracks
    }.distinctBy { track -> track.id }
    val releases = LinkedHashMap<String, Track>()
    source.forEach { track -> releases.putIfAbsent(exploreReleaseIdentity(track), track) }
    val spotlight = releases.values.take(ExploreSpotlightLimit)
    val spotlightIds = spotlight.mapTo(HashSet()) { track -> track.id }
    val moment = source
        .filterNot { track -> track.id in spotlightIds }
        .take(ExploreMomentRowsPerPage * ExploreMomentPageLimit)
    return ExploreFreshFeed(resolvedScope, spotlight, moment)
}

internal fun exploreReleaseIdentity(track: Track): String {
    val album = track.album.trim().lowercase()
    if (album.isEmpty()) return "track|${track.id}"
    return "release|${track.artist.trim().lowercase()}|$album"
}

internal fun exploreMomentPages(tracks: List<Track>): List<List<Track>> =
    tracks.chunked(ExploreMomentRowsPerPage).take(ExploreMomentPageLimit)

internal fun exploreReleaseKind(track: Track): ExploreReleaseKind {
    when (track.albumType.trim().lowercase()) {
        "single" -> return ExploreReleaseKind.Single
        "album", "compilation", "ep" -> return ExploreReleaseKind.Album
    }
    val album = track.album.trim()
    if (album.isEmpty()) return ExploreReleaseKind.Release
    return if (album.equals(track.title.trim(), ignoreCase = true)) {
        ExploreReleaseKind.Single
    } else {
        ExploreReleaseKind.Album
    }
}

internal fun exploreCoverArtworkTrack(track: Track): Track {
    val large = track.largeThumbnailUrl.trim()
    if (large.isBlank() || !LevyraPersonalOrbit.isVideoFrameArtworkUrl(large)) return track
    val cover = track.thumbnailUrl.trim()
    if (cover.isBlank() || LevyraPersonalOrbit.isVideoFrameArtworkUrl(cover)) return track
    return track.copy(largeThumbnailUrl = cover)
}
