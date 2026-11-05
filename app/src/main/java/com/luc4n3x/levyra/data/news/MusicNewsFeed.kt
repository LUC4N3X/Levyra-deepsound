package com.luc4n3x.levyra.data.news

import com.luc4n3x.levyra.data.qobuz.QobuzCatalogParser
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

data class MusicNewsStory(
    val url: String,
    val title: String,
    val source: String,
    val imageUrl: String,
    val publishedAtMs: Long
)

internal data class MusicNewsPage(val stories: List<MusicNewsStory>, val fetchedAtMs: Long)

internal object MusicNewsFeedParser {
    const val MAX_STORIES = 30
    private val doctype = Regex("<!DOCTYPE|<!ENTITY", RegexOption.IGNORE_CASE)
    private val inlineImage = Regex("<img\\b[^>]*?\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val imageExtension = Regex("\\.(jpe?g|png|webp|gif|avif)$", RegexOption.IGNORE_CASE)

    fun parse(xml: String, source: String): List<MusicNewsStory> {
        if (doctype.containsMatchIn(xml)) return emptyList()
        val root = runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isExpandEntityReferences = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            }
            factory.newDocumentBuilder().parse(InputSource(StringReader(xml))).documentElement
        }.getOrNull() ?: return emptyList()
        val entries = ArrayList<Element>()
        collectEntries(root, entries)
        return entries.asSequence()
            .mapNotNull { story(it, source) }
            .distinctBy { it.url }
            .take(MAX_STORIES)
            .toList()
    }

    fun isHttps(url: String): Boolean = url.startsWith("https://", ignoreCase = true) && url.length > "https://".length

    private fun collectEntries(element: Element, into: MutableList<Element>) {
        if (into.size >= MAX_STORIES * 2) return
        if (localName(element) == "item" || localName(element) == "entry") {
            into += element
            return
        }
        children(element).forEach { collectEntries(it, into) }
    }

    private fun story(entry: Element, source: String): MusicNewsStory? {
        val nodes = children(entry)
        val title = nodes.firstOrNull { localName(it) == "title" }?.textContent?.let(QobuzCatalogParser::text).orEmpty()
        if (title.isBlank()) return null
        val url = nodes.asSequence()
            .filter { localName(it) == "link" && it.getAttribute("rel").let { rel -> rel.isBlank() || rel == "alternate" } }
            .map { it.textContent.trim().ifBlank { it.getAttribute("href").trim() } }
            .firstOrNull(::isHttps) ?: return null
        val published = nodes.firstOrNull { localName(it) in DATE_FIELDS && it.textContent.isNotBlank() }
            ?.textContent?.let(::parseDate) ?: 0L
        return MusicNewsStory(url = url, title = title, source = source, imageUrl = image(nodes), publishedAtMs = published)
    }

    private fun image(nodes: List<Element>): String {
        val media = nodes.firstNotNullOfOrNull { node ->
            val url = node.getAttribute("url").trim()
            val isMedia = node.namespaceURI == MEDIA_NAMESPACE || node.nodeName.startsWith("media:")
            val accepted = when {
                !isMedia || !isHttps(url) -> false
                localName(node) == "thumbnail" -> true
                localName(node) == "content" -> isImage(node.getAttribute("medium"), node.getAttribute("type"), url)
                else -> false
            }
            url.takeIf { accepted }
        }
        if (media != null) return media
        val enclosure = nodes.firstNotNullOfOrNull { node ->
            node.getAttribute("url").trim().takeIf {
                localName(node) == "enclosure" && node.getAttribute("type").startsWith("image/", ignoreCase = true) && isHttps(it)
            }
        }
        if (enclosure != null) return enclosure
        return nodes.asSequence()
            .filter { localName(it) in HTML_FIELDS }
            .mapNotNull { inlineImage.find(it.textContent)?.groupValues?.get(1)?.trim()?.replace("&amp;", "&") }
            .firstOrNull(::isHttps)
            .orEmpty()
    }

    private fun isImage(medium: String, type: String, url: String): Boolean = when {
        medium.isNotBlank() -> medium.equals("image", ignoreCase = true)
        type.isNotBlank() -> type.startsWith("image/", ignoreCase = true)
        else -> imageExtension.containsMatchIn(url.substringBefore('?').substringBefore('#'))
    }

    internal fun parseDate(raw: String): Long {
        val value = raw.trim()
        return runCatching { ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .recoverCatching { Instant.parse(value).toEpochMilli() }
            .getOrDefault(0L)
    }

    private fun children(element: Element): List<Element> {
        val nodes = element.childNodes
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun localName(element: Element): String =
        (element.localName ?: element.nodeName.substringAfter(':')).lowercase(Locale.ROOT)

    private const val MEDIA_NAMESPACE = "http://search.yahoo.com/mrss/"
    private val DATE_FIELDS = setOf("pubdate", "published", "updated", "date")
    private val HTML_FIELDS = setOf("description", "encoded", "content", "summary")
}

internal object MusicNewsCodec {
    fun encode(page: MusicNewsPage): String = JSONObject()
        .put("fetchedAtMs", page.fetchedAtMs)
        .put("stories", JSONArray().apply {
            page.stories.forEach {
                put(
                    JSONObject().put("url", it.url).put("title", it.title).put("source", it.source)
                        .put("image", it.imageUrl).put("publishedAtMs", it.publishedAtMs)
                )
            }
        })
        .toString()

    fun decode(raw: String): MusicNewsPage? = runCatching {
        val json = JSONObject(raw)
        val array = json.optJSONArray("stories") ?: JSONArray()
        MusicNewsPage(
            stories = (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                MusicNewsStory(
                    url = item.optString("url"),
                    title = item.optString("title"),
                    source = item.optString("source"),
                    imageUrl = item.optString("image").takeIf(MusicNewsFeedParser::isHttps).orEmpty(),
                    publishedAtMs = item.optLong("publishedAtMs").coerceAtLeast(0L)
                ).takeIf { MusicNewsFeedParser.isHttps(it.url) && it.title.isNotBlank() }
            }.take(MusicNewsFeedParser.MAX_STORIES),
            fetchedAtMs = json.getLong("fetchedAtMs")
        )
    }.getOrNull()
}
