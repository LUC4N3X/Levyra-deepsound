package com.luc4n3x.levyra.domain

import org.json.JSONArray
import org.json.JSONObject

enum class NavStyle(val storageId: String) {
    Original("original"),
    SuperExpressive("super_expressive"),
    FloatingDock("floating_dock"),
    Minimal("minimal"),
    ExpressiveCompact("expressive_compact"),
    LiquidGlass("liquid_glass"),
    YtMusic("yt_music"),
    SplitDock("split_dock"),
    ConnectedDock("connected_dock"),
    LiquidCapsule("liquid_capsule");

    val floating: Boolean
        get() = this == FloatingDock || this == LiquidGlass || this == SplitDock || this == ConnectedDock || this == LiquidCapsule

    val separatesSearch: Boolean get() = this == SplitDock || this == LiquidCapsule

    val supportsDockMorph: Boolean get() = this == Original || this == LiquidCapsule

    companion object {
        fun from(value: String?): NavStyle = entries.firstOrNull { it.storageId == value } ?: Original

        val studioOrder: List<NavStyle> = listOf(Original, LiquidCapsule) + entries.filterNot { it == Original || it == LiquidCapsule }
    }
}

enum class NavLabelMode(val storageId: String) {
    Always("always"),
    ActiveOnly("active"),
    Never("never"),
    Compact("compact"),
    Expanding("expanding");

    companion object {
        fun from(value: String?): NavLabelMode? = entries.firstOrNull { it.storageId == value }
    }
}

enum class NavIndicator(val storageId: String) {
    Pill("pill"),
    Morph("morph"),
    Dot("dot"),
    Line("line"),
    Circle("circle"),
    Floating("floating"),
    None("none");

    val filled: Boolean get() = this == Pill || this == Morph || this == Circle || this == Floating

    companion object {
        fun from(value: String?): NavIndicator? = entries.firstOrNull { it.storageId == value }
    }
}

enum class NavBackground(val storageId: String) {
    Solid("solid"),
    Gradient("gradient"),
    Translucent("translucent");

    companion object {
        fun from(value: String?): NavBackground? = entries.firstOrNull { it.storageId == value }
    }
}

enum class NavMotion(val storageId: String) {
    Standard("standard"),
    Expressive("expressive"),
    Spring("spring"),
    Minimal("minimal"),
    Off("off");

    companion object {
        fun from(value: String?): NavMotion? = entries.firstOrNull { it.storageId == value }
    }
}

enum class NavColorSource(val storageId: String) {
    Theme("theme"),
    Dynamic("dynamic"),
    Custom("custom"),
    Artwork("artwork");

    companion object {
        fun from(value: String?): NavColorSource? = entries.firstOrNull { it.storageId == value }
    }
}

enum class NavSurfaceTone(val storageId: String) {
    Auto("auto"),
    Light("light"),
    Dark("dark"),
    Amoled("amoled");

    companion object {
        fun from(value: String?): NavSurfaceTone? = entries.firstOrNull { it.storageId == value }
    }
}

enum class NavIndicatorTone(val storageId: String) {
    Tonal("tonal"),
    Vivid("vivid"),
    Neutral("neutral");

    companion object {
        fun from(value: String?): NavIndicatorTone? = entries.firstOrNull { it.storageId == value }
    }
}

object NavDestinations {
    val defaultOrder: List<LevyraTab> = listOf(LevyraTab.Home, LevyraTab.Search, LevyraTab.Explore, LevyraTab.Library)
    val essential: Set<LevyraTab> = setOf(LevyraTab.Home, LevyraTab.Search)

    fun storageId(tab: LevyraTab): String = tab.name.lowercase()

    fun from(value: String?): LevyraTab? = defaultOrder.firstOrNull { storageId(it) == value }
}

data class NavStyleSpec(
    val height: IntRange,
    val corner: IntRange,
    val indicators: List<NavIndicator>,
    val backgrounds: List<NavBackground>,
    val defaults: NavTuning
)

