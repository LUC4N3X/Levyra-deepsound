package com.luc4n3x.levyra.desktop.core.news

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import java.io.StringReader
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import org.w3c.dom.Element
import org.xml.sax.InputSource

data class NewsStory(
    val url: String,
    val title: String,
    val source: String,
    val imageUrl: String,
    val publishedAtMs: Long
)

data class NewsSource(val name: String, val feedUrl: String)

class MusicNews(private val client: OkHttpClient = ExtractorHttp.client) {
    suspend fun latest(languageCode: String, limit: Int = 60): List<NewsStory> = coroutineScope {
        sources(languageCode).map { source ->
            async(Dispatchers.IO) {
                try {
                    fetch(source.feedUrl)?.let { parse(it, source.name) }.orEmpty()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten()
            .distinctBy { it.url }
            .sortedByDescending { it.publishedAtMs }
            .take(limit)
            .let { withArticleImages(it) }
    }

    private suspend fun withArticleImages(stories: List<NewsStory>): List<NewsStory> = coroutineScope {
        val gate = Semaphore(ARTICLE_PARALLELISM)
        val lookups = stories.filter { it.imageUrl.isBlank() }
            .take(MAX_ARTICLE_LOOKUPS)
            .associate { story -> story.url to async(Dispatchers.IO) { gate.withPermit { articleImageAt(story.url) } } }
        stories.map { story ->
            lookups[story.url]?.await()?.takeIf(String::isNotBlank)?.let { story.copy(imageUrl = it) } ?: story
        }
    }

    private fun articleImageAt(url: String): String = try {
        val request = Request.Builder().url(url).header("Accept", "text/html").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || !response.request.url.isHttps) return@use ""
            val body = response.body ?: return@use ""
            val source = body.source()
            val buffer = Buffer()
            while (buffer.size < MAX_ARTICLE_HEAD_BYTES && source.read(buffer, READ_CHUNK_BYTES) != -1L) Unit
            articleImage(buffer.readString(body.contentType()?.charset() ?: Charsets.UTF_8))
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ""
    }

    private suspend fun fetch(url: String): String? = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body ?: return@use null
            if (body.contentLength() > MAX_BYTES) return@use null
            body.string().takeIf { it.length <= MAX_BYTES }
        }
    }

    companion object {
        private const val MAX_BYTES = 4L * 1024L * 1024L
        private const val MAX_ARTICLE_LOOKUPS = 30
        private const val ARTICLE_PARALLELISM = 6
        private const val MAX_ARTICLE_HEAD_BYTES = 96L * 1024L
        private const val READ_CHUNK_BYTES = 8L * 1024L
        private val openGraphImage = Regex(
            """<meta\b[^>]*?(?:property|name)\s*=\s*["'](?:og:image|og:image:secure_url|twitter:image)["'][^>]*?\bcontent\s*=\s*["']([^"']+)["']""" +
                """|<meta\b[^>]*?\bcontent\s*=\s*["']([^"']+)["'][^>]*?(?:property|name)\s*=\s*["'](?:og:image|og:image:secure_url|twitter:image)["']""",
            RegexOption.IGNORE_CASE
        )

        fun articleImage(html: String): String =
            openGraphImage.findAll(html)
                .map { match -> match.groupValues[1].ifBlank { match.groupValues[2] }.trim().replace("&amp;", "&") }
                .firstOrNull { it.startsWith("https://", ignoreCase = true) && it.length > "https://".length }
                .orEmpty()
        private val pitchfork = NewsSource("Pitchfork", "https://pitchfork.com/feed/feed-news/rss")
        private val english = listOf(
            pitchfork,
            NewsSource("Stereogum", "https://stereogum.com/feed/"),
            NewsSource("Consequence", "https://consequence.net/feed/"),
            NewsSource("NME", "https://www.nme.com/news/music/feed")
        )
        private val byLanguage = mapOf(
            "it" to listOf(
                NewsSource("Rockol", "https://www.rockol.it/rss/news"),
                NewsSource("All Music Italia", "https://www.allmusicitalia.it/feed"),
                NewsSource("Rolling Stone Italia", "https://www.rollingstone.it/musica/feed/")
            ),
            "de" to listOf(NewsSource("Musikexpress", "https://www.musikexpress.de/feed/"), pitchfork),
            "fr" to listOf(NewsSource("Les Inrocks", "https://www.lesinrocks.com/musique/feed/"), pitchfork),
            "es" to listOf(NewsSource("Mondo Sonoro", "https://www.mondosonoro.com/feed/"), pitchfork)
        )
        private val imageInHtml = Regex("""<img[^>]+src=["'](https://[^"']+)["']""", RegexOption.IGNORE_CASE)
        private val tags = Regex("<[^>]+>")
        private val lenientRfc = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

        fun sources(languageCode: String): List<NewsSource> =
            byLanguage[languageCode.substringBefore('-').lowercase(Locale.ROOT)] ?: english

        fun parse(xml: String, source: String): List<NewsStory> {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                isExpandEntityReferences = false
            }
            val document = runCatching { factory.newDocumentBuilder().parse(InputSource(StringReader(xml))) }.getOrNull() ?: return emptyList()
            val items = document.getElementsByTagNameNS("*", "item").let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } } +
                document.getElementsByTagNameNS("*", "entry").let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }
            return items.mapNotNull { item ->
                val title = child(item, "title")?.textContent?.replace(tags, "")?.trim().orEmpty()
                val link = child(item, "link")?.let { it.getAttribute("href").ifBlank { it.textContent } }?.trim().orEmpty()
                if (title.isEmpty() || !link.startsWith("https://", ignoreCase = true)) return@mapNotNull null
                val date = (child(item, "pubDate") ?: child(item, "published") ?: child(item, "updated") ?: child(item, "date"))?.textContent.orEmpty()
                NewsStory(link, decode(title), source, image(item), parseDate(date))
            }
        }

        private fun child(item: Element, name: String): Element? {
            val nodes = item.childNodes
            return (0 until nodes.length).map { nodes.item(it) }.filterIsInstance<Element>().firstOrNull { it.localName == name || it.nodeName == name }
        }

        private fun image(item: Element): String {
            val nodes = item.childNodes
            val elements = (0 until nodes.length).map { nodes.item(it) }.filterIsInstance<Element>()
            elements.firstOrNull { it.localName in setOf("content", "thumbnail") && it.getAttribute("url").startsWith("https://") }
                ?.let { return it.getAttribute("url") }
            elements.firstOrNull { it.localName == "enclosure" && it.getAttribute("type").startsWith("image") && it.getAttribute("url").startsWith("https://") }
                ?.let { return it.getAttribute("url") }
            val html = elements.filter { it.localName == "encoded" || it.localName == "description" || it.localName == "content" }.joinToString(" ") { it.textContent }
            return imageInHtml.find(html)?.groupValues?.get(1).orEmpty()
        }

        private fun decode(value: String): String = value
            .replace("&amp;", "&").replace("&quot;", "\"").replace("&#8217;", "’").replace("&#8216;", "‘")
            .replace("&#8220;", "“").replace("&#8221;", "”").replace("&#039;", "'").replace("&#8211;", "–")

        fun parseDate(raw: String): Long {
            val value = raw.trim()
            if (value.isEmpty()) return 0L
            return runCatching { ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
                .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
                .recoverCatching { ZonedDateTime.parse(value.substringAfter(", "), lenientRfc).toInstant().toEpochMilli() }
                .getOrDefault(0L)
        }
    }
}
