package com.luc4n3x.levyra.desktop.core.moods

import com.luc4n3x.levyra.desktop.core.catalog.desktopArtistArtworkUrl
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class MoodCategory(val title: String, val params: String, val sectionIndex: Int, val artworkUrl: String = "")

data class MoodPlaylist(val id: String, val title: String, val subtitle: String, val artworkUrl: String)

data class MoodShelf(val title: String, val playlists: List<MoodPlaylist>, val moreParams: String = "")

fun moodBrowseOrder(categories: List<MoodCategory>, decades: List<MoodCategory>): List<MoodCategory> {
    val split = categories.indexOfLast { it.sectionIndex == 0 } + 1
    val merged = if (categories.isEmpty()) categories else categories.take(split) + decades + categories.drop(split)
    return merged.filter { it.title.isNotBlank() && it.params.isNotBlank() }
        .distinctBy { it.params }
        .sortedBy { category ->
            when {
                category.sectionIndex == 0 -> 0
                category.sectionIndex < 0 -> 1
                else -> 2
            }
        }
}

class MoodsRepository(
    private val client: OkHttpClient = ExtractorHttp.client,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val shelfCache = ConcurrentHashMap<String, List<MoodShelf>>()

    suspend fun categories(languageTag: String, country: String): List<MoodCategory> = withContext(dispatcher) {
        browse(languageTag, country, ROOT_BROWSE_ID, "")?.let(MoodsParser::categories).orEmpty()
    }

    suspend fun shelves(params: String, languageTag: String, country: String): List<MoodShelf> = withContext(dispatcher) {
        if (params.isBlank()) return@withContext emptyList()
        val key = "$languageTag|$country|$params"
        shelfCache[key]?.let { return@withContext it }
        val shelves = browse(languageTag, country, CATEGORY_BROWSE_ID, params)?.let(MoodsParser::shelves).orEmpty()
        if (shelves.isNotEmpty()) {
            if (shelfCache.size >= MAX_CACHED_SHELVES) shelfCache.clear()
            shelfCache[key] = shelves
        }
        shelves
    }

    suspend fun artwork(params: String, languageTag: String, country: String): String =
        shelves(params, languageTag, country).asSequence()
            .flatMap { it.playlists.asSequence() }
            .map { it.artworkUrl }
            .firstOrNull(String::isNotBlank)
            .orEmpty()

    suspend fun curatedShelves(languageTag: String, country: String): List<MoodShelf> = withContext(dispatcher) {
        CURATED_BROWSE_IDS.asSequence()
            .map { browseId -> browse(languageTag, country, browseId, "")?.let(MoodsParser::shelves).orEmpty() }
            .map { shelves -> shelves.filter { it.playlists.isNotEmpty() } }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
    }

    suspend fun decades(languageTag: String, country: String): List<MoodCategory> =
        MoodsParser.decades(shelves(DECADES_PARAMS, languageTag, country))

    private fun browse(languageTag: String, country: String, browseId: String, params: String): JSONObject? {
        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", CLIENT_VERSION)
            .put("hl", innerTubeLanguage(languageTag))
            .put("gl", country.trim().uppercase().take(2).ifBlank { "US" })
        val payload = JSONObject().put("context", JSONObject().put("client", client)).put("browseId", browseId)
        if (params.isNotBlank()) payload.put("params", params)
        val request = Request.Builder()
            .url(BROWSE_URL)
            .post(payload.toString().toRequestBody(JSON))
            .header("Content-Type", "application/json")
            .header("Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
            .header("X-Youtube-Client-Name", "67")
            .header("X-Youtube-Client-Version", CLIENT_VERSION)
            .build()
        return runCatching {
            this.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val bytes = response.peekBody(MAX_BYTES + 1).bytes()
                if (bytes.size > MAX_BYTES) null else JSONObject(bytes.toString(Charsets.UTF_8))
            }
        }.getOrNull()
    }

    companion object {
        const val DECADES_PARAMS = "ggMPOg1uX1V4cTVVT0JjY0xI"
        private val CURATED_BROWSE_IDS = listOf("FEmusic_mixed_for_you", "FEmusic_home")
        private const val ROOT_BROWSE_ID = "FEmusic_moods_and_genres"
        private const val CATEGORY_BROWSE_ID = "FEmusic_moods_and_genres_category"
        private const val ORIGIN = "https://music.youtube.com"
        private const val BROWSE_URL = "$ORIGIN/youtubei/v1/browse?prettyPrint=false"
        private const val CLIENT_VERSION = "1.20260804.16.00"
        private const val MAX_BYTES = 6L * 1024 * 1024
        private const val MAX_CACHED_SHELVES = 160
        private val JSON = "application/json".toMediaType()

        fun innerTubeLanguage(tag: String): String = when (tag) {
            "pt" -> "pt-PT"
            "zh" -> "zh-CN"
            "zh-Hant" -> "zh-TW"
            else -> tag.ifBlank { "en" }
        }
    }
}

internal object MoodsParser {
    private val shelfRenderers = setOf("gridRenderer", "musicCarouselShelfRenderer")
    private val shelfHeaders = listOf("gridHeaderRenderer", "musicCarouselShelfBasicHeaderRenderer")
    private const val FIRST_DECADE = 1950
    private const val LAST_DECADE = 2020

