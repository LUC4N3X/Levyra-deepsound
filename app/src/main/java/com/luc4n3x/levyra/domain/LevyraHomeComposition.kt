package com.luc4n3x.levyra.domain

import org.json.JSONArray
import org.json.JSONObject

enum class HomeCompositionMode(val storageId: String) {
    Original("original"),
    Custom("custom"),
    Levyra("levyra");

    companion object {
        fun from(value: String?): HomeCompositionMode =
            entries.firstOrNull { it.storageId == value } ?: Original
    }
}

sealed interface HomeBlock {
    val storageId: String

    data class Section(val id: HomeSectionId) : HomeBlock {
        override val storageId: String get() = id.storageId
    }

    data object SkinFeed : HomeBlock {
        override val storageId: String = "skin_feed"
    }

    companion object {
        fun from(value: String?): HomeBlock? =
            if (value == SkinFeed.storageId) SkinFeed else HomeSectionId.from(value)?.let(::Section)
    }
}

object LevyraHomeCatalog {
    private val SkinsWithOwnFeed = setOf(
        LevyraSkin.AppleMusicStyle,
        LevyraSkin.YouTubeMusicStyle,
        LevyraSkin.QobuzStyle,
        LevyraSkin.Atelier
    )

    fun hasOwnFeed(skin: LevyraSkin): Boolean = skin in SkinsWithOwnFeed

    fun availableBlocks(skin: LevyraSkin): List<HomeBlock> = buildList {
        if (hasOwnFeed(skin)) add(HomeBlock.SkinFeed)
        addAll(levyraBlocks(skin))
    }

    fun levyraBlocks(skin: LevyraSkin): List<HomeBlock> =
        LevyraHomeStudioSettings.defaultLayout(HomeStyle.Original).order
            .filterNot { skin == LevyraSkin.SpotifyStyle && it == HomeSectionId.GenreChips }
            .map(HomeBlock::Section)

    fun originalBlocks(skin: LevyraSkin, studio: LevyraHomeStudioSettings): List<HomeBlock> {
        if (hasOwnFeed(skin)) return listOf(HomeBlock.SkinFeed)
        val supported = levyraBlocks(skin).toSet()
        val layout = studio.activeLayout()
        return layout.renderOrder()
            .filterNot(layout::isHidden)
            .map(HomeBlock::Section)
            .filter { it in supported }
    }
}

data class SkinHomeComposition(
    val mode: HomeCompositionMode = HomeCompositionMode.Original,
    val customBlocks: List<HomeBlock>? = null
) {
    fun isDefault(): Boolean = mode == HomeCompositionMode.Original && customBlocks == null
}

data class LevyraHomeCompositionSettings(
    val skins: Map<LevyraSkin, SkinHomeComposition> = emptyMap()
) {
    fun forSkin(skin: LevyraSkin): SkinHomeComposition = skins[skin] ?: SkinHomeComposition()

    fun modeFor(skin: LevyraSkin): HomeCompositionMode = forSkin(skin).mode

    fun resolvedBlocks(skin: LevyraSkin, studio: LevyraHomeStudioSettings): List<HomeBlock>? {
        val composition = forSkin(skin)
        return when (composition.mode) {
            HomeCompositionMode.Original -> null
            HomeCompositionMode.Levyra -> LevyraHomeCatalog.levyraBlocks(skin)
            HomeCompositionMode.Custom ->
                sanitize(skin, composition.customBlocks ?: LevyraHomeCatalog.originalBlocks(skin, studio))
        }
    }

    fun withMode(skin: LevyraSkin, mode: HomeCompositionMode): LevyraHomeCompositionSettings =
        updated(skin, forSkin(skin).copy(mode = mode))

    fun withCustomBlocks(skin: LevyraSkin, blocks: List<HomeBlock>): LevyraHomeCompositionSettings =
        updated(skin, SkinHomeComposition(HomeCompositionMode.Custom, sanitize(skin, blocks)))

    fun reset(skin: LevyraSkin): LevyraHomeCompositionSettings = copy(skins = skins - skin)

    fun normalized(): LevyraHomeCompositionSettings = copy(
        skins = skins
            .mapValues { (skin, value) -> value.copy(customBlocks = value.customBlocks?.let { sanitize(skin, it) }) }
            .filterValues { !it.isDefault() }
    )

    fun encode(): String = JSONObject().apply {
        put("version", STORAGE_VERSION)
        put("skins", JSONObject().apply {
            normalized().skins.forEach { (skin, value) ->
                put(skin.name, JSONObject().apply {
                    put("mode", value.mode.storageId)
                    value.customBlocks?.let { blocks -> put("blocks", JSONArray(blocks.map { it.storageId })) }
                })
            }
        })
    }.toString()

    private fun updated(skin: LevyraSkin, value: SkinHomeComposition): LevyraHomeCompositionSettings =
        copy(skins = if (value.isDefault()) skins - skin else skins + (skin to value))

    private fun sanitize(skin: LevyraSkin, blocks: List<HomeBlock>): List<HomeBlock> {
        val supported = LevyraHomeCatalog.availableBlocks(skin).toSet()
        return blocks.distinct().filter { it in supported }
    }

    companion object {
        const val STORAGE_VERSION = 1

        fun decode(raw: String?): LevyraHomeCompositionSettings {
            if (raw.isNullOrBlank()) return LevyraHomeCompositionSettings()
            return runCatching {
                val skinsJson = JSONObject(raw).optJSONObject("skins") ?: JSONObject()
                val skins = buildMap {
                    skinsJson.keys().forEach { key ->
                        val skin = LevyraSkin.entries.firstOrNull { it.name == key } ?: return@forEach
                        val json = skinsJson.optJSONObject(key) ?: return@forEach
                        val blocks = json.optJSONArray("blocks")?.let { array ->
                            (0 until array.length()).mapNotNull { HomeBlock.from(array.optString(it)) }
                        }
                        put(skin, SkinHomeComposition(HomeCompositionMode.from(json.optString("mode")), blocks))
                    }
                }
                LevyraHomeCompositionSettings(skins).normalized()
            }.getOrDefault(LevyraHomeCompositionSettings())
        }
    }
}
