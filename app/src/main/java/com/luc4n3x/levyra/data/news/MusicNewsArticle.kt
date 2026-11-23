package com.luc4n3x.levyra.data.news

import com.luc4n3x.levyra.data.qobuz.QobuzCatalogParser
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Locale

data class MusicNewsArticle(
    val title: String,
    val author: String,
    val heroImageUrl: String,
    val blocks: List<MusicNewsBlock>
)

sealed interface MusicNewsBlock {
    data class Paragraph(val html: String) : MusicNewsBlock
    data class Heading(val text: String) : MusicNewsBlock
    data class Quote(val html: String) : MusicNewsBlock
    data class Image(val url: String, val caption: String) : MusicNewsBlock
    data class Bullet(val html: String) : MusicNewsBlock
}

data class MusicNewsReaderState(
    val story: MusicNewsStory,
    val article: MusicNewsArticle?,
    val loading: Boolean
)

internal object MusicNewsArticleParser {
    private const val MAX_BLOCKS = 160
    private const val PRELOADED_STATE = "window.__PRELOADED_STATE__"
    private const val MAX_BODY_CHARS = 200_000
    private const val ARTICLE_END = "</article>"
    private const val JSON_LD_TYPE = "application/ld+json"
    private val IMAGE_SIZES = listOf("xl", "lg", "xxl", "md", "sm")
    private val options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    private val block = Regex("<(p|h[2-6]|blockquote|figure|ul|ol)\\b([^>]*)>(.*?)</\\1\\s*>", options)
    private val imageSource = Regex("<img\\b[^>]*?\\b(?:data-lazy-src|data-src|src)\\s*=\\s*[\"'](https://[^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val caption = Regex("<figcaption\\b[^>]*>(.*?)</figcaption\\s*>", options)
    private val listItem = Regex("<li\\b[^>]*>(.*?)</li\\s*>", options)
    private val embedded = Regex("<(iframe|script|video|audio|form)\\b", RegexOption.IGNORE_CASE)
    private val socialEmbed = Regex("instagram-media|tiktok-embed|twitter-tweet|wp-block-embed", RegexOption.IGNORE_CASE)
    private val paragraphTag = Regex("</?p\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val inlineImage = Regex("<(?:img|picture|source)\\b[^>]*>|</picture\\s*>", RegexOption.IGNORE_CASE)
    private val anchorLeadingSpace = Regex("(<a\\b[^>]*>)(\\s+)", RegexOption.IGNORE_CASE)
    private val anchorTrailingSpace = Regex("(\\s+)(</a\\s*>)", RegexOption.IGNORE_CASE)
    private val linkWithTarget = Regex(
        "<a\\b[^>]*?\\bhref\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>(.*?)</a\\s*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val unsafeLinkOpen = Regex("<a\\b(?!\\s+href=\"https://[^\"]*\">)[^>]*>", RegexOption.IGNORE_CASE)
    private val anchor = Regex("<a\\b[^>]*>.*?</a\\s*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val relatedModule = Regex("related|latest|popular|most-read|read-more|correlat|lett|recommend", RegexOption.IGNORE_CASE)
    private val sizeSuffix = Regex("-\\d+x\\d+(?=\\.[a-z]+$)", RegexOption.IGNORE_CASE)

    fun wordpress(json: String, story: MusicNewsStory): MusicNewsArticle? {
        val post = JSONObject(json)
        val blocks = blocks(post.optJSONObject("content")?.string("rendered").orEmpty())
        if (blocks.isEmpty()) return null
        val embeddedData = post.optJSONObject("_embedded")
        val author = embeddedData?.optJSONArray("author")?.optJSONObject(0)?.string("name")
            ?.let(QobuzCatalogParser::text).orEmpty()
        val hero = embeddedData?.optJSONArray("wp:featuredmedia")?.optJSONObject(0)?.string("source_url")
            ?.trim()?.takeIf(MusicNewsFeedParser::isHttps) ?: story.imageUrl
        val title = post.optJSONObject("title")?.string("rendered")?.let(QobuzCatalogParser::text)
            ?.takeIf(String::isNotBlank) ?: story.title
        val body = withoutLeadingHero(blocks, hero).takeIf(List<MusicNewsBlock>::isNotEmpty) ?: return null
        return MusicNewsArticle(title = title, author = author, heroImageUrl = hero, blocks = body)
    }

    fun blocks(html: String): List<MusicNewsBlock> {
        val groups = block.findAll(html)
            .map { match -> toGroup(match.groupValues[1].lowercase(Locale.ROOT), match.groupValues[2], match.groupValues[3]) }
            .toList()
        return groups.filterNot { it.relatedStories }
            .flatMap { it.blocks }
            .take(MAX_BLOCKS)
            .withoutOrphanHeadings()
    }

    private class BlockGroup(val blocks: List<MusicNewsBlock>, val relatedStories: Boolean = false)

    private fun toGroup(tag: String, attributes: String, inner: String): BlockGroup =
        if (tag == "ul" || tag == "ol") listGroup(attributes, inner) else BlockGroup(toBlocks(tag, attributes, inner).toList())

    private fun List<MusicNewsBlock>.withoutOrphanHeadings(): List<MusicNewsBlock> =
        filterIndexed { index, block -> block !is MusicNewsBlock.Heading || getOrNull(index + 1).let { it != null && it !is MusicNewsBlock.Heading } }

    private fun toBlocks(tag: String, attributes: String, inner: String): Sequence<MusicNewsBlock> = when {
        socialEmbed.containsMatchIn(attributes) || embedded.containsMatchIn(inner) -> emptySequence()
        tag == "figure" -> listOfNotNull(image(inner)).asSequence()
        tag == "p" && text(inner).isBlank() -> listOfNotNull(image(inner)).asSequence()
        tag == "p" -> sequenceOf(MusicNewsBlock.Paragraph(inlineHtml(inner)))
        tag == "blockquote" -> quote(inner)
        else -> text(inner).takeIf(String::isNotBlank)?.let { sequenceOf(MusicNewsBlock.Heading(it)) } ?: emptySequence()
    }

    private fun listGroup(attributes: String, inner: String): BlockGroup {
        val items = listItem.findAll(inner).map { inlineHtml(it.groupValues[1]) }.filter { text(it).isNotBlank() }.toList()
        val looksRelated = inlineImage.containsMatchIn(inner) || relatedModule.containsMatchIn(attributes)
        return BlockGroup(items.map(MusicNewsBlock::Bullet), relatedStories = looksRelated && items.isNotEmpty() && items.all(::isOnlyLinks))
    }

    private fun isOnlyLinks(html: String): Boolean = anchor.containsMatchIn(html) && text(anchor.replace(html, "")).isBlank()

    private fun inlineHtml(html: String): String = html.replace(inlineImage, "")
        .replace(linkWithTarget) { match ->
            val target = match.groupValues[1]
            val inner = match.groupValues[2]
            if (MusicNewsFeedParser.isHttps(target.replace("&amp;", "&").trim())) "<a href=\"${target.trim()}\">$inner</a>" else inner
        }
        .replace(unsafeLinkOpen, "")
        .replace(anchorLeadingSpace, "\$2\$1")
        .replace(anchorTrailingSpace, "\$2\$1")
        .trim()

    private fun quote(inner: String): Sequence<MusicNewsBlock> {
        val html = inlineHtml(inner).replace(paragraphTag, "<br>").trim().removePrefix("<br>").removeSuffix("<br>").trim()
        return if (text(html).isBlank()) emptySequence() else sequenceOf(MusicNewsBlock.Quote(html))
    }

    private fun image(inner: String): MusicNewsBlock.Image? {
        val url = imageSource.find(inner)?.groupValues?.get(1)?.replace("&amp;", "&")?.trim() ?: return null
        val caption = caption.find(inner)?.groupValues?.get(1)?.let(::text).orEmpty()
        return MusicNewsBlock.Image(url, caption)
    }

    fun preloadedState(html: String, story: MusicNewsStory): MusicNewsArticle? {
        val marker = html.indexOf(PRELOADED_STATE)
        if (marker < 0) return null
        val start = html.indexOf('{', marker)
        val end = html.indexOf(";</script>", start)
        if (start < 0 || end <= start) return null
        val article = JSONObject(html.substring(start, end)).optJSONObject("transformed")?.optJSONObject("article") ?: return null
        val blocks = ArrayList<MusicNewsBlock>()
        article.optJSONArray("body")?.let { treeBlock(it, blocks) }
        if (blocks.isEmpty()) return null
        val header = article.optJSONObject("headerProps")
        val authors = header?.optJSONObject("contributors")?.optJSONObject("author")?.optJSONArray("items")
        val author = (0 until (authors?.length() ?: 0))
            .mapNotNull { authors?.optJSONObject(it)?.string("name")?.trim()?.takeIf(String::isNotBlank) }
            .joinToString(", ")
        val hero = sourceUrl(header?.optJSONObject("lede")?.optJSONObject("sources")) ?: story.imageUrl
        val title = header?.string("dangerousHed")?.let(::text)?.takeIf(String::isNotBlank) ?: story.title
        return MusicNewsArticle(title, author, hero, blocks.take(MAX_BLOCKS))
    }

    fun jsonLdBody(html: String, story: MusicNewsStory, bodyMarker: String, bodyEnd: String = ARTICLE_END): MusicNewsArticle? {
        val marker = html.indexOf(bodyMarker).takeIf { bodyMarker.isNotBlank() && it >= 0 } ?: return null
        val start = html.indexOf('>', marker) + 1
        if (start <= 0) return null
        val end = html.indexOf(bodyEnd.ifBlank { ARTICLE_END }, start).takeIf { it > start } ?: minOf(html.length, start + MAX_BODY_CHARS)
        val blocks = blocks(html.substring(start, end))
        if (blocks.isEmpty()) return null
        val meta = jsonLdArticle(html)
        val author = when (val value = meta?.opt("author")) {
            is JSONObject -> value.string("name")
            is JSONArray -> (0 until value.length()).mapNotNull { value.optJSONObject(it)?.string("name")?.takeIf(String::isNotBlank) }.joinToString(", ")
            is String -> value
            else -> ""
        }.let(::text)
        val hero = when (val value = meta?.opt("image")) {
            is String -> value
            is JSONArray -> value.opt(0).let { first -> (first as? JSONObject)?.string("url") ?: first as? String }
            is JSONObject -> value.string("url")
            else -> null
        }?.trim()?.takeIf(MusicNewsFeedParser::isHttps) ?: story.imageUrl
        val title = meta?.string("headline")?.let(::text)?.takeIf(String::isNotBlank) ?: story.title
        val body = withoutLeadingHero(blocks, hero).takeIf(List<MusicNewsBlock>::isNotEmpty) ?: return null
        return MusicNewsArticle(title, author, hero, body)
    }

    private fun withoutLeadingHero(blocks: List<MusicNewsBlock>, hero: String): List<MusicNewsBlock> =
        blocks.dropWhile { it is MusicNewsBlock.Image && sameImage(it.url, hero) }

    private fun jsonLdArticle(html: String): JSONObject? = jsonLdScripts(html)
        .mapNotNull { script -> runCatching { JSONTokener(script.trim()).nextValue() }.getOrNull() }
        .flatMap { value ->
            when (value) {
                is JSONArray -> (0 until value.length()).mapNotNull(value::optJSONObject).asSequence()
                is JSONObject -> value.optJSONArray("@graph")?.let { graph -> (0 until graph.length()).mapNotNull(graph::optJSONObject).asSequence() }
                    ?: sequenceOf(value)
                else -> emptySequence()
            }
        }
        .firstOrNull { it.string("@type").let { type -> type.endsWith("Article") || type == "BlogPosting" } }

    private fun jsonLdScripts(html: String): Sequence<String> = sequence {
        var from = 0
        while (true) {
            val type = html.indexOf(JSON_LD_TYPE, from, ignoreCase = true).takeIf { it >= 0 } ?: break
            val start = html.indexOf('>', type) + 1
            val end = html.indexOf("</script", start, ignoreCase = true)
            if (start <= 0 || end < start) break
            yield(html.substring(start, end))
            from = end
        }
    }

    private fun treeBlock(node: JSONArray, out: MutableList<MusicNewsBlock>) {
        if (out.size >= MAX_BLOCKS) return
        when (node.string(0).lowercase(Locale.ROOT)) {
            "p" -> treeHtml(node).trim().takeIf { text(it).isNotBlank() }?.let { out += MusicNewsBlock.Paragraph(it) }
            "h2", "h3", "h4", "h5" -> text(treeHtml(node)).takeIf(String::isNotBlank)?.let { out += MusicNewsBlock.Heading(it) }
            "blockquote" -> treeHtml(node).trim().takeIf { text(it).isNotBlank() }?.let { out += MusicNewsBlock.Quote(it) }
            "ul", "ol" -> treeChildren(node).filterIsInstance<JSONArray>()
                .filter { it.string(0).equals("li", ignoreCase = true) }
                .map { treeHtml(it).trim() }
                .filter { text(it).isNotBlank() }
                .take(MAX_BLOCKS - out.size)
                .forEach { out += MusicNewsBlock.Bullet(it) }
            "inline-embed" -> embeddedImage(node.optJSONObject(1))?.let { out += it }
            "div", "section", "article" -> treeChildren(node).filterIsInstance<JSONArray>().forEach { treeBlock(it, out) }
        }
    }

    private fun embeddedImage(attributes: JSONObject?): MusicNewsBlock.Image? {
        if (attributes?.string("type") != "image") return null
        val props = attributes.optJSONObject("props") ?: return null
        val url = sourceUrl(props.optJSONObject("image")?.optJSONObject("sources")) ?: return null
        return MusicNewsBlock.Image(url, text(props.string("dangerousCaption")))
    }

    private fun sourceUrl(sources: JSONObject?): String? = IMAGE_SIZES.firstNotNullOfOrNull { size ->
        sources?.optJSONObject(size)?.string("url")?.trim()?.takeIf(MusicNewsFeedParser::isHttps)
    }

    private fun treeChildren(node: JSONArray): List<Any> =
        (1 until node.length()).mapNotNull { node.opt(it) }.filterNot { it is JSONObject }

    private fun treeHtml(node: JSONArray): String = buildString {
        treeChildren(node).forEach { child ->
            when (child) {
                is String -> append(escapeHtml(child))
                is JSONArray -> appendTreeElement(child)
            }
        }
    }

    private fun StringBuilder.appendTreeElement(element: JSONArray) {
        val inner = treeHtml(element)
        when (element.string(0).lowercase(Locale.ROOT)) {
            "a" -> {
                val href = element.optJSONObject(1)?.string("href")?.trim()?.takeIf(MusicNewsFeedParser::isHttps)
                if (href == null) append(inner) else append("<a href=\"").append(escapeHtml(href)).append("\">").append(inner).append("</a>")
            }
            "em", "i" -> append("<i>").append(inner).append("</i>")
            "strong", "b" -> append("<b>").append(inner).append("</b>")
            "br" -> append("<br>")
            "ad", "native-ad", "cm-unit", "inline-embed", "script" -> Unit
            else -> append(inner)
        }
    }

    private fun escapeHtml(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun sameImage(first: String, second: String): Boolean =
        imageKey(first).isNotBlank() && imageKey(first) == imageKey(second)

    private fun imageKey(url: String): String =
        url.substringBefore('#').substringBefore('?').substringAfter("://").replace(sizeSuffix, "")

    private fun text(html: String): String = QobuzCatalogParser.text(html).trim()

    private fun JSONObject.string(key: String): String = (opt(key) as? String).orEmpty()

    private fun JSONArray.string(index: Int): String = (opt(index) as? String).orEmpty()
}
