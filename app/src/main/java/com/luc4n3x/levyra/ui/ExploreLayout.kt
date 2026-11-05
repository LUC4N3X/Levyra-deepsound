package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.data.isYoutubeShortTrack
import com.luc4n3x.levyra.data.news.MusicNewsStory

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

internal data class ExploreReleaseCard(
    val kind: ExploreReleaseKind,
    val title: String,
    val subtitle: String,
    val opensAlbum: Boolean
)

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
    Moods
}

internal enum class ExploreShortcut(val anchor: ExploreAnchor?, val zoneId: String?) {
    NewReleases(ExploreAnchor.Fresh, ExploreCatalog.NEW_RELEASES_ZONE_ID),
    Samples(null, null),
    Moods(ExploreAnchor.Moods, null),
    Podcasts(null, null)
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

    data object MoodJourney : ExploreRow {
        override val key: String = "explore-mood-journey"
    }

    data object MixTools : ExploreRow {
        override val key: String = "explore-mix-tools"
    }

    data object Samples : ExploreRow {
        override val key: String = "explore-samples"
    }

    data class MoodRail(val zones: List<ExploreZone>) : ExploreRow {
        override val key: String = "explore-mood-rail"
    }

    data object NewsHero : ExploreRow {
        override val key: String = "explore-news-hero"
    }

    data object DailyTop100 : ExploreRow {
        override val key: String = "explore-daily-top-100"
    }

    data object NewsList : ExploreRow {
        override val key: String = "explore-news-list"
    }
}

internal fun levyraSearchOwnsBrowse(spotify: Boolean, apple: Boolean, ownExplore: Boolean): Boolean =
    !spotify && !apple && !ownExplore

internal fun buildExploreRows(
    zones: List<ExploreZone>,
    isFreshLoading: Boolean,
    hasFreshTracks: Boolean,
    hasFreshScopes: Boolean = false,
    hasFreshMoment: Boolean = false,
    spotifyStyle: Boolean = false,
    moodsInSearch: Boolean = spotifyStyle,
    journeyInPlaceOfMoods: Boolean = false,
    editorial: Boolean = false,
    hasNews: Boolean = false,
    hasNewsDesk: Boolean = hasNews
): List<ExploreRow> {
    val rows = mutableListOf<ExploreRow>()
    if (editorial) {
        if (hasNews) rows += ExploreRow.NewsHero
        rows += exploreFreshRows(isFreshLoading, hasFreshTracks, hasFreshScopes, hasFreshMoment = false)
        rows += ExploreRow.DailyTop100
        if (hasNews && hasNewsDesk) rows += ExploreRow.NewsList
        return rows
    }
    rows += ExploreRow.Shortcuts
    if (spotifyStyle) rows += ExploreRow.Samples
    rows += exploreFreshRows(isFreshLoading, hasFreshTracks, hasFreshScopes, hasFreshMoment)

    val distinctZones = zones.distinctBy { it.id }
    if (journeyInPlaceOfMoods) {
        rows += ExploreRow.MoodJourney
    } else if (distinctZones.isNotEmpty() && !moodsInSearch) {
        rows += ExploreRow.Header(ExploreAnchor.Moods)
        rows += ExploreRow.MoodRail(distinctZones)
    }
    return rows
}

private fun exploreFreshRows(
    isFreshLoading: Boolean,
    hasFreshTracks: Boolean,
    hasFreshScopes: Boolean,
    hasFreshMoment: Boolean
): List<ExploreRow> = buildList {
    add(ExploreRow.Header(ExploreAnchor.Fresh))
    when {
        hasFreshTracks -> {
            if (hasFreshScopes) add(ExploreRow.FreshScopes)
            add(ExploreRow.FreshSpotlight)
            if (hasFreshMoment) add(ExploreRow.FreshMoment)
        }
        isFreshLoading -> add(ExploreRow.FreshLoading)
        else -> add(ExploreRow.FreshEmpty)
    }
}

internal const val ExploreRadioSeedLimit = 6

internal fun exploreRadioSeeds(
    mostPlayed: List<Track>,
    recent: List<Track>,
    limit: Int = ExploreRadioSeedLimit
): List<Track> = (mostPlayed.asSequence() + recent.asSequence())
    .filter { track -> track.artist.isNotBlank() }
    .distinctBy { track -> track.artist.trim().lowercase() }
    .take(limit.coerceAtLeast(0))
    .toList()

