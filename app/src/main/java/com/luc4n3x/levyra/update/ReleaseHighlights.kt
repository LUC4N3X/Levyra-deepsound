package com.luc4n3x.levyra.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val RELEASE_HIGHLIGHTS_MARKER = "<!-- levyra-highlights"
private const val RELEASE_HIGHLIGHTS_END = "-->"
private const val RELEASE_HIGHLIGHTS_MAX_ITEMS = 6
private const val RELEASE_HIGHLIGHT_MAX_LENGTH = 240

internal fun releaseNoteHighlights(releaseNotes: String): Map<String, List<String>> {
    val start = releaseNotes.indexOf(RELEASE_HIGHLIGHTS_MARKER)
    if (start < 0) return emptyMap()
    val end = releaseNotes.indexOf(RELEASE_HIGHLIGHTS_END, start + RELEASE_HIGHLIGHTS_MARKER.length)
    if (end < 0) return emptyMap()
    val payload = releaseNotes.substring(start + RELEASE_HIGHLIGHTS_MARKER.length, end)
    val root = runCatching { Json.parseToJsonElement(payload) as? JsonObject }.getOrNull() ?: return emptyMap()
    return root.mapNotNull { (code, value) ->
        val items = (value as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content?.trim() }
            .filter { it.isNotEmpty() && it.length <= RELEASE_HIGHLIGHT_MAX_LENGTH }
            .take(RELEASE_HIGHLIGHTS_MAX_ITEMS)
        items.takeIf { it.isNotEmpty() }?.let { code to it }
    }.toMap()
}
