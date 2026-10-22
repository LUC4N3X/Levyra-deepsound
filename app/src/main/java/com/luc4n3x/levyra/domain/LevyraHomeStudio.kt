package com.luc4n3x.levyra.domain

import org.json.JSONArray
import org.json.JSONObject

enum class HomeStyle(val storageId: String) {
    Original("original"),
    ExpressiveMax("expressive_max"),
    Flow("flow"),
    Editorial("editorial"),
    Minimal("minimal"),
    Immersive("immersive"),
    MaterialExpressive("material_expressive");

    companion object {
        fun from(value: String?): HomeStyle =
            entries.firstOrNull { it.storageId == value } ?: Original
    }
}

enum class HomeSectionKind {
    Tracks,
    Albums,
    Artists,
    Fixed
}

enum class HomeSectionId(val storageId: String, val kind: HomeSectionKind) {
    GenreChips("genre_chips", HomeSectionKind.Fixed),
    SpeedDial("speed_dial", HomeSectionKind.Fixed),
    Spotlight("spotlight", HomeSectionKind.Fixed),
    Discovery("discovery", HomeSectionKind.Fixed),
    PersonalOrbit("personal_orbit", HomeSectionKind.Fixed),
    FavoriteRadio("favorite_radio", HomeSectionKind.Fixed),
    EditorialCollections("editorial_collections", HomeSectionKind.Fixed),
    QuickPicks("quick_picks", HomeSectionKind.Tracks),
    ForgottenFavorites("forgotten_favorites", HomeSectionKind.Tracks),
    AlbumsForYou("albums_for_you", HomeSectionKind.Albums),
    TrendingArtists("trending_artists", HomeSectionKind.Artists),
    NewReleases("new_releases", HomeSectionKind.Tracks),
    MusicVideos("music_videos", HomeSectionKind.Fixed),
    ReleaseRadar("release_radar", HomeSectionKind.Fixed),
    SimilarArtists("similar_artists", HomeSectionKind.Artists),
    MoreSections("more_sections", HomeSectionKind.Tracks),
    Resonance("resonance", HomeSectionKind.Fixed),
    Charts("charts", HomeSectionKind.Fixed);

    val supportsLayouts: Boolean get() = kind != HomeSectionKind.Fixed

    companion object {
        val QuickAccess: Set<HomeSectionId> = setOf(GenreChips, SpeedDial)

        fun from(value: String?): HomeSectionId? = entries.firstOrNull { it.storageId == value }
    }
}

enum class HomeSectionLayout(val storageId: String) {
    Auto("auto"),
    Carousel("carousel"),
    Grid("grid"),
    List("list"),
    Compact("compact"),
    Large("large");

    companion object {
        fun from(value: String?): HomeSectionLayout =
            entries.firstOrNull { it.storageId == value } ?: Auto
    }
}

enum class HomeDensity(val storageId: String) {
    Compact("compact"),
    Balanced("balanced"),
    Spacious("spacious");

    companion object {
        fun from(value: String?): HomeDensity = entries.firstOrNull { it.storageId == value } ?: Balanced
    }
}

enum class HomeMotion(val storageId: String) {
    Reduced("reduced"),
    Normal("normal"),
    Expressive("expressive");

    companion object {
        fun from(value: String?): HomeMotion = entries.firstOrNull { it.storageId == value } ?: Normal
    }
}

