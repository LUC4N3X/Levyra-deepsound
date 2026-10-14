package com.luc4n3x.levyra.domain

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LevyraCustomTheme(
    val id: String,
    val name: String,
    val isLight: Boolean,
    val background: Int,
    val surface: Int,
    val primary: Int,
    val secondary: Int,
    val accent: Int,
    val cornerScale: Int = DEFAULT_CORNER_SCALE
) {
    fun normalized(): LevyraCustomTheme = copy(
        name = name.trim().take(MAX_NAME_LENGTH).ifBlank { FALLBACK_NAME },
        background = background or OPAQUE,
        surface = surface or OPAQUE,
        primary = primary or OPAQUE,
        secondary = secondary or OPAQUE,
        accent = accent or OPAQUE,
        cornerScale = cornerScale.coerceIn(CORNER_SCALE_RANGE)
    )

    internal fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("light", isLight)
        put("background", background)
        put("surface", surface)
        put("primary", primary)
        put("secondary", secondary)
        put("accent", accent)
        put("corners", cornerScale)
    }

    companion object {
        const val ID_PREFIX = "custom:"
        const val MAX_NAME_LENGTH = 32
        const val DEFAULT_CORNER_SCALE = 100
        val CORNER_SCALE_RANGE = 60..140
        private const val FALLBACK_NAME = "Levyra"
        private const val OPAQUE = 0xFF000000.toInt()

        fun isCustomId(id: String?): Boolean = id != null && id.startsWith(ID_PREFIX) && id.length > ID_PREFIX.length

        fun newId(): String = ID_PREFIX + UUID.randomUUID().toString()

        internal fun fromJson(json: JSONObject): LevyraCustomTheme? {
            val id = json.optString("id")
            if (!isCustomId(id)) return null
            if (listOf("background", "surface", "primary", "secondary", "accent").any { !json.has(it) }) return null
            return LevyraCustomTheme(
                id = id,
                name = json.optString("name"),
                isLight = json.optBoolean("light", false),
                background = json.optInt("background"),
                surface = json.optInt("surface"),
                primary = json.optInt("primary"),
                secondary = json.optInt("secondary"),
                accent = json.optInt("accent"),
                cornerScale = json.optInt("corners", DEFAULT_CORNER_SCALE)
            ).normalized()
        }
    }
}

object LevyraCustomThemes {
    const val MAX_THEMES = 24
    private const val STORAGE_VERSION = 1

    fun normalized(themes: List<LevyraCustomTheme>): List<LevyraCustomTheme> =
        themes
            .filter { LevyraCustomTheme.isCustomId(it.id) }
            .distinctBy { it.id }
            .take(MAX_THEMES)
            .map { it.normalized() }

    fun encode(themes: List<LevyraCustomTheme>): String = JSONObject().apply {
        put("version", STORAGE_VERSION)
        put("themes", JSONArray(normalized(themes).map { it.toJson() }))
    }.toString()

    fun decode(raw: String?): List<LevyraCustomTheme> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONObject(raw).optJSONArray("themes") ?: return emptyList()
            normalized((0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(LevyraCustomTheme::fromJson) })
        }.getOrDefault(emptyList())
    }

    fun upsert(themes: List<LevyraCustomTheme>, theme: LevyraCustomTheme): List<LevyraCustomTheme> {
        val normalized = theme.normalized()
        val index = themes.indexOfFirst { it.id == normalized.id }
        return if (index >= 0) {
            themes.toMutableList().apply { set(index, normalized) }
        } else {
            normalized(themes + normalized)
        }
    }

    fun duplicate(themes: List<LevyraCustomTheme>, id: String, suffix: String): List<LevyraCustomTheme> {
        if (themes.size >= MAX_THEMES) return themes
        val source = themes.firstOrNull { it.id == id } ?: return themes
        val base = source.name.take((LevyraCustomTheme.MAX_NAME_LENGTH - suffix.length - 1).coerceAtLeast(1)).trimEnd()
        val copy = source.copy(id = LevyraCustomTheme.newId(), name = "$base $suffix")
        val index = themes.indexOf(source)
        return themes.toMutableList().apply { add(index + 1, copy.normalized()) }
    }

    fun remove(themes: List<LevyraCustomTheme>, id: String): List<LevyraCustomTheme> = themes.filterNot { it.id == id }
}
