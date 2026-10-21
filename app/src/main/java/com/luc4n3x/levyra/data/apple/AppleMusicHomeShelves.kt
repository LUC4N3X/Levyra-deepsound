package com.luc4n3x.levyra.data.apple

enum class AppleHomeShelfRole { Hero, Songs, Cards, Charts }

data class AppleHomeShelf(
    val shelf: AppleCatalogShelf,
    val role: AppleHomeShelfRole
)

object AppleMusicHomeShelves {
    private val chartTitle = Regex("^Top\\s+\\d+\\s*[:：]", RegexOption.IGNORE_CASE)
    private const val EXCLUSIVE_MARKER = "apple music"

    fun select(page: AppleCatalogPage?): List<AppleHomeShelf> {
        page ?: return emptyList()
        val selected = ArrayList<AppleHomeShelf>()
        var chartShelves = 0
        for (shelf in page.shelves) {
            if (chartShelves >= 2) break
            val role = roleOf(shelf) ?: continue
            if (role != AppleHomeShelfRole.Hero && shelf.title.isBlank()) continue
            if (shelf.title.lowercase().contains(EXCLUSIVE_MARKER)) continue
            val playable = shelf.items.filter { it.kind != AppleCatalogItemKind.Other && it.kind != AppleCatalogItemKind.Video }
            if (playable.isEmpty()) continue
            if (role == AppleHomeShelfRole.Charts) chartShelves++
            selected += AppleHomeShelf(shelf.copy(items = playable), role)
        }
        return selected
    }

    fun chartParts(title: String): Pair<String, String>? {
        val match = chartTitle.find(title) ?: return null
        val badge = title.substring(0, match.range.last).trim()
        val name = title.substring(match.range.last + 1).trim()
        if (name.isBlank()) return null
        return badge to name
    }

    fun roleOf(shelf: AppleCatalogShelf): AppleHomeShelfRole? = when (shelf.kind) {
        AppleCatalogShelfKind.Hero -> AppleHomeShelfRole.Hero
        AppleCatalogShelfKind.Songs -> AppleHomeShelfRole.Songs
        AppleCatalogShelfKind.Grid -> if (isChartShelf(shelf)) AppleHomeShelfRole.Charts else AppleHomeShelfRole.Cards
        else -> null
    }

    private fun isChartShelf(shelf: AppleCatalogShelf): Boolean {
        val playlists = shelf.items.filter { it.kind == AppleCatalogItemKind.Playlist }
        return playlists.isNotEmpty() && playlists.size * 2 >= shelf.items.size && playlists.count { chartParts(it.title) != null } * 2 >= playlists.size
    }
}