data class HomeStyleLayout(
    val order: List<HomeSectionId>,
    val hidden: Set<HomeSectionId> = emptySet(),
    val layouts: Map<HomeSectionId, HomeSectionLayout> = emptyMap(),
    val featured: List<HomeSectionId> = emptyList(),
    val density: HomeDensity = HomeDensity.Balanced,
    val motion: HomeMotion = HomeMotion.Normal,
    val dynamicPalette: Boolean = true
) {
    fun isHidden(id: HomeSectionId): Boolean = id in hidden

    fun layoutFor(id: HomeSectionId): HomeSectionLayout =
        if (id.supportsLayouts) layouts[id] ?: HomeSectionLayout.Auto else HomeSectionLayout.Auto

    fun renderOrder(): List<HomeSectionId> {
        val promoted = featured.filter { it in order }
        return promoted + order.filterNot { it in promoted }
    }

    fun moved(from: Int, to: Int): HomeStyleLayout {
        if (from !in order.indices || to !in order.indices || from == to) return this
        val next = order.toMutableList()
        next.add(to, next.removeAt(from))
        return copy(order = next)
    }

    fun withVisibility(id: HomeSectionId, visible: Boolean): HomeStyleLayout =
        copy(hidden = if (visible) hidden - id else hidden + id)

    fun withLayout(id: HomeSectionId, layout: HomeSectionLayout): HomeStyleLayout {
        if (!id.supportsLayouts) return this
        return copy(layouts = if (layout == HomeSectionLayout.Auto) layouts - id else layouts + (id to layout))
    }

    fun withFeatured(id: HomeSectionId, featuredOn: Boolean): HomeStyleLayout {
        val next = if (featuredOn) {
            (featured - id + id).takeLast(MAX_FEATURED)
        } else {
            featured - id
        }
        return copy(featured = next)
    }

    fun normalized(style: HomeStyle): HomeStyleLayout {
        val defaults = LevyraHomeStudioSettings.defaultLayout(style).order
        val known = order.distinct()
        val merged = known.toMutableList()
        defaults.filterNot { it in known }.forEach { id ->
            val anchor = defaults.indexOf(id)
            val previous = defaults.take(anchor).lastOrNull { it in merged }
            val insertAt = previous?.let { merged.indexOf(it) + 1 } ?: 0
            merged.add(insertAt, id)
        }
        return copy(
            order = merged,
            layouts = layouts.filter { (id, layout) -> id.supportsLayouts && layout != HomeSectionLayout.Auto },
            featured = featured.distinct().takeLast(MAX_FEATURED),
            motion = if (style == HomeStyle.Original && motion == HomeMotion.Expressive) HomeMotion.Normal else motion
        )
    }

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("order", JSONArray(order.map { it.storageId }))
        put("hidden", JSONArray(hidden.map { it.storageId }))
        put("layouts", JSONObject().apply { layouts.forEach { (id, layout) -> put(id.storageId, layout.storageId) } })
        put("featured", JSONArray(featured.map { it.storageId }))
        put("density", density.storageId)
        put("motion", motion.storageId)
        put("dynamicPalette", dynamicPalette)
    }

    companion object {
        const val MAX_FEATURED = 2

        internal fun fromJson(json: JSONObject, style: HomeStyle): HomeStyleLayout {
            val defaults = LevyraHomeStudioSettings.defaultLayout(style)
            val layoutsJson = json.optJSONObject("layouts")
            val layouts = buildMap {
                layoutsJson?.keys()?.forEach { key ->
                    val id = HomeSectionId.from(key) ?: return@forEach
                    put(id, HomeSectionLayout.from(layoutsJson.optString(key)))
                }
            }
            return HomeStyleLayout(
                order = json.optJSONArray("order")?.sectionIds() ?: defaults.order,
                hidden = json.optJSONArray("hidden")?.sectionIds()?.toSet() ?: defaults.hidden,
                layouts = if (layoutsJson == null) defaults.layouts else layouts,
                featured = json.optJSONArray("featured")?.sectionIds() ?: defaults.featured,
                density = HomeDensity.from(json.optString("density", defaults.density.storageId)),
                motion = HomeMotion.from(json.optString("motion", defaults.motion.storageId)),
                dynamicPalette = json.optBoolean("dynamicPalette", defaults.dynamicPalette)
            ).normalized(style)
        }

        private fun JSONArray.sectionIds(): List<HomeSectionId> =
            (0 until length()).mapNotNull { HomeSectionId.from(optString(it)) }
    }
}

