package com.luc4n3x.levyra.data.youtubemusic

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

internal data class YoutubeMusicAccount(
    val id: String,
    val name: String,
    val handle: String,
    val avatarUrl: String,
    val pageId: String?,
    val authUser: Int,
    val selected: Boolean = false
)

internal object YoutubeMusicAccountParser {
    private val authUserPattern = Regex("[?&]authuser=(\\d+)")
    private const val EDIT_ICON = "EDIT"
    private const val LIBRARY_ICON_PREFIX = "LIBRARY_"
    private val MENU_ICON_KEYS = listOf("icon", "defaultIcon", "toggledIcon")

    fun accounts(root: JSONObject): List<YoutubeMusicAccount> {
        if (root.optString("code") != "SUCCESS" || root.optJSONObject("data") == null) {
            throw YoutubeMusicProtocolException()
        }
        return objects(root.getJSONObject("data"), "accountItem").mapNotNull { item ->
            if (item.optBoolean("isDisabled")) return@mapNotNull null
            val name = text(item.optJSONObject("accountName"))
            if (name.isBlank()) return@mapNotNull null
            val tokens = item.optJSONObject("serviceEndpoint")
                ?.optJSONObject("selectActiveIdentityEndpoint")?.optJSONArray("supportedTokens")
            val signinUrl = objects(tokens, "accountSigninToken").firstOrNull()?.optString("signinUrl").orEmpty()
            val authUser = authUserPattern.find(signinUrl)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val pageId = item.optString("onBehalfOfParameter").takeIf { it.isNotBlank() && it != "null" }
                ?: objects(tokens, "pageIdToken").firstOrNull()?.optString("pageId")?.takeIf(String::isNotBlank)
            val handle = text(item.optJSONObject("channelHandle")).ifBlank { text(item.optJSONObject("accountByline")) }
            YoutubeMusicAccount(
                identity(handle.ifBlank { signinUrl.ifBlank { name } }, pageId, authUser), name,
                handle, thumbnail(item.optJSONObject("accountPhoto")), pageId, authUser, item.optBoolean("isSelected")
            )
        }.distinctBy { it.id }
    }

    fun activeAccount(root: JSONObject, previous: YoutubeMusicAccount): YoutubeMusicAccount {
        val header = objects(root, "activeAccountHeaderRenderer").firstOrNull() ?: throw YoutubeMusicSessionExpired()
        val name = text(header.optJSONObject("accountName")).takeIf(String::isNotBlank) ?: throw YoutubeMusicProtocolException()
        return previous.copy(
            name = name,
            handle = text(header.optJSONObject("channelHandle")).ifBlank { previous.handle },
            avatarUrl = thumbnail(header.optJSONObject("accountPhoto")).ifBlank { previous.avatarUrl }
        )
    }

    fun identity(handle: String, pageId: String?, authUser: Int): String =
        MessageDigest.getInstance("SHA-256").digest("$handle|${pageId.orEmpty()}|$authUser".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun content(root: JSONObject): Any? {
        root.optJSONObject("continuationContents")?.let { return it }
        val actions = root.optJSONArray("onResponseReceivedActions") ?: root.optJSONArray("onResponseReceivedEndpoints")
        if (actions != null) return actions
        val contents = root.optJSONObject("contents") ?: return null
        val tabs = contents.optJSONObject("singleColumnBrowseResultsRenderer")?.optJSONArray("tabs")
            ?: contents.optJSONObject("twoColumnBrowseResultsRenderer")?.optJSONArray("tabs")
        if (tabs != null) {
            val renderers = (0 until tabs.length()).mapNotNull { tabs.optJSONObject(it)?.optJSONObject("tabRenderer") }
            return (renderers.firstOrNull { it.optBoolean("selected") } ?: renderers.firstOrNull())?.optJSONObject("content")
        }
        return contents
    }

    fun ownedPlaylistBrowseIds(content: Any?): Set<String> = buildSet {
        listOf("musicTwoRowItemRenderer", "musicResponsiveListItemRenderer").forEach { rendererKey ->
            objects(content, rendererKey).forEach { renderer ->
                val menu = renderer.optJSONObject("menu") ?: return@forEach
                val browseId = objects(renderer, "browseEndpoint")
                    .map { endpoint -> endpoint.optString("browseId") }
                    .firstOrNull { id -> id.startsWith("VL") }
                    ?: return@forEach
                val icons = MENU_ICON_KEYS.flatMap { key -> objects(menu, key) }.map { icon -> icon.optString("iconType") }
                val editable = EDIT_ICON in icons
                val libraryToggle = icons.any { icon -> icon.startsWith(LIBRARY_ICON_PREFIX) }
                val linkedAuthor = objects(renderer.optJSONObject("subtitle"), "browseEndpoint").isNotEmpty()
                if (editable || (!libraryToggle && !linkedAuthor)) add(browseId)
            }
        }
    }

    fun continuation(root: JSONObject): String {
        val body = content(root) ?: return ""
        return objects(body, "nextContinuationData").firstOrNull()?.optString("continuation")?.takeIf(String::isNotBlank)
            ?: objects(body, "continuationItemRenderer").firstNotNullOfOrNull {
                it.optJSONObject("continuationEndpoint")?.optJSONObject("continuationCommand")
                    ?.optString("token")?.takeIf(String::isNotBlank)
            }.orEmpty()
    }

    fun text(value: JSONObject?): String {
        if (value == null) return ""
        value.optString("simpleText").takeIf(String::isNotBlank)?.let { return it }
        val runs = value.optJSONArray("runs") ?: return ""
        return (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() }
    }

    fun thumbnail(value: Any?): String = objects(value, "thumbnails").lastOrNull()?.let { node ->
        node.optString("url")
    }.orEmpty().ifBlank {
        val arrays = mutableListOf<JSONArray>()
        visit(value) { key, child -> if (key == "thumbnails" && child is JSONArray) arrays += child }
        arrays.flatMap { array -> (0 until array.length()).mapNotNull { array.optJSONObject(it) } }
            .maxByOrNull { it.optInt("width") }?.optString("url").orEmpty()
    }

    fun objects(value: Any?, target: String): List<JSONObject> = buildList {
        visit(value) { key, child -> if (key == target && child is JSONObject) add(child) }
    }

    private fun visit(value: Any?, depth: Int = 0, onValue: (String, Any?) -> Unit) {
        if (depth > 64) throw YoutubeMusicProtocolException()
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                val child = value.opt(key)
                onValue(key, child)
                visit(child, depth + 1, onValue)
            }
            is JSONArray -> for (index in 0 until value.length()) visit(value.opt(index), depth + 1, onValue)
        }
    }
}
