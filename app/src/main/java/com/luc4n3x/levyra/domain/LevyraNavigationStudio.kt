package com.luc4n3x.levyra.domain

import org.json.JSONArray
import org.json.JSONObject

enum class NavStyle(val storageId: String) {
    Original("original"),
    SuperExpressive("super_expressive"),
    FloatingDock("floating_dock"),
    Minimal("minimal"),
    ExpressiveCompact("expressive_compact");

    companion object {
        fun from(value: String?): NavStyle = entries.firstOrNull { it.storageId == value } ?: Original
    }
}

enum class NavLabelMode(val storageId: String) {
    Always("always"),
    ActiveOnly("active"),
    Never("never");

    companion object {
        fun from(value: String?): NavLabelMode? = entries.firstOrNull { it.storageId == value }
    }
}

enum class NavIndicator(val storageId: String) {
    Pill("pill"),
    Morph("morph"),
    Dot("dot"),
    Line("line");

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

object NavDestinations {
    val defaultOrder: List<LevyraTab> = listOf(LevyraTab.Home, LevyraTab.Search, LevyraTab.Explore, LevyraTab.Library)
    val essential: Set<LevyraTab> = setOf(LevyraTab.Home, LevyraTab.Search)

    fun storageId(tab: LevyraTab): String = tab.name.lowercase()

    fun from(value: String?): LevyraTab? = defaultOrder.firstOrNull { storageId(it) == value }
}

data class NavStyleSpec(
    val height: IntRange,
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
    val expressiveMotion: Boolean = true,
    val intensity: Int = 60
) {
    fun visibleOrder(): List<LevyraTab> = order.filterNot { it in hidden }

    fun normalized(style: NavStyle): NavTuning {
        val spec = LevyraNavigationStudioSettings.spec(style)
        val known = order.filter { it in NavDestinations.defaultOrder }.distinct()
        val merged = known + NavDestinations.defaultOrder.filterNot { it in known }
        return copy(
            order = merged,
            hidden = hidden.filter { it in NavDestinations.defaultOrder && it !in NavDestinations.essential }.toSet(),
            iconDp = iconDp.coerceIn(ICON_RANGE),
            heightDp = heightDp.coerceIn(spec.height),
            intensity = intensity.coerceIn(0, 100)
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
        put("expressive", expressiveMotion)
        put("intensity", intensity)
    }

    companion object {
        val ICON_RANGE = 20..30

        internal fun fromJson(json: JSONObject, style: NavStyle): NavTuning {
            val defaults = LevyraNavigationStudioSettings.spec(style).defaults
            fun JSONArray.tabs(): List<LevyraTab> = (0 until length()).mapNotNull { NavDestinations.from(optString(it)) }
            return NavTuning(
                order = json.optJSONArray("order")?.tabs() ?: defaults.order,
                hidden = json.optJSONArray("hidden")?.tabs()?.toSet() ?: defaults.hidden,
                labels = NavLabelMode.from(json.optString("labels")) ?: defaults.labels,
                indicator = NavIndicator.from(json.optString("indicator")) ?: defaults.indicator,
                background = NavBackground.from(json.optString("background")) ?: defaults.background,
                iconDp = json.optInt("icon", defaults.iconDp),
                heightDp = json.optInt("height", defaults.heightDp),
                expressiveMotion = json.optBoolean("expressive", defaults.expressiveMotion),
                intensity = json.optInt("intensity", defaults.intensity)
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
        const val STORAGE_VERSION = 1

        private val originalSpec = NavStyleSpec(height = 76..76, defaults = NavTuning(heightDp = 76))
        private val superExpressiveSpec = NavStyleSpec(
            height = 72..88,
            defaults = NavTuning(indicator = NavIndicator.Morph, background = NavBackground.Gradient, iconDp = 24, heightDp = 80, intensity = 70)
        )
        private val floatingSpec = NavStyleSpec(
            height = 56..76,
            defaults = NavTuning(
                labels = NavLabelMode.ActiveOnly,
                indicator = NavIndicator.Pill,
                background = NavBackground.Translucent,
                iconDp = 24,
                heightDp = 64,
                intensity = 55
            )
        )
        private val minimalSpec = NavStyleSpec(
            height = 52..72,
            defaults = NavTuning(
                labels = NavLabelMode.Never,
                indicator = NavIndicator.Dot,
                background = NavBackground.Solid,
                iconDp = 24,
                heightDp = 60,
                expressiveMotion = false,
                intensity = 30
            )
        )
        private val compactSpec = NavStyleSpec(
            height = 52..64,
            defaults = NavTuning(
                labels = NavLabelMode.ActiveOnly,
                indicator = NavIndicator.Morph,
                background = NavBackground.Solid,
                iconDp = 22,
                heightDp = 56,
                intensity = 50
            )
        )

        fun spec(style: NavStyle): NavStyleSpec = when (style) {
            NavStyle.Original -> originalSpec
            NavStyle.SuperExpressive -> superExpressiveSpec
            NavStyle.FloatingDock -> floatingSpec
            NavStyle.Minimal -> minimalSpec
            NavStyle.ExpressiveCompact -> compactSpec
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