data class LevyraHomeStudioSettings(
    val style: HomeStyle = HomeStyle.Original,
    val styleLayouts: Map<HomeStyle, HomeStyleLayout> = emptyMap()
) {
    fun layoutFor(target: HomeStyle): HomeStyleLayout =
        styleLayouts[target]?.normalized(target) ?: defaultLayout(target)

    fun activeLayout(): HomeStyleLayout = layoutFor(style)

    fun usesClassicOriginalHome(): Boolean =
        style == HomeStyle.Original && activeLayout() == defaultLayout(HomeStyle.Original)

    fun withStyle(target: HomeStyle): LevyraHomeStudioSettings = copy(style = target)

    fun updateLayout(target: HomeStyle, transform: (HomeStyleLayout) -> HomeStyleLayout): LevyraHomeStudioSettings {
        val next = transform(layoutFor(target)).normalized(target)
        return copy(
            styleLayouts = if (next == defaultLayout(target)) styleLayouts - target else styleLayouts + (target to next)
        )
    }

    fun resetStyle(target: HomeStyle): LevyraHomeStudioSettings = copy(styleLayouts = styleLayouts - target)

    fun normalized(): LevyraHomeStudioSettings = copy(
        styleLayouts = styleLayouts
            .mapValues { (target, layout) -> layout.normalized(target) }
            .filter { (target, layout) -> layout != defaultLayout(target) }
    )

    fun encode(): String = JSONObject().apply {
        put("version", STORAGE_VERSION)
        put("style", style.storageId)
        put("layouts", JSONObject().apply {
            normalized().styleLayouts.forEach { (target, layout) -> put(target.storageId, layout.toJson()) }
        })
    }.toString()

    companion object {
        const val STORAGE_VERSION = 1

        private val OriginalOrder = listOf(
            HomeSectionId.GenreChips,
            HomeSectionId.SpeedDial,
            HomeSectionId.Spotlight,
            HomeSectionId.Discovery,
            HomeSectionId.PersonalOrbit,
            HomeSectionId.FavoriteRadio,
            HomeSectionId.EditorialCollections,
            HomeSectionId.QuickPicks,
            HomeSectionId.ForgottenFavorites,
            HomeSectionId.AlbumsForYou,
            HomeSectionId.TrendingArtists,
            HomeSectionId.NewReleases,
            HomeSectionId.MusicVideos,
            HomeSectionId.ReleaseRadar,
            HomeSectionId.SimilarArtists,
            HomeSectionId.MoreSections,
            HomeSectionId.Resonance,
            HomeSectionId.Charts
        )

        fun defaultLayout(style: HomeStyle): HomeStyleLayout = when (style) {
            HomeStyle.Original -> HomeStyleLayout(order = OriginalOrder)
            HomeStyle.ExpressiveMax -> HomeStyleLayout(
                order = listOf(
                    HomeSectionId.Spotlight,
                    HomeSectionId.Discovery,
                    HomeSectionId.SpeedDial,
                    HomeSectionId.QuickPicks,
                    HomeSectionId.PersonalOrbit,
                    HomeSectionId.FavoriteRadio,
                    HomeSectionId.AlbumsForYou,
                    HomeSectionId.GenreChips,
                    HomeSectionId.TrendingArtists,
                    HomeSectionId.EditorialCollections,
                    HomeSectionId.NewReleases,
                    HomeSectionId.ForgottenFavorites,
                    HomeSectionId.MusicVideos,
                    HomeSectionId.ReleaseRadar,
                    HomeSectionId.SimilarArtists,
                    HomeSectionId.MoreSections,
                    HomeSectionId.Charts,
                    HomeSectionId.Resonance
                ),
                layouts = mapOf(
                    HomeSectionId.QuickPicks to HomeSectionLayout.Large,
                    HomeSectionId.AlbumsForYou to HomeSectionLayout.Carousel,
                    HomeSectionId.TrendingArtists to HomeSectionLayout.Carousel,
                    HomeSectionId.NewReleases to HomeSectionLayout.Grid,
                    HomeSectionId.ForgottenFavorites to HomeSectionLayout.List,
                    HomeSectionId.SimilarArtists to HomeSectionLayout.Compact,
                    HomeSectionId.MoreSections to HomeSectionLayout.Carousel
                ),
                motion = HomeMotion.Expressive
            )
            HomeStyle.Flow -> HomeStyleLayout(
                order = listOf(
                    HomeSectionId.SpeedDial,
                    HomeSectionId.QuickPicks,
                    HomeSectionId.PersonalOrbit,
                    HomeSectionId.FavoriteRadio,
                    HomeSectionId.GenreChips,
                    HomeSectionId.AlbumsForYou,
                    HomeSectionId.NewReleases,
                    HomeSectionId.EditorialCollections,
                    HomeSectionId.TrendingArtists,
                    HomeSectionId.ForgottenFavorites,
                    HomeSectionId.MoreSections,
                    HomeSectionId.Charts,
                    HomeSectionId.MusicVideos,
                    HomeSectionId.ReleaseRadar,
                    HomeSectionId.SimilarArtists,
                    HomeSectionId.Spotlight,
                    HomeSectionId.Discovery,
                    HomeSectionId.Resonance
                ),
                layouts = mapOf(
                    HomeSectionId.QuickPicks to HomeSectionLayout.Compact,
                    HomeSectionId.AlbumsForYou to HomeSectionLayout.Carousel,
                    HomeSectionId.NewReleases to HomeSectionLayout.Carousel,
                    HomeSectionId.TrendingArtists to HomeSectionLayout.Carousel,
                    HomeSectionId.ForgottenFavorites to HomeSectionLayout.Compact,
                    HomeSectionId.SimilarArtists to HomeSectionLayout.Carousel,
                    HomeSectionId.MoreSections to HomeSectionLayout.Carousel
                ),
                density = HomeDensity.Compact
            )
            HomeStyle.Editorial -> HomeStyleLayout(
                order = listOf(
                    HomeSectionId.Spotlight,
                    HomeSectionId.Discovery,
                    HomeSectionId.EditorialCollections,
                    HomeSectionId.NewReleases,
                    HomeSectionId.PersonalOrbit,
                    HomeSectionId.FavoriteRadio,
                    HomeSectionId.AlbumsForYou,
                    HomeSectionId.QuickPicks,
                    HomeSectionId.TrendingArtists,
                    HomeSectionId.ReleaseRadar,
                    HomeSectionId.MusicVideos,
                    HomeSectionId.ForgottenFavorites,
                    HomeSectionId.SimilarArtists,
                    HomeSectionId.MoreSections,
                    HomeSectionId.Charts,
                    HomeSectionId.Resonance,
                    HomeSectionId.SpeedDial,
                    HomeSectionId.GenreChips
                ),
                layouts = mapOf(
                    HomeSectionId.NewReleases to HomeSectionLayout.Large,
                    HomeSectionId.AlbumsForYou to HomeSectionLayout.Large,
                    HomeSectionId.QuickPicks to HomeSectionLayout.List,
                    HomeSectionId.TrendingArtists to HomeSectionLayout.Large,
                    HomeSectionId.ForgottenFavorites to HomeSectionLayout.List,
                    HomeSectionId.SimilarArtists to HomeSectionLayout.Carousel,
                    HomeSectionId.MoreSections to HomeSectionLayout.Carousel
                ),
                density = HomeDensity.Spacious
            )
            HomeStyle.Minimal -> HomeStyleLayout(
                order = listOf(
                    HomeSectionId.SpeedDial,
                    HomeSectionId.QuickPicks,
                    HomeSectionId.Discovery,
                    HomeSectionId.PersonalOrbit,
                    HomeSectionId.FavoriteRadio,
                    HomeSectionId.AlbumsForYou,
                    HomeSectionId.NewReleases,
                    HomeSectionId.TrendingArtists,
                    HomeSectionId.ForgottenFavorites,
                    HomeSectionId.MoreSections,
                    HomeSectionId.Charts,
                    HomeSectionId.ReleaseRadar,
                    HomeSectionId.SimilarArtists,
                    HomeSectionId.EditorialCollections,
                    HomeSectionId.MusicVideos,
                    HomeSectionId.Spotlight,
                    HomeSectionId.Resonance,
                    HomeSectionId.GenreChips
                ),
                hidden = setOf(HomeSectionId.GenreChips, HomeSectionId.Spotlight),
                layouts = mapOf(
                    HomeSectionId.QuickPicks to HomeSectionLayout.List,
                    HomeSectionId.AlbumsForYou to HomeSectionLayout.Grid,
                    HomeSectionId.NewReleases to HomeSectionLayout.List,
                    HomeSectionId.TrendingArtists to HomeSectionLayout.Compact,
                    HomeSectionId.ForgottenFavorites to HomeSectionLayout.List,
                    HomeSectionId.SimilarArtists to HomeSectionLayout.Compact,
                    HomeSectionId.MoreSections to HomeSectionLayout.List
                ),
                motion = HomeMotion.Reduced,
                dynamicPalette = false
            )
            HomeStyle.MaterialExpressive -> HomeStyleLayout(
                order = listOf(
                    HomeSectionId.Discovery,
                    HomeSectionId.QuickPicks,
                    HomeSectionId.SpeedDial,
                    HomeSectionId.GenreChips,
                    HomeSectionId.PersonalOrbit,
                    HomeSectionId.FavoriteRadio,
                    HomeSectionId.TrendingArtists,
                    HomeSectionId.AlbumsForYou,
                    HomeSectionId.ForgottenFavorites,
                    HomeSectionId.EditorialCollections,
                    HomeSectionId.NewReleases,
                    HomeSectionId.MusicVideos,
                    HomeSectionId.ReleaseRadar,
                    HomeSectionId.SimilarArtists,
                    HomeSectionId.MoreSections,
                    HomeSectionId.Charts,
                    HomeSectionId.Resonance,
                    HomeSectionId.Spotlight
                ),
                layouts = mapOf(
                    HomeSectionId.QuickPicks to HomeSectionLayout.Large,
                    HomeSectionId.AlbumsForYou to HomeSectionLayout.Carousel,
                    HomeSectionId.TrendingArtists to HomeSectionLayout.Large,
                    HomeSectionId.ForgottenFavorites to HomeSectionLayout.List,
                    HomeSectionId.NewReleases to HomeSectionLayout.Grid,
                    HomeSectionId.SimilarArtists to HomeSectionLayout.Compact,
                    HomeSectionId.MoreSections to HomeSectionLayout.Carousel
                ),
                motion = HomeMotion.Expressive
            )
            HomeStyle.Immersive -> HomeStyleLayout(
                order = listOf(
                    HomeSectionId.Spotlight,
                    HomeSectionId.Discovery,
                    HomeSectionId.PersonalOrbit,
                    HomeSectionId.FavoriteRadio,
                    HomeSectionId.QuickPicks,
                    HomeSectionId.AlbumsForYou,
                    HomeSectionId.MusicVideos,
                    HomeSectionId.TrendingArtists,
                    HomeSectionId.NewReleases,
                    HomeSectionId.EditorialCollections,
                    HomeSectionId.SpeedDial,
                    HomeSectionId.ForgottenFavorites,
                    HomeSectionId.ReleaseRadar,
                    HomeSectionId.SimilarArtists,
                    HomeSectionId.MoreSections,
                    HomeSectionId.Charts,
                    HomeSectionId.Resonance,
                    HomeSectionId.GenreChips
                ),
                layouts = mapOf(
                    HomeSectionId.QuickPicks to HomeSectionLayout.Large,
                    HomeSectionId.AlbumsForYou to HomeSectionLayout.Large,
                    HomeSectionId.TrendingArtists to HomeSectionLayout.Large,
                    HomeSectionId.NewReleases to HomeSectionLayout.Carousel,
                    HomeSectionId.ForgottenFavorites to HomeSectionLayout.Carousel,
                    HomeSectionId.SimilarArtists to HomeSectionLayout.Carousel,
                    HomeSectionId.MoreSections to HomeSectionLayout.Large
                ),
                density = HomeDensity.Spacious
            )
        }

        fun decode(raw: String?): LevyraHomeStudioSettings {
            if (raw.isNullOrBlank()) return LevyraHomeStudioSettings()
            return runCatching {
                val json = JSONObject(raw)
                val layoutsJson = json.optJSONObject("layouts")
                val layouts = buildMap {
                    layoutsJson?.keys()?.forEach { key ->
                        val target = HomeStyle.entries.firstOrNull { it.storageId == key } ?: return@forEach
                        val layout = layoutsJson.optJSONObject(key) ?: return@forEach
                        put(target, HomeStyleLayout.fromJson(layout, target))
                    }
                }
                LevyraHomeStudioSettings(
                    style = HomeStyle.from(json.optString("style")),
                    styleLayouts = layouts
                ).normalized()
            }.getOrDefault(LevyraHomeStudioSettings())
        }
    }
}
