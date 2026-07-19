package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Extracts playlist rows and continuation tokens from the response shapes currently returned by
 * YouTube Music's browse endpoint.
 *
 * Playlist pages do not always use the same shelf renderer. Regular playlists commonly use
 * musicPlaylistShelfRenderer, while auto-generated/library playlists may use musicShelfRenderer.
 * Continuation pages can likewise arrive through continuationContents or response actions.
 */
internal object YoutubeMusicPlaylistPageParser {
    private val shelfKeys = listOf(
        "musicPlaylistShelfRenderer",
        "musicShelfRenderer"
    )

    private val continuationShelfKeys = listOf(
        "musicPlaylistShelfContinuation",
        "musicShelfContinuation"
    )

    private val actionKeys = listOf(
        "appendContinuationItemsAction",
        "reloadContinuationItemsCommand"
    )

    fun renderers(root: JSONObject): List<JSONObject> {
        return selectedContainers(root)
            .flatMap(::renderersFromContainer)
    }

    fun continuation(root: JSONObject): String {
        selectedContainers(root).forEach { container ->
            continuationFromContainer(container).takeIf(String::isNotBlank)?.let { return it }
        }
        return ""
    }

    private fun selectedContainers(root: JSONObject): List<JSONObject> {
        root.optJSONObject("continuationContents")?.let { continuationContents ->
            continuationShelfKeys.forEach { key ->
                continuationContents.optJSONObject(key)?.let { return listOf(it) }
            }
        }

        val actionContainers = buildList {
            for (responseKey in listOf("onResponseReceivedActions", "onResponseReceivedEndpoints")) {
                val actions = root.optJSONArray(responseKey) ?: continue
                for (index in 0 until actions.length()) {
                    val action = actions.optJSONObject(index) ?: continue
                    for (key in actionKeys) {
                        action.optJSONObject(key)?.let { add(it) }
                    }
                }
            }
        }
        if (actionContainers.isNotEmpty()) return actionContainers

        canonicalSectionContents(root)?.let { contents ->
            shelfKeys.forEach { shelfKey ->
                for (index in 0 until contents.length()) {
                    contents.optJSONObject(index)?.optJSONObject(shelfKey)?.let { return listOf(it) }
                }
            }
        }

        shelfKeys.forEach { shelfKey ->
            val shelves = mutableListOf<JSONObject>()
            collectObjectsByKey(root, shelfKey, shelves)
            shelves.firstOrNull()?.let { return listOf(it) }
        }
        return emptyList()
    }

    private fun canonicalSectionContents(root: JSONObject): JSONArray? {
        val tabs = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?: return null
        for (index in 0 until tabs.length()) {
            val contents = tabs.optJSONObject(index)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
            if (contents != null) return contents
        }
        return null
    }

    private fun renderersFromContainer(container: JSONObject): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        for (arrayKey in listOf("contents", "continuationItems")) {
            val items = container.optJSONArray(arrayKey) ?: continue
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val direct = item.optJSONObject("musicResponsiveListItemRenderer")
                if (direct != null) {
                    result += direct
                } else {
                    val nested = mutableListOf<JSONObject>()
                    collectObjectsByKey(item, "musicResponsiveListItemRenderer", nested)
                    result.addAll(nested)
                }
            }
        }
        return result
    }

    private fun continuationFromContainer(container: JSONObject): String {
        container.optJSONArray("continuations")?.let { continuations ->
            for (index in 0 until continuations.length()) {
                val item = continuations.optJSONObject(index) ?: continue
                item.optJSONObject("nextContinuationData")
                    ?.optString("continuation")
                    ?.takeIf(String::isNotBlank)
                    ?.let { return it }
                item.optJSONObject("reloadContinuationData")
                    ?.optString("continuation")
                    ?.takeIf(String::isNotBlank)
                    ?.let { return it }
            }
        }

        for (arrayKey in listOf("contents", "continuationItems")) {
            val items = container.optJSONArray(arrayKey) ?: continue
            for (index in 0 until items.length()) {
                val continuationRenderer = items.optJSONObject(index)
                    ?.optJSONObject("continuationItemRenderer")
                    ?: continue
                continuationRenderer.optJSONObject("continuationEndpoint")
                    ?.optJSONObject("continuationCommand")
                    ?.optString("token")
                    ?.takeIf(String::isNotBlank)
                    ?.let { return it }
            }
        }

        val nextNodes = mutableListOf<JSONObject>()
        collectObjectsByKey(container, "nextContinuationData", nextNodes)
        nextNodes.forEach { node ->
            node.optString("continuation").takeIf(String::isNotBlank)?.let { return it }
        }
        val commandNodes = mutableListOf<JSONObject>()
        collectObjectsByKey(container, "continuationCommand", commandNodes)
        commandNodes.forEach { node ->
            node.optString("token").takeIf(String::isNotBlank)?.let { return it }
        }
        val reloadNodes = mutableListOf<JSONObject>()
        collectObjectsByKey(container, "reloadContinuationData", reloadNodes)
        reloadNodes.forEach { node ->
            node.optString("continuation").takeIf(String::isNotBlank)?.let { return it }
        }
        return ""
    }

    private fun collectObjectsByKey(value: Any?, targetKey: String, output: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val child = value.opt(key)
                    if (key == targetKey && child is JSONObject) output += child
                    collectObjectsByKey(child, targetKey, output)
                }
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    collectObjectsByKey(value.opt(index), targetKey, output)
                }
            }
        }
    }
}