data class NavTuning(
    val order: List<LevyraTab> = NavDestinations.defaultOrder,
    val hidden: Set<LevyraTab> = emptySet(),
    val labels: NavLabelMode = NavLabelMode.Always,
    val indicator: NavIndicator = NavIndicator.Pill,
    val background: NavBackground = NavBackground.Solid,
    val iconDp: Int = 24,
    val heightDp: Int = 72,
    val motion: NavMotion = NavMotion.Expressive,
    val intensity: Int = 60,
    val colorSource: NavColorSource = NavColorSource.Theme,
    val accent: Int = DEFAULT_ACCENT,
    val surfaceTone: NavSurfaceTone = NavSurfaceTone.Auto,
    val indicatorTone: NavIndicatorTone = NavIndicatorTone.Tonal,
    val opacity: Int = 100,
    val inactiveAlpha: Int = 100,
    val cornerDp: Int = 28,
    val spacingDp: Int = 0,
    val paddingDp: Int = 0,
    val indicatorScale: Int = 100,
    val haptics: Boolean = true
) {
    fun visibleOrder(): List<LevyraTab> = order.filterNot { it in hidden }

    fun normalized(style: NavStyle): NavTuning {
        val spec = LevyraNavigationStudioSettings.spec(style)
        val known = order.filter { it in NavDestinations.defaultOrder }.distinct()
        val merged = known + NavDestinations.defaultOrder.filterNot { it in known }
        return copy(
            order = merged,
            hidden = hidden.filter { it in NavDestinations.defaultOrder && it !in NavDestinations.essential }.toSet(),
            indicator = indicator.takeIf { it in spec.indicators } ?: spec.defaults.indicator,
            background = background.takeIf { it in spec.backgrounds } ?: spec.defaults.background,
            iconDp = iconDp.coerceIn(ICON_RANGE),
            heightDp = heightDp.coerceIn(spec.height),
            intensity = intensity.coerceIn(0, 100),
            accent = accent or OPAQUE,
            opacity = opacity.coerceIn(OPACITY_RANGE),
            inactiveAlpha = inactiveAlpha.coerceIn(INACTIVE_RANGE),
            cornerDp = cornerDp.coerceIn(spec.corner),
            spacingDp = spacingDp.coerceIn(SPACING_RANGE),
            paddingDp = paddingDp.coerceIn(PADDING_RANGE),
            indicatorScale = indicatorScale.coerceIn(INDICATOR_SCALE_RANGE)
        )
    }

    fun moved(from: Int, to: Int): NavTuning {
        if (from !in order.indices || to !in order.indices || from == to) return this
        return copy(order = order.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun withVisibility(tab: LevyraTab, visible: Boolean): NavTuning = when {
        tab in NavDestinations.essential -> this
        visible -> copy(hidden = hidden - tab)
        else -> copy(hidden = hidden + tab)
    }

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("order", JSONArray(order.map(NavDestinations::storageId)))
        put("hidden", JSONArray(hidden.map(NavDestinations::storageId)))
        put("labels", labels.storageId)
        put("indicator", indicator.storageId)
        put("background", background.storageId)
        put("icon", iconDp)
        put("height", heightDp)
        put("motion", motion.storageId)
        put("intensity", intensity)
        put("colors", colorSource.storageId)
        put("accent", accent)
        put("surface", surfaceTone.storageId)
        put("indicatorTone", indicatorTone.storageId)
        put("opacity", opacity)
        put("inactive", inactiveAlpha)
        put("corner", cornerDp)
        put("spacing", spacingDp)
        put("padding", paddingDp)
        put("indicatorScale", indicatorScale)
        put("haptics", haptics)
    }

    companion object {
        val ICON_RANGE = 20..30
        val OPACITY_RANGE = 50..100
        val INACTIVE_RANGE = 50..100
        val SPACING_RANGE = 0..16
        val PADDING_RANGE = 0..24
        val INDICATOR_SCALE_RANGE = 80..120
        const val DEFAULT_ACCENT = 0xFF7C8CFF.toInt()
        private const val OPAQUE = 0xFF000000.toInt()

        internal fun fromJson(json: JSONObject, style: NavStyle): NavTuning {
            val defaults = LevyraNavigationStudioSettings.spec(style).defaults
            fun JSONArray.tabs(): List<LevyraTab> = (0 until length()).mapNotNull { NavDestinations.from(optString(it)) }
            val legacyMotion = if (json.has("expressive")) {
                if (json.optBoolean("expressive", true)) NavMotion.Expressive else NavMotion.Minimal
            } else {
                null
            }
            return NavTuning(
                order = json.optJSONArray("order")?.tabs() ?: defaults.order,
                hidden = json.optJSONArray("hidden")?.tabs()?.toSet() ?: defaults.hidden,
                labels = NavLabelMode.from(json.optString("labels")) ?: defaults.labels,
                indicator = NavIndicator.from(json.optString("indicator")) ?: defaults.indicator,
                background = NavBackground.from(json.optString("background")) ?: defaults.background,
                iconDp = json.optInt("icon", defaults.iconDp),
                heightDp = json.optInt("height", defaults.heightDp),
                motion = NavMotion.from(json.optString("motion")) ?: legacyMotion ?: defaults.motion,
                intensity = json.optInt("intensity", defaults.intensity),
                colorSource = NavColorSource.from(json.optString("colors")) ?: defaults.colorSource,
                accent = json.optInt("accent", defaults.accent),
                surfaceTone = NavSurfaceTone.from(json.optString("surface")) ?: defaults.surfaceTone,
                indicatorTone = NavIndicatorTone.from(json.optString("indicatorTone")) ?: defaults.indicatorTone,
                opacity = json.optInt("opacity", defaults.opacity),
                inactiveAlpha = json.optInt("inactive", defaults.inactiveAlpha),
                cornerDp = json.optInt("corner", defaults.cornerDp),
                spacingDp = json.optInt("spacing", defaults.spacingDp),
                paddingDp = json.optInt("padding", defaults.paddingDp),
                indicatorScale = json.optInt("indicatorScale", defaults.indicatorScale),
                haptics = json.optBoolean("haptics", defaults.haptics)
            ).normalized(style)
        }
    }
}

data class LevyraNavigationStudioSettings(
    val style: NavStyle = NavStyle.Original,
    val tunings: Map<NavStyle, NavTuning> = emptyMap()
) {
    fun tuningFor(target: NavStyle): NavTuning = tunings[target]?.normalized(target) ?: spec(target).defaults

    fun activeTuning(): NavTuning = tuningFor(style)

    fun withStyle(target: NavStyle): LevyraNavigationStudioSettings = copy(style = target)

    fun updateTuning(target: NavStyle, transform: (NavTuning) -> NavTuning): LevyraNavigationStudioSettings {
        if (target == NavStyle.Original) return this
        val next = transform(tuningFor(target)).normalized(target)
        return copy(tunings = if (next == spec(target).defaults) tunings - target else tunings + (target to next))
    }

    fun resetStyle(target: NavStyle): LevyraNavigationStudioSettings = copy(tunings = tunings - target)

    fun normalized(): LevyraNavigationStudioSettings = copy(
        tunings = tunings
            .filterKeys { it != NavStyle.Original }
            .mapValues { (target, tuning) -> tuning.normalized(target) }
            .filter { (target, tuning) -> tuning != spec(target).defaults }
    )

    fun encode(): String = JSONObject().apply {
        put("version", STORAGE_VERSION)
        put("style", style.storageId)
        put("tunings", JSONObject().apply {
            normalized().tunings.forEach { (target, tuning) -> put(target.storageId, tuning.toJson()) }
        })
    }.toString()

    companion object {
        const val STORAGE_VERSION = 2

        private const val YT_MUSIC_CONTENT = 0xFFFFFFFF.toInt()
        private val allIndicators = NavIndicator.entries.toList()
        private val allBackgrounds = NavBackground.entries.toList()

        private val originalSpec = NavStyleSpec(
            height = 76..76,
            corner = 28..28,
            indicators = listOf(NavIndicator.Pill),
            backgrounds = listOf(NavBackground.Solid),
            defaults = NavTuning(heightDp = 76)
        )
        private val superExpressiveSpec = NavStyleSpec(
            height = 72..88,
            corner = 0..40,
            indicators = listOf(NavIndicator.Morph, NavIndicator.Pill, NavIndicator.Circle, NavIndicator.Floating),
            backgrounds = allBackgrounds,
            defaults = NavTuning(
                labels = NavLabelMode.Expanding,
                indicator = NavIndicator.Morph,
                background = NavBackground.Gradient,
                iconDp = 24,
                heightDp = 80,
                intensity = 70,
                cornerDp = 32,
                indicatorScale = 110
            )
        )
        private val floatingSpec = NavStyleSpec(
            height = 56..76,
            corner = 16..40,
            indicators = allIndicators,
            backgrounds = allBackgrounds,
            defaults = NavTuning(
                labels = NavLabelMode.Expanding,
                indicator = NavIndicator.Pill,
                background = NavBackground.Translucent,
                iconDp = 24,
                heightDp = 64,
                intensity = 55,
                cornerDp = 40,
                paddingDp = 6
            )
        )
        private val minimalSpec = NavStyleSpec(
            height = 52..72,
            corner = 0..32,
            indicators = listOf(NavIndicator.Dot, NavIndicator.Line, NavIndicator.Circle),
            backgrounds = allBackgrounds,
            defaults = NavTuning(
                labels = NavLabelMode.Expanding,
                indicator = NavIndicator.Dot,
                background = NavBackground.Solid,
                iconDp = 24,
                heightDp = 60,
                motion = NavMotion.Minimal,
                intensity = 30,
                inactiveAlpha = 80,
                cornerDp = 20,
                paddingDp = 12
            )
        )
        private val compactSpec = NavStyleSpec(
            height = 52..64,
            corner = 0..32,
            indicators = listOf(NavIndicator.Morph, NavIndicator.Pill, NavIndicator.Line, NavIndicator.Dot),
            backgrounds = allBackgrounds,
            defaults = NavTuning(
                labels = NavLabelMode.Expanding,
                indicator = NavIndicator.Morph,
                background = NavBackground.Solid,
                iconDp = 22,
                heightDp = 56,
                intensity = 50,
                cornerDp = 24,
                indicatorScale = 90
            )
        )
        private val liquidGlassSpec = NavStyleSpec(
            height = 56..76,
            corner = 20..40,
            indicators = listOf(NavIndicator.Floating, NavIndicator.Pill, NavIndicator.Circle, NavIndicator.Dot),
            backgrounds = listOf(NavBackground.Translucent),
            defaults = NavTuning(
                labels = NavLabelMode.Expanding,
                indicator = NavIndicator.Floating,
                background = NavBackground.Translucent,
                iconDp = 24,
                heightDp = 64,
                motion = NavMotion.Spring,
                intensity = 50,
                opacity = 70,
                cornerDp = 40,
                paddingDp = 6
            )
        )

        private val liquidCapsuleSpec = liquidGlassSpec.copy(
            height = 60..76,
            corner = 28..40,
            defaults = liquidGlassSpec.defaults.copy(
                labels = NavLabelMode.Always,
                indicator = NavIndicator.Floating,
                indicatorTone = NavIndicatorTone.Neutral,
                heightDp = 66,
                opacity = 58,
                cornerDp = 40,
                paddingDp = 5
            )
        )

        private val ytMusicSpec = NavStyleSpec(
            height = 56..72,
            corner = 0..0,
            indicators = listOf(NavIndicator.None, NavIndicator.Line, NavIndicator.Dot),
            backgrounds = listOf(NavBackground.Solid),
            defaults = NavTuning(
                labels = NavLabelMode.Always,
                indicator = NavIndicator.None,
                background = NavBackground.Solid,
                iconDp = 24,
                heightDp = 60,
                motion = NavMotion.Minimal,
                intensity = 0,
                colorSource = NavColorSource.Custom,
                accent = YT_MUSIC_CONTENT,
                surfaceTone = NavSurfaceTone.Amoled,
                cornerDp = 0
            )
        )

        private val splitDockSpec = NavStyleSpec(
            height = 56..72,
            corner = 20..36,
            indicators = listOf(NavIndicator.Floating, NavIndicator.Pill, NavIndicator.Morph, NavIndicator.Dot, NavIndicator.Line),
            backgrounds = allBackgrounds,
            defaults = NavTuning(
                labels = NavLabelMode.Expanding,
                indicator = NavIndicator.Floating,
                background = NavBackground.Solid,
                iconDp = 24,
                heightDp = 64,
                motion = NavMotion.Expressive,
                intensity = 60,
                cornerDp = 32,
                paddingDp = 6
            )
        )

        private val connectedDockSpec = NavStyleSpec(
            height = 56..72,
            corner = 20..36,
            indicators = listOf(NavIndicator.Pill, NavIndicator.Floating, NavIndicator.Morph, NavIndicator.Dot, NavIndicator.Line),
            backgrounds = allBackgrounds,
            defaults = NavTuning(
                labels = NavLabelMode.Expanding,
                indicator = NavIndicator.Pill,
                background = NavBackground.Solid,
                iconDp = 24,
                heightDp = 64,
                motion = NavMotion.Expressive,
                intensity = 50,
                cornerDp = 28,
                paddingDp = 4
            )
        )

        fun spec(style: NavStyle): NavStyleSpec = when (style) {
            NavStyle.Original -> originalSpec
            NavStyle.SuperExpressive -> superExpressiveSpec
            NavStyle.FloatingDock -> floatingSpec
            NavStyle.Minimal -> minimalSpec
            NavStyle.ExpressiveCompact -> compactSpec
            NavStyle.LiquidGlass -> liquidGlassSpec
            NavStyle.YtMusic -> ytMusicSpec
            NavStyle.SplitDock -> splitDockSpec
            NavStyle.ConnectedDock -> connectedDockSpec
            NavStyle.LiquidCapsule -> liquidCapsuleSpec
        }

        fun decode(raw: String?): LevyraNavigationStudioSettings {
            if (raw.isNullOrBlank()) return LevyraNavigationStudioSettings()
            return runCatching {
                val json = JSONObject(raw)
                val tuningsJson = json.optJSONObject("tunings")
                val tunings = buildMap {
                    tuningsJson?.keys()?.forEach { key ->
                        val target = NavStyle.entries.firstOrNull { it.storageId == key } ?: return@forEach
                        val tuning = tuningsJson.optJSONObject(key) ?: return@forEach
                        put(target, NavTuning.fromJson(tuning, target))
                    }
                }
                LevyraNavigationStudioSettings(style = NavStyle.from(json.optString("style")), tunings = tunings).normalized()
            }.getOrDefault(LevyraNavigationStudioSettings())
        }
    }
}
