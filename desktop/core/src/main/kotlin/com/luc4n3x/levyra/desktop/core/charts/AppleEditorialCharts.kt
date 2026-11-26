package com.luc4n3x.levyra.desktop.core.charts

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.model.Track
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject

data class AppleDailyChart(val playlistId: String, val label: String, val global: Boolean)

data class AppleCityChart(val playlistId: String, val badge: String, val name: String, val storefront: String = "")

data class AppleChartPlaylist(val id: String, val title: String, val artworkUrl: String, val tracks: List<Track>)

internal data class AppleEditorialItem(
    val id: String,
    val kind: String,
    val title: String,
    val subtitle: String,
    val artworkTemplate: String,
    val url: String,
    val durationMs: Long,
    val album: String,
    val explicit: Boolean
)

internal data class AppleEditorialSection(val itemKind: String, val items: List<AppleEditorialItem>)

class AppleEditorialCharts(
    private val client: OkHttpClient = ExtractorHttp.client,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val pages = ConcurrentHashMap<String, Pair<Long, List<AppleEditorialSection>>>()

    suspend fun dailyTop100(storefront: String, language: String): List<AppleDailyChart> =
        dailyCharts(sections(DAILY_TOP_100_PATH, storefront, language), storefront)

    suspend fun cityCharts(storefront: String, language: String): List<AppleCityChart> {
        val front = normalizeStorefront(storefront)
        val local = cityCharts(sections(NEW_PATH, front, language))
        if (local.size >= MIN_LOCAL_CITY_CHARTS || front == FALLBACK_STOREFRONT) return local
        val global = cityCharts(sections(NEW_PATH, FALLBACK_STOREFRONT, language)).map { it.copy(storefront = FALLBACK_STOREFRONT) }
        return (local + global).distinctBy { it.playlistId }
    }

    suspend fun playlist(storefront: String, playlistId: String, language: String): AppleChartPlaylist? {
        val id = playlistId(playlistId) ?: return null
        val sections = sections("playlist/levyra/$id", normalizeStorefront(storefront), language)
        val header = sections.firstOrNull { it.itemKind in HEADER_KINDS }?.items?.firstOrNull()
        val tracks = sections.filter { it.itemKind == TRACK_KIND }
            .flatMap { it.items }
            .filter { it.kind == "song" }
            .map(::track)
            .distinctBy { it.id }
        if (tracks.isEmpty()) return null
        return AppleChartPlaylist(id, header?.title.orEmpty(), artwork(header?.artworkTemplate.orEmpty(), ARTWORK_SIZE), tracks)
    }

    private suspend fun sections(path: String, storefront: String, language: String): List<AppleEditorialSection> {
        val front = normalizeStorefront(storefront)
        val lang = normalizeLanguage(language)
        val key = "$front|$lang|$path"
        val now = clock()
        pages[key]?.takeIf { now - it.first < PAGE_TTL_MS }?.let { return it.second }
        val parsed = withContext(dispatcher) {
            fetch("https://music.apple.com/$front/$path?l=$lang")?.let(::parseSections).orEmpty()
        }
        if (parsed.isNotEmpty()) {
            if (pages.size >= MAX_PAGES) pages.clear()
            pages[key] = now to parsed
        }
        return parsed
    }

    private fun fetch(url: String): String? = runCatching {
        if (!isCatalogUrl(url)) return@runCatching null
        val request = Request.Builder().url(url).header("Accept", "text/html").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || !isCatalogUrl(response.request.url.toString())) return@use null
            val body = response.body ?: return@use null
            val source = body.source()
            val buffer = Buffer()
            while (buffer.size < MAX_PAGE_BYTES && source.read(buffer, READ_CHUNK_BYTES) != -1L) Unit
            if (buffer.size >= MAX_PAGE_BYTES) null else buffer.readUtf8()
        }
    }.getOrNull()

    companion object {
        const val GLOBAL_PLAYLIST_ID = "pl.d25f5d1181894928af76c85c967f8f31"
        const val FALLBACK_STOREFRONT = "us"
        private const val DAILY_TOP_100_PATH = "room/6504092645"
        private const val NEW_PATH = "new"
        private const val TRACK_KIND = "trackLockup"
        private val HEADER_KINDS = setOf("containerDetailHeaderLockup", "headerComponentModel")
        private const val MIN_LOCAL_CITY_CHARTS = 3
        private const val MAX_CITY_CHARTS = 30
        private const val COUNTRY_CHART_SIZE = "100"
        private const val ARTWORK_SIZE = 600
        private const val MAX_SECTIONS = 40
        private const val MAX_ITEMS = 100
        private const val MAX_PAGES = 24
        private const val PAGE_TTL_MS = 60L * 60L * 1000L
        private const val MAX_PAGE_BYTES = 6L * 1024L * 1024L
        private const val READ_CHUNK_BYTES = 64L * 1024L
        private const val CHART_ALBUM = "Apple Music Charts"
        private val leadingMarkets = listOf("us", "gb")
        private val countryCharts = mapOf(
            "it" to "pl.737e067787df485a8062e2c4927d94db",
            "us" to "pl.606afcbb70264d2eb2b51d8dbcfa6a12",
            "gb" to "pl.c2273b7e89b44121b3093f67228918e7",
            "es" to "pl.0d656d7feae64198bc5fb1b02786ed75",
            "fr" to "pl.6e8cfd81d51042648fa36c9df5236b8d",
            "de" to "pl.c10a2c113db14685a0b09fa5834d8e8b",
            "pt" to "pl.5437c1490ac74e9e9505fc7d1f201655",
            "br" to "pl.11ac7cc7d09741c5822e8c66e5c7edbb",
            "mx" to "pl.df3f10ca27b1479087de2cd3f9f6716b",
            "nl" to "pl.26fb1998d54a4b3192be548529a97f8e",
            "pl" to "pl.8c91cbb0ef4e48308dbbba4238135eaf",
            "ro" to "pl.0c6bea611ad54c79b854299bc515a5a6",
            "gr" to "pl.0f15f3a8ba014979b9fdd7a0ef906dca",
            "se" to "pl.5876877c387b4ffb8860ac3ea2c244c3",
            "dk" to "pl.d08496850bc840a4874e877177a69f9f",
            "cz" to "pl.e447d9ba54254130a76143bf6fdfa65c",
            "ua" to "pl.815f78effb3844909a8259d759ecbddb",
            "ru" to "pl.728bd30a9247487c80a483f4168a9dcd",
            "tr" to "pl.f3e0d6ef238542609572c18b0de1513b",
            "sa" to "pl.a5365fa3b6ec4a34994339ca100801ae",
            "cn" to "pl.fde851dc95ce4ffbb74028dfd254ced5",
            "jp" to "pl.043a2c9876114d95a4659988497567be",
            "kr" to "pl.d3d10c32fbc540b38e266367dc8cb00c",
            "in" to "pl.c0e98d2423e54c39b3df955c24df3cc5",
            "id" to "pl.2b7e089dc9ef4dd7a18429df9c6e26a3",
            "vn" to "pl.550110ec6feb4ae0aff364bcde6d1372",
            "th" to "pl.c509137d97214632a087129ece060a3d",
            "ph" to "pl.b9eb00f9d195440e8b0bdf19b8db7f34",
            "tw" to "pl.741ff34016704547853b953ec5181d83",
            "il" to "pl.0c9765e5330048af96c2336fa7bc3525",
            "hu" to "pl.cee165c3a51e466481bde5de75d6dee3",
            "bg" to "pl.040cf0b4c7e9467eb9eed2d33e7a29d6",
            "fi" to "pl.acea41a017664a8ebcd5aa1622aecc88",
            "no" to "pl.05a67957c3974729aac67c01247e55b6",
            "sk" to "pl.2e50996a5bf44ab78cbb5c34b1992701",
            "my" to "pl.a165defeeccb4b17a59bb5c85637b9b7",
            "ar" to "pl.7ae8594e422f44658e58212d876d9323",
            "ca" to "pl.79bac9045a2540e0b195e983df8ba569",
            "au" to "pl.18be1cf04dfd4ffb9b6b0453e8fae8f1"
        )
        private val knownCharts = countryCharts.values.toSet()
        private val titlePrefix = Regex("""^\s*(?:le\s+)?(?:top|топ|トップ)\s*-?\s*100\s*[:：\-–]?\s*""", RegexOption.IGNORE_CASE)
        private val chartTitle = Regex("""^(?:Top|Топ)[\s-]*\d+\s*[:：]""", RegexOption.IGNORE_CASE)
        private val serverData = Regex("""<script[^>]*id="serialized-server-data"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        private val playlistPattern = Regex("""^pl\.[a-z0-9-]{8,64}$""")

        fun normalizeStorefront(value: String): String {
            val code = value.trim().lowercase(Locale.ROOT)
            return if (code.length == 2 && code.all { it in 'a'..'z' }) code else FALLBACK_STOREFRONT
        }

        private fun normalizeLanguage(value: String): String {
            val code = value.trim().lowercase(Locale.ROOT).substringBefore('_').substringBefore('-')
            return if (code.length in 2..3 && code.all { it in 'a'..'z' }) code else "en"
        }

        fun playlistId(value: String): String? = value.trim().lowercase(Locale.ROOT).takeIf(playlistPattern::matches)

        private fun playlistIdOf(item: AppleEditorialItem): String? =
            playlistId(item.id) ?: playlistId(item.url.substringAfterLast('/').substringBefore('?'))

        fun isCatalogUrl(url: String): Boolean {
            val parsed = runCatching { URI(url) }.getOrNull() ?: return false
            return parsed.scheme == "https" && parsed.host == "music.apple.com" && parsed.port == -1 && parsed.userInfo == null
        }

        internal fun dailyCharts(sections: List<AppleEditorialSection>, storefront: String): List<AppleDailyChart> {
            val all = sections.asSequence()
                .flatMap { it.items.asSequence() }
                .filter { it.kind == "playlist" }
                .mapNotNull { item ->
                    val id = playlistIdOf(item) ?: return@mapNotNull null
                    val known = id == GLOBAL_PLAYLIST_ID || id in knownCharts
                    if (!known && !item.title.contains("100")) return@mapNotNull null
                    AppleDailyChart(id, chartLabel(item.title), id == GLOBAL_PLAYLIST_ID)
                }
                .distinctBy { it.playlistId }
                .toList()
            val countries = all.filterNot { it.global }
            val byId = countries.associateBy { it.playlistId }
            val local = countryCharts[normalizeStorefront(storefront)]?.let(byId::get) ?: countries.firstOrNull()
            val leading = leadingMarkets.mapNotNull { countryCharts[it]?.let(byId::get) }
            val (known, other) = countries.partition { it.playlistId in knownCharts }
            return (listOfNotNull(local, all.firstOrNull { it.global }) + leading + known + other).distinct()
        }

        internal fun cityCharts(sections: List<AppleEditorialSection>): List<AppleCityChart> =
            sections.asSequence()
                .flatMap { it.items.asSequence() }
                .filter { it.kind == "playlist" }
                .mapNotNull { item ->
                    val id = playlistIdOf(item) ?: return@mapNotNull null
                    val (badge, name) = chartParts(item.title) ?: return@mapNotNull null
                    if (badge.filter(Char::isDigit) == COUNTRY_CHART_SIZE || id == GLOBAL_PLAYLIST_ID || id in knownCharts) return@mapNotNull null
                    AppleCityChart(id, badge, name)
                }
                .distinctBy { it.playlistId }
                .take(MAX_CITY_CHARTS)
                .toList()

        fun chartLabel(title: String): String {
            val trimmed = title.trim()
            return trimmed.replaceFirst(titlePrefix, "").trim().ifEmpty { trimmed }.replaceFirstChar { it.titlecase(Locale.ROOT) }
        }

        fun chartParts(title: String): Pair<String, String>? {
            val match = chartTitle.find(title) ?: return null
            val badge = title.substring(0, match.range.last).trim()
            val name = title.substring(match.range.last + 1).trim()
            if (name.isBlank()) return null
            return badge to name
        }

        fun artwork(template: String, size: Int): String {
            if (template.isBlank() || !template.contains("{w}")) return template
            return template.replace("{w}", size.toString()).replace("{h}", size.toString()).replace("{c}", "cc").replace("{f}", "jpg")
        }

        private fun track(item: AppleEditorialItem): Track {
            val artist = item.subtitle.ifBlank { "Various Artists" }
            return Track(
                id = ChartFeedParser.chartId(item.title, artist),
                title = item.title,
                artist = artist,
                album = item.album.ifBlank { CHART_ALBUM },
                videoUrl = "",
                artworkUrl = artwork(item.artworkTemplate, ARTWORK_SIZE),
                durationMs = item.durationMs,
                explicit = item.explicit.takeIf { it }
            )
        }

        internal fun parseSections(html: String): List<AppleEditorialSection> {
            val payload = serverData.find(html)?.groupValues?.get(1)?.let(::decodeEntities) ?: return emptyList()
            val root = runCatching { JSONObject(payload) }.getOrNull() ?: return emptyList()
            val sections = pageNode(root)?.optJSONArray("sections") ?: return emptyList()
            return (0 until minOf(sections.length(), MAX_SECTIONS)).mapNotNull { index ->
                val section = sections.optJSONObject(index) ?: return@mapNotNull null
                val items = section.optJSONArray("items") ?: return@mapNotNull null
                val parsed = (0 until minOf(items.length(), MAX_ITEMS)).mapNotNull { items.optJSONObject(it)?.let(::parseItem) }
                    .distinctBy { "${it.kind}:${it.id}" }
                if (parsed.isEmpty()) null else AppleEditorialSection(section.optString("itemKind"), parsed)
            }
        }

        private fun pageNode(root: JSONObject): JSONObject? {
            var node: Any? = root
            repeat(6) {
                when (val current = node) {
                    is JSONObject -> {
                        if (current.has("sections")) return current
                        node = current.opt("data")
                    }
                    is JSONArray -> node = current.opt(0)
                    else -> return null
                }
            }
            return null
        }

        private fun parseItem(item: JSONObject): AppleEditorialItem? {
            val title = item.optJSONArray("titleLinks")?.optJSONObject(0)?.optString("title").orEmpty().ifBlank { item.optString("title") }.trim()
            if (title.isBlank()) return null
            val descriptor = item.optJSONObject("contentDescriptor")
                ?: item.optJSONObject("segue")?.optJSONObject("destination")?.optJSONObject("contentDescriptor")
                ?: item.optJSONArray("items")?.optJSONObject(0)?.optJSONObject("contentDescriptor")
            val id = descriptor?.optJSONObject("identifiers")?.optString("storeAdamID").orEmpty().ifBlank { item.optString("id") }
            val artwork = item.optJSONObject("artwork")?.optJSONObject("dictionary")?.optString("url").orEmpty()
            val subtitle = item.optJSONArray("subtitleLinks")?.let { links ->
                (0 until links.length()).mapNotNull { links.optJSONObject(it)?.optString("title")?.trim()?.takeIf(String::isNotBlank) }.joinToString(", ")
            }.orEmpty().ifBlank { item.optString("subtitle") }.trim()
            return AppleEditorialItem(
                id = id,
                kind = descriptor?.optString("kind").orEmpty(),
                title = title,
                subtitle = subtitle,
                artworkTemplate = artwork.takeIf { it.startsWith("https://") }.orEmpty(),
                url = descriptor?.optString("url").orEmpty().takeIf { it.startsWith("https://music.apple.com/") }.orEmpty(),
                durationMs = item.optLong("duration", 0L).coerceAtLeast(0L),
                album = item.optJSONArray("tertiaryLinks")?.optJSONObject(0)?.optString("title").orEmpty().trim(),
                explicit = item.optBoolean("showExplicitBadge", false)
            )
        }

        private fun decodeEntities(value: String): String =
            if (value.trimStart().startsWith("{") || value.trimStart().startsWith("[")) value
            else value.replace("&quot;", "\"").replace("&#34;", "\"").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&")
    }
}