internal enum class ExploreNewsAgeUnit {
    Minutes,
    Hours,
    Days
}

internal fun exploreNewsAge(nowMs: Long, publishedAtMs: Long): Pair<ExploreNewsAgeUnit, Int>? {
    if (publishedAtMs <= 0L) return null
    val minutes = (nowMs - publishedAtMs).coerceAtLeast(0L) / 60_000L
    return when {
        minutes < 60L -> ExploreNewsAgeUnit.Minutes to minutes.toInt()
        minutes < 24L * 60L -> ExploreNewsAgeUnit.Hours to (minutes / 60L).toInt()
        else -> ExploreNewsAgeUnit.Days to (minutes / (24L * 60L)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}

internal const val ExploreNewsSecondaryCount = 2
internal const val ExploreNewsFrontSize = 1 + ExploreNewsSecondaryCount
internal const val ExploreNewsDeskLimit = 8
internal const val ExploreReleaseShelfLimit = 12

internal class ExploreNewsFront(val lead: MusicNewsStory, val secondary: List<MusicNewsStory>)

internal fun exploreNewsFront(stories: List<MusicNewsStory>): ExploreNewsFront? {
    val lead = stories.firstOrNull { story -> story.imageUrl.isNotBlank() } ?: stories.firstOrNull() ?: return null
    return ExploreNewsFront(lead, stories.filter { story -> story.url != lead.url }.take(ExploreNewsSecondaryCount))
}

internal fun exploreNewsSources(stories: List<MusicNewsStory>): List<String> =
    stories.map { story -> story.source.trim() }.filter(String::isNotEmpty).distinct()

internal fun exploreNewsDesk(stories: List<MusicNewsStory>, source: String?): List<MusicNewsStory> {
    val picked = if (source.isNullOrBlank()) {
        val front = exploreNewsFront(stories)
        val frontUrls = listOfNotNull(front?.lead).plus(front?.secondary.orEmpty()).mapTo(HashSet()) { story -> story.url }
        stories.filterNot { story -> story.url in frontUrls }
    } else {
        stories.filter { story -> story.source.trim() == source }
    }
    return picked.take(ExploreNewsDeskLimit)
}

internal fun exploreReleaseShelf(feed: ExploreFreshFeed, limit: Int = ExploreReleaseShelfLimit): List<Track> =
    (feed.spotlight + feed.moment).distinctBy(::exploreReleaseIdentity).take(limit.coerceAtLeast(0))

internal fun exploreAnchorIndex(rows: List<ExploreRow>, anchor: ExploreAnchor): Int =
    rows.indexOfFirst { row -> row is ExploreRow.Header && row.anchor == anchor }

internal fun exploreAvailableAnchors(rows: List<ExploreRow>): Set<ExploreAnchor> =
    rows.asSequence()
        .filterIsInstance<ExploreRow.Header>()
        .map { row -> row.anchor }
        .toSet()
        .let { anchors -> if (ExploreRow.MoodJourney in rows) anchors + ExploreAnchor.Moods else anchors }

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

private val ExploreDeclaredAlbumTypes = setOf("album", "compilation", "ep")

internal fun exploreReleaseCard(track: Track): ExploreReleaseCard {
    val declaredType = track.albumType.trim().lowercase()
    val album = track.album.trim()
    if (declaredType in ExploreDeclaredAlbumTypes && album.isNotEmpty()) {
        return ExploreReleaseCard(
            kind = ExploreReleaseKind.Album,
            title = album,
            subtitle = track.albumArtist.trim().ifBlank { track.artist.trim() },
            opensAlbum = true
        )
    }
    val kind = if (declaredType == "single" || track.trackTotal == 1) {
        ExploreReleaseKind.Single
    } else {
        ExploreReleaseKind.Release
    }
    return ExploreReleaseCard(
        kind = kind,
        title = track.title.trim(),
        subtitle = track.artist.trim(),
        opensAlbum = false
    )
}

internal fun exploreCoverArtworkTrack(track: Track): Track {
    val large = track.largeThumbnailUrl.trim()
    if (large.isBlank() || !LevyraPersonalOrbit.isVideoFrameArtworkUrl(large)) return track
    val cover = track.thumbnailUrl.trim()
    if (cover.isBlank() || LevyraPersonalOrbit.isVideoFrameArtworkUrl(cover)) return track
    return track.copy(largeThumbnailUrl = cover)
}
