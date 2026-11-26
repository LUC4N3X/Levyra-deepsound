package com.luc4n3x.levyra.desktop.core.moods

import com.luc4n3x.levyra.desktop.core.catalog.desktopArtistArtworkUrl
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.extractor.YoutubeMusicBrowse
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

data class MoodCategory(val title: String, val params: String, val sectionIndex: Int, val artworkUrl: String = "", val section: String = "")

data class MoodPlaylist(val id: String, val title: String, val subtitle: String, val artworkUrl: String)

data class MoodShelf(
    val title: String,
    val playlists: List<MoodPlaylist>,
    val moreParams: String = "",
    val subtitle: String = "",
    val browseId: String = "",
    val playlistOnly: Boolean = true
)

data class CuratedCollections(
    val decades: List<MoodPlaylist>,
    val everyday: List<MoodPlaylist>,
    val categories: List<MoodCategory>
)

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
    client: OkHttpClient = ExtractorHttp.client,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val music = YoutubeMusicBrowse(client)
    private val shelfCache = ConcurrentHashMap<String, List<MoodShelf>>()

    suspend fun categories(languageTag: String, country: String): List<MoodCategory> = withContext(dispatcher) {
        music.browse(languageTag, country, ROOT_BROWSE_ID, "")?.let(MoodsParser::categories).orEmpty()
    }

    suspend fun shelves(params: String, languageTag: String, country: String): List<MoodShelf> = withContext(dispatcher) {
        if (params.isBlank()) return@withContext emptyList()
        val key = "$languageTag|$country|$params"
        shelfCache[key]?.let { return@withContext it }
        val shelves = music.browse(languageTag, country, CATEGORY_BROWSE_ID, params)?.let(MoodsParser::shelves).orEmpty()
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
        val mixed = async { music.browse(languageTag, country, MIXED_BROWSE_ID, "")?.let(MoodsParser::homeShelves).orEmpty() }
        val home = async { curatedHome(languageTag, country) }
        val mixedShelves = mixed.await()
        val homeShelves = home.await()
        val source = mixedShelves.filter { it.playlistOnly }
            .ifEmpty { homeShelves.filter { it.browseId == ROOT_BROWSE_ID && it.playlistOnly } }
            .ifEmpty { homeShelves.filter { it.playlistOnly } }
            .ifEmpty { homeShelves }
        if (source.isEmpty()) return@withContext emptyList()
        val featured = homeShelves.asSequence()
            .filter { it.browseId.startsWith("FEmusic_new_releases") }
            .flatMap { it.playlists.asSequence() }
            .firstOrNull { it.artworkUrl.isNotBlank() }
            ?: music.browse(languageTag, country, "VL$FEATURED_PLAYLIST_ID", "")?.let { MoodsParser.featuredPlaylist(it, FEATURED_PLAYLIST_ID) }
        val ordered = MoodsParser.orderCurated(source, featured)
        val lead = ordered.first()
        val missing = FEATURED_SHELF_SIZE - lead.playlists.size
        if (missing <= 0) return@withContext ordered
        val used = lead.playlists.mapTo(HashSet()) { it.id }
        val additions = homeShelves.asSequence().flatMap { it.playlists.asSequence() }
            .filter { it.artworkUrl.isNotBlank() && used.add(it.id) }.take(missing).toList()
        val decadePicks = if (additions.size < missing) {
            MoodsParser.decadePlaylists(shelves(DECADES_PARAMS, languageTag, country))
                .filter { used.add(it.id) }.take(missing - additions.size)
        } else emptyList()
        listOf(lead.copy(playlists = lead.playlists + additions + decadePicks)) + ordered.drop(1)
    }

    private suspend fun curatedHome(languageTag: String, country: String): List<MoodShelf> {
        val shelves = mutableListOf<MoodShelf>()
        val visited = HashSet<String>()
        var root = music.browse(languageTag, country, HOME_BROWSE_ID, "")
        var visitor = ""
        for (page in 1..FEATURED_PAGES) {
            coroutineContext.ensureActive()
            val current = root ?: break
            shelves += MoodsParser.homeShelves(current)
            if (page == FEATURED_PAGES || shelves.any { it.browseId == ROOT_BROWSE_ID && it.playlistOnly }) break
            val next = MoodsParser.continuation(current)?.takeIf(visited::add) ?: break
            visitor = current.optJSONObject("responseContext")?.optString("visitorData").orEmpty().ifBlank { visitor }
            root = music.browse(languageTag, country, "", "", next, visitor)
        }
        return shelves + music.browse(languageTag, country, EXPLORE_BROWSE_ID, "")?.let(MoodsParser::homeShelves).orEmpty()
    }

    suspend fun curatedCollections(languageTag: String, country: String): CuratedCollections = withContext(dispatcher) {
        val decadeRequest = async { shelves(DECADES_PARAMS, languageTag, country) }
        val providerCategories = categories(languageTag, country)
        val byParams = providerCategories.associateBy { it.params }
        val everydayRequests = EVERYDAY_PARAMS.mapNotNull { params ->
            val category = byParams[params] ?: return@mapNotNull null
            async { category.title to curatedCategory(params, languageTag, country, limit = 8) }
        }
        val used = HashSet<String>()
        val everyday = everydayRequests.awaitAll().mapNotNull { (title, playlists) ->
            playlists.firstOrNull { it.artworkUrl.isNotBlank() && used.add(it.id) }?.copy(subtitle = title)
        }
        val decades = decadeRequest.await()
        CuratedCollections(
            decades = MoodsParser.decadePlaylists(decades),
            everyday = everyday,
            categories = providerCategories.filter { it.sectionIndex >= 0 && it.params != DECADES_PARAMS && it.section.isNotBlank() } +
                MoodsParser.decades(decades)
        )
    }

    suspend fun curatedCategory(params: String, languageTag: String, country: String, limit: Int = 40): List<MoodPlaylist> = withContext(dispatcher) {
        if (params.isBlank()) return@withContext emptyList()
        music.browse(languageTag, country, CATEGORY_BROWSE_ID, params)?.let(MoodsParser::playlists).orEmpty().take(limit)
    }

    suspend fun decades(languageTag: String, country: String): List<MoodCategory> =
        MoodsParser.decades(shelves(DECADES_PARAMS, languageTag, country))

    companion object {
        const val DECADES_PARAMS = "ggMPOg1uX1V4cTVVT0JjY0xI"
        private const val HOME_BROWSE_ID = "FEmusic_home"
        private const val MIXED_BROWSE_ID = "FEmusic_mixed_for_you"
        private const val EXPLORE_BROWSE_ID = "FEmusic_explore"
        private const val FEATURED_PAGES = 3
        private const val FEATURED_SHELF_SIZE = 7
        private const val FEATURED_PLAYLIST_ID = "RDTMAK5uy_lr0LWzGrq6FU9GIxWvFHTRPQD2LHMqlFA"
        private val EVERYDAY_PARAMS = listOf(
            "ggMPOg1uXzIxYkNac21YZ2Z0", "ggMPOg1uX0NvNGNhWThMYWRh",
            "ggMPOg1uX1JOQWZFeDByc2Jm", "ggMPOg1uX1MxaFQ3Z0JMZkN4",
            "ggMPOg1uX2w1aW1CRDFTSUNo", "ggMPOg1uX044Z2o5WERLckpU",
            "ggMPOg1uXzZQbDB5eThLRTQ3"
        )
        private const val ROOT_BROWSE_ID = "FEmusic_moods_and_genres"
        private const val CATEGORY_BROWSE_ID = "FEmusic_moods_and_genres_category"
        private const val MAX_CACHED_SHELVES = 160

        fun innerTubeLanguage(tag: String): String = YoutubeMusicBrowse.innerTubeLanguage(tag)
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
            val section = grid.optJSONObject("header")?.optJSONObject("gridHeaderRenderer")?.text("title").orEmpty()
            objects(grid.optJSONArray("items"), setOf("musicNavigationButtonRenderer")).forEach { button ->
                category(button, sectionIndex)?.copy(section = section)?.let { result.putIfAbsent(it.params, it) }
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

    fun homeShelves(root: JSONObject): List<MoodShelf> =
        objects(root, setOf("musicCarouselShelfRenderer")).mapNotNull { shelf ->
            val header = shelf.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                ?: return@mapNotNull null
            val title = header.text("title")
            val contents = shelf.optJSONArray("contents") ?: return@mapNotNull null
            val candidates = (0 until contents.length()).mapNotNull(contents::optJSONObject)
                .filterNot { it.has("musicNavigationButtonRenderer") }
            val playlists = candidates.map { entry -> objects(entry, setOf("musicTwoRowItemRenderer")).firstNotNullOfOrNull(::playlist) }
            val items = playlists.filterNotNull().distinctBy { it.id }
            if (title.isBlank() || items.isEmpty()) return@mapNotNull null
            val browseId = header.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
            MoodShelf(title, items, subtitle = header.text("strapline"), browseId = browseId, playlistOnly = playlists.none { it == null })
        }

    fun playlists(root: JSONObject): List<MoodPlaylist> =
        objects(root, setOf("musicTwoRowItemRenderer")).mapNotNull(::playlist).distinctBy { it.id }

    fun orderCurated(shelves: List<MoodShelf>, featured: MoodPlaylist?): List<MoodShelf> {
        val ordered = shelves.map { shelf ->
            val chart = shelf.playlists.firstOrNull { it.title.contains("Trending 20", ignoreCase = true) }
            if (chart == null) shelf else shelf.copy(playlists = listOf(chart) + shelf.playlists.filterNot { it.id == chart.id })
        }
        if (featured == null) return ordered
        if (ordered.any { shelf -> shelf.playlists.any { it.id == featured.id } }) {
            if (featured.artworkUrl.isBlank()) return ordered
            return ordered.map { shelf ->
                shelf.copy(playlists = shelf.playlists.map { if (it.id == featured.id) it.copy(artworkUrl = featured.artworkUrl) else it })
            }
        }
        return ordered.mapIndexed { index, shelf ->
            if (index != 0) shelf else shelf.copy(playlists = shelf.playlists.take(1) + featured + shelf.playlists.drop(1))
        }
    }

    fun featuredPlaylist(root: JSONObject, id: String): MoodPlaylist? =
        objects(root, setOf("musicDetailHeaderRenderer", "musicResponsiveHeaderRenderer"))
            .firstNotNullOfOrNull { header ->
                header.text("title").takeIf(String::isNotBlank)?.let { title ->
                    MoodPlaylist(id, title, header.text("subtitle"), desktopArtistArtworkUrl(bestThumbnail(header), 544))
                }
            }

    fun continuation(root: JSONObject): String? =
        listOf("nextContinuationData" to "continuation", "reloadContinuationData" to "continuation", "continuationCommand" to "token")
            .firstNotNullOfOrNull { (renderer, field) ->
                objects(root, setOf(renderer)).firstNotNullOfOrNull { it.optString(field).takeIf(String::isNotBlank) }
            }

    fun decadePlaylists(shelves: List<MoodShelf>): List<MoodPlaylist> {
        val byDecade = sortedMapOf<Int, MoodPlaylist>()
        val used = HashSet<String>()
        shelves.forEach { shelf ->
            val decade = decadeOf(shelf.title)?.takeIf { it in FIRST_DECADE..LAST_DECADE } ?: return@forEach
            if (decade in byDecade) return@forEach
            val playlist = shelf.playlists.firstOrNull { it.artworkUrl.isNotBlank() && used.add(it.id) } ?: return@forEach
            byDecade[decade] = playlist.copy(subtitle = shelf.title)
        }
        return byDecade.values.toList()
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

    private fun JSONObject.text(key: String): String =
        optJSONObject(key)?.let { it.optJSONArray("runs")?.joinText() ?: it.optString("simpleText") }.orEmpty()

    private fun JSONArray.joinText(): String =
        (0 until length()).joinToString("") { optJSONObject(it)?.optString("text").orEmpty() }.trim()
}