    fun categories(root: JSONObject): List<MoodCategory> {
        val result = LinkedHashMap<String, MoodCategory>()
        objects(root, setOf("gridRenderer")).forEachIndexed { sectionIndex, grid ->
            objects(grid.optJSONArray("items"), setOf("musicNavigationButtonRenderer")).forEach { button ->
                category(button, sectionIndex)?.let { result.putIfAbsent(it.params, it) }
            }
        }
        if (result.isEmpty()) {
            objects(root, setOf("musicNavigationButtonRenderer")).forEach { button ->
                category(button, -1)?.let { result.putIfAbsent(it.params, it) }
            }
        }
        return result.values.toList()
    }

    fun shelves(root: JSONObject): List<MoodShelf> {
        val seen = HashSet<String>()
        return objects(root, shelfRenderers).mapNotNull { shelf ->
            val header = shelf.optJSONObject("header")
            val title = shelfHeaders.firstNotNullOfOrNull { key ->
                header?.optJSONObject(key)?.optJSONObject("title")?.optJSONArray("runs")?.joinText()?.takeIf(String::isNotBlank)
            } ?: return@mapNotNull null
            val playlists = objects(shelf.optJSONArray("items") ?: shelf.optJSONArray("contents"), setOf("musicTwoRowItemRenderer"))
                .mapNotNull(::playlist)
                .filter { seen.add("$title|${it.id}") }
            if (playlists.isEmpty()) null else MoodShelf(title, playlists, moreParams(header))
        }
    }

    fun decades(shelves: List<MoodShelf>): List<MoodCategory> {
        val byDecade = sortedMapOf<Int, MoodCategory>()
        shelves.forEach { shelf ->
            val decade = decadeOf(shelf.title)?.takeIf { it in FIRST_DECADE..LAST_DECADE } ?: return@forEach
            if (decade in byDecade || shelf.moreParams.isBlank()) return@forEach
            val cover = shelf.playlists.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty()
            byDecade[decade] = MoodCategory(shelf.title, shelf.moreParams, -1, cover)
        }
        return byDecade.values.distinctBy { it.params }
    }

    fun decadeOf(title: String): Int? {
        val runs = ArrayList<String>()
        val digits = StringBuilder()
        (title + " ").forEach { char ->
            val digit = Character.digit(char, 10)
            if (digit >= 0) digits.append(digit) else if (digits.isNotEmpty()) { runs += digits.toString(); digits.clear() }
        }
        runs.firstOrNull { it.length == 4 && it.endsWith('0') }?.let { year -> return year.toInt().takeIf { it in 1900..2090 } }
        val short = runs.lastOrNull { it.length == 2 && it.endsWith('0') }?.toInt() ?: return null
        return if (short >= 30) 1900 + short else 2000 + short
    }

    private fun category(button: JSONObject, sectionIndex: Int): MoodCategory? {
        val title = (button.optJSONObject("buttonText") ?: button.optJSONObject("title"))?.optJSONArray("runs")?.joinText().orEmpty()
        val endpoint = button.optJSONObject("clickCommand")?.optJSONObject("browseEndpoint")
            ?: button.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            ?: return null
        val params = endpoint.optString("params")
        if (title.isBlank() || params.isBlank()) return null
        return MoodCategory(title, params, sectionIndex)
    }

    private fun playlist(card: JSONObject): MoodPlaylist? {
        val title = card.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty()
        if (title.isBlank()) return null
        val navigation = card.optJSONObject("navigationEndpoint")
        val browseId = navigation?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
        val id = navigation?.optJSONObject("watchEndpoint")?.optString("playlistId").orEmpty()
            .ifBlank { if (browseId.startsWith("VL")) browseId.removePrefix("VL") else "" }
        if (id.isBlank()) return null
        val subtitle = card.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        return MoodPlaylist(id, title, subtitle, desktopArtistArtworkUrl(bestThumbnail(card), 544))
    }

    private fun moreParams(header: JSONObject?): String =
        objects(header, setOf("browseEndpoint")).firstNotNullOfOrNull { endpoint ->
            endpoint.optString("params").takeIf { endpoint.optString("browseId") == "FEmusic_moods_and_genres_category" && it.isNotBlank() }
        }.orEmpty()

    private fun bestThumbnail(card: JSONObject): String {
        val arrays = ArrayList<JSONArray>()
        collect(card) { key, value -> if (key == "thumbnails" && value is JSONArray) arrays += value }
        return arrays.firstNotNullOfOrNull { array ->
            (0 until array.length()).mapNotNull(array::optJSONObject)
                .maxByOrNull { it.optInt("width") }
                ?.optString("url")
                ?.takeIf(String::isNotBlank)
        }.orEmpty()
    }

    private fun objects(value: Any?, keys: Set<String>): List<JSONObject> {
        val result = ArrayList<JSONObject>()
        collect(value) { key, child -> if (key in keys && child is JSONObject) result += child }
        return result
    }

    private fun collect(value: Any?, visit: (String, Any?) -> Unit) {
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                val child = value.opt(key)
                visit(key, child)
                collect(child, visit)
            }
            is JSONArray -> for (index in 0 until value.length()) collect(value.opt(index), visit)
        }
    }

    private fun JSONArray.joinText(): String =
        (0 until length()).joinToString("") { optJSONObject(it)?.optString("text").orEmpty() }.trim()
}
