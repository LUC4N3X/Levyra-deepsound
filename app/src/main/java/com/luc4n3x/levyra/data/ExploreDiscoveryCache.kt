package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import org.json.JSONArray
import org.json.JSONObject

internal data class ExploreDiscoverySnapshot(
    val languageCode: String,
    val savedAtMs: Long,
    val categories: List<ExploreCategory>,
    val artwork: Map<String, String>
) {
    fun isFresh(
        nowMs: Long = System.currentTimeMillis(),
        maxAgeMs: Long = EXPLORE_DISCOVERY_CACHE_TTL_MS
    ): Boolean = categories.isNotEmpty() && savedAtMs > 0L && nowMs - savedAtMs in 0 until maxAgeMs
}

internal fun encodeExploreDiscoverySnapshot(
    languageCode: String,
    categories: List<ExploreCategory>,
    artwork: Map<String, String>,
    savedAtMs: Long = System.currentTimeMillis()
): String {
    val normalizedLanguage = LevyraLanguageCatalog.normalize(languageCode)
    val sanitizedCategories = categories.asSequence()
        .filter { category -> category.title.isNotBlank() && category.params.isNotBlank() }
        .distinctBy { category -> category.params }
        .take(EXPLORE_DISCOVERY_MAX_CATEGORIES)
        .toList()
    val validParams = sanitizedCategories.mapTo(HashSet()) { category -> category.params }
    val categoriesJson = JSONArray()
    sanitizedCategories.forEach { category ->
        categoriesJson.put(
            JSONObject()
                .put("title", category.title)
                .put("params", category.params)
                .put("section", category.section)
                .put("sectionIndex", category.sectionIndex)
        )
    }
    val artworkJson = JSONObject()
    artwork.forEach { (params, url) ->
        if (params in validParams && url.isNotBlank()) artworkJson.put(params, url)
    }
    return JSONObject()
        .put("schema", EXPLORE_DISCOVERY_SCHEMA)
        .put("languageCode", normalizedLanguage)
        .put("savedAtMs", savedAtMs)
        .put("categories", categoriesJson)
        .put("artwork", artworkJson)
        .toString()
}

internal fun decodeExploreDiscoverySnapshot(
    raw: String,
    languageCode: String
): ExploreDiscoverySnapshot? {
    if (raw.isBlank()) return null
    val normalizedLanguage = LevyraLanguageCatalog.normalize(languageCode)
    return runCatching {
        val root = JSONObject(raw)
        if (root.optInt("schema") != EXPLORE_DISCOVERY_SCHEMA) return null
        if (LevyraLanguageCatalog.normalize(root.optString("languageCode")) != normalizedLanguage) return null
        val categoriesJson = root.optJSONArray("categories") ?: JSONArray()
        val categories = LinkedHashMap<String, ExploreCategory>()
        for (index in 0 until categoriesJson.length()) {
            val item = categoriesJson.optJSONObject(index) ?: continue
            val title = item.optString("title").trim()
            val params = item.optString("params").trim()
            if (title.isBlank() || params.isBlank()) continue
            categories.putIfAbsent(
                params,
                ExploreCategory(
                    title = title,
                    params = params,
                    section = item.optString("section").trim(),
                    sectionIndex = item.optInt("sectionIndex", -1)
                )
            )
            if (categories.size >= EXPLORE_DISCOVERY_MAX_CATEGORIES) break
        }
        if (categories.isEmpty()) return null
        val artworkJson = root.optJSONObject("artwork") ?: JSONObject()
        val artwork = LinkedHashMap<String, String>()
        val keys = artworkJson.keys()
        while (keys.hasNext()) {
            val params = keys.next()
            val url = artworkJson.optString(params).trim()
            if (params in categories && url.isNotBlank()) artwork[params] = url
        }
        ExploreDiscoverySnapshot(
            languageCode = normalizedLanguage,
            savedAtMs = root.optLong("savedAtMs", 0L),
            categories = categories.values.toList(),
            artwork = artwork
        )
    }.getOrNull()
}

internal fun exploreArtworkWarmCandidates(
    categories: List<ExploreCategory>,
    artwork: Map<String, String>,
    limit: Int = EXPLORE_DISCOVERY_ARTWORK_WARM_LIMIT
): List<ExploreCategory> {
    if (limit <= 0) return emptyList()
    return categories.asSequence()
        .filter { category ->
            category.params.isNotBlank() && artwork[category.params].isNullOrBlank()
        }
        .distinctBy { category -> category.params }
        .take(limit)
        .toList()
}

internal const val EXPLORE_DISCOVERY_CACHE_TTL_MS = 12L * 60L * 60L * 1000L
internal const val EXPLORE_DISCOVERY_ARTWORK_WARM_LIMIT = 12
private const val EXPLORE_DISCOVERY_SCHEMA = 1
private const val EXPLORE_DISCOVERY_MAX_CATEGORIES = 120
